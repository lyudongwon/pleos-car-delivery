package ai.cardeli.starter.feature

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.VoiceState
import ai.cardeli.starter.feature.demo.DemoBus
import ai.cardeli.starter.feature.demo.DemoCommand
import ai.cardeli.starter.feature.demo.DemoSnapshot
import ai.cardeli.starter.feature.geo.Geo
import ai.cardeli.starter.feature.mock.DemoData
import ai.cardeli.starter.feature.mock.MockData
import ai.cardeli.starter.feature.sdk.DrivingStateSource
import ai.cardeli.starter.feature.sdk.Listener
import ai.cardeli.starter.feature.sdk.LocationSource
import ai.cardeli.starter.feature.sdk.NaviClient
import ai.cardeli.starter.feature.sdk.VehicleInfo
import ai.cardeli.starter.feature.geo.LatLng
import ai.cardeli.starter.feature.route.StoreFinder
import ai.cardeli.starter.feature.route.PickupSelector
import ai.cardeli.starter.feature.route.PickupLog
import ai.cardeli.starter.feature.route.RiderTracker
import ai.cardeli.starter.feature.mock.DemoStore
import ai.cardeli.starter.contract.PickupPoint
import ai.cardeli.starter.contract.DeliveryInfo
import ai.cardeli.starter.feature.sdk.Speaker
import ai.cardeli.starter.feature.voice.OrderParser
import ai.cardeli.starter.feature.voice.VoiceOrderFlow
import ai.cardeli.starter.feature.mock.DemoRoute
import ai.cardeli.starter.contract.StoreSuggestion
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

class OrderViewModel(private val app: Application) : AndroidViewModel(app) {

    private val initialState = OrderUiState(menus = MockData.menus)
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    private val speaker = Speaker(app)
    private val listener = Listener(app)
    private val navi = NaviClient(app)

    init {
        viewModelScope.launch {
            DrivingStateSource(app).isDriving().collect { driving ->
                _state.update { it.copy(isDriving = driving) }
            }
        }
        viewModelScope.launch {
            DemoBus.commands.collect { onDemoCommand(it) }
        }
    }

    fun onEvent(event: OrderEvent) {
        when (event) {
            is OrderEvent.AddMenu -> _state.update { it.copy(cart = it.cart.add(event.menu)) }
            is OrderEvent.RemoveMenu -> _state.update { it.copy(cart = it.cart.remove(event.menu)) }
            OrderEvent.ReviewOrder -> _state.update {
                val canReview = it.status == OrderStatus.Browsing && it.cart.isNotEmpty()
                if (canReview) it.copy(status = OrderStatus.Confirming) else it
            }
            OrderEvent.BackToMenu -> _state.update {
                if (it.status == OrderStatus.Confirming) it.copy(status = OrderStatus.Browsing) else it
            }
            OrderEvent.PlaceOrder -> placeOrder()
            OrderEvent.ConfirmReceived -> _state.update {
                if (it.status == OrderStatus.Arrived) it.copy(status = OrderStatus.Received) else it
            }
        }
    }

    /** 주행 중일 때만 센다 */
    fun onScreenTouched() {
        _state.update { if (it.isDriving) it.copy(touchCount = it.touchCount + 1) else it }
    }

    // ---------- F14 데모 명령 ----------

    private fun onDemoCommand(command: DemoCommand) {
        when (command) {
            DemoCommand.Reset -> {
                flow.reset()
                locationSource.clearArrival()
                riderTracker.reset()
                _state.update { initialState.copy(isDriving = it.isDriving) }
            }
            DemoCommand.Dump -> dumpState()
            is DemoCommand.Event -> eventFrom(command)?.let(::onEvent)
                ?: Log.w(TAG, "보낼 수 없는 이벤트: $command")
            is DemoCommand.Rider -> onRider(command)
            is DemoCommand.Say -> viewModelScope.launch { speaker.say(command.text) }
            DemoCommand.Route -> viewModelScope.launch {
                val r = navi.startRoute(DemoData.s1Destination, DemoData.S1_DESTINATION_NAME)
                Log.i(TAG, "내비 안내 시작 ${DemoData.S1_DESTINATION_NAME}: ${r?.let { "${it.distanceM}m ${it.timeSec}초" } ?: "실패"}")
            }
            is DemoCommand.Utter -> viewModelScope.launch { hear(command.name) }
        }
    }

