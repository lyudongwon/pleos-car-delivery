package ai.cardeli.starter.ui.components

import ai.cardeli.starter.contract.Menu
import ai.cardeli.starter.ui.theme.Background
import ai.cardeli.starter.ui.theme.ButtonSecondary
import ai.cardeli.starter.ui.theme.Inactive
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.OnPrimary
import ai.cardeli.starter.ui.theme.Outline
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.PrimaryLogo
import ai.cardeli.starter.ui.theme.SurfaceHigh
import ai.cardeli.starter.ui.theme.SurfaceLow
import ai.cardeli.starter.ui.theme.SurfaceSunken
import ai.cardeli.starter.ui.theme.Suit
import ai.cardeli.starter.ui.theme.TextBody
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// 여러 화면에서 같이 쓰는 컴포넌트. 피그마 "Pleos Connect | 밥먹GO" 기준.
// 규칙은 ui/README.md 참고. 컴포넌트도 값과 콜백만 받는다.

/** 4,000원 */
fun won(amount: Int): String = String.format(Locale.KOREA, "%,d원", amount)

/** 350m, 1.2km */
fun meters(m: Int): String =
    if (m >= 1000) String.format(Locale.KOREA, "%.1fkm", m / 1000f) else "${m}m"

// ---------------- 화면 틀 ----------------

enum class NavTab(val label: String) { Home("홈"), Orders("주문내역"), Favorites("즐겨찾기"), My("마이페이지") }

/** 모든 정차 화면의 바깥 틀: 배경, 상단바, 여백 */
@Composable
fun AppScaffold(
    active: NavTab?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = Background, contentColor = OnBackground) {
        Column(Modifier.fillMaxSize().padding(horizontal = 44.dp, vertical = 30.dp)) {
            AppTopBar(active)
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
fun Logo(size: Int = 32) {
    Text(
        buildAnnotatedString {
            append("밥먹")
            withStyle(SpanStyle(color = PrimaryLogo)) { append("GO") }
        },
        style = TextStyle(fontFamily = Suit, fontSize = size.sp, fontWeight = FontWeight.ExtraBold),
        color = OnBackground,
    )
}

/** 로고 + 오른쪽 메뉴 알약. 지금 화면에 해당하는 칸만 노랗게 칠한다 */
@Composable
fun AppTopBar(active: NavTab?) {
    Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo()
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            NavTab.values().forEach { NavPill(it.label, it == active) }
        }
    }
}

@Composable
private fun NavPill(label: String, selected: Boolean) {
    Box(
        Modifier
            .height(44.dp)
            .widthIn(min = 132.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (selected) Primary else Color.Transparent)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (selected) OnPrimary else OnBackground)
    }
}

/** 화면 제목. 뒤로 가기 화살표 또는 노란 세로 막대를 앞에 붙인다 */
@Composable
fun ScreenTitle(
    text: String,
    onBack: (() -> Unit)? = null,
    accentBar: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { ChevronIcon(pointsLeft = true, size = 22.dp, color = OnBackground) }
            Spacer(Modifier.width(4.dp))
        }
        if (accentBar) {
            Box(Modifier.width(5.dp).height(42.dp).background(Primary))
            Spacer(Modifier.width(14.dp))
        }
        Text(text, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(14.dp))
        trailing()
    }
}

// ---------------- 면·버튼 ----------------

/** 둥근 카드 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = SurfaceLow,
    padding: Dp = 26.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(color).padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(if (enabled) Primary else ButtonSecondary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) OnPrimary else TextSub,
            maxLines = 1,
        )
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(58.dp))
            .background(ButtonSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = OnBackground, maxLines = 1)
    }
}

/** 테두리 알약 (메뉴 분류, 상태 표시) */
@Composable
fun Chip(text: String, selected: Boolean = false) {
    val shape = RoundedCornerShape(48.dp)
    Box(
        Modifier
            .height(40.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(Outline) else Modifier.border(1.dp, Outline, shape))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = OnBackground)
    }
}

@Composable
fun HorizontalLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Outline))
}

// ---------------- 메뉴·장바구니 ----------------

