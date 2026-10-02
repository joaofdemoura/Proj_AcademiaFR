<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Exercise extends Model
{
    protected $fillable = ['name', 'muscle_group', 'instructions', 'video_url', 'image_path'];
}
