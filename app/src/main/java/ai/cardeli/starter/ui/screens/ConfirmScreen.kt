package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.ui.theme.CardeliTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** 주문 확인. 주문은 OrderEvent.PlaceOrder, 돌아가기는 OrderEvent.BackToMenu. 받는 곳은 "지금 차 위치"로만 표시한다 */
@Composable
fun ConfirmScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    // TODO(UI): 임시 구현. 상태 값을 Text로만 찍는다.
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(32.dp)) {
            Text("ConfirmScreen", style = MaterialTheme.typography.headlineMedium)
            state.cart.forEach { Text("· ${it.menu.name} x${it.quantity} = ${it.totalPrice}원") }
            Text("합계: ${state.totalPrice}원")
            Text("받는 곳: 지금 차 위치")
            Text("status=${state.status}")
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun ConfirmScreenPreview() {
    CardeliTheme { ConfirmScreen(PreviewStates.confirming) {} }
}
