<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class GymUserRole extends Model
{
    public $timestamps = false;

    public $incrementing = false;

    protected $primaryKey = null;

    protected $fillable = ['gym_id', 'user_id', 'role'];

    public function gym(): BelongsTo
    {
        return $this->belongsTo(Gym::class);
    }
}
