<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Plan extends Model
{
    protected $fillable = ['gym_id', 'name', 'description', 'billing_period', 'price', 'currency', 'active'];

    protected function casts(): array
    {
        return ['price' => 'decimal:2', 'active' => 'boolean'];
    }

    public function features(): HasMany
    {
        return $this->hasMany(PlanFeature::class)->orderBy('sort_order');
    }

    /** Academias onde o plano pode ser contratado. */
    public function gyms(): BelongsToMany
    {
        return $this->belongsToMany(Gym::class, 'plan_gyms');
    }
}
