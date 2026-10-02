<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class WorkoutExercise extends Model
{
    public $timestamps = false;

    protected $fillable = [
        'workout_day_id', 'exercise_id', 'sort_order', 'sets',
        'repetitions_min', 'repetitions_max', 'target_weight_kg', 'rest_seconds', 'notes',
    ];

    protected function casts(): array
    {
        return ['target_weight_kg' => 'decimal:2'];
    }

    public function exercise(): BelongsTo
    {
        return $this->belongsTo(Exercise::class);
    }
}
