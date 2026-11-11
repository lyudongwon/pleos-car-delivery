package ai.cardeli.starter.feature.mock

import ai.cardeli.starter.contract.Menu

/** 정차 중 메뉴판(S2)에 띄우는 가상 데이터. 실제 가게·메뉴 연동은 만들지 않는다. 원본은 DemoData */
object MockData {

    val menus: List<Menu> = DemoData.s2Store.menus

    /**
     * 주문 후 배달기사 거리 변화(m). 화면 흐름을 보기 위한 임시값이다.
     * TODO(F11): adb broadcast로 받은 기사 좌표와 차 좌표의 실제 거리로 바꾼다.
     */
    val riderDistancesM = listOf(800, 500, 200, 50, 0)
    const val RIDER_STEP_MS = 2_000L
    const val COOKING_MS = 2_000L
}
