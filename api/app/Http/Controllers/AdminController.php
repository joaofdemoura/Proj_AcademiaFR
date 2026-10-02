<?php

namespace App\Http\Controllers;

use App\Models\Exercise;
use App\Models\Gym;
use App\Models\Membership;
use App\Models\Plan;
use App\Models\User;
use App\Models\WorkoutPlan;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * Rotas do painel web. Mantêm o formato que o painel usa
 * ({students, plans, workouts}) e traduzem para as tabelas do banco.
 * Admin geral vê tudo; admin de unidade só a própria academia.
 */
class AdminController extends Controller
{
    private const STATUS_TO_DB = ['Ativo' => 'active', 'Pendente' => 'pending'];

    public function data(Request $request): JsonResponse
    {
        $allowed = $this->allowedGymIds($request);

        return response()->json([
            'students' => $this->students($allowed),
            'plans' => $this->plans($allowed),
            'workouts' => $this->workouts($allowed),
        ]);
    }

    public function save(Request $request): JsonResponse
    {
        $allowed = $this->allowedGymIds($request);
        $kind = $request->validate(['kind' => ['required', Rule::in(['students', 'plans', 'workouts'])]])['kind'];
        $record = $request->input('record');
        abort_unless(is_array($record), 400, 'Registro inválido.');

        $saved = match ($kind) {
            'students' => $this->saveStudent($record, $allowed),
            'plans' => $this->savePlan($record, $allowed),
            'workouts' => $this->saveWorkout($record, $allowed),
        };

        return response()->json(['record' => $saved]);
    }

    // ---------------------------------------------------------------- leitura

    private function students(?array $allowed, ?int $onlyUser = null, ?int $onlyGym = null): array
    {
        $rows = DB::table('gym_users as gu')
            ->join('users as u', 'u.id', '=', 'gu.user_id')
            ->join('gyms as g', 'g.id', '=', 'gu.gym_id')
            ->join('gym_user_roles as r', fn ($j) => $j->on('r.gym_id', '=', 'gu.gym_id')->on('r.user_id', '=', 'gu.user_id')->where('r.role', 'student'))
            ->when($allowed !== null, fn ($q) => $q->whereIn('gu.gym_id', $allowed))
            ->when($onlyUser, fn ($q) => $q->where('gu.user_id', $onlyUser)->where('gu.gym_id', $onlyGym))
            ->select('u.id', 'u.name', 'u.email', 'g.name as unit', 'gu.gym_id', 'gu.status')
            ->get();

        $latest = Membership::whereIn('user_id', $rows->pluck('id'))
            ->orderByDesc('ends_on')->orderByDesc('id')->get()
            ->unique(fn ($m) => $m->gym_id.'-'.$m->user_id)
            ->keyBy(fn ($m) => $m->gym_id.'-'.$m->user_id);

        return $rows->map(function ($row) use ($latest) {
            $m = $latest->get($row->gym_id.'-'.$row->id);
            $status = match (true) {
                $row->status === 'inactive', ! $m => 'Inativo',
                $m->status === 'active' => 'Ativo',
                $m->status === 'pending' => 'Pendente',
                default => 'Inativo',
            };

            return [
                'id' => (string) $row->id,
                'name' => $row->name,
                'email' => $row->email,
                'unit' => $row->unit,
                'plan' => $m ? (string) $m->plan_id : '',
                'status' => $status,
                'expires' => $m ? $m->ends_on->format('Y-m-d') : '',
            ];
        })->values()->all();
    }

    private function plans(?array $allowed): array
    {
        return Plan::with('features')
            ->when($allowed !== null, fn ($q) => $q->where(fn ($q) => $q->whereNull('gym_id')->orWhereIn('gym_id', $allowed)))
            ->orderBy('price')->get()
            ->map(fn (Plan $p) => $this->planRecord($p))->all();
    }

    private function planRecord(Plan $p): array
    {
        return [
            'id' => (string) $p->id,
            'name' => $p->name,
            'price' => (float) $p->price,
            'features' => $p->features->pluck('description')->implode("\n"),
            'unit' => $p->gym_id ? Gym::find($p->gym_id)?->name : 'Todas',
        ];
    }

