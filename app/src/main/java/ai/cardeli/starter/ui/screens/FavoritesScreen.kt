package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.R
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.theme.ButtonSecondary
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.OnBackground
import ai.cardeli.starter.ui.theme.OnPrimary
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.SurfaceLow
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 피그마 "즐겨찾기" 화면. 아직 즐겨찾기 데이터가 없어 빈 화면만 있다.

private val EmptySubColor = androidx.compose.ui.graphics.Color(0xFFD9D9D9)

private enum class FavTab(val label: String, @DrawableRes val icon: Int, val iconSize: Dp) {
    Stores("음식점", R.drawable.fav_ic_shop, 25.dp),
    Menus("메뉴", R.drawable.fav_ic_fork, 16.dp),
    Coupons("쿠폰", R.drawable.fav_ic_ticket, 25.dp),
}

/** 즐겨찾기 탭. [음식점 둘러보기]를 누르면 onBrowse */
@Composable
fun FavoritesScreen(state: OrderUiState, onBrowse: () -> Unit) {
    // 음식점·메뉴·쿠폰 칸은 이 화면 안에서만 바뀐다
    var tab by remember { mutableStateOf(FavTab.Stores) }

    AppScaffold(active = NavTab.Favorites) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 12.dp)) {
                Spacer(Modifier.height(11.dp))
                ScreenTitle("즐겨찾기", accentBar = true)
                Spacer(Modifier.height(24.dp))
                Column(
                    Modifier
                        .padding(start = 1.dp)
                        .width(208.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(SurfaceLow)
                        .padding(start = 11.dp, end = 8.dp, top = 14.dp, bottom = 17.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FavTab.values().forEach { t ->
                        FavTabRow(t, selected = t == tab, count = if (t == FavTab.Stores) 0 else null) { tab = t }
                    }
                }
            }

            // 빈 화면 안내 (피그마: 화면 가운데에서 오른쪽으로 조금 치우침)
            Column(
                Modifier.fillMaxWidth().padding(start = 48.dp, top = 244.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(R.drawable.fav_empty_x), null, Modifier.size(78.dp))
                Spacer(Modifier.height(30.dp))
                Text(
                    "즐겨찾는 음식점이 없어요",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "맛집 구경이나 해볼까요?",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = EmptySubColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(31.dp))
                Box(
                    Modifier
                        .width(215.dp)
                        .height(47.dp)
                        .clip(RoundedCornerShape(58.dp))
                        .background(ButtonSecondary)
                        .clickable(onClick = onBrowse),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("음식점 둘러보기", style = MaterialTheme.typography.bodyLarge, color = OnBackground)
                }
            }
        }
    }
}

@Composable
private fun FavTabRow(tab: FavTab, selected: Boolean, count: Int?, onClick: () -> Unit) {
    val fg = if (selected) OnPrimary else OnBackground
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) Primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(25.dp), contentAlignment = Alignment.CenterStart) {
            Image(
                painterResource(tab.icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(fg),
                modifier = Modifier.size(tab.iconSize),
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(tab.label, style = MaterialTheme.typography.bodyLarge, color = fg, modifier = Modifier.weight(1f))
        if (count != null) {
            Text("$count", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = fg)
        }
    }
}

@Preview(widthDp = 1664, heightDp = 948)
@Composable
private fun FavoritesScreenPreview() {
    CardeliTheme { FavoritesScreen(OrderUiState()) {} }
}
