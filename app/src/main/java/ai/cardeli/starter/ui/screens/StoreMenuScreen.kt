package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.R
import ai.cardeli.starter.contract.CartItem
import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.StoreCard
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.LineIcon
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.PrimaryButton
import ai.cardeli.starter.ui.components.won
import ai.cardeli.starter.ui.theme.Background
import ai.cardeli.starter.ui.theme.ButtonSecondary
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.Outline
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.SurfaceHigh
import ai.cardeli.starter.ui.theme.SurfaceLow
import ai.cardeli.starter.ui.theme.Suit
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * 가게 메뉴판 (피그마 8:11530 빈 장바구니, 8:12131 최소주문 미달, 45:13547 두 개 담김, 24:9080 픽업).
 * 왼쪽: 가게 정보 + 배달/픽업 + 내 장바구니. 오른쪽: 분류 탭 + 메뉴 목록.
 * 화면은 state만 읽고 사용자 동작은 OrderEvent로 올려 보낸다.
 */
@Composable
fun StoreMenuScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    val store = state.stores.firstOrNull { it.id == state.selectedStoreId }
    AppScaffold(active = NavTab.Home, location = null) {
        Row(Modifier.fillMaxSize()) {
            StoreColumn(state, store, onEvent, Modifier.weight(655f).fillMaxHeight())
            Spacer(Modifier.width(40.dp))
            MenuColumn(state, onEvent, Modifier.weight(581f).fillMaxHeight())
        }
    }
}

// ---------------- 피그마 값 ----------------

private val TextDesc = Color(0xFFDFDFDF)       // 메뉴 설명, 옵션 글
private val TextLastOrder = Color(0xFFCECECE)  // 라스트오더, "더 담아야해요"
private val TextReview = Color(0xFFDADADA)     // 리뷰 N

private fun suit(size: Int, weight: FontWeight, color: Color = OnBackground) =
    TextStyle(fontFamily = Suit, fontSize = size.sp, fontWeight = weight, color = color)

/** 메뉴 id → 피그마 사진. 사진이 없는 가게 메뉴는 null (빈 자리 표시) */
@DrawableRes
private fun menuPhoto(menuId: String): Int? = when {
    menuId == "s2-1" -> R.drawable.menu_daom_gimbap
    menuId == "s2-2" -> R.drawable.menu_chamchi_gimbap
    menuId == "s2-3" -> R.drawable.menu_gochu_chamchi_gimbap
    menuId == "s2-4" -> R.drawable.menu_mukeunji_chamchi_gimbap
    menuId == "s2-5" -> R.drawable.menu_nalchial_gimbap
    menuId.startsWith("s2b-") -> R.drawable.menu_bburinkle_combo
    else -> null
}

// ---------------- 왼쪽: 가게 정보 + 장바구니 ----------------

