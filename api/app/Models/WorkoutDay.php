<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class WorkoutDay extends Model
{
    public $timestamps = false;

    protected $fillable = ['workout_plan_id', 'label', 'title', 'estimated_minutes', 'sort_order'];

    public function exercises(): HasMany
    {
        return $this->hasMany(WorkoutExercise::class)->orderBy('sort_order');
    }
}
