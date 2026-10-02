<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class WorkoutPlan extends Model
{
    protected $fillable = [
        'gym_id', 'student_id', 'instructor_id', 'name', 'goal', 'version',
        'status', 'published_at', 'schedule_description', 'starts_on', 'ends_on', 'archived_at',
    ];

    protected function casts(): array
    {
        return [
            'published_at' => 'datetime',
            'starts_on' => 'date:Y-m-d',
            'ends_on' => 'date:Y-m-d',
            'archived_at' => 'datetime',
        ];
    }

    public function student(): BelongsTo
    {
        return $this->belongsTo(User::class, 'student_id');
    }

    public function gym(): BelongsTo
    {
        return $this->belongsTo(Gym::class);
    }

    public function days(): HasMany
    {
        return $this->hasMany(WorkoutDay::class)->orderBy('sort_order');
    }

    /** Fichas que o aluno vê no app: publicadas e não arquivadas. */
    public function scopeVisibleToStudent(Builder $query): void
    {
        $query->where('status', 'published')->whereNull('archived_at');
    }
}
