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
    val description: String = "",
    val reviewCount: Int = 0,
)

data class CartItem(
    val menu: Menu,
    val quantity: Int,
) {
    val totalPrice: Int get() = menu.price * quantity
}

/** 주문 진행 단계. 화면의 진행 표시(주문 접수 → 조리 중 → 배달 시작 → 배달 완료)와 맞춘다 */
enum class OrderStatus {
    Browsing,     // 메뉴 고르는 중
    Confirming,   // 주문서 작성 중 (배달 방식·창문·위치 알림·요청사항·결제)
    Ordered,      // 주문 접수
    Cooking,      // 사장님이 조리 중
    Delivering,   // 배달 시작 (기사가 200m 밖)
    RiderComing,  // 라이더가 곧 도착 (200m 안, riderDistanceM 사용)
    Arrived,      // 배달기사 도착 (30m 안). 화면은 "음식을 수령하셨나요?"를 띄운다
    Received,     // 수령 완료 = 배달 완료
}

/** 어디서 받을지 */
enum class DeliveryMethod(val label: String) {
    ToCar("차에서 직접 받을게요"),
    PickupPoint("지정 장소에서 수령할게요"),
}

/** 음식을 받을 창문. 배달기사에게 같이 보낸다 */
enum class ReceiveWindow(val label: String) {
    Driver("운전석"),
    Passenger("조수석"),
    RearLeft("왼쪽 뒷좌석"),
    RearRight("오른쪽 뒷좌석"),
}

/** 배달기사가 내 차를 알아보는 방법 */
enum class LocateBy(val label: String) {
    Hazard("비상등으로 알리기"),    // 기사가 가까워지면 앱이 운전자에게 비상등을 켜 달라고 말한다
    CarInfo("차 정보로 알리기"),    // 번호판·차종·색
}

enum class PaymentMethod(val label: String) {
    Card("신용·체크카드"),
    KakaoPay("카카오페이"),
    HyundaiPay("현대 카페이"),
    Other("기타 결제수단"),
}

/** 주문서에서 고르는 값. 기본값으로 시작하고 화면에서 바꾼다 */
data class OrderOptions(
    val method: DeliveryMethod = DeliveryMethod.ToCar,
    val window: ReceiveWindow = ReceiveWindow.Driver,
    val locateBy: LocateBy = LocateBy.CarInfo,
    val riderNote: String = "",          // 추가 전달 사항 (기사에게)
    val storeNote: String = "",          // 가게 요청사항
    val payment: PaymentMethod = PaymentMethod.Card,
)

