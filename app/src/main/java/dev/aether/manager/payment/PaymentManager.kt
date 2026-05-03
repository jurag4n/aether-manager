package dev.aether.manager.payment

import android.content.Context
import dev.aether.manager.NativeAether
import dev.aether.manager.license.LicenseManager
import dev.aether.manager.license.LicensePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object PaymentManager {

    private const val POLL_INTERVAL_MS = 5_000L
    private const val POLL_TIMEOUT_MS  = 30 * 60 * 1_000L // 30 menit

    // ── Endpoint URL getters (dari native layer, dengan hardcoded fallback) ───

    private fun createOrderUrl(): String =
        if (NativeAether.isLoaded)
            runCatching { NativeAether.nativeGetCreateOrderUrl() }.getOrNull()
                ?: "https://aether-app-weld.vercel.app/api/payment/create-order"
        else "https://aether-app-weld.vercel.app/api/payment/create-order"

    private fun pollOrderUrl(): String =
        if (NativeAether.isLoaded)
            runCatching { NativeAether.nativeGetPollOrderUrl() }.getOrNull()
                ?: "https://aether-app-weld.vercel.app/api/payment/poll-order"
        else "https://aether-app-weld.vercel.app/api/payment/poll-order"

    // ── Data classes ──────────────────────────────────────────────────────────

    data class PaymentMethod(
        val id:         String,  // "gopay", "dana", "ovo", "bri", dll.
        val label:      String,  // "GoPay", "DANA", "OVO", …
        val type:       String,  // "ewallet" | "bank"
        val number:     String,
        val holderName: String,
    )

    data class OrderInfo(
        val orderId:        String,
        val paymentMethods: List<PaymentMethod>,
        val nominal:        Int,
    )

    sealed class CreateOrderResult {
        data class Success(val order: OrderInfo) : CreateOrderResult()
        data class Error(val message: String)    : CreateOrderResult()
    }

    sealed class PollResult {
        data object Pending : PollResult()
        data class Completed(val licenseKey: String, val expiresAt: Long) : PollResult()
        data class Failed(val status: String)  : PollResult()
        data class Error(val message: String)  : PollResult()
    }

    // ── Create order ──────────────────────────────────────────────────────────

    suspend fun createOrder(
        ctx:   Context,
        name:  String,
        phone: String,
    ): CreateOrderResult = withContext(Dispatchers.IO) {
        try {
            val deviceId = LicenseManager.getDeviceId(ctx)
            val conn = openPost(createOrderUrl())

            val body = JSONObject().apply {
                put("name",     name.trim())
                put("phone",    phone.trim())
                put("deviceId", deviceId)
            }.toString()

            conn.outputStream.use { it.write(body.toByteArray()) }

            val code     = conn.responseCode
            val response = readResponse(conn, code)
            val json     = runCatching { JSONObject(response) }.getOrDefault(JSONObject())

            if (code == 200) {
                // Parse paymentMethods jika ada, fallback ke gopay+dana lama
                val methods = mutableListOf<PaymentMethod>()
                if (json.has("paymentMethods")) {
                    val arr: JSONArray = json.getJSONArray("paymentMethods")
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        methods += PaymentMethod(
                            id         = m.optString("id", ""),
                            label      = m.optString("label", ""),
                            type       = m.optString("type", "ewallet"),
                            number     = m.optString("number", "-"),
                            holderName = m.optString("holderName", "Al** A**** Kh****"),
                        )
                    }
                } else {
                    // backward-compat: server lama hanya kirim gopay & dana
                    val gopay = json.optString("gopay", "-")
                    val dana  = json.optString("dana", "-")
                    if (gopay != "-") methods += PaymentMethod("gopay", "GoPay", "ewallet", gopay, "Al** A**** Kh****")
                    if (dana  != "-") methods += PaymentMethod("dana",  "DANA",  "ewallet", dana,  "Al** A**** Kh****")
                }

                CreateOrderResult.Success(
                    OrderInfo(
                        orderId        = json.getString("orderId"),
                        paymentMethods = methods,
                        nominal        = json.optInt("nominal", 25000),
                    )
                )
            } else {
                CreateOrderResult.Error(json.optString("error", "Gagal membuat order"))
            }
        } catch (e: Exception) {
            CreateOrderResult.Error(e.message ?: "Network error")
        }
    }

    // ── Poll sampai admin konfirmasi ──────────────────────────────────────────

    suspend fun pollUntilDone(
        ctx:     Context,
        orderId: String,
        onPoll:  suspend (PollResult) -> Unit,
    ): PollResult = withContext(Dispatchers.IO) {
        val deviceId  = LicenseManager.getDeviceId(ctx)
        val startTime = System.currentTimeMillis()

        while (true) {
            if (System.currentTimeMillis() - startTime > POLL_TIMEOUT_MS) {
                val r = PollResult.Error("Timeout – belum ada konfirmasi dari admin dalam 30 menit")
                onPoll(r)
                return@withContext r
            }

            val result = checkOrder(orderId, deviceId)
            onPoll(result)

            when (result) {
                is PollResult.Pending -> delay(POLL_INTERVAL_MS)
                else -> {
                    if (result is PollResult.Completed) {
                        LicensePrefs.save(ctx, result.licenseKey, result.expiresAt)
                        InvoicePrefs.updateStatus(ctx, orderId, "paid")
                    }
                    return@withContext result
                }
            }
        }

        @Suppress("UNREACHABLE_CODE")
        PollResult.Error("Unexpected exit")
    }

    // ── Single poll ───────────────────────────────────────────────────────────

    private suspend fun checkOrder(orderId: String, deviceId: String): PollResult =
        withContext(Dispatchers.IO) {
            try {
                val oid  = java.net.URLEncoder.encode(orderId,  "UTF-8")
                val did  = java.net.URLEncoder.encode(deviceId, "UTF-8")
                val conn = (URL("${pollOrderUrl()}?orderId=$oid&deviceId=$did")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod  = "GET"
                    connectTimeout = 8_000
                    readTimeout    = 8_000
                }

                val code     = conn.responseCode
                val response = readResponse(conn, code)
                val json     = runCatching { JSONObject(response) }.getOrDefault(JSONObject())

                when (json.optString("status", "pending")) {
                    "completed" -> PollResult.Completed(
                        licenseKey = json.getString("licenseKey"),
                        expiresAt  = json.getLong("expiresAt"),
                    )
                    "pending"   -> PollResult.Pending
                    else        -> PollResult.Failed(json.optString("status", "unknown"))
                }
            } catch (e: Exception) {
                PollResult.Error(e.message ?: "Network error")
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun openPost(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 15_000
        }

    private fun readResponse(conn: HttpURLConnection, code: Int): String =
        (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText() ?: "{}"
}
