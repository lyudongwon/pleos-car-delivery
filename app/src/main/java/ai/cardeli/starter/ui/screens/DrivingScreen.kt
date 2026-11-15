package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.VoiceState
import ai.cardeli.starter.ui.components.Logo
import ai.cardeli.starter.ui.components.meters
import ai.cardeli.starter.ui.theme.Background
import ai.cardeli.starter.ui.theme.ButtonSecondary
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.Outline
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.TextHint
import ai.cardeli.starter.ui.theme.TextSub
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 주행 중 화면. (F1) 주행 중엔 터치 조작을 두지 않는다.
 * '주행 중' 배지, 음성 상태, 텍스트 1줄, 수령 지점 1줄, 화면 터치 횟수만 보여준다.
 */
@Composable
fun DrivingScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Background, contentColor = OnBackground) {
        Box(Modifier.fillMaxSize().padding(horizontal = 44.dp, vertical = 30.dp)) {
            // 위: 로고 + 주행 중 배지
            Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo()
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(32.dp))
                        .border(2.dp, Primary, RoundedCornerShape(32.dp))
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("주행 중", style = MaterialTheme.typography.bodyLarge, color = Primary) }
            }

            // 가운데: 음성 상태 + 한 줄
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VoiceIndicator(state.voiceState)
                Spacer(Modifier.height(16.dp))
                Text(voiceLabel(state.voiceState), style = MaterialTheme.typography.bodyLarge, color = TextSub)
                Spacer(Modifier.height(40.dp))
                Text(
                    mainLine(state),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val pickup = state.pickup
                if (pickup != null) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (state.atPickup) "${pickup.name} 도착" else "${pickup.name}에서 받아요",
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // 구석: 화면 터치 횟수
            Text(
                "화면 터치 ${state.touchCount}회",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/** 음성 상태 원. 듣는 중이면 노랗게 채우고, 말하는 중·생각 중이면 노란 테두리 */
@Composable
private fun VoiceIndicator(voice: VoiceState) {
    val (fill, ring) = when (voice) {
        VoiceState.Listening -> Primary to Primary
        VoiceState.Speaking -> ButtonSecondary to Primary
        VoiceState.Thinking -> ButtonSecondary to Primary.copy(alpha = 0.5f)
        VoiceState.Idle -> ButtonSecondary to Outline
    }
    Box(
        Modifier.size(132.dp).clip(CircleShape).background(fill).border(6.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // 마이크 모양: 둥근 막대 + 받침
        val ink = if (voice == VoiceState.Listening) Color.Black else OnBackground
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(width = 26.dp, height = 42.dp).clip(RoundedCornerShape(13.dp)).background(ink))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(width = 6.dp, height = 10.dp).background(ink))
            Box(Modifier.size(width = 26.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(ink))
        }
    }
}

private fun voiceLabel(voice: VoiceState): String = when (voice) {
    VoiceState.Listening -> "듣고 있어요"
    VoiceState.Thinking -> "확인하고 있어요"
    VoiceState.Speaking -> "말하는 중"
    VoiceState.Idle -> "\"밥 먹고 싶어\"라고 말해 보세요"
}

/** 화면 가운데 한 줄: 듣는 중이면 자막, 아니면 앱이 마지막으로 한 말, 그것도 없으면 주문 단계 */
private fun mainLine(state: OrderUiState): String {
    if (state.voiceState == VoiceState.Listening && state.transcript.isNotBlank()) return state.transcript
    if (state.assistantText.isNotBlank()) return state.assistantText
    return when (state.status) {
        OrderStatus.Browsing -> "가는 길에 받을 수 있는 가게를 찾아 드려요"
        OrderStatus.Confirming -> "주문 내용을 확인하고 있어요"
        OrderStatus.Ordered -> "주문이 접수됐어요"
        OrderStatus.Cooking -> "사장님이 조리 중이에요"
        OrderStatus.Delivering -> state.etaMin?.let { "약 ${it}분 후 받아요" } ?: "배달이 시작됐어요"
        OrderStatus.RiderComing -> state.riderDistanceM?.let { "기사님이 ${meters(it)} 거리에 있어요" } ?: "기사님이 곧 도착해요"
        OrderStatus.Arrived -> "기사님이 도착했어요"
        OrderStatus.Received -> "수령 완료"
        else -> "밥먹GO"
    }
}

@Preview(widthDp = 1360, heightDp = 948)
@Composable
private fun DrivingScreenPreview() {
    CardeliTheme { DrivingScreen(PreviewStates.driving) {} }
}