    private function workouts(?array $allowed): array
    {
        return WorkoutPlan::with(['gym:id,name', 'days.exercises.exercise:id,name'])
            ->whereNull('archived_at')
            ->when($allowed !== null, fn ($q) => $q->whereIn('gym_id', $allowed))
            ->orderByDesc('updated_at')->get()
            ->map(fn (WorkoutPlan $w) => $this->workoutRecord($w))->all();
    }

    private function workoutRecord(WorkoutPlan $w): array
    {
        $w->loadMissing(['gym:id,name', 'days.exercises.exercise:id,name']);

        return [
            'id' => (string) $w->id,
            'name' => $w->name,
            'student' => (string) $w->student_id,
            'unit' => $w->gym->name,
            'goal' => $w->goal ?? '',
            'status' => $w->status === 'published' ? 'Publicado' : 'Rascunho',
            'days' => $w->schedule_description ?? '',
            'exercises' => $w->days->flatMap(fn ($d) => $d->exercises)->map(fn ($e) => [
                'name' => $e->exercise->name,
                'sets' => $e->sets,
                'reps' => $e->repetitions_min === $e->repetitions_max || $e->repetitions_max === null
                    ? (string) $e->repetitions_min
                    : $e->repetitions_min.'–'.$e->repetitions_max,
                'rest' => $e->rest_seconds ?? 0,
            ])->values()->all(),
            'updated' => $w->updated_at?->format('Y-m-d') ?? '',
        ];
    }

    // ---------------------------------------------------------------- alunos

    private function saveStudent(array $record, ?array $allowed): array
    {
        $data = $this->validateRecord($record, [
            'id' => ['required', 'string'],
            'name' => ['required', 'string', 'min:2', 'max:100'],
            'email' => ['required', 'email', 'max:255'],
            'unit' => ['required', 'string'],
            'plan' => ['required', 'string'],
            'status' => ['required', Rule::in(['Ativo', 'Pendente', 'Inativo'])],
            'expires' => ['required', 'date_format:Y-m-d'],
        ]);

        $gym = $this->gymByName($data['unit'], $allowed);
        $plan = ctype_digit($data['plan']) ? Plan::find($data['plan']) : null;
        abort_unless($plan, 400, 'Selecione um plano válido.');
        abort_unless($plan->gyms()->whereKey($gym->id)->exists(), 400, 'O plano não está disponível nesta unidade.');

        $user = ctype_digit($data['id']) ? User::find($data['id']) : null;
        if ($user) {
            $gymIds = DB::table('gym_user_roles')->where('user_id', $user->id)->where('role', 'student')->pluck('gym_id')->map(fn ($id) => (int) $id);
            abort_if($gymIds->isEmpty(), 404, 'Aluno não encontrado.');
            abort_unless($gymIds->contains($gym->id), 400, 'Não é possível trocar o aluno de academia.');
        }
        abort_if(User::where('email', $data['email'])->when($user, fn ($q) => $q->whereKeyNot($user->id))->exists(), 400, 'Já existe uma conta com este e-mail.');

        DB::transaction(function () use (&$user, $data, $gym, $plan) {
            $user ??= new User;
            $user->fill(['name' => $data['name'], 'email' => $data['email']])->save();

            DB::table('gym_users')->upsert(
                ['gym_id' => $gym->id, 'user_id' => $user->id, 'status' => $data['status'] === 'Inativo' ? 'inactive' : 'active', 'joined_at' => now()->toDateString()],
                ['gym_id', 'user_id'], ['status'],
            );
            DB::table('gym_user_roles')->insertOrIgnore(['gym_id' => $gym->id, 'user_id' => $user->id, 'role' => 'student']);

            $m = Membership::where('gym_id', $gym->id)->where('user_id', $user->id)->orderByDesc('ends_on')->orderByDesc('id')->first();
            $m ??= new Membership(['gym_id' => $gym->id, 'user_id' => $user->id, 'status' => 'pending', 'starts_on' => now()->toDateString()]);
            if ((int) $m->plan_id !== (int) $plan->id) {
                $m->plan_id = $plan->id;
                $m->contracted_price = $plan->price;
            }
            $m->ends_on = $data['expires'];
            if ($m->starts_on > $m->ends_on) {
                $m->starts_on = $m->ends_on;
            }
            $m->status = self::STATUS_TO_DB[$data['status']] ?? $m->status;
            $m->save();
        });

        return $this->students(null, $user->id, $gym->id)[0];
    }

