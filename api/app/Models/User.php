<?php

namespace App\Models;

use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

#[Fillable(['name', 'email', 'username', 'password', 'phone', 'avatar_path'])]
#[Hidden(['password', 'remember_token'])]
class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable;

    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
            'is_global_admin' => 'boolean',
        ];
    }

    /** Academias às quais o usuário pertence (tabela gym_users). */
    public function gyms(): BelongsToMany
    {
        return $this->belongsToMany(Gym::class, 'gym_users')->withPivot('status', 'joined_at');
    }

    public function roles(): HasMany
    {
        return $this->hasMany(GymUserRole::class);
    }

    public function memberships(): HasMany
    {
        return $this->hasMany(Membership::class);
    }

    public function workoutPlans(): HasMany
    {
        return $this->hasMany(WorkoutPlan::class, 'student_id');
    }

    public function hasRole(int $gymId, string $role): bool
    {
        return $this->roles->contains(fn (GymUserRole $r) => $r->gym_id === $gymId && $r->role === $role);
    }

    /** IDs das academias que o usuário administra; null = todas (admin geral). */
    public function administeredGymIds(): ?array
    {
        if ($this->is_global_admin) {
            return null;
        }

        return $this->roles->where('role', 'admin')->pluck('gym_id')->values()->all();
    }
}
