package ai.cardeli.starter.feature.voice

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.feature.mock.DemoStore

/**
 * 주행 중 음성 주문 대화 흐름 (F5).
 * 대기 → 가게 제안(지난번 주문한 가게 하나) → 메뉴 → 주문 확인 → 주문 완료. 결제는 가상이라 확인하면 바로 주문 완료다.
 *
 * 화면이나 SDK를 모르는 순수 로직이다. 문장을 받아 다음 단계와 앱이 할 말을 돌려준다.
 * 가게 고르기(F6)는 [findStores]로 받는다.
 */
class VoiceOrderFlow(
    private val lastOrderedStoreId: String? = null,
    private val findStores: (category: String?) -> List<DemoStore>,
) {

    sealed interface Stage {
        data object Idle : Stage
        data class ChoosingMenu(val store: DemoStore) : Stage
        data class Confirming(val store: DemoStore, val cart: List<CartItem>) : Stage
        data class Ordered(val store: DemoStore, val cart: List<CartItem>) : Stage
    }

    data class Reply(val stage: Stage, val say: String)

    var stage: Stage = Stage.Idle
        private set
    private var missed = 0

    fun reset() {
        stage = Stage.Idle
        missed = 0
    }

    fun onHeard(p: ParsedUtterance): Reply {
        if (p.cancel && stage != Stage.Idle && stage !is Stage.Ordered) {
            return go(Stage.Idle, "알겠어요, 주문을 취소할게요.")
        }
        val next = when (val s = stage) {
            Stage.Idle -> start(p)
            is Stage.ChoosingMenu -> chooseMenu(s.store, p) ?: askMenu(s.store, p)
            is Stage.Confirming -> confirm(s, p)
            is Stage.Ordered -> null   // 주문 뒤의 말은 무시한다
        }
        if (next != null) {
            missed = 0
            return next
        }
        if (stage is Stage.Ordered) return Reply(stage, "")
        missed++
        return if (missed == 1) {
            Reply(stage, "다시 말씀해 주세요.")
        } else {
            go(Stage.Idle, "죄송해요, 처음부터 다시 말씀해 주세요.")
        }
    }

    /**
     * Gleo AI가 메뉴·수량을 정해서 넘긴 경우 (F15). 가게 제안을 건너뛰고,
     * 경로 근처 가게 중 그 메뉴가 모두 있는 첫 가게로 바로 주문 확인을 묻는다.
     */
    fun startWithItems(items: List<ParsedItem>): Reply {
        missed = 0
        if (items.isEmpty()) return go(Stage.Idle, "무엇을 주문할지 다시 말씀해 주세요.")
        val store = findStores(null).firstOrNull { s -> items.all { i -> s.menus.any { it.name == i.menuName } } }
            ?: return go(Stage.Idle, "가는 길에 그 메뉴를 파는 가게가 없어요.")
        val p = ParsedUtterance(false, null, null, store.name, items, confirm = false, cancel = false)
        val r = chooseMenu(store, p) ?: return go(Stage.Idle, "가는 길에 그 메뉴를 파는 가게가 없어요.")
        return Reply(r.stage, "${store.name}에서 ${r.say}")
    }

    /** 경로 근처 가게 중 지난번에 주문한 가게를 먼저, 없으면 가장 먼저 지나가는 가게 하나를 제안한다 (기획안 5-1) */
    private fun start(p: ParsedUtterance): Reply? {
        if (!p.wantsOrder) return null
        val stores = findStores(p.category)
        if (stores.isEmpty()) return Reply(Stage.Idle, "가는 길에는 받을 수 있는 가게가 없어요.")
        val last = stores.firstOrNull { it.id == lastOrderedStoreId }
        val store = last ?: stores.first()
        val say = if (last != null) {
            "가는 길에 배달받을 수 있는 가게가 있습니다. 지난번에 드신 ${store.name} 어떠세요?"
        } else {
            "가는 길에 배달받을 수 있는 ${store.name}${Korean.iGa(store.name)} 있습니다. 어떠세요?"
        }
        return go(Stage.ChoosingMenu(store), say)
    }

    /** 가게는 좋다고 했는데 메뉴를 말하지 않았을 때 */
    private fun askMenu(store: DemoStore, p: ParsedUtterance): Reply? =
        if (p.confirm) go(Stage.ChoosingMenu(store), "${store.name}에서 무엇을 드릴까요?") else null

    private fun chooseMenu(store: DemoStore, p: ParsedUtterance): Reply? {
        val cart = p.items.mapNotNull { item -> store.menus.firstOrNull { it.name == item.menuName }?.let { CartItem(it, item.quantity) } }
        if (cart.isEmpty()) return null
        val list = cart.joinToString(", ") { "${it.menu.name} ${Korean.count(it.quantity)}" }
        return go(Stage.Confirming(store, cart), "$list, ${Korean.won(cart.sumOf { it.totalPrice })}입니다. 주문할까요?")
    }

    private fun confirm(s: Stage.Confirming, p: ParsedUtterance): Reply? {
        if (!p.confirm) return null
        return go(Stage.Ordered(s.store, s.cart), "주문했어요.")
    }

    private fun go(next: Stage, say: String): Reply {
        stage = next
        return Reply(next, say)
    }
}

/** 응답 문장용 한국어 도우미 */
object Korean {
    /** 받침이 있으면 "이", 없으면 "가" */
    fun iGa(word: String): String {
        val c = word.lastOrNull() ?: return "이"
        if (c !in '가'..'힣') return "이"
        return if ((c - '가') % 28 != 0) "이" else "가"
    }

    fun count(n: Int): String = when (n) {
        1 -> "하나"
        2 -> "두 개"
        3 -> "세 개"
        4 -> "네 개"
        else -> "${n}개"
    }

    fun won(price: Int): String = String.format(java.util.Locale.ROOT, "%,d원", price)
}