    // ---------------------------------------------------------------- planos

    private function savePlan(array $record, ?array $allowed): array
    {
        $data = $this->validateRecord($record, [
            'id' => ['required', 'string'],
            'name' => ['required', 'string', 'min:2', 'max:60'],
            'price' => ['required', 'numeric', 'min:0', 'max:100000'],
            'features' => ['nullable', 'string', 'max:1000'],
            'unit' => ['required', 'string'],
        ]);

        $network = $data['unit'] === 'Todas';
        abort_if($network && $allowed !== null, 403, 'Somente o administrador geral pode gerenciar planos de todas as unidades.');
        $gym = $network ? null : $this->gymByName($data['unit'], $allowed);

        $plan = ctype_digit($data['id']) ? Plan::find($data['id']) : null;
        if ($plan) {
            abort_if($plan->gym_id === null && $allowed !== null, 403, 'Somente o administrador geral pode alterar planos de todas as unidades.');
            if ($plan->gym_id !== null) {
                $this->authorizeGym($allowed, (int) $plan->gym_id);
            }
        }

        DB::transaction(function () use (&$plan, $data, $gym) {
            $plan ??= new Plan(['billing_period' => 'monthly']);
            $plan->fill(['name' => $data['name'], 'price' => $data['price'], 'gym_id' => $gym?->id])->save();

            $plan->features()->delete();
            $lines = collect(preg_split('/\r?\n/', $data['features'] ?? ''))->map(fn ($l) => trim($l))->filter()->values();
            foreach ($lines as $i => $line) {
                $plan->features()->create(['description' => mb_substr($line, 0, 255), 'sort_order' => $i]);
            }

            $target = $gym ? collect([$gym->id]) : Gym::where('active', true)->pluck('id');
            $current = DB::table('plan_gyms')->where('plan_id', $plan->id)->pluck('gym_id');
            foreach ($target->diff($current) as $gymId) {
                DB::table('plan_gyms')->insert(['gym_id' => $gymId, 'plan_id' => $plan->id]);
            }
            foreach ($current->diff($target) as $gymId) {
                $inUse = Membership::where('gym_id', $gymId)->where('plan_id', $plan->id)->exists()
                    || DB::table('promotions')->where('gym_id', $gymId)->where('plan_id', $plan->id)->exists();
                abort_if($inUse, 400, 'Há alunos ou promoções usando este plano em outra unidade; não é possível removê-la.');
                DB::table('plan_gyms')->where('gym_id', $gymId)->where('plan_id', $plan->id)->delete();
            }
        });

        return $this->planRecord($plan->fresh('features'));
    }

    // ---------------------------------------------------------------- fichas

