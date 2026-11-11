package ai.cardeli.starter.feature.sdk

import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * Gleo AI 앱 연동 (F15). assets/schema.json의 ORDER_FOOD를 Gleo가 인텐트로 보낸다.
 * 확인된 형식(docs/gleo_check.md): action = "ai.cardeli.starter.ORDER_FOOD", extra ACTION_PARAM = {"menu":"참치 김밥","quantity":1}.
 * 메뉴 이름은 호출마다 띄어쓰기가 다르게 온다("참치 김밥"/"참치김밥") → OrderParser가 띄어쓰기를 무시한다.
 */
object GleoBridge {

    data class Order(val menu: String, val quantity: Int)

    fun isOrder(intent: Intent?) = intent?.action?.endsWith("ORDER_FOOD") == true

    fun parse(intent: Intent): Order? {
        val raw = intent.getStringExtra("ACTION_PARAM") ?: return null
        return runCatching {
            // 문서 예시처럼 따옴표로 한 번 더 감싸 올 수도 있어 벗긴다
            val t = raw.trim()
            val text = if (t.startsWith("\"") && t.endsWith("\"")) t.substring(1, t.length - 1).replace("\\\"", "\"") else t
            val json = JSONObject(text)
            Order(json.optString("menu"), json.optInt("quantity", 1).coerceAtLeast(1))
        }.onFailure { Log.w(TAG, "ACTION_PARAM 파싱 실패: $raw", it) }.getOrNull()?.takeIf { it.menu.isNotBlank() }
    }

    /** Gleo에 결과를 돌려준다. Gleo가 message를 읽으므로 짧게 쓴다 (주문 대화는 앱이 이어서 한다) */
    fun respond(context: Context, ok: Boolean, message: String) {
        val response = JSONObject().apply {
            put("functionName", "ORDER_FOOD")
            put("status", if (ok) "success" else "asking")
            put("message", message)
        }
        context.sendBroadcast(
            Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
                putExtra("packageName", context.packageName)
                putExtra("response", response.toString())
            },
        )
        Log.i(TAG, "APP_ACTION_RESPONSE $response")
    }

    private const val TAG = "GleoBridge"
}
