package ai.cardeli.starter.feature.demo

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.feature.geo.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemoCommandTest {

    private fun parse(vararg pairs: Pair<String, String>) = DemoCommand.parse { key -> pairs.toMap()[key] }

    @Test
    fun `명령 이름으로 명령을 만든다`() {
        assertEquals(DemoCommand.Reset, parse("cmd" to "reset"))
        assertEquals(DemoCommand.Dump, parse("cmd" to "DUMP"))
        assertEquals(DemoCommand.Utter("order_1"), parse("cmd" to "utter", "name" to "order_1"))
        assertEquals(DemoCommand.Event("AddMenu", "s2-2"), parse("cmd" to "event", "name" to "AddMenu", "id" to "s2-2"))
        assertEquals(DemoCommand.Rider(LatLng(37.3851, 127.1256)), parse("cmd" to "rider", "lat" to "37.3851", "lng" to "127.1256"))
    }

    @Test
    fun `값이 빠지거나 모르는 명령이면 null`() {
        assertNull(parse("cmd" to "fly"))
        assertNull(parse("cmd" to "utter"))
        assertNull(parse("cmd" to "rider", "lat" to "37.1"))
        assertNull(parse("cmd" to "rider", "lat" to "abc", "lng" to "127"))
        assertNull(parse())
    }

    @Test
    fun `state를 JSON으로 저장한다`() {
        val m = Menu("s2-2", "서현김밥", "참치김밥", 5_500)
        val json = DemoSnapshot.toJson(OrderUiState(menus = listOf(m), cart = listOf(CartItem(m, 2)), status = OrderStatus.RiderComing, riderDistanceM = 200))
        assertEquals(
            "{\"isDriving\":false,\"status\":\"RiderComing\",\"store\":\"서현김밥\",\"cart\":[{\"id\":\"s2-2\",\"name\":\"참치김밥\",\"qty\":2}],\"totalPrice\":11000,\"riderDistanceM\":200}",
            json,
        )
    }
}
