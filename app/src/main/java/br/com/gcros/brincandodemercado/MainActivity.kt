package br.com.gcros.brincandodemercado

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.Context
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mercado_virtual", Context.MODE_PRIVATE) }
    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var balance = 500.0
    private val cart = linkedMapOf<String, Int>()
    private lateinit var root: LinearLayout
    private lateinit var balanceText: TextView
    private lateinit var cartText: TextView
    private lateinit var currentBody: LinearLayout
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
        showHome()
    }
    private fun saveState() {
        val obj = JSONObject()
        cart.forEach { (key, value) -> obj.put(key, value) }
        prefs.edit().putFloat("balance", balance.toFloat()).putString("cart", obj.toString()).apply()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 16f, color: Int = Color.rgb(32,43,37), bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color); setPadding(dp(4), dp(5), dp(4), dp(5))
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
    private fun action(label: String, callback: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { callback() }
    }
    private fun setup(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(246,248,245))
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        root.addView(label("🛒 BRINCANDO DE MERCADO", 21f, Color.rgb(24,105,67), true))
        root.addView(label(title, 14f, Color.DKGRAY))
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balanceText = label("Saldo: " + money.format(balance), 16f, Color.rgb(24,105,67), true)
        cartText = label("Carrinho: " + cart.values.sum())
        top.addView(balanceText, LinearLayout.LayoutParams(0,-2,1f)); top.addView(cartText); root.addView(top)
        val scroll = ScrollView(this)
        currentBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(currentBody); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(action("🏠 Início / Produtos") { showHome() })
        setContentView(root)
    }
    private fun refreshTop() {
        balanceText.text = "Saldo: " + money.format(balance)
        cartText.text = "Carrinho: " + cart.values.sum()
        saveState()
    }
    private fun showHome() {
        setup("Escolha produtos e pague com dinheiro fictício")
        currentBody.addView(label("Seu mercadinho", 24f, Color.rgb(32,43,37), true))
        currentBody.addView(label("Você começa com R$ 500,00 de brincadeira. Nenhum pagamento real é feito."))
        currentBody.addView(action("🛍️ Ver carrinho (" + cart.values.sum() + " itens)") { showCart() })
        products.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(10),dp(6),dp(10),dp(6)); setBackgroundColor(Color.WHITE)
            }
            card.addView(label(p.emoji + "  " + p.name, 18f, Color.rgb(35,52,42), true))
            card.addView(label(p.category + " • " + money.format(p.price)))
            card.addView(action("＋ Adicionar ao carrinho") {
                cart[p.name] = (cart[p.name] ?: 0) + 1; refreshTop()
                Toast.makeText(this, p.name + " adicionado!", Toast.LENGTH_SHORT).show()
            })
            val lp = LinearLayout.LayoutParams(-1,-2); lp.bottomMargin = dp(9); currentBody.addView(card,lp)
        }
        currentBody.addView(action("🔄 Reiniciar jogo (voltar a R$ 500)") {
            balance = 500.0; cart.clear(); saveState(); showHome()
        })
    }
    private fun total(): Double = cart.entries.sumOf { (name, qty) ->
        (products.firstOrNull { it.name == name }?.price ?: 0.0) * qty
    }
    private fun showCart() {
        setup("Confira a compra antes de pagar")
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
        currentBody.addView(label("🔒 Ambiente de brincadeira: não use dados bancários reais.", 12f, Color.GRAY))
        currentBody.addView(action("↩ Esvaziar carrinho") { cart.clear(); saveState(); showCart() })
    }

    private fun showPaymentOptions() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Adicione produtos antes de pagar.", Toast.LENGTH_SHORT).show()
            return
        }
        setup("Checkout • escolha como deseja pagar")
        currentBody.addView(label("RESUMO DO PEDIDO", 13f, Color.GRAY, true))
        currentBody.addView(label("Mercadinho • ${cart.values.sum()} item(ns)", 18f, Color.rgb(35,52,42), true))
        currentBody.addView(label("Total a pagar", 14f, Color.DKGRAY))
        currentBody.addView(label(money.format(total()), 30f, Color.rgb(24,105,67), true))
        currentBody.addView(label("FORMA DE PAGAMENTO", 13f, Color.GRAY, true))
        currentBody.addView(action("🟢 Pix • pagamento instantâneo") { showPixPayment() })
        currentBody.addView(action("💳 Cartão de crédito") { showCardPayment(true) })
        currentBody.addView(action("💳 Cartão de débito") { showCardPayment(false) })
        currentBody.addView(action("💵 Dinheiro") { showCashPayment() })
        currentBody.addView(label("Todos os pagamentos são simulados. O app não se conecta a bancos, operadoras ou sistemas Pix.", 12f, Color.GRAY))
        currentBody.addView(action("← Voltar ao carrinho") { showCart() })
    }

    private fun showPixPayment() {
        setup("Pix • checkout simulado")
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
        currentBody.addView(label("Código Pix fictício (não é válido para transferência):", 13f, Color.GRAY))
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
        currentBody.addView(label("Este QR e este código são ilustrativos e não movimentam dinheiro.", 12f, Color.rgb(170,60,40)))
        currentBody.addView(action("✅ Simular pagamento Pix") { pay("Pix simulado") })
        currentBody.addView(action("← Escolher outra forma") { showPaymentOptions() })
    }

    private fun showCardPayment(credit: Boolean) {
        setup(if (credit) "Cartão de crédito • simulação" else "Cartão de débito • simulação")
        currentBody.addView(label("💳", 36f, Color.rgb(24,105,67), true))
        currentBody.addView(label(if (credit) "Crédito" else "Débito", 24f, Color.rgb(35,52,42), true))
        currentBody.addView(label("Total: " + money.format(total()), 23f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Use somente dados inventados. Não informe número, validade ou CVV de um cartão real.", 13f, Color.rgb(170,60,40), true))
        val holder = EditText(this).apply {
            hint = "Nome fictício no cartão"
            singleLine = true
            setText("CLIENTE TESTE")
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        currentBody.addView(holder, LinearLayout.LayoutParams(-1, -2))
        val number = EditText(this).apply {
            hint = "Número fictício (ex.: 4242 4242 4242 4242)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("4242424242424242")
            singleLine = true
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
        setup("Dinheiro • caixa")
        currentBody.addView(label("💵 Pagamento em dinheiro", 23f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Total da compra: " + money.format(total()), 24f, Color.rgb(24,105,67), true))
        currentBody.addView(label("Receba o comprovante fictício após confirmar. O valor será descontado do saldo de brincadeira."))
        currentBody.addView(action("✅ Confirmar pagamento em dinheiro") { pay("Dinheiro simulado") })
        currentBody.addView(action("← Escolher outra forma") { showPaymentOptions() })
    }

    private fun pay(method: String) {
        val amount = total()
        if (cart.isEmpty()) {
            Toast.makeText(this,"Adicione produtos antes de pagar.",Toast.LENGTH_SHORT).show(); return
        }
        if (amount > balance) {
            Toast.makeText(this,"Saldo fictício insuficiente! Remova produtos ou reinicie o jogo.",Toast.LENGTH_LONG).show(); return
        }
        balance -= amount; cart.clear(); saveState()
        setup("Compra concluída")
        currentBody.addView(label("✅ PAGAMENTO APROVADO",24f,Color.rgb(24,105,67),true))
        currentBody.addView(label("Forma de pagamento: " + method,18f))
        currentBody.addView(label("Total da compra: " + money.format(amount),22f,Color.rgb(24,105,67),true))
        currentBody.addView(label("Saldo restante: " + money.format(balance),19f))
        currentBody.addView(label("Comprovante fictício: nenhuma transação financeira real foi realizada."))
        currentBody.addView(action("🛍️ Continuar comprando") { showHome() })
    }
}
