// 天一维基底栏：直接移植 BiliTools（GPL-3.0）的 ui/liquidtabs，
// 其上游为 Kyant0/AndroidLiquidGlass 2.0.0（Apache-2.0）。
// 改动：数据由 String/Icon 资源改为运行时列表；主题色取自 MaterialTheme。
package org.tyzx.wikiapp.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import org.tyzx.wikiapp.glass.liquidtabs.LiquidBottomTab
import org.tyzx.wikiapp.glass.liquidtabs.LiquidBottomTabs
import org.tyzx.wikiapp.glass.liquidtabs.LiquidGlassStyle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars

data class TyTab(val label: String, val icon: ImageVector)

@Composable
fun TyBottomBar(
    tabs: List<TyTab>,
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: LayerBackdrop,
    glassStyle: LiquidGlassStyle,
    surfaceAlpha: Float = 0.25f,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        LiquidBottomTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = tabs.size,
            accentColor = MaterialTheme.colorScheme.primary,
            containerColor = (if (isLightTheme) Color.White else Color.Black)
                .copy(alpha = surfaceAlpha.coerceIn(0f, 1f)),
            glassStyle = glassStyle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
        ) {
            val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            tabs.forEachIndexed { index, tab ->
                LiquidBottomTab(onClick = { onTabSelected(index) }) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = tab.label,
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
