package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.R
import ai.cardeli.starter.contract.ChargingInfo
import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.StoreCard
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.ChevronIcon
import ai.cardeli.starter.ui.components.Logo
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.components.meters
import ai.cardeli.starter.ui.components.won
import ai.cardeli.starter.ui.theme.Background
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.OnPrimary
import ai.cardeli.starter.ui.theme.Outline
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.Suit
import ai.cardeli.starter.ui.theme.SurfaceHigh
import ai.cardeli.starter.ui.theme.SurfaceSunken
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// 스플래시 · 홈 · 가게 목록. 피그마 "Pleos Connect | 밥먹GO" 8:21317, 1:9694, 8:10411, 8:10936

// 피그마에만 쓰인 색 (테마에 없는 값)
private val HeroSub = Color(0xFFCCCCCC)
private val GleoButtonBg = Color(0xFF191A1D)
private val ChipText = Color(0xFFD9D9D9)
private val CardMeta = Color(0xFFDFDFDF)
private val PopupSub = Color(0xFFCECECE)
private val PopupIconBg = Color(0xFF2A2C32)
private val PopupChipBg = Color(0x5C3F424B)   // rgba(63,66,75,0.36)

private fun suit(size: Int, weight: FontWeight, lineHeight: Int? = null) = TextStyle(
    fontFamily = Suit,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
)

// ======================================================================
// 스플래시
// ======================================================================

/** 앱을 켤 때 잠깐 보이는 로고 화면 */
@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column {
            Text("배고프니까", style = suit(36, FontWeight.Normal, lineHeight = 52), color = OnBackground)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(size = 96)
                SpeedLines(Modifier.offset(x = (-7).dp, y = (-8).dp))
            }
        }
    }
}

/** 로고 GO 뒤의 노란 속도선 세 줄 */
@Composable
private fun SpeedLines(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 60.dp, height = 41.dp)) {
        val stroke = 7.dp.toPx()
        val len = 35.17.dp.toPx()
        val half = stroke / 2
        listOf(0f to 0f, 17.58f to 16.69f, 11.92f to 33.38f).forEach { (x, y) ->
            val sx = half + x.dp.toPx()
            val sy = half + y.dp.toPx()
            drawLine(Primary, Offset(sx, sy), Offset(sx + len, sy), stroke, StrokeCap.Round)
        }
    }
}

// ======================================================================
// 홈
// ======================================================================

/** 정차 중 첫 화면. 음식 주문하기 → 가게 목록, Gleo로 주문하기 → 음성 주문 */
@Composable
fun HomeScreen(state: OrderUiState, onOrderFood: () -> Unit, onGleo: () -> Unit) {
    AppScaffold(active = NavTab.Home, location = state.placeLabel) {
        HomeHero(Modifier.fillMaxWidth().weight(1f), onOrderFood, onGleo)
        Spacer(Modifier.height(29.dp))
        HomeMenuRow(onCategory = onOrderFood)
    }
}

@Composable
private fun HomeHero(modifier: Modifier, onOrderFood: () -> Unit, onGleo: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(17.dp))) {
        // 오른쪽 지도 ("현재 위치" 표시가 그림에 들어 있다)
        Image(
            painterResource(R.drawable.home_map),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 왼쪽은 배경색, 오른쪽으로 갈수록 지도가 드러난다
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.63f)
                .background(
                    Brush.horizontalGradient(
                        0f to Background,
                        0.774f to Background,
                        1f to Background.copy(alpha = 0f),
                    ),
                ),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 68.dp, bottom = 24.dp)) {
            Text(
                buildAnnotatedString {
                    append("배달 주소가\n꼭 ")
                    withStyle(SpanStyle(color = Primary)) { append("집") }
                    append("일 필요는 없잖아요?")
                },
                style = suit(55, FontWeight.ExtraBold, lineHeight = 68),
                color = OnBackground,
            )
            Spacer(Modifier.height(13.dp))
            Text("오늘은 내 차에서 만나요.", style = suit(24, FontWeight.Normal, lineHeight = 34), color = HeroSub)
            Spacer(Modifier.height(46.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                OrderFoodButton(onOrderFood)
                GleoButton(onGleo)
            }
        }
    }
}

