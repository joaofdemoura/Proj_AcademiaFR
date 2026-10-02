<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;

class Gym extends Model
{
    protected $fillable = ['name', 'logo_path', 'address', 'active'];

    protected function casts(): array
    {
        return ['active' => 'boolean'];
    }

    public function users(): BelongsToMany
    {
        return $this->belongsToMany(User::class, 'gym_users')->withPivot('status', 'joined_at');
    }

    /** Planos que podem ser contratados nesta academia (inclui planos da rede). */
    public function plans(): BelongsToMany
    {
        return $this->belongsToMany(Plan::class, 'plan_gyms');
    }
}
