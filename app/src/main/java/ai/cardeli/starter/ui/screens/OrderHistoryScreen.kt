package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.R
import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.DeliveryInfo
import ai.cardeli.starter.contract.LocateBy
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderOptions
import ai.cardeli.starter.contract.OrderReceipt
import ai.cardeli.starter.contract.OrderStatus
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.PastOrder
import ai.cardeli.starter.contract.ReceiveWindow
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.ChevronIcon
import ai.cardeli.starter.ui.components.HintText
import ai.cardeli.starter.ui.components.HorizontalLine
import ai.cardeli.starter.ui.components.InfoRow
import ai.cardeli.starter.ui.components.LineIcon
import ai.cardeli.starter.ui.components.LineItemRow
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.components.SectionLabel
import ai.cardeli.starter.ui.components.StepProgress
import ai.cardeli.starter.ui.components.TotalRow
import ai.cardeli.starter.ui.theme.Background
import ai.cardeli.starter.ui.theme.ButtonSecondary
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.OnPrimary
import ai.cardeli.starter.ui.theme.Outline
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.Surface
import ai.cardeli.starter.ui.theme.SurfaceHigh
import ai.cardeli.starter.ui.theme.SurfaceLow
import ai.cardeli.starter.ui.theme.TextBody
import ai.cardeli.starter.ui.theme.TextMuted
import ai.cardeli.starter.ui.theme.TextSub
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 피그마 "주문내역" 화면 (주문 접수 → 조리 중 → 배달 시작 → 곧 도착 → 도착 → 배달 완료)
// 왼쪽: 주문 목록(지금 주문 + 지난 주문), 오른쪽: 지금 주문 상세.
// 도착(Arrived)에서는 오른쪽에 "음식을 수령하셨나요?"를 띄운다.

private val StoreNameColor = Color(0xFFD0D0D0)   // 상세 패널 가게 이름
private val PastStatusColor = Color(0xFFE3E3E3)  // 지난 주문 "수령 완료"
private val ChipTextColor = Color(0xFFD9D9D9)    // 검색·조회 기간·주문 상태

/** 주문내역 탭. 화면은 state를 그리기만 하고, 수령 확인만 onEvent로 올린다 */
@Composable
fun OrderHistoryScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    val hasCurrent = state.status != OrderStatus.Browsing && state.status != OrderStatus.Confirming

    AppScaffold(active = NavTab.Orders) {
        Row(Modifier.fillMaxSize().padding(start = 12.dp)) {
            // 왼쪽: 제목, 필터, 주문 목록
            Column(Modifier.weight(584f).fillMaxHeight()) {
                Spacer(Modifier.height(11.dp))
                ScreenTitle("주문내역", accentBar = true)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip("검색", leading = { SearchIcon(20.dp, OnBackground) }, width = 112.dp)
                    FilterChip("조회 기간", trailing = { ChevronDownIcon(16.dp, OnBackground) }, width = 138.dp)
                    FilterChip("주문 상태", trailing = { ChevronDownIcon(16.dp, OnBackground) }, width = 141.dp)
                }
                Spacer(Modifier.height(22.dp))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (hasCurrent) {
                        OrderCard(
                            thumb = currentThumb(state.selectedStoreId),
                            storeName = currentStoreName(state),
                            summary = currentSummary(state),
                            statusLabel = listLabel(state.status),
                            current = true,
                        )
                    }
                    state.pastOrders.forEachIndexed { i, past ->
                        OrderCard(
                            thumb = pastThumb(past.id),
                            storeName = past.storeName,
                            summary = "${past.summary} · ${past.dateLabel}",
                            statusLabel = past.statusLabel,
                            // 지금 주문이 없으면 가장 최근 지난 주문을 골라 오른쪽에 보여준다
                            current = !hasCurrent && i == 0,
                            labelColor = PastStatusColor,
                        )
                    }
                }
            }
            Spacer(Modifier.width(25.dp))
            // 오른쪽: 상세 패널
            Box(
                Modifier
                    .weight(648f)
                    .fillMaxHeight()
                    .padding(top = 21.dp, bottom = 14.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Surface),
            ) {
                when {
                    state.status == OrderStatus.Arrived -> ReceiveQuestion(onEvent)
                    hasCurrent -> CurrentOrderDetail(state)
                    state.pastOrders.isNotEmpty() -> PastOrderDetail(state.pastOrders.first())
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        HintText("주문내역이 없어요")
                    }
                }
            }
        }
    }
}

// ---------------- 왼쪽 목록 ----------------

