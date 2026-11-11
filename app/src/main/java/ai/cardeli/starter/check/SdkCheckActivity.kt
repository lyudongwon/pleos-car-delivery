package ai.cardeli.starter.check

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
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
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.tts.constant.Mode as TtsMode

/**
 * 기획서 "직접 확인할 것" 문서의 에뮬레이터 확인 항목을 차례로 돌리는 시험 화면.
 * 1) FusedLocation  2) NaviHelper 경로·경유지·Trip  3) TTS(on-device)  4) STT(on-device)
 * 결과는 화면과 logcat(PleosCheck)에 남는다.
 */
class SdkCheckActivity : Activity() {

    private val tag = "PleosCheck"
    private val main = Handler(Looper.getMainLooper())
    private val lines = mutableListOf<String>()
    private lateinit var text: TextView

    private lateinit var navi: NaviHelper
    private lateinit var fused: FusedLocation
    private var tts: TextToSpeech? = null
    private var stt: SpeechToText? = null

    private var here: Pair<Double, Double>? = null
    private var routeRequested = false
    private var waypointsAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = TextView(this).apply { textSize = 18f; typeface = Typeface.MONOSPACE; setPadding(32, 32, 32, 32) }
        setContentView(ScrollView(this).apply { addView(text) })
        log("시작")
        when (intent.getStringExtra("test")) {
            "P" -> { checkProximity(); return }
            "S" -> { checkSttFromFile(); return }
        }
        checkFused()
        checkNavi()
        main.postDelayed({ checkTts() }, 2000)
        main.postDelayed({ checkStt() }, 6000)
    }

    // ---------- 1. FusedLocation ----------
    private fun checkFused() {
        try {
            fused = FusedLocation(this)
            fused.initialize()
            main.postDelayed({
                try {
                    log("[Fused] isSupported = ${fused.isSupported()}")
                    fused.getLastKnownLocation(object : FusedLocationListener {
                        override fun onChanged(data: LocationData?) = log("[Fused] lastKnown = $data")
                        override fun onError(e: Exception) = log("[Fused] lastKnown 실패: ${e.javaClass.simpleName}: ${e.message}")
                    })
                } catch (e: Exception) { log("[Fused] 실패: ${e.javaClass.simpleName}: ${e.message}") }
            }, 1500)
        } catch (e: Exception) { log("[Fused] initialize 실패: ${e.javaClass.simpleName}: ${e.message}") }
    }

    // ---------- 2. NaviHelper ----------
    private val naviListener = object : NaviHelperEventListener {
        override fun onNavigationStatus(status: NavigationStatus) = log("[Navi] onNavigationStatus = $status")
        override fun onError(code: NaviErrorCode) = log("[Navi] onError = $code")
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            log("[Navi] onCurrentLocationInfo = ${info.latitude}, ${info.longitude}, heading ${info.heading}, '${info.address}', routeId=${info.routeId}, idx=${info.currentRouteIndex}")
            if (here == null) { here = info.latitude to info.longitude; startRoute() }
        }
        override fun onRouteStarted(info: RouteStartInfo) { log("[Navi] onRouteStarted = $info"); afterRoute() }
        override fun onRoutingCompleted() { log("[Navi] onRoutingCompleted"); afterRoute() }
        override fun onRouteStateInfo(info: RouteStateInfo) = log("[Navi] onRouteStateInfo = $info")
        override fun onDestinationInfo(info: DestinationInfo) = log("[Navi] onDestinationInfo = ${info.name} ${info.latitude},${info.longitude} 남은 ${info.remainDistance}m ${info.remainTime}s" +
            if (intent.getStringExtra("test") in setOf("T", "M")) " | [T] $tLabel dest ${info.remainDistance}m ${info.remainTime}s" else "")
        override fun onWaypointChanged(info: WaypointChangedInfo) = log("[Navi] onWaypointChanged = ${info.index} ${info.cmdType} ${info.routeStartInfo}")
        override fun onWayPointInfo(list: List<WaypointInfo>) = log("[Navi] onWayPointInfo (${list.size}개) = " + list.joinToString { "${it.name} (${it.latitude},${it.longitude}) 남은 ${it.remainDistance}m ${it.remainTime}s" })
        override fun onWayPointArrived(info: WaypointArrivedInfo) = log("[Navi] onWayPointArrived = ${info.index} ${info.name} ${info.distance}m ${info.duration}s")
        override fun onDestinationArrived(info: DestinationArrivedInfo) = log("[Navi] onDestinationArrived = $info")
        override fun onTripInfo(info: TripInfo) = log("[Navi] onTripInfo = [T] $tLabel trip 거리 ${info.distance}m, eta ${info.eta}, routePoints ${info.summary?.routePoints?.size}개: " +
            (info.summary?.routePoints?.joinToString { "${it.pointType}(${it.latitude},${it.longitude}, ratio ${it.ratioOnRoute})" } ?: "null") +
            ", traffic ${info.summary?.trafficConditions?.size}개")
        override fun onRouteCancelled() = log("[Navi] onRouteCancelled")
        override fun onReRouteEnded() = log("[Navi] onReRouteEnded")
    }

    private fun checkNavi() {
        navi = NaviHelper(this)
        navi.initialize({
            log("[Navi] initialize 성공")
            navi.addListener(naviListener)
            navi.getCurrentLocationInfo()
            navi.getRouteStateInfo()
            main.postDelayed({ if (here == null) { log("[Navi] 현재 위치 응답 없음 → 서울시청 기준으로 진행"); here = 37.5665 to 126.9780; startRoute() } }, 4000)
        }, { e -> log("[Navi] initialize 실패: ${e.javaClass.simpleName}: ${e.message}") })
    }

    private fun startRoute() {
        if (routeRequested) return
        routeRequested = true
        val (lat, lng) = here!!
        val dLat = lat + 0.03; val dLng = lng + 0.03   // 북동쪽 약 4km
        val dest = RouteInfo(dLat, dLng, null, null, "시험 목적지", "", "", "", RouteOption.RECOMMENDED, "", false)
        if (intent.getStringExtra("test") == "T") { measureCandidates(lat, lng, dest); return }
        if (intent.getStringExtra("test") == "M") { measureDemoPickups(); return }
        if (intent.getStringExtra("test") == "D") {
            // 시험 D: 실제 흐름. 목적지만 잡고 안내가 시작된 뒤, 충분히 기다렸다가 만남 지점을 한 번 추가
            log("[Navi] D) requestRoute → 목적지만")
            navi.requestRoute(dest)
            main.postDelayed({ log("[Navi] D) addWaypoint FIRST 만남지점 (안내 시작 10초 뒤)"); navi.addWaypoint(RequestWaypointInfo(lat + 0.015, lng + 0.015, "만남지점", WaypointIndex.FIRST, "", "", "", "")) }, 15000)
            main.postDelayed({ log("[Navi] D) 15초 뒤 조회"); navi.getWaypointInfo(); navi.getDestinationInfo() }, 30000)
            main.postDelayed({ log("[Navi] D) 30초 뒤 조회"); navi.getWaypointInfo(); navi.getDestinationInfo() }, 45000)
            return
        }
        // 시험 A: 목적지 + 경유지 2개를 한 번에
        log("[Navi] A) requestRouteWithWaypoints → 목적지 ($dLat, $dLng) + 경유지 2개")
        try {
            navi.requestRouteWithWaypoints(ai.pleos.playground.navi.data.RouteWithWaypointsRequest(dest, listOf(
                RequestWaypointInfo(lat + 0.01, lng + 0.01, "경유1", WaypointIndex.FIRST, "", "", "", ""),
                RequestWaypointInfo(lat + 0.02, lng + 0.02, "경유2", WaypointIndex.SECOND, "", "", "", ""))))
        } catch (e: Exception) { log("[Navi] requestRouteWithWaypoints 실패: ${e.javaClass.simpleName}: ${e.message}") }
        main.postDelayed({ log("[Navi] A) 결과 조회"); navi.getWaypointInfo(); navi.getDestinationInfo() }, 7000)
        // 시험 B: 경유지 하나 지우고 새로 넣기 (주행 중 만남 지점 추가 흐름)
        main.postDelayed({ log("[Navi] B) removeWaypoint SECOND"); try { navi.removeWaypoint(WaypointIndex.SECOND) } catch (e: Exception) { log("[Navi] removeWaypoint 실패: ${e.message}") } }, 10000)
        main.postDelayed({ log("[Navi] B) addWaypoint SECOND 만남지점"); try { navi.addWaypoint(RequestWaypointInfo(lat + 0.015, lng + 0.015, "만남지점", WaypointIndex.SECOND, "", "", "", "")) } catch (e: Exception) { log("[Navi] addWaypoint 실패: ${e.message}") } }, 14000)
        main.postDelayed({ log("[Navi] B) 결과 조회"); navi.getWaypointInfo(); navi.getDestinationInfo(); navi.getRouteStateInfo() }, 19000)
        // 시험 C: 지점까지 예상 거리·시간
        main.postDelayed({ log("[Navi] C) requestTrip → 만남지점"); try { navi.requestTrip(TripRequest(lat + 0.015, lng + 0.015, "", "만남지점", "")) } catch (e: Exception) { log("[Navi] requestTrip 실패: ${e.message}") } }, 22000)
    }

    private fun afterRoute() {}

    /**
     * 시험 M (F7 실측): S1 시연 경로(시흥동 → 기흥역)에서 '가게 들러 포장'과 수령 후보 5곳의
     * 추가 거리·시간을 내비로 잰다. 후보마다 경로를 새로 요청한다(0단계에서 확인한 방식).
     * 같은 지점에 requestTrip도 불러 현재 위치 → 지점 도착 시간(대기 시간 계산용)을 받는다.
     * 로그: [M] base / [M] id=... dest / [M] id=... trip
     */
    private fun measureDemoPickups() {
        val d = ai.cardeli.starter.feature.mock.DemoData
        val dest = RouteInfo(d.s1Destination.lat, d.s1Destination.lng, null, null, d.S1_DESTINATION_NAME, "", "", "", RouteOption.RECOMMENDED, "", false)
        val store = d.s1Stores.first()
        val points = listOf("store-${store.id}" to store.location) + d.s1Pickups.map { it.id to it.location }
        log("[M] requestRoute → ${d.S1_DESTINATION_NAME}. 지점 ${points.size}개")
        navi.requestRoute(dest)
        main.postDelayed({ tLabel = "M base"; navi.getDestinationInfo() }, 12_000)
        points.forEachIndexed { i, (id, p) ->
            val t0 = 15_000L + i * 16_000L
            main.postDelayed({
                log("[M] id=$id requestRouteWithWaypoints (${p.lat},${p.lng})")
                navi.requestRouteWithWaypoints(ai.pleos.playground.navi.data.RouteWithWaypointsRequest(dest,
                    listOf(RequestWaypointInfo(p.lat, p.lng, id, WaypointIndex.FIRST, "", "", "", ""))))
            }, t0)
            main.postDelayed({ tLabel = "M id=$id"; navi.getDestinationInfo() }, t0 + 12_000)
            main.postDelayed({ tLabel = "M id=$id"; navi.requestTrip(TripRequest(p.lat, p.lng, "", id, "")) }, t0 + 14_000)
        }
    }

    /**
     * 시험 T (0단계 PoC): 후보 지점마다 경유지를 바꿔 넣어 목적지까지 남은 거리·시간이 얼마나 늘어나는지 잰다.
     * 같은 지점에 requestTrip도 불러, 현재 위치 → 후보 거리·시간을 따로 받는지 본다.
     * 로그: [T] base / [T] cand=i dest / [T] cand=i trip
     */
    private var tLabel = "base"
    private fun measureCandidates(lat: Double, lng: Double, dest: RouteInfo) {
        val cands = listOf(0.2 to 0.0, 0.4 to 0.004, 0.5 to -0.004, 0.6 to 0.0, 0.8 to 0.006)
            .map { (r, off) -> (lat + 0.03 * r + off) to (lng + 0.03 * r - off) }
        log("[T] requestRoute → 목적지만. 후보 ${cands.size}개: " + cands.joinToString { "(%.5f,%.5f)".format(it.first, it.second) })
        navi.requestRoute(dest)
        main.postDelayed({ tLabel = "base"; navi.getDestinationInfo() }, 12_000)
        if (intent.getStringExtra("mode") == "fresh") {
            // 후보마다 경로를 새로 요청: 목적지 + 경유지(후보) 하나
            cands.forEachIndexed { i, (cLat, cLng) ->
                val t0 = 15_000L + i * 15_000L
                main.postDelayed({
                    log("[T] cand=$i requestRouteWithWaypoints ($cLat,$cLng)")
                    navi.requestRouteWithWaypoints(ai.pleos.playground.navi.data.RouteWithWaypointsRequest(dest,
                        listOf(RequestWaypointInfo(cLat, cLng, "후보$i", WaypointIndex.FIRST, "", "", "", ""))))
                }, t0)
                main.postDelayed({ tLabel = "cand=$i"; navi.getDestinationInfo() }, t0 + 12_000)
            }
            return
        }
        cands.forEachIndexed { i, (cLat, cLng) ->
            val t0 = 15_000L + i * 16_000L
            main.postDelayed({
                log("[T] cand=$i addWaypoint FIRST ($cLat,$cLng)")
                navi.addWaypoint(RequestWaypointInfo(cLat, cLng, "후보$i", WaypointIndex.FIRST, "", "", "", ""))
            }, t0)
            main.postDelayed({ tLabel = "cand=$i"; navi.getDestinationInfo(); navi.getWaypointInfo() }, t0 + 11_000)
            main.postDelayed({ tLabel = "cand=$i"; navi.requestTrip(TripRequest(cLat, cLng, "", "후보$i", "")) }, t0 + 13_000)
        }
    }

    // ---------- 3. TTS (on-device, 등록 없이) ----------
    private fun checkTts() {
        try {
            val t = TextToSpeech(this, TtsMode.ON_DEVICE)
            tts = t
            val ev = object : EventListener {
                override fun onStart() = log("[TTS] onStart")
                override fun onReady() = log("[TTS] onReady")
                override fun onDone() = log("[TTS] onDone")
                override fun onError(e: ai.pleos.playground.tts.constant.ErrorType) = log("[TTS] onError = $e")
                override fun onStop() = log("[TTS] onStop")
                override fun onUpdatedRms(rms: Double) {}
            }
            t.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                override fun onSuccess() { log("[TTS] initialize 성공 → speak"); t.addEventListener(ev); t.speak("만남 지점을 경로에 추가했습니다", false) }
                override fun onFailure(e: ai.pleos.playground.tts.constant.ErrorType) = log("[TTS] initialize 실패 = $e")
            })
        } catch (e: Exception) { log("[TTS] 실패: ${e.javaClass.simpleName}: ${e.message}") }
    }

    // ---------- 4. STT (on-device, 등록 없이) ----------
    private fun checkStt() {
        try {
            val s = SpeechToText(this, SttMode.ON_DEVICE)
            stt = s
            s.addListener(object : ResultListener {
                override fun onUpdated(text: String, isFinal: Boolean) = log("[STT] onUpdated '$text' final=$isFinal")
                override fun onUpdatedEpdData(on: Long, off: Long) {}
                override fun onStartedRecognition() = log("[STT] onStartedRecognition")
                override fun onEndedRecognition() = log("[STT] onEndedRecognition")
                override fun onError(e: ai.pleos.playground.stt.constant.ErrorType) = log("[STT] onError = $e")
                override fun onReady() = log("[STT] onReady")
                override fun onUpdatedRms(rms: Double) {}
            })
            s.initialize(object : ai.pleos.playground.stt.listener.OnInitializeListener {
                override fun onSuccess() { log("[STT] initialize 성공 → request"); s.request() }
                override fun onFailure(e: ai.pleos.playground.stt.constant.ErrorType) = log("[STT] initialize 실패 = $e")
            })
        } catch (e: Exception) { log("[STT] 실패: ${e.javaClass.simpleName}: ${e.message}") }
    }

    // ---------- 5. FusedLocation 근접 알림 (만남 지점 반경 100m) ----------
    private fun checkProximity() {
        fused = FusedLocation(this)
        fused.initialize()
        main.postDelayed({
            log("[Prox] isSupported = ${fused.isSupported()}")
            fused.registerFusedLocationCallback(object : FusedLocationListener {
                override fun onChanged(data: LocationData?) = log("[Prox] 위치 ${data?.latitude}, ${data?.longitude}")
                override fun onError(e: Exception) = log("[Prox] 위치 오류 ${e.message}")
            })
            val req = ai.pleos.playground.location.fused.model.ProximityAlertRequest(37.4281, 127.1095, 100f,
                setOf(ai.pleos.playground.location.fused.model.ProximityTransition.ENTER, ai.pleos.playground.location.fused.model.ProximityTransition.EXIT))
            fused.addProximityAlert(req, object : ai.pleos.playground.location.fused.listener.ProximityAlertListener {
                override fun onRegistered(id: String) = log("[Prox] 등록됨 id=$id (만남 지점 37.4281,127.1095 반경 100m)")
                override fun onTransition(e: ai.pleos.playground.location.fused.model.ProximityAlertEvent) = log("[Prox] ★ onTransition = ${e.transition} (id=${e.alertId})")
                override fun onError(e: Exception) = log("[Prox] 등록 오류 ${e.javaClass.simpleName}: ${e.message}")
            })
        }, 1500)
    }

    // ---------- 6. STT: 녹음 파일(16kHz 16bit mono PCM)을 넣어 실제 인식 ----------
    private fun checkSttFromFile() {
        val s = SpeechToText(this, SttMode.ON_DEVICE)
        stt = s
        s.initialize(object : ai.pleos.playground.stt.listener.OnInitializeListener {
            override fun onSuccess() {
                log("[STT-file] initialize 성공")
                s.addListener(object : ResultListener {
                    override fun onUpdated(text: String, isFinal: Boolean) = log("[STT-file] ★ onUpdated '$text' final=$isFinal")
                    override fun onUpdatedEpdData(on: Long, off: Long) = log("[STT-file] epd $on~$off")
                    override fun onStartedRecognition() = log("[STT-file] onStartedRecognition")
                    override fun onEndedRecognition() = log("[STT-file] onEndedRecognition")
                    override fun onError(e: ai.pleos.playground.stt.constant.ErrorType) = log("[STT-file] onError = $e")
                    override fun onReady() = log("[STT-file] onReady")
                    override fun onUpdatedRms(rms: Double) {}
                })
                s.setLocale(java.util.Locale.KOREA)
                Thread {
                    try {
                        val pcm = assets.open("order.pcm").use { it.readBytes() }
                        log("[STT-file] 음성 ${pcm.size}바이트(${pcm.size / 32000.0}초) 전송 시작: '가는 길에 받을 수 있는 국밥 시켜줘'")
                        pcm.asList().chunked(3200).forEach { s.sendAudio(it.toByteArray()); Thread.sleep(100) }
                        s.completeAudioSend()
                        log("[STT-file] completeAudioSend")
                    } catch (e: Exception) { log("[STT-file] 전송 실패: ${e.javaClass.simpleName}: ${e.message}") }
                }.start()
            }
            override fun onFailure(e: ai.pleos.playground.stt.constant.ErrorType) = log("[STT-file] initialize 실패 = $e")
        })
    }

    private fun log(s: String) {
        Log.i(tag, s)
        main.post { lines += s; text.text = lines.joinToString("\n") }
    }

    override fun onDestroy() {
        try { navi.removeListener(naviListener); navi.release() } catch (_: Exception) {}
        try { fused.release() } catch (_: Exception) {}
        try { tts?.release() } catch (_: Exception) {}
        try { stt?.release() } catch (_: Exception) {}
        super.onDestroy()
    }
}
