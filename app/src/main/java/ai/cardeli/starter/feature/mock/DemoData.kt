package ai.cardeli.starter.feature.mock

import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.feature.geo.LatLng

/**
 * 시연 데이터. 가게·주차장 이름은 모두 가상이다. 좌표는 S1 시연 경로(DemoRoute) 기준으로 잡았다.
 * 메뉴는 지원서 시나리오 1 기준: 참치김밥 + 어묵국 = 9,500원.
 */
data class DemoStore(
    val id: String,
    val name: String,
    val location: LatLng,
    val cookMin: Int,          // 주문부터 음식이 나오기까지(분)
    val menus: List<Menu>,
)

enum class PickupType { PublicParking, StopZone }

data class PickupCandidate(
    val id: String,
    val name: String,
    val location: LatLng,
    val type: PickupType,
)

data class DemoVehicle(val plate: String, val color: String)

/** 내비 실측: 원래 경로보다 늘어나는 거리·시간, 현재 위치에서 그 지점까지 걸리는 시간 */
data class PickupMeasure(val detourM: Int, val detourSec: Int, val etaSec: Int)

object DemoData {

    private fun menusOf(storeName: String, prefix: String) = listOf(
        Menu("$prefix-1", storeName, "원조김밥", 4_000),
        Menu("$prefix-2", storeName, "참치김밥", 5_500),
        Menu("$prefix-3", storeName, "라볶이", 6_500),
        Menu("$prefix-4", storeName, "어묵국", 4_000),
    )

    // ---------- S1: 주행 중 음성 주문 ----------

    /** 출발 위치 = 에뮬레이터 내비 고정 위치. FusedLocation도 `adb emu geo fix 127.094681 37.4127212`로 맞춘다 */
    val s1Start = LatLng(37.4127212, 127.094681)
    val s1Destination = LatLng(37.384553, 127.123706)
    const val S1_DESTINATION_NAME = "서현역"

    /**
     * S1 가게 4곳. 경로 근처 앞쪽인 A·B만 추천돼야 한다.
     * C는 출발점 뒤쪽(이미 지나친 방향), D는 경로에서 약 1km 떨어져 있어 걸러진다.
     */
    val s1Stores = listOf(
        DemoStore("s1-a", "길목김밥", LatLng(37.400661, 127.106877), cookMin = 10, menus = menusOf("길목김밥", "a")),
        DemoStore("s1-b", "모퉁이김밥", LatLng(37.392223, 127.114064), cookMin = 12, menus = menusOf("모퉁이김밥", "b")),
        DemoStore("s1-c", "시흥김밥", LatLng(37.415864, 127.091854), cookMin = 8, menus = menusOf("시흥김밥", "c")),
        DemoStore("s1-d", "먼길김밥", LatLng(37.382647, 127.110909), cookMin = 10, menus = menusOf("먼길김밥", "d")),
    )

    /** 길목김밥(s1-a) 근처 수령 후보 5곳. 추가 시간은 F7에서 내비 실측으로 채운다 */
    val s1Pickups = listOf(
        PickupCandidate("p0", "길목 공영주차장", LatLng(37.402198, 127.105473), PickupType.PublicParking),
        PickupCandidate("p1", "길목사거리 정차구역", LatLng(37.398806, 127.106622), PickupType.StopZone),
        PickupCandidate("p2", "동쪽 상가 주차장", LatLng(37.400661, 127.110269), PickupType.PublicParking),
        PickupCandidate("p3", "서쪽 공원 주차장", LatLng(37.400212, 127.103937), PickupType.PublicParking),
        PickupCandidate("p4", "남쪽 정차구역", LatLng(37.397882, 127.106944), PickupType.StopZone),
    )

    /**
     * 내비 실측값 (11/11, docs/poc/F7_pickup_measure_1111.json). S1 출발점 → 서현역 경로 기준.
     * detour = (지점을 경유지로 넣은 경로) − (목적지만), eta = 현재 위치 → 지점(requestTrip).
     * 내비 값은 100m·60초 단위로 반올림돼 있다. 출발점이 아닌 곳에서는 맞지 않는다.
     */
    val s1Measures = mapOf(
        "store-s1-a" to PickupMeasure(detourM = 600, detourSec = 120, etaSec = 406),
        "p0" to PickupMeasure(detourM = 600, detourSec = 120, etaSec = 437),
        "p1" to PickupMeasure(detourM = 0, detourSec = 0, etaSec = 416),
        "p2" to PickupMeasure(detourM = 500, detourSec = 300, etaSec = 509),
        "p3" to PickupMeasure(detourM = 800, detourSec = 180, etaSec = 443),
        "p4" to PickupMeasure(detourM = 0, detourSec = 0, etaSec = 426),
    )

    // ---------- S2: 정차 중 주문 (학원 주차장) ----------

    /** 차가 서 있는 학원 주차장. `adb emu geo fix 127.1240 37.3838` */
    val s2Parking = LatLng(37.3838, 127.1240)
    const val S2_PARKING_NAME = "서현 학원가 주차장"

    val s2Store = DemoStore("s2-a", "서현김밥", LatLng(37.3851, 127.1256), cookMin = 10, menus = menusOf("서현김밥", "s2"))

    /** 처음 한 번 등록해 둔 상태로 시작한다 (getCRNInfo는 "invalid"라 못 씀) */
    val vehicle = DemoVehicle(plate = "12가 3456", color = "흰색")

    /** 배달기사 출발 위치(약 800m 떨어진 곳). 이후 좌표는 adb broadcast로 보낸다 */
    val s2RiderStart = LatLng(37.3900, 127.1300)
}
