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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 我的：外观、字号、关于。行样式照维基百科客户端——标题 + 说明，行间一条极淡的分隔线。 */
@Composable
fun ProfilePage(
    version: String,
    themeMode: Int,
    dynamic: Boolean,
    fontStep: Int,
    onTheme: () -> Unit,
    onDynamic: () -> Unit,
    onFont: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val themeText = listOf("跟随系统", "浅色", "深色")[themeMode]
    val fontText = listOf("小", "标准", "大", "特大")[fontStep]

    Column(Modifier.fillMaxSize().background(cs.background).verticalScroll(rememberScrollState())) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("我的", fontFamily = FontFamily.Serif, fontSize = 26.sp, color = cs.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "江苏省天一中学 · 非官方维基",
                fontSize = 13.sp,
                color = cs.onSurfaceVariant,
            )
        }

        GroupTitle("外观")
        SettingRow("主题", themeText, onTheme)
        SettingRow("字号", fontText, onFont)
        SettingRow("动态取色", if (dynamic) "开" else "关", onDynamic)

        GroupTitle("阅读")
        InfoRow("渲染方式", "维基百科客户端同款排版")
        InfoRow("内容来源", "zh-wiki.tyzx.org")

        GroupTitle("关于")
        InfoRow("版本", version)
        InfoRow("协议", "GPL-3.0")
        InfoRow("界面参考", "维基百科安卓客户端")
        InfoRow("底栏参考", "BiliTools（GPL-3.0）")

        Spacer(Modifier.height(20.dp))
        Text(
            "本应用是非官方客户端，与江苏省天一中学及天一维基管理团队无隶属关系。" +
                "条目内容版权归各自作者所有，以站点声明为准。",
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun GroupTitle(t: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        t,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.5.sp, color = cs.onSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = cs.onSurfaceVariant)
    }
    HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun InfoRow(title: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.5.sp, color = cs.onSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.5.sp, color = cs.onSurfaceVariant)
    }
    HorizontalDivider(color = cs.outlineVariant, thickness = 0.7.dp, modifier = Modifier.padding(start = 16.dp))
}
