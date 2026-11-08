package org.tyzx.wikiapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import org.tyzx.wikiapp.net.Wiki

/**
 * 探索页，结构照维基百科客户端的 Explore feed：
 * 顶部字标 + 搜索框，然后是「随机条目」「最近更新」「分类」三块，块与块之间用细分隔线。
 */
@Composable
fun ExplorePage(
    onOpen: (String) -> Unit,
    onSearch: () -> Unit,
    backdrop: LayerBackdrop,
    reloadSignal: Int = 0,
    onMenu: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    var feed by remember { mutableStateOf<List<Wiki.Feed>>(emptyList()) }
    var recent by remember { mutableStateOf<List<Wiki.Recent>>(emptyList()) }
    var cats by remember { mutableStateOf<List<Wiki.Cat>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload, reloadSignal) {
        loading = true
        runCatching { Wiki.random(12) }.onSuccess { feed = it }
        runCatching { Wiki.recent(6) }.onSuccess { recent = it }
        runCatching { Wiki.categoriesWithSize() }.onSuccess { cats = it.take(8) }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.statusBarsPadding()) {
                // 字标行
                Row(
                    Modifier.fillMaxWidth().padding(start = 6.dp, end = 10.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(50)).clickable { onMenu() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Menu, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "天一维基",
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(50)).clickable { onSearch() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                }

                // 搜索框
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.surfaceContainer)
                        .clickable { onSearch() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("搜索天一维基", fontSize = 15.sp, color = cs.onSurfaceVariant)
                }
            }
        }

        // 最近更新
        if (recent.isNotEmpty()) {
            item { SectionHeader("最近更新") }
            items(recent) { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(r.title) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            r.title,
                            fontSize = 16.sp,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            ago(r.timestamp),
                            fontSize = 12.5.sp,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp)
            }
        }

        // 随机条目
        if (feed.isNotEmpty()) {
            item { SectionHeader("随机条目", action = "换一批") { reload++ } }
            items(feed) { f -> FeedBlock(f, onOpen) }
        }

        // 分类
        if (cats.isNotEmpty()) {
            item { SectionHeader("分类") }
            items(cats) { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(c.name) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("分类:" + c.name, fontSize = 15.sp, color = cs.primary, modifier = Modifier.weight(1f))
                    Text("${c.size}", fontSize = 13.sp, color = cs.onSurfaceVariant)
                }
                HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp)
            }
        }

        if (loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = cs.primary, strokeWidth = 2.dp)
                }
            }
        }

        item { Spacer(Modifier.height(110.dp)) }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                fontSize = 13.sp,
                color = cs.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onAction() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** 随机条目卡片：满宽图片 + 衬线标题 + 灰色摘要，外面一圈细边框。 */
@Composable
private fun FeedBlock(f: Wiki.Feed, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(cs.surface)
            .clickable { onOpen(f.title) },
    ) {
        val thumb = f.thumb
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 12.dp)) {
            Text(
                f.title,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                color = cs.onSurface,
            )
            val extract = f.extract
            if (!extract.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    extract,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun ago(ts: String): String {
    return runCatching {
        val t = java.time.OffsetDateTime.parse(ts)
        val mins = java.time.Duration.between(t, java.time.OffsetDateTime.now()).toMinutes()
        when {
            mins < 1 -> "刚刚"
            mins < 60 -> "$mins 分钟前"
            mins < 60 * 24 -> "${mins / 60} 小时前"
            else -> "${mins / (60 * 24)} 天前"
        }
    }.getOrDefault("")
}
