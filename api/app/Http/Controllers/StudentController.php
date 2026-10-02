<?php

namespace App\Http\Controllers;

use App\Models\Gym;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Rotas do app Android para o aluno logado. */
class StudentController extends Controller
{
    /** Fichas publicadas do aluno, com dias e exercícios. */
    public function workouts(Request $request): JsonResponse
    {
        $plans = $request->user()->workoutPlans()
            ->visibleToStudent()
            ->with(['gym:id,name', 'days.exercises.exercise:id,name,muscle_group,video_url,image_path'])
            ->orderByDesc('published_at')
            ->get();

        return response()->json($plans);
    }

    /** Matrículas do aluno (plano, status e vencimento). */
    public function memberships(Request $request): JsonResponse
    {
        $memberships = $request->user()->memberships()
            ->with(['gym:id,name', 'plan:id,name,billing_period,price'])
            ->orderByDesc('ends_on')
            ->get();

        return response()->json($memberships);
    }

    /** Planos disponíveis numa academia da qual o usuário faz parte. */
    public function plans(Request $request, Gym $gym): JsonResponse
    {
        $user = $request->user();
        abort_unless($user->is_global_admin || $user->gyms()->whereKey($gym->id)->exists(), 403, 'Você não faz parte desta academia.');

        return response()->json(
            $gym->plans()->where('active', true)->with('features')->orderBy('price')->get()
        );
    }
}
