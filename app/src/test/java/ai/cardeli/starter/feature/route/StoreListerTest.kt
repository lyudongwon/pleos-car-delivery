package ai.cardeli.starter.feature.route

import ai.cardeli.starter.contract.ChargingInfo
import ai.cardeli.starter.feature.mock.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreListerTest {

    private val lister = StoreLister()
    private val here = DemoData.s2Parking

    @Test
    fun `충전 중이 아니면 데이터 순서 그대로이고 모두 받을 수 있다`() {
        val cards = lister.cards(DemoData.s2Stores, here, null)
        assertEquals(DemoData.s2Stores.map { it.id }, cards.map { it.id })
        assertTrue(cards.all { it.beforeChargeEnds })
    }

    @Test
    fun `차 위치에서 가게까지 거리는 화면 디자인 숫자와 같다`() {
        val d = lister.cards(DemoData.s2Stores, here, null).associate { it.name to it.distanceM }
        assertEquals(1_300.0, d.getValue("BHC치킨 우면점").toDouble(), 10.0)
        assertEquals(1_500.0, d.getValue("대한감자탕뼈해장국").toDouble(), 10.0)
        assertEquals(1_600.0, d.getValue("다옴 양재온누리점").toDouble(), 10.0)
    }

    @Test
    fun `예상 시간은 조리 시간과 기사 이동(분당 250m)`() {
        val s = DemoData.s2Store   // 다옴 양재온누리점, 조리 13분, 차에서 약 1.6km(기사 6.4분 → 7분)
        assertEquals(20, lister.etaMin(s, here))
    }

    @Test
    fun `충전 중이면 충전 끝나기 전에 받을 수 있는 가게가 앞에 온다`() {
        val cards = lister.cards(DemoData.s2Stores, here, ChargingInfo(socPercent = 60, remainMin = 15))
        val ok = cards.takeWhile { it.beforeChargeEnds }
        assertTrue(ok.isNotEmpty() && ok.size < cards.size)
        assertTrue(ok.all { it.etaMin <= 15 })
        assertTrue(cards.drop(ok.size).none { it.beforeChargeEnds })
    }
}

class ChargeEstimatorTest {

    @Test
    fun `충전구가 열리고 잔량이 올라야 충전 중이다`() {
        val c = ChargeEstimator(targetPercent = 80)
        assertNull(c.update(0, portOpen = false, soc = 50))
        assertNull(c.update(1_000, portOpen = true, soc = 50))      // 열리기만 함
        assertNull(c.update(31_000, portOpen = true, soc = 50))     // 아직 안 오름
        val info = c.update(61_000, portOpen = true, soc = 52)!!    // 1분에 2% → 80%까지 14분
        assertEquals(52, info.socPercent)
        assertEquals(14, info.remainMin)
    }

    @Test
    fun `충전구를 닫으면 충전 아님`() {
        val c = ChargeEstimator()
        c.update(0, true, 50); c.update(60_000, true, 55)
        assertNull(c.update(70_000, portOpen = false, soc = 55))
    }
}
