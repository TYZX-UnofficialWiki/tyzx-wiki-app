package org.tyzx.wikiapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.tyzx.wikiapp.net.Wiki

/** 搜索页：维基百科客户端的搜索是「一个输入框 + 一条细分隔线 + 结果列表」，没有多余装饰。 */
@Composable
fun SearchPage(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Wiki.Hit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    LaunchedEffect(q) {
        if (q.isBlank()) {
            hits = emptyList()
            return@LaunchedEffect
        }
        loading = true
        delay(280)
        runCatching { Wiki.search(q, 30) }.onSuccess { hits = it }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(cs.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 2.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = cs.onSurface, modifier = Modifier.size(22.dp))
            }
            Row(
                Modifier.weight(1f).padding(end = 14.dp)
                    .background(cs.surfaceContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) {
                        Text("搜索条目", fontSize = 15.sp, color = cs.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = q,
                        onValueChange = { q = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
        }

        when {
            q.isBlank() -> CenterHint("输入关键词搜索天一维基", cs.onSurfaceVariant)
            hits.isEmpty() && !loading -> CenterHint("没有找到「$q」", cs.onSurfaceVariant)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(hits) { h ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onOpen(h.title) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(h.title, fontFamily = FontFamily.Serif, fontSize = 17.sp, color = cs.onSurface)
                        if (h.snippet.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(h.snippet, fontSize = 13.sp, lineHeight = 19.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp)
                }
            }
        }
    }
}

@Composable
private fun CenterHint(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, fontSize = 14.sp, color = color, modifier = Modifier.padding(top = 40.dp))
    }
}