@Composable
private fun OrderFoodButton(onClick: () -> Unit) {
    Row(
        Modifier
            .height(55.dp)
            .width(279.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Primary)
            .clickable(onClick = onClick)
            .padding(start = 21.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("음식 주문하기", style = suit(24, FontWeight.ExtraBold), color = Background, maxLines = 1)
        Box(
            Modifier.size(43.dp).clip(CircleShape).background(Background),
            contentAlignment = Alignment.Center,
        ) { ChevronIcon(pointsLeft = false, size = 16.dp, color = OnBackground) }
    }
}

@Composable
private fun GleoButton(onClick: () -> Unit) {
    Row(
        Modifier
            .height(55.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(GleoButtonBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 21.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.home_gleo), contentDescription = null, modifier = Modifier.size(30.dp, 31.5.dp))
        Text("Gleo로 주문하기", style = suit(20, FontWeight.Bold), color = OnBackground, maxLines = 1)
    }
}

private class HomeCategory(
    val label: String,
    @param:DrawableRes val icon: Int,
    val iconSize: Dp,
    val iconX: Dp = 0.dp,
    val iconTop: Dp = 0.dp,     // 카드 윗변보다 53dp 위에서부터 잰 값
    val badge: String? = null,
    val badgeTop: Dp = 0.dp,
)

private val homeCategories = listOf(
    HomeCategory("카페&디저트", R.drawable.home_cat_coffee, 119.5.dp, badge = "위치 근처", badgeTop = 109.4.dp),
    HomeCategory("패스트푸드", R.drawable.home_cat_burger, 128.dp),
    HomeCategory("치킨", R.drawable.home_cat_chicken, 133.7.dp),   // 피그마의 "바로 픽업 가능" 배지는 픽업을 범위에서 빼서 넣지 않음
    HomeCategory("피자", R.drawable.home_cat_pizza, 109.5.dp, iconX = 3.dp, iconTop = 7.3.dp),
    HomeCategory("일식", R.drawable.home_cat_sushi, 133.7.dp, iconX = (-4).dp),
    HomeCategory("한식", R.drawable.home_cat_korean, 127.9.dp, iconTop = 1.6.dp),
)

private val CategoryOverhang = 53.dp   // 음식 그림이 카드 위로 튀어나온 높이
private val CategoryCardHeight = 117.dp

/** 아래 줄: 메뉴 ✦ 세상은 넓고 맛집은 많다 + 분류 카드 */
@Composable
private fun HomeMenuRow(onCategory: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(CategoryOverhang + CategoryCardHeight + 19.dp)) {
        Column(Modifier.width(175.dp).padding(start = 15.dp, top = CategoryOverhang + 11.dp)) {
            Row {
                Text("메뉴", style = suit(36, FontWeight.SemiBold), color = OnBackground)
                Image(
                    painterResource(R.drawable.home_stars),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 2.dp).offset(y = (-7).dp).size(22.dp, 21.dp),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text("세상은 넓고 맛집은 많다", style = suit(16, FontWeight.SemiBold), color = OnBackground, maxLines = 1)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(19.dp)) {
            homeCategories.forEach { CategoryCard(it, Modifier.weight(1f), onCategory) }
        }
        Box(
            Modifier
                .padding(top = CategoryOverhang)
                .height(CategoryCardHeight)
                .width(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCategory),
            contentAlignment = Alignment.Center,
        ) { ChevronIcon(pointsLeft = false, size = 26.dp, color = OnBackground) }
    }
}

@Composable
private fun CategoryCard(category: HomeCategory, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight()) {
        Box(
            Modifier
                .padding(top = CategoryOverhang)
                .fillMaxWidth()
                .height(CategoryCardHeight)
                .clip(RoundedCornerShape(23.dp))
                .background(SurfaceHigh)
                .clickable(onClick = onClick),
        ) {
            Text(
                category.label,
                style = suit(18, FontWeight.SemiBold),
                color = OnBackground,
                maxLines = 1,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 74.dp),
            )
        }
        Image(
            painterResource(category.icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = category.iconX, y = category.iconTop)
                .size(category.iconSize),
        )
        if (category.badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = CategoryOverhang + category.badgeTop)
                    .height(26.dp)
                    .clip(RoundedCornerShape(49.dp))
                    .background(Primary)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(category.badge, style = suit(14, FontWeight.Bold), color = OnPrimary, maxLines = 1)
            }
        }
    }
}

