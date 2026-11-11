package ai.cardeli.starter.feature.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderParserTest {

    private val parser = OrderParser(
        menuNames = listOf("원조김밥", "참치김밥", "라볶이", "어묵국"),
        storeNames = listOf("길목김밥", "모퉁이김밥"),
    )

    // ---------- 시연 대사의 실제 인식 결과 (에뮬레이터 11/11) ----------

    @Test
    fun `대사1 주문 시작과 음식 종류`() {
        val p = parser.parse("가는 길에 받을 수 있는 김밥 시켜줘")
        assertTrue(p.wantsOrder)
        assertEquals("김밥", p.category)
        assertTrue(p.items.isEmpty())
        assertFalse(p.confirm)
    }

    @Test
    fun `대사2 가게 순서와 메뉴 두 개`() {
        val p = parser.parse("첫 번째 참치김밥 하나랑 어묵국 하나")
        assertEquals(0, p.storeIndex)
        assertEquals(listOf(ParsedItem("참치김밥", 1), ParsedItem("어묵국", 1)), p.items)
        assertNull("메뉴 이름 속 김밥은 종류로 보지 않는다", p.category)
    }

    @Test
    fun `대사3 확인`() {
        val p = parser.parse("응 주문해")
        assertTrue(p.confirm)
        assertFalse(p.cancel)
    }

    // ---------- 인식이 조금 흔들릴 때 ----------

    @Test
    fun `조사와 띄어쓰기가 달라도 같다`() {
        assertEquals("김밥", parser.parse("가는 길을 받을 수 있는 김밥 시켜 줘").category)
        assertEquals(
            listOf(ParsedItem("참치김밥", 1), ParsedItem("어묵국", 1)),
            parser.parse("첫번째 참치 김밥 하나 하고 어묵국 하나").items,
        )
    }

    @Test
    fun `수량과 두 번째 가게`() {
        val p = parser.parse("두 번째, 원조김밥 두 개랑 라볶이")
        assertEquals(1, p.storeIndex)
        assertEquals(listOf(ParsedItem("원조김밥", 2), ParsedItem("라볶이", 1)), p.items)
        assertEquals(listOf(ParsedItem("참치김밥", 3)), parser.parse("참치김밥 3개").items)
    }

    @Test
    fun `가게 이름을 직접 말하면 이름으로 잡는다`() {
        assertEquals("모퉁이김밥", parser.parse("모퉁이김밥에서 어묵국 하나").storeName)
    }

    @Test
    fun `취소와 주문 시작은 확인이 아니다`() {
        assertTrue(parser.parse("아니 취소해").cancel)
        assertFalse(parser.parse("아니 취소해").confirm)
        assertFalse(parser.parse("김밥 주문해줘").confirm)
        assertTrue(parser.parse("김밥 주문해줘").wantsOrder)
    }
}
