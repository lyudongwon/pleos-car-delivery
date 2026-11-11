package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState

/** @Preview 전용 가짜 state. 앱 실행에는 쓰지 않는다. 화면을 보며 자유롭게 바꿔도 된다. */
internal object PreviewStates {

    private val menus = listOf(
        Menu("m1", "한입김밥", "원조김밥", 4_000),
        Menu("m2", "한입김밥", "참치김밥", 5_500),
        Menu("m3", "한입김밥", "라볶이", 6_500),
        Menu("m4", "한입김밥", "어묵국", 4_000),
    )
    private val cart = listOf(CartItem(menus[1], 1), CartItem(menus[3], 1))

    val browsing = OrderUiState(menus = menus, cart = cart, status = OrderStatus.Browsing)
    val confirming = browsing.copy(status = OrderStatus.Confirming)
    val riderComing = browsing.copy(status = OrderStatus.RiderComing, riderDistanceM = 200)
    val driving = browsing.copy(isDriving = true, status = OrderStatus.Ordered)
}
