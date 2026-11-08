package org.tyzx.wikiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.tyzx.wikiapp.glass.TyBottomBar
import org.tyzx.wikiapp.glass.TyTab
import org.tyzx.wikiapp.glass.liquidtabs.LiquidGlassStyle
import org.tyzx.wikiapp.ui.ArticlePage
import org.tyzx.wikiapp.ui.ExplorePage
import org.tyzx.wikiapp.ui.ProfilePage
import org.tyzx.wikiapp.ui.SearchPage

/**
 * 界面语言取自维基百科安卓客户端：白底、衬线标题、细分隔线、满宽图片。
 * 底栏沿用 BiliTools 的液态玻璃 tab（GPL-3.0）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

private val WikiLight = lightColorScheme(
    primary = Color(0xFF3366CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAF3FF),
    onPrimaryContainer = Color(0xFF1A3B7A),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202122),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202122),
    surfaceVariant = Color(0xFFF8F9FA),
    onSurfaceVariant = Color(0xFF54595D),
    surfaceContainer = Color(0xFFF8F9FA),
    surfaceContainerHigh = Color(0xFFEFF2F5),
    outlineVariant = Color(0xFFEAECF0),
    error = Color(0xFFD73333),
)

private val WikiDark = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B1B33),
    primaryContainer = Color(0xFF1F2D45),
    onPrimaryContainer = Color(0xFFD6E4FF),
    background = Color(0xFF101418),
    onBackground = Color(0xFFEAECF0),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFEAECF0),
    surfaceVariant = Color(0xFF181C20),
    onSurfaceVariant = Color(0xFFA2A9B1),
    surfaceContainer = Color(0xFF181C20),
    surfaceContainerHigh = Color(0xFF202428),
    outlineVariant = Color(0xFF27292D),
    error = Color(0xFFFF8A80),
)

@Composable
fun App() {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences("tyzx2", android.content.Context.MODE_PRIVATE) }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var themeMode by remember { mutableIntStateOf(store.getInt("theme", 0).coerceIn(0, 2)) }
    var dynamic by remember { mutableStateOf(store.getBoolean("dynamic", false)) }
    var fontStep by remember { mutableIntStateOf(store.getInt("font", 1).coerceIn(0, 3)) }
    val fontScale = listOf(0.9f, 1f, 1.15f, 1.3f)[fontStep]

    val dark = when (themeMode) {
        0 -> systemDark
        1 -> false
        else -> true
    }
    val scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> WikiDark
        else -> WikiLight
    }

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    MaterialTheme(colorScheme = scheme) {
        val cs = MaterialTheme.colorScheme
        val backdrop = rememberLayerBackdrop()
        var tab by remember { mutableIntStateOf(0) }
        var reading by remember { mutableStateOf<String?>(null) }
        var searching by remember { mutableStateOf(false) }

        BackHandler(enabled = reading != null) { reading = null }
        BackHandler(enabled = reading == null && searching) { searching = false }

        Box(Modifier.fillMaxSize().background(cs.background)) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                when {
                    reading != null -> ArticlePage(
                        title = reading!!,
                        fontScale = fontScale,
                        dark = dark,
                        onBack = { reading = null },
                        onOpen = { reading = it },
                    )
                    searching -> SearchPage(
                        onBack = { searching = false },
                        onOpen = { reading = it },
                    )
                    tab == 0 -> ExplorePage(
                        onOpen = { reading = it },
                        onSearch = { searching = true },
                        backdrop = backdrop,
                    )
                    else -> ProfilePage(
                        version = version,
                        themeMode = themeMode,
                        dynamic = dynamic,
                        fontStep = fontStep,
                        onTheme = {
                            themeMode = (themeMode + 1) % 3
                            store.edit().putInt("theme", themeMode).apply()
                        },
                        onDynamic = {
                            dynamic = !dynamic
                            store.edit().putBoolean("dynamic", dynamic).apply()
                        },
                        onFont = {
                            fontStep = (fontStep + 1) % 4
                            store.edit().putInt("font", fontStep).apply()
                        },
                    )
                }
            }

            // 底栏在内容层之上，内容层已被 layerBackdrop 记录为玻璃背景
            if (reading == null && !searching) {
                TyBottomBar(
                    tabs = listOf(
                        TyTab("探索", Icons.Filled.Home),
                        TyTab("搜索", Icons.Filled.Search),
                        TyTab("我的", Icons.Filled.Person),
                    ),
                    selectedTabIndex = { tab },
                    onTabSelected = { index ->
                        if (index == 1) searching = true else tab = index
                    },
                    backdrop = backdrop,
                    glassStyle = LiquidGlassStyle(
                        blurRadiusDp = 8f,
                        refractionHeightDp = 18f,
                        refractionAmountFrac = 0.32f,
                        chromaticAberration = true,
                    ),
                    surfaceAlpha = if (dark) 0.34f else 0.55f,
                )
            }
        }
    }
}
