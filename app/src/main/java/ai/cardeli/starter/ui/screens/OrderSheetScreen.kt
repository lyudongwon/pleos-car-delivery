package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.R
import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.DeliveryMethod
import ai.cardeli.starter.contract.LocateBy
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderOptions
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.PaymentMethod
import ai.cardeli.starter.contract.ReceiveWindow
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.CheckIcon
import ai.cardeli.starter.ui.components.ChevronIcon
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.Panel
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.components.SectionLabel
import ai.cardeli.starter.ui.components.won
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.Inactive
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.OnPrimary
import ai.cardeli.starter.ui.theme.Outline
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.SurfaceHigh
import ai.cardeli.starter.ui.theme.SurfaceLow
import ai.cardeli.starter.ui.theme.SurfaceSunken
import ai.cardeli.starter.ui.theme.Suit
import ai.cardeli.starter.ui.theme.TextHint
import ai.cardeli.starter.ui.theme.TextSub
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * 주문하기 시트 (배달). 피그마 8:12671 ~ 8:16268, 결제 완료 8:16761.
 *
 * 왼쪽: 담은 메뉴 → 배달방식 → 가게 요청사항 → (배달방식을 다 고르면) 결제방식
 * 오른쪽: 단계별 패널. 받을 창문 → 내 차 위치 알리기 → (결제방식을 고르면) 결제 금액 요약
 * 화면은 state를 읽고 OrderEvent만 보낸다. 단계는 이 화면 안에서만 기억한다.
 */

// 피그마 값 (테마에 없는 것만)
private val SubText = Color(0xA3FFFFFF)        // rgba(255,255,255,0.62)
private val ItemText = Color(0xFFDFDFDF)       // 메뉴 카드 보조 글
private val PillText = Color(0xFFC9C9C9)       // 안 고른 창문 글자
private val CaptionText = Color(0xFFBEBEBE)    // 위치 알림 카드 설명
private val Divider = Color(0xFF4C4E59)        // 금액 구분선, 창문 연결선

private fun suit(size: Int, weight: FontWeight, color: Color = OnBackground) =
    TextStyle(fontFamily = Suit, fontSize = size.sp, fontWeight = weight, color = color)

/** 오른쪽 패널 진행 단계 */
private enum class SheetStep { Method, Window, Locate, Done }

private const val NoSpoonFork = "수저, 포크 안 받기"
private const val NoSideDish = "기본반찬 안 받기"

@Composable
fun OrderSheetScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    OrderSheetContent(state, onEvent)
}

