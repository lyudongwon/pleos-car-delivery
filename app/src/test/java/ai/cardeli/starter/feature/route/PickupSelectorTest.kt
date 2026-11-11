package ai.cardeli.starter.feature.route

import ai.cardeli.starter.feature.mock.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupSelectorTest {

    private val store = DemoData.s1Stores.first()   // 길목김밥, 조리 10분
    private val sel = PickupSelector().choose(store, DemoData.s1Pickups, DemoData.s1Measures)!!

    @Test
    fun `추가 시간이 가장 작은 경로 위 지점을 고른다`() {
        // p1(길목사거리 정차구역)·p3·p4 모두 +0분, 대기 0분. 가게에서 가장 가까운 p1
        assertEquals("p1", sel.chosen.id)
        assertEquals(0, sel.chosen.detourMin)
        assertEquals(0, sel.chosen.detourM)
    }

    @Test
    fun `대기 시간은 조리 시간과 기사 이동에서 차 도착을 뺀 값`() {
        // 조리 10분 + 기사 약 4.5분(가게→p1 약 1.1km) − 차 도착 17.6분 < 0 → 0분 (차가 음식보다 늦게 온다)
        assertEquals(4.5, sel.chosen.riderMin, 0.2)
        assertEquals(0, sel.chosen.waitMin)
    }

    @Test
    fun `가게 들러 포장 비교값도 같이 나온다`() {
        val t = assertNotNull(sel.takeout).let { sel.takeout!! }
        assertEquals(1_000, t.detourM)
        assertEquals(6, t.detourMin)
        assertTrue(t.detourM > sel.chosen.detourM)
    }

    @Test
    fun `실측값이 없는 후보는 빼고 고른다`() {
        val s = PickupSelector().choose(store, DemoData.s1Pickups, DemoData.s1Measures - "p1")!!
        assertEquals("p4", s.chosen.id)
        assertEquals(4, s.candidates.size)
    }

    @Test
    fun `로그 JSON에 후보 5곳과 선택, 포장 비교가 들어간다`() {
        val json = PickupLog.toJson(store, sel, DemoData.s1Pickups)
        assertTrue(json.contains("\"chosen\":\"p1\""))
        assertEquals(5 + 1, Regex("\"detourMin\"").findAll(json).count())
        assertTrue(json.contains("\"takeout\":{\"id\":\"store-s1-a\""))
    }
}