    /** 녹음 대사를 인식한다 (F2). 듣는 동안 중간 결과를 자막(transcript)으로 내보낸다 */
    private suspend fun hear(name: String) {
        _state.update { it.copy(voiceState = VoiceState.Listening, transcript = "") }
        val text = listener.recognizeAsset(name) { partial -> _state.update { it.copy(transcript = partial) } }
        _state.update { it.copy(voiceState = VoiceState.Idle, transcript = text ?: it.transcript) }
        if (text == null) Log.w(TAG, "인식 실패: $name") else onHeard(text)
    }

    private val parser = OrderParser(
        menuNames = DemoData.s1Stores.first().menus.map { it.name },
        storeNames = DemoData.s1Stores.map { it.name },
    )

    private val locationSource = LocationSource(app)
    private val storeFinder = StoreFinder(DemoRoute.points, DemoData.s1Stores)
    private var lastFound: List<StoreFinder.Found> = emptyList()

    /** 지금 차 위치(FusedLocation) 기준으로 경로 근처 앞쪽 가게를 고른다 (F6) */
    private val flow = VoiceOrderFlow { category ->
        val here = locationSource.location.value
            ?: DemoData.s1Start.also { Log.w(TAG, "차 위치를 아직 못 받아 출발점 기준으로 고름") }
        storeFinder.find(here, category).also {
            lastFound = it
            Log.i(TAG, "가게 추천 (${here.lat}, ${here.lng}) $category → " + it.joinToString { f -> "${f.store.name} 앞 ${f.aheadM.roundToInt()}m·옆 ${f.offsetM.roundToInt()}m" })
        }.map { it.store }
    }

    /** 해석 결과로 대화를 한 단계 진행하고, 앱이 할 말을 읽는다 (F5) */
    private suspend fun onHeard(text: String) {
        val parsed = parser.parse(text)
        val reply = flow.onHeard(parsed)
        Log.i(TAG, "들음: '$text' → ${reply.stage::class.simpleName} '${reply.say}'")
        _state.update { it.withStage(reply.stage).copy(assistantText = reply.say.ifEmpty { it.assistantText }) }
        if (reply.say.isNotEmpty()) speak(reply.say)
        val stage = reply.stage
        if (stage is VoiceOrderFlow.Stage.Ordered) choosePickup(stage.store)
    }

    private val pickupSelector = PickupSelector()

    /** 차종은 차에서 한 번 읽어 둔다 */
    private val carModel: String by lazy { VehicleInfo.carModel(app) ?: "차종 미확인" }

    private fun deliveryInfo(label: String, at: LatLng) = DeliveryInfo(
        placeLabel = label,
        latitude = at.lat,
        longitude = at.lng,
        plate = DemoData.vehicle.plate,
        color = DemoData.vehicle.color,
        carModel = carModel,
    )

