package ai.cardeli.starter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// TODO(UI): 밝은 테마가 필요하면 lightColorScheme을 추가한다. 차량 화면 기준으로 일단 어두운 테마 하나만 둔다.
private val AppColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
)

@Composable
fun CardeliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        // TODO(UI): shapes = AppShapes (모서리 둥글기 규격이 정해지면)
        content = content,
    )
}