@Composable
private fun OrderSheetContent(
    state: OrderUiState,
    onEvent: (OrderEvent) -> Unit,
    initialStep: SheetStep = SheetStep.Method,
    initialPicked: Boolean = false,
) {
    val options = state.options
    var step by rememberSaveable { mutableStateOf(initialStep) }
    // 피그마는 처음에 아무것도 고르지 않은 모습이라, 사용자가 직접 누른 뒤에만 선택 표시를 한다
    var windowPicked by rememberSaveable { mutableStateOf(initialPicked) }
    var locatePicked by rememberSaveable { mutableStateOf(initialPicked) }
    var paymentPicked by rememberSaveable { mutableStateOf(initialPicked) }

    AppScaffold(active = NavTab.Home) {
        ScreenTitle("주문하기", onBack = { onEvent(OrderEvent.BackToMenu) })
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxSize().padding(start = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            // ---------- 왼쪽: 주문서 ----------
            Column(
                Modifier.weight(627f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CartPanel(state.cart)

                SheetPanel("배달방식") {
                    if (step == SheetStep.Done && options.method == DeliveryMethod.ToCar) {
                        DeliverySummaryCard(options, state.isParked) { step = SheetStep.Window }
                    } else {
                        val chosen = step != SheetStep.Method
                        Column(
                            Modifier.padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            MethodCard(
                                method = DeliveryMethod.ToCar,
                                imageRes = R.drawable.sheet_method_tocar,
                                imageWidth = 165.dp,
                                imageHeight = 98.dp,
                                subtitle = if (state.isParked) "기어 P · 지금 차 위치로 받아요" else null,
                                selected = chosen && options.method == DeliveryMethod.ToCar,
                            ) {
                                onEvent(OrderEvent.SelectMethod(DeliveryMethod.ToCar))
                                step = SheetStep.Window
                            }
                            MethodCard(
                                method = DeliveryMethod.PickupPoint,
                                imageRes = R.drawable.sheet_method_pickuppoint,
                                imageWidth = 172.dp,
                                imageHeight = 115.dp,
                                subtitle = null,
                                selected = chosen && options.method == DeliveryMethod.PickupPoint,
                            ) {
                                onEvent(OrderEvent.SelectMethod(DeliveryMethod.PickupPoint))
                                step = SheetStep.Done
                            }
                        }
                    }
                }

                StoreNotePanel(options.storeNote) { onEvent(OrderEvent.SetStoreNote(it)) }

                if (step == SheetStep.Done) {
                    PaymentPanel(selected = if (paymentPicked) options.payment else null) {
                        paymentPicked = true
                        onEvent(OrderEvent.SelectPayment(it))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ---------- 오른쪽: 단계별 패널 ----------
            Box(Modifier.weight(589f).fillMaxHeight()) {
                when (step) {
                    SheetStep.Method -> Unit
                    SheetStep.Window -> WindowPanel(
                        selected = if (windowPicked) options.window else null,
                        carModel = state.delivery?.carModel,
                        onSelect = {
                            windowPicked = true
                            onEvent(OrderEvent.SelectWindow(it))
                        },
                        onDone = { step = SheetStep.Locate },
                        modifier = Modifier.fillMaxSize(),
                    )
                    SheetStep.Locate -> LocateColumn(
                        selected = if (locatePicked) options.locateBy else null,
                        riderNote = options.riderNote,
                        onSelect = {
                            locatePicked = true
                            onEvent(OrderEvent.SelectLocateBy(it))
                        },
                        onRiderNote = { onEvent(OrderEvent.SetRiderNote(it)) },
                        onDone = { step = SheetStep.Done },
                    )
                    SheetStep.Done -> if (paymentPicked) {
                        PaySummary(
                            menuPrice = state.totalPrice,
                            deliveryFee = state.deliveryFee,
                            payTotal = state.payTotal,
                            enabled = state.cart.isNotEmpty(),
                            onPay = { onEvent(OrderEvent.PlaceOrder) },
                            modifier = Modifier.align(Alignment.TopEnd).width(456.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 결제 완료 (피그마 8:16761) */
@Composable
fun PaymentDoneScreen() {
    AppScaffold(active = NavTab.Home) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.offset(y = (-48).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(90.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center,
                ) { CheckIcon(size = 48.dp, color = OnPrimary) }
                Spacer(Modifier.height(26.dp))
                Text("결제 완료!", style = suit(32, FontWeight.Bold))
                Spacer(Modifier.height(14.dp))
                Text("주문이 성공적으로 전달되었어요.", style = suit(16, FontWeight.Normal, TextSub))
            }
        }
    }
}

// ======================= 왼쪽 패널 =======================

/** 섹션 카드: 피그마 #16171b, 모서리 20, 제목 20 Bold */
@Composable
private fun SheetPanel(title: String, content: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth(), color = SurfaceLow, padding = 0.dp) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 25.dp, bottom = 25.dp)) {
            Box(Modifier.padding(start = 4.dp)) { SectionLabel(title) }
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun CartPanel(cart: List<CartItem>) {
    SheetPanel("담은 메뉴 (${cart.sumOf { it.quantity }})") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cart.forEach { CartLine(it) }
        }
    }
}

/** 담은 메뉴 한 줄: 이름, 수량, 가격, 오른쪽 사진 */
@Composable
private fun CartLine(item: CartItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(121.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHigh)
            .padding(start = 27.dp, end = 27.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.menu.name, style = suit(24, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("수량 ${item.quantity}개", style = suit(14, FontWeight.Normal, ItemText))
            Spacer(Modifier.height(10.dp))
            Text(won(item.totalPrice), style = suit(20, FontWeight.Bold))
        }
        MenuThumb(item.menu.id, Modifier.size(width = 75.dp, height = 74.dp))
    }
}

/** sheet_menu_<id> 사진이 있으면 보여주고, 없으면 회색 칸 */
@Composable
private fun MenuThumb(menuId: String, modifier: Modifier) {
    val context = LocalContext.current
    val name = "sheet_menu_" + menuId.lowercase().replace('-', '_')
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    val shaped = modifier.clip(RoundedCornerShape(10.dp))
    if (id != 0) {
        Image(painterResource(id), contentDescription = null, modifier = shaped, contentScale = ContentScale.Crop)
    } else {
        Box(shaped.background(SurfaceSunken))
    }
}

/** 배달방식 카드 (그림 + 제목). 고르면 노란 테두리와 오른쪽 화살표 */
@Composable
private fun MethodCard(
    method: DeliveryMethod,
    imageRes: Int,
    imageWidth: Dp,
    imageHeight: Dp,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(shape)
            .background(SurfaceSunken)
            .border(1.dp, if (selected) Primary else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(176.dp), contentAlignment = Alignment.Center) {
            Image(
                painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(imageWidth, imageHeight),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(method.label, style = suit(24, FontWeight.Bold))
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = suit(16, FontWeight.SemiBold, TextSub))
            }
        }
        if (selected) ChevronIcon(pointsLeft = false, size = 22.dp, color = OnBackground)
    }
}

/** 배달방식을 다 고른 뒤 접힌 카드 (피그마 8:15265). 누르면 창문 고르기부터 다시 */
@Composable
private fun DeliverySummaryCard(options: OrderOptions, isParked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 115.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(start = 27.dp, end = 25.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(DeliveryMethod.ToCar.label, style = suit(24, FontWeight.Bold))
            Spacer(Modifier.height(12.dp))
            val detail = suit(14, FontWeight.Normal, ItemText)
            Text("수령할 창문: ${options.window.label}", style = detail)
            Text(options.locateBy.label, style = detail)
            if (isParked) Text("기어 P · 지금 차 위치로 받아요", style = detail.copy(color = TextSub))
        }
        ChevronIcon(pointsLeft = false, size = 22.dp, color = OnBackground)
    }
}

/** 가게 요청사항: 체크박스 두 개 + 입력칸. 체크하면 그 문구를 요청사항에 붙인다 */
@Composable
private fun StoreNotePanel(note: String, onNote: (String) -> Unit) {
    SheetPanel("가게 요청사항") {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                listOf(NoSpoonFork, NoSideDish).forEach { text ->
                    var checked by rememberSaveable(text) { mutableStateOf(note.contains(text)) }
                    CheckRow(text, checked) {
                        checked = !checked
                        onNote(if (checked) appendNote(note, text) else removeNote(note, text))
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
            BoxNoteField(note, onNote, "요청사항 없음", Modifier.fillMaxWidth())
        }
    }
}

private fun appendNote(note: String, text: String): String =
    if (note.contains(text)) note else if (note.isBlank()) text else "${note.trim()}, $text"

private fun removeNote(note: String, text: String): String =
    note.replace(", $text", "").replace("$text, ", "").replace(text, "").trim()

@Composable
private fun CheckRow(text: String, checked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.clip(shape).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(shape)
                .then(if (checked) Modifier.background(Primary) else Modifier.border(1.dp, Outline, shape)),
            contentAlignment = Alignment.Center,
        ) { if (checked) CheckIcon(size = 20.dp, color = OnPrimary) }
        Spacer(Modifier.width(6.dp))
        Text(text, style = suit(16, FontWeight.SemiBold))
    }
}

/** 여러 줄 입력칸 (높이 105, 테두리 #3f424b) */
@Composable
private fun BoxNoteField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .height(105.dp)
            .clip(shape)
            .border(1.dp, Outline, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, style = suit(16, FontWeight.Normal, TextHint))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = suit(16, FontWeight.Normal),
            cursorBrush = SolidColor(Primary),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 결제방식 칩 4개 (3 + 1) */
@Composable
private fun PaymentPanel(selected: PaymentMethod?, onSelect: (PaymentMethod) -> Unit) {
    SheetPanel("결제방식") {
        Column(
            Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PaymentMethod.values().toList().chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { p ->
                        val shape = RoundedCornerShape(17.dp)
                        Box(
                            Modifier
                                .size(width = 187.dp, height = 61.dp)
                                .clip(shape)
                                .background(SurfaceSunken)
                                .border(1.dp, if (p == selected) Primary else Color.Transparent, shape)
                                .clickable { onSelect(p) },
                            contentAlignment = Alignment.Center,
                        ) { Text(p.label, style = suit(20, FontWeight.SemiBold), maxLines = 1) }
                    }
                }
            }
        }
    }
}

// ======================= 오른쪽 패널 =======================

@Composable
private fun PanelHeader(title: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = suit(24, FontWeight.Bold))
        Spacer(Modifier.height(5.dp))
        Text(sub, style = suit(15, FontWeight.SemiBold, SubText))
    }
}

