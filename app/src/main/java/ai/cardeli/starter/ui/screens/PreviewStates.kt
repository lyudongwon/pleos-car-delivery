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
        Menu("m1", "서현김밥", "원조김밥", 4_000),
        Menu("m2", "서현김밥", "참치김밥", 5_500),
        Menu("m3", "서현김밥", "라볶이", 6_500),
        Menu("m4", "서현김밥", "어묵국", 4_000),
    )
    private val cart = listOf(CartItem(menus[1], 1), CartItem(menus[3], 1))

    private val stores = listOf(
        StoreCard("s2-a", "서현김밥", "김밥", 350, 18, 2_300, beforeChargeEnds = true),
        StoreCard("s2-b", "모퉁이치킨", "치킨", 900, 32, 3_000, beforeChargeEnds = false),
        StoreCard("s2-c", "길목카페", "카페", 500, 15, 2_000, beforeChargeEnds = true),
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