@Composable
private fun OrderCard(
    @DrawableRes thumb: Int?,
    storeName: String,
    summary: String,
    statusLabel: String,
    current: Boolean,
    labelColor: Color = Primary,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(shape)
            .background(SurfaceLow)
            .then(if (current) Modifier.border(1.dp, Primary, shape) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 사진 + 오른쪽으로 카드 색에 섞이는 그라데이션
        Box(Modifier.width(127.dp).fillMaxHeight()) {
            if (thumb != null) {
                Image(
                    painterResource(thumb),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(SurfaceHigh))
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(27.dp)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(SurfaceLow.copy(alpha = 0f), SurfaceLow))),
            )
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(
                storeName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(statusLabel, style = MaterialTheme.typography.bodyLarge, color = if (current) Primary else labelColor)
            Spacer(Modifier.width(4.dp))
            ChevronIcon(pointsLeft = false, size = 16.dp, color = if (current) Primary else labelColor)
        }
        Spacer(Modifier.width(18.dp))
    }
}

/** 테두리 알약 (검색 / 조회 기간 ⌄ / 주문 상태 ⌄). 모양만 있다 */
@Composable
private fun FilterChip(
    text: String,
    width: Dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(48.dp)
    Row(
        Modifier.width(width).height(40.dp).clip(shape).border(1.dp, Outline, shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            color = ChipTextColor,
            maxLines = 1,
        )
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
    }
}

// ---------------- 오른쪽 상세 ----------------

@Composable
private fun CurrentOrderDetail(state: OrderUiState) {
    val status = state.status
    Box(Modifier.fillMaxSize()) {
        StatusIllustration(status)
        Column(Modifier.fillMaxSize().padding(start = 42.dp, end = 34.dp, top = 44.dp, bottom = 26.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                StoreHeader(storeIcon(state.selectedStoreId), currentStoreName(state))
                Spacer(Modifier.height(24.dp))
                Text(headline(status), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(5.dp))
                Text(subline(state), style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                Spacer(Modifier.height(57.dp))
                StepProgress(doneCount(status), Modifier.padding(end = 6.dp))

                if (status == OrderStatus.RiderComing) {
                    Spacer(Modifier.height(36.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        ReceiveHintCard(
                            icon = R.drawable.status_ic_car,
                            text = AnnotatedString("${state.options.window.label}에서 수령할게요"),
                            width = 250.dp,
                        )
                        val hazard = state.delivery?.locateBy == LocateBy.Hazard
                        ReceiveHintCard(
                            icon = R.drawable.status_ic_hazard,
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = if (hazard) Primary else TextBody)) { append("비상등") }
                                append("을 켜주세요")
                            },
                            width = 236.dp,
                        )
                    }
                    Spacer(Modifier.height(19.dp))
                } else {
                    Spacer(Modifier.height(39.dp))
                    SectionLabel("주문정보")
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.padding(start = 15.dp)) {
                        InfoRow("주문번호", state.receipt?.number.orEmpty())
                        InfoRow("주문일시", state.receipt?.orderedAt.orEmpty())
                        InfoRow("배달방식", "내 차량에서 수령")
                    }
                    Spacer(Modifier.height(16.dp))
                }

                SectionLabel("주문 상세")
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(start = 15.dp, end = 16.dp)) {
                    state.cart.forEach { LineItemRow(it.menu.name, it.quantity, it.totalPrice) }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalLine()
                Column(Modifier.padding(start = 15.dp, end = 16.dp)) {
                    TotalRow("합계", state.payTotal, note = "배달팁 포함")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                PillButton("매장에 전화하기", Modifier.weight(1f)) {
                    Image(painterResource(R.drawable.status_ic_phone), null, Modifier.size(23.dp))
                }
                PillButton("추가 주문하기", Modifier.weight(1f)) {
                    LineIcon(plus = true, size = 22.dp, color = OnBackground)
                }
            }
        }
    }
}

/** 지금 주문이 없을 때: 가장 최근 지난 주문을 수령 완료로 보여준다 */
@Composable
private fun PastOrderDetail(past: PastOrder) {
    Box(Modifier.fillMaxSize()) {
        StatusIllustration(OrderStatus.Received)
        Column(Modifier.fillMaxSize().padding(start = 42.dp, end = 34.dp, top = 44.dp, bottom = 26.dp)) {
            StoreHeader(null, past.storeName)
            Spacer(Modifier.height(24.dp))
            Text(headline(OrderStatus.Received), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(5.dp))
            Text("맛있게 드세요!", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            Spacer(Modifier.height(57.dp))
            StepProgress(4, Modifier.padding(end = 6.dp))
            Spacer(Modifier.height(39.dp))
            SectionLabel("주문 상세")
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(start = 15.dp)) {
                InfoRow("주문 메뉴", past.summary)
                InfoRow("주문일시", past.dateLabel)
                InfoRow("주문 상태", past.statusLabel)
            }
        }
    }
}