/** 노란 알약 버튼 (선택완료 161×46, 결제하기 391×46) */
@Composable
private fun YellowButton(text: String, textSize: Int, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(if (enabled) Primary else Inactive)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = suit(textSize, FontWeight.Bold, OnPrimary)) }
}

/** "음식을 수령할 창문을 선택해주세요." 차 위에서 본 그림 + 창문 네 개 (피그마 8:13239, 8:13731) */
@Composable
private fun WindowPanel(
    selected: ReceiveWindow?,
    carModel: String?,
    onSelect: (ReceiveWindow) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    Panel(modifier, color = SurfaceLow, padding = 0.dp) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(690.dp)) {
                PanelHeader(
                    "음식을 수령할 창문을 선택해주세요.",
                    "선택사항이 라이더님에게 전달될거예요.",
                    Modifier.padding(start = 32.dp, top = 37.dp),
                )
                if (!carModel.isNullOrEmpty()) {
                    Text(
                        "인식된 차량: $carModel",
                        style = suit(15, FontWeight.Normal, SubText),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 149.dp),
                    )
                }
                Image(
                    painterResource(R.drawable.sheet_car_top),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = (-12).dp, y = 121.dp)
                        .size(width = 356.dp, height = 534.dp),
                    contentScale = ContentScale.Fit,
                )
                // 왼쪽 = 운전석 쪽, 오른쪽 = 조수석 쪽 (차 앞이 위)
                WindowRow(ReceiveWindow.Driver, ReceiveWindow.Passenger, selected, onSelect, Modifier.padding(top = 346.dp))
                WindowRow(ReceiveWindow.RearLeft, ReceiveWindow.RearRight, selected, onSelect, Modifier.padding(top = 417.dp))
                if (selected != null) {
                    YellowButton(
                        "선택완료",
                        20,
                        Modifier.align(Alignment.TopCenter).padding(top = 616.dp).width(161.dp),
                        onClick = onDone,
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowRow(
    left: ReceiveWindow,
    right: ReceiveWindow,
    selected: ReceiveWindow?,
    onSelect: (ReceiveWindow) -> Unit,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).padding(end = 60.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WindowPill(left, selected == left) { onSelect(left) }
            Spacer(Modifier.width(5.dp))
            ConnectorLine(dotAtEnd = true)
        }
        Row(
            Modifier.weight(1f).padding(start = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectorLine(dotAtEnd = false)
            Spacer(Modifier.width(5.dp))
            WindowPill(right, selected == right) { onSelect(right) }
        }
    }
}

@Composable
private fun WindowPill(window: ReceiveWindow, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(59.dp)
    Box(
        Modifier
            .height(39.dp)
            .widthIn(min = 104.dp)
            .clip(shape)
            .background(if (selected) Outline else Color.Transparent)
            .border(1.dp, Inactive, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            window.label,
            style = suit(20, FontWeight.SemiBold, if (selected) OnBackground else PillText),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** 창문 이름에서 차까지 이어지는 선. 차 쪽 끝에 점 */
@Composable
private fun ConnectorLine(dotAtEnd: Boolean) {
    Canvas(Modifier.size(width = 62.dp, height = 6.dp)) {
        val r = 2.67.dp.toPx()
        val y = size.height / 2
        val dotX = if (dotAtEnd) size.width - r else r
        val lineFrom = if (dotAtEnd) 0f else r
        val lineTo = if (dotAtEnd) size.width - r else size.width
        drawLine(Divider, Offset(lineFrom, y), Offset(lineTo, y), strokeWidth = 1.dp.toPx())
        drawCircle(Divider, radius = r, center = Offset(dotX, y))
    }
}

/** "내 차 위치를 어떻게 알릴까요?" + "추가 전달 사항" + 선택완료 (피그마 8:14242, 8:14755) */
@Composable
private fun LocateColumn(
    selected: LocateBy?,
    riderNote: String,
    onSelect: (LocateBy) -> Unit,
    onRiderNote: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Panel(Modifier.fillMaxWidth(), color = SurfaceLow, padding = 0.dp) {
            PanelHeader(
                "내 차 위치를 어떻게 알릴까요?",
                "선택사항이 라이더님에게 전달될거예요.",
                Modifier.padding(start = 32.dp, top = 37.dp),
            )
            Spacer(Modifier.height(43.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterHorizontally),
            ) {
                LocateCard(
                    title = LocateBy.Hazard.label,
                    caption = "정차 중일때만 가능해요.",
                    imageRes = R.drawable.sheet_hazard_car,
                    imageTop = 8.dp,
                    imageWidth = 225.dp,
                    imageHeight = 150.dp,
                    selected = selected == LocateBy.Hazard,
                ) { onSelect(LocateBy.Hazard) }
                LocateCard(
                    title = LocateBy.CarInfo.label,
                    caption = "전송된 정보는 배달 완료 후 자동 삭제되요.",
                    imageRes = R.drawable.sheet_license_plate,
                    imageTop = 27.dp,
                    imageWidth = 217.dp,
                    imageHeight = 145.dp,
                    selected = selected == LocateBy.CarInfo,
                ) { onSelect(LocateBy.CarInfo) }
            }
            Spacer(Modifier.height(38.dp))
        }
        Spacer(Modifier.height(15.dp))
        Panel(Modifier.fillMaxWidth(), color = SurfaceLow, padding = 0.dp) {
            Column(Modifier.padding(start = 31.dp, end = 31.dp, top = 28.dp, bottom = 28.dp)) {
                PanelHeader("추가 전달 사항", "선택사항이 라이더님에게 전달될거예요.")
                Spacer(Modifier.height(18.dp))
                BoxNoteField(riderNote, onRiderNote, "요청사항 없음", Modifier.fillMaxWidth())
            }
        }
        if (selected != null) {
            Spacer(Modifier.height(22.dp))
            YellowButton("선택완료", 20, Modifier.width(161.dp), onClick = onDone)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LocateCard(
    title: String,
    caption: String,
    imageRes: Int,
    imageTop: Dp,
    imageWidth: Dp,
    imageHeight: Dp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(width = 249.dp, height = 224.dp)
            .clip(shape)
            .background(SurfaceSunken)
            .border(1.dp, if (selected) Primary else Color.Transparent, shape)
            .clickable(onClick = onClick),
    ) {
        Image(
            painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = imageTop).size(imageWidth, imageHeight),
            contentScale = ContentScale.Fit,
        )
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 150.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = suit(20, FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            Text(caption, style = suit(12, FontWeight.SemiBold, CaptionText), textAlign = TextAlign.Center)
        }
    }
}

/** 메뉴 금액 · 배달팁 · 할인금액 · 결제금액 + 결제하기 (피그마 8:2671) */
@Composable
private fun PaySummary(
    menuPrice: Int,
    deliveryFee: Int,
    payTotal: Int,
    enabled: Boolean,
    onPay: () -> Unit,
    modifier: Modifier,
) {
    Panel(modifier, color = SurfaceLow, padding = 0.dp) {
        Column(Modifier.padding(start = 32.dp, end = 32.dp, top = 35.dp, bottom = 22.dp)) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountRow("메뉴 금액", won(menuPrice))
                AmountRow("배달팁", won(deliveryFee))
                AmountRow("할인금액", won(0), color = Primary)
            }
            Spacer(Modifier.height(19.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("결제금액", style = suit(24, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                Text(won(payTotal), style = suit(24, FontWeight.SemiBold))
            }
            Spacer(Modifier.height(32.dp))
            YellowButton("결제하기", 24, Modifier.fillMaxWidth(), enabled = enabled, onClick = onPay)
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, color: Color = OnBackground) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = suit(20, FontWeight.Normal, color), modifier = Modifier.weight(1f))
        Text(value, style = suit(20, FontWeight.Normal, color))
    }
}

// ======================= 미리보기 =======================

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderSheetPreview() {
    CardeliTheme { OrderSheetScreen(PreviewStates.confirming) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderSheetWindowPreview() {
    CardeliTheme { OrderSheetContent(PreviewStates.confirming, {}, SheetStep.Window, initialPicked = true) }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderSheetLocatePreview() {
    val state = PreviewStates.confirming.copy(options = OrderOptions(locateBy = LocateBy.Hazard))
    CardeliTheme { OrderSheetContent(state, {}, SheetStep.Locate, initialPicked = true) }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun OrderSheetPayPreview() {
    val state = PreviewStates.confirming.copy(
        options = OrderOptions(locateBy = LocateBy.Hazard, payment = PaymentMethod.HyundaiPay),
    )
    CardeliTheme { OrderSheetContent(state, {}, SheetStep.Done, initialPicked = true) }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun PaymentDonePreview() {
    CardeliTheme { PaymentDoneScreen() }
}
