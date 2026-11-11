package ai.cardeli.starter.feature.route

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiveDetectorTest {

    @Test
    fun `도착 뒤 창문이 열렸다 닫히면 한 번 잡는다`() {
        val d = ReceiveDetector()
        d.arm()
        assertFalse(d.onWindow(10))
        assertTrue(d.onWindow(0))
        assertFalse("두 번 잡지 않는다", d.onWindow(10).or(d.onWindow(0)))
    }

    @Test
    fun `도착 전 창문은 무시한다`() {
        val d = ReceiveDetector()
        d.onWindow(10)
        assertFalse(d.onWindow(0))
    }

    @Test
    fun `열린 적 없이 닫힘만 오면 무시한다`() {
        val d = ReceiveDetector()
        d.arm()
        assertFalse(d.onWindow(0))
    }

    @Test
    fun `반쯤 열었다 닫아도 잡는다`() {
        val d = ReceiveDetector()
        d.arm()
        d.onWindow(4)
        assertTrue(d.onWindow(0))
    }
}