@Composable
private fun StoreHeader(@DrawableRes icon: Int?, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Image(painterResource(icon), null, Modifier.size(30.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceHigh))
        }
        Spacer(Modifier.width(11.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = StoreNameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 상태 그림. 피그마처럼 패널 오른쪽 위에 둔다 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.StatusIllustration(status: OrderStatus) {
    data class Slot(val res: Int, val w: Dp, val h: Dp, val end: Dp, val top: Dp)
    val slot = when (status) {
        OrderStatus.Ordered -> Slot(R.drawable.status_ordered, 119.dp, 135.dp, 59.dp, 45.dp)
        OrderStatus.Cooking -> Slot(R.drawable.status_cooking, 118.dp, 142.dp, 63.dp, 50.dp)
        OrderStatus.Delivering, OrderStatus.RiderComing, OrderStatus.Arrived ->
            Slot(R.drawable.status_delivering, 153.dp, 136.dp, 22.dp, 44.dp)
        OrderStatus.Received -> Slot(R.drawable.status_received, 118.dp, 90.dp, 65.dp, 74.dp)
        else -> return
    }
    Image(
        painterResource(slot.res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = slot.end, top = slot.top)
            .size(slot.w, slot.h),
    )
}

/** 곧 도착: 받는 창문 / 비상등 안내 카드. 모양만 있다 */
@Composable
private fun ReceiveHintCard(@DrawableRes icon: Int, text: AnnotatedString, width: Dp) {
    Column(
        Modifier
            .width(width)
            .height(151.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(31.dp))
        Image(painterResource(icon), null, Modifier.size(53.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp),
            color = TextBody,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** 배달기사 도착: 음식을 수령하셨나요? [네] [아니요 못 받았어요.] */
@Composable
private fun ReceiveQuestion(onEvent: (OrderEvent) -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(76.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
            Text("?", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 44.sp), color = OnPrimary)
        }
        Spacer(Modifier.height(28.dp))
        Text("음식을 수령하셨나요?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(26.dp))
        QuestionButton("네") { onEvent(OrderEvent.ConfirmReceived) }
        Spacer(Modifier.height(8.dp))
        QuestionButton("아니요 못 받았어요.") { onEvent(OrderEvent.NotReceived) }
    }
}

@Composable
private fun QuestionButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(246.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(58.dp))
            .background(ButtonSecondary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = OnBackground, maxLines = 1)
    }
}

/** 아이콘 + 글자 보조 버튼 (매장에 전화하기 / 추가 주문하기). 모양만 있다 */
@Composable
private fun PillButton(text: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(58.dp)).background(ButtonSecondary),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = OnBackground, maxLines = 1)
    }
}

@Composable
private fun SearchIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.09f
        drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(stroke))
        drawLine(color, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.9f, w * 0.9f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun ChevronDownIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.09f
        drawLine(color, Offset(w * 0.15f, w * 0.35f), Offset(w * 0.5f, w * 0.7f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, w * 0.7f), Offset(w * 0.85f, w * 0.35f), stroke, StrokeCap.Round)
    }
}

// ---------------- 상태 → 글자·그림 ----------------

private fun doneCount(status: OrderStatus): Int = when (status) {
    OrderStatus.Ordered -> 1
    OrderStatus.Cooking -> 2
    OrderStatus.Delivering, OrderStatus.RiderComing, OrderStatus.Arrived -> 3
    OrderStatus.Received -> 4
    else -> 0
}

private fun headline(status: OrderStatus): String = when (status) {
    OrderStatus.Ordered -> "주문이 접수되었어요."
    OrderStatus.Cooking -> "사장님이 조리중이에요"
    OrderStatus.Delivering -> "배달이 시작됐어요"
    OrderStatus.RiderComing, OrderStatus.Arrived -> "라이더님이 곧 도착해요."
    OrderStatus.Received -> "배달이 완료되었어요"
    else -> ""
}

private fun listLabel(status: OrderStatus): String = when (status) {
    OrderStatus.Ordered -> "주문 접수"
    OrderStatus.Cooking -> "조리 중"
    OrderStatus.Delivering -> "배달 시작"
    OrderStatus.RiderComing -> "곧 도착"
    OrderStatus.Arrived -> "도착"
    OrderStatus.Received -> "수령 완료"
    else -> ""
}

