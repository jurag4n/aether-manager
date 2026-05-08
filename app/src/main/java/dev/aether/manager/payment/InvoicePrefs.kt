package dev.aether.manager.payment

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object InvoicePrefs {

    private const val PREFS_NAME   = "aether_invoices"
    private const val KEY_INVOICES = "invoice_list"

    data class Invoice(
        val orderId:        String,
        val name:           String,
        val phone:          String,
        val nominal:        Int,
        val createdAt:      Long,
        val paymentMethods: List<PaymentManager.PaymentMethod>,
        val status:         String,  // "pending" | "paid" | "expired"
        // backward-compat
        val gopay: String = paymentMethods.firstOrNull { it.id == "gopay" }?.number ?: "-",
        val dana:  String = paymentMethods.firstOrNull { it.id == "dana"  }?.number ?: "-",
    ) {
        val dateLabel: String get() =
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(createdAt))
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<Invoice> {
        val raw = prefs(ctx).getString(KEY_INVOICES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                // Parse paymentMethods array if present, else fallback gopay+dana
                val methods = mutableListOf<PaymentManager.PaymentMethod>()
                if (o.has("paymentMethods")) {
                    val ma = o.getJSONArray("paymentMethods")
                    for (j in 0 until ma.length()) {
                        val m = ma.getJSONObject(j)
                        methods += PaymentManager.PaymentMethod(
                            id         = m.optString("id", ""),
                            label      = m.optString("label", ""),
                            type       = m.optString("type", "ewallet"),
                            number     = m.optString("number", "-"),
                            holderName = m.optString("holderName", "Al** A**** Kh****"),
                        )
                    }
                } else {
                    val gopay = o.optString("gopay", "-")
                    val dana  = o.optString("dana", "-")
                    if (gopay != "-") methods += PaymentManager.PaymentMethod("gopay", "GoPay", "ewallet", gopay, "Al** A**** Kh****")
                    if (dana  != "-") methods += PaymentManager.PaymentMethod("dana",  "DANA",  "ewallet", dana,  "Al** A**** Kh****")
                }
                Invoice(
                    orderId        = o.getString("orderId"),
                    name           = o.getString("name"),
                    phone          = o.getString("phone"),
                    nominal        = o.getInt("nominal"),
                    createdAt      = o.getLong("createdAt"),
                    paymentMethods = methods,
                    status         = o.optString("status", "pending"),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun add(ctx: Context, invoice: Invoice) {
        val list = getAll(ctx).toMutableList()
        list.removeAll { it.orderId == invoice.orderId }
        list.add(0, invoice)
        save(ctx, list.take(20))
    }

    fun updateStatus(ctx: Context, orderId: String, status: String) {
        val list = getAll(ctx).map {
            if (it.orderId == orderId) it.copy(status = status) else it
        }
        save(ctx, list)
    }

    fun remove(ctx: Context, orderId: String) {
        save(ctx, getAll(ctx).filter { it.orderId != orderId })
    }

    private fun save(ctx: Context, list: List<Invoice>) {
        val arr = JSONArray()
        list.forEach { inv ->
            val methodsArr = JSONArray()
            inv.paymentMethods.forEach { m ->
                methodsArr.put(JSONObject().apply {
                    put("id",         m.id)
                    put("label",      m.label)
                    put("type",       m.type)
                    put("number",     m.number)
                    put("holderName", m.holderName)
                })
            }
            arr.put(JSONObject().apply {
                put("orderId",        inv.orderId)
                put("name",           inv.name)
                put("phone",          inv.phone)
                put("nominal",        inv.nominal)
                put("createdAt",      inv.createdAt)
                put("paymentMethods", methodsArr)
                put("status",         inv.status)
            })
        }
        prefs(ctx).edit().putString(KEY_INVOICES, arr.toString()).apply()
    }
}