// ======================================================================
// 가게 목록
// ======================================================================

private val storeFilters = listOf("전체", "카페·디저트", "한식", "중식", "일식", "패스트푸드")

/**
 * 정차 중 가게 목록. 칩으로 분류를 거르고, 카드나 지도 핀을 누르면 그 가게를 강조하고
 * 지도 아래에 요약 카드를 띄운다. 요약 카드의 [주문하기] → OrderEvent.SelectStore.
 */
@Composable
fun StoreListScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    var filter by remember { mutableStateOf(storeFilters.first()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val shown = if (filter == storeFilters.first()) state.stores else state.stores.filter { it.category == filter }
    val selected = shown.firstOrNull { it.id == selectedId }
    val charging = state.charging

    AppScaffold(active = NavTab.Home, location = state.placeLabel) {
        ScreenTitle("주문하기", accentBar = true)
        if (charging != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                chargingLine(charging),
                style = suit(16, FontWeight.SemiBold),
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            storeFilters.forEach { label ->
                FilterChip(label, selected = label == filter) {
                    filter = label
                    if (label != storeFilters.first() && state.stores.firstOrNull { it.id == selectedId }?.category != label) {
                        selectedId = null
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(623f).fillMaxHeight().bleed(bottom = 30.dp),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(shown, key = { it.id }) { store ->
                    StoreGridCard(
                        store = store,
                        highlighted = store.id == selected?.id,
                        showChargeBadge = charging != null && store.beforeChargeEnds,
                    ) { selectedId = store.id }
                }
            }
            Spacer(Modifier.width(38.dp))
            StoreMap(
                stores = shown,
                carLatitude = state.carLatitude,
                carLongitude = state.carLongitude,
                selected = selected,
                onPin = { selectedId = it.id },
                onOrder = { onEvent(OrderEvent.SelectStore(it.id)) },
                modifier = Modifier
                    .weight(611f)
                    .fillMaxHeight()
                    .padding(top = 9.dp)
                    .bleed(end = 44.dp, bottom = 30.dp),
            )
        }
    }
}

private fun chargingLine(charging: ChargingInfo): String {
    val remain = charging.remainMin?.let { " · 약 ${it}분 남음" } ?: ""
    return "충전 중 ${charging.socPercent}%$remain · 충전이 끝나기 전에 받을 수 있는 가게부터 보여 드려요"
}

/** AppScaffold 여백을 무시하고 화면 끝까지 늘린다 (지도·목록이 오른쪽·아래 끝에 붙는 디자인) */
private fun Modifier.bleed(end: Dp = 0.dp, bottom: Dp = 0.dp) = layout { measurable, constraints ->
    val e = end.roundToPx()
    val b = bottom.roundToPx()
    val w = constraints.maxWidth
    val h = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(w + e, h + b))
    layout(w, h) { placeable.place(0, 0) }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(48.dp)
    Box(
        Modifier
            .height(38.dp)
            .widthIn(min = 93.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(Primary) else Modifier.border(1.dp, Outline, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = suit(20, if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) Background else ChipText,
            maxLines = 1,
        )
    }
}

// 가게 id → 피그마 사진
@DrawableRes
private fun storePhoto(id: String): Int? = when (id) {
    "s2-b" -> R.drawable.list_store_bhc
    "s2-c" -> R.drawable.list_store_gamjatang
    "s2-a" -> R.drawable.list_store_daom
    "s2-d" -> R.drawable.list_store_palwol
    "s2-e" -> R.drawable.list_store_sangjiwon
    "s2-f" -> R.drawable.list_store_lilyblanc
    else -> null
}

// 지도 요약 카드의 정사각 사진 (피그마에 따로 있는 것만, 나머지는 목록 사진)
@DrawableRes
private fun storePopupPhoto(id: String): Int? = when (id) {
    "s2-a" -> R.drawable.list_popup_daom
    else -> storePhoto(id)
}

@Composable
private fun StorePhoto(id: String, modifier: Modifier, popup: Boolean = false) {
    val res = if (popup) storePopupPhoto(id) else storePhoto(id)
    if (res != null) {
        Image(painterResource(res), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(SurfaceSunken))
    }
}

private fun rating(value: Double) = String.format(Locale.KOREA, "%.1f", value)

/** ★ 4.8 (121) */
@Composable
private fun RatingLabel(store: StoreCard) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.list_star), contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            buildAnnotatedString {
                append("${rating(store.rating)} ")
                withStyle(
                    SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = OnBackground.copy(alpha = 0.7f)),
                ) { append("(${store.reviewCount})") }
            },
            style = suit(20, FontWeight.SemiBold),
            color = OnBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun StoreGridCard(store: StoreCard, highlighted: Boolean, showChargeBadge: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(233.dp)
            .clip(shape)
            .background(SurfaceHigh)
            .border(2.dp, if (highlighted) Primary else Color.Transparent, shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(138.dp)) {
            StorePhoto(store.id, Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(SurfaceHigh.copy(alpha = 0f), SurfaceHigh))),
            )
            if (showChargeBadge) {
                Box(
                    Modifier
                        .padding(10.dp)
                        .clip(RoundedCornerShape(49.dp))
                        .background(Primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) { Text("충전 중 수령 가능", style = suit(14, FontWeight.Bold), color = OnPrimary, maxLines = 1) }
            }
        }
        Row(
            Modifier.padding(start = 11.dp, end = 10.dp, top = 16.dp).height(31.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                store.name,
                style = suit(20, FontWeight.Bold),
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(4.dp))
            RatingLabel(store)
        }
        Row(Modifier.padding(start = 17.dp, top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("최소주문 ${won(store.minOrder)}", style = suit(14, FontWeight.Normal), color = CardMeta, maxLines = 1)
            Spacer(Modifier.width(10.dp))
            Image(painterResource(R.drawable.list_geo), contentDescription = null, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(5.dp))
            Text(meters(store.distanceM), style = suit(14, FontWeight.Normal), color = CardMeta, maxLines = 1)
        }
    }
}