@Composable
private fun StoreColumn(
    state: OrderUiState,
    store: StoreCard?,
    onEvent: (OrderEvent) -> Unit,
    modifier: Modifier,
) {
    val storeName = store?.name ?: state.menus.firstOrNull()?.storeName.orEmpty()

    Column(modifier.padding(start = 31.dp, bottom = 7.dp)) {
        Spacer(Modifier.height(11.dp))
        // ‹ 가게 이름 ★ 4.6 (89) >
        Row(Modifier.offset(x = (-28).dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(37.dp).height(44.dp).clickable { onEvent(OrderEvent.BackToStores) },
                contentAlignment = Alignment.CenterStart,
            ) {
                Image(ChevronLeftIcon, contentDescription = "뒤로", modifier = Modifier.padding(start = 12.dp).size(20.dp))
            }
            Text(storeName, style = suit(36, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (store != null) {
                Spacer(Modifier.width(6.dp))
                Image(StarIcon, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)) {
                            append(String.format(Locale.KOREA, "%.1f ", store.rating))
                        }
                        withStyle(
                            SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = OnBackground.copy(alpha = 0.7f)),
                        ) { append("(${store.reviewCount}) >") }
                    },
                    style = suit(20, FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (store != null) {
            Text(
                buildAnnotatedString {
                    append("영업 중 · ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextLastOrder)) {
                        append("${store.lastOrder}에 라스트오더")
                    }
                },
                style = suit(16, FontWeight.Bold),
            )
            Spacer(Modifier.height(16.dp))
        }
        // 배달만 다룬다 (픽업 선택 알약은 범위 밖이라 넣지 않음)
        Column(Modifier.padding(start = 9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (store != null) FeeRow("최소주문", won(store.minOrder))
            FeeRow("배달", won(state.deliveryFee))
        }
        Spacer(Modifier.height(35.dp))
        CartPanel(state, store, onEvent, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun FeeRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = suit(16, FontWeight.Normal, OnBackground.copy(alpha = 0.8f)),
            modifier = Modifier.width(59.dp),
        )
        Spacer(Modifier.width(23.dp))
        Text(value, style = suit(16, FontWeight.Bold))
    }
}

@Composable
private fun CartPanel(
    state: OrderUiState,
    store: StoreCard?,
    onEvent: (OrderEvent) -> Unit,
    modifier: Modifier,
) {
    val count = state.cart.sumOf { it.quantity }
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLow)
            .padding(start = 21.dp, end = 22.dp, top = 25.dp, bottom = 28.dp),
    ) {
        Text(
            if (state.cart.isEmpty()) "내 장바구니" else "내 장바구니 ($count)",
            style = suit(20, FontWeight.Bold),
            modifier = Modifier.padding(start = 5.dp),
        )
        if (state.cart.isEmpty()) {
            EmptyCart(Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        Spacer(Modifier.height(27.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.cart.forEach { item ->
                CartRow(
                    item = item,
                    onMinus = { onEvent(OrderEvent.RemoveMenu(item.menu)) },
                    onPlus = { onEvent(OrderEvent.AddMenu(item.menu)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PayBar(state, store, onEvent)
    }
}

@Composable
private fun EmptyCart(modifier: Modifier) {
    Box(modifier, contentAlignment = BiasAlignment(0f, -0.35f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(ArrowRightIcon, contentDescription = null, modifier = Modifier.width(152.dp).height(158.dp))
            Text("담은 메뉴가 없어요", style = suit(24, FontWeight.Bold), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Primary)) { append("오른쪽 메뉴") }
                    append("에서 골라주세요")
                },
                style = suit(20, FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 장바구니 한 줄: 이름·옵션·가격·사진, 아래에 옵션 변경 + 수량 */
@Composable
private fun CartRow(item: CartItem, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHigh)
            .padding(start = 27.dp, end = 27.dp, top = 15.dp, bottom = 18.dp),
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(item.menu.name, style = suit(24, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("서비스 음료선택: 서비스음료 미제공", style = suit(14, FontWeight.Normal, TextDesc), maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Text(won(item.totalPrice), style = suit(20, FontWeight.Bold))
            }
            Spacer(Modifier.width(12.dp))
            MenuPhoto(item.menu.id, Modifier.padding(top = 7.dp).width(75.dp).height(74.dp))
        }
        Spacer(Modifier.height(11.dp))
        Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // 옵션 변경은 시연 범위 밖이라 눌러도 아무 일도 하지 않는다
            Box(
                Modifier.width(99.dp).height(33.dp).clip(RoundedCornerShape(10.dp)).background(Outline.copy(alpha = 0.36f)),
                contentAlignment = Alignment.Center,
            ) { Text("옵션 변경", style = suit(16, FontWeight.SemiBold), maxLines = 1) }
            SmallStepper(item.quantity, onMinus, onPlus)
        }
    }
}

/** 피그마 수량 조절 (− 1 +), 90×33 */
@Composable
private fun SmallStepper(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.width(90.dp).height(33.dp).clip(RoundedCornerShape(10.dp)).background(Outline.copy(alpha = 0.36f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
            LineIcon(plus = false, size = 8.dp, color = OnBackground)
        }
        Text("$quantity", style = suit(16, FontWeight.SemiBold), textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
        Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
            LineIcon(plus = true, size = 8.dp, color = OnBackground)
        }
    }
}

/** 결제 금액 + [주문하기]. 최소주문(배달 기준)에 못 미치면 "N원 더 담아야해요" */
@Composable
private fun PayBar(state: OrderUiState, store: StoreCard?, onEvent: (OrderEvent) -> Unit) {
    val minOrder = store?.minOrder ?: 0
    val canOrder = state.cart.isNotEmpty() && state.totalPrice >= minOrder

    Row(Modifier.fillMaxWidth().padding(start = 7.dp, end = 6.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text("결제 금액", style = suit(20, FontWeight.Normal))
            Spacer(Modifier.height(6.dp))
            Text(won(state.totalPrice), style = suit(32, FontWeight.Bold))
        }
        if (canOrder) {
            PrimaryButton(
                text = "주문하기",
                onClick = { onEvent(OrderEvent.ReviewOrder) },
                modifier = Modifier.padding(bottom = 3.dp).width(161.dp).height(46.dp),
            )
        } else {
            Box(
                Modifier
                    .padding(bottom = 3.dp)
                    .height(46.dp)
                    .widthIn(min = 161.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Background)
                    .padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${won((minOrder - state.totalPrice).coerceAtLeast(0))} 더 담아야해요",
                    style = suit(20, FontWeight.SemiBold, TextLastOrder),
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------- 오른쪽: 분류 탭 + 메뉴 ----------------

private val menuTabs = listOf("쿠폰 할인 메뉴", "인기 메뉴", "신메뉴", "3&7 콤보")

@Composable
private fun MenuColumn(state: OrderUiState, onEvent: (OrderEvent) -> Unit, modifier: Modifier) {
    // 분류 탭은 이 화면 안에서만 고른다. 어느 탭이든 같은 메뉴 목록을 보여 준다
    var selectedTab by rememberSaveable { mutableStateOf(1) }

    Column(modifier) {
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(35.dp).clip(CircleShape).border(1.dp, Outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(SearchIcon, contentDescription = "메뉴 검색", modifier = Modifier.size(16.dp)) }
                menuTabs.forEachIndexed { i, label ->
                    MenuTab(label, selected = i == selectedTab) { selectedTab = i }
                }
                Spacer(Modifier.width(48.dp))
            }
            // 오른쪽 끝 흐림 (탭이 더 있다는 표시)
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(82.dp)
                    .height(35.dp)
                    .background(Brush.horizontalGradient(listOf(Background.copy(alpha = 0f), Background))),
            )
        }
        Spacer(Modifier.height(25.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            items(state.menus, key = { it.id }) { menu ->
                MenuRow(menu) { onEvent(OrderEvent.AddMenu(menu)) }
            }
        }
    }
}

@Composable
private fun MenuTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(48.dp)
    Box(
        Modifier
            .height(35.dp)
            .widthIn(min = 116.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(Outline) else Modifier.border(1.dp, Outline, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = suit(18, FontWeight.SemiBold), maxLines = 1)
    }
}

/** 메뉴 카드: 이름·설명·가격·리뷰 수, 오른쪽 사진. 누르면 담긴다 */
@Composable
private fun MenuRow(menu: Menu, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(135.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHigh)
            .clickable(onClick = onAdd)
            .padding(start = 31.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 16.dp, bottom = 15.dp, end = 24.dp)) {
            Text(menu.name, style = suit(24, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(
                menu.description,
                style = suit(14, FontWeight.Normal, TextDesc),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 342.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)) {
                        append(won(menu.price))
                    }
                    withStyle(SpanStyle(fontSize = 11.sp)) { append("  ") }
                    withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TextReview)) {
                        append("리뷰 ")
                    }
                    withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextReview)) {
                        append("${menu.reviewCount}")
                    }
                },
                style = suit(20, FontWeight.Bold),
                maxLines = 1,
            )
        }
        MenuPhoto(menu.id, Modifier.width(101.dp).height(100.dp))
    }
}

/** 메뉴 사진. 피그마에 사진이 없는 메뉴는 둥근 빈 자리 */
@Composable
private fun MenuPhoto(menuId: String, modifier: Modifier) {
    val photo = menuPhoto(menuId)
    val shaped = modifier.clip(RoundedCornerShape(10.dp))
    if (photo != null) {
        Image(painterResource(photo), contentDescription = null, contentScale = ContentScale.Crop, modifier = shaped)
    } else {
        Box(shaped.background(ButtonSecondary))
    }
}

// ---------------- 아이콘 (피그마 벡터 경로 그대로) ----------------

private fun vectorIcon(
    width: Float,
    height: Float,
    pathData: String,
    fill: Color,
    evenOdd: Boolean = false,
    stroke: Color? = null,
): ImageVector =
    ImageVector.Builder(
        defaultWidth = width.dp,
        defaultHeight = height.dp,
        viewportWidth = width,
        viewportHeight = height,
    ).addPath(
        pathData = addPathNodes(pathData),
        pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
        fill = SolidColor(fill),
        stroke = stroke?.let { SolidColor(it) },
        strokeLineWidth = if (stroke != null) 1f else 0f,
    ).build()

private val ChevronLeftIcon: ImageVector by lazy {
    vectorIcon(
        20f, 20f,
        "M14.1919 2.05806C14.436 2.30214 14.436 2.69786 14.1919 2.94194L7.13388 10L14.1919 17.0581C14.436 17.3021 14.436 17.6979 14.1919 17.9419C13.9479 18.186 13.5521 18.186 13.3081 17.9419L5.80806 10.4419C5.56398 10.1979 5.56398 9.80214 5.80806 9.55806L13.3081 2.05806C13.5521 1.81398 13.9479 1.81398 14.1919 2.05806Z",
        fill = Color.White,
        evenOdd = true,
        stroke = Color.White,
    )
}

private val StarIcon: ImageVector by lazy {
    vectorIcon(
        15f, 15f,
        "M7.02196 0.311312C7.21751 -0.103771 7.78249 -0.10377 7.97804 0.311313L9.88269 4.35435C9.96034 4.51918 10.1104 4.63342 10.2841 4.65985L14.543 5.30819C14.9802 5.37475 15.1548 5.93767 14.8384 6.26077L11.7566 9.40783C11.631 9.53613 11.5737 9.72099 11.6033 9.90215L12.3308 14.3459C12.4055 14.8021 11.9485 15.15 11.5574 14.9346L7.74807 12.8366C7.59277 12.751 7.40723 12.751 7.25193 12.8366L3.44264 14.9346C3.05155 15.15 2.59447 14.8021 2.66916 14.3459L3.39667 9.90215C3.42633 9.72099 3.369 9.53613 3.24335 9.40783L0.161568 6.26077C-0.154827 5.93767 0.0197643 5.37475 0.457012 5.30819L4.71594 4.65985C4.88957 4.63342 5.03966 4.51918 5.11732 4.35435L7.02196 0.311312Z",
        fill = Primary,
    )
}

private val SearchIcon: ImageVector by lazy {
    vectorIcon(
        16f, 16f,
        "M11.7422 10.3439C12.5329 9.2673 13 7.9382 13 6.5C13 2.91015 10.0899 0 6.5 0C2.91015 0 0 2.91015 0 6.5C0 10.0899 2.91015 13 6.5 13C7.93858 13 9.26801 12.5327 10.3448 11.7415L10.3439 11.7422C10.3734 11.7822 10.4062 11.8204 10.4424 11.8566L14.2929 15.7071C14.6834 16.0976 15.3166 16.0976 15.7071 15.7071C16.0976 15.3166 16.0976 14.6834 15.7071 14.2929L11.8566 10.4424C11.8204 10.4062 11.7822 10.3734 11.7422 10.3439ZM12 6.5C12 9.53757 9.53757 12 6.5 12C3.46243 12 1 9.53757 1 6.5C1 3.46243 3.46243 1 6.5 1C9.53757 1 12 3.46243 12 6.5Z",
        fill = Color(0xFFD9D9D9),
        evenOdd = true,
    )
}

/** 빈 장바구니의 노란 오른쪽 화살표 (152×158) */
private val ArrowRightIcon: ImageVector by lazy {
    vectorIcon(
        152f, 158f,
        "M38 79C38 76.2731 40.1266 74.0625 42.75 74.0625H97.7825L77.3912 52.8663C75.5362 50.9381 75.5362 47.8119 77.3912 45.8837C79.2462 43.9554 82.2538 43.9554 84.1088 45.8837L112.609 75.5087C114.464 77.4369 114.464 80.5631 112.609 82.4913L84.1088 112.116C82.2538 114.045 79.2462 114.045 77.3912 112.116C75.5362 110.188 75.5362 107.062 77.3912 105.134L97.7825 83.9375H42.75C40.1266 83.9375 38 81.7269 38 79Z",
        fill = Primary,
        evenOdd = true,
    )
}

// ---------------- 미리보기 ----------------

private object StoreMenuSamples {
    private const val DAOM = "다옴 양재온누리점"
    private const val BHC = "BHC치킨 우면점"

    val daomStore = StoreCard(
        id = "s2", name = DAOM, category = "한식", distanceM = 450, etaMin = 25,
        deliveryFee = 2_300, minOrder = 10_000, rating = 4.6, reviewCount = 89,
        latitude = 37.47, longitude = 127.04, lastOrder = "22:20", beforeChargeEnds = true,
    )
    val bhcStore = StoreCard(
        id = "s2b", name = BHC, category = "치킨", distanceM = 600, etaMin = 30,
        deliveryFee = 3_000, minOrder = 15_000, rating = 4.8, reviewCount = 121,
        latitude = 37.46, longitude = 127.03, lastOrder = "22:20", beforeChargeEnds = true,
    )

    val daomMenus = listOf(
        Menu("s2-1", DAOM, "다옴김밥", 4_000, "신선한 채소와 달걀, 햄을 듬뿍 넣어 고소하고 담백한 맛을 살린 기본 김밥", 14),
        Menu("s2-2", DAOM, "참치김밥", 5_500, "고소한 참치마요와 아삭한 채소가 어우러져 부드럽고 풍성한 맛을 즐길 수 있는 김밥", 31),
        Menu("s2-3", DAOM, "고추참치김밥", 6_000, "매콤한 고추참치에 신선한 채소를 더해 감칠맛과 매운맛이 조화를 이루는 김밥", 11),
        Menu("s2-4", DAOM, "묵은지참치김밥", 6_000, "깊게 숙성된 묵은지의 새콤하고 아삭한 식감이 입맛을 돋우는 별미 김밥", 14),
        Menu("s2-5", DAOM, "날치알고추냉이김밥", 5_000, "톡톡 터지는 날치알과 알싸한 고추냉이가 만나 독특하고 산뜻한 풍미를 선사하는 김밥", 8),
    )
    val bhcMenus = listOf(
        Menu("s2b-1", BHC, "뿌링클 콤보", 25_000, "뿌링뿌링 세상에 없던 마법의 맛 뿌링클, 퍽퍽한 닭가슴살 없이 윙, 봉, 닭다리만 담은 콤보", 14),
        Menu("s2b-2", BHC, "뿌링클", 21_000, "치즈와 요거트 시즈닝을 듬뿍 뿌린 BHC 대표 메뉴", 52),
        Menu("s2b-3", BHC, "맛초킹", 22_000, "달콤짭조름한 간장 소스에 청양고추를 더한 치킨", 27),
        Menu("s2b-4", BHC, "후라이드", 19_000, "바삭한 튀김옷의 기본 후라이드 치킨", 33),
    )

    private val daomBase = OrderUiState(
        isParked = true,
        stores = listOf(daomStore, bhcStore),
        selectedStoreId = daomStore.id,
        menus = daomMenus,
        deliveryFee = daomStore.deliveryFee,
    )

    val empty = daomBase
    val oneItem = daomBase.copy(cart = listOf(CartItem(daomMenus[2], 1)))
    val twoItems = daomBase.copy(cart = listOf(CartItem(daomMenus[2], 1), CartItem(daomMenus[0], 1)))
    val bhc = OrderUiState(
        isParked = true,
        stores = listOf(daomStore, bhcStore),
        selectedStoreId = bhcStore.id,
        menus = bhcMenus,
        deliveryFee = bhcStore.deliveryFee,
        cart = listOf(CartItem(bhcMenus[0], 1)),
    )
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun StoreMenuEmptyPreview() {
    CardeliTheme { StoreMenuScreen(StoreMenuSamples.empty) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun StoreMenuBelowMinPreview() {
    CardeliTheme { StoreMenuScreen(StoreMenuSamples.oneItem) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun StoreMenuTwoItemsPreview() {
    CardeliTheme { StoreMenuScreen(StoreMenuSamples.twoItems) {} }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun StoreMenuBhcPreview() {
    CardeliTheme { StoreMenuScreen(StoreMenuSamples.bhc) {} }
}
