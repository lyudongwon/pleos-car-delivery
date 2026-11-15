package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.DeliveryMethod
import ai.cardeli.starter.contract.LocateBy
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.HintText
import ai.cardeli.starter.ui.components.HorizontalLine
import ai.cardeli.starter.ui.components.InfoRow
import ai.cardeli.starter.ui.components.LineItemRow
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.OptionalIllustration
import ai.cardeli.starter.ui.components.Panel
import ai.cardeli.starter.ui.components.PrimaryButton
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.components.SecondaryButton
import ai.cardeli.starter.ui.components.SectionLabel
import ai.cardeli.starter.ui.components.StatusLink
import ai.cardeli.starter.ui.components.StepProgress
import ai.cardeli.starter.ui.components.TotalRow
import ai.cardeli.starter.ui.components.meters
import ai.cardeli.starter.ui.components.won
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.Surface
import ai.cardeli.starter.ui.theme.TextMuted
import ai.cardeli.starter.ui.theme.TextSub
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 주문 내역 (Ordered → Cooking → Delivering → RiderComing → Arrived → Received).
 * Arrived에서 "음식을 수령하셨나요?" → 네 = OrderEvent.ConfirmReceived, 아니요 = OrderEvent.NotReceived
 */
@Composable
fun DeliveryScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    val storeName = state.cart.firstOrNull()?.menu?.storeName ?: state.menus.firstOrNull()?.storeName.orEmpty()
    val d = state.delivery
    val placeLabel = d?.placeLabel ?: "지금 차 위치"

    AppScaffold(active = NavTab.Orders) {
        ScreenTitle("주문내역", accentBar = true)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
            // 왼쪽: 지금 주문 카드 + 기사에게 전달된 정보
            Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(
                    Modifier.fillMaxWidth().border(1.dp, Primary, MaterialTheme.shapes.medium),
                    padding = 30.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(storeName, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            Text(cartSummary(state), style = MaterialTheme.typography.bodyMedium, color = TextSub)
                        }
                        StatusLink(shortStatus(state.status))
                    }
                }

                Panel(Modifier.fillMaxWidth().weight(1f), padding = 30.dp) {
                    SectionLabel("기사님에게 전달된 정보")
                    Spacer(Modifier.height(16.dp))
                    if (d == null) {
                        HintText("주문을 보내는 중이에요")
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            InfoRow("받는 곳", if (d.fixedByPark) "${d.placeLabel} (기어 P에서 확정)" else d.placeLabel)
                            InfoRow("받을 창문", d.window.label)
                            InfoRow("찾는 방법", d.locateBy.label)
                            InfoRow("차량 번호", d.plate)
                            InfoRow("색상", d.color)
                            InfoRow("차종", d.carModel)
                            if (d.riderNote.isNotBlank()) InfoRow("전달 사항", d.riderNote)
                        }
                    }
                }
            }

            // 오른쪽: 진행 상황 패널
            Panel(Modifier.weight(1.1f).fillMaxHeight(), color = Surface, padding = 40.dp) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(storeName, style = MaterialTheme.typography.titleMedium, color = TextSub)
                            Spacer(Modifier.height(18.dp))
                            Text(headline(state.status), style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(10.dp))
                            Text(subline(state), style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                        }
                        OptionalIllustration("illust_rider", Modifier.size(width = 160.dp, height = 140.dp))
                    }
                    Spacer(Modifier.height(36.dp))
                    StepProgress(doneCount(state.status))
                    Spacer(Modifier.height(36.dp))

                    SectionLabel("주문정보")
                    Spacer(Modifier.height(8.dp))
                    InfoRow("배달방식", if (state.options.method == DeliveryMethod.ToCar) "내 차량에서 수령" else "지정 장소에서 수령")
                    InfoRow("받는 곳", placeLabel)
                    InfoRow("결제수단", state.options.payment.label)
                    Spacer(Modifier.height(20.dp))

                    SectionLabel("주문 상세")
                    Spacer(Modifier.height(8.dp))
                    state.cart.forEach { LineItemRow(it.menu.name, it.quantity, it.totalPrice) }
                    Spacer(Modifier.height(8.dp))
                    HorizontalLine()
                    TotalRow("합계", state.payTotal, note = if (state.deliveryFee > 0) "배달팁 포함" else null)
                }

                if (state.status == OrderStatus.Arrived) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(
                            text = "네, 받았어요",
                            onClick = { onEvent(OrderEvent.ConfirmReceived) },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "아니요, 못 받았어요",
                            onClick = { onEvent(OrderEvent.NotReceived) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun doneCount(status: OrderStatus): Int = when (status) {
    OrderStatus.Ordered -> 1
    OrderStatus.Cooking -> 2
    OrderStatus.Delivering, OrderStatus.RiderComing, OrderStatus.Arrived -> 3
    OrderStatus.Received -> 4
    else -> 0
}

private fun headline(status: OrderStatus): String = when (status) {
    OrderStatus.Ordered -> "주문이 접수됐어요"
    OrderStatus.Cooking -> "사장님이 조리 중이에요"
    OrderStatus.Delivering -> "배달이 시작됐어요"
    OrderStatus.RiderComing -> "기사님이 곧 도착해요"
    OrderStatus.Arrived -> "음식을 수령하셨나요?"
    OrderStatus.Received -> "맛있게 드세요"
    else -> "주문을 보내고 있어요"
}

private fun shortStatus(status: OrderStatus): String = when (status) {
    OrderStatus.Ordered -> "주문 접수"
    OrderStatus.Cooking -> "조리 중"
    OrderStatus.Delivering -> "배달 시작"
    OrderStatus.RiderComing -> "곧 도착"
    OrderStatus.Arrived -> "도착"
    OrderStatus.Received -> "수령 완료"
    else -> "주문 중"
}

private fun subline(state: OrderUiState): AnnotatedString = buildAnnotatedString {
    val eta = state.etaMin
    when (state.status) {
        OrderStatus.Ordered, OrderStatus.Cooking, OrderStatus.Delivering -> {
            if (eta != null) {
                append("약 ")
                withStyle(SpanStyle(color = Primary)) { append("${eta}분 후") }
                append(" 배달이 완료돼요.")
            } else {
                append(if (state.status == OrderStatus.Delivering) "기사님이 차로 오고 있어요." else "가게에서 음식을 준비하고 있어요.")
            }
        }
        OrderStatus.RiderComing -> {
            val m = state.riderDistanceM
            if (m != null) {
                append("기사님이 ")
                withStyle(SpanStyle(color = Primary)) { append(meters(m)) }
                append(" 거리에 있어요.")
            } else {
                append("기사님이 거의 다 왔어요.")
            }
            if (state.delivery?.locateBy == LocateBy.Hazard) append(" 비상등을 켜 주세요.")
        }
        OrderStatus.Arrived -> append("기사님이 차 옆에 도착했어요.")
        OrderStatus.Received -> append("수령이 완료됐어요.")
        else -> append("")
    }
}

private fun cartSummary(state: OrderUiState): String {
    val first = state.cart.firstOrNull() ?: return ""
    val rest = state.cart.size - 1
    return if (rest > 0) "${first.menu.name} 외 ${rest}개 · 오늘" else "${first.menu.name} · 오늘"
}

@Preview(widthDp = 1360, heightDp = 948)
@Composable
private fun DeliveryScreenPreview() {
    CardeliTheme { DeliveryScreen(PreviewStates.riderComing) {} }
}

@Preview(widthDp = 1360, heightDp = 948)
@Composable
private fun DeliveryScreenArrivedPreview() {
    CardeliTheme { DeliveryScreen(PreviewStates.arrived) {} }
}
