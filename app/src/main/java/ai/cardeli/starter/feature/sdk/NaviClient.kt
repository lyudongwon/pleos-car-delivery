package ai.cardeli.starter.feature.sdk

import ai.cardeli.starter.feature.geo.LatLng
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 내비 연동 (F8). Pleos NaviHelper.
 *
 * 조회 메서드는 반환값이 없고 결과가 리스너로 온다. 그래서 여기서 요청과 응답을 짝지어 suspend 함수로 바꾼다.
 * 주의(docs/sdk_check.md): 경로를 바꾸면 onRouteCancelled 뒤 재탐색(약 5.5초)이 끝날 때까지 남은 거리가 0으로 온다.
 * getWaypointInfo는 좌표를 0,0으로 주므로 넣은 경유지 좌표는 [waypoint]에 따로 저장한다.
 */
class NaviClient(context: Context) {

    data class Remain(val distanceM: Int, val timeSec: Int)

    private val navi = NaviHelper(context)
    private val ready = CompletableDeferred<Boolean>()
    private val destinations = MutableSharedFlow<DestinationInfo>(extraBufferCapacity = 8)
    private val cancelled = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val trips = MutableSharedFlow<TripInfo>(extraBufferCapacity = 8)

    /** 앱이 넣은 경유지 (이름, 좌표) */
    var waypoint: Pair<String, LatLng>? = null
        private set

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(info: DestinationInfo) {
            destinations.tryEmit(info)
        }
        override fun onRouteCancelled() {
            cancelled.tryEmit(Unit)
        }
        override fun onError(code: NaviErrorCode) {
            Log.w(TAG, "onError $code")
        }
        override fun onWaypointChanged(info: WaypointChangedInfo) {
            Log.i(TAG, "onWaypointChanged ${info.index} ${info.cmdType}")
        }
        override fun onWayPointArrived(info: WaypointArrivedInfo) {
            Log.i(TAG, "onWayPointArrived ${info.name}")
        }
        override fun onNavigationStatus(status: NavigationStatus) {}
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {}
        override fun onRouteStarted(info: RouteStartInfo) {}
        override fun onRoutingCompleted() {}
        override fun onRouteStateInfo(info: RouteStateInfo) {}
        override fun onWayPointInfo(list: List<WaypointInfo>) {}
        override fun onDestinationArrived(info: DestinationArrivedInfo) {}
        override fun onTripInfo(info: TripInfo) {
            trips.tryEmit(info)
        }
        override fun onReRouteEnded() {}
    }

    init {
        try {
            navi.initialize({
                navi.addListener(listener)
                ready.complete(true)
            }, { e ->
                Log.w(TAG, "NaviHelper 초기화 실패 ${e.message}")
                ready.complete(false)
            })
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper 초기화 예외", e)
            ready.complete(false)
        }
    }

    private suspend fun isReady() = withTimeoutOrNull(5_000) { ready.await() } ?: false

    /** 목적지까지 남은 거리·시간. 안내 중이 아니거나 재탐색 중이면 null */
    suspend fun remain(): Remain? {
        if (!isReady()) return null
        val info = withTimeoutOrNull(3_000) {
            awaitAfter(destinations) { navi.getDestinationInfo() }
        } ?: return null
        return if (info.remainDistance > 0) Remain(info.remainDistance, info.remainTime) else null
    }

    /** 재탐색이 끝나 남은 거리가 다시 0보다 커질 때까지 1초마다 묻는다 */
    private suspend fun awaitRoute(timeoutMs: Long): Remain? = withTimeoutOrNull(timeoutMs) {
        var r: Remain? = null
        while (r == null) {
            delay(1_000)
            r = remain()
        }
        r
    }

    /**
     * 목적지만으로 안내를 시작한다 (시연에서 운전자가 목적지를 찍은 상태를 만든다).
     * 에뮬레이터 재부팅 직후에는 내비 엔진이 덜 떠서 첫 요청이 경로를 못 잡는다(11/12 콜드 부팅에서 확인).
     * 그래서 경로가 안 잡히면 [attempts]번까지 다시 요청한다.
     */
    suspend fun startRoute(dest: LatLng, name: String, attempts: Int = 3): Remain? {
        if (!isReady()) return null
        waypoint = null
        repeat(attempts) { i ->
            withTimeoutOrNull(5_000) {
                awaitAfter(cancelled) { navi.requestRoute(RouteInfo(dest.lat, dest.lng, null, null, name, "", "", "", RouteOption.RECOMMENDED, "", false)) }
            }
            awaitRoute(20_000)?.let { return it }
            Log.w(TAG, "안내 시작 실패 (${i + 1}/$attempts), 다시 요청")
            delay(3_000)
        }
        return null
    }

    /** 경유지(FIRST)를 넣고, 재탐색이 끝난 뒤의 남은 거리·시간을 돌려준다 */
    suspend fun addWaypoint(p: LatLng, name: String): Remain? {
        if (!isReady()) return null
        waypoint = name to p
        withTimeoutOrNull(5_000) {
            awaitAfter(cancelled) { navi.addWaypoint(RequestWaypointInfo(p.lat, p.lng, name, WaypointIndex.FIRST, "", "", "", "")) }
        }
        return awaitRoute(20_000)
    }

    /** 넣었던 경유지(FIRST)를 빼고, 원래 목적지만으로 다시 잡힌 경로의 남은 거리·시간을 돌려준다 (F13) */
    suspend fun removeWaypoint(): Remain? {
        if (!isReady() || waypoint == null) return null
        withTimeoutOrNull(5_000) { awaitAfter(cancelled) { navi.removeWaypoint(WaypointIndex.FIRST) } }
        waypoint = null
        return awaitRoute(20_000)
    }

    /**
     * 목적지 + 경유지(지점) 하나로 경로를 새로 요청해 남은 거리·시간을 잰다 (F7 실측).
     * 0단계에서 확인한 방식: 경유지를 바꿔 끼우면 첫 번째만 재지고, 경로를 새로 요청해야 매번 재진다.
     */
    suspend fun measureVia(dest: LatLng, destName: String, point: LatLng, pointName: String): Remain? {
        if (!isReady()) return null
        val req = RouteWithWaypointsRequest(
            RouteInfo(dest.lat, dest.lng, null, null, destName, "", "", "", RouteOption.RECOMMENDED, "", false),
            listOf(RequestWaypointInfo(point.lat, point.lng, pointName, WaypointIndex.FIRST, "", "", "", "")),
        )
        withTimeoutOrNull(5_000) { awaitAfter(cancelled) { navi.requestRouteWithWaypoints(req) } }
        return awaitRoute(20_000)
    }

    /** 현재 위치에서 지점까지 걸리는 시간(초). requestTrip */
    suspend fun tripEtaSec(point: LatLng, name: String): Int? {
        if (!isReady()) return null
        return withTimeoutOrNull(5_000) {
            awaitAfter(trips) { navi.requestTrip(TripRequest(point.lat, point.lng, "", name, "")) }
        }?.eta?.toInt()
    }

    /** 콜백을 놓치지 않게 먼저 구독하고 요청한다 */
    private suspend fun <T> awaitAfter(flow: MutableSharedFlow<T>, request: () -> Unit): T = coroutineScope {
        val result = async(start = CoroutineStart.UNDISPATCHED) { flow.first() }
        request()
        result.await()
    }

    fun release() {
        try {
            navi.removeListener(listener)
            navi.release()
        } catch (_: Exception) {}
    }

    private companion object {
        const val TAG = "NaviClient"
    }
}
