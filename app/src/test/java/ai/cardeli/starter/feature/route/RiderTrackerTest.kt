package ai.cardeli.starter.feature.route

import ai.cardeli.starter.feature.route.RiderTracker.Event
import org.junit.Assert.assertEquals
import org.junit.Test

class RiderTrackerTest {

    @Test
    fun `200m 안에 처음 들어올 때와 30m 안에 들어올 때 한 번씩`() {
        val t = RiderTracker()
        assertEquals(emptyList<Event>(), t.update(871))
        assertEquals(listOf(Event.Approaching), t.update(180))
        assertEquals(emptyList<Event>(), t.update(120))
        assertEquals(listOf(Event.Arrived), t.update(28))
        assertEquals(emptyList<Event>(), t.update(10))
    }

    @Test
    fun `멀어졌다 다시 와도 반복하지 않는다`() {
        val t = RiderTracker()
        t.update(150)
        t.update(400)
        assertEquals(emptyList<Event>(), t.update(150))
    }

    @Test
    fun `바로 도착 거리로 들어오면 도착만 알린다`() {
        val t = RiderTracker()
        assertEquals(listOf(Event.Arrived), t.update(20))
    }

    @Test
    fun `리셋하면 다음 주문에서 다시 알린다`() {
        val t = RiderTracker()
        t.update(150)
        t.reset()
        assertEquals(listOf(Event.Approaching), t.update(150))
    }
}