// ---------------- 지도 ----------------

// 지도 그림(list_map) 안에서 차 표시의 위치 (그림 크기 대비 비율)
private const val MAP_CAR_FX = 0.485f
private const val MAP_CAR_FY = 0.7226f
// 패널 안에서 차를 놓을 높이 (위아래 가게가 고르게 보이도록 그림을 키워 맞춘다)
private const val MAP_CAR_TARGET_Y = 0.55f
// 차에서 이 거리(m)까지는 패널 안에 들어오게 축척을 잡는다
private const val MAP_FIT_METERS = 1_700f
private const val METERS_PER_DEG_LAT = 111_000.0
private const val METERS_PER_DEG_LNG = 88_400.0

// 피그마 핀 모양 (27.3 x 36.4)
private val pinPath = PathParser().parsePathString(
    "M13.6523 36.4062C13.6523 36.4062 27.3047 23.4677 27.3047 13.6523C27.3047 6.11235 21.1923 0 13.6523 0" +
        "C6.11235 0 0 6.11235 0 13.6523C0 23.4677 13.6523 36.4062 13.6523 36.4062Z" +
        "M13.6523 20.4785C9.88234 20.4785 6.82616 17.4223 6.82616 13.6523C6.82616 9.88234 9.88234 6.82616 13.6523 6.82616" +
        "C17.4223 6.82616 20.4785 9.88234 20.4785 13.6523C20.4785 17.4223 17.4223 20.4785 13.6523 20.4785Z",
).toPath()
private const val PIN_W = 27.3047f
private const val PIN_H = 36.4062f

