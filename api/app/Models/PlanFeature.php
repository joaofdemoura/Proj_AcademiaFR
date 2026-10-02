<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PlanFeature extends Model
{
    public $timestamps = false;

    protected $fillable = ['plan_id', 'description', 'sort_order'];
}
