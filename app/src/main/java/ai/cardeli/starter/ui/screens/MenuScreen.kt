package ai.cardeli.starter.ui.screens

import ai.cardeli.starter.contract.OrderEvent
import ai.cardeli.starter.contract.OrderUiState
import ai.cardeli.starter.contract.StoreCard
import ai.cardeli.starter.ui.components.AppScaffold
import ai.cardeli.starter.ui.components.CartItemCard
import ai.cardeli.starter.ui.components.Chip
import ai.cardeli.starter.ui.components.HintText
import ai.cardeli.starter.ui.components.MenuCard
import ai.cardeli.starter.ui.components.NavTab
import ai.cardeli.starter.ui.components.Panel
import ai.cardeli.starter.ui.components.PrimaryButton
import ai.cardeli.starter.ui.components.ScreenTitle
import ai.cardeli.starter.ui.components.SectionLabel
import ai.cardeli.starter.ui.components.StatusLink
import ai.cardeli.starter.ui.components.meters
import ai.cardeli.starter.ui.components.won
import ai.cardeli.starter.ui.theme.CardeliTheme
import ai.cardeli.starter.ui.theme.Primary
import ai.cardeli.starter.ui.theme.SurfaceHigh
import ai.cardeli.starter.ui.theme.TextSub
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 정차 중 주문 (Browsing). 가게 목록 → 가게를 고르면 OrderEvent.SelectStore → 메뉴판.
 * 메뉴를 누르면 OrderEvent.AddMenu, 장바구니 확인은 OrderEvent.ReviewOrder.
 * 가게 목록/메뉴판 중 무엇을 보일지는 이 화면 안에서만 쓰는 상태(remember)다.
 */
@Composable
fun MenuScreen(state: OrderUiState, onEvent: (OrderEvent) -> Unit) {
    var showMenu by remember { mutableStateOf(state.stores.isEmpty() || state.cart.isNotEmpty()) }

    // 데모 명령처럼 화면 밖에서 가게가 바뀌거나 장바구니가 채워지면 메뉴판으로 넘어간다
    val menusKey = state.menus.firstOrNull()?.id
    var lastMenusKey by remember { mutableStateOf(menusKey) }
    LaunchedEffect(menusKey) {
        if (menusKey != lastMenusKey) {
            lastMenusKey = menusKey
            showMenu = true
        }
    }
    LaunchedEffect(state.cart.isNotEmpty()) {
        if (state.cart.isNotEmpty()) showMenu = true
    }

    AppScaffold(active = NavTab.Home) {
        if (showMenu || state.stores.isEmpty()) {
            StoreMenu(state, onEvent, onBack = if (state.stores.isEmpty()) null else ({ showMenu = false }))
        } else {
            StoreList(state) { store ->
                onEvent(OrderEvent.SelectStore(store.id))
                showMenu = true
            }
        }
    }
}

// ---------------- 가게 목록 ----------------

@Composable
private fun StoreList(state: OrderUiState, onSelect: (StoreCard) -> Unit) {
    val charging = state.charging
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("배달 주문", accentBar = true)
        Spacer(Modifier.height(10.dp))
        Text(
            if (charging != null) {
                val remain = charging.remainMin?.let { " · 약 ${it}분 남음" } ?: ""
                "충전 중 ${charging.socPercent}%$remain · 충전이 끝나기 전에 받을 수 있는 가게부터 보여 드려요"
            } else {
                "지금 차 위치로 배달해 드려요"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (charging != null) Primary else TextSub,
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.stores, key = { it.id }) { store ->
                StoreRow(store, showChargeBadge = charging != null) { onSelect(store) }
            }
        }
    }
}

@Composable
private fun StoreRow(store: StoreCard, showChargeBadge: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(store.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(14.dp))
                Chip(store.category)
                if (showChargeBadge && store.beforeChargeEnds) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.height(40.dp).clip(MaterialTheme.shapes.medium)
                            .border(1.dp, Primary, MaterialTheme.shapes.medium)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("충전 중 수령 가능", style = MaterialTheme.typography.labelLarge, color = Primary) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${meters(store.distanceM)} · 약 ${store.etaMin}분 · 배달비 ${won(store.deliveryFee)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSub,
            )
        }
        StatusLink("메뉴 보기")
    }
}

// ---------------- 메뉴판 ----------------

@Composable
private fun StoreMenu(state: OrderUiState, onEvent: (OrderEvent) -> Unit, onBack: (() -> Unit)?) {
    val storeName = state.menus.firstOrNull()?.storeName.orEmpty()
    val card = state.stores.firstOrNull { it.name == storeName }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(44.dp)) {
        // 왼쪽: 가게 정보 + 내 장바구니
        Column(Modifier.weight(1f).fillMaxHeight()) {
            ScreenTitle(storeName, onBack = onBack)
            Spacer(Modifier.height(10.dp))
            Text(
                if (card != null) "약 ${card.etaMin}분 · 배달비 ${won(card.deliveryFee)}" else "배달비 ${won(state.deliveryFee)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSub,
            )
            Spacer(Modifier.height(16.dp))
            Chip("내 차로 배달", selected = true)
            Spacer(Modifier.height(24.dp))

            Panel(Modifier.fillMaxWidth().weight(1f)) {
                SectionLabel("내 장바구니")
                Spacer(Modifier.height(18.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (state.cart.isEmpty()) {
                        HintText("메뉴를 눌러 담아 주세요")
                    } else {
                        Column(
                            Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            state.cart.forEach { item ->
                                CartItemCard(
                                    name = item.menu.name,
                                    price = item.totalPrice,
                                    quantity = item.quantity,
                                    onMinus = { onEvent(OrderEvent.RemoveMenu(item.menu)) },
                                    onPlus = { onEvent(OrderEvent.AddMenu(item.menu)) },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("메뉴 금액", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal))
                        Spacer(Modifier.height(6.dp))
                        Text(won(state.totalPrice), style = MaterialTheme.typography.headlineMedium)
                    }
                    PrimaryButton(
                        text = "주문하기",
                        onClick = { onEvent(OrderEvent.ReviewOrder) },
                        enabled = state.cart.isNotEmpty(),
                    )
                }
            }
        }

        // 오른쪽: 메뉴 목록
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("인기 메뉴", selected = true)
                Chip("전체 메뉴")
            }
            Spacer(Modifier.height(20.dp))
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.menus, key = { it.id }) { menu ->
                    val quantity = state.cart.firstOrNull { it.menu.id == menu.id }?.quantity ?: 0
                    MenuCard(
                        menu = menu,
                        quantity = quantity,
                        onAdd = { onEvent(OrderEvent.AddMenu(menu)) },
                        onRemove = { onEvent(OrderEvent.RemoveMenu(menu)) },
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 1360, heightDp = 948)
@Composable
private fun StoreListPreview() {
    CardeliTheme { MenuScreen(PreviewStates.storeList) {} }
}

@Preview(widthDp = 1360, heightDp = 948)
@Composable
private fun MenuScreenPreview() {
    CardeliTheme { MenuScreen(PreviewStates.browsing) {} }
}
