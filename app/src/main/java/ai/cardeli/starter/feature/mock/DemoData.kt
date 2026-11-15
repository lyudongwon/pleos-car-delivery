package ai.cardeli.starter.feature.mock

import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.feature.geo.LatLng

/**
 * 시연 데이터. S1(주행 중) 가게는 가상 이름이고 좌표는 S1 시연 경로(DemoRoute) 기준이다.
 * S2(정차 중) 가게·메뉴는 화면 디자인(Figma)에 나온 가게를 그대로 쓴다.
 */
data class DemoStore(
    val id: String,
    val name: String,
    val location: LatLng,
    val cookMin: Int,          // 주문부터 음식이 나오기까지(분)
    val menus: List<Menu>,
    val category: String = "김밥",
    val deliveryFee: Int = 2_300,
    val minOrder: Int = 10_000,
    val rating: Double = 4.5,
    val reviewCount: Int = 0,
    val lastOrder: String = "22:20",
)

/** 주문내역에 보이는 지난 주문 (시연 데이터) */
data class DemoPastOrder(val id: String, val storeName: String, val summary: String, val dateLabel: String)

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

    // ---------- S2: 정차 중 주문 (화면 디자인 기준: 서울 서초구 헌릉로, 다옴 양재온누리점) ----------

    /** 차가 서 있는 곳. `adb emu geo fix 127.0390 37.4630`. 가게까지 거리는 앱이 이 좌표로 계산한다 */
    val s2Parking = LatLng(37.4630, 127.0390)
    const val S2_PLACE_LABEL = "서울 서초구 헌릉로"

    private fun m(id: String, store: String, name: String, price: Int, desc: String, reviews: Int) = Menu(id, store, name, price, desc, reviews)

    private const val DAOM = "다옴 양재온누리점"
    val s2Store = DemoStore(
        "s2-a", DAOM, LatLng(37.4750, 127.0290), cookMin = 13, category = "한식", deliveryFee = 2_300,
        minOrder = 10_000, rating = 4.6, reviewCount = 89,
        menus = listOf(
            m("s2-1", DAOM, "다옴김밥", 4_000, "신선한 채소와 달걀, 햄을 듬뿍 넣어 고소하고 담백한 맛을 살린 기본 김밥", 14),
            m("s2-2", DAOM, "참치김밥", 5_500, "고소한 참치마요와 아삭한 채소가 어우러져 부드럽고 풍성한 맛을 즐길 수 있는 김밥", 31),
            m("s2-3", DAOM, "고추참치김밥", 6_000, "매콤한 고추참치에 신선한 채소를 더해 감칠맛과 매운맛이 조화를 이루는 김밥", 11),
            m("s2-4", DAOM, "묵은지참치김밥", 6_000, "깊게 숙성된 묵은지의 새콤하고 아삭한 식감이 입맛을 돋우는 별미 김밥", 14),
            m("s2-5", DAOM, "날치알고추냉이김밥", 5_000, "톡톡 터지는 날치알과 알싸한 고추냉이가 만나 독특하고 산뜻한 풍미를 선사하는 김밥", 8),
        ),
    )

    private const val BHC = "BHC치킨 우면점"
    private const val GAMJA = "대한감자탕뼈해장국"
    private const val PALWOL = "팔월오일 커피"
    private const val SANGJI = "상지원"
    private const val LILY = "카페 릴리블랑"

    /** 정차 중 가게 목록. 거리·예상 시간은 앱이 지금 차 위치로 계산한다 */
    val s2Stores = listOf(
        DemoStore("s2-b", BHC, LatLng(37.4710, 127.0497), cookMin = 23, category = "패스트푸드", deliveryFee = 3_000,
            minOrder = 18_000, rating = 4.8, reviewCount = 121, menus = listOf(
                m("s2b-1", BHC, "뿌링클 콤보", 25_000, "뿌링뿌링 세상에 없던 마법의 맛 뿌링클, 퍽퍽한 닭가슴살 없이 윙, 봉, 닭다리만 담은 콤보", 14),
                m("s2b-2", BHC, "뿌링클", 21_000, "치즈와 요거트 시즈닝을 듬뿍 뿌린 BHC 대표 메뉴", 52),
                m("s2b-3", BHC, "맛초킹", 22_000, "달콤짭조름한 간장 소스에 청양고추를 더한 치킨", 27),
                m("s2b-4", BHC, "후라이드", 19_000, "바삭한 튀김옷의 기본 후라이드 치킨", 33),
            )),
        DemoStore("s2-c", GAMJA, LatLng(37.4530, 127.0504), cookMin = 15, category = "한식", deliveryFee = 2_500,
            minOrder = 10_000, rating = 4.3, reviewCount = 132, menus = listOf(
                m("s2c-1", GAMJA, "뼈해장국", 10_000, "푹 고아 낸 등뼈와 우거지를 넣은 얼큰한 해장국", 41),
                m("s2c-2", GAMJA, "감자탕(소)", 32_000, "두 사람이 먹기 좋은 감자탕", 18),
            )),
        s2Store,
        DemoStore("s2-d", PALWOL, LatLng(37.4500, 127.0310), cookMin = 8, category = "카페·디저트", deliveryFee = 2_000,
            minOrder = 12_000, rating = 4.3, reviewCount = 153, menus = listOf(
                m("s2d-1", PALWOL, "아메리카노", 4_000, "고소한 원두로 내린 아메리카노", 64),
                m("s2d-2", PALWOL, "카페라테", 4_800, "부드러운 우유와 에스프레소", 22),
                m("s2d-3", PALWOL, "크로플", 6_500, "바삭하게 구운 크로와상 와플", 19),
            )),
        DemoStore("s2-e", SANGJI, LatLng(37.4747, 127.0390), cookMin = 18, category = "중식", deliveryFee = 3_000,
            minOrder = 18_000, rating = 4.8, reviewCount = 64, menus = listOf(
                m("s2e-1", SANGJI, "짜장면", 8_000, "춘장을 볶아 만든 기본 짜장면", 30),
                m("s2e-2", SANGJI, "짬뽕", 10_000, "해산물을 넣은 얼큰한 짬뽕", 21),
                m("s2e-3", SANGJI, "탕수육", 18_000, "바삭하게 튀긴 등심 탕수육", 17),
            )),
        DemoStore("s2-f", LILY, LatLng(37.4630, 127.0243), cookMin = 10, category = "카페·디저트", deliveryFee = 2_500,
            minOrder = 18_000, rating = 4.5, reviewCount = 173, menus = listOf(
                m("s2f-1", LILY, "딸기 생크림 케이크", 7_500, "생딸기와 생크림을 올린 케이크", 40),
                m("s2f-2", LILY, "바닐라 라테", 5_500, "바닐라 시럽을 더한 라테", 12),
            )),
    )

    /** 주문내역의 지난 주문 (화면 디자인 기준) */
    val pastOrders = listOf(
        DemoPastOrder("past-1", "엘빙할루카", "생망고 케이크 외 2개", "어제 12:45"),
        DemoPastOrder("past-2", "올리앤 양재점", "상하이 파스타 외 1개", "11/11 (수)"),
        DemoPastOrder("past-3", "메가MGC커피 서초양재점", "(ICE)아메리카노 외 2개", "11.09 (월)"),
        DemoPastOrder("past-4", "호랑이초밥 양재점", "런치호랑이특(12p)", "11.05 (목)"),
    )

    /** 처음 한 번 등록해 둔 상태로 시작한다 (getCRNInfo는 "invalid"라 못 씀) */
    val vehicle = DemoVehicle(plate = "123저4567", color = "회색")

    /** 배달기사 출발 위치(약 850m 떨어진 곳). 이후 좌표는 adb broadcast로 보낸다 */
    val s2RiderStart = LatLng(37.4690, 127.0450)
}
