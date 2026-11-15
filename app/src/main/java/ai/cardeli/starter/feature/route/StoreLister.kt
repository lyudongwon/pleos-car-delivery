package ai.cardeli.starter.feature.route

import ai.cardeli.starter.contract.ChargingInfo
import ai.cardeli.starter.contract.StoreCard
import ai.cardeli.starter.feature.geo.Geo
import ai.cardeli.starter.feature.geo.LatLng
import ai.cardeli.starter.feature.mock.DemoStore
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 정차 중 가게 목록 (F9·F16).
 * 예상 시간 = 조리 시간 + 기사 이동(가게 → 차, 분당 [riderMPerMin]m 가정). 수령 지점 계산(PickupSelector)과 같은 가정이다.
 * 충전 중이면 충전이 끝나기 전에 받을 수 있는 가게를 앞에 두고, 나머지는 가까운 순.
 */
class StoreLister(private val riderMPerMin: Double = 250.0) {

    fun etaMin(store: DemoStore, to: LatLng): Int =
        store.cookMin + ceil(Geo.distanceM(store.location, to) / riderMPerMin).toInt()

    fun cards(stores: List<DemoStore>, here: LatLng, charging: ChargingInfo?): List<StoreCard> {
        val remain = charging?.remainMin
        return stores.map { s ->
            val eta = etaMin(s, here)
            StoreCard(
                id = s.id,
                name = s.name,
                category = s.category,
                distanceM = Geo.distanceM(s.location, here).roundToInt(),
                etaMin = eta,
                deliveryFee = s.deliveryFee,
                beforeChargeEnds = remain == null || eta <= remain,
            )
        }.sortedWith(compareByDescending<StoreCard> { it.beforeChargeEnds }.thenBy { it.distanceM })
    }
}

/**
 * 충전 상태 추정 (F16). 충전구가 열린 상태에서 배터리 잔량이 오르면 충전 중으로 본다.
 * 남은 시간은 충전 중 잔량이 오른 속도로 [targetPercent]까지 걸리는 시간을 계산한다.
 */
class ChargeEstimator(private val targetPercent: Int = 80) {

    private var first: Pair<Long, Int>? = null   // 충전구가 열린 뒤 처음 본 (시각, 잔량)
    private var last: Pair<Long, Int>? = null
    private var portOpen = false

    fun reset() {
        first = null
        last = null
        portOpen = false
    }

    /** 새 신호를 넣고 지금 충전 중이면 상태를, 아니면 null을 돌려준다 */
    fun update(nowMs: Long, portOpen: Boolean, soc: Int?): ChargingInfo? {
        if (!portOpen) {
            reset()
            return null
        }
        if (!this.portOpen) {
            this.portOpen = true
            first = soc?.let { nowMs to it }
            last = first
            return null
        }
        if (soc != null) {
            if (first == null) first = nowMs to soc
            last = nowMs to soc
        }
        val f = first ?: return null
        val l = last ?: return null
        if (l.second <= f.second) return null   // 아직 오르지 않았다
        val perMin = (l.second - f.second) / ((l.first - f.first) / 60_000.0)
        val remain = if (perMin > 0 && l.second < targetPercent) ceil((targetPercent - l.second) / perMin).toInt() else 0
        return ChargingInfo(socPercent = l.second, remainMin = remain)
    }
}
