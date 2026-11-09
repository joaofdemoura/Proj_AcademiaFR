<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Support\Facades\DB;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class PanelAccessTest extends TestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->assertSame(':memory:', config('database.connections.sqlite.database'));
        $this->assertSame('sqlite', config('database.default'));
        DB::unprepared(file_get_contents(__DIR__.'/../Fixtures/panel.sql'));
        DB::table('gyms')->insert([['id' => 1, 'name' => 'Pryme Academia'], ['id' => 2, 'name' => 'Move Academia']]);
        foreach ([1, 2] as $gym) {
            $this->account($gym, 'student', $gym);
            $this->account($gym + 2, 'instructor', $gym);
            $this->account($gym + 4, 'admin', $gym);
        }
        User::create(['id' => 7, 'name' => 'Geral', 'email' => 'geral@example.com'])->forceFill(['is_global_admin' => true])->save();
    }

    private function account(int $id, string $role, int $gym): void
    {
        $user = new User(['name' => "Conta {$id}", 'email' => "conta{$id}@example.com"]);
        $user->id = $id;
        $user->save();
        DB::table('gym_users')->insert(['gym_id' => $gym, 'user_id' => $id, 'status' => 'active']);
        DB::table('gym_user_roles')->insert(['gym_id' => $gym, 'user_id' => $id, 'role' => $role]);
    }

    private function loginAs(int $id): void
    {
        Sanctum::actingAs(User::findOrFail($id));
    }

    private function workout(int $student, string $unit): array
    {
        return ['kind' => 'workouts', 'record' => ['id' => 'new', 'student' => (string) $student,
            'unit' => $unit, 'name' => 'Treino de teste', 'goal' => 'Força', 'days' => 'Segunda', 'status' => 'Publicado',
            'exercises' => [['name' => 'Agachamento', 'sets' => 3, 'reps' => '10–12', 'rest' => 60]]]];
    }

    public function test_guests_and_students_cannot_open_the_panel(): void
    {
        $this->getJson('/api/admin/data')->assertUnauthorized();
        $this->loginAs(1);
        $this->getJson('/api/admin/data')->assertForbidden();
        $this->postJson('/api/admin/records', $this->workout(1, 'Pryme Academia'))->assertForbidden();
    }

    public function test_general_admin_sees_both_units_and_unit_admins_only_their_unit(): void
    {
        $this->loginAs(7);
        $this->getJson('/api/admin/data')->assertOk()->assertJsonCount(2, 'students');
        foreach ([5 => 'Pryme Academia', 6 => 'Move Academia'] as $id => $unit) {
            $this->loginAs($id);
            $this->getJson('/api/admin/data')->assertOk()->assertJsonCount(1, 'students')->assertJsonPath('students.0.unit', $unit);
        }
    }

    public function test_each_personal_can_publish_only_in_their_own_unit(): void
    {
        foreach ([3 => [1, 'Pryme Academia', 2, 'Move Academia'], 4 => [2, 'Move Academia', 1, 'Pryme Academia']] as $id => [$student, $unit, $other, $otherUnit]) {
            $this->loginAs($id);
            $this->getJson('/api/admin/data')->assertOk()->assertJsonCount(1, 'students')->assertJsonPath('students.0.unit', $unit)->assertJsonCount(0, 'plans');
            $this->postJson('/api/admin/records', $this->workout($student, $unit))->assertOk()->assertJsonPath('record.unit', $unit)->assertJsonPath('record.status', 'Publicado');
            $this->postJson('/api/admin/records', $this->workout($other, $otherUnit))->assertForbidden();
            $this->postJson('/api/admin/records', $this->workout($other, $unit))->assertStatus(400);
            foreach (['students', 'plans'] as $kind) {
                $this->postJson('/api/admin/records', ['kind' => $kind, 'record' => []])->assertForbidden();
            }
        }
    }

    public function test_personal_cannot_edit_a_workout_from_another_unit_by_forging_its_id(): void
    {
        $this->loginAs(4);
        $saved = $this->postJson('/api/admin/records', $this->workout(2, 'Move Academia'))->assertOk()->json('record.id');
        $this->loginAs(3);
        $payload = $this->workout(1, 'Pryme Academia');
        $payload['record']['id'] = $saved;
        $this->postJson('/api/admin/records', $payload)->assertForbidden();
        $this->getJson('/api/admin/data')->assertJsonCount(0, 'workouts');
    }

    public function test_inactive_membership_revokes_personal_access(): void
    {
        DB::table('gym_users')->where('user_id', 3)->update(['status' => 'inactive']);
        $this->loginAs(3);
        $this->getJson('/api/admin/data')->assertForbidden();
    }
    public function test_inactive_student_cannot_read_gym_plans(): void
    {
        $this->loginAs(1);
        DB::table('gym_users')->where('user_id', 1)->update(['status' => 'inactive']);
        $this->getJson('/api/gyms/1/plans')->assertForbidden();
    }

    public function test_disabled_gym_does_not_allow_student_plan_access(): void
    {
        $this->loginAs(1);
        DB::table('gyms')->where('id', 1)->update(['active' => false]);
        $this->getJson('/api/gyms/1/plans')->assertForbidden();
    }

    public function test_student_registration_password_duplicates_and_edits_are_consistent(): void
    {
        $plan = DB::table('plans')->insertGetId(['gym_id' => 1, 'name' => 'Plano teste', 'price' => 99, 'billing_period' => 'monthly']);
        DB::table('plan_gyms')->insert(['gym_id' => 1, 'plan_id' => $plan]);
        $this->loginAs(7);
        $record = ['id' => 'new', 'name' => 'Novo Aluno', 'email' => 'novo@example.com', 'unit' => 'Pryme Academia',
            'plan' => (string) $plan, 'status' => 'Ativo', 'expires' => now()->addMonth()->toDateString()];
        $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $record])->assertStatus(400);
        $password = bin2hex(random_bytes(12));
        $record['password'] = $password;
        $response = $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $record])
            ->assertOk()->assertJsonMissingPath('record.password');
        $id = $response->json('record.id');
        $hash = User::findOrFail($id)->password;
        $this->assertTrue(\Illuminate\Support\Facades\Hash::check($password, $hash));
        $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $record])->assertStatus(400);
        unset($record['password']);
        $record['id'] = $id;
        $record['status'] = 'Inativo';
        $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $record])->assertOk();
        $this->assertSame($hash, User::findOrFail($id)->password);
        $this->assertSame('cancelled', DB::table('memberships')->where('user_id', $id)->value('status'));
        $record['status'] = 'Ativo';
        $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $record])->assertOk();
        $this->assertSame('active', DB::table('memberships')->where('user_id', $id)->value('status'));
        $this->assertNull(DB::table('memberships')->where('user_id', $id)->value('cancelled_at'));
    }

    public function test_each_unit_admin_can_manage_own_records_and_cannot_forge_other_unit_ids(): void
    {
        $this->loginAs(7);
        $plans = [];
        foreach ([1 => 'Pryme Academia', 2 => 'Move Academia'] as $gym => $unit) {
            $record = ['id' => 'new', 'name' => 'Plano '.$gym, 'price' => 90, 'features' => 'Musculação', 'unit' => $unit];
            $plans[$gym] = $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $record])->assertOk()->json('record');
        }
        foreach ([5 => [1, 2], 6 => [2, 1]] as $admin => [$gym, $other]) {
            $this->loginAs($admin);
            $plan = $plans[$gym];
            $plan['price'] = 110;
            $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $plan])->assertOk()->assertJsonPath('record.price', 110);
            $student = ['id' => 'new', 'name' => 'Aluno da unidade '.$gym, 'email' => 'unidade'.$gym.'@example.com',
                'password' => bin2hex(random_bytes(12)), 'unit' => $plan['unit'], 'plan' => $plan['id'], 'status' => 'Ativo', 'expires' => now()->addMonth()->toDateString()];
            $saved = $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $student])->assertOk()->json('record');
            $saved['name'] = 'Aluno editado '.$gym;
            $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $saved])->assertOk()->assertJsonPath('record.name', $saved['name']);
            $this->postJson('/api/admin/records', $this->workout((int) $saved['id'], $plan['unit']))->assertOk();
            $foreign = $plans[$other];
            $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $foreign])->assertForbidden();
            $foreign['unit'] = $plan['unit'];
            $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $foreign])->assertForbidden();
            $student['unit'] = $plans[$other]['unit'];
            $student['plan'] = $plans[$other]['id'];
            $this->postJson('/api/admin/records', ['kind' => 'students', 'record' => $student])->assertForbidden();
            $this->postJson('/api/admin/records', $this->workout($other, $plans[$other]['unit']))->assertForbidden();
            $this->getJson('/api/admin/data')->assertOk()->assertJsonCount(2, 'students');
        }
    }

    public function test_global_admin_publishes_in_both_units_and_only_global_admin_edits_network_plans(): void
    {
        $this->loginAs(7);
        foreach ([1 => 'Pryme Academia', 2 => 'Move Academia'] as $student => $unit) {
            $this->postJson('/api/admin/records', $this->workout($student, $unit))->assertOk()->assertJsonPath('record.unit', $unit);
        }
        $record = ['id' => 'new', 'name' => 'Plano da rede', 'price' => 150, 'features' => 'Duas unidades', 'unit' => 'Todas'];
        $network = $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $record])->assertOk()->json('record');
        foreach ([5 => 'Pryme Academia', 6 => 'Move Academia'] as $admin => $unit) {
            $this->loginAs($admin);
            $this->getJson('/api/admin/data')->assertOk()->assertJsonCount(1, 'plans');
            $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $network])->assertForbidden();
            $forged = [...$network, 'unit' => $unit];
            $this->postJson('/api/admin/records', ['kind' => 'plans', 'record' => $forged])->assertForbidden();
        }
    }

}
