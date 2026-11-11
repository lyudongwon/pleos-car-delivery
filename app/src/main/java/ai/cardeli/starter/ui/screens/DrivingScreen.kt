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

/**
 * 주행 중 화면. (F1) 주행 중엔 터치 조작을 두지 않는다.
 * '주행 중' 배지, 텍스트 1줄, 음성 상태만 보여준다.
 */
@Composable
fun DrivingScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    // TODO(UI): 임시 구현. 상태 값을 Text로만 찍는다.
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(32.dp)) {
            Text("DrivingScreen · 주행 중", style = MaterialTheme.typography.headlineMedium)
            Text("status=${state.status}")
            Text("기사까지 거리: ${state.riderDistanceM?.let { "${it}m" } ?: "-"}")
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun DrivingScreenPreview() {
    CardeliTheme { DrivingScreen(PreviewStates.driving) {} }
}