@Composable
private fun StoreMap(
    stores: List<StoreCard>,
    carLatitude: Double?,
    carLongitude: Double?,
    selected: StoreCard?,
    onPin: (StoreCard) -> Unit,
    onOrder: (StoreCard) -> Unit,
    modifier: Modifier,
) {
    val mapPainter = painterResource(R.drawable.list_map)
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(topStart = 20.dp))
            .background(SurfaceSunken)
            .innerShade(),
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val iw = mapPainter.intrinsicSize.width
        val ih = mapPainter.intrinsicSize.height
        // 패널을 덮으면서 차를 MAP_CAR_TARGET_Y 높이에 둘 수 있는 크기로 그림을 키운다
        val scale = max(max(w / iw, h / ih), h * (1 - MAP_CAR_TARGET_Y) / ((1 - MAP_CAR_FY) * ih))
        val imgW = iw * scale
        val imgH = ih * scale
        val left = (w * 0.5f - MAP_CAR_FX * imgW).coerceIn(w - imgW, 0f)
        val top = (h * MAP_CAR_TARGET_Y - MAP_CAR_FY * imgH).coerceIn(h - imgH, 0f)
        val carX = left + MAP_CAR_FX * imgW
        val carY = top + MAP_CAR_FY * imgH
        val pxPerMeter = min(min(carX, w - carX), min(carY, h - carY)) * 0.92f / MAP_FIT_METERS

        Canvas(Modifier.fillMaxSize()) {
            translate(left, top) { with(mapPainter) { draw(Size(imgW, imgH)) } }
        }

        if (carLatitude != null && carLongitude != null) {
            Box(Modifier.fillMaxSize().clipToBounds()) {
                val ordered = stores.sortedBy { it.id == selected?.id }   // 고른 핀을 맨 위에
                ordered.forEach { store ->
                    val dx = (store.longitude - carLongitude) * METERS_PER_DEG_LNG
                    val dy = (store.latitude - carLatitude) * METERS_PER_DEG_LAT
                    val x = carX + (dx * pxPerMeter).toFloat()
                    val y = carY - (dy * pxPerMeter).toFloat()
                    val isSelected = store.id == selected?.id
                    MapPin(
                        label = if (isSelected) store.name else store.category.substringBefore("·"),
                        big = isSelected,
                        modifier = Modifier.offset {
                            with(density) {
                                IntOffset(
                                    (x - PinBoxWidth.toPx() / 2).roundToInt(),
                                    (y - PinBoxHeight.toPx()).roundToInt(),
                                )
                            }
                        },
                        onClick = { onPin(store) },
                    )
                }
            }
        }

        if (selected != null) {
            StorePopup(
                store = selected,
                onOrder = { onOrder(selected) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
            )
        }
    }
}

private val PinBoxWidth = 220.dp
private val PinBoxHeight = 66.dp

/** 노란 핀 + 위에 글자. 상자 아래 가운데가 가게 위치 */
@Composable
private fun MapPin(label: String, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(PinBoxWidth, PinBoxHeight), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = suit(if (big) 16 else 15, FontWeight.Bold),
                color = OnBackground,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(if (big) 3.dp else 2.dp))
            val pinW = if (big) 30.66.dp else 27.3.dp
            val pinH = if (big) 40.87.dp else 36.4.dp
            Canvas(Modifier.size(pinW, pinH)) {
                scale(size.width / PIN_W, size.height / PIN_H, pivot = Offset.Zero) {
                    drawPath(pinPath, Primary)
                }
            }
        }
    }
}

/** 지도 가장자리를 배경색으로 살짝 어둡게 (피그마 inset shadow) */
private fun Modifier.innerShade() = drawWithContent {
    drawContent()
    val d = 22.dp.toPx()
    val solid = Background
    val clear = Background.copy(alpha = 0f)
    drawRect(Brush.verticalGradient(listOf(solid, clear), startY = 0f, endY = d + 4.dp.toPx()), size = Size(size.width, d + 4.dp.toPx()))
    drawRect(Brush.horizontalGradient(listOf(solid, clear), startX = 0f, endX = d), size = Size(d, size.height))
    drawRect(
        Brush.horizontalGradient(listOf(clear, solid), startX = size.width - d, endX = size.width),
        topLeft = Offset(size.width - d, 0f),
        size = Size(d, size.height),
    )
    drawRect(
        Brush.verticalGradient(listOf(clear, solid), startY = size.height - d, endY = size.height),
        topLeft = Offset(0f, size.height - d),
        size = Size(size.width, d),
    )
}

