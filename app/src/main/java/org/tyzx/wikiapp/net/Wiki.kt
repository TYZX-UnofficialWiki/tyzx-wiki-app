package org.tyzx.wikiapp.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 天一维基接口层。全部走匿名接口，登录态另加 cookie 即可复用。
 */
object Wiki {
    const val HOST = "https://zh-wiki.tyzx.org"
    const val API = "$HOST/api.php"
    const val REST = "$HOST/rest.php/v1"

    private const val UA = "TianyiWikiAndroid/0.1 (+https://github.com/TYZX-UnofficialWiki/tyzx-wiki-android)"

    /** 登录态 cookie。进程内保存，重启需重新登录。 */
    private val jar = linkedMapOf<String, String>()

    var user: String? = null
        private set

    private fun cookieHeader(): String? = synchronized(jar) {
        if (jar.isEmpty()) null else jar.entries.joinToString("; ") { (k, v) -> "$k=$v" }
    }

    private fun rememberCookies(conn: java.net.HttpURLConnection) {
        synchronized(jar) {
        conn.headerFields.forEach { (k, v) ->
            if (k != null && k.equals("Set-Cookie", ignoreCase = true)) {
                v.forEach { raw ->
                    val pair = raw.substringBefore(';')
                    val i = pair.indexOf('=')
                    if (i > 0) jar[pair.substring(0, i).trim()] = pair.substring(i + 1).trim()
                }
            }
        }
        }
    }

