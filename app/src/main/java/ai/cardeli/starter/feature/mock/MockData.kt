package ai.cardeli.starter.feature.mock

import ai.cardeli.starter.contract.Menu

/** 정차 중 메뉴판(S2)에 띄우는 가상 데이터. 실제 가게·메뉴 연동은 만들지 않는다. 원본은 DemoData */
object MockData {
    val menus: List<Menu> = DemoData.s2Store.menus
}
