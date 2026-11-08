# 天一维基 · Android 客户端（第二版）

江苏省天一中学非官方维基的安卓客户端。

⚠️ **本应用是非官方客户端**，与江苏省天一中学、天一维基管理团队均无隶属关系。

## 这一版和上一版的区别

上一版（[tyzx-wiki-android](https://github.com/TYZX-UnofficialWiki/tyzx-wiki-android)）是自制布局，用户评价"模仿拙劣"。这一版换了思路：

- **界面**：完全照维基百科安卓客户端的视觉语言——白底、衬线标题、细分隔线、满宽图片、行间 0.7dp 淡线。
- **底栏**：沿用 [BiliTools](https://github.com/happycola233/BiliTools) 的液态玻璃 tab（静止态是平涂的 Material 3 指示器，按下才液化成带色散的玻璃）。

两个仓库并存，互不影响，可以同时安装（包名不同）。

## 状态

| 模块 | 状态 |
|---|---|
| 探索页（随机条目 / 最近更新 / 分类） | 可用 |
| 条目页（WebView 渲染 Parsoid HTML） | 可用 |
| 搜索 | 可用 |
| 我的（主题 / 字号 / 关于） | 可用 |
| 液态玻璃底栏 | 可用 |
| 登录、编辑、上传、讨论、分享 | 本版未移植 |

## 下载

最新构建：[`v1.0.*`](https://github.com/TYZX-UnofficialWiki/tyzx-wiki-app/releases/latest)

直链：`https://github.com/TYZX-UnofficialWiki/tyzx-wiki-app/releases/latest/download/app-debug.apk`

要求 Android 13（API 33）及以上，arm64。

## 构建

```sh
gradle :app:assembleDebug
```

CI 在 `.github/workflows/build.yml`，每次推 main 自动出 APK 并发 Release。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)（Apache-2.0）：玻璃的模糊/折射/色散
- [BiliTools](https://github.com/happycola233/BiliTools)（GPL-3.0）：液态玻璃底栏
- [Coil](https://github.com/coil-kt/coil)（Apache-2.0）：图片
- MediaWiki API + Parsoid REST（`/rest.php/v1/page/{title}/html`）

## 致谢

| 项目 | 协议 | 用途 |
|---|---|---|
| [BiliTools](https://github.com/happycola233/BiliTools) | GPL-3.0 | 液态玻璃底栏（`glass/liquidtabs/`） |
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | Apache-2.0 | 玻璃渲染底座 |
| [维基百科安卓客户端](https://github.com/wikimedia/apps-android-wikipedia) | Apache-2.0 | 界面视觉语言参考 |
| [Ghost Downloader 3](https://github.com/XiaoYouChR/Ghost-Downloader-3) | GPL-3.0 | 交互参考（未使用代码） |

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。
