package ai.cardeli.starter.feature

import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.feature.mock.MockData
import ai.cardeli.starter.feature.sdk.DrivingStateSource
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrderViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(OrderUiState(menus = MockData.menus))
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    private var deliveryJob: Job? = null

    init {
        viewModelScope.launch {
            DrivingStateSource(app).isDriving().collect { driving ->
                _state.update { it.copy(isDriving = driving) }
            }
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

    private fun placeOrder() {
        if (_state.value.status != OrderStatus.Confirming) return
        // 결제는 가상: 화면 없이 바로 성공 처리
        _state.update { it.copy(status = OrderStatus.Ordered) }
        deliveryJob?.cancel()
        deliveryJob = viewModelScope.launch { simulateDelivery() }
    }

    /** TODO(F11): 가상 진행을 adb broadcast로 받은 기사 좌표 기반 계산으로 바꾼다. */
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
}
