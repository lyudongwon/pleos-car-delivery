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
import ai.cardeli.starter.feature.route.StoreLister
import ai.cardeli.starter.feature.route.ChargeEstimator
import ai.cardeli.starter.feature.sdk.VehicleSignals
import ai.cardeli.starter.contract.DeliveryMethod
import ai.cardeli.starter.contract.LocateBy
import ai.cardeli.starter.contract.PaymentMethod
import ai.cardeli.starter.contract.ReceiveWindow
import ai.cardeli.starter.feature.sdk.GleoBridge
import ai.cardeli.starter.feature.mock.DemoStore
import ai.cardeli.starter.feature.mock.PickupMeasure
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
import kotlin.math.ceil
import kotlin.math.roundToInt

class OrderViewModel(private val app: Application) : AndroidViewModel(app) {

    private val initialState = OrderUiState(menus = MockData.menus, deliveryFee = DemoData.s2Store.deliveryFee)
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    private val speaker = Speaker(app)
    private val listener = Listener(app)
    private val navi = NaviClient(app)
    /** S1에서 주문한 뒤 "받았어"를 기다리는 중 (F13: 창문 감지 대신 말로 수령 완료) */
    private var awaitingReceive = false
    private val storeLister = StoreLister()
    private val chargeEstimator = ChargeEstimator()
    private var selectedStore: DemoStore = DemoData.s2Store
    private val vehicleSignals = VehicleSignals(
        app,
        onGear = { parked -> _state.update { it.copy(isParked = parked) } },
        onCharge = { open, soc -> onCharge(open, soc) },
    )

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
            is OrderEvent.SelectStore -> selectStore(event.storeId)
            is OrderEvent.AddMenu -> _state.update { it.copy(cart = it.cart.add(event.menu)) }
            is OrderEvent.RemoveMenu -> _state.update { it.copy(cart = it.cart.remove(event.menu)) }
            OrderEvent.ReviewOrder -> _state.update {
                val canReview = it.status == OrderStatus.Browsing && it.cart.isNotEmpty()
                if (canReview) it.copy(status = OrderStatus.Confirming) else it
            }
            OrderEvent.BackToMenu -> _state.update {
                if (it.status == OrderStatus.Confirming) it.copy(status = OrderStatus.Browsing) else it
            }
            is OrderEvent.SelectMethod -> _state.update { it.copy(options = it.options.copy(method = event.method)) }
            is OrderEvent.SelectWindow -> _state.update { it.copy(options = it.options.copy(window = event.window)) }
            is OrderEvent.SelectLocateBy -> _state.update { it.copy(options = it.options.copy(locateBy = event.locateBy)) }
            is OrderEvent.SetRiderNote -> _state.update { it.copy(options = it.options.copy(riderNote = event.text)) }
            is OrderEvent.SetStoreNote -> _state.update { it.copy(options = it.options.copy(storeNote = event.text)) }
            is OrderEvent.SelectPayment -> _state.update { it.copy(options = it.options.copy(payment = event.payment)) }
            OrderEvent.PlaceOrder -> placeOrder()
            OrderEvent.ConfirmReceived -> if (_state.value.status in RECEIVABLE || _state.value.atPickup) viewModelScope.launch { finishReceive() }
            OrderEvent.NotReceived -> viewModelScope.launch {
                val say = "기사님께 아직 못 받았다고 알렸어요."
                Log.i(TAG, "수령 안 됨 → 기사에게 알림")
                _state.update { it.copy(assistantText = say) }
                speak(say)
            }
        }
    }

    /** 가게 목록에서 가게를 고르면 그 가게 메뉴로 바꾼다. 다른 가게면 장바구니를 비운다 */
    private fun selectStore(id: String) {
        val store = DemoData.s2Stores.firstOrNull { it.id == id } ?: return
        if (store.id != selectedStore.id) _state.update { it.copy(cart = emptyList()) }
        selectedStore = store
        _state.update { it.copy(menus = store.menus, deliveryFee = store.deliveryFee) }
    }

    /** 지금 차 위치와 충전 상태로 정차 중 가게 목록을 다시 만든다 (F9·F16) */
    private fun refreshStores() {
        val here = locationSource.location.value ?: return
        _state.update { it.copy(stores = storeLister.cards(DemoData.s2Stores, here, it.charging)) }
    }

    /** 충전구 열림·배터리 잔량이 바뀌었다 (F16). 충전 중이면 충전 끝나기 전에 받을 수 있는 가게를 앞에 둔다 */
    private fun onCharge(portOpen: Boolean, soc: Int?) {
        val info = chargeEstimator.update(System.currentTimeMillis(), portOpen, soc)
        Log.i(TAG, "충전 상태 port=$portOpen soc=$soc → ${info?.let { "충전 중 ${it.socPercent}% 남은 ${it.remainMin}분" } ?: "충전 아님"}")
        _state.update { it.copy(charging = info) }
        refreshStores()
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
                awaitingReceive = false
                locationSource.clearArrival()
                riderTracker.reset()
                selectedStore = DemoData.s2Store
                _state.update { initialState.copy(isDriving = it.isDriving, isParked = it.isParked, charging = it.charging) }
                refreshStores()
            }
            is DemoCommand.Progress -> OrderStatus.entries.firstOrNull { it.name == command.status }?.let(::progress)
            is DemoCommand.Charge -> onCharge(command.portOpen, command.soc)
            DemoCommand.Dump -> dumpState()
            is DemoCommand.Event -> eventFrom(command)?.let(::onEvent)
                ?: Log.w(TAG, "보낼 수 없는 이벤트: $command")
            is DemoCommand.Rider -> onRider(command)
            is DemoCommand.Say -> viewModelScope.launch { speaker.say(command.text) }
            DemoCommand.Measure -> viewModelScope.launch { measureLive() }
            is DemoCommand.KeepAlive -> if (command.on) KeepAliveService.start(app) else KeepAliveService.stop(app)
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

    init {
        // 차 위치가 바뀌면 정차 중 가게 목록(거리·예상 시간)을 다시 계산한다
        viewModelScope.launch { locationSource.location.collect { refreshStores() } }
    }
    private val storeFinder = StoreFinder(DemoRoute.points, DemoData.s1Stores)
    private var lastFound: List<StoreFinder.Found> = emptyList()

    /** 지금 차 위치(FusedLocation) 기준으로 경로 근처 앞쪽 가게를 고른다 (F6) */
    private val flow = VoiceOrderFlow(DemoData.S1_LAST_ORDERED_STORE_ID) { category ->
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
        if (awaitingReceive) {
            Log.i(TAG, "들음(수령 확인): '$text' received=${parsed.received}")
            if (parsed.received) finishReceive() else speak("음식을 받으시면 받았다고 말씀해 주세요.")
            return
        }
        val wasOrdered = flow.stage is VoiceOrderFlow.Stage.Ordered
        val reply = flow.onHeard(parsed)
        Log.i(TAG, "들음: '$text' → ${reply.stage::class.simpleName} '${reply.say}'")
        _state.update { it.withStage(reply.stage).copy(assistantText = reply.say.ifEmpty { it.assistantText }) }
        if (reply.say.isNotEmpty()) speak(reply.say)
        val stage = reply.stage
        // 주문 완료로 처음 넘어갈 때만 수령 지점을 고른다 (주문 뒤의 말에 다시 고르지 않게)
        if (stage is VoiceOrderFlow.Stage.Ordered && !wasOrdered) {
            awaitingReceive = true
            choosePickup(stage.store)
        }
    }

    /** Gleo AI가 넘긴 주문으로 대화를 시작한다 (F15). 주문 확인 단계로 갔으면 true */
    fun onGleoOrder(order: GleoBridge.Order): Boolean {
        val items = parser.parse("${order.menu} ${order.quantity}개").items
        Log.i(TAG, "Gleo 주문 ${order.menu} x${order.quantity} → $items")
        flow.reset()
        val reply = flow.startWithItems(items)
        _state.update { it.withStage(reply.stage).copy(assistantText = reply.say) }
        viewModelScope.launch { speak(reply.say) }
        return reply.stage is VoiceOrderFlow.Stage.Confirming
    }

    private val pickupSelector = PickupSelector()

    /** 앱이 직접 잰 실측값. 있으면 미리 재 둔 값(DemoData.s1Measures) 대신 쓴다 */
    private var liveMeasures: Map<String, PickupMeasure>? = null

    /**
     * 내비에 지점마다 경로를 새로 요청해 추가 거리·시간을 재고, 현재 위치→지점 시간도 잰다 (F7 실시간 계산).
     * 지점당 약 15초. 끝나면 목적지만으로 안내를 되돌린다. 결과는 files/pickup_measure_live.json
     */
    private suspend fun measureLive() {
        val dest = DemoData.s1Destination
        val name = DemoData.S1_DESTINATION_NAME
        val base = navi.startRoute(dest, name) ?: run { Log.w(TAG, "실측: 기준 경로를 못 받음"); return }
        val store = DemoData.s1Stores.first()
        val points = listOf("store-${store.id}" to store.location) + DemoData.s1Pickups.map { it.id to it.location }
        val result = linkedMapOf<String, PickupMeasure>()
        for ((id, loc) in points) {
            val via = navi.measureVia(dest, name, loc, id)
            val eta = navi.tripEtaSec(loc, id)
            if (via == null || eta == null) {
                Log.w(TAG, "실측 실패 $id via=$via eta=$eta")
                continue
            }
            result[id] = PickupMeasure(
                detourM = (via.distanceM - base.distanceM).coerceAtLeast(0),
                detourSec = (via.timeSec - base.timeSec).coerceAtLeast(0),
                etaSec = eta,
            )
            Log.i(TAG, "실측 $id: 기준 ${base.distanceM}m ${base.timeSec}초 → ${via.distanceM}m ${via.timeSec}초, 현재→지점 ${eta}초 (미리 잰 값 ${DemoData.s1Measures[id]})")
        }
        navi.startRoute(dest, name)
        liveMeasures = result
        val json = result.entries.joinToString(",", "{\"baseM\":${base.distanceM},\"baseSec\":${base.timeSec},\"points\":{", "}}") { (id, m) ->
            "\"$id\":{\"detourM\":${m.detourM},\"detourSec\":${m.detourSec},\"etaSec\":${m.etaSec}}"
        }
        File(getApplication<Application>().filesDir, "pickup_measure_live.json").writeText(json)
        Log.i(TAG, "실측 끝 ${result.size}/${points.size}곳")
    }

    /** 차종은 차에서 한 번 읽어 둔다 */
    private val carModel: String by lazy { VehicleInfo.carModel(app) ?: "차종 미확인" }

    private fun deliveryInfo(label: String, at: LatLng, fixedByPark: Boolean = false): DeliveryInfo {
        val o = _state.value.options
        return DeliveryInfo(
            placeLabel = label,
            latitude = at.lat,
            longitude = at.lng,
            plate = DemoData.vehicle.plate,
            color = DemoData.vehicle.color,
            carModel = carModel,
            window = o.window,
            locateBy = o.locateBy,
            riderNote = o.riderNote,
            fixedByPark = fixedByPark,
        )
    }

    /** 수령 지점을 고르고(F7), 계산 로그를 남기고, 안내한다 */
    private suspend fun choosePickup(store: DemoStore) {
        val candidates = DemoData.s1Pickups.takeIf { store.id == "s1-a" }.orEmpty()   // 후보 실측은 길목김밥 근처만 있다
        val measures = liveMeasures ?: DemoData.s1Measures
        Log.i(TAG, "수령 지점 계산에 쓰는 값: ${if (liveMeasures != null) "앱 실측" else "미리 잰 값"}")
        val sel = pickupSelector.choose(store, candidates, measures)
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
        _state.update { it.copy(delivery = deliveryInfo(c.name, pickupLoc), etaMin = storeLister.etaMin(store, pickupLoc)) }
        val say = "${c.name}에서 받으세요. " + if (c.waitMin > 0) "도착하면 ${c.waitMin}분쯤 기다려요." else "도착하면 바로 받을 수 있어요."
        _state.update {
            it.copy(pickup = PickupPoint(c.name, detourMin = c.detourMin, detourM = c.detourM, waitMin = c.waitMin), assistantText = say)
        }
        locationSource.watchArrival(pickupLoc, ARRIVAL_RADIUS_M) { viewModelScope.launch { onArrivedAtPickup(c.name) } }
        // 경유지 재탐색(약 8초)을 안내 말과 함께 시작한다. 말을 다 하고 시작하면 그만큼 말이 끊긴다(11/12 리허설)
        val waypoint = viewModelScope.launch { addWaypoint(c.name, pickupLoc, c) }
        speak(say)
        waypoint.join()
    }

    /** 수령 지점 반경 안에 들어왔다 (F12) */
    private suspend fun onArrivedAtPickup(name: String) {
        if (_state.value.atPickup) return
        Log.i(TAG, "수령 지점 도착 $name")
        // 계산상 음식이 먼저 와 있으면(대기 0분) 기다리고 있다고, 아니면 오고 있다고 말한다
        val rider = if ((_state.value.pickup?.waitMin ?: 1) == 0) "기사님이 기다리고 있어요." else "기사님이 오고 있어요."
        val say = "${name}에 도착했어요. $rider"
        _state.update { it.copy(atPickup = true, assistantText = say) }
        speak(say)
    }

    /** 받았다(말 "받았어" 또는 화면 "네")면 수령 완료. 경유지가 있으면 빼고 원래 길로 (F13) */
    private suspend fun finishReceive() {
        if (_state.value.status == OrderStatus.Received) return
        awaitingReceive = false
        locationSource.clearArrival()
        val onRoute = navi.waypoint != null
        val say = if (onRoute) "맛있게 드세요. 원래 길로 안내할게요." else "맛있게 드세요."
        _state.update { it.copy(status = OrderStatus.Received, etaMin = 0, assistantText = say) }
        speak(say)
        if (onRoute) {
            val r = navi.removeWaypoint()
            Log.i(TAG, "경유지 뺌: ${r?.let { "${it.distanceM}m ${it.timeSec}초" } ?: "경로 다시 받지 못함"}")
        }
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
        val say = if (detourMin <= 0) "수령 지점을 경로에 추가했습니다." else "수령 지점을 경로에 추가했습니다. 원래보다 ${detourMin}분 더 걸려요."
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
        is VoiceOrderFlow.Stage.ChoosingMenu -> copy(
            suggestions = listOf(StoreSuggestion(stage.store.name, lastFound.firstOrNull { it.store.id == stage.store.id }?.aheadM?.roundToInt() ?: 0)),
        )
        is VoiceOrderFlow.Stage.Confirming -> copy(cart = stage.cart, status = OrderStatus.Confirming)
        is VoiceOrderFlow.Stage.Ordered -> copy(cart = stage.cart, status = OrderStatus.Ordered)
    }

    private fun eventFrom(c: DemoCommand.Event): OrderEvent? {
        val menu = c.id?.let { id -> _state.value.menus.firstOrNull { it.id == id } }
        return when (c.name) {
            "SelectStore" -> c.id?.let { OrderEvent.SelectStore(it) }
            "SelectWindow" -> c.id?.let { id -> ReceiveWindow.entries.firstOrNull { it.name == id } }?.let { OrderEvent.SelectWindow(it) }
            "SelectLocateBy" -> c.id?.let { id -> LocateBy.entries.firstOrNull { it.name == id } }?.let { OrderEvent.SelectLocateBy(it) }
            "SelectMethod" -> c.id?.let { id -> DeliveryMethod.entries.firstOrNull { it.name == id } }?.let { OrderEvent.SelectMethod(it) }
            "SelectPayment" -> c.id?.let { id -> PaymentMethod.entries.firstOrNull { it.name == id } }?.let { OrderEvent.SelectPayment(it) }
            "SetRiderNote" -> OrderEvent.SetRiderNote(c.id ?: "")
            "NotReceived" -> OrderEvent.NotReceived
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
        if (status !in IN_DELIVERY) return
        val target = _state.value.delivery?.let { LatLng(it.latitude, it.longitude) } ?: DemoData.s2Parking
        val d = Geo.distanceM(c.location, target).roundToInt()
        val next = when {
            riderTracker.isArrived(d) -> OrderStatus.Arrived
            d <= APPROACH_M -> OrderStatus.RiderComing
            else -> OrderStatus.Delivering
        }
        _state.update { it.copy(riderDistanceM = d, status = next, etaMin = ceil(d / RIDER_M_PER_MIN).toInt()) }
        val hazard = _state.value.delivery?.locateBy == LocateBy.Hazard
        for (e in riderTracker.update(d)) {
            val say = when (e) {
                RiderTracker.Event.Approaching -> if (hazard) "배달기사가 곧 도착해요. 비상등을 켜 주세요." else "배달기사가 곧 도착해요."
                RiderTracker.Event.Arrived -> if (_state.value.isDriving) "배달기사가 도착했어요." else "배달기사가 도착했어요. 음식을 받으셨나요?"
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
        // 결제는 가상: 바로 성공 처리. 받는 곳은 주소 대신 지금 차 좌표 (F10). 기어 P면 그 자리로 확정한다
        val here = locationSource.location.value
        val parked = _state.value.isParked
        if (here == null) Log.w(TAG, "차 위치를 아직 못 받아 배달 정보 없이 주문")
        if (!parked) Log.w(TAG, "기어가 P가 아닌 상태에서 정차 주문: 위치를 확정하지 않고 보낸다")
        val delivery = here?.let { deliveryInfo("지금 차 위치", it, fixedByPark = parked) }
        val eta = here?.let { storeLister.etaMin(selectedStore, it) }
        Log.i(TAG, "배달 정보 $delivery, 예상 ${eta}분, 결제 ${_state.value.options.payment.label} ${_state.value.payTotal}원")
        riderTracker.reset()
        _state.update { it.copy(status = OrderStatus.Ordered, delivery = delivery, etaMin = eta) }
    }

    /** 주문 진행 단계를 넘긴다 (데모 명령: 주문 접수 → 조리 중 등). 기사 단계는 기사 좌표로 정해진다 */
    private fun progress(status: OrderStatus) {
        if (_state.value.status !in IN_DELIVERY) return
        Log.i(TAG, "주문 진행 → $status")
        _state.update { it.copy(status = status) }
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
        vehicleSignals.release()
        locationSource.release()
        super.onCleared()
    }

    private companion object {
        const val TAG = "Demo"
        const val ARRIVAL_RADIUS_M = 100f
        const val APPROACH_M = 200
        const val RIDER_M_PER_MIN = 250.0   // StoreLister·PickupSelector와 같은 가정
        val IN_DELIVERY = setOf(OrderStatus.Ordered, OrderStatus.Cooking, OrderStatus.Delivering, OrderStatus.RiderComing, OrderStatus.Arrived)
        val RECEIVABLE = setOf(OrderStatus.RiderComing, OrderStatus.Arrived)
    }
}
