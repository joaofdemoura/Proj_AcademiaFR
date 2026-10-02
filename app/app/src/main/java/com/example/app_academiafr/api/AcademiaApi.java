package com.example.app_academiafr.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Rotas da API Laravel usadas pelo app (ver api/README.md no repositório). */
public interface AcademiaApi {
    @POST("login")
    Call<Models.LoginResponse> login(@Body Models.LoginRequest body);

    @GET("me")
    Call<Models.User> me();

    /** Recebe o token explicitamente: o app já pode ter limpado a sessão quando a chamada sai. */
    @POST("logout")
    Call<Void> logout(@Header("Authorization") String bearerToken);

    @GET("me/workouts")
    Call<List<Models.WorkoutPlan>> workouts();

    @GET("me/memberships")
    Call<List<Models.Membership>> memberships();

    @GET("gyms/{gym}/plans")
    Call<List<Models.Plan>> plans(@Path("gym") long gymId);
}