    /** 수령 지점을 고르고(F7), 계산 로그를 남기고, 안내한다 */
    private suspend fun choosePickup(store: DemoStore) {
        val candidates = DemoData.s1Pickups.takeIf { store.id == "s1-a" }.orEmpty()   // 후보 실측은 길목김밥 근처만 있다
        val sel = pickupSelector.choose(store, candidates, DemoData.s1Measures)
        if (sel == null) {
            Log.w(TAG, "${store.name} 근처 수령 후보 실측값이 없음")
            val say = "${store.name} 앞에서 받을게요."
            _state.update { it.copy(assistantText = say) }
            speak(say)
            return
        }
        File(getApplication<Application>().filesDir, "pickup_log.json").writeText(PickupLog.toJson(store, sel, candidates))
        Log.i(TAG, "수령 지점 ${sel.chosen.name} +${sel.chosen.detourMin}분(+${sel.chosen.detourM}m) 대기 ${sel.chosen.waitMin}분 / 포장 +${sel.takeout?.detourMin}분(+${sel.takeout?.detourM}m)")
        val c = sel.chosen
        val pickupLoc = candidates.first { it.id == c.id }.location
        _state.update { it.copy(delivery = deliveryInfo(c.name, pickupLoc)) }
        val say = "${c.name}에서 받으세요. " + if (c.waitMin > 0) "도착하면 ${c.waitMin}분쯤 기다려요." else "도착하면 바로 받을 수 있어요."
        _state.update {
            it.copy(pickup = PickupPoint(c.name, detourMin = c.detourMin, detourM = c.detourM, waitMin = c.waitMin), assistantText = say)
        }
        speak(say)
        addWaypoint(c.name, pickupLoc, c)
        locationSource.watchArrival(pickupLoc, ARRIVAL_RADIUS_M) { viewModelScope.launch { onArrivedAtPickup(c.name) } }
    }

    /** 수령 지점 반경 안에 들어왔다 (F12) */
    private suspend fun onArrivedAtPickup(name: String) {
        if (_state.value.atPickup) return
        Log.i(TAG, "수령 지점 도착 $name")
        val say = "${name}에 도착했어요. 기사님이 오고 있어요."
        _state.update { it.copy(atPickup = true, assistantText = say) }
        speak(say)
    }

    /** 고른 지점을 내비 경유지로 넣고, 넣기 전후 남은 거리·시간 차이로 +N분을 다시 잰다 (F8) */
    private suspend fun addWaypoint(name: String, loc: LatLng, planned: PickupSelector.Evaluated) {
        val before = navi.remain()
        if (before == null) {
            Log.w(TAG, "내비 안내 중이 아니라 경유지를 넣지 못함")
            return
        }
        val after = navi.addWaypoint(loc, name)
        if (after == null) {
            Log.w(TAG, "경유지 추가 후 경로를 다시 받지 못함")
            return
        }
        val detourM = after.distanceM - before.distanceM
        val detourMin = ((after.timeSec - before.timeSec) / 60.0).roundToInt()
        Log.i(TAG, "경유지 추가 $name: ${before.distanceM}m ${before.timeSec}초 → ${after.distanceM}m ${after.timeSec}초 (+${detourM}m +${detourMin}분, 미리 잰 값 +${planned.detourM}m +${planned.detourMin}분)")
        val say = if (detourMin <= 0) "경로에 넣었어요. 가던 길 그대로예요." else "경로에 넣었어요. 원래보다 ${detourMin}분 더 걸려요."
        _state.update { s ->
            s.copy(pickup = s.pickup?.copy(detourMin = detourMin.coerceAtLeast(0), detourM = detourM.coerceAtLeast(0)), assistantText = say)
        }
        speak(say)
    }

    private suspend fun speak(text: String) {
        _state.update { it.copy(voiceState = VoiceState.Speaking) }
        speaker.say(text)
        _state.update { it.copy(voiceState = VoiceState.Idle) }
    }

    private fun OrderUiState.withStage(stage: VoiceOrderFlow.Stage): OrderUiState = when (stage) {
        VoiceOrderFlow.Stage.Idle -> copy(suggestions = emptyList(), cart = emptyList(), status = OrderStatus.Browsing)
        is VoiceOrderFlow.Stage.ChoosingStore -> copy(
            suggestions = stage.stores.map { s ->
                StoreSuggestion(s.name, lastFound.firstOrNull { it.store.id == s.id }?.aheadM?.roundToInt() ?: 0)
            },
        )
        is VoiceOrderFlow.Stage.ChoosingMenu -> copy(suggestions = suggestions.filter { it.storeName == stage.store.name })
        is VoiceOrderFlow.Stage.Confirming -> copy(cart = stage.cart, status = OrderStatus.Confirming)
        is VoiceOrderFlow.Stage.Ordered -> copy(cart = stage.cart, status = OrderStatus.Ordered)
    }

