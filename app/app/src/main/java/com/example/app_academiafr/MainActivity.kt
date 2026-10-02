package com.example.app_academiafr

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** academiafinal: native screens with local demonstration data. */
class MainActivity : AppCompatActivity() {
    private val ink = Color.rgb(9, 13, 6)
    private val panel = Color.rgb(22, 27, 17)
    private val orange = Color.rgb(237, 112, 29)
    private val muted = Color.rgb(151, 157, 140)
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private var screen = "welcome"
    private var academy = "ACADEMIA MOVE"
    private var userName = "Marina"
    private var day = 15
    private var category = "Todas"
    private var goal = "Hipertrofia"
    private var metric = "Peso"
    private var annual = false
    private var chosenPlan = "TOTAL"
    private var adTitle = "BLACK WEEK FITNESS"
    private var discount = 40
    private var loud = true
    private var audience = "Inativos 30d"
    private var training = false
    private var allAcademies = false
    private var campaignTab = "CAMPANHAS"
    private val reservations = mutableSetOf("15:3")
    private val completed = mutableSetOf<Int>()
    private val history = mutableListOf<String>()
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = savedInstanceState
        if (state != null) {
            screen = state.getString("screen", "welcome"); userName = state.getString("name", "Marina")
            academy = state.getString("academy", "ACADEMIA MOVE")
            reservations.clear(); reservations.addAll(state.getStringArrayList("reservations").orEmpty())
            completed.addAll(state.getIntArray("completed")?.toList().orEmpty())
            history.addAll(state.getStringArrayList("history").orEmpty())
            day = state.getInt("day", 15)
            adTitle = state.getString("adTitle", "BLACK WEEK FITNESS"); discount = state.getInt("discount", 40)
        } else {
            adTitle = getPreferences(MODE_PRIVATE).getString("adTitle", adTitle) ?: adTitle
            discount = getPreferences(MODE_PRIVATE).getInt("discount", 40)
            if (getPreferences(MODE_PRIVATE).getBoolean("remember", false)) {
                userName = getPreferences(MODE_PRIVATE).getString("name", "Marina") ?: "Marina"
                screen = "academies"
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (history.isNotEmpty()) show(history.removeAt(history.lastIndex))
                else if (screen != "welcome") show("welcome") else finish()
            }
        })
        show(screen)
    }
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("screen", screen); out.putString("name", userName); out.putString("academy", academy)
        out.putStringArrayList("reservations", ArrayList(reservations)); out.putIntArray("completed", completed.toIntArray())
        out.putStringArrayList("history", ArrayList(history)); out.putInt("day", day)
        out.putString("adTitle", adTitle); out.putInt("discount", discount)
    }
    private fun go(destination: String) { if (destination != screen) history.add(screen); show(destination) }
    private fun bg(color: Int = panel, radius: Int = 14, stroke: Boolean = false) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke) setStroke(dp(1), orange)
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun text(value: String, size: Int = 14, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-condensed" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun LinearLayout.add(view: View, height: Int = -2, gap: Int = 0) {
        addView(view, LinearLayout.LayoutParams(-1, if (height < 0) height else dp(height)).apply { bottomMargin = dp(gap) })
    }
    private fun space(n: Int = 16) { body.add(View(this), n) }
    private fun label(value: String) { body.add(text(value, 11, muted), gap = 8) }
    private fun title(value: String) { body.add(text(value, 24, bold = true), gap = 16) }
    private fun card(color: Int = panel, border: Boolean = false) = column().apply {
        background = bg(color, 14, border); setPadding(dp(18), dp(18), dp(18), dp(18))
    }
    private fun button(value: String, primary: Boolean = true, action: () -> Unit) = text(value, 13, if (primary) ink else Color.WHITE, true).apply {
        gravity = Gravity.CENTER; background = bg(if (primary) orange else panel, 26)
        minHeight = dp(48); setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener { action() }
    }
    private fun notice(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    private fun header(kicker: String, heading: String, action: String? = null, onAction: () -> Unit = {}) {
        val h = row()
        h.addView(button("‹", false) { onBackPressedDispatcher.onBackPressed() }, LinearLayout.LayoutParams(dp(44), dp(44)))
        val names = column().apply { setPadding(dp(12), 0, 0, 0); add(text(kicker, 10, muted)); add(text(heading, 22, bold = true)) }
        h.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        if (action != null) h.addView(button(action, false, onAction), LinearLayout.LayoutParams(dp(44), dp(44)))
        body.add(h, gap = 20)
    }
    private fun chips(items: List<String>, selected: String, change: (String) -> Unit) {
        val strip = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val r = row()
        items.forEach { item -> r.addView(button(item, item == selected) { change(item) }, LinearLayout.LayoutParams(-2, dp(44)).apply { rightMargin = dp(8) }) }
        strip.addView(r); body.add(strip, gap = 20)
    }
    private fun field(hint: String, value: String = "", password: Boolean = false): EditText = EditText(this).apply {
        setSingleLine(); textSize = 14f; setTextColor(Color.WHITE); setHintTextColor(muted)
        this.hint = hint; setText(value); background = bg(panel, 6); setPadding(dp(14), dp(8), dp(14), dp(8))
        inputType = if (password) 129 else 1; body.add(this, 52, 18)
    }
    private fun show(destination: String) {
        screen = destination
        root = column().apply { setBackgroundColor(ink) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = column().apply { setPadding(dp(22), dp(24), dp(22), dp(24)) }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        when (destination) {
            "welcome" -> welcome(); "login" -> login(); "academies" -> academies()
            "home", "manager" -> home(destination == "manager")
            "classes" -> classes(); "workout" -> workout(); "progress" -> progress()
            "plans" -> plans(); "retention" -> retention(); "ad" -> ad(); "profile" -> profile()
        }
        if (destination in listOf("home", "manager", "classes", "workout", "progress", "profile")) navigation()
    }
    private fun navigation() {
        val nav = row().apply { setPadding(dp(8), dp(10), dp(8), dp(8)); background = bg(ink, 0) }
        listOf(Triple("⌂", "INÍCIO", "home"), Triple("↔", "TREINOS", "workout"), Triple("▣", "AULAS", "classes"), Triple("▥", "EVOLUÇÃO", "progress"), Triple("○", "PERFIL", "profile")).forEach { (icon, name, route) ->
            val c = column().apply { gravity = Gravity.CENTER; contentDescription = name; setOnClickListener { go(route) } }
            c.add(text(icon, 24, if (screen == route) orange else muted).apply { gravity = Gravity.CENTER })
            c.add(text(name, 9, if (screen == route) orange else muted).apply { gravity = Gravity.CENTER })
            nav.addView(c, LinearLayout.LayoutParams(0, dp(58), 1f))
        }; root.add(nav)
    }
    private fun welcome() {
        label("ACADEMIA  /  FITNESS & MOVIMENTO"); space(90)
        body.add(text("TREINE", 58, bold = true)); body.add(text("ONDE", 58, orange, true)); body.add(text("QUISER.", 58, orange, true))
        space(26); body.add(text("Uma plataforma para todas as suas metas. Treinos, aulas e evolução em um só lugar.", 14, muted), gap = 36)
        body.add(button("ENTRAR") { go("login") }, gap = 12)
        body.add(button("VER ACADEMIAS", false) { go("academies") })
    }
    private fun login() {
        label("BEM-VINDO DE VOLTA"); space(30); title("Bem-vindo\nde volta.")
        body.add(text("Entre para acompanhar seus treinos,\naulas e evolução.", 14, muted), gap = 32)
        label("Nome do usuário"); val user = field("Digite seu nome")
        label("Senha"); val pass = field("Digite sua senha", password = true)
        val check = CheckBox(this).apply { text = "Manter conectado"; setTextColor(muted); textSize = 12f }; body.add(check, gap = 24)
        body.add(button("Entrar") {
            if (user.text.isBlank()) user.error = "Digite seu nome"
            else if (pass.text.isBlank()) pass.error = "Digite sua senha"
            else {
                userName = user.text.toString().trim()
                getPreferences(MODE_PRIVATE).edit().putBoolean("remember", check.isChecked).putString("name", userName).apply()
                go("academies")
            }
        }, gap = 24)
        body.add(text("Use o cadastro da sua academia para acessar.", 12, muted)); space()
        body.add(text("Prévia local: preencha nome e senha para explorar as telas.", 11, muted))
    }
    private fun academies() {
        header("ESCOLHA SUA ACADEMIA", "ONDE VAMOS TREINAR")
        listOf("PRYME ACADEMIA", "MOVE ACADEMIA").forEachIndexed { index, item ->
            val c = card()
            c.add(text(if (index == 0) "PRYME▰" else "↔ MOVE", 36, if (index == 0) Color.WHITE else orange, true).apply { gravity = Gravity.CENTER; background = bg(ink, 2) }, 130, 16)
            c.add(text("ACADEMIA", 10, orange)); c.add(text(item, 26, bold = true), gap = 8)
            c.add(text(if (index == 0) "3 unidades   •   Capivari, SC" else "1 unidade   •   Tubarão, SC", 12, muted), gap = 20)
            c.add(button("ACESSAR") { academy = item; go("home") }); body.add(c, gap = 18)
        }
    }
    private fun home(manager: Boolean) {
        val r = row(); r.addView(text("$academy ⌄", 18, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(button(userName.take(2).uppercase()) { go("profile") }, LinearLayout.LayoutParams(dp(48), dp(48))); body.add(r, gap = 24)
        body.add(text("BOM DIA,", 30, bold = true)); body.add(text(userName.uppercase(), 30, orange, true))
        body.add(text("Hoje é dia de treino de pernas · 50 min", 13, muted), gap = 24)
        val streak = card(orange); streak.add(text("SEQUÊNCIA ATUAL", 10, ink, true)); streak.add(text("14 DIAS", 44, ink, true), gap = 8)
        streak.add(text("▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▮ ▯ ▯", 20, ink)); streak.add(text("SEG     TER     QUA     QUI     SEX     SÁB     DOM", 9, ink)); body.add(streak, gap = 20)
        val shortcuts = row()
        listOf("TREINO" to "workout", "AULAS" to "classes", "EVOLUÇÃO" to "progress", "PLANOS" to "plans").forEach { (caption, route) -> shortcuts.addView(button(caption, false) { go(route) }.apply { textSize = 10f; setPadding(dp(2), dp(8), dp(2), dp(8)) }, LinearLayout.LayoutParams(0, dp(64), 1f).apply { marginEnd = dp(6) }) }; body.add(shortcuts, gap = 24)
        title("PROMOÇÕES")
        val offer = card(border = true); offer.add(text("−40% OFF", 10, orange), gap = 8); offer.add(text("PLANO ANUAL\nBLACK WEEK", 30, bold = true), gap = 8)
        offer.add(text("Dê o próximo passo na sua evolução.", 13, muted), gap = 16); offer.add(button("GARANTIR") { go("plans") }); body.add(offer, gap = 24)
        if (manager) { body.add(button("RETENÇÃO DE ALUNOS", false) { go("retention") }, gap = 12); body.add(button("CRIAR ANÚNCIO") { go("ad") }) }
    }
    private fun classes() {
        header("SEMANA · MAIO", "AGENDAR AULAS", "⌕") { notice("Escolha uma modalidade nos filtros") }
        val dates = row(); (13..19).forEach { d -> dates.addView(button(listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM")[d - 13] + "\n$d", d == day) { day = d; show("classes") }.apply { textSize = 11f; setPadding(dp(2), dp(6), dp(2), dp(6)) }, LinearLayout.LayoutParams(0, dp(64), 1f).apply { marginEnd = dp(3) }) }; body.add(dates, gap = 16)
        chips(listOf("Todas", "Musculação", "Spinning", "Funcional", "Yoga"), category) { category = it; show("classes") }
        val names = listOf("Spinning · ride", "Musculação guiada", "HIIT funcional", "Musculação avançada", "Yoga · equilíbrio")
        val types = listOf("Spinning", "Musculação", "Funcional", "Musculação", "Yoga")
        names.forEachIndexed { i, item -> if (category == "Todas" || category == types[i]) {
            val key = "$day:$i"; val reserved = reservations.contains(key); val c = card(if (reserved) Color.rgb(143, 66, 10) else panel)
            c.add(text(listOf("06:30", "08:00", "12:00", "18:30", "19:30")[i] + "   ·   " + types[i].uppercase(), 15, bold = true), gap = 8)
            c.add(text(item, 21, bold = true), gap = 6); c.add(text("50 min   •   " + if (i == 2) "LOTADA · fila de espera" else "${18 - i * 2}/25 vagas", 12, muted), gap = 14)
            c.add(button(if (reserved) "✓ ${if (i == 2) "NA FILA" else "RESERVADA"} · CANCELAR" else if (i == 2) "ENTRAR NA FILA" else "+ RESERVAR", !reserved) {
                if (reserved) reservations.remove(key) else reservations.add(key); show("classes")
                notice(if (reserved) "Reserva cancelada" else "Registrado nesta prévia local")
            }); body.add(c, gap = 12)
        } }
    }
    private fun workout() {
        header("TERÇA · PERNAS", "SEU TREINO")
        chips(listOf("Hipertrofia", "Emagrecer", "Resistência", "Mobilidade"), goal) { goal = it; show("workout") }
        val c = card(); c.add(text("TREINO DE HOJE", 10, orange), gap = 8); c.add(text("PERNAS ·\n${goal.uppercase()}", 30, bold = true), gap = 12)
        c.add(text("50 MIN   420 KCAL   ${completed.size}/5", 20, bold = true), gap = 16)
        c.add(button(if (training) "CONTINUAR TREINO" else "INICIAR TREINO") { training = true; notice("Treino iniciado. Toque nos exercícios para marcar as séries."); show("workout") }); body.add(c, gap = 24)
        title("EXERCÍCIOS")
        listOf("Agachamento livre", "Leg press 45°", "Cadeira extensora", "Stiff com barra", "Panturrilha em pé").forEachIndexed { i, item ->
            val exercise = card(); exercise.add(text("${if (completed.contains(i)) "✓" else i + 1}   $item   ›", 18, bold = true), gap = 6)
            exercise.add(text("3 × 12   •   ${listOf(60, 120, 38, 40, 30)[i]} kg   •   60s", 12, muted))
            exercise.setOnClickListener { androidx.appcompat.app.AlertDialog.Builder(this).setTitle(item).setMessage("3 séries de 12 repetições. Descanse 60 segundos entre as séries.")
                .setPositiveButton(if (completed.contains(i)) "Desmarcar" else "Concluir exercício") { _, _ -> if (!completed.add(i)) completed.remove(i); show("workout") }.setNegativeButton("Voltar", null).show() }
            body.add(exercise, gap = 10)
        }
    }
    private fun progress() {
        header("ÚLTIMA AVALIAÇÃO", "EVOLUÇÃO")
        listOf(listOf("PESO ATUAL" to "75.2 kg", "FREQUÊNCIA" to "92%"), listOf("MASSA MAGRA" to "58.4 kg", "% GORDURA" to "18.2%")).forEach { stats ->
            val r = row(); stats.forEach { (caption, value) -> val c = card(); c.add(text(caption, 10, muted)); c.add(text(value, 30, bold = true)); r.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) }) }; body.add(r, gap = 12)
        }; space()
        chips(listOf("Peso", "Medidas", "Força", "Frequência"), metric) { metric = it; show("progress") }
        val chart = card(); chart.add(text("${metric.uppercase()} · EVOLUÇÃO", 11, muted)); chart.add(text(when(metric) { "Peso" -> "75.2 KG"; "Medidas" -> "82 CM"; "Força" -> "60 KG"; else -> "92%" }, 26, bold = true), gap = 24)
        val bars = row().apply { gravity = Gravity.BOTTOM }
        val heights = if (metric in listOf("Força", "Frequência")) listOf(40, 65, 75, 100, 110, 125, 140) else listOf(140, 120, 100, 85, 70, 65, 45)
        heights.forEachIndexed { i, h -> val bar = column(); bar.add(View(this).apply { background = bg(if (i == 6) orange else Color.rgb(39, 46, 31), 5) }, h, 8); bar.add(text("0${i + 1}", 10, muted).apply { gravity = Gravity.CENTER }); bars.addView(bar, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) }) }
        chart.add(bars, 180, 16); chart.add(text("Acompanhamento das últimas 7 avaliações", 11, muted)); body.add(chart)
    }
    private fun plans() {
        header("ESCOLHA E ASSINE", "PLANOS")
        label("ACADEMIA"); chips(listOf("Atual", "Todas"), if (allAcademies) "Todas" else "Atual") { allAcademies = it == "Todas"; show("plans") }
        chips(listOf("MENSAL", "ANUAL"), if (annual) "ANUAL" else "MENSAL") { annual = it == "ANUAL"; show("plans") }
        listOf("ESSENCIAL", "TOTAL").forEachIndexed { i, item ->
            val c = card(border = item == chosenPlan); if (i == 1) c.add(text("MAIS VENDIDO", 11, orange, true), gap = 10)
            c.add(text(if (i == 0 && !allAcademies) academy else "TODAS AS UNIDADES", 10, muted)); c.add(text(item + if (item == chosenPlan) "   ◉" else "   ○", 28, bold = true), gap = 10)
            c.add(text("R$ ${if (i == 0) if (annual) 79 else 89 else if (annual) 129 else 149} /mês", 32, bold = true), gap = 16)
            c.add(text("✓ Musculação livre\n✓ ${if (i == 0) "2 aulas/semana" else "Aulas ilimitadas"}\n✓ App + treinos\n✓ Avaliação trimestral" + if (i == 1) "\n✓ Acesso a todas as academias" else "", 14, muted))
            c.setOnClickListener { chosenPlan = item; show("plans") }; body.add(c, gap = 16)
        }
        body.add(button("ESCOLHER $chosenPlan") { androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Plano $chosenPlan").setMessage("Plano selecionado nesta prévia. A contratação depende da integração com o serviço da academia.").setPositiveButton("Entendi", null).show() })
    }
    private fun retention() {
        header("ALUNOS INATIVOS", "RETENÇÃO", "+") { go("ad") }
        val c = card(); c.add(text("ALUNOS INATIVOS · 30 DIAS", 11, muted)); c.add(text("342               64%", 32, bold = true)); body.add(c, gap = 24)
        chips(listOf("CAMPANHAS", "ENVIADAS", "AGENDADAS"), campaignTab) { campaignTab = it; show("retention") }
        val campaigns = mutableListOf("Sua vaga te espera", "50% off para voltar", "Volte ao seu ritmo")
        getPreferences(MODE_PRIVATE).getString("published", null)?.let { campaigns.add(0, it) }
        if (campaignTab == "AGENDADAS") body.add(text("Nenhuma campanha agendada.", 14, muted), gap = 24)
        else campaigns.forEach { item -> val campaign = card(); campaign.add(text("◉   $item", 20, bold = true), gap = 8); campaign.add(text("ATIVA · Incentivo para alunos inativos", 12, muted), gap = 16); campaign.add(text("342 enviados   ·   182 respostas", 12, muted)); body.add(campaign, gap = 12) }
        body.add(button("NOVA CAMPANHA") { go("ad") })
    }
    private fun ad() {
        header("PAINEL DO GESTOR", "CRIAR ANÚNCIO"); label("PRÉ-VISUALIZAÇÃO")
        val preview = card(if (loud) orange else panel, !loud); val previewColor = if (loud) ink else Color.WHITE
        val offer = text("PRYME                       −$discount%", 20, previewColor, true); preview.add(offer, gap = 24)
        val headline = text(adTitle, 32, previewColor, true); preview.add(headline, gap = 12)
        preview.add(text("Para quem quer ir além. Promoção válida até 30/11.", 13, previewColor), gap = 18)
        preview.add(button("QUERO ESSA", false) { notice("Pré-visualização do anúncio") }); body.add(preview, gap = 22)
        label("TEMPLATE"); chips(listOf("Minimalista", "Chamativo"), if (loud) "Chamativo" else "Minimalista") { loud = it == "Chamativo"; show("ad") }
        label("TÍTULO"); val input = field("Título do anúncio", adTitle)
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { adTitle = s.toString(); headline.text = adTitle }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val discountLabel = text("DESCONTO · $discount%", 11, muted); body.add(discountLabel)
        body.add(SeekBar(this).apply { max = 80; progress = discount; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { discount = progress; discountLabel.text = "DESCONTO · $discount%"; offer.text = "PRYME                       −$discount%" }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}; override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }) }, gap = 18)
        label("PÚBLICO-ALVO"); chips(listOf("Inativos 30d", "Inativos 60d"), audience) { audience = it; show("ad") }
        body.add(button("SALVAR RASCUNHO", false) { saveDraft(); notice("Rascunho salvo no dispositivo") }, gap = 12)
        body.add(button("PUBLICAR") {
            if (adTitle.isBlank()) input.error = "Informe o título"
            else { saveDraft(); getPreferences(MODE_PRIVATE).edit().putString("published", adTitle).apply(); campaignTab = "CAMPANHAS"; notice("Anúncio registrado na prévia local"); go("retention") }
        })
    }
    private fun saveDraft() { getPreferences(MODE_PRIVATE).edit().putString("adTitle", adTitle).putInt("discount", discount).apply() }
    private fun profile() {
        header("SUA CONTA", "PERFIL")
        val c = card(); c.add(text(userName.take(2).uppercase(), 42, orange, true), gap = 12); c.add(text(userName, 26, bold = true)); c.add(text(academy, 12, muted)); body.add(c, gap = 24)
        body.add(button("MEUS PLANOS", false) { go("plans") }, gap = 12)
        body.add(button("TROCAR ACADEMIA", false) { go("academies") }, gap = 12)
        body.add(button("PAINEL DO GESTOR", false) { go("manager") }, gap = 12)
        body.add(button("RETENÇÃO DE ALUNOS", false) { go("retention") }, gap = 24)
        body.add(button("SAIR") { getPreferences(MODE_PRIVATE).edit().putBoolean("remember", false).apply(); history.clear(); show("welcome") })
    }
}
