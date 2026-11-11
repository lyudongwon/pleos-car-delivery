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

/** 배달 진행 (Ordered → RiderComing → Arrived → Received). 도착 후 수령 확인은 OrderEvent.ConfirmReceived */
@Composable
fun DeliveryScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    // TODO(UI): 임시 구현. 상태 값을 Text로만 찍는다.
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(32.dp)) {
            Text("DeliveryScreen", style = MaterialTheme.typography.headlineMedium)
            Text("status=${state.status}")
            Text("기사까지 거리: ${state.riderDistanceM?.let { "${it}m" } ?: "-"}")
            Text("주문: ${state.cart.joinToString { "${it.menu.name} x${it.quantity}" }}")
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun DeliveryScreenPreview() {
    CardeliTheme { DeliveryScreen(PreviewStates.riderComing) {} }
}
