<?php

namespace Database\Seeders;

use App\Models\Gym;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class PersonalSeeder extends Seeder
{
    public function run(): void
    {
        $accounts = [
            ['personal.prime', 'Personal Pryme', 'Pryme Academia', 'PERSONAL_PRIME_PASSWORD'],
            ['personal.move', 'Personal Move', 'Move Academia', 'PERSONAL_MOVE_PASSWORD'],
        ];

        DB::transaction(function () use ($accounts) {
            foreach ($accounts as [$username, $name, $gymName, $passwordEnv]) {
                // Não altera senha nem permissões de uma conta já existente.
                if (User::where('username', $username)->exists()) {
                    $this->command?->info("{$username} já existe; preservado.");

                    continue;
                }
                $password = env($passwordEnv);
                if (! $password) {
                    throw new \RuntimeException("Defina {$passwordEnv} no .env antes de rodar o seeder.");
                }
                $gym = Gym::where('name', $gymName)->firstOrFail();
                $user = User::create(['username' => $username, 'name' => $name,
                    'email' => $username.'@academiafr.local', 'password' => $password]);
                DB::table('gym_users')->insert(['gym_id' => $gym->id, 'user_id' => $user->id,
                    'status' => 'active', 'joined_at' => now()->toDateString()]);
                DB::table('gym_user_roles')->insert(['gym_id' => $gym->id, 'user_id' => $user->id, 'role' => 'instructor']);
                $this->command?->info("{$username} criado em {$gymName}.");
            }
        });
    }
}
