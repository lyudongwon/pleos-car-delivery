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

/** 정차 중 메뉴판. (F9) 메뉴를 누르면 OrderEvent.AddMenu, 장바구니 확인은 OrderEvent.ReviewOrder */
@Composable
fun MenuScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    // TODO(UI): 임시 구현. 상태 값을 Text로만 찍는다.
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(32.dp)) {
            Text("MenuScreen", style = MaterialTheme.typography.headlineMedium)
            Text("가게: ${state.menus.firstOrNull()?.storeName ?: "-"}")
            state.menus.forEach { Text("· ${it.name} ${it.price}원") }
            Text("장바구니: ${state.cart.joinToString { "${it.menu.name} x${it.quantity}" }.ifEmpty { "비어 있음" }}")
            Text("합계: ${state.totalPrice}원")
            Text("status=${state.status}, isDriving=${state.isDriving}")
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun MenuScreenPreview() {
    CardeliTheme { MenuScreen(PreviewStates.browsing) {} }
}
