package org.tyzx.wikiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
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

/** 三个顶层页 + 两个覆盖页，用密封类让转场能分清方向。 */
private sealed interface Screen {
    data object Explore : Screen
    data object Profile : Screen
    data object Search : Screen
    data class Article(val title: String) : Screen
}

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
        var shuffle by remember { mutableIntStateOf(0) }

        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        BackHandler(enabled = reading != null) { reading = null }
        BackHandler(enabled = reading == null && searching) { searching = false }

        val screen: Screen = when {
            reading != null -> Screen.Article(reading!!)
            searching -> Screen.Search
            tab == 0 -> Screen.Explore
            else -> Screen.Profile
        }

        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = reading == null && !searching,
            scrimColor = Color.Black.copy(alpha = 0.28f),
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = cs.background,
                    modifier = Modifier.width(300.dp),
                ) {
                    WikiDrawer(
                        version = version,
                        onExplore = { scope.launch { drawer.close() }; reading = null; searching = false; tab = 0 },
                        onShuffle = {
                            scope.launch { drawer.close() }
                            reading = null; searching = false; tab = 0; shuffle++
                        },
                        onSearch = { scope.launch { drawer.close() }; reading = null; searching = true },
                        onSettings = { scope.launch { drawer.close() }; reading = null; searching = false; tab = 2 },
                    )
                }
            },
        ) {
            Box(Modifier.fillMaxSize().background(cs.background)) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            val entering = (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 })
                            entering togetherWith fadeOut(tween(140))
                        },
                        label = "screen",
                    ) { s ->
                        when (s) {
                            Screen.Explore -> ExplorePage(
                                onOpen = { reading = it },
                                onSearch = { searching = true },
                                backdrop = backdrop,
                                reloadSignal = shuffle,
                                onMenu = { scope.launch { drawer.open() } },
                            )
                            Screen.Profile -> ProfilePage(
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
                            Screen.Search -> SearchPage(
                                onBack = { searching = false },
                                onOpen = { reading = it },
                            )
                            is Screen.Article -> ArticlePage(
                                title = s.title,
                                fontScale = fontScale,
                                dark = dark,
                                onBack = { reading = null },
                                onOpen = { reading = it },
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = reading == null && !searching,
                    enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
                ) {
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
                            blurRadiusDp = 5f,
                            refractionHeightDp = 16f,
                            refractionAmountFrac = 0.30f,
                            chromaticAberration = true,
                        ),
                        containerColorOverride = if (dark) {
                            Color.Black.copy(alpha = 0.34f)
                        } else {
                            Color(0xFFF1F3F6).copy(alpha = 0.86f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WikiDrawer(
    version: String,
    onExplore: () -> Unit,
    onShuffle: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp)) {
            Text("天一维基", fontFamily = FontFamily.Serif, fontSize = 24.sp, color = cs.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("江苏省天一中学 · 非官方维基", fontSize = 12.5.sp, color = cs.onSurfaceVariant)
        }
        HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp)
        Spacer(Modifier.height(8.dp))

        DrawerItem(Icons.Filled.Home, "探索", onExplore)
        DrawerItem(Icons.Filled.Refresh, "随机条目", onShuffle)
        DrawerItem(Icons.Filled.Search, "搜索", onSearch)
        DrawerItem(Icons.Filled.Settings, "设置", onSettings)

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp)
        Column(Modifier.padding(24.dp)) {
            Text("非官方客户端", fontSize = 12.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text("v$version · GPL-3.0", fontSize = 12.sp, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, fontSize = 15.5.sp, color = cs.onSurface, fontWeight = FontWeight.Normal)
    }
}
