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
}
