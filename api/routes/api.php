<?php

use App\Http\Controllers\AdminController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\StudentController;
use Illuminate\Support\Facades\Route;

// Login: 5 tentativas por minuto por IP.
Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:5,1');

Route::middleware('auth:sanctum')->group(function () {
    Route::get('/me', [AuthController::class, 'me']);
    Route::post('/logout', [AuthController::class, 'logout']);

    // App do aluno
    Route::get('/me/workouts', [StudentController::class, 'workouts']);
    Route::get('/me/memberships', [StudentController::class, 'memberships']);
    Route::get('/gyms/{gym}/plans', [StudentController::class, 'plans']);

    // Painel web (somente administradores)
    Route::get('/admin/data', [AdminController::class, 'data']);
    Route::post('/admin/records', [AdminController::class, 'save']);
});
