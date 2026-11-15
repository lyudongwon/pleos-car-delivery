package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.DeliveryMethod
import ai.cardeli.starter.contract.LocateBy
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.PaymentMethod
import ai.cardeli.starter.contract.ReceiveWindow
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.HorizontalLine
import ai.cardeli.starter.ui.components.InfoRow
import ai.cardeli.starter.ui.components.LineItemRow
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.NoteField
import ai.cardeli.starter.ui.components.OptionCard
import ai.cardeli.starter.ui.components.OptionalIllustration
import ai.cardeli.starter.ui.components.Panel
import ai.cardeli.starter.ui.components.PrimaryButton
import ai.cardeli.starter.ui.components.RadioRow
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.components.SecondaryButton
import ai.cardeli.starter.ui.components.SectionLabel
import ai.cardeli.starter.ui.components.TotalRow
import ai.cardeli.starter.ui.components.won
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.Surface
import ai.cardeli.starter.ui.theme.TextSub
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 주문서 (Confirming). 배달 방식·받을 창문·내 차 알리기·요청사항·결제수단을 고르고 OrderEvent.PlaceOrder로 결제한다.
 * 돌아가기는 OrderEvent.BackToMenu.
 */
@Composable
fun ConfirmScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    val options = state.options
    val storeName = state.cart.firstOrNull()?.menu?.storeName ?: state.menus.firstOrNull()?.storeName.orEmpty()

    AppScaffold(active = NavTab.Home) {
        ScreenTitle("주문하기", onBack = { onEvent(OrderEvent.BackToMenu) })
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
            // 왼쪽: 주문서 항목 (스크롤)
            Column(
                Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("담은 메뉴 (${state.cart.sumOf { it.quantity }})")
                    Spacer(Modifier.height(12.dp))
                    state.cart.forEach { LineItemRow(it.menu.name, it.quantity, it.totalPrice) }
                }

                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("배달방식")
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DeliveryMethod.values().forEach { m ->
                            OptionCard(
                                title = m.label,
                                subtitle = when (m) {
                                    DeliveryMethod.ToCar -> if (state.isParked) "기어 P · 지금 차 위치로 받아요" else "지금 차 위치로 받아요"
                                    DeliveryMethod.PickupPoint -> "가는 길의 정차하기 좋은 곳에서 받아요"
                                },
                                selected = options.method == m,
                                onClick = { onEvent(OrderEvent.SelectMethod(m)) },
                                modifier = Modifier.fillMaxWidth(),
                                leading = {
                                    OptionalIllustration(
                                        if (m == DeliveryMethod.ToCar) "illust_car" else "illust_pickup",
                                        Modifier.size(width = 150.dp, height = 90.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("받을 창문")
                    Spacer(Modifier.height(16.dp))
                    val windows = ReceiveWindow.values().toList()
                    windows.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { w ->
                                OptionCard(
                                    title = w.label,
                                    selected = options.window == w,
                                    onClick = { onEvent(OrderEvent.SelectWindow(w)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("기사님이 내 차를 찾는 방법")
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LocateBy.values().forEach { l ->
                            OptionCard(
                                title = l.label,
                                subtitle = when (l) {
                                    LocateBy.Hazard -> "기사님이 가까워지면 비상등을 켜 달라고 알려 드려요"
                                    LocateBy.CarInfo -> "차량 번호·차종·색상을 기사님에게 보내요"
                                },
                                selected = options.locateBy == l,
                                onClick = { onEvent(OrderEvent.SelectLocateBy(l)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("기사님에게 전달할 말", style = MaterialTheme.typography.bodyMedium, color = TextSub)
                    Spacer(Modifier.height(8.dp))
                    NoteField(options.riderNote, { onEvent(OrderEvent.SetRiderNote(it)) }, "예: 흰색 SUV, 조수석 창문으로 주세요")
                }

                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("가게 요청사항")
                    Spacer(Modifier.height(12.dp))
                    NoteField(options.storeNote, { onEvent(OrderEvent.SetStoreNote(it)) }, "요청사항 없음")
                }
            }

            // 오른쪽: 결제
            Panel(Modifier.weight(1f).fillMaxHeight(), color = Surface, padding = 40.dp) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(storeName, style = MaterialTheme.typography.titleMedium, color = TextSub)
                    Spacer(Modifier.height(10.dp))
                    Text("결제하기", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(24.dp))

                    SectionLabel("결제수단")
                    Spacer(Modifier.height(4.dp))
                    PaymentMethod.values().forEach { p ->
                        RadioRow(p.label, options.payment == p) { onEvent(OrderEvent.SelectPayment(p)) }
                    }
                    Spacer(Modifier.height(20.dp))

                    InfoRow("받을 창문", options.window.label)
                    InfoRow("메뉴 금액", won(state.totalPrice))
                    InfoRow("배달비", won(state.deliveryFee))
                    Spacer(Modifier.height(12.dp))
                    HorizontalLine()
                    Spacer(Modifier.height(4.dp))
                    TotalRow("결제 금액", state.payTotal)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = "${won(state.payTotal)} 결제하기",
                    onClick = { onEvent(OrderEvent.PlaceOrder) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.cart.isNotEmpty(),
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = "메뉴 더 담기",
                    onClick = { onEvent(OrderEvent.BackToMenu) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(widthDp = 1360, heightDp = 948)
@Composable
private fun ConfirmScreenPreview() {
    CardeliTheme { ConfirmScreen(PreviewStates.confirming) {} }
}
