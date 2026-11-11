package ai.cardeli.starter.feature.demo

import ai.cardeli.starter.contract.OrderUiState

/**
 * state를 JSON 문자열로 바꾼다. 스모크 테스트가 `adb shell run-as ai.cardeli.starter cat files/demo_state.json`으로 읽는다.
 * org.json은 JVM 단위 테스트에서 못 쓰므로 직접 만든다.
 */
object DemoSnapshot {

    fun toJson(s: OrderUiState): String = buildString {
        append('{')
        append("\"isDriving\":").append(s.isDriving).append(',')
        append("\"status\":").append(str(s.status.name)).append(',')
        append("\"store\":").append(str(s.menus.firstOrNull()?.storeName ?: "")).append(',')
        append("\"cart\":[")
        append(s.cart.joinToString(",") { "{\"id\":${str(it.menu.id)},\"name\":${str(it.menu.name)},\"qty\":${it.quantity}}" })
        append("],")
        append("\"totalPrice\":").append(s.totalPrice).append(',')
        append("\"riderDistanceM\":").append(s.riderDistanceM?.toString() ?: "null")
        append('}')
    }

    private fun str(v: String) = "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
