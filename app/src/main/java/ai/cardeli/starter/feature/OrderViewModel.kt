package ai.cardeli.starter.feature

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.feature.demo.DemoBus
import ai.cardeli.starter.feature.demo.DemoCommand
import ai.cardeli.starter.feature.demo.DemoSnapshot
import ai.cardeli.starter.feature.geo.Geo
import ai.cardeli.starter.feature.mock.DemoData
import ai.cardeli.starter.feature.mock.MockData
import ai.cardeli.starter.feature.sdk.DrivingStateSource
import ai.cardeli.starter.feature.sdk.Speaker
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

class OrderViewModel(app: Application) : AndroidViewModel(app) {

    private val initialState = OrderUiState(menus = MockData.menus)
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    private var deliveryJob: Job? = null
    private val speaker = Speaker(app)

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

    // ---------- F14 데모 명령 ----------

    private fun onDemoCommand(command: DemoCommand) {
        when (command) {
            DemoCommand.Reset -> {
                deliveryJob?.cancel()
                _state.update { initialState.copy(isDriving = it.isDriving) }
            }
            DemoCommand.Dump -> dumpState()
            is DemoCommand.Event -> eventFrom(command)?.let(::onEvent)
                ?: Log.w(TAG, "보낼 수 없는 이벤트: $command")
            is DemoCommand.Rider -> onRider(command)
            is DemoCommand.Say -> viewModelScope.launch { speaker.say(command.text) }
            // TODO(F2): assets/voice/<name>.pcm을 음성 인식에 넣는다
            is DemoCommand.Utter -> Log.i(TAG, "대사 넣기는 F2에서 연결: ${command.name}")
        }
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

    /**
     * 기사 좌표가 들어오면 가상 타이머를 멈추고 차(학원 주차장)까지의 거리로 상태를 바꾼다.
     * TODO(F11): 차 위치를 FusedLocation 실제 좌표로, 200m 안 진입 시 음성 안내
     */
    private fun onRider(c: DemoCommand.Rider) {
        val status = _state.value.status
        if (status != OrderStatus.Ordered && status != OrderStatus.RiderComing && status != OrderStatus.Arrived) return
        deliveryJob?.cancel()
        val d = Geo.distanceM(c.location, DemoData.s2Parking).roundToInt()
        _state.update {
            it.copy(riderDistanceM = d, status = if (d <= ARRIVED_M) OrderStatus.Arrived else OrderStatus.RiderComing)
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
        // 결제는 가상: 화면 없이 바로 성공 처리
        _state.update { it.copy(status = OrderStatus.Ordered) }
        deliveryJob?.cancel()
        deliveryJob = viewModelScope.launch { simulateDelivery() }
    }

    /** TODO(F11): 가상 진행을 adb broadcast로 받은 기사 좌표 기반 계산으로 바꾼다. 기사 좌표가 오면 멈춘다 */
    private suspend fun simulateDelivery() {
        delay(MockData.COOKING_MS)
        for (d in MockData.riderDistancesM) {
            _state.update {
                it.copy(
                    status = if (d == 0) OrderStatus.Arrived else OrderStatus.RiderComing,
                    riderDistanceM = d,
                )
            }
            delay(MockData.RIDER_STEP_MS)
        }
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
        super.onCleared()
    }

    private companion object {
        const val TAG = "Demo"
        const val ARRIVED_M = 30
    }
}