    suspend fun raw(url: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json, text/html;q=0.9")
            cookieHeader()?.let { setRequestProperty("Cookie", it) }
        }
        try {
            val code = conn.responseCode
            rememberCookies(conn)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            if (code !in 200..299) error("HTTP $code: ${text.take(200)}")
            text
        } finally {
            conn.disconnect()
        }
    }

    suspend fun api(vararg params: Pair<String, String>): JSONObject {
        val q = buildString {
            append("?format=json&formatversion=2")
            params.forEach { (k, v) -> append('&').append(k).append('=').append(enc(v)) }
        }
        return JSONObject(raw(API + q))
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    data class Feed(
        val title: String,
        val extract: String?,
        val thumb: String?,
        val categories: List<String>,
    )

    /** 随机条目，带缩略图与导语 */
    suspend fun random(limit: Int = 16): List<Feed> {
        val j = api(
            "action" to "query",
            "generator" to "random",
            "grnnamespace" to "0",
            "grnlimit" to limit.toString(),
            "prop" to "pageimages|extracts|info",
            "piprop" to "thumbnail",
            "pithumbsize" to "600",
            "exintro" to "1",
            "explaintext" to "1",
            "exlimit" to "max",
            "inprop" to "url",
        )
        val pages = j.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until pages.length()).mapNotNull { i ->
            val p = pages.optJSONObject(i) ?: return@mapNotNull null
            val title = p.optString("title")
            if (title.isBlank()) return@mapNotNull null
            Feed(
                title = title,
                extract = p.optString("extract").ifBlank { null },
                thumb = p.optJSONObject("thumbnail")?.optString("source"),
                categories = emptyList(),
            )
        }
    }

    /** 分类筛选 + 随机：用 incategory 搜索，拿不到图就退化成标题列表 */
    suspend fun byCategory(category: String, limit: Int = 16): List<Feed> {
        if (category.isBlank()) return random(limit)
        val j = api(
            "action" to "query",
            "generator" to "search",
            "gsrsearch" to "incategory:\"$category\"",
            "gsrnamespace" to "0",
            "gsrlimit" to limit.toString(),
            "prop" to "pageimages|extracts",
            "piprop" to "thumbnail",
            "pithumbsize" to "600",
            "exintro" to "1",
            "explaintext" to "1",
            "exlimit" to "max",
        )
        val pages = j.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until pages.length()).mapNotNull { i ->
            val p = pages.optJSONObject(i) ?: return@mapNotNull null
            val title = p.optString("title")
            if (title.isBlank()) return@mapNotNull null
            Feed(
                title = title,
                extract = p.optString("extract").ifBlank { null },
                thumb = p.optJSONObject("thumbnail")?.optString("source"),
                categories = emptyList(),
            )
        }
    }

    data class Recent(val title: String, val timestamp: String, val comment: String?)

    suspend fun recent(limit: Int = 6): List<Recent> {
        val j = api(
            "action" to "query",
            "list" to "recentchanges",
            "rclimit" to limit.toString(),
            "rcnamespace" to "0",
            "rcprop" to "title|timestamp|comment",
            "rctype" to "edit|new",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("recentchanges") ?: return emptyList()
        val seen = HashSet<String>()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val t = o.optString("title")
            if (t.isBlank() || !seen.add(t)) return@mapNotNull null
            Recent(t, o.optString("timestamp"), o.optString("comment").ifBlank { null })
        }
    }

    data class Hit(val title: String, val snippet: String)

    suspend fun search(q: String, limit: Int = 20): List<Hit> {
        if (q.isBlank()) return emptyList()
        val j = api(
            "action" to "query",
            "list" to "search",
            "srsearch" to q,
            "srlimit" to limit.toString(),
            "srnamespace" to "0",
            "srprop" to "snippet",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Hit(o.optString("title"), stripTags(o.optString("snippet")))
        }
    }

    private fun stripTags(s: String): String =
        s.replace(Regex("<[^>]+>"), "").replace("&quot;", "\"").replace("&amp;", "&").trim()

    /** Parsoid 输出的完整 HTML */
    suspend fun pageHtml(title: String): String = raw("$REST/page/${enc(title)}/html")

    data class Cat(val name: String, val size: Int)

    suspend fun categoriesWithSize(): List<Cat> {
        val j = api(
            "action" to "query",
            "list" to "allcategories",
            "aclimit" to "100",
            "acprop" to "size",
            "acmin" to "1",
            "acdir" to "ascending",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("allcategories") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val n = o.optString("category")
            if (n.isBlank()) null else Cat(n, o.optInt("size"))
        }.sortedByDescending { it.size }
    }

    suspend fun categoryMembers(cat: String, limit: Int = 100): List<String> {
        val j = api(
            "action" to "query",
            "list" to "categorymembers",
            "cmtitle" to "Category:$cat",
            "cmlimit" to limit.toString(),
            "cmnamespace" to "0",
            "cmprop" to "title",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("categorymembers") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("title") }
            .filter { it.isNotBlank() }
    }

    suspend fun categories(): List<String> {
        val j = api(
            "action" to "query",
            "list" to "allcategories",
            "aclimit" to "60",
            "acprop" to "size",
            "acmin" to "1",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("allcategories") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("category") }
            .filter { it.isNotBlank() }
    }

    suspend fun post(vararg params: Pair<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val body = buildString {
            append("format=json&formatversion=2")
            params.forEach { (k, v) -> append('&').append(enc(k)).append('=').append(enc(v)) }
        }.toByteArray()
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 25000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            cookieHeader()?.let { setRequestProperty("Cookie", it) }
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            rememberCookies(conn)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            JSONObject(text.ifBlank { "{}" })
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun token(type: String): String =
        api("action" to "query", "meta" to "tokens", "type" to type)
            .optJSONObject("query")?.optJSONObject("tokens")?.optString(type + "token")
            .orEmpty()

    /** 返回 null 表示成功，否则返回给用户看的中文错误。 */
    suspend fun login(name: String, pass: String): String? {
        val t = token("login")
        val j = post("action" to "login", "lgname" to name, "lgpassword" to pass, "lgtoken" to t)
        val r = j.optJSONObject("login")
        val result = r?.optString("result").orEmpty()
        return when (result) {
            "Success" -> {
                user = r?.optString("lgusername")?.ifBlank { name }
                null
            }
            "Failed" -> {
                val why = r?.optString("reason").orEmpty()
                when {
                    why.contains("Incorrect username or password", true) -> "用户名或密码不对"
                    why.contains("not specified a username", true) -> "请填写用户名"
                    why.contains("not specified a password", true) -> "请填写密码"
                    why.contains("blocked", true) -> "这个账号被封禁了"
                    why.isBlank() -> "登录失败"
                    else -> why
                }
            }
            "Throttled" -> "尝试太频繁，等一会儿再试"
            "NeedToken" -> "会话过期，请重试"
            "Aborted" -> "登录被站点拒绝"
            else -> "登录失败（$result）"
        }
    }

    suspend fun logout() {
        runCatching { post("action" to "logout", "token" to token("csrf")) }
        jar.clear()
        user = null
    }

    /** 注册时可能拿到的验证码（ConfirmEdit）。 */
    data class RegCaptcha(val id: String?, val imageUrl: String?)

    data class RegResult(
        val error: String?,
        val captcha: RegCaptcha?,
        val ok: Boolean,
    )

    suspend fun register(
        name: String,
        pass: String,
        email: String,
        captcha: RegCaptcha? = null,
        captchaWord: String? = null,
    ): RegResult {
        val t = token("createaccount")
        val params = mutableListOf(
            "action" to "createaccount",
            "createreturnurl" to HOST,
            "createtoken" to t,
            "username" to name,
            "password" to pass,
            "retype" to pass,
            "email" to email,
            "realname" to "",
        )
        if (captcha != null) {
            params += "captchaId" to (captcha.id ?: "")
            params += "captchaWord" to (captchaWord ?: "")
        }
        val j = post(*params.toTypedArray())
        val r = j.optJSONObject("createaccount")
        if (r?.optString("status") == "PASS") {
            user = r.optString("username").ifBlank { name }
            return RegResult(null, null, true)
        }

        // AuthManager 把验证码塞在 requests[].fields 里
        var captchaNow: RegCaptcha? = null
        val reqs = r?.optJSONArray("requests")
        if (reqs != null && reqs.length() > 0) {
            val f = reqs.optJSONObject(0)?.optJSONObject("fields")
            val id = f?.optJSONObject("captchaId")?.optString("value")
            val info = f?.optJSONObject("captchaInfo")?.optString("value").orEmpty()
            val src = Regex("src=\"([^\"]+)\"").find(info)?.groupValues?.get(1)
            if (id != null || src != null) {
                captchaNow = RegCaptcha(
                    id = id,
                    imageUrl = src?.let { if (it.startsWith("http")) it else HOST + it },
                )
            }
        }

        val msg = r?.optString("message")?.ifBlank { null }
            ?: r?.optJSONObject("message")?.optString("text")
            ?: j.optJSONObject("error")?.optString("info")
            ?: "注册失败"
        return RegResult(msg, captchaNow, false)
    }

    /** 带会话 cookie 取二进制（验证码图片用）。 */
    suspend fun fetchBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("User-Agent", UA)
            cookieHeader()?.let { setRequestProperty("Cookie", it) }
        }
        try {
            val code = conn.responseCode
            rememberCookies(conn)
            if (code !in 200..299) error("HTTP $code")
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun csrf(): String = token("csrf")

    /** 返回 (wikitext, 最新版本号)。 */
    suspend fun pageSource(title: String): Pair<String, Long> {
        val j = JSONObject(raw("$REST/page/" + enc(title)))
        // REST 返回的 latest 是对象：{"id":366,"timestamp":"..."}，不是数字
        val rev = j.optJSONObject("latest")?.optLong("id", 0L) ?: 0L
        return (j.optString("source") to rev)
    }

    data class SaveResult(val ok: Boolean, val message: String?, val newRev: Long)

    suspend fun save(title: String, text: String, summary: String, baseRev: Long, watch: Boolean): SaveResult {
        val j = post(
            "action" to "edit",
            "title" to title,
            "text" to text,
            "summary" to summary,
            "token" to csrf(),
            "baserevid" to baseRev.toString(),
            "watchlist" to if (watch) "watch" else "nochange",
            "bot" to "0",
        )
        val e = j.optJSONObject("edit")
        if (e?.optString("result") == "Success") {
            return SaveResult(true, null, e.optLong("newrevid", 0L))
        }
        val err = j.optJSONObject("error")
        if (err != null) {
            val info = err.optString("info")
            val code = err.optString("code")
            return SaveResult(
                false,
                when (code) {
                    "editconflict" -> "有人抢先改了这条，请重新加载再改"
                    "protectedpage", "permissiondenied" -> "你没有编辑这条的权限"
                    "blocked" -> "这个账号被封禁了"
                    "badtoken" -> "登录状态过期，请重新登录"
                    else -> info.ifBlank { code }
                },
                0L,
            )
        }
        return SaveResult(false, "保存失败", 0L)
    }

    /** 讨论页 HTML；页面不存在时返回空串。 */
    suspend fun talkHtml(title: String): String {
        val t = "Talk:" + title
        return try {
            raw("$REST/page/" + enc(t) + "/html")
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) "" else throw e
        }
    }

    /** 在讨论页新开一节。 */
    suspend fun addTopic(page: String, heading: String, body: String): SaveResult {
        val j = post(
            "action" to "edit",
            "title" to "Talk:$page",
            "section" to "new",
            "sectiontitle" to heading,
            "text" to body,
            "summary" to heading,
            "token" to csrf(),
            "watchlist" to "nochange",
        )
        val e = j.optJSONObject("edit")
        if (e?.optString("result") == "Success") return SaveResult(true, null, e.optLong("newrevid", 0L))
        val err = j.optJSONObject("error")
        return SaveResult(
            false,
            when (err?.optString("code")) {
                "badtoken" -> "登录状态过期，请重新登录"
                "blocked" -> "这个账号被封禁了"
                null -> "发送失败"
                else -> err.optString("info").ifBlank { err.optString("code") }
            },
            0L,
        )
    }

    data class UploadResult(val ok: Boolean, val filename: String?, val message: String?)

    /** multipart 上传文件。bytes 为原图字节。 */
    suspend fun upload(fileName: String, bytes: ByteArray, mime: String, comment: String): UploadResult =
        withContext(Dispatchers.IO) {
            val boundary = "----TyzxWiki" + System.currentTimeMillis()
            val crlf = "\r\n"
            fun part(name: String, value: String) = buildString {
                append("--").append(boundary).append(crlf)
                append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(crlf)
                append(crlf).append(value).append(crlf)
            }
            val head = StringBuilder()
            head.append(part("action", "upload"))
            head.append(part("format", "json"))
            head.append(part("formatversion", "2"))
            head.append(part("filename", fileName))
            head.append(part("comment", comment))
            head.append(part("ignorewarnings", "1"))
            head.append(part("token", csrf()))
            head.append("--").append(boundary).append(crlf)
            head.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append(crlf)
            head.append("Content-Type: ").append(mime).append(crlf)
            head.append(crlf)
            val tail = crlf + "--" + boundary + "--" + crlf
            val headBytes = head.toString().toByteArray()
            val tailBytes = tail.toByteArray()

            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20000
                readTimeout = 90000
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                cookieHeader()?.let { setRequestProperty("Cookie", it) }
            }
            try {
                conn.setFixedLengthStreamingMode(headBytes.size + bytes.size + tailBytes.size)
                conn.outputStream.use { out ->
                    out.write(headBytes)
                    out.write(bytes)
                    out.write(tailBytes)
                    out.flush()
                }
                val code = conn.responseCode
                rememberCookies(conn)
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                if (text.isBlank()) {
                    return@withContext UploadResult(false, null, "服务器没有返回内容（HTTP $code）")
                }
                val j = JSONObject(text)
                val up = j.optJSONObject("upload")
                if (up?.optString("result") == "Success") {
                    return@withContext UploadResult(true, up.optString("filename").ifBlank { fileName }, null)
                }
                val err = j.optJSONObject("error")
                val warn = up?.optJSONObject("warnings")
                val msg = when {
                    err != null -> err.optString("info").ifBlank { err.optString("code") }
                    warn != null -> "文件已存在或需要确认：${warn.keys().asSequence().joinToString()}"
                    else -> "上传失败"
                }
                UploadResult(false, null, msg)
            } finally {
                conn.disconnect()
            }
        }

    /** 带 cookie 的 JSON POST，返回原始文本（Parsoid 转换接口回的是 text/plain）。 */
    private suspend fun postText(url: String, json: String): String = withContext(Dispatchers.IO) {
        val body = json.toByteArray()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 40000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Content-Type", "application/json")
            cookieHeader()?.let { setRequestProperty("Cookie", it) }
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            rememberCookies(conn)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            if (code !in 200..299) error("HTTP $code: ${text.take(160)}")
            text
        } finally {
            conn.disconnect()
        }
    }

    /** 可视化编辑保存前的关键一步：把改过的 HTML 交回 Parsoid 转成 wikitext。 */
    suspend fun htmlToWikitext(title: String, html: String): String = postText(
        "$REST/transform/html/to/wikitext/" + enc(title),
        JSONObject().put("html", html).toString(),
    )
}
