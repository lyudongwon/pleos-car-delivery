package ai.cardeli.starter.feature.voice

import ai.cardeli.starter.feature.mock.DemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceOrderFlowTest {

    private val stores = DemoData.s1Stores.take(2)   // 길목김밥, 모퉁이김밥
    private val parser = OrderParser(stores.first().menus.map { it.name }, stores.map { it.name })
    private fun flow() = VoiceOrderFlow { stores }
    private fun VoiceOrderFlow.hear(t: String) = onHeard(parser.parse(t))

    @Test
    fun `시연 대화가 터치 없이 주문 완료까지 간다`() {
        val f = flow()
        val r1 = f.hear("가는 길에 받을 수 있는 김밥 시켜줘")
        assertEquals("지나가는 길에 길목김밥, 모퉁이김밥이 있어요.", r1.say)
        assertTrue(r1.stage is VoiceOrderFlow.Stage.ChoosingStore)

        val r2 = f.hear("첫 번째 참치김밥 하나랑 어묵국 하나")
        assertEquals("참치김밥 하나, 어묵국 하나, 9,500원이에요. 주문할까요?", r2.say)
        val confirming = r2.stage as VoiceOrderFlow.Stage.Confirming
        assertEquals("길목김밥", confirming.store.name)
        assertEquals(9_500, confirming.cart.sumOf { it.totalPrice })

        val r3 = f.hear("응 주문해")
        assertTrue(r3.stage is VoiceOrderFlow.Stage.Ordered)
        assertEquals("주문했어요.", r3.say)
    }

    @Test
    fun `가게만 고르면 메뉴를 묻는다`() {
        val f = flow()
        f.hear("김밥 시켜줘")
        assertEquals("모퉁이김밥에서 무엇을 드릴까요?", f.hear("두 번째").say)
        assertEquals("원조김밥 두 개, 8,000원이에요. 주문할까요?", f.hear("원조김밥 두 개").say)
    }

    @Test
    fun `못 알아들으면 한 번 다시 묻고 두 번째엔 처음으로`() {
        val f = flow()
        f.hear("김밥 시켜줘")
        assertEquals("다시 말씀해 주세요.", f.hear("음 글쎄").say)
        assertTrue(f.stage is VoiceOrderFlow.Stage.ChoosingStore)
        f.hear("뭐였더라")
        assertTrue(f.stage is VoiceOrderFlow.Stage.Idle)
    }

    @Test
    fun `확인 단계에서 아니라고 하면 취소`() {
        val f = flow()
        f.hear("김밥 시켜줘"); f.hear("첫 번째 참치김밥 하나")
        assertEquals("알겠어요, 주문을 취소할게요.", f.hear("아니 취소해").say)
        assertTrue(f.stage is VoiceOrderFlow.Stage.Idle)
    }

    @Test
    fun `가는 길에 가게가 없으면 그렇게 말한다`() {
        val f = VoiceOrderFlow { emptyList() }
        assertEquals("가는 길에는 받을 수 있는 가게가 없어요.", f.onHeard(parser.parse("김밥 시켜줘")).say)
    }

    @Test
    fun `받침에 따라 이 가`() {
        assertEquals("이", Korean.iGa("모퉁이김밥"))
        assertEquals("가", Korean.iGa("라볶이"))
    }
}
