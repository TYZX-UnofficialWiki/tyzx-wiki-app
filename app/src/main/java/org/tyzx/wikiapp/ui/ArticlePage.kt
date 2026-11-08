package org.tyzx.wikiapp.ui

import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.tyzx.wikiapp.net.Wiki

/**
 * 条目页：照维基百科客户端的排版——满宽图、衬线标题、灰色定义句、松行高。
 * 正文用 WebView 渲染 Parsoid HTML（MediaWiki 语法里有大量 HTML 标签，原生渲染兼容性差）。
 */
@Composable
fun ArticlePage(
    title: String,
    fontScale: Float,
    dark: Boolean,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    var html by remember(title) { mutableStateOf<String?>(null) }
    var err by remember(title) { mutableStateOf<String?>(null) }

    LaunchedEffect(title) {
        runCatching { Wiki.pageHtml(title) }
            .onSuccess { html = it }
            .onFailure { err = it.message ?: "加载失败" }
    }

    Column(Modifier.fillMaxSize().background(cs.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = cs.onSurface, modifier = Modifier.size(22.dp))
            }
            Text(
                title,
                fontSize = 15.sp,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                val url = Wiki.HOST + "/wiki/" + java.net.URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                        },
                        "分享条目",
                    )
                )
            }) {
                Icon(Icons.Filled.Share, "分享", tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }

        Box(Modifier.fillMaxSize()) {
            val body = html
            if (err != null) {
                Text(
                    "打不开这个条目：$err",
                    color = cs.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(20.dp),
                )
            } else if (body != null) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.loadsImagesAutomatically = true
                            setBackgroundColor(0x00000000)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    val url = request.url.toString()
                                    return if (url.contains("/wiki/")) {
                                        val t = java.net.URLDecoder.decode(
                                            url.substringAfter("/wiki/")
                                                .substringBefore("#")
                                                .replace('_', ' '),
                                            "UTF-8",
                                        )
                                        if (t.isNotBlank() &&
                                            !t.startsWith("File:") &&
                                            !t.startsWith("Special:") &&
                                            !t.startsWith("Category:")
                                        ) {
                                            onOpen(t)
                                        }
                                        true
                                    } else if (url.startsWith(Wiki.HOST)) {
                                        true
                                    } else {
                                        runCatching {
                                            view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                        }
                                        true
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: android.webkit.WebResourceError,
                                ) {
                                    if (request.isForMainFrame) err = error.description?.toString()
                                }
                            }
                        }
                    },
                    update = { w ->
                        w.loadDataWithBaseURL(
                            Wiki.HOST + "/",
                            wrapHtml(body, dark, fontScale),
                            "text/html",
                            "utf-8",
                            null,
                        )
                    },
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                )
            } else {
                CircularProgressIndicator(
                    Modifier.align(Alignment.TopCenter).padding(top = 60.dp).size(22.dp),
                    color = cs.primary,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

internal fun wrapHtml(body: String, isDark: Boolean, fontScale: Float): String {
    val bg = if (isDark) "#101418" else "#FFFFFF"
    val fg = if (isDark) "#EAECF0" else "#202122"
    val sub = if (isDark) "#A2A9B1" else "#54595D"
    val line = if (isDark) "#27292D" else "#EAECF0"
    val link = if (isDark) "#8AB4F8" else "#3366CC"
    val base = (17f * fontScale).toInt()
    return """
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  html,body{margin:0;padding:0;background:$bg;color:$fg;}
  body{font-family:-apple-system,"Noto Serif SC","Noto Sans SC",serif;font-size:${base}px;
       line-height:${(base * 1.65f).toInt()}px;padding:0 16px 96px 16px;-webkit-text-size-adjust:100%;}
  h1,h2,h3{font-family:"Noto Serif SC","Noto Sans SC",serif;font-weight:600;line-height:1.35;}
  h1{font-size:${base + 9}px;margin:8px 0 6px;}
  h2{font-size:${base + 3}px;margin:28px 0 8px;padding-bottom:6px;border-bottom:1px solid $line;}
  h3{font-size:${base}px;margin:20px 0 6px;}
  p{margin:0 0 16px;}
  a{color:$link;text-decoration:none;}
  img{max-width:100%;height:auto;display:block;margin:8px 0 4px;border-radius:2px;}
  figure{margin:14px 0;padding:0;}
  figcaption{font-size:${base - 3}px;color:$sub;line-height:1.5;margin-top:6px;}
  .infobox,.navbox,.mw-editsection,.ambox,.metadata,.reference{display:none;}
  table{border-collapse:collapse;width:100%;font-size:${base - 3}px;display:block;overflow-x:auto;}
  td,th{border:1px solid $line;padding:6px 8px;text-align:left;}
  blockquote{margin:14px 0;padding-left:12px;border-left:3px solid $line;color:$sub;}
  ul,ol{padding-left:22px;margin:0 0 16px;}
  li{margin:6px 0;}
</style></head><body>$body</body></html>
""".trimIndent()
}
