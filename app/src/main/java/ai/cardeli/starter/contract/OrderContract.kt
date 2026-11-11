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

    // ---------- S1: 주행 중 음성 주문 (DrivingScreen) ----------
    val voiceState: VoiceState = VoiceState.Idle,
    val transcript: String = "",             // 운전자가 말하는 중인 문장(음성 인식 중간 결과). 자막처럼 보여준다
    val assistantText: String = "",          // 앱이 마지막으로 한 말. 주행 화면 텍스트 1줄
    val suggestions: List<StoreSuggestion> = emptyList(),  // 경로 근처 추천 가게 (최대 2개)
    val pickup: PickupPoint? = null,         // 정해진 수령 지점. 정해지기 전에는 null
    val touchCount: Int = 0,                 // 주행 중 화면 터치 횟수. 영상에서 '화면 터치 0회'로 보여준다

    // ---------- 배달기사에게 가는 정보 (F10·F11 기사 화면) ----------
    val delivery: DeliveryInfo? = null,      // 주문 뒤 채워진다
) {
    val totalPrice: Int get() = cart.sumOf { it.totalPrice }
}

enum class VoiceState {
    Idle,       // 대기
    Listening,  // 듣는 중
    Thinking,   // 해석·계산 중
    Speaking,   // 앱이 말하는 중
}

data class StoreSuggestion(
    val storeName: String,
    val aheadM: Int,           // 지금 위치에서 경로를 따라 가게 앞까지 거리(m)
)

/** 기사에게 보내는 받는 곳 정보. 주소 대신 좌표와 차량 정보로 찾아온다 */
data class DeliveryInfo(
    val placeLabel: String,    // S2: "지금 차 위치", S1: 수령 지점 이름
    val latitude: Double,
    val longitude: Double,
    val plate: String,         // 처음 한 번 등록한 차량 번호
    val color: String,
    val carModel: String,      // 차에서 읽은 차종. 못 읽으면 "차종 미확인"
)

data class PickupPoint(
    val name: String,          // 예: 길목 공영주차장
    val detourMin: Int,        // 원래 경로보다 늘어나는 시간(분). 화면에 "+N분"
    val detourM: Int,          // 원래 경로보다 늘어나는 거리(m)
    val waitMin: Int,          // 도착해서 음식을 기다리는 시간(분). 0이면 바로 받음
)

sealed interface OrderEvent {
    data class AddMenu(val menu: Menu) : OrderEvent
    data class RemoveMenu(val menu: Menu) : OrderEvent
    data object ReviewOrder : OrderEvent      // Browsing → Confirming (장바구니가 비어 있으면 무시)
    data object BackToMenu : OrderEvent       // Confirming → Browsing
    data object PlaceOrder : OrderEvent       // Confirming → Ordered
    data object ConfirmReceived : OrderEvent  // Arrived → Received
}
