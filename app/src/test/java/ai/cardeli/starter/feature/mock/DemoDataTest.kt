package ai.cardeli.starter.feature.mock

import ai.cardeli.starter.feature.geo.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시연 데이터가 시나리오 전제(배달 가능 거리 안 앞쪽 가게 2곳, 걸러질 가게 2곳, 9,500원 주문)를 지키는지 확인한다 */
class DemoDataTest {

    private val route = DemoRoute.points
    private val cum = Geo.cumulativeM(route)

    private fun store(id: String) = DemoData.s1Stores.first { it.id == id }

    @Test
    fun `경로는 시연 출발점에서 시작해 목적지 근처에서 끝난다`() {
        assertTrue(Geo.distanceM(route.first(), DemoData.s1Start) < 30)
        assertTrue(Geo.distanceM(route.last(), DemoData.s1Destination) < 30)
        assertTrue("경로 길이 ${cum.last()}", cum.last() in 16_000.0..19_000.0)
    }

    @Test
    fun `가게 A와 B는 경로에서 1~2km 떨어져 있고 출발점보다 앞쪽이다`() {
        // 배달처럼 가게가 경로 바로 옆이 아니어야 한다(너무 가까우면 포장과 차이가 안 난다)
        for (id in listOf("s1-a", "s1-b")) {
            val p = Geo.project(store(id).location, route, cum)
            assertTrue("$id offset ${p.offsetM}", p.offsetM in 950.0..2_000.0)
            assertTrue("$id along ${p.alongM}", p.alongM > 100)
        }
        // 지나가는 순서: A가 B보다 먼저
        assertTrue(Geo.project(store("s1-a").location, route, cum).alongM < Geo.project(store("s1-b").location, route, cum).alongM)
    }

    @Test
    fun `가게 C는 출발점 뒤쪽, 가게 D는 경로에서 2km 넘게 떨어져 있다`() {
        assertTrue(Geo.project(store("s1-c").location, route, cum).alongM < 1.0)
        assertTrue(Geo.project(store("s1-d").location, route, cum).offsetM > 2_000)
    }

    @Test
    fun `수령 후보 5곳은 경로에서 400m 안이고 길목김밥에서 2km 안이다`() {
        assertEquals(5, DemoData.s1Pickups.size)
        for (c in DemoData.s1Pickups) {
            val d = Geo.distanceM(c.location, store("s1-a").location)
            assertTrue("${c.id} 가게까지 $d", d < 2_000)
            assertTrue("${c.id} 경로까지", Geo.project(c.location, route, cum).offsetM < 400)
        }
    }

    @Test
    fun `참치김밥과 어묵국은 9500원이다`() {
        for (s in DemoData.s1Stores + DemoData.s2Store) {
            val total = s.menus.filter { it.name == "참치김밥" || it.name == "어묵국" }.sumOf { it.price }
            assertEquals(s.name, 9_500, total)
        }
    }
}
