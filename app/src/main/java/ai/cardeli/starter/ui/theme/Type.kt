package ai.cardeli.starter.ui.theme

import ai.cardeli.starter.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 피그마 글꼴: SUIT (OFL-1.1, res/font)
val Suit = FontFamily(
    Font(R.font.suit_regular, FontWeight.Normal),
    Font(R.font.suit_semibold, FontWeight.SemiBold),
    Font(R.font.suit_bold, FontWeight.Bold),
    Font(R.font.suit_extrabold, FontWeight.ExtraBold),
)

private fun suit(size: Int, weight: FontWeight) =
    TextStyle(fontFamily = Suit, fontSize = size.sp, fontWeight = weight)

// 피그마 크기 기준: 화면 제목 36, 패널 제목 42, 카드 제목 24~27, 본문 20, 보조 16
val AppTypography = Typography(
    displaySmall = suit(42, FontWeight.Bold),      // 배달 상태 제목, 주행 화면 한 줄
    headlineLarge = suit(36, FontWeight.Bold),     // 화면 제목, 가게 이름
    headlineMedium = suit(32, FontWeight.Bold),    // 결제 금액
    headlineSmall = suit(27, FontWeight.SemiBold), // 주문내역 카드 가게 이름
    titleLarge = suit(24, FontWeight.Bold),        // 메뉴 이름, 합계
    titleMedium = suit(20, FontWeight.Bold),       // 섹션 제목, 가격
    bodyLarge = suit(20, FontWeight.SemiBold),     // 본문, 버튼
    bodyMedium = suit(16, FontWeight.SemiBold),    // 보조 글
    bodySmall = suit(14, FontWeight.Normal),       // 설명
    labelLarge = suit(18, FontWeight.SemiBold),    // 칩
    labelMedium = suit(16, FontWeight.SemiBold),   // 진행 단계 글자
)
