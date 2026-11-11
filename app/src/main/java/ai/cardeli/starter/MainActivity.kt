package ai.cardeli.starter

import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.feature.OrderViewModel
import ai.cardeli.starter.ui.screens.ConfirmScreen
import ai.cardeli.starter.ui.screens.DeliveryScreen
import ai.cardeli.starter.ui.screens.DrivingScreen
import ai.cardeli.starter.ui.screens.MenuScreen
import ai.cardeli.starter.ui.theme.CardeliTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val viewModel: OrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardeliTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                AppScreen(state, viewModel::onEvent)
            }
        }
    }
}

/** 주행 중이면 DrivingScreen, 정차 중이면 주문 단계에 맞는 화면 */
@Composable
private fun AppScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    if (state.isDriving) {
        DrivingScreen(state, onEvent)
        return
    }
    when (state.status) {
        OrderStatus.Browsing -> MenuScreen(state, onEvent)
        OrderStatus.Confirming -> ConfirmScreen(state, onEvent)
        OrderStatus.Ordered,
        OrderStatus.RiderComing,
        OrderStatus.Arrived,
        OrderStatus.Received -> DeliveryScreen(state, onEvent)
    }
}