/** 제목 아래 한 줄: "약 N분 후 배달이 완료되요." / "수령 준비를 해주세요." / "맛있게 드세요!" */
private fun subline(state: OrderUiState): AnnotatedString = buildAnnotatedString {
    when (state.status) {
        OrderStatus.RiderComing, OrderStatus.Arrived -> append("수령 준비를 해주세요.")
        OrderStatus.Received -> append("맛있게 드세요!")
        else -> {
            val eta = state.etaMin
            if (eta != null) {
                append("약 ")
                withStyle(SpanStyle(color = Primary)) { append("${eta}분 후") }
                append(" 배달이 완료되요.")
            }
        }
    }
}

private fun currentStoreName(state: OrderUiState): String =
    state.cart.firstOrNull()?.menu?.storeName
        ?: state.stores.firstOrNull { it.id == state.selectedStoreId }?.name
        ?: ""

/** "고추참치김밥 외 1개 · 오늘 11:29" */
private fun currentSummary(state: OrderUiState): String {
    val first = state.cart.firstOrNull() ?: return ""
    val rest = state.cart.size - 1
    val items = if (rest > 0) "${first.menu.name} 외 ${rest}개" else first.menu.name
    val time = state.receipt?.orderedAt?.let { Regex("""\d{1,2}:\d{2}""").findAll(it).lastOrNull()?.value }
    return if (time != null) "$items · 오늘 $time" else "$items · 오늘"
}

@DrawableRes
private fun currentThumb(storeId: String?): Int? = when (storeId) {
    "s2-a" -> R.drawable.status_thumb_daom
    "s2-b" -> R.drawable.status_thumb_bhc
    else -> null
}

@DrawableRes
private fun storeIcon(storeId: String?): Int? = when (storeId) {
    "s2-a" -> R.drawable.status_store_daom
    "s2-b" -> R.drawable.status_store_bhc
    else -> null
}

@DrawableRes
private fun pastThumb(id: String): Int? = when (id) {
    "past-1" -> R.drawable.status_thumb_past1
    "past-2" -> R.drawable.status_thumb_past2
    "past-3" -> R.drawable.status_thumb_past3
    "past-4" -> R.drawable.status_thumb_past4
    else -> null
}

// ---------------- 미리보기 ----------------

private object OrderHistorySamples {
    private val menus = listOf(
        Menu("s2-3", "다옴 양재온누리점", "고추참치김밥", 6_000),
        Menu("s2-1", "다옴 양재온누리점", "다옴김밥", 4_000),
    )
    private val past = listOf(
        PastOrder("past-1", "엘빙할루카", "생망고 케이크 외 2개", "어제 12:45", "수령 완료"),
        PastOrder("past-2", "올리앤 양재점", "상하이 파스타 외 1개", "11/11 (수)", "수령 완료"),
        PastOrder("past-3", "메가MGC커피 서초양재점", "(ICE)아메리카노 외 2개", "11.09 (월)", "수령 완료"),
        PastOrder("past-4", "호랑이초밥 양재점", "런치호랑이특(12p)", "11.05 (목)", "수령 완료"),
    )
    val ordered = OrderUiState(
        selectedStoreId = "s2-a",
        cart = menus.map { CartItem(it, 1) },
        deliveryFee = 2_300,
        status = OrderStatus.Ordered,
        etaMin = 41,
        receipt = OrderReceipt("H4ANDAE0013", "2026.11.13(금) 11:29"),
        pastOrders = past,
        options = OrderOptions(window = ReceiveWindow.Driver, locateBy = LocateBy.Hazard),
        delivery = DeliveryInfo(
            placeLabel = "지금 차 위치", latitude = 37.47, longitude = 127.03,
            plate = "12가 3456", color = "흰색", carModel = "IONIQ 6",
            window = ReceiveWindow.Driver, locateBy = LocateBy.Hazard,
        ),
    )
    val cooking = ordered.copy(status = OrderStatus.Cooking, etaMin = 31)
    val delivering = ordered.copy(status = OrderStatus.Delivering, etaMin = 21)
    val riderComing = ordered.copy(status = OrderStatus.RiderComing, etaMin = 3, riderDistanceM = 180)
    val arrived = riderComing.copy(status = OrderStatus.Arrived, riderDistanceM = 10)
    val received = ordered.copy(status = OrderStatus.Received, etaMin = null)
    val browsing = OrderUiState(pastOrders = past)
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryOrderedPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.ordered) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryCookingPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.cooking) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryDeliveringPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.delivering) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryRiderComingPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.riderComing) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryArrivedPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.arrived) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryReceivedPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.received) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderHistoryNoCurrentPreview() {
    CardeliTheme { OrderHistoryScreen(OrderHistorySamples.browsing) {} }
}
