package com.example.app_academiafr;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_academiafr.api.ApiClient;
import com.example.app_academiafr.api.Models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Telas nativas do app. Login, academias, treino, planos e perfil usam a API
 * Laravel; aulas, evolução, retenção e anúncios ainda são demonstração local.
 */
public class MainActivity extends AppCompatActivity {
    private final int ink = Color.rgb(9, 13, 6);
    private final int panel = Color.rgb(22, 27, 17);
    private final int orange = Color.rgb(237, 112, 29);
    private final int muted = Color.rgb(151, 157, 140);
    private LinearLayout root, body;
    private String screen = "welcome", academy = "", userName = "";
    private String category = "Todas", metric = "Peso";
    private String adTitle = "BLACK WEEK FITNESS", audience = "Inativos 30d", campaignTab = "CAMPANHAS";
    private int day = 15, discount = 40, selectedWorkout;
    private long gymId, chosenPlanId;
    private boolean loud = true, training, signingIn;
    private final Set<String> reservations = new HashSet<>(Arrays.asList("15:3"));
    private final Set<Integer> completed = new HashSet<>();
    private final ArrayList<String> history = new ArrayList<>();

    // Dados vindos da API (null = ainda não carregado).
    private Models.User user;
    private List<Models.WorkoutPlan> workoutPlans;
    private List<Models.Membership> memberships;
    private List<Models.Plan> gymPlans;
    private final Set<String> loading = new HashSet<>();
    private final Map<String, String> loadErrors = new HashMap<>();

    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            screen = savedInstanceState.getString("screen", "welcome");
            userName = savedInstanceState.getString("name", "");
            academy = savedInstanceState.getString("academy", "");
            gymId = savedInstanceState.getLong("gymId", 0);
            reservations.clear();
            ArrayList<String> savedReservations = savedInstanceState.getStringArrayList("reservations");
            if (savedReservations != null) reservations.addAll(savedReservations);
            int[] savedCompleted = savedInstanceState.getIntArray("completed");
            if (savedCompleted != null) for (int i : savedCompleted) completed.add(i);
            ArrayList<String> savedHistory = savedInstanceState.getStringArrayList("history");
            if (savedHistory != null) history.addAll(savedHistory);
            day = savedInstanceState.getInt("day", 15);
            adTitle = savedInstanceState.getString("adTitle", adTitle);
            discount = savedInstanceState.getInt("discount", 40);
            if (ApiClient.getToken() == null && !screen.equals("welcome") && !screen.equals("login")) screen = "welcome";
        } else {
            adTitle = getPreferences(MODE_PRIVATE).getString("adTitle", adTitle);
            discount = getPreferences(MODE_PRIVATE).getInt("discount", 40);
            String savedToken = getPreferences(MODE_PRIVATE).getString("token", null);
            if (getPreferences(MODE_PRIVATE).getBoolean("remember", false) && savedToken != null) {
                ApiClient.setToken(savedToken);
                userName = getPreferences(MODE_PRIVATE).getString("name", "");
                screen = "academies";
            }
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!history.isEmpty()) show(history.remove(history.size() - 1));
                else if (!screen.equals("welcome")) show("welcome");
                else finish();
            }
        });
        show(screen);
    }

    @Override public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("screen", screen); out.putString("name", userName); out.putString("academy", academy); out.putLong("gymId", gymId);
        out.putStringArrayList("reservations", new ArrayList<>(reservations));
        out.putIntArray("completed", completed.stream().mapToInt(Integer::intValue).toArray());
        out.putStringArrayList("history", history); out.putInt("day", day);
        out.putString("adTitle", adTitle); out.putInt("discount", discount);
    }

    private void go(String destination) { if (!destination.equals(screen)) history.add(screen); show(destination); }
    private GradientDrawable bg(int color, int radius, boolean stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius));
        if (stroke) shape.setStroke(dp(1), orange); return shape;
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Typeface.create(bold ? "sans-serif-condensed" : "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setLineSpacing(dp(3), 1f); return v;
    }
    private TextView text(String value, int size) { return text(value, size, Color.WHITE, false); }
    private TextView text(String value, int size, int color) { return text(value, size, color, false); }
    private TextView bold(String value, int size) { return text(value, size, Color.WHITE, true); }
    private void add(LinearLayout parent, View view, int height, int gap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, height < 0 ? height : dp(height));
        p.bottomMargin = dp(gap); parent.addView(view, p);
    }
    private void add(LinearLayout parent, View view, int gap) { add(parent, view, -2, gap); }
    private void add(LinearLayout parent, View view) { add(parent, view, -2, 0); }
    private void space(int n) { add(body, new View(this), n, 0); }
    private void label(String value) { add(body, text(value, 11, muted), 8); }
    private void title(String value) { add(body, bold(value, 24), 16); }
    private LinearLayout card(int color, boolean border) {
        LinearLayout c = column(); c.setBackground(bg(color, 14, border)); c.setPadding(dp(18), dp(18), dp(18), dp(18)); return c;
    }
    private LinearLayout card() { return card(panel, false); }
    private TextView button(String value, boolean primary, Runnable action) {
        TextView v = text(value, 13, primary ? ink : Color.WHITE, true);
        v.setGravity(Gravity.CENTER); v.setBackground(bg(primary ? orange : panel, 26, false));
        v.setMinHeight(dp(48)); v.setPadding(dp(12), dp(10), dp(12), dp(10)); v.setOnClickListener(w -> action.run()); return v;
    }
    private TextView button(String value, Runnable action) { return button(value, true, action); }
    private void notice(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private void header(String kicker, String heading) { header(kicker, heading, null, () -> {}); }
    private void header(String kicker, String heading, String action, Runnable onAction) {
        LinearLayout h = row(); h.addView(button("‹", false, () -> getOnBackPressedDispatcher().onBackPressed()), new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout names = column(); names.setPadding(dp(12), 0, 0, 0); add(names, text(kicker, 10, muted)); add(names, bold(heading, 22));
        h.addView(names, new LinearLayout.LayoutParams(0, -2, 1f));
        if (action != null) h.addView(button(action, false, onAction), new LinearLayout.LayoutParams(dp(44), dp(44)));
        add(body, h, 20);
    }
    private void chips(List<String> items, String selected, Consumer<String> change) {
        HorizontalScrollView strip = new HorizontalScrollView(this); strip.setHorizontalScrollBarEnabled(false);
        LinearLayout r = row();
        for (String item : items) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(44)); p.rightMargin = dp(8);
            r.addView(button(item, item.equals(selected), () -> change.accept(item)), p);
        }
        strip.addView(r); add(body, strip, 20);
    }
    private EditText field(String hint, String value, boolean password) {
        EditText v = new EditText(this); v.setSingleLine(true); v.setTextSize(14); v.setTextColor(Color.WHITE); v.setHintTextColor(muted);
        v.setHint(hint); v.setText(value); v.setBackground(bg(panel, 6, false)); v.setPadding(dp(14), dp(8), dp(14), dp(8));
        v.setInputType(password ? 129 : 1); add(body, v, 52, 18); return v;
    }
    private EditText field(String hint) { return field(hint, "", false); }

    // ------------------------------------------------------------ API

    /**
     * Garante que um dado da API está carregado. Se ainda não está, mostra
     * "Carregando…" (ou o erro com botão de tentar de novo), dispara a chamada
     * e redesenha a tela quando a resposta chega. Retorna true se já há dados.
     */
    private <T> boolean ensure(String key, T current, Supplier<Call<T>> call, Consumer<T> store, boolean showStatus) {
        if (current != null) return true;
        String error = loadErrors.get(key);
        if (error != null) {
            if (showStatus) {
                LinearLayout c = card(); add(c, text(error, 14, muted), 16);
                add(c, button("TENTAR DE NOVO", () -> { loadErrors.remove(key); show(screen); })); add(body, c, 16);
            }
            return false;
        }
        if (showStatus) add(body, text("Carregando…", 14, muted), 16);
        if (loading.add(key)) {
            call.get().enqueue(new Callback<T>() {
                @Override public void onResponse(Call<T> c, Response<T> r) {
                    loading.remove(key);
                    if (r.code() == 401) { sessionExpired(); return; }
                    if (r.isSuccessful() && r.body() != null) store.accept(r.body());
                    else loadErrors.put(key, ApiClient.errorMessage(r, "Não foi possível carregar. Tente novamente."));
                    if (!isFinishing()) show(screen);
                }
                @Override public void onFailure(Call<T> c, Throwable t) {
                    loading.remove(key); loadErrors.put(key, ApiClient.OFFLINE);
                    if (!isFinishing()) show(screen);
                }
            });
        }
        return false;
    }

    private void clearSession() {
        ApiClient.setToken(null);
        getPreferences(MODE_PRIVATE).edit().putBoolean("remember", false).remove("token").apply();
        user = null; workoutPlans = null; memberships = null; gymPlans = null; gymId = 0; academy = "";
        loadErrors.clear(); completed.clear(); history.clear();
    }

    private void sessionExpired() {
        if (ApiClient.getToken() == null) return;
        clearSession(); notice("Sua sessão expirou. Entre novamente."); show("login");
    }

    // ------------------------------------------------------------ telas

    private void show(String destination) {
        screen = destination; root = column(); root.setBackgroundColor(ink);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        body = column(); body.setPadding(dp(22), dp(24), dp(22), dp(24));
        scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root);
        switch (destination) {
            case "welcome": welcome(); break; case "login": login(); break; case "academies": academies(); break;
            case "home": case "manager": home(destination.equals("manager")); break;
            case "classes": classes(); break; case "workout": workout(); break; case "progress": progress(); break;
            case "plans": plans(); break; case "retention": retention(); break; case "ad": ad(); break; case "profile": profile(); break;
        }
        if (Arrays.asList("home", "manager", "classes", "workout", "progress", "profile").contains(destination)) navigation();
    }
    private void navigation() {
        LinearLayout nav = row(); nav.setPadding(dp(8), dp(10), dp(8), dp(8)); nav.setBackground(bg(ink, 0, false));
        String[][] tabs = {{"⌂","INÍCIO","home"},{"↔","TREINOS","workout"},{"▣","AULAS","classes"},{"▥","EVOLUÇÃO","progress"},{"○","PERFIL","profile"}};
        for (String[] tab : tabs) {
            LinearLayout c = column(); c.setGravity(Gravity.CENTER); c.setContentDescription(tab[1]); c.setOnClickListener(v -> go(tab[2]));
            TextView icon = text(tab[0], 24, screen.equals(tab[2]) ? orange : muted); icon.setGravity(Gravity.CENTER); add(c, icon);
            TextView name = text(tab[1], 9, screen.equals(tab[2]) ? orange : muted); name.setGravity(Gravity.CENTER); add(c, name);
            nav.addView(c, new LinearLayout.LayoutParams(0, dp(58), 1f));
        }
        add(root, nav);
    }
    private void welcome() {
        label("ACADEMIA  /  FITNESS & MOVIMENTO"); space(90);
        add(body, bold("TREINE", 58)); add(body, text("ONDE", 58, orange, true)); add(body, text("QUISER.", 58, orange, true));
        space(26); add(body, text("Uma plataforma para todas as suas metas. Treinos, aulas e evolução em um só lugar.", 14, muted), 36);
        add(body, button("ENTRAR", () -> go(ApiClient.getToken() == null ? "login" : "academies")), 12);
        add(body, button("VER ACADEMIAS", false, () -> go(ApiClient.getToken() == null ? "login" : "academies")));
    }
    private void login() {
        label("BEM-VINDO DE VOLTA"); space(30); title("Bem-vindo\nde volta.");
        add(body, text("Entre para acompanhar seus treinos,\naulas e evolução.", 14, muted), 32);
        label("E-mail"); EditText email = field("Digite seu e-mail");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        label("Senha"); EditText pass = field("Digite sua senha", "", true);
        CheckBox check = new CheckBox(this); check.setText("Manter conectado"); check.setTextColor(muted); check.setTextSize(12); add(body, check, 24);
        TextView enter = button(signingIn ? "Entrando…" : "Entrar", () -> {});
        enter.setOnClickListener(v -> {
            String login = email.getText().toString().trim(), password = pass.getText().toString();
            if (login.isEmpty()) { email.setError("Digite seu e-mail"); return; }
            if (password.isEmpty()) { pass.setError("Digite sua senha"); return; }
            if (signingIn) return;
            signingIn = true; enter.setText("Entrando…");
            ApiClient.get().login(new Models.LoginRequest(login, password, "android " + Build.MODEL)).enqueue(new Callback<Models.LoginResponse>() {
                @Override public void onResponse(Call<Models.LoginResponse> c, Response<Models.LoginResponse> r) {
                    signingIn = false; enter.setText("Entrar");
                    if (!r.isSuccessful() || r.body() == null || r.body().token == null) {
                        String message = r.code() == 429 ? "Muitas tentativas. Aguarde um minuto e tente novamente."
                                : r.code() == 422 || r.code() == 401 ? ApiClient.errorMessage(r, "E-mail ou senha incorretos.")
                                : ApiClient.errorMessage(r, "Não foi possível entrar. Tente novamente.");
                        pass.setError(message); notice(message); return;
                    }
                    ApiClient.setToken(r.body().token);
                    user = r.body().user; userName = user.firstName();
                    workoutPlans = null; memberships = null; gymPlans = null; loadErrors.clear(); completed.clear();
                    if (check.isChecked()) getPreferences(MODE_PRIVATE).edit().putBoolean("remember", true).putString("token", r.body().token).putString("name", userName).apply();
                    else getPreferences(MODE_PRIVATE).edit().putBoolean("remember", false).remove("token").apply();
                    go("academies");
                }
                @Override public void onFailure(Call<Models.LoginResponse> c, Throwable t) {
                    signingIn = false; enter.setText("Entrar"); notice(ApiClient.OFFLINE);
                }
            });
        });
        add(body, enter, 24);
        add(body, text("Use o e-mail e a senha cadastrados na sua academia.", 12, muted));
    }
    private void academies() {
        header("ESCOLHA SUA ACADEMIA", "ONDE VAMOS TREINAR");
        if (!ensure("me", user, () -> ApiClient.get().me(), u -> { user = u; userName = u.firstName(); }, true)) return;
        List<Models.Gym> gyms = new ArrayList<>();
        for (Models.Gym g : user.gyms) if (!"inactive".equals(g.status)) gyms.add(g);
        if (gyms.isEmpty()) {
            LinearLayout c = card(); add(c, bold("Nenhuma academia ativa", 20), 8);
            add(c, text("Sua conta ainda não está ligada a uma academia. Fale com a recepção.", 13, muted)); add(body, c, 16);
            add(body, button("SAIR", false, this::signOut));
            return;
        }
        for (Models.Gym g : gyms) {
            boolean pryme = g.name.toUpperCase().contains("PRYME");
            LinearLayout c = card();
            TextView logo = text(pryme ? "PRYME▰" : "↔ " + g.name.toUpperCase().replace("ACADEMIA", "").trim(), 36, pryme ? Color.WHITE : orange, true);
            logo.setGravity(Gravity.CENTER); logo.setBackground(bg(ink, 2, false)); add(c, logo, 130, 16);
            add(c, text("ACADEMIA", 10, orange)); add(c, bold(g.name.toUpperCase(), 26), 8);
            add(c, text(g.roles.contains("student") ? "Você é aluno desta academia" : "Seu acesso nesta academia", 12, muted), 20);
            add(c, button("ACESSAR", () -> {
                if (gymId != g.id) { gymPlans = null; loadErrors.remove("plans"); }
                academy = g.name.toUpperCase(); gymId = g.id; go("home");
            }));
            add(body, c, 18);
        }
    }
    private Models.WorkoutPlan currentWorkout() {
        if (workoutPlans == null || workoutPlans.isEmpty()) return null;
        return workoutPlans.get(Math.min(selectedWorkout, workoutPlans.size() - 1));
    }
    private void home(boolean manager) {
        LinearLayout r = row(); r.addView(bold(academy + " ⌄", 18), new LinearLayout.LayoutParams(0, -2, 1f));
        r.getChildAt(0).setOnClickListener(v -> go("academies"));
        r.addView(button(initials(), () -> go("profile")), new LinearLayout.LayoutParams(dp(48), dp(48)));
        add(body, r, 24); add(body, bold("BOM DIA,", 30)); add(body, text(userName.toUpperCase(), 30, orange, true));
        ensure("workouts", workoutPlans, () -> ApiClient.get().workouts(), w -> workoutPlans = w, false);
        Models.WorkoutPlan plan = currentWorkout();
        String today = loadErrors.containsKey("workouts") ? "Não foi possível carregar sua ficha."
                : workoutPlans == null ? "Carregando sua ficha…"
                : plan == null ? "Nenhuma ficha publicada ainda."
                : "Sua ficha: " + plan.name + (plan.scheduleDescription != null ? " · " + plan.scheduleDescription : "");
        add(body, text(today, 13, muted), 24);
        LinearLayout streak = card(orange, false); add(streak, text("SEQUÊNCIA ATUAL", 10, ink, true)); add(streak, text("14 DIAS", 44, ink, true), 8);
        add(streak, text("▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▯ ▯", 20, ink)); add(streak, text("SEG     TER     QUA     QUI     SEX     SÁB     DOM", 9, ink)); add(body, streak, 20);
        LinearLayout shortcuts = row(); String[][] links = {{"TREINO","workout"},{"AULAS","classes"},{"EVOLUÇÃO","progress"},{"PLANOS","plans"}};
        for (String[] link : links) {
            TextView b = button(link[0], false, () -> go(link[1])); b.setTextSize(10); b.setPadding(dp(2),dp(8),dp(2),dp(8));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(64), 1f); p.rightMargin = dp(6); shortcuts.addView(b,p);
        }
        add(body, shortcuts, 24); title("PROMOÇÕES");
        LinearLayout offer = card(panel, true); add(offer, text("−40% OFF", 10, orange), 8); add(offer, bold("PLANO ANUAL\nBLACK WEEK", 30), 8);
        add(offer, text("Dê o próximo passo na sua evolução.", 13, muted), 16); add(offer, button("GARANTIR", () -> go("plans"))); add(body, offer, 24);
        if (manager) { add(body, button("RETENÇÃO DE ALUNOS", false, () -> go("retention")), 12); add(body, button("CRIAR ANÚNCIO", () -> go("ad"))); }
    }
    private String initials() {
        String n = userName.trim();
        return n.isEmpty() ? "EU" : n.substring(0, Math.min(2, n.length())).toUpperCase();
    }
    private void classes() {
        header("SEMANA · MAIO", "AGENDAR AULAS", "⌕", () -> notice("Escolha uma modalidade nos filtros"));
        LinearLayout dates = row(); String[] weekdays = {"SEG","TER","QUA","QUI","SEX","SÁB","DOM"};
        for (int d = 13; d <= 19; d++) {
            final int chosenDay = d; TextView b = button(weekdays[d-13] + "\n" + d, d == day, () -> { day = chosenDay; show("classes"); });
            b.setTextSize(11); b.setPadding(dp(2),dp(6),dp(2),dp(6)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,dp(64),1f); p.rightMargin=dp(3); dates.addView(b,p);
        }
        add(body, dates, 16);
        chips(Arrays.asList("Todas","Musculação","Spinning","Funcional","Yoga"), category, value -> { category=value; show("classes"); });
        String[] names = {"Spinning · ride","Musculação guiada","HIIT funcional","Musculação avançada","Yoga · equilíbrio"};
        String[] types = {"Spinning","Musculação","Funcional","Musculação","Yoga"};
        String[] hours = {"06:30","08:00","12:00","18:30","19:30"};
        for (int i=0;i<names.length;i++) {
            if (!category.equals("Todas") && !category.equals(types[i])) continue;
            final int index=i; String key=day+":"+i; boolean reserved=reservations.contains(key);
            LinearLayout c=card(reserved ? Color.rgb(143,66,10) : panel,false);
            add(c,bold(hours[i]+"   ·   "+types[i].toUpperCase(),15),8); add(c,bold(names[i],21),6);
            add(c,text("50 min   •   "+(i==2 ? "LOTADA · fila de espera" : (18-i*2)+"/25 vagas"),12,muted),14);
            String label = reserved ? "✓ "+(i==2?"NA FILA":"RESERVADA")+" · CANCELAR" : (i==2?"ENTRAR NA FILA":"+ RESERVAR");
            add(c,button(label,!reserved,()->{ if(reserved) reservations.remove(key); else reservations.add(key); show("classes"); notice(reserved?"Reserva cancelada":"Registrado nesta prévia local"); }));
            add(body,c,12);
        }
    }
    private void workout() {
        Models.WorkoutPlan current = currentWorkout();
        header(current != null && current.scheduleDescription != null ? current.scheduleDescription.toUpperCase() : "SUA FICHA", "SEU TREINO");
        if (!ensure("workouts", workoutPlans, () -> ApiClient.get().workouts(), w -> workoutPlans = w, true)) return;
        if (workoutPlans.isEmpty()) {
            LinearLayout c = card(); add(c, bold("Nenhuma ficha publicada", 20), 8);
            add(c, text("Quando seu professor publicar uma ficha no painel, ela aparece aqui.", 13, muted), 16);
            add(c, button("ATUALIZAR", false, () -> { workoutPlans = null; show("workout"); })); add(body, c);
            return;
        }
        Models.WorkoutPlan plan = currentWorkout();
        if (workoutPlans.size() > 1) {
            List<String> names = new ArrayList<>(); for (Models.WorkoutPlan p : workoutPlans) names.add(p.name);
            chips(names, plan.name, value -> { selectedWorkout = names.indexOf(value); completed.clear(); show("workout"); });
        }
        List<Models.WorkoutExercise> exercises = plan.allExercises();
        LinearLayout c=card(); add(c,text("TREINO DE HOJE",10,orange),8);
        add(c,bold(plan.name.toUpperCase()+(plan.goal!=null?" ·\n"+plan.goal.toUpperCase():""),30),12);
        add(c,bold(exercises.size()+" EXERCÍCIOS   "+completed.size()+"/"+exercises.size(),20),16);
        add(c,button(training?"CONTINUAR TREINO":"INICIAR TREINO",()->{training=true;notice("Treino iniciado. Toque nos exercícios para marcar as séries.");show("workout");}));
        add(body,c,24); title("EXERCÍCIOS");
        for(int i=0;i<exercises.size();i++) {
            final int index=i; Models.WorkoutExercise e=exercises.get(i);
            String item=e.exercise!=null?e.exercise.name:"Exercício "+(i+1);
            int rest=e.restSeconds!=null?e.restSeconds:0;
            String weight=e.targetWeightKg!=null?"   •   "+e.targetWeightKg.replaceAll("\\.?0+$","")+" kg":"";
            LinearLayout ex=card();
            add(ex,bold((completed.contains(i)?"✓":String.valueOf(i+1))+"   "+item+"   ›",18),6);
            add(ex,text(e.sets+" × "+e.reps()+weight+"   •   "+rest+"s",12,muted));
            String message=e.sets+" séries de "+e.reps()+" repetições. Descanse "+rest+" segundos entre as séries."
                    +(e.notes!=null&&!e.notes.isEmpty()?"\n\n"+e.notes:"");
            ex.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(item).setMessage(message)
                .setPositiveButton(completed.contains(index)?"Desmarcar":"Concluir exercício",(dialog,which)->{
                    if(!completed.add(index)) completed.remove(index); show("workout");
                }).setNegativeButton("Voltar",null).show()); add(body,ex,10);
        }
    }
    private void progress() {
        header("ÚLTIMA AVALIAÇÃO","EVOLUÇÃO");
        String[][][] stats={{{"PESO ATUAL","75.2 kg"},{"FREQUÊNCIA","92%"}},{{"MASSA MAGRA","58.4 kg"},{"% GORDURA","18.2%"}}};
        for(String[][] pair:stats) {
            LinearLayout r=row(); for(String[] stat:pair) { LinearLayout c=card(); add(c,text(stat[0],10,muted)); add(c,bold(stat[1],30));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f); p.rightMargin=dp(8); r.addView(c,p); }
            add(body,r,12);
        }
        space(16); chips(Arrays.asList("Peso","Medidas","Força","Frequência"),metric,value->{metric=value;show("progress");});
        LinearLayout chart=card(); add(chart,text(metric.toUpperCase()+" · EVOLUÇÃO",11,muted));
        String reading=metric.equals("Peso")?"75.2 KG":metric.equals("Medidas")?"82 CM":metric.equals("Força")?"60 KG":"92%";
        add(chart,bold(reading,26),24); LinearLayout bars=row(); bars.setGravity(Gravity.BOTTOM);
        int[] heights=(metric.equals("Força")||metric.equals("Frequência"))?new int[]{40,65,75,100,110,125,140}:new int[]{140,120,100,85,70,65,45};
        for(int i=0;i<heights.length;i++) {
            LinearLayout bar=column(); View fill=new View(this); fill.setBackground(bg(i==6?orange:Color.rgb(39,46,31),5,false)); add(bar,fill,heights[i],8);
            TextView number=text("0"+(i+1),10,muted); number.setGravity(Gravity.CENTER); add(bar,number);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f); p.rightMargin=dp(8); bars.addView(bar,p);
        }
        add(chart,bars,180,16); add(chart,text("Acompanhamento das últimas 7 avaliações",11,muted)); add(body,chart);
    }
    private void plans() {
        header("ESCOLHA E ASSINE","PLANOS");
        if (gymId == 0) {
            add(body, text("Escolha uma academia para ver os planos.", 14, muted), 16);
            add(body, button("ESCOLHER ACADEMIA", () -> go("academies"))); return;
        }
        label(academy);
        if (!ensure("plans", gymPlans, () -> ApiClient.get().plans(gymId), p -> gymPlans = p, true)) return;
        if (gymPlans.isEmpty()) { add(body, text("Nenhum plano disponível nesta academia no momento.", 14, muted)); return; }
        Models.Plan chosen = null;
        for (Models.Plan p : gymPlans) if (p.id == chosenPlanId) chosen = p;
        if (chosen == null) { chosen = gymPlans.get(0); chosenPlanId = chosen.id; }
        for (Models.Plan p : gymPlans) {
            boolean selected = p.id == chosenPlanId;
            LinearLayout c=card(panel,selected);
            add(c,text("annual".equals(p.billingPeriod)?"PLANO ANUAL":"PLANO MENSAL",10,muted));
            add(c,bold(p.name.toUpperCase()+(selected?"   ◉":"   ○"),28),10);
            add(c,bold(ApiClient.money(p.price)+("annual".equals(p.billingPeriod)?" /ano":" /mês"),32),16);
            StringBuilder features = new StringBuilder();
            for (Models.PlanFeature f : p.features) features.append(features.length() > 0 ? "\n" : "").append("✓ ").append(f.description);
            if (features.length() > 0) add(c,text(features.toString(),14,muted));
            c.setOnClickListener(v->{chosenPlanId=p.id;show("plans");}); add(body,c,16);
        }
        String chosenName = chosen.name.toUpperCase();
        add(body,button("ESCOLHER "+chosenName,()->new AlertDialog.Builder(this).setTitle("Plano "+chosenName)
            .setMessage("Para contratar ou trocar de plano, fale com a recepção da academia.")
            .setPositiveButton("Entendi",null).show()));
    }
    private void retention() {
        header("ALUNOS INATIVOS","RETENÇÃO","+",()->go("ad"));
        LinearLayout c=card(); add(c,text("ALUNOS INATIVOS · 30 DIAS",11,muted)); add(c,bold("342               64%",32)); add(body,c,24);
        chips(Arrays.asList("CAMPANHAS","ENVIADAS","AGENDADAS"),campaignTab,value->{campaignTab=value;show("retention");});
        List<String> campaigns=new ArrayList<>(Arrays.asList("Sua vaga te espera","50% off para voltar","Volte ao seu ritmo"));
        String published=getPreferences(MODE_PRIVATE).getString("published",null); if(published!=null) campaigns.add(0,published);
        if(campaignTab.equals("AGENDADAS")) add(body,text("Nenhuma campanha agendada.",14,muted),24);
        else for(String item:campaigns) { LinearLayout campaign=card(); add(campaign,bold("◉   "+item,20),8);
            add(campaign,text("ATIVA · Incentivo para alunos inativos",12,muted),16);
            add(campaign,text("342 enviados   ·   182 respostas",12,muted)); add(body,campaign,12); }
        add(body,button("NOVA CAMPANHA",()->go("ad")));
    }
    private void ad() {
        header("PAINEL DO GESTOR","CRIAR ANÚNCIO"); label("PRÉ-VISUALIZAÇÃO");
        LinearLayout preview=card(loud?orange:panel,!loud); int previewColor=loud?ink:Color.WHITE;
        TextView offer=text("PRYME                       −"+discount+"%",20,previewColor,true); add(preview,offer,24);
        TextView headline=text(adTitle,32,previewColor,true); add(preview,headline,12);
        add(preview,text("Para quem quer ir além. Promoção válida até 30/11.",13,previewColor),18);
        add(preview,button("QUERO ESSA",false,()->notice("Pré-visualização do anúncio"))); add(body,preview,22);
        label("TEMPLATE"); chips(Arrays.asList("Minimalista","Chamativo"),loud?"Chamativo":"Minimalista",value->{loud=value.equals("Chamativo");show("ad");});
        label("TÍTULO"); EditText input=field("Título do anúncio",adTitle,false);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {adTitle=s.toString();headline.setText(adTitle);}
            @Override public void afterTextChanged(Editable s) {}
        });
        TextView discountLabel=text("DESCONTO · "+discount+"%",11,muted); add(body,discountLabel);
        SeekBar seek=new SeekBar(this); seek.setMax(80); seek.setProgress(discount);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser) {
                discount=progress;discountLabel.setText("DESCONTO · "+discount+"%");offer.setText("PRYME                       −"+discount+"%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        }); add(body,seek,18);
        label("PÚBLICO-ALVO"); chips(Arrays.asList("Inativos 30d","Inativos 60d"),audience,value->{audience=value;show("ad");});
        add(body,button("SALVAR RASCUNHO",false,()->{saveDraft();notice("Rascunho salvo no dispositivo");}),12);
        add(body,button("PUBLICAR",()->{
            if(adTitle.trim().isEmpty()) input.setError("Informe o título");
            else {saveDraft();getPreferences(MODE_PRIVATE).edit().putString("published",adTitle).apply();
                campaignTab="CAMPANHAS";notice("Anúncio registrado na prévia local");go("retention");}
        }));
    }
    private void saveDraft() {getPreferences(MODE_PRIVATE).edit().putString("adTitle",adTitle).putInt("discount",discount).apply();}
    private boolean isManager() {
        if (user == null) return false;
        if (user.isGlobalAdmin) return true;
        for (Models.Gym g : user.gyms) if (g.roles.contains("admin")) return true;
        return false;
    }
    private static String membershipStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "active": return "Ativo"; case "pending": return "Pendente";
            case "expired": return "Vencido"; case "cancelled": return "Cancelado"; default: return status;
        }
    }
    private void profile() {
        header("SUA CONTA","PERFIL"); LinearLayout c=card();
        add(c,text(initials(),42,orange,true),12);
        add(c,bold(user!=null?user.name:userName,26));
        if (user!=null && user.email!=null) add(c,text(user.email,12,muted));
        add(c,text(academy,12,muted)); add(body,c,16);
        if (ensure("memberships", memberships, () -> ApiClient.get().memberships(), m -> memberships = m, true)) {
            Models.Membership current = null;
            for (Models.Membership m : memberships) if (m.gym != null && m.gym.id == gymId) { current = m; break; }
            if (current == null && !memberships.isEmpty()) current = memberships.get(0);
            LinearLayout plan=card(); add(plan,text("MEU PLANO",10,orange),8);
            if (current == null || current.plan == null) add(plan,text("Você ainda não tem um plano ativo.",14,muted));
            else {
                add(plan,bold(current.plan.name.toUpperCase(),24),6);
                add(plan,text(membershipStatus(current.status)+"   •   vence em "+ApiClient.date(current.endsOn),13,muted));
            }
            add(body,plan,24);
        }
        add(body,button("MEUS PLANOS",false,()->go("plans")),12);
        add(body,button("TROCAR ACADEMIA",false,()->go("academies")),12);
        if (isManager()) {
            add(body,button("PAINEL DO GESTOR",false,()->go("manager")),12);
            add(body,button("RETENÇÃO DE ALUNOS",false,()->go("retention")),12);
        }
        space(12);
        add(body,button("SAIR",this::signOut));
    }
    private void signOut() {
        String token = ApiClient.getToken();
        if (token != null) {
            ApiClient.get().logout("Bearer " + token).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        }
        clearSession(); show("welcome");
    }
}
