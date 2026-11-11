package ai.cardeli.starter.feature.route

import ai.cardeli.starter.feature.geo.Geo
import ai.cardeli.starter.feature.mock.DemoStore
import ai.cardeli.starter.feature.mock.PickupCandidate
import ai.cardeli.starter.feature.mock.PickupMeasure
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 수령 지점 고르기 (F7).
 *
 * 후보마다 내비 실측 추가 시간(detour)을 보고 가장 작은 곳을 고른다. 같으면 대기 시간이 짧은 곳,
 * 그것도 같으면 가게에서 가까운 곳. 가중치 점수는 쓰지 않는다.
 *
 * 대기 시간 = (조리 시간 + 기사가 가게에서 지점까지 가는 시간) − (차가 지점까지 가는 시간), 0보다 작으면 0.
 * 기사 이동 속도는 가정값(분당 250m ≈ 시속 15km, 오토바이 시내 근거리)이다.
 */
class PickupSelector(private val riderMPerMin: Double = 250.0) {

    data class Evaluated(
        val id: String,
        val name: String,
        val detourM: Int,
        val detourMin: Int,
        val etaMin: Double,     // 차가 지점까지
        val riderMin: Double,   // 기사가 가게에서 지점까지
        val waitMin: Int,
        val storeDistanceM: Int,
    )

    data class Selection(val chosen: Evaluated, val candidates: List<Evaluated>, val takeout: Evaluated?)

    fun choose(
        store: DemoStore,
        candidates: List<PickupCandidate>,
        measures: Map<String, PickupMeasure>,
    ): Selection? {
        val evaluated = candidates.mapNotNull { c -> measures[c.id]?.let { evaluate(store, c.id, c.name, Geo.distanceM(store.location, c.location), it) } }
        val chosen = evaluated.minWithOrNull(compareBy<Evaluated>({ it.detourMin }, { it.waitMin }, { it.storeDistanceM })) ?: return null
        // 비교용: 가게에 직접 들러 포장할 때 (기사 이동 없음)
        val takeout = measures["store-${store.id}"]?.let { evaluate(store, "store-${store.id}", "${store.name} 들러 포장", 0.0, it) }
        return Selection(chosen, evaluated, takeout)
    }

    private fun evaluate(store: DemoStore, id: String, name: String, storeDistM: Double, m: PickupMeasure): Evaluated {
        val etaMin = m.etaSec / 60.0
        val riderMin = storeDistM / riderMPerMin
        val wait = max(0.0, store.cookMin + riderMin - etaMin)
        return Evaluated(
            id = id,
            name = name,
            detourM = m.detourM,
            detourMin = (m.detourSec / 60.0).roundToInt(),
            etaMin = etaMin,
            riderMin = riderMin,
            waitMin = ceil(wait).toInt(),
            storeDistanceM = storeDistM.roundToInt(),
        )
    }
}

/** 계산 로그 JSON (영상 0:16~0:26 지도 애니메이션의 원본). 화면·영상 숫자는 이 로그에서만 가져온다 */
object PickupLog {
    fun toJson(store: DemoStore, s: PickupSelector.Selection, candidates: List<PickupCandidate>): String {
        fun e(v: PickupSelector.Evaluated): String {
            val loc = candidates.firstOrNull { it.id == v.id }?.location ?: store.location
            return "{\"id\":\"${v.id}\",\"name\":\"${v.name}\",\"lat\":${loc.lat},\"lng\":${loc.lng}," +
                "\"detourM\":${v.detourM},\"detourMin\":${v.detourMin},\"etaMin\":${"%.1f".format(java.util.Locale.ROOT, v.etaMin)}," +
                "\"riderMin\":${"%.1f".format(java.util.Locale.ROOT, v.riderMin)},\"waitMin\":${v.waitMin},\"storeDistanceM\":${v.storeDistanceM}}"
        }
        return "{\"store\":\"${store.name}\",\"cookMin\":${store.cookMin},\"chosen\":\"${s.chosen.id}\"," +
            "\"candidates\":[${s.candidates.joinToString(",") { e(it) }}]," +
            "\"takeout\":${s.takeout?.let { e(it) } ?: "null"}," +
            "\"source\":\"docs/poc/F7_pickup_measure_1111.json\"}"
    }
}
