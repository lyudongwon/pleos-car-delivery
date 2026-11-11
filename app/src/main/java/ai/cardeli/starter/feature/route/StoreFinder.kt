package ai.cardeli.starter.feature.route

import ai.cardeli.starter.feature.geo.Geo
import ai.cardeli.starter.feature.geo.LatLng
import ai.cardeli.starter.feature.mock.DemoStore

/**
 * 경로 근처 가게 추천 (F6). 지원서가 내세우는 차별점이라 고정값으로 대신하지 않고 실제로 계산한다.
 *
 * 남은 경로에서 [maxOffsetM] 안이고, 지금 위치보다 [minAheadM] 넘게 앞쪽인 가게만 남겨
 * 먼저 지나가는 순서로 고른다. 이미 지나친 가게, 경로에서 먼 가게는 빠진다.
 */
class StoreFinder(
    private val route: List<LatLng>,
    private val stores: List<DemoStore>,
    private val maxOffsetM: Double = 500.0,
    private val minAheadM: Double = 100.0,
) {
    private val cum = Geo.cumulativeM(route)

    data class Found(val store: DemoStore, val aheadM: Double, val offsetM: Double)

    fun find(here: LatLng, category: String?, limit: Int = 2): List<Found> {
        val hereAlong = Geo.project(here, route, cum).alongM
        return stores
            .filter { category == null || matches(it, category) }
            .map { s -> Geo.project(s.location, route, cum).let { p -> Found(s, p.alongM - hereAlong, p.offsetM) } }
            .filter { it.offsetM <= maxOffsetM && it.aheadM > minAheadM }
            .sortedBy { it.aheadM }
            .take(limit)
    }

    private fun matches(store: DemoStore, category: String) =
        category in store.name || store.menus.any { category in it.name }
}
