package ai.cardeli.starter.contract

/**
 * 화면(ui/)과 기능(feature/)이 주고받는 약속.
 *
 * - 화면은 [OrderUiState]를 받아 그리기만 하고, 사용자 동작은 [OrderEvent]로 올려 보낸다.
 * - 이 파일은 개발 담당이 고친다. 화면에 필요한 값이 없으면 개발 담당에게 요청한다.
 */

data class Menu(
    val id: String,
    val storeName: String,
    val name: String,
    val price: Int,          // 원
)

data class CartItem(
    val menu: Menu,
    val quantity: Int,
) {
    val totalPrice: Int get() = menu.price * quantity
}

enum class OrderStatus {
    Browsing,     // 메뉴 고르는 중
    Confirming,   // 주문 내용 확인 중
    Ordered,      // 주문 완료, 조리 중
    RiderComing,  // 배달기사 이동 중 (riderDistanceM 사용)
    Arrived,      // 배달기사 도착
    Received,     // 수령 완료
}

data class OrderUiState(
    val isDriving: Boolean = false,          // true면 DrivingScreen만 보인다
    val menus: List<Menu> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val status: OrderStatus = OrderStatus.Browsing,
    val riderDistanceM: Int? = null,         // 배달기사와 차 사이 거리(m). 배달 전에는 null
) {
    val totalPrice: Int get() = cart.sumOf { it.totalPrice }
}

sealed interface OrderEvent {
    data class AddMenu(val menu: Menu) : OrderEvent
    data class RemoveMenu(val menu: Menu) : OrderEvent
    data object ReviewOrder : OrderEvent      // Browsing → Confirming (장바구니가 비어 있으면 무시)
    data object BackToMenu : OrderEvent       // Confirming → Browsing
    data object PlaceOrder : OrderEvent       // Confirming → Ordered
    data object ConfirmReceived : OrderEvent  // Arrived → Received
}
