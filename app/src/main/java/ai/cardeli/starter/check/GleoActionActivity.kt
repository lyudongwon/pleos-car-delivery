package ai.cardeli.starter.check

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import org.json.JSONObject

/**
 * Gleo AI 앱 연동(App Schema) 시험.
 * Gleo가 assets/schema.json의 ORDER_FOOD를 Intent(action = "ORDER_FOOD", extra "ACTION_PARAM")로 보내면
 * 받은 값을 로그로 찍고, APP_ACTION_RESPONSE 브로드캐스트로 결과를 돌려준다.
 *
 * Gleo 없이 받는 쪽만 시험:
 *   adb shell am start -a ORDER_FOOD -p ai.cardeli.starter --es ACTION_PARAM '{"menu":"참치김밥","quantity":1}'
 * 로그: adb logcat -s GleoCheck
 */
class GleoActionActivity : Activity() {
    private val tag = "GleoCheck"
    private lateinit var out: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply { textSize = 24f; setPadding(40, 40, 40, 40) }
        setContentView(out)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun log(msg: String) {
        Log.i(tag, msg)
        out.append(msg + "\n")
    }

    private fun handle(intent: Intent?) {
        if (intent == null) return
        log("action=${intent.action} from=${referrer}")
        // 문서에는 ACTION_PARAM 하나로 오는 예시와 key마다 putExtra 한다는 설명이 같이 있어서 extra 전부를 찍는다
        intent.extras?.keySet()?.forEach { k -> log("extra $k = ${intent.extras?.get(k)}") }
        // Gleo는 "ai.cardeli.starter.ORDER_FOOD"로 보낸다(문서 예시는 "ORDER_FOOD")
        if (intent.action?.endsWith("ORDER_FOOD") != true) return

        val raw = intent.getStringExtra("ACTION_PARAM")
        var menu = intent.getStringExtra("menu") ?: ""
        var quantity = intent.extras?.get("quantity")?.toString() ?: ""
        if (raw != null) {
            runCatching {
                // 문서 예시처럼 따옴표로 한 번 더 감싸 올 수 있어 벗겨 본다
                val text = raw.trim().let { if (it.startsWith("\"") && it.endsWith("\"")) it.substring(1, it.length - 1).replace("\\\"", "\"") else it }
                val json = JSONObject(text)
                menu = json.optString("menu", menu)
                quantity = json.opt("quantity")?.toString() ?: quantity
            }.onFailure { log("ACTION_PARAM 파싱 실패: $it") }
        }
        log("ORDER_FOOD menu=$menu quantity=$quantity")
        respond(menu, quantity)
    }

    private fun respond(menu: String, quantity: String) {
        val ok = menu.isNotBlank()
        val response = JSONObject().apply {
            put("functionName", "ORDER_FOOD")
            put("status", if (ok) "success" else "asking")
            put("message", if (ok) "The order for $menu, quantity $quantity, has been placed." else "The menu item to order is required.")
            if (ok) put("data", "[{\"menu\":\"$menu\",\"quantity\":\"$quantity\"}]")
        }
        Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
            putExtra("packageName", packageName)
            putExtra("response", response.toString())
        }.also { sendBroadcast(it) }
        log("APP_ACTION_RESPONSE 보냄: $response")
    }
}
