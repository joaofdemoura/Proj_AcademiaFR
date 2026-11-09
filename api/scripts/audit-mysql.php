<?php
// Integração opt-in, com rollback: php scripts/audit-mysql.php --run-local
if (($argv[1] ?? '') !== '--run-local') { exit("Use --run-local no banco de desenvolvimento.\n"); }
require __DIR__.'/../vendor/autoload.php';
$app = require __DIR__.'/../bootstrap/app.php';
$app->make(Illuminate\Contracts\Console\Kernel::class)->bootstrap();
use Illuminate\Support\Facades\DB;
use Illuminate\Http\Request;
use App\Models\User;
use App\Http\Controllers\AdminController;
use App\Http\Controllers\StudentController;
function check($value, $label) { if (!$value) throw new RuntimeException($label); echo "PASS $label\n"; }
function req(User $u, array $body = []) { $r = Request::create('/', 'POST', $body); $r->setUserResolver(fn () => $u); return $r; }
$admin = User::where('username', 'admin')->firstOrFail();
$personal = User::where('username', 'personal.prime')->firstOrFail();
$a = new AdminController; $s = new StudentController;
DB::beginTransaction();
try {
    $save = fn ($kind, $record, $u = null) => $a->save(req($u ?? $admin, compact('kind', 'record')))->getData(true)['record'];
    $plan = $save('plans', ['id'=>'audit-new','name'=>'AUDIT temporário','price'=>99.90,'features'=>"Musculação\nAvaliação",'unit'=>'Pryme Academia']);
    check(count($s->plans(req($admin), App\Models\Gym::findOrFail(1))->getData(true)) > 0, 'criar plano e consultar no app');
    $password = bin2hex(random_bytes(12));
    $student = $save('students', ['id'=>'audit-new','password'=>$password,'name'=>'Aluno Auditoria','email'=>'audit-'.bin2hex(random_bytes(4)).'@example.invalid','unit'=>'Pryme Academia','plan'=>$plan['id'],'status'=>'Ativo','expires'=>date('Y-m-d', strtotime('+30 days'))]);
    $u = User::findOrFail($student['id']);
    check(Illuminate\Support\Facades\Hash::check($password, $u->password), 'senha inicial gravada como hash');
    check(!array_key_exists('password', $student), 'senha não devolvida ao painel');
    $duplicate = [...$student, 'id'=>'audit-duplicate', 'password'=>$password];
    try { $save('students', $duplicate); throw new RuntimeException('E-mail duplicado aceito'); }
    catch (Symfony\Component\HttpKernel\Exception\HttpException $e) { check($e->getStatusCode() === 400, 'cadastro duplicado recusado sem alterar conta existente'); }
    $student['name'] = 'Aluno Auditoria Editado'; $save('students', $student);
    check(Illuminate\Support\Facades\Hash::check($password, $u->fresh()->password), 'edição do aluno preserva senha de acesso');
    $loginRequest = Request::create('/api/login', 'POST', ['login'=>$u->email, 'password'=>$password, 'device_name'=>'audit-rollback']);
    $login = (new App\Http\Controllers\AuthController)->login($loginRequest)->getData(true);
    check(!empty($login['token']) && $login['user']['id'] === $u->id, 'conta cadastrada no painel autentica no contrato do app');
    check(count($s->memberships(req($u))->getData(true)) === 1, 'matrícula persistida e visível ao aluno');
    $payload = ['id'=>'audit-new','name'=>'Auditoria força','student'=>$student['id'],'unit'=>'Pryme Academia','goal'=>'Força','status'=>'Rascunho','days'=>'Segunda e quarta','exercises'=>[['name'=>'Agachamento auditoria','sets'=>3,'reps'=>'10–12','rest'=>60]]];
    $workout = $save('workouts', $payload, $personal);
    check(count($s->workouts(req($u))->getData(true)) === 0, 'rascunho invisível ao aluno');
    $payload['id'] = $workout['id']; $payload['status'] = 'Publicado';
    $published = $save('workouts', $payload, $personal);
    $visible = $s->workouts(req($u))->getData(true);
    check(count($visible) === 1 && $visible[0]['days'][0]['exercises'][0]['repetitions_max'] === 12, 'publicação e exercícios no contrato Android');
    $payload['name'] = 'Auditoria editada';
    $edited = $save('workouts', $payload, $personal);
    check($edited['id'] === $published['id'] && $edited['name'] === 'Auditoria editada', 'editar ficha sem duplicar');
    $invalid = $payload; $invalid['exercises'][0]['reps'] = '0';
    try { $save('workouts', $invalid, $personal); throw new RuntimeException('Repetições inválidas aceitas'); }
    catch (Symfony\Component\HttpKernel\Exception\HttpException $e) { check($e->getStatusCode() === 400, 'repetições inválidas recusadas antes de alterar a ficha'); }
    $oldDay = DB::table('workout_days')->where('workout_plan_id', $published['id'])->value('id');
    DB::table('workout_sessions')->insert(['gym_id'=>1, 'user_id'=>$u->id, 'workout_day_id'=>$oldDay, 'started_at'=>now(), 'finished_at'=>now(), 'status'=>'completed']);
    $versioned = $save('workouts', $payload, $personal);
    check($versioned['id'] !== $published['id'] && App\Models\WorkoutPlan::findOrFail($published['id'])->archived_at !== null, 'nova versão preserva ficha já utilizada');
    check(count($s->workouts(req($u))->getData(true)) === 1 && DB::table('workout_exercises')->where('workout_day_id', $oldDay)->count() === 1, 'histórico preservado e só versão atual visível no app');

    foreach (['students','plans'] as $kind) {
        try { $save($kind, [], $personal); throw new RuntimeException('Permissão indevida'); }
        catch (Symfony\Component\HttpKernel\Exception\HttpException $e) { check($e->getStatusCode() === 403, "personal bloqueado: $kind"); }
    }
    $student['status'] = 'Inativo'; $save('students', $student);
    try { $s->plans(req($u), App\Models\Gym::findOrFail(1)); throw new RuntimeException('Acesso inativo indevido'); }
    catch (Symfony\Component\HttpKernel\Exception\HttpException $e) { check($e->getStatusCode() === 403, 'vínculo inativo bloqueado'); }
    $student['status'] = 'Ativo'; $save('students', $student);
    check(count($s->plans(req($u), App\Models\Gym::findOrFail(1))->getData(true)) > 0, 'reativação de vínculo');
    $plan['price'] = 120; $plan['features'] = 'Recurso atualizado';
    check((float) $save('plans', $plan)['price'] === 120.0, 'editar preço e recursos do plano');
} catch (Throwable $e) { fwrite(STDERR, "FAIL ".$e->getMessage()."\n"); $failed = true; } finally { DB::rollBack(); echo "ROLLBACK: dados de auditoria removidos.\n"; }

exit(isset($failed) ? 1 : 0);
