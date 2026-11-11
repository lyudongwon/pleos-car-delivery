package ai.cardeli.starter.feature.route

import ai.cardeli.starter.feature.geo.LatLng
import ai.cardeli.starter.feature.mock.DemoData
import ai.cardeli.starter.feature.mock.DemoRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreFinderTest {

    private val finder = StoreFinder(DemoRoute.points, DemoData.s1Stores)

    /** 경로 위, 길목김밥을 지나고 모퉁이김밥 전 (출발점에서 약 2.5km) */
    private val pastA = LatLng(37.395996, 127.106793)

    @Test
    fun `출발점에서는 앞쪽 경로 근처 두 곳을 지나가는 순서로`() {
        val found = finder.find(DemoData.s1Start, "김밥")
        assertEquals(listOf("길목김밥", "모퉁이김밥"), found.map { it.store.name })
        assertTrue(found[0].aheadM < found[1].aheadM)
    }

    @Test
    fun `지나친 가게와 경로에서 먼 가게는 빠진다`() {
        val names = finder.find(DemoData.s1Start, null, limit = 10).map { it.store.name }
        assertTrue("시흥김밥(지나친 방향) $names", "시흥김밥" !in names)
        assertTrue("먼길김밥(1km 떨어짐) $names", "먼길김밥" !in names)
    }

    @Test
    fun `길목김밥을 지나면 모퉁이김밥만 남는다`() {
        assertEquals(listOf("모퉁이김밥"), finder.find(pastA, "김밥").map { it.store.name })
    }

    @Test
    fun `없는 음식 종류면 아무것도 없다`() {
        assertTrue(finder.find(DemoData.s1Start, "국밥").isEmpty())
    }
}
