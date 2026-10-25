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
        currentBody.addView(action("💳 Pagar com cartão fictício") { pay("Cartão fictício") })
        currentBody.addView(action("💵 Pagar com dinheiro fictício") { pay("Dinheiro fictício") })
        currentBody.addView(action("↩ Esvaziar carrinho") { cart.clear(); saveState(); showCart() })
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
