package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.ChargingInfo
import ai.cardeli.starter.contract.DeliveryInfo
import ai.cardeli.starter.contract.LocateBy
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.PickupPoint
import ai.cardeli.starter.contract.ReceiveWindow
import ai.cardeli.starter.contract.StoreCard
import ai.cardeli.starter.contract.VoiceState

/** @Preview 전용 가짜 state. 앱 실행에는 쓰지 않는다. 화면을 보며 자유롭게 바꿔도 된다. */
internal object PreviewStates {

    private val menus = listOf(
        Menu("s2-1", "다옴 양재온누리점", "다옴김밥", 4_000, "신선한 채소와 달걀, 햄을 듬뿍 넣어 고소하고 담백한 맛을 살린 기본 김밥", 14),
        Menu("s2-2", "다옴 양재온누리점", "참치김밥", 5_500, "고소한 참치마요와 아삭한 채소가 어우러진 김밥", 31),
        Menu("s2-3", "다옴 양재온누리점", "고추참치김밥", 6_000, "매콤한 고추참치에 신선한 채소를 더한 김밥", 11),
    )
    private val cart = listOf(CartItem(menus[2], 1), CartItem(menus[0], 1))

    private fun card(id: String, name: String, cat: String, dist: Int, eta: Int, fee: Int, min: Int, rating: Double, reviews: Int, ok: Boolean = true) =
        StoreCard(id, name, cat, dist, eta, deliveryFee = fee, minOrder = min, rating = rating, reviewCount = reviews,
            latitude = 37.47, longitude = 127.04, lastOrder = "22:20", beforeChargeEnds = ok)

    private val stores = listOf(
        card("s2-b", "BHC치킨 우면점", "패스트푸드", 1_300, 29, 3_000, 18_000, 4.8, 121),
        card("s2-c", "대한감자탕뼈해장국", "한식", 1_500, 21, 2_500, 10_000, 4.3, 132, ok = false),
        card("s2-a", "다옴 양재온누리점", "한식", 1_600, 20, 2_300, 10_000, 4.6, 89),
    )

    private val delivery = DeliveryInfo(
        placeLabel = "지금 차 위치",
        latitude = 37.3838,
        longitude = 127.1240,
        plate = "12가 3456",
        color = "흰색",
        carModel = "Hyundai 42dot Connect",
        window = ReceiveWindow.Passenger,
        locateBy = LocateBy.Hazard,
        fixedByPark = true,
    )

    val storeList = OrderUiState(
        stores = stores,
        menus = menus,
        deliveryFee = 2_300,
        charging = ChargingInfo(socPercent = 62, remainMin = 25),
    )
    val browsing = storeList.copy(cart = cart, status = OrderStatus.Browsing)
    val confirming = browsing.copy(status = OrderStatus.Confirming, isParked = true)
    val riderComing = browsing.copy(status = OrderStatus.RiderComing, riderDistanceM = 180, etaMin = 3, delivery = delivery)
    val arrived = riderComing.copy(status = OrderStatus.Arrived, riderDistanceM = 10)
    val driving = browsing.copy(
        isDriving = true,
        status = OrderStatus.Cooking,
        voiceState = VoiceState.Speaking,
        assistantText = "수령 지점을 경로에 추가했습니다.",
        pickup = PickupPoint("길목사거리 정차구역", detourMin = 0, detourM = 0, waitMin = 0),
    )
}
