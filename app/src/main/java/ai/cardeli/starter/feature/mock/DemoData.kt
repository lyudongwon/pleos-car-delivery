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
    val category: String = "김밥",
    val deliveryFee: Int = 2_300,
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
    val s1Destination = LatLng(37.274701, 127.11587)
    const val S1_DESTINATION_NAME = "기흥역"

    /**
     * S1 가게 4곳. 배달 가능 거리(경로에서 2km) 안 앞쪽인 A·B만 추천돼야 한다.
     * A는 수령 지점(경로 14.7km, 출발 약 17분 뒤)에서 1.1km·경로에서 1.1km, B는 경로에서 1km 떨어져 있다. 실제 배달처럼 가게가 경로 바로 옆이 아니다.
     * C는 출발점 뒤쪽(이미 지나친 방향), D는 경로에서 약 3.5km 떨어져 있어 걸러진다.
     */
    val s1Stores = listOf(
        DemoStore("s1-a", "길목김밥", LatLng(37.302420, 127.115447), cookMin = 10, menus = menusOf("길목김밥", "a")),
        DemoStore("s1-b", "모퉁이김밥", LatLng(37.280771, 127.127205), cookMin = 12, menus = menusOf("모퉁이김밥", "b")),
        DemoStore("s1-c", "시흥김밥", LatLng(37.415864, 127.091854), cookMin = 8, menus = menusOf("시흥김밥", "c")),
        DemoStore("s1-d", "먼길김밥", LatLng(37.346167, 127.141255), cookMin = 10, menus = menusOf("먼길김밥", "d")),
    )

    /** 지난번에 주문한 가게. 앱은 경로 근처 가게 중 이 가게를 먼저 제안한다(기획안 5-1). 주문 이력 저장은 만들지 않는다 */
    const val S1_LAST_ORDERED_STORE_ID = "s1-a"

    /** 길목김밥(s1-a)에서 기사가 경로까지 가져올 수령 후보 5곳(가게에서 0.8~1.8km). 정차구역 두 곳은 내비 경로 위(실측 추가 0), 주차장 세 곳은 경로에서 100~300m. 추가 시간은 F7에서 내비 실측으로 채운다 */
    val s1Pickups = listOf(
        PickupCandidate("p0", "길목 공영주차장", LatLng(37.297803, 127.105515), PickupType.PublicParking),
        PickupCandidate("p1", "길목사거리 정차구역", LatLng(37.294920, 127.107047), PickupType.StopZone),
        PickupCandidate("p2", "북쪽 상가 주차장", LatLng(37.297170, 127.109567), PickupType.PublicParking),
        PickupCandidate("p3", "건너편 공원 주차장", LatLng(37.287535, 127.108067), PickupType.PublicParking),
        PickupCandidate("p4", "남쪽 정차구역", LatLng(37.289623, 127.109114), PickupType.StopZone),
    )

    /**
     * 내비 실측값 (11/12, docs/poc/F7_pickup_measure_1112.json). S1 출발점 → 기흥역 경로 기준(내비 17km · 23분).
     * detour = (지점을 경유지로 넣은 경로) − (목적지만), eta = 현재 위치 → 지점(requestTrip).
     * 내비 값은 1km·60초 단위로 반올림돼 있다. 출발점이 아닌 곳에서는 맞지 않는다.
     */
    val s1Measures = mapOf(
        "store-s1-a" to PickupMeasure(detourM = 1_000, detourSec = 360, etaSec = 1_050),
        "p0" to PickupMeasure(detourM = 0, detourSec = 60, etaSec = 1_021),
        "p1" to PickupMeasure(detourM = 0, detourSec = 0, etaSec = 1_054),
        "p2" to PickupMeasure(detourM = 0, detourSec = 120, etaSec = 1_101),
        "p3" to PickupMeasure(detourM = 0, detourSec = 0, etaSec = 1_137),
        "p4" to PickupMeasure(detourM = 0, detourSec = 0, etaSec = 1_087),
    )

    // ---------- S2: 정차 중 주문 (학원 주차장) ----------

    /** 차가 서 있는 학원 주차장. `adb emu geo fix 127.1240 37.3838` */
    val s2Parking = LatLng(37.3838, 127.1240)
    const val S2_PARKING_NAME = "서현 학원가 주차장"

    val s2Store = DemoStore("s2-a", "서현김밥", LatLng(37.3851, 127.1256), cookMin = 10, menus = menusOf("서현김밥", "s2"))

    /** 정차 중 가게 목록. 거리·예상 시간은 앱이 지금 차 위치로 계산한다. 이름은 모두 가상 */
    val s2Stores = listOf(
        s2Store,
        DemoStore("s2-b", "바삭치킨", LatLng(37.3880, 127.1290), cookMin = 18, category = "치킨", deliveryFee = 3_000, menus = listOf(
            Menu("s2b-1", "바삭치킨", "후라이드치킨", 19_000), Menu("s2b-2", "바삭치킨", "양념치킨", 20_000), Menu("s2b-3", "바삭치킨", "치킨너겟", 6_000),
        )),
        DemoStore("s2-c", "모퉁이카페", LatLng(37.3842, 127.1268), cookMin = 5, category = "카페", deliveryFee = 2_000, menus = listOf(
            Menu("s2c-1", "모퉁이카페", "아메리카노", 3_000), Menu("s2c-2", "모퉁이카페", "카페라테", 3_800), Menu("s2c-3", "모퉁이카페", "샌드위치", 5_500),
        )),
        DemoStore("s2-d", "한그릇국밥", LatLng(37.3905, 127.1330), cookMin = 12, category = "한식", deliveryFee = 2_500, menus = listOf(
            Menu("s2d-1", "한그릇국밥", "돼지국밥", 9_000), Menu("s2d-2", "한그릇국밥", "순대국밥", 9_000), Menu("s2d-3", "한그릇국밥", "김치찌개", 8_500),
        )),
        DemoStore("s2-e", "언덕피자", LatLng(37.3990, 127.1420), cookMin = 20, category = "피자", deliveryFee = 3_000, menus = listOf(
            Menu("s2e-1", "언덕피자", "페퍼로니피자", 18_000), Menu("s2e-2", "언덕피자", "고구마피자", 21_000),
        )),
    )

    /** 처음 한 번 등록해 둔 상태로 시작한다 (getCRNInfo는 "invalid"라 못 씀) */
    val vehicle = DemoVehicle(plate = "12가 3456", color = "흰색")

    /** 배달기사 출발 위치(약 800m 떨어진 곳). 이후 좌표는 adb broadcast로 보낸다 */
    val s2RiderStart = LatLng(37.3900, 127.1300)
}
