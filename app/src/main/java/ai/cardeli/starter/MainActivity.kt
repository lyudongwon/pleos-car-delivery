package ai.cardeli.starter

import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.feature.KeepAliveService
import ai.cardeli.starter.feature.OrderViewModel
import ai.cardeli.starter.feature.sdk.GleoBridge
import ai.cardeli.starter.ui.components.LocalTabNavigator
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.screens.DrivingScreen
import ai.cardeli.starter.ui.screens.FavoritesScreen
import ai.cardeli.starter.ui.screens.HomeScreen
import ai.cardeli.starter.ui.screens.OrderHistoryScreen
import ai.cardeli.starter.ui.screens.OrderSheetScreen
import ai.cardeli.starter.ui.screens.PaymentDoneScreen
import ai.cardeli.starter.ui.screens.SplashScreen
import ai.cardeli.starter.ui.screens.StoreListScreen
import ai.cardeli.starter.ui.screens.StoreMenuScreen
import ai.cardeli.starter.ui.theme.CardeliTheme
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
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

/**
 * 화면 연결 (화면 디자인: Pleos Connect | 밥먹GO).
 * 주행 중이면 DrivingScreen. 정차 중이면 상단 탭(홈·주문내역·즐겨찾기)과 주문 단계로 화면을 고른다.
 * 앱을 켜면 스플래시 1.5초, 결제하면 "결제 완료!" 1.5초를 보여 준 뒤 주문내역으로 간다.
 */
@Composable
private fun AppScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1_500); showSplash = false }
    if (state.isDriving) {
        DrivingScreen(state, onEvent)
        return
    }
    if (showSplash) {
        SplashScreen()
        return
    }

    var tab by remember { mutableStateOf(NavTab.Home) }
    var showHome by remember { mutableStateOf(true) }
    var payDone by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 주문하면 결제 완료를 잠깐 보여 주고 주문내역 탭으로. 리셋(주문 없음)이면 홈으로
    LaunchedEffect(state.receipt) {
        if (state.receipt == null) {
            tab = NavTab.Home
            showHome = true
        } else {
            payDone = true
            delay(1_500)
            payDone = false
            tab = NavTab.Orders
        }
    }

    val ordered = state.status != OrderStatus.Browsing && state.status != OrderStatus.Confirming
    val navigate: (NavTab) -> Unit = { t ->
        tab = t
        if (t == NavTab.Home) {
            showHome = true
            if (state.status == OrderStatus.Browsing && state.selectedStoreId != null) onEvent(OrderEvent.BackToStores)
        }
    }

    CompositionLocalProvider(LocalTabNavigator provides navigate) {
        when {
            payDone -> PaymentDoneScreen()
            tab == NavTab.Favorites -> FavoritesScreen(state) {
                tab = NavTab.Home
                showHome = false
            }
            tab == NavTab.Orders || (ordered && tab != NavTab.Home) -> OrderHistoryScreen(state, onEvent)
            else -> when (state.status) {
                OrderStatus.Browsing -> when {
                    state.selectedStoreId != null -> StoreMenuScreen(state, onEvent)
                    showHome -> HomeScreen(
                        state,
                        onOrderFood = { showHome = false },
                        onGleo = {
                            Toast.makeText(context, "Gleo에게 \"밥먹GO로 참치김밥 주문해줘\"라고 말해 보세요", Toast.LENGTH_LONG).show()
                        },
                    )
                    else -> StoreListScreen(state, onEvent)
                }
                OrderStatus.Confirming -> OrderSheetScreen(state, onEvent)
                else -> OrderHistoryScreen(state, onEvent)
            }
        }
    }
}