/** 메뉴 한 줄. 카드를 누르면 담기, 담긴 뒤에는 수량 조절 */
@Composable
fun MenuCard(menu: Menu, quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceHigh)
            .clickable(onClick = onAdd)
            .padding(horizontal = 30.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(menu.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Text(won(menu.price), style = MaterialTheme.typography.titleMedium, color = Primary)
        }
        if (quantity > 0) {
            QuantityStepper(quantity, onMinus = onRemove, onPlus = onAdd)
        } else {
            Box(
                Modifier.height(48.dp).clip(RoundedCornerShape(40.dp)).border(1.dp, Outline, RoundedCornerShape(40.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) { Text("담기", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/** 장바구니 안 항목 */
@Composable
fun CartItemCard(name: String, price: Int, quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceHigh)
            .padding(horizontal = 26.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(won(price), style = MaterialTheme.typography.titleMedium)
        }
        QuantityStepper(quantity, onMinus, onPlus)
    }
}

/** − 1 + */
@Composable
fun QuantityStepper(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.height(48.dp).clip(MaterialTheme.shapes.small).background(Outline.copy(alpha = 0.36f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
            LineIcon(plus = false, size = 14.dp, color = OnBackground)
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp),
        )
        Box(Modifier.size(48.dp).clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
            LineIcon(plus = true, size = 14.dp, color = OnBackground)
        }
    }
}

// ---------------- 주문 정보 ----------------

/** 주문번호  H4ANDAE0013 처럼 왼쪽 이름, 오른쪽 값 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextBody, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TextBody)
    }
}

/** 메뉴 이름 · 수량 · 가격 */
@Composable
fun LineItemRow(name: String, quantity: Int, price: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyLarge, color = TextBody, modifier = Modifier.weight(1f))
        Text("${quantity}개", style = MaterialTheme.typography.bodyLarge, color = TextBody, modifier = Modifier.width(80.dp))
        Text(
            won(price),
            style = MaterialTheme.typography.bodyLarge,
            color = TextBody,
            textAlign = TextAlign.End,
            modifier = Modifier.width(140.dp),
        )
    }
}

/** 합계 줄: 왼쪽 "합계", 오른쪽 노란 금액 */
@Composable
fun TotalRow(label: String, amount: Int, note: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = TextBody, modifier = Modifier.weight(1f))
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = TextSub)
            Spacer(Modifier.width(10.dp))
        }
        Text(won(amount), style = MaterialTheme.typography.titleLarge, color = Primary)
    }
}

// ---------------- 주문서 선택지 ----------------

/** 고르는 카드. 고른 것은 노란 테두리 */
@Composable
fun OptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier
            .clip(shape)
            .background(SurfaceSunken)
            .border(2.dp, if (selected) Primary else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = if (selected) OnBackground else TextBody)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSub)
            }
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(30.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                CheckIcon(size = 18.dp, color = OnPrimary)
            }
        }
    }
}

/** 동그라미 + 글자 한 줄 (결제수단) */
@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .border(2.dp, if (selected) Primary else Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(14.dp).clip(CircleShape).background(Primary))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (selected) OnBackground else TextBody)
    }
}

/** 요청사항 입력칸 */
@Composable
fun NoteField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Outline, shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal), color = TextHint)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnBackground),
            cursorBrush = SolidColor(Primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------- 배달 진행 ----------------

private val stepLabels = listOf("주문 접수", "조리 중", "배달 시작", "수령 완료")

/** 4단계 진행 표시. doneCount개까지 노란 체크 */
@Composable
fun StepProgress(doneCount: Int, modifier: Modifier = Modifier) {
    val stepWidth = 96.dp
    val dot = 26.dp
    Box(modifier.fillMaxWidth()) {
        // 점 사이 연결선: 첫 점 중심 ~ 마지막 점 중심을 3등분
        Row(Modifier.fillMaxWidth().padding(start = stepWidth / 2, end = stepWidth / 2, top = dot / 2 - 1.5.dp)) {
            for (i in 0 until stepLabels.size - 1) {
                Box(
                    Modifier.weight(1f).height(3.dp)
                        .background(if (i + 1 < doneCount) Primary else Inactive),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stepLabels.forEachIndexed { i, label ->
                val done = i < doneCount
                Column(Modifier.width(stepWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(dot).clip(CircleShape).background(if (done) Primary else Inactive),
                        contentAlignment = Alignment.Center,
                    ) { if (done) CheckIcon(size = 16.dp, color = OnPrimary) }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (done) Primary else Inactive,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ---------------- 그림 ----------------

/**
 * res/drawable에 [name] 이미지가 있으면 보여주고, 없으면 아무것도 그리지 않는다.
 * 피그마 일러스트(배달 기사 illust_rider, 차에서 받기 illust_car)를 png로 내보내 넣으면 바로 나온다.
 */
@Composable
fun OptionalIllustration(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    if (id != 0) {
        Image(painterResource(id), contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    }
}

@Composable
fun CheckIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.14f
        drawLine(color, Offset(w * 0.18f, h * 0.52f), Offset(w * 0.42f, h * 0.74f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.82f, h * 0.30f), stroke, StrokeCap.Round)
    }
}

@Composable
fun ChevronIcon(pointsLeft: Boolean, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f
        val tipX = if (pointsLeft) w * 0.30f else w * 0.70f
        val tailX = if (pointsLeft) w * 0.68f else w * 0.32f
        drawLine(color, Offset(tailX, h * 0.12f), Offset(tipX, h * 0.5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(tipX, h * 0.5f), Offset(tailX, h * 0.88f), stroke, StrokeCap.Round)
    }
}

/** + 또는 − */
@Composable
fun LineIcon(plus: Boolean, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.14f
        drawLine(color, Offset(0f, h / 2), Offset(w, h / 2), stroke, StrokeCap.Round)
        if (plus) drawLine(color, Offset(w / 2, 0f), Offset(w / 2, h), stroke, StrokeCap.Round)
    }
}

/** 상태 글자 + 오른쪽 화살표 (주문내역 카드 오른쪽) */
@Composable
fun StatusLink(text: String, color: Color = Primary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
        Spacer(Modifier.width(6.dp))
        ChevronIcon(pointsLeft = false, size = 16.dp, color = color)
    }
}

@Composable
fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal), color = TextHint)
}
