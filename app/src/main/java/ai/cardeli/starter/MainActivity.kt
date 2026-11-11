package ai.cardeli.starter

import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.feature.KeepAliveService
import ai.cardeli.starter.feature.OrderViewModel
import ai.cardeli.starter.feature.sdk.GleoBridge
import ai.cardeli.starter.ui.screens.ConfirmScreen
import ai.cardeli.starter.ui.screens.DeliveryScreen
import ai.cardeli.starter.ui.screens.DrivingScreen
import ai.cardeli.starter.ui.screens.MenuScreen
import ai.cardeli.starter.ui.theme.CardeliTheme
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
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
        KeepAliveService.start(this)   // 내비가 앞에 나와도 앱이 얼지 않게
        handleGleo(intent)
        setContent {
            CardeliTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                AppScreen(state, viewModel::onEvent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGleo(intent)
    }

    /** Gleo AI가 ORDER_FOOD로 앱을 부르면 주문 대화를 시작하고 Gleo에 결과를 돌려준다 (F15) */
    private fun handleGleo(intent: Intent?) {
        if (intent == null || !GleoBridge.isOrder(intent)) return
        val order = GleoBridge.parse(intent)
        val ok = order != null && viewModel.onGleoOrder(order)
        GleoBridge.respond(this, ok, if (ok) "밥먹GO에서 주문을 이어갈게요." else "주문할 메뉴를 다시 말씀해 주세요.")
    }

    /** 주행 중 화면 터치를 센다 (F1 '화면 터치 0회'). 화면 코드와 상관없이 창 전체에서 센다 */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) viewModel.onScreenTouched()
        return super.dispatchTouchEvent(ev)
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
