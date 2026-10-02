package com.example.app_academiafr.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import retrofit2.Response;

/**
 * Testa o cliente do app contra a API Laravel rodando no computador (Herd).
 * Usa o aluno de teste do banco (bloco [TESTE] de banco/academiafinal.sql).
 * Se a API não estiver no ar, os testes são pulados.
 */
public class ApiContractTest {
    private static final String LOCAL_API = "http://127.0.0.1/api/";

    @BeforeClass public static void pointToLocalApi() {
        ApiClient.baseUrl = LOCAL_API;
        boolean up;
        try {
            up = ApiClient.get().me().execute().code() == 401; // sem token: a API responde 401
        } catch (Exception e) {
            up = false;
        }
        assumeTrue("API local fora do ar; testes pulados", up);
    }

    @After public void signOut() throws Exception {
        if (ApiClient.getToken() != null) ApiClient.get().logout("Bearer " + ApiClient.getToken()).execute();
        ApiClient.setToken(null);
    }

    private Models.User login(String email, String password) throws Exception {
        Response<Models.LoginResponse> r = ApiClient.get().login(new Models.LoginRequest(email, password, "teste-junit")).execute();
        assertTrue("login falhou: HTTP " + r.code(), r.isSuccessful());
        ApiClient.setToken(r.body().token);
        return r.body().user;
    }

    @Test public void senhaErradaDevolveMensagemDaApi() throws Exception {
        Response<Models.LoginResponse> r = ApiClient.get()
                .login(new Models.LoginRequest("gabriel.souza@example.com", "errada", "teste-junit")).execute();
        assertEquals(422, r.code());
        assertEquals("Usuário ou senha incorretos.", ApiClient.errorMessage(r, "?"));
    }

    @Test public void loginLePerfilEAcademias() throws Exception {
        Models.User user = login("gabriel.souza@example.com", "Gabriel@2026!");
        assertEquals("Gabriel", user.firstName());
        assertEquals("Pryme Academia", user.gyms.get(0).name);
        assertTrue(user.gyms.get(0).roles.contains("student"));

        Models.User me = ApiClient.get().me().execute().body();
        assertNotNull(me);
        assertEquals(user.email, me.email);
    }

    @Test public void leFichasMatriculasEPlanos() throws Exception {
        Models.User user = login("gabriel.souza@example.com", "Gabriel@2026!");

        List<Models.WorkoutPlan> plans = ApiClient.get().workouts().execute().body();
        assertNotNull(plans);
        for (Models.WorkoutPlan p : plans) {
            assertEquals("published", p.status);
            for (Models.WorkoutExercise e : p.allExercises()) {
                assertNotNull(e.exercise.name);
                assertTrue(e.sets > 0);
                assertFalse(e.reps().isEmpty());
            }
        }

        List<Models.Membership> memberships = ApiClient.get().memberships().execute().body();
        assertNotNull(memberships);
        for (Models.Membership m : memberships) assertNotNull(m.plan.name);

        List<Models.Plan> gymPlans = ApiClient.get().plans(user.gyms.get(0).id).execute().body();
        assertNotNull(gymPlans);
        for (Models.Plan p : gymPlans) assertTrue(ApiClient.money(p.price).startsWith("R$"));

        System.out.println("fichas=" + plans.size() + " matriculas=" + memberships.size() + " planos=" + gymPlans.size());
        if (!plans.isEmpty()) {
            Models.WorkoutPlan p = plans.get(0);
            System.out.println("ficha: " + p.name + " / " + p.scheduleDescription + " / " + p.allExercises().size() + " exercícios, 1º: "
                    + p.allExercises().get(0).exercise.name + " " + p.allExercises().get(0).sets + "x" + p.allExercises().get(0).reps());
        }
        if (!memberships.isEmpty()) System.out.println("matrícula: " + memberships.get(0).plan.name + " " + memberships.get(0).status + " vence " + ApiClient.date(memberships.get(0).endsOn));
        if (!gymPlans.isEmpty()) System.out.println("plano: " + gymPlans.get(0).name + " " + ApiClient.money(gymPlans.get(0).price) + " recursos=" + gymPlans.get(0).features.size());
    }

    @Test public void alunoNaoVePlanosDeOutraAcademia() throws Exception {
        login("gabriel.souza@example.com", "Gabriel@2026!");
        Response<List<Models.Plan>> r = ApiClient.get().plans(2).execute(); // Move Academia
        assertEquals(403, r.code());
    }

    @Test public void tokenDeixaDeValerDepoisDoLogout() throws Exception {
        login("gabriel.souza@example.com", "Gabriel@2026!");
        String token = ApiClient.getToken();
        assertTrue(ApiClient.get().logout("Bearer " + token).execute().isSuccessful());
        assertEquals(401, ApiClient.get().me().execute().code());
        ApiClient.setToken(null);
    }
}
