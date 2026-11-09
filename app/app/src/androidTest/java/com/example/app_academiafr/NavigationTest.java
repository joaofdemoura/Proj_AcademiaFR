package com.example.app_academiafr;

import android.content.Context;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.app_academiafr.api.ApiClient;
import org.junit.*;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

/** Navegação real no emulador; requer API e contas de demonstração. */
@RunWith(AndroidJUnit4.class)
public class NavigationTest {
    private ActivityScenario<MainActivity> scenario;
    @Before public void start() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().clear().commit();
        ApiClient.setToken(null);
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(ctx.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        scenario = ActivityScenario.launch(MainActivity.class);
    }
    @After public void finish() throws Exception {
        if (ApiClient.getToken() != null) ApiClient.get().logout("Bearer " + ApiClient.getToken()).execute();
        ApiClient.setToken(null);
        if (scenario != null) scenario.close();
    }
    private void tap(String text) {
        onView(allOf(withText(text), isDescendantOfA(isAssignableFrom(ScrollView.class)))).perform(scrollTo(), click());
    }
    private void nav(String text) {
        onView(allOf(withText(text), not(isDescendantOfA(isAssignableFrom(ScrollView.class))))).perform(click());
    }
    private void visible(String text) {
        onView(allOf(withText(text), isDescendantOfA(isAssignableFrom(ScrollView.class)))).perform(scrollTo()).check(matches(isDisplayed()));
    }
    private void awaitText(String text) throws Exception {
        long until = System.currentTimeMillis() + 15000;
        while (true) {
            try { visible(text); return; }
            catch (AssertionError | androidx.test.espresso.NoMatchingViewException e) {
                if (System.currentTimeMillis() >= until) throw e;
                Thread.sleep(200);
            }
        }
    }
    private void login() throws Exception {
        tap("ENTRAR");
        onView(withHint("Digite seu e-mail")).perform(replaceText("gabriel.souza@example.com"), closeSoftKeyboard());
        onView(withHint("Digite sua senha")).perform(replaceText("Gabriel@2026!"), closeSoftKeyboard());
        tap("Entrar"); awaitText("PRYME ACADEMIA");
        onView(withText("MOVE ACADEMIA")).check(doesNotExist());
        tap("ACESSAR"); awaitText("BOM DIA,");
    }
    private void awaitPlans() throws Exception {
        java.util.List<com.example.app_academiafr.api.Models.Plan> plans = ApiClient.get().plans(1).execute().body();
        org.junit.Assert.assertNotNull(plans);
        awaitText(plans.isEmpty() ? "Nenhum plano disponível nesta academia no momento."
                : "ESCOLHER " + plans.get(0).name.toUpperCase(java.util.Locale.ROOT));
    }
    @Test public void validationAndBack() {
        tap("ENTRAR"); tap("Entrar");
        onView(withHint("Digite seu e-mail")).check(matches(hasErrorText("Digite seu e-mail")));
        onView(withHint("Digite seu e-mail")).perform(replaceText("aluno@example.com"), closeSoftKeyboard());
        tap("Entrar"); onView(withHint("Digite sua senha")).check(matches(hasErrorText("Digite sua senha")));
        tap("VOLTAR"); visible("TREINE"); tap("VER ACADEMIAS"); visible("BEM-VINDO DE VOLTA");
        pressBack(); visible("TREINE");
    }
    @Test public void allStudentScreensReservationsFiltersBackAndLogout() throws Exception {
        login(); tap("AULAS"); visible("AGENDAR AULAS"); tap("Spinning"); tap("+ RESERVAR");
        visible("✓ RESERVADA · CANCELAR"); scenario.recreate(); tap("Spinning");
        visible("✓ RESERVADA · CANCELAR"); tap("✓ RESERVADA · CANCELAR"); tap("Funcional");
        tap("ENTRAR NA FILA"); visible("✓ NA FILA · CANCELAR"); tap("✓ NA FILA · CANCELAR");
        tap("Yoga"); visible("Yoga · equilíbrio"); tap("Musculação"); visible("Musculação guiada");
        nav("EVOLUÇÃO");
        for (String metric : new String[]{"Medidas", "Força", "Frequência", "Peso"}) tap(metric);
        visible("75.2 KG"); nav("TREINOS"); awaitText("ATUALIZAR TREINOS"); tap("ATUALIZAR TREINOS");
        awaitText("ATUALIZAR TREINOS"); nav("INÍCIO"); tap("PLANOS"); awaitPlans(); pressBack();
        visible("BOM DIA,"); tap("GARANTIR"); visible("PLANOS"); tap("‹"); nav("PERFIL");
        visible("PERFIL"); onView(withText("PAINEL DO GESTOR")).check(doesNotExist());
        tap("MEUS PLANOS"); awaitPlans(); tap("‹"); awaitText("TROCAR ACADEMIA"); tap("TROCAR ACADEMIA");
        visible("ONDE VAMOS TREINAR"); awaitText("ACESSAR"); tap("ACESSAR"); nav("PERFIL"); tap("SAIR");
        visible("TREINE"); pressBackUnconditionally();
    }
}
