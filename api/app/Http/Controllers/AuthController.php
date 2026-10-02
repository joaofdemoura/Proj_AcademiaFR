<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;

class AuthController extends Controller
{
    /**
     * Login do app e do painel. Aceita e-mail (alunos) ou usuário (admins).
     * Devolve um token Sanctum para enviar em "Authorization: Bearer <token>".
     */
    public function login(Request $request): JsonResponse
    {
        $data = $request->validate([
            'login' => ['required', 'string', 'max:255'],
            'password' => ['required', 'string', 'max:255'],
            'device_name' => ['required', 'string', 'max:100'],
        ]);

        $field = str_contains($data['login'], '@') ? 'email' : 'username';
        $user = User::where($field, $data['login'])->first();

        // password NULL = aluno cadastrado pelo painel que ainda não ativou o acesso.
        if (! $user || ! $user->password || ! Hash::check($data['password'], $user->password)) {
            throw ValidationException::withMessages(['login' => 'Usuário ou senha incorretos.']);
        }

        return response()->json([
            'token' => $user->createToken($data['device_name'])->plainTextToken,
            'user' => $this->profile($user),
        ]);
    }

    public function me(Request $request): JsonResponse
    {
        return response()->json($this->profile($request->user()));
    }

    public function logout(Request $request): JsonResponse
    {
        $request->user()->currentAccessToken()->delete();

        return response()->json(['ok' => true]);
    }

    private function profile(User $user): array
    {
        $user->loadMissing('gyms', 'roles');

        return [
            'id' => $user->id,
            'name' => $user->name,
            'email' => $user->email,
            'username' => $user->username,
            'avatar_path' => $user->avatar_path,
            'is_global_admin' => $user->is_global_admin,
            'gyms' => $user->gyms->map(fn ($gym) => [
                'id' => $gym->id,
                'name' => $gym->name,
                'status' => $gym->pivot->status,
                'roles' => $user->roles->where('gym_id', $gym->id)->pluck('role')->values(),
            ])->values(),
        ];
    }
}
