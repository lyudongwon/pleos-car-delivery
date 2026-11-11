package ai.cardeli.starter.feature.voice

/**
 * 주문 해석 (F3). 인식된 문장에서 의도를 규칙으로 뽑는다. LLM SDK는 제공 종료라 쓰지 않는다.
 *
 * 무엇을 뜻하는지(주문 시작인지, 확인인지)는 대화 단계에 따라 F5가 고른다.
 * 여기서는 문장에 들어 있는 조각만 빠짐없이 찾는다. 띄어쓰기는 무시한다("참치 김밥" = "참치김밥").
 */
data class ParsedItem(val menuName: String, val quantity: Int)

data class ParsedUtterance(
    val wantsOrder: Boolean,      // 시켜줘, 주문, 배달, 먹고 싶어
    val category: String?,        // 김밥, 국밥처럼 찾는 음식 종류
    val storeIndex: Int?,         // "첫 번째" = 0, "두 번째" = 1
    val storeName: String?,       // 가게 이름을 직접 말한 경우
    val items: List<ParsedItem>,
    val confirm: Boolean,         // 응, 네, 그래, 주문해
    val cancel: Boolean,          // 아니, 취소, 그만
)

class OrderParser(
    private val menuNames: List<String>,
    private val storeNames: List<String> = emptyList(),
    private val categories: List<String> = listOf("김밥", "국밥", "떡볶이", "분식"),
) {

    fun parse(text: String): ParsedUtterance {
        val s = normalize(text)
        var rest = s

        val storeIndex = ORDINALS.entries.firstOrNull { (words, _) -> words.any { it in rest } }?.let { (words, idx) ->
            words.filter { it in rest }.forEach { rest = rest.replace(it, " ") }
            idx
        }
        val storeName = storeNames.firstOrNull { normalize(it) in rest }

        val items = findItems(rest)
        // 메뉴 이름("참치김밥") 안의 "김밥"을 종류로 보지 않도록, 메뉴를 지운 나머지에서 종류를 찾는다
        var withoutMenus = rest
        menuNames.sortedByDescending { it.length }.forEach { withoutMenus = withoutMenus.replace(normalize(it), " ") }
        val category = categories.firstOrNull { it in withoutMenus }

        return ParsedUtterance(
            wantsOrder = ORDER_WORDS.any { it in s },
            category = category,
            storeIndex = storeIndex,
            storeName = storeName,
            items = items,
            confirm = CONFIRM_WORDS.any { s.startsWith(it) } || "주문해" in s && !s.endsWith("줘"),
            cancel = CANCEL_WORDS.any { it in s },
        )
    }

    /** 메뉴 이름이 나온 자리부터 다음 메뉴 이름 전까지에서 수량을 찾는다. 없으면 1개 */
    private fun findItems(s: String): List<ParsedItem> {
        val hits = menuNames
            .sortedByDescending { it.length }   // "원조김밥"이 "김밥"보다 먼저 잡히게
            .flatMap { name -> Regex(Regex.escape(normalize(name))).findAll(s).map { it.range to name } }
            .sortedBy { it.first.first }
            .fold(mutableListOf<Pair<IntRange, String>>()) { acc, hit ->
                if (acc.none { it.first.last >= hit.first.first }) acc += hit   // 겹치는 짧은 이름은 버린다
                acc
            }
        return hits.mapIndexed { i, (range, name) ->
            val end = hits.getOrNull(i + 1)?.first?.first ?: s.length
            ParsedItem(name, quantityIn(s.substring(range.last + 1, end)))
        }
            .groupBy { it.menuName }
            .map { (name, list) -> ParsedItem(name, list.sumOf { it.quantity }) }
    }

    private fun quantityIn(seg: String): Int {
        Regex("(\\d+)개?").find(seg)?.let { return it.groupValues[1].toInt().coerceIn(1, 20) }
        return QUANTITIES.firstOrNull { (words, _) -> words.any { seg.startsWith(it) || it in seg.take(4) } }?.second ?: 1
    }

    private companion object {
        fun normalize(t: String) = t.replace(Regex("[\\s,.?!~]"), "")

        val ORDINALS = mapOf(
            listOf("첫번째", "첫째", "첫번", "1번", "일번") to 0,
            listOf("두번째", "둘째", "2번", "이번째") to 1,
        )
        val QUANTITIES = listOf(
            listOf("하나", "한개", "한그릇", "한줄", "한") to 1,
            listOf("둘", "두개", "두그릇", "두줄", "두") to 2,
            listOf("셋", "세개", "세그릇", "세줄", "세") to 3,
            listOf("넷", "네개", "네그릇", "네줄") to 4,
        )
        val ORDER_WORDS = listOf("시켜", "주문", "배달", "먹고싶", "사줘")
        val CONFIRM_WORDS = listOf("응", "네", "예", "그래", "좋아", "어주문", "주문할게")
        val CANCEL_WORDS = listOf("아니", "취소", "그만", "됐어")
    }
}