/** 지도 아래 가게 요약 카드 */
@Composable
private fun StorePopup(store: StoreCard, onOrder: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(156.dp)
            .shadow(14.dp, shape)
            .clip(shape)
            .background(SurfaceHigh),
    ) {
        // 끌어내리기 손잡이
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(42.dp, 2.dp)
                .clip(CircleShape)
                .background(OnBackground.copy(alpha = 0.7f)),
        )
        Row(Modifier.fillMaxSize().padding(start = 15.dp, end = 17.dp, top = 17.dp, bottom = 17.dp)) {
            StorePhoto(
                store.id,
                Modifier.padding(top = 6.dp).size(96.dp).clip(RoundedCornerShape(15.dp)),
                popup = true,
            )
            Spacer(Modifier.width(19.dp))
            Column(Modifier.weight(1f).padding(top = 10.dp)) {
                Text(
                    store.name,
                    style = suit(24, FontWeight.Bold),
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                RatingLabel(store)
                Spacer(Modifier.height(10.dp))
                Text(
                    buildAnnotatedString {
                        append("영업 중 · ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = PopupSub)) {
                            append("${store.lastOrder}에 라스트오더")
                        }
                    },
                    style = suit(16, FontWeight.Bold),
                    color = OnBackground,
                    maxLines = 1,
                )
            }
            Column(
                Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier
                        .widthIn(min = 87.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(PopupChipBg)
                        .padding(horizontal = 17.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(R.drawable.list_cursor), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(meters(store.distanceM), style = suit(16, FontWeight.SemiBold), color = OnBackground, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(PopupIconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painterResource(R.drawable.list_bookmark), contentDescription = null, modifier = Modifier.size(14.dp, 18.dp))
                    }
                    Box(
                        Modifier
                            .height(40.dp)
                            .width(186.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(Primary)
                            .clickable(onClick = onOrder),
                        contentAlignment = Alignment.Center,
                    ) { Text("주문하기", style = suit(20, FontWeight.Bold), color = Background, maxLines = 1) }
                }
            }
        }
    }
}

// ======================================================================
// 미리보기 (가짜 데이터, 앱 실행에는 쓰지 않는다)
// ======================================================================

private object HomePreviewData {
    private fun store(
        id: String, name: String, category: String, dist: Int, min: Int, rating: Double, reviews: Int,
        lat: Double, lng: Double, ok: Boolean = true,
    ) = StoreCard(
        id, name, category, distanceM = dist, etaMin = 25, deliveryFee = 2_500, minOrder = min,
        rating = rating, reviewCount = reviews, latitude = lat, longitude = lng, lastOrder = "22:20", beforeChargeEnds = ok,
    )

    val state = OrderUiState(
        placeLabel = "서울 서초구 헌릉로",
        carLatitude = 37.4625,
        carLongitude = 127.0380,
        stores = listOf(
            store("s2-b", "BHC치킨 우면점", "패스트푸드", 1_300, 18_000, 4.8, 121, 37.4710, 127.0497),
            store("s2-c", "대한감자탕뼈해장국", "한식", 1_500, 10_000, 4.3, 132, 37.4530, 127.0504, ok = false),
            store("s2-a", "다옴 양재온누리점", "한식", 1_600, 10_000, 4.6, 89, 37.4750, 127.0290),
            store("s2-d", "팔월오일 커피", "카페·디저트", 1_600, 12_000, 4.3, 153, 37.4500, 127.0310),
            store("s2-e", "상지원", "중식", 1_300, 18_000, 4.8, 64, 37.4747, 127.0390),
            store("s2-f", "카페 릴리블랑", "카페·디저트", 1_300, 18_000, 4.5, 173, 37.4630, 127.0243),
        ),
    )
    val charging = state.copy(charging = ChargingInfo(socPercent = 62, remainMin = 25))
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun SplashScreenPreview() {
    CardeliTheme { SplashScreen() }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun HomeScreenPreview() {
    CardeliTheme { HomeScreen(HomePreviewData.state, onOrderFood = {}, onGleo = {}) }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun StoreListScreenPreview() {
    CardeliTheme { StoreListScreen(HomePreviewData.charging) {} }
}
