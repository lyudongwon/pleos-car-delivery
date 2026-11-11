package ai.cardeli.starter.feature.mock

import ai.cardeli.starter.contract.Menu

/** 시연용 가상 데이터. 실제 가게·메뉴 연동은 만들지 않는다. */
object MockData {

    const val STORE_NAME = "한입김밥"

    val menus = listOf(
        Menu(id = "m1", storeName = STORE_NAME, name = "원조김밥", price = 4_000),
        Menu(id = "m2", storeName = STORE_NAME, name = "참치김밥", price = 5_500),
        Menu(id = "m3", storeName = STORE_NAME, name = "라볶이", price = 6_500),
        Menu(id = "m4", storeName = STORE_NAME, name = "어묵국", price = 4_000),
    )

    /**
     * 주문 후 배달기사 거리 변화(m). 화면 흐름을 보기 위한 임시값이다.
     * TODO(F11): adb broadcast로 받은 기사 좌표와 차 좌표의 실제 거리로 바꾼다.
     */
    val riderDistancesM = listOf(800, 500, 200, 50, 0)
    const val RIDER_STEP_MS = 2_000L
    const val COOKING_MS = 2_000L
}