    private fun eventFrom(c: DemoCommand.Event): OrderEvent? {
        val menu = c.id?.let { id -> _state.value.menus.firstOrNull { it.id == id } }
        return when (c.name) {
            "AddMenu" -> menu?.let { OrderEvent.AddMenu(it) }
            "RemoveMenu" -> menu?.let { OrderEvent.RemoveMenu(it) }
            "ReviewOrder" -> OrderEvent.ReviewOrder
            "BackToMenu" -> OrderEvent.BackToMenu
            "PlaceOrder" -> OrderEvent.PlaceOrder
            "ConfirmReceived" -> OrderEvent.ConfirmReceived
            else -> null
        }
    }

    private val riderTracker = RiderTracker()

    /**
     * 기사 좌표가 들어오면 받는 곳(배달 정보 좌표)까지 거리를 계산해 상태를 바꾸고,
     * 200m 안에 처음 들어오면 "곧 도착", 30m 안이면 "도착"을 한 번씩 알린다 (F11).
     */
    private fun onRider(c: DemoCommand.Rider) {
        val status = _state.value.status
        if (status != OrderStatus.Ordered && status != OrderStatus.RiderComing && status != OrderStatus.Arrived) return
        val target = _state.value.delivery?.let { LatLng(it.latitude, it.longitude) } ?: DemoData.s2Parking
        val d = Geo.distanceM(c.location, target).roundToInt()
        _state.update {
            it.copy(riderDistanceM = d, status = if (riderTracker.isArrived(d)) OrderStatus.Arrived else OrderStatus.RiderComing)
        }
        for (e in riderTracker.update(d)) {
            val say = when (e) {
                RiderTracker.Event.Approaching -> "배달기사가 곧 도착해요."
                RiderTracker.Event.Arrived -> "배달기사가 도착했어요."
            }
            Log.i(TAG, "기사 $e (${d}m)")
            _state.update { it.copy(assistantText = say) }
            viewModelScope.launch { speak(say) }
        }
    }

    private fun dumpState() {
        val file = File(getApplication<Application>().filesDir, "demo_state.json")
        file.writeText(DemoSnapshot.toJson(_state.value))
        Log.i(TAG, "state 저장: ${file.absolutePath}")
    }

    // ---------- 주문 ----------

    private fun placeOrder() {
        if (_state.value.status != OrderStatus.Confirming) return
        // 결제는 가상: 화면 없이 바로 성공 처리. 받는 곳은 주소 대신 지금 차 좌표 (F10)
        val here = locationSource.location.value
        if (here == null) Log.w(TAG, "차 위치를 아직 못 받아 배달 정보 없이 주문")
        val delivery = here?.let { deliveryInfo("지금 차 위치", it) }
        Log.i(TAG, "배달 정보 $delivery")
        riderTracker.reset()
        _state.update { it.copy(status = OrderStatus.Ordered, delivery = delivery) }
    }

    private fun List<CartItem>.add(menu: Menu): List<CartItem> =
        if (none { it.menu.id == menu.id }) {
            this + CartItem(menu, 1)
        } else {
            map { if (it.menu.id == menu.id) it.copy(quantity = it.quantity + 1) else it }
        }

    private fun List<CartItem>.remove(menu: Menu): List<CartItem> =
        mapNotNull {
            when {
                it.menu.id != menu.id -> it
                it.quantity > 1 -> it.copy(quantity = it.quantity - 1)
                else -> null
            }
        }

    override fun onCleared() {
        speaker.release()
        listener.release()
        navi.release()
        locationSource.release()
        super.onCleared()
    }

    private companion object {
        const val TAG = "Demo"
        const val ARRIVAL_RADIUS_M = 100f
    }
}