data class OrderUiState(
    val isDriving: Boolean = false,          // true면 DrivingScreen만 보인다
    val isParked: Boolean = false,           // 기어 P. 이때 주문하면 지금 차 위치를 받는 곳으로 확정한다
    val placeLabel: String = "",             // 지금 차가 있는 곳 (예: 서울 서초구 헌릉로)
    val carLatitude: Double? = null,         // 지금 차 위치 (지도에 표시)
    val carLongitude: Double? = null,
    val stores: List<StoreCard> = emptyList(),   // 정차 중 가게 목록 (충전 중이면 충전 끝나기 전에 받을 수 있는 가게가 앞에 온다)
    val selectedStoreId: String? = null,     // 고른 가게. null이면 가게 목록을 보여 준다
    val menus: List<Menu> = emptyList(),     // 고른 가게의 메뉴
    val cart: List<CartItem> = emptyList(),
    val deliveryFee: Int = 0,                // 고른 가게의 배달비(원)
    val options: OrderOptions = OrderOptions(),
    val status: OrderStatus = OrderStatus.Browsing,
    val etaMin: Int? = null,                 // 배달 완료까지 남은 시간(분). 주문 전에는 null
    val receipt: OrderReceipt? = null,       // 주문번호·주문 시각. 주문 뒤 채워진다
    val pastOrders: List<PastOrder> = emptyList(),   // 주문내역의 지난 주문 (시연 데이터)
    val riderDistanceM: Int? = null,         // 배달기사와 받는 곳 사이 거리(m). 배달 전에는 null
    val charging: ChargingInfo? = null,      // 충전 중이면 채워진다

    // ---------- S1: 주행 중 음성 주문 (DrivingScreen) ----------
    val voiceState: VoiceState = VoiceState.Idle,
    val transcript: String = "",             // 운전자가 말하는 중인 문장(음성 인식 중간 결과). 자막처럼 보여준다
    val assistantText: String = "",          // 앱이 마지막으로 한 말. 주행 화면 텍스트 1줄
    val suggestions: List<StoreSuggestion> = emptyList(),  // 앱이 제안한 가게 (지난번 주문한 가게 하나)
    val pickup: PickupPoint? = null,         // 정해진 수령 지점. 정해지기 전에는 null
    val touchCount: Int = 0,                 // 주행 중 화면 터치 횟수. 영상에서 '화면 터치 0회'로 보여준다
    val atPickup: Boolean = false,           // 차가 수령 지점(반경 100m)에 도착했는지

    // ---------- 배달기사에게 가는 정보 ----------
    val delivery: DeliveryInfo? = null,      // 주문 뒤 채워진다
) {
    val totalPrice: Int get() = cart.sumOf { it.totalPrice }
    val payTotal: Int get() = if (cart.isEmpty()) 0 else totalPrice + deliveryFee
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

/** 정차 중 가게 목록 한 줄 */
data class StoreCard(
    val id: String,
    val name: String,
    val category: String,      // 카페·디저트, 한식, 중식, 일식, 패스트푸드
    val distanceM: Int,        // 차에서 가게까지 직선 거리(m)
    val etaMin: Int,           // 조리 + 기사 이동 예상(분)
    val deliveryFee: Int,
    val minOrder: Int,         // 최소 주문 금액(원)
    val rating: Double,
    val reviewCount: Int,
    val latitude: Double,
    val longitude: Double,
    val lastOrder: String,     // 예: 22:20
    val beforeChargeEnds: Boolean,   // 충전이 끝나기 전에 받을 수 있는지. 충전 중이 아니면 true
)

/** 주문번호와 주문 시각 */
data class OrderReceipt(val number: String, val orderedAt: String)

/** 주문내역 목록의 지난 주문 */
data class PastOrder(
    val id: String,
    val storeName: String,
    val summary: String,       // 예: 생망고 케이크 외 2개
    val dateLabel: String,     // 예: 어제 12:45
    val statusLabel: String,   // 예: 수령 완료
)

/** 충전 상태 (Vehicle SDK EvBattery) */
data class ChargingInfo(
    val socPercent: Int,
    val remainMin: Int?,       // 목표(80%)까지 남은 시간 추정(분). 아직 속도를 모르면 null
)

/** 기사에게 보내는 받는 곳 정보. 주소 대신 좌표와 차량 정보로 찾아온다 */
data class DeliveryInfo(
    val placeLabel: String,    // S2: "지금 차 위치", S1: 수령 지점 이름
    val latitude: Double,
    val longitude: Double,
    val plate: String,         // 처음 한 번 등록한 차량 번호
    val color: String,
    val carModel: String,      // 차에서 읽은 차종. 못 읽으면 "차종 미확인"
    val window: ReceiveWindow = ReceiveWindow.Driver,
    val locateBy: LocateBy = LocateBy.CarInfo,
    val riderNote: String = "",
    val fixedByPark: Boolean = false,   // 기어 P에서 확정한 위치인지
)

data class PickupPoint(
    val name: String,          // 예: 길목 공영주차장
    val detourMin: Int,        // 원래 경로보다 늘어나는 시간(분). 화면에 "+N분"
    val detourM: Int,          // 원래 경로보다 늘어나는 거리(m)
    val waitMin: Int,          // 도착해서 음식을 기다리는 시간(분). 0이면 바로 받음
)

sealed interface OrderEvent {
    data class SelectStore(val storeId: String) : OrderEvent   // 가게 목록 → 메뉴
    data object BackToStores : OrderEvent     // 메뉴 → 가게 목록
    data class AddMenu(val menu: Menu) : OrderEvent
    data class RemoveMenu(val menu: Menu) : OrderEvent
    data object ReviewOrder : OrderEvent      // Browsing → Confirming (장바구니가 비어 있으면 무시)
    data object BackToMenu : OrderEvent       // Confirming → Browsing
    data class SelectMethod(val method: DeliveryMethod) : OrderEvent
    data class SelectWindow(val window: ReceiveWindow) : OrderEvent
    data class SelectLocateBy(val locateBy: LocateBy) : OrderEvent
    data class SetRiderNote(val text: String) : OrderEvent
    data class SetStoreNote(val text: String) : OrderEvent
    data class SelectPayment(val payment: PaymentMethod) : OrderEvent
    data object PlaceOrder : OrderEvent       // Confirming → Ordered (결제 완료)
    data object ConfirmReceived : OrderEvent  // "음식을 수령하셨나요?" → 네
    data object NotReceived : OrderEvent      // "음식을 수령하셨나요?" → 아니요, 못 받았어요
}
