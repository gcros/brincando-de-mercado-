package br.com.gcros.brincandodemercado

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.Context
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.*
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mercado_virtual", Context.MODE_PRIVATE) }
    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var balance = 500.0
    private val cart = linkedMapOf<String, Int>()
    private var currentScreen = "home"
    private val screenHistory = mutableListOf<String>()
    private lateinit var root: LinearLayout
    private lateinit var balanceText: TextView
    private lateinit var cartText: TextView
    private lateinit var currentBody: LinearLayout
    private lateinit var bagButton: Button
    private var toneGenerator: ToneGenerator? = null
    data class Product(val name: String, val price: Double, val emoji: String, val category: String)
    private val products = listOf(
        Product("Arroz 5 kg", 26.90, "🍚", "Mercearia"), Product("Feijão 1 kg", 8.49, "🫘", "Mercearia"),
        Product("Leite 1 L", 5.29, "🥛", "Laticínios"), Product("Pão francês", 1.00, "🥖", "Padaria"),
        Product("Ovos (dúzia)", 12.90, "🥚", "Hortifruti"), Product("Banana (kg)", 6.99, "🍌", "Hortifruti"),
        Product("Maçã (kg)", 8.99, "🍎", "Hortifruti"), Product("Café 500 g", 18.50, "☕", "Mercearia"),
        Product("Óleo 900 ml", 7.49, "🫗", "Mercearia"), Product("Refrigerante 2 L", 9.90, "🥤", "Bebidas"),
        Product("Sabão em pó", 14.90, "🧼", "Limpeza"), Product("Chocolate", 6.50, "🍫", "Doces")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balance = prefs.getFloat("balance", 500f).toDouble()
        val saved = JSONObject(prefs.getString("cart", "{}") ?: "{}")
        saved.keys().forEach { key -> cart[key] = saved.optInt(key, 0) }
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        showHome()
    }
    override fun onDestroy() {
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }
    private fun playSound(tone: Int = ToneGenerator.TONE_PROP_BEEP) {
        try { toneGenerator?.startTone(tone, 120) } catch (_: Exception) { }
    }
    private fun saveState() {
        val obj = JSONObject()
        cart.forEach { (key, value) -> obj.put(key, value) }
        prefs.edit().putFloat("balance", balance.toFloat()).putString("cart", obj.toString()).apply()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float = 16f, color: Int = Color.rgb(32,43,37), bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(dp(5), dp(6), dp(5), dp(6))
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
    private fun action(label: String, callback: () -> Unit) = Button(this).apply {
        text = label
        textSize = 15f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(20, 133, 83)))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        minHeight = dp(46)
        setOnClickListener { callback() }
    }
    private fun setup(title: String, screenKey: String = "home") {
        if (screenKey != currentScreen) {
            screenHistory.add(currentScreen)
            currentScreen = screenKey
        }
        window.statusBarColor = Color.rgb(16, 67, 48)
        window.navigationBarColor = Color.rgb(16, 67, 48)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(244, 247, 242))
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnApplyWindowInsetsListener { view, insets ->
                @Suppress("DEPRECATION")
                val topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                val bottomInset = insets.systemWindowInsetBottom
                view.setPadding(dp(14), dp(8) + topInset, dp(14), dp(8) + bottomInset)
                insets
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(Color.rgb(16, 67, 48), 22)
            elevation = dp(3).toFloat()
        }
        header.addView(label("🛒  BRINCANDO DE MERCADO", 19f, Color.WHITE, true))
        header.addView(label(title, 13f, Color.rgb(213, 235, 221)))
        if (screenKey != "home" && screenKey != "processing") {
            val back = Button(this).apply {
                text = "←  VOLTAR"
                textSize = 13f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(20, 133, 83)))
                setPadding(dp(12), dp(5), dp(12), dp(5))
                minHeight = dp(40)
                setOnClickListener { goBack() }
            }
            val backLp = LinearLayout.LayoutParams(-2, -2)
            backLp.topMargin = dp(6)
            header.addView(back, backLp)
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = rounded(Color.rgb(28, 88, 64), 14)
        }
        balanceText = label("💰 " + money.format(balance), 16f, Color.WHITE, true)
        cartText = label("🧺 " + cart.values.sum() + " itens", 14f, Color.rgb(220, 244, 229), true)
        top.addView(balanceText, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(cartText)
        val topLp = LinearLayout.LayoutParams(-1, -2)
        topLp.topMargin = dp(8)
        header.addView(top, topLp)
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, dp(10), 0, dp(8))
            overScrollMode = ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        currentBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(1), dp(2), dp(1), dp(8))
        }
        scroll.addView(currentBody)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        bagButton = Button(this).apply {
            text = "🛍️  SACOLA  •  ${cart.values.sum()} itens  •  ${money.format(total())}"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackground(rounded(Color.rgb(20, 133, 83), 18))
            setPadding(dp(12), dp(13), dp(12), dp(13))
            minHeight = dp(56)
            elevation = dp(5).toFloat()
            setOnClickListener { playSound(ToneGenerator.TONE_PROP_ACK); showCart() }
        }
        val bagLp = LinearLayout.LayoutParams(-1, -2)
        bagLp.topMargin = dp(6)
        bagLp.bottomMargin = dp(2)
        root.addView(bagButton, bagLp)
        setContentView(root)
        root.requestApplyInsets()
    }
    private fun refreshTop() {
        balanceText.text = "Saldo: " + money.format(balance)
        cartText.text = "Carrinho: " + cart.values.sum()
        if (::bagButton.isInitialized) bagButton.text = "🛍️  SACOLA  •  ${cart.values.sum()} itens  •  ${money.format(total())}"
        saveState()
    }
    private fun showHome() {
        screenHistory.clear()
        setup("Escolha produtos e pague com dinheiro fictício", "home")
        currentBody.addView(label("O que vamos levar hoje? 🛍️", 23f, Color.rgb(22, 65, 46), true))
        currentBody.addView(label("Toque em qualquer produto para ver os detalhes.", 14f, Color.rgb(94, 108, 98)))
        products.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(12))
                background = rounded(Color.WHITE, 18)
                elevation = dp(2).toFloat()
                isClickable = true
                isFocusable = true
            }
            val mainRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            val emojiBadge = TextView(this).apply {
                text = p.emoji
                textSize = 32f
                gravity = Gravity.CENTER
                setBackground(rounded(Color.rgb(238, 246, 239), 15))
            }
            mainRow.addView(emojiBadge, LinearLayout.LayoutParams(dp(66), dp(66)))
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }
            details.addView(label(p.name, 17f, Color.rgb(35, 52, 42), true))
            details.addView(label(p.category, 12f, Color.rgb(115, 128, 118)))
            details.addView(label(money.format(p.price), 19f, Color.rgb(18, 133, 83), true))
            mainRow.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(mainRow)
            card.addView(action("＋  VER PRODUTO E ADICIONAR") { showProductPreview(p) })
            card.setOnClickListener { showProductPreview(p) }
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = dp(12)
            currentBody.addView(card, lp)
        }
        currentBody.addView(action("🔄 Reiniciar jogo (voltar a R$ 500)") {
            balance = 500.0; cart.clear(); saveState(); showHome()
        })
    }

    private fun showProductPreview(p: Product) {
        playSound(ToneGenerator.TONE_PROP_BEEP)
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(26), dp(24), dp(24))
            setBackgroundColor(Color.rgb(250, 252, 249))
        }
        val emoji = label(p.emoji, 76f, Color.BLACK, true).apply { gravity = Gravity.CENTER }
        overlay.addView(emoji, LinearLayout.LayoutParams(-1, dp(130)))
        overlay.addView(label(p.name, 25f, Color.rgb(25, 75, 48), true).apply { gravity = Gravity.CENTER })
        overlay.addView(label(p.category.uppercase(Locale("pt", "BR")), 12f, Color.GRAY, true).apply { gravity = Gravity.CENTER })
        overlay.addView(label(money.format(p.price), 31f, Color.rgb(18, 133, 83), true).apply { gravity = Gravity.CENTER })
        overlay.addView(label("Vai para a sua sacola!", 14f, Color.DKGRAY).apply { gravity = Gravity.CENTER })
        val dialog = android.app.Dialog(this)
        val add = Button(this).apply {
            text = "🛍️  ADICIONAR À SACOLA"
            textSize = 17f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(18, 133, 83)))
            setPadding(dp(10), dp(14), dp(10), dp(14))
            setOnClickListener {
                cart[p.name] = (cart[p.name] ?: 0) + 1
                refreshTop()
                playSound(ToneGenerator.TONE_PROP_ACK)
                dialog.dismiss()
                Toast.makeText(this@MainActivity, p.name + " adicionado à sacola", Toast.LENGTH_SHORT).show()
                val pulse = android.view.animation.ScaleAnimation(1f, 1.08f, 1f, 1.08f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f).apply {
                    duration = 140
                    repeatCount = 1
                    repeatMode = android.view.animation.Animation.REVERSE
                }
                bagButton.startAnimation(pulse)
            }
        }
        val addLp = LinearLayout.LayoutParams(-1, -2)
        addLp.topMargin = dp(18)
        overlay.addView(add, addLp)
        overlay.addView(action("Continuar comprando") { dialog.dismiss() })
        dialog.setContentView(overlay)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), -2)
        overlay.scaleX = 0.72f
        overlay.scaleY = 0.72f
        overlay.alpha = 0.3f
        overlay.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(240)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun goBack() {
        if (currentScreen == "processing") {
            Toast.makeText(this, "Aguarde a conclusão da simulação do pagamento.", Toast.LENGTH_SHORT).show()
            return
        }
        val previous = if (screenHistory.isNotEmpty()) screenHistory.removeAt(screenHistory.lastIndex) else "home"
        currentScreen = previous
        when (previous) {
            "cart" -> showCart()
            "options" -> showPaymentOptions()
            "pix" -> showPixPayment()
            "credit" -> showCardPayment(true)
            "debit" -> showCardPayment(false)
            "cash" -> showCashPayment()
            "receipt", "home" -> showHome()
            else -> showHome()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentScreen == "home") {
            super.onBackPressed()
        } else {
            goBack()
        }
    }

    private fun total(): Double = cart.entries.sumOf { (name, qty) ->
        (products.firstOrNull { it.name == name }?.price ?: 0.0) * qty
    }
    private fun showCart() {
        setup("Confira a compra antes de pagar", "cart")
        currentBody.addView(label("🧺 Seu carrinho", 23f, Color.rgb(24,105,67), true))
        if (cart.isEmpty()) currentBody.addView(label("O carrinho está vazio. Volte aos produtos e adicione alguma coisa."))
        cart.toMap().forEach { (name, qty) ->
            val p = products.firstOrNull { it.name == name } ?: return@forEach
            val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            line.addView(label(name + "\n" + money.format(p.price) + " cada",15f), LinearLayout.LayoutParams(0,-2,1f))
            line.addView(action("−") {
                val n = (cart[name] ?: 0) - 1
                if (n <= 0) cart.remove(name) else cart[name] = n
                saveState(); showCart()
            })
            line.addView(label(" $qty "))
            line.addView(action("＋") { cart[name] = qty + 1; saveState(); showCart() })
            currentBody.addView(line)
        }
        currentBody.addView(label("TOTAL: " + money.format(total()), 24f, Color.rgb(24,105,67), true))
        currentBody.addView(action("💳 Ir para pagamento") { showPaymentOptions() })
        currentBody.addView(action("↩ Esvaziar carrinho") { cart.clear(); saveState(); showCart() })
    }

    private fun showPaymentOptions() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Adicione produtos antes de pagar.", Toast.LENGTH_SHORT).show()
            return
        }
        setup("Checkout • escolha como deseja pagar", "options")
        currentBody.addView(label("RESUMO DO PEDIDO", 13f, Color.GRAY, true))
        currentBody.addView(label("Mercadinho • ${cart.values.sum()} item(ns)", 18f, Color.rgb(35,52,42), true))
        currentBody.addView(label("Total a pagar", 14f, Color.DKGRAY))
        currentBody.addView(label(money.format(total()), 30f, Color.rgb(24,105,67), true))
        currentBody.addView(label("FORMA DE PAGAMENTO", 13f, Color.GRAY, true))
        currentBody.addView(action("🟢 Pix • pagamento instantâneo") { showPixPayment() })
        currentBody.addView(action("💳 Cartão de crédito") { showCardPayment(true) })
        currentBody.addView(action("💳 Cartão de débito") { showCardPayment(false) })
        currentBody.addView(action("💵 Dinheiro") { showCashPayment() })
        currentBody.addView(label("CRISTO REI PAY  •  Terminal de pagamento", 12f, Color.GRAY))
        currentBody.addView(action("← Voltar ao carrinho") { showCart() })
    }

    private fun showPixPayment() {
        setup("Pix • checkout simulado", "pix")
        val pixCode = "00020126330014BR.GOV.BCB.PIX0111MERCADOFAKE5204000053039865802BR5920BRINCANDO DE MERCADO6009SANTANA RS62070503***6304FAKE"
        currentBody.addView(label("🟢 Pague com Pix", 24f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Total: " + money.format(total()), 25f, Color.rgb(24,105,67), true))
        currentBody.addView(label("QR CODE DE DEMONSTRAÇÃO", 13f, Color.GRAY, true))
        val qr = TextView(this).apply {
            text = "▟▀▚▀▞▚▀▟\n▚ ▄ ▞ ▄ ▚\n▞▚▀▚▞▀▚▞\n▟ ▄ ▟ ▄ ▟\n▚▀▞▚▀▞▀▚\n▞ ▄ ▚ ▄ ▞\n▟▀▚▀▞▚▀▟"
            textSize = 25f
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        currentBody.addView(qr, LinearLayout.LayoutParams(-1, -2))
        currentBody.addView(label("Código Pix", 13f, Color.GRAY))
        val code = EditText(this).apply {
            setText(pixCode)
            textSize = 12f
            isFocusable = false
            isClickable = true
            setTextIsSelectable(true)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        currentBody.addView(code, LinearLayout.LayoutParams(-1, -2))
        currentBody.addView(action("✅ Simular pagamento Pix") { pay("Pix simulado") })
        currentBody.addView(action("← Escolher outra forma") { showPaymentOptions() })
    }

    private fun showCardPayment(credit: Boolean) {
        setup(if (credit) "Cartão de crédito • simulação" else "Cartão de débito • simulação", if (credit) "credit" else "debit")
        currentBody.addView(label("💳", 36f, Color.rgb(24,105,67), true))
        currentBody.addView(label(if (credit) "Crédito" else "Débito", 24f, Color.rgb(35,52,42), true))
        currentBody.addView(label("Total: " + money.format(total()), 23f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Dados do cartão", 13f, Color.GRAY))
        val holder = EditText(this).apply {
            hint = "Nome fictício no cartão"
            maxLines = 1
            setText("CLIENTE TESTE")
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        currentBody.addView(holder, LinearLayout.LayoutParams(-1, -2))
        val number = EditText(this).apply {
            hint = "Número fictício (ex.: 4242 4242 4242 4242)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("4242424242424242")
            maxLines = 1
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        val numberLp = LinearLayout.LayoutParams(-1, -2)
        numberLp.topMargin = dp(8)
        currentBody.addView(number, numberLp)
        if (credit) {
            val installments = Spinner(this)
            val options = listOf("1x sem juros", "2x sem juros", "3x sem juros")
            installments.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = dp(8)
            currentBody.addView(installments, lp)
            currentBody.addView(label("Parcelas apenas demonstrativas; o saldo do jogo será debitado uma única vez.", 12f, Color.GRAY))
        }
        currentBody.addView(action("🔐 Autorizar pagamento fictício") {
            val digits = number.text.toString().filter { it.isDigit() }
            if (holder.text.toString().isBlank() || digits.length < 12) {
                Toast.makeText(this, "Preencha os dados fictícios de demonstração.", Toast.LENGTH_LONG).show()
            } else {
                pay(if (credit) "Cartão de crédito simulado" else "Cartão de débito simulado")
            }
        })
        currentBody.addView(action("← Escolher outra forma") { showPaymentOptions() })
    }

    private fun showCashPayment() {
        setup("Dinheiro • caixa", "cash")
        currentBody.addView(label("💵 Pagamento em dinheiro", 23f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Total da compra: " + money.format(total()), 24f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Total da compra: " + money.format(total())))
        currentBody.addView(action("✅ Confirmar pagamento em dinheiro") { pay("Dinheiro simulado") })
        currentBody.addView(action("← Escolher outra forma") { showPaymentOptions() })
    }

    private fun pay(method: String) {
        val amount = total()
        playSound(ToneGenerator.TONE_PROP_BEEP)
        if (cart.isEmpty()) {
            Toast.makeText(this, "Adicione produtos antes de pagar.", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount > balance) {
            Toast.makeText(this, "Saldo fictício insuficiente! Remova produtos ou reinicie o jogo.", Toast.LENGTH_LONG).show()
            return
        }

        // Terminal de demonstração: nenhuma rede bancária ou transferência real é acionada.
        setup("Terminal de pagamento • SIMULAÇÃO", "processing")
        root.setBackgroundColor(Color.rgb(20, 27, 31))
        currentBody.setPadding(dp(8), dp(10), dp(8), dp(10))
        currentBody.addView(label("CRISTO REI PAY", 15f, Color.rgb(111, 226, 177), true))
        currentBody.addView(label("APROXIME • INSIRA • PAGUE", 12f, Color.LTGRAY, true))
        val terminal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(22), dp(18), dp(22))
            setBackgroundColor(Color.rgb(34, 45, 50))
        }
        terminal.addView(label("VALOR DA COMPRA", 13f, Color.LTGRAY, true).apply { gravity = Gravity.CENTER })
        terminal.addView(label(money.format(amount), 32f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        terminal.addView(label(method.uppercase(Locale("pt", "BR")), 14f, Color.rgb(111, 226, 177), true).apply { gravity = Gravity.CENTER })
        val status = label("Conectando ao terminal...", 16f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        terminal.addView(status)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 5
        }
        val progressLp = LinearLayout.LayoutParams(-1, dp(12))
        progressLp.topMargin = dp(16)
        progressLp.bottomMargin = dp(12)
        terminal.addView(progress, progressLp)
        terminal.addView(label("Não remova o cartão nem feche a tela", 12f, Color.LTGRAY).apply { gravity = Gravity.CENTER })
        currentBody.addView(terminal, LinearLayout.LayoutParams(-1, -2))
        action("Processando...") {}.also { it.isEnabled = false; currentBody.addView(it) }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val stages = listOf(
            700L to (22 to "Iniciando terminal seguro..."),
            1500L to (48 to "Validando dados fictícios..."),
            2400L to (73 to "Aguardando confirmação simulada..."),
            3300L to (92 to "Finalizando operação de demonstração..."),
            4100L to (100 to "Pagamento aprovado na simulação!")
        )
        stages.forEach { (delay, stage) ->
            handler.postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                progress.progress = stage.first
                status.text = stage.second
                if (stage.first == 100) {
                    playSound(ToneGenerator.TONE_PROP_ACK)
                    balance -= amount
                    cart.clear()
                    saveState()
                    setup("Comprovante • terminal", "receipt")
                    root.setBackgroundColor(Color.rgb(20, 27, 31))
                    currentBody.addView(label("✓  APROVADO", 28f, Color.rgb(111, 226, 177), true))
                    currentBody.addView(label("CRISTO REI PAY  •  COMPROVANTE", 14f, Color.LTGRAY, true))
                    currentBody.addView(label("Forma: " + method, 17f, Color.WHITE))
                    currentBody.addView(label("Total: " + money.format(amount), 25f, Color.rgb(111, 226, 177), true))
                    currentBody.addView(label("Saldo fictício restante: " + money.format(balance), 17f, Color.WHITE))
                    currentBody.addView(label("Operação demonstrativa nº " + (System.currentTimeMillis() % 1000000), 12f, Color.LTGRAY))
                    currentBody.addView(action("🛍️ Nova compra") { showHome() })
                }
            }, delay)
        }
    }
}