    private function saveWorkout(array $record, ?array $allowed): array
    {
        $data = $this->validateRecord($record, [
            'id' => ['required', 'string'],
            'name' => ['required', 'string', 'min:2', 'max:100'],
            'student' => ['required', 'string'],
            'goal' => ['required', 'string', 'max:150'],
            'status' => ['required', Rule::in(['Rascunho', 'Publicado'])],
            'days' => ['required', 'string', 'max:200'],
            'exercises' => ['required', 'array', 'min:1', 'max:40'],
            'exercises.*.name' => ['required', 'string', 'min:2', 'max:100'],
            'exercises.*.sets' => ['required', 'integer', 'min:1', 'max:20'],
            'exercises.*.reps' => ['required', 'string', 'max:40'],
            'exercises.*.rest' => ['required', 'integer', 'min:0', 'max:600'],
        ]);

        $gymId = ctype_digit($data['student'])
            ? DB::table('gym_user_roles')->where('user_id', $data['student'])->where('role', 'student')->value('gym_id')
            : null;
        abort_unless($gymId, 400, 'Selecione um aluno válido.');
        $gymId = (int) $gymId;
        abort_if($allowed !== null && ! in_array($gymId, $allowed, true), 403, 'Este aluno pertence a outra academia.');

        $exercises = collect($data['exercises'])->map(function ($e) {
            abort_unless(preg_match('/^\s*(\d{1,3})\s*(?:[-–—a]\s*(\d{1,3}))?\s*$/u', $e['reps'], $m), 400, 'Repetições devem ser um número ou um intervalo, ex.: 10–12.');
            $min = (int) $m[1];
            $max = isset($m[2]) ? (int) $m[2] : $min;
            abort_if($max < $min, 400, 'No intervalo de repetições, o primeiro número deve ser o menor.');

            return [...$e, 'min' => $min, 'max' => $max];
        });

        $plan = ctype_digit($data['id']) ? WorkoutPlan::find($data['id']) : null;
        if ($plan) {
            $this->authorizeGym($allowed, (int) $plan->gym_id);
            abort_unless((int) $plan->student_id === (int) $data['student'], 400, 'Não é possível trocar o aluno de uma ficha; crie uma nova.');
        }

        $plan = DB::transaction(function () use ($plan, $data, $gymId, $exercises) {
            // Ficha que o aluno já usou não é alterada: vira histórico e nasce uma nova versão.
            $used = $plan && DB::table('workout_sessions as s')->join('workout_days as d', 'd.id', '=', 's.workout_day_id')
                ->where('d.workout_plan_id', $plan->id)->exists();
            if ($used) {
                $plan->update(['archived_at' => now()]);
                $plan = new WorkoutPlan(['version' => $plan->version + 1, 'status' => 'draft']);
            }
            $plan ??= new WorkoutPlan(['status' => 'draft']);

            $publish = $data['status'] === 'Publicado';
            $plan->fill([
                'gym_id' => $gymId,
                'student_id' => (int) $data['student'],
                'name' => $data['name'],
                'goal' => $data['goal'],
                'schedule_description' => $data['days'],
                'published_at' => $publish ? ($plan->status === 'published' ? $plan->published_at : now()) : null,
                'status' => $publish ? 'published' : 'draft',
            ])->save();

            $day = $plan->days()->firstOrCreate(['label' => 'A'], ['title' => $data['name'], 'sort_order' => 0]);
            $day->update(['title' => $data['name']]);
            $day->exercises()->delete();
            foreach ($exercises->values() as $i => $e) {
                $day->exercises()->create([
                    'exercise_id' => Exercise::firstOrCreate(['name' => trim($e['name'])])->id,
                    'sort_order' => $i,
                    'sets' => $e['sets'],
                    'repetitions_min' => $e['min'],
                    'repetitions_max' => $e['max'],
                    'rest_seconds' => $e['rest'],
                ]);
            }
            $plan->touch();

            return $plan;
        });

        return $this->workoutRecord($plan->fresh());
    }

    // ---------------------------------------------------------------- apoio

    /** IDs das academias do admin logado; null = admin geral. */
    private function allowedGymIds(Request $request): ?array
    {
        $ids = $request->user()->administeredGymIds();
        abort_if($ids !== null && $ids === [], 403, 'Acesso restrito a administradores.');

        return $ids === null ? null : array_map('intval', $ids);
    }

    private function authorizeGym(?array $allowed, int $gymId): void
    {
        abort_if($allowed !== null && ! in_array($gymId, $allowed, true), 403, 'Você só pode gerenciar registros da sua academia.');
    }

    private function gymByName(string $name, ?array $allowed): Gym
    {
        $gym = Gym::where('name', $name)->first();
        abort_unless($gym, 400, 'Unidade inválida.');
        $this->authorizeGym($allowed, (int) $gym->id);

        return $gym;
    }

    private function validateRecord(array $record, array $rules): array
    {
        $validator = validator($record, $rules);
        abort_if($validator->fails(), 400, 'Confira os campos obrigatórios e os valores informados.');

        return $validator->validated();
    }
}
