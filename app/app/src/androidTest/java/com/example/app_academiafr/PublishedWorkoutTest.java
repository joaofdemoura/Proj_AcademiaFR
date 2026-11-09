package com.example.app_academiafr;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.app_academiafr.api.ApiClient;
import org.junit.*;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

/** Opt-in: valida a conta criada no web e a ficha persistida, inclusive após reabrir. */
@RunWith(AndroidJUnit4.class)
public class PublishedWorkoutTest {
    private ActivityScenario<MainActivity> scenario;
    private Bundle args;
    private void tap(String text) { onView(allOf(withText(text), isDescendantOfA(isAssignableFrom(ScrollView.class)))).perform(scrollTo(), click()); }
    private void waitFor(String text) throws Exception {
        long until = System.currentTimeMillis() + 15000;
        while (true) {
            try { onView(allOf(withText(text), isDescendantOfA(isAssignableFrom(ScrollView.class)))).perform(scrollTo()).check(matches(isDisplayed())); return; }
            catch (AssertionError | androidx.test.espresso.NoMatchingViewException e) {
                if (System.currentTimeMillis() >= until) throw e;
                Thread.sleep(200);
            }
        }
    }
    @Before public void start() {
        args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue("Ative com ACADEMIA_E2E_EMAIL/PASSWORD/TITLE", args.getString("e2eEmail") != null);
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().clear().commit();
        ApiClient.setToken(null);
        if (Build.VERSION.SDK_INT >= 37) InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(ctx.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        scenario = ActivityScenario.launch(MainActivity.class);
    }
    @After public void finish() throws Exception {
        if (scenario == null) return;
        if (ApiClient.getToken() != null) ApiClient.get().logout("Bearer " + ApiClient.getToken()).execute();
        ApiClient.setToken(null);
        InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().clear().commit();
        scenario.close();
    }
    @Test public void webAccountAndPublishedWorkoutSurviveAppRestart() throws Exception {
        tap("ENTRAR");
        onView(withHint("Digite seu e-mail")).perform(replaceText(args.getString("e2eEmail")), closeSoftKeyboard());
        onView(withHint("Digite sua senha")).perform(replaceText(decoded("e2ePasswordBase64")), closeSoftKeyboard());
        tap("Manter conectado"); tap("Entrar"); waitFor("ACESSAR"); tap("ACESSAR"); tap("TREINO");
        verifyWorkout();
        scenario.close();
        // Simula a perda do estado estático do processo: só a preferência persistida pode restaurar a sessão.
        ApiClient.setToken(null);
        scenario = ActivityScenario.launch(MainActivity.class);
        waitFor("ACESSAR"); tap("ACESSAR"); tap("TREINO"); verifyWorkout();
        tap("INICIAR TREINO"); waitFor("CONTINUAR TREINO");
        tap("ATUALIZAR TREINOS"); verifyWorkout();
    }
    private String decoded(String key) { return new String(android.util.Base64.decode(args.getString(key), android.util.Base64.NO_WRAP), java.nio.charset.StandardCharsets.UTF_8); }
    private void verifyWorkout() throws Exception {
        waitFor(decoded("e2eTitleBase64").toUpperCase(java.util.Locale.ROOT) + " ·\nHIPERTROFIA");
        int count = Integer.parseInt(args.getString("e2eCount", "1"));
        waitFor(count + " EXERCÍCIOS   0/" + count);
        java.util.List<com.example.app_academiafr.api.Models.WorkoutPlan> plans = ApiClient.get().workouts().execute().body();
        assertNotNull(plans);
        com.example.app_academiafr.api.Models.WorkoutPlan plan = plans.stream().filter(p -> decoded("e2eTitleBase64").equals(p.name)).findFirst().orElse(null);
        assertNotNull("Ficha publicada não encontrada na API", plan);
        assertEquals(count, plan.allExercises().size());
        assertEquals(Integer.parseInt(args.getString("e2eSets", "3")), plan.allExercises().get(0).sets);
    }
}
