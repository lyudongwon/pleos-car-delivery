package ai.cardeli.starter.feature.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTimingTest {

    @Test
    fun `공백과 문장부호는 세지 않는다`() {
        assertEquals(SpeechTiming.estimateMs("주문할까요"), SpeechTiming.estimateMs("주문할까요?  "))
    }

    @Test
    fun `시연 응답 문장은 2초에서 7초 사이로 추정한다`() {
        val lines = listOf(
            "지나가는 길에 길목김밥, 모퉁이김밥이 있어요.",
            "참치김밥 하나, 어묵국 하나, 9,500원이에요. 주문할까요?",
            "길목 공영주차장에서 받으세요. 도착하면 5분쯤 기다려요.",
        )
        for (l in lines) assertTrue("$l ${SpeechTiming.estimateMs(l)}", SpeechTiming.estimateMs(l) in 2_000L..7_000L)
    }
}
