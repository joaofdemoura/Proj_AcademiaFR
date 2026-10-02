<?php

namespace Database\Seeders;

use App\Models\Gym;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

/**
 * Cria (ou atualiza) os administradores do painel web.
 * As senhas vêm do .env: ADMIN_PASSWORD, ADMIN_PRIME_PASSWORD e ADMIN_MOVE_PASSWORD.
 *
 *   php artisan db:seed --class=AdminSeeder
 */
class AdminSeeder extends Seeder
{
    public function run(): void
    {
        $admins = [
            ['username' => 'admin', 'name' => 'Administrador geral', 'email' => 'admin@academiafr.local', 'env' => 'ADMIN_PASSWORD', 'gym' => null],
            ['username' => 'admin.prime', 'name' => 'Admin Pryme', 'email' => 'admin.prime@academiafr.local', 'env' => 'ADMIN_PRIME_PASSWORD', 'gym' => 'Pryme Academia'],
            ['username' => 'admin.move', 'name' => 'Admin Move', 'email' => 'admin.move@academiafr.local', 'env' => 'ADMIN_MOVE_PASSWORD', 'gym' => 'Move Academia'],
        ];

        DB::transaction(function () use ($admins) {
            foreach ($admins as $a) {
                $password = env($a['env']);
                if (! $password) {
                    throw new \RuntimeException("Defina {$a['env']} no .env antes de rodar o seeder.");
                }

                $user = User::firstOrNew(['username' => $a['username']]);
                $user->fill(['name' => $a['name'], 'email' => $a['email'], 'password' => $password]);
                $user->forceFill(['is_global_admin' => $a['gym'] === null, 'email_verified_at' => now()])->save();

                if ($a['gym']) {
                    $gym = Gym::where('name', $a['gym'])->firstOrFail();
                    DB::table('gym_users')->insertOrIgnore(['gym_id' => $gym->id, 'user_id' => $user->id, 'status' => 'active', 'joined_at' => now()->toDateString()]);
                    DB::table('gym_user_roles')->insertOrIgnore(['gym_id' => $gym->id, 'user_id' => $user->id, 'role' => 'admin']);
                }

                $this->command?->info("{$a['username']} pronto");
            }
        });
    }
}
