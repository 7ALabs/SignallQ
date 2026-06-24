package io.veloo.app.feature.fibra

import android.util.Base64
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "SignallQFibra"
private const val USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

// Gerencia sessão HTTP com o modem Nokia G-1425G-B.
// Protocolo: GET page → extrair crypto material → POST login → usar sid/X-SID para páginas.
internal class NokiaModemClient(private val host: String) {

    private val baseUrl = "http://$host"
    private var sid = ""
    private var lsid = ""
    private var lang = "eng"
    private var xSid = ""

    val isLoggedIn: Boolean get() = sid.isNotEmpty()

    @Throws(IOException::class)
    fun login(username: String, password: String) {
        Log.i(TAG, "login: iniciando em $baseUrl")

        // Etapa 1: GET página de login para extrair crypto material.
        val loginPage = httpGet("/?t=${System.currentTimeMillis()}&lang=eng")
        val html = loginPage.body
        Log.i(TAG, "login: page=${html.length} bytes status=${loginPage.statusCode}")

        val pubKeyBase64 = NokiaModemCrypto.extractPublicKeyBase64(html)
            ?: throw IOException("pubkey nao encontrado na pagina de login")
        val nonce = NokiaModemCrypto.extractNonce(html)
            ?: throw IOException("nonce nao encontrado na pagina de login")
        val csrfToken = NokiaModemCrypto.extractCsrfToken(html)
            ?: throw IOException("csrf_token nao encontrado na pagina de login")

        Log.i(TAG, "login: pubkey=${pubKeyBase64.length}chars nonce=${nonce.length} token=${csrfToken.length}")

        // Etapa 2: gerar material criptográfico.
        // aesKey/iv: usados para cifrar o payload interno → ct.
        // decKey/decIv: passados dentro do payload cifrado como chaves de sessão internas.
        val aesKey = NokiaModemCrypto.generateSecureBytes(16)
        val iv = NokiaModemCrypto.generateSecureBytes(16)
        val decKey = NokiaModemCrypto.generateSecureBytes(16)
        val decIv = NokiaModemCrypto.generateSecureBytes(16)

        val aesKeyB64 = Base64.encodeToString(aesKey, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        // Payload interno (plaintext que será cifrado em ct).
        // Começa com & conforme o crypto_page.js do firmware.
        val plainPayload = "&username=$username" +
            "&password=${URLEncoder.encode(password, "UTF-8")}" +
            "&csrf_token=$csrfToken" +
            "&nonce=$nonce" +
            "&enckey=${NokiaModemCrypto.base64UrlEscape(Base64.encodeToString(decKey, Base64.NO_WRAP))}" +
            "&enciv=${NokiaModemCrypto.base64UrlEscape(Base64.encodeToString(decIv, Base64.NO_WRAP))}"

        // ct = AES-CBC-ISO7816-4(plainPayload) → base64url sem padding.
        val ctBytes = NokiaModemCrypto.aesCbcEncryptSjcl(aesKey, iv, plainPayload.toByteArray(Charsets.UTF_8))
        val ct = NokiaModemCrypto.base64UrlNoPad(Base64.encodeToString(ctBytes, Base64.NO_WRAP))

        // ck = RSA-PKCS1v1.5("base64(aesKey) base64(iv)") → base64url-escape (= → .).
        val ckRaw = NokiaModemCrypto.rsaEncryptPkcs1(pubKeyBase64, "$aesKeyB64 $ivB64")
        val ck = NokiaModemCrypto.base64UrlEscape(ckRaw)

        Log.i(TAG, "login: ct=${ct.length}chars ck=${ck.length}chars")

        // Etapa 3: POST /login.cgi — body contém apenas encrypted=1, ct e ck.
        val postBody = "encrypted=1&ct=$ct&ck=$ck"
        val resp = httpPost("/login.cgi", postBody, loginPage.cookies)

        Log.i(TAG, "login: resposta status=${resp.statusCode} headers=${resp.headers}")

        // Extrair sid da resposta (prioridade: X-SID header, depois Set-Cookie).
        val respXSid = resp.headers["x-sid"]?.trim() ?: ""
        val respSid = resp.cookies["sid"]?.trim() ?: respXSid
        val respLsid = resp.cookies["lsid"]?.trim() ?: ""
        val respLang = resp.cookies["lang"]?.trim() ?: "eng"

        if ((resp.statusCode == 299 || resp.statusCode == 200) && respSid.isNotEmpty()) {
            sid = respSid
            lsid = respLsid
            lang = respLang.ifEmpty { "eng" }
            xSid = respSid
            Log.i(TAG, "login: SUCESSO sid=${sid.take(8)}...")
            return
        }

        // Diagnosticar falha via err_t no body.
        val errT = Regex("""err_t\s*=\s*\[([^\]]*)]""").find(resp.body)?.groupValues?.get(1)?.trim()
        val mensagem = when (errT) {
            "0" -> "sessao em uso por outro acesso (err_t=0)"
            "1" -> "credenciais invalidas (err_t=1)"
            "2" -> "token expirado — retry necessario (err_t=2)"
            else -> "login falhou: status=${resp.statusCode} err_t=$errT body=${resp.body.take(200)}"
        }
        Log.w(TAG, "login: FALHA $mensagem")
        throw IOException(mensagem)
    }

    @Throws(IOException::class)
    fun fetchPage(path: String): String {
        val resp = httpGet(path, buildSessionHeaders())
        Log.i(TAG, "fetchPage: $path status=${resp.statusCode} bytes=${resp.body.length}")
        return resp.body
    }

    private fun buildSessionHeaders(): Map<String, String> {
        val cookieParts = mutableListOf<String>()
        if (sid.isNotEmpty()) cookieParts.add("sid=$sid")
        if (lsid.isNotEmpty()) cookieParts.add("lsid=$lsid")
        cookieParts.add("lang=$lang")
        return mapOf(
            "Cookie" to cookieParts.joinToString("; "),
            "X-SID" to xSid,
            "Referer" to "$baseUrl/",
            "X-Requested-With" to "XMLHttpRequest",
        )
    }

    private data class HttpResponse(
        val statusCode: Int,
        val body: String,
        val headers: Map<String, String>,
        val cookies: Map<String, String>,
    )

    // Executa GET seguindo redirecionamentos manualmente para preservar Set-Cookie de cada hop.
    private fun httpGet(path: String, extraHeaders: Map<String, String> = emptyMap()): HttpResponse {
        val accumulatedCookies = mutableMapOf<String, String>()
        var currentUrl = "$baseUrl$path"
        var hops = 0

        while (hops < 5) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.9")
                if (accumulatedCookies.isNotEmpty()) {
                    val cookieStr = accumulatedCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    setRequestProperty("Cookie", cookieStr)
                }
                extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val resp = readResponse(conn)
            accumulatedCookies.putAll(resp.cookies)

            if (resp.statusCode in 301..303 || resp.statusCode == 307 || resp.statusCode == 308) {
                val location = resp.headers["location"] ?: break
                currentUrl = if (location.startsWith("http")) location else "$baseUrl$location"
                Log.i(TAG, "httpGet: redirect ${resp.statusCode} → $currentUrl")
                hops++
                continue
            }

            // Merge cookies acumulados ao longo dos hops
            return resp.copy(cookies = accumulatedCookies)
        }

        throw IOException("demasiados redirecionamentos ao acessar $path")
    }

    private fun httpPost(
        path: String,
        body: String,
        initCookies: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
            setRequestProperty("Referer", "$baseUrl/")
            setRequestProperty("Origin", baseUrl)
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Connection", "close")
            setFixedLengthStreamingMode(bodyBytes.size)
            if (initCookies.isNotEmpty()) {
                val cookieStr = initCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                setRequestProperty("Cookie", cookieStr)
            }
            outputStream.write(bodyBytes)
            outputStream.flush()
        }
        return readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): HttpResponse {
        return try {
            val statusCode = conn.responseCode
            val body = (if (statusCode >= 400) conn.errorStream else conn.inputStream)
                ?.bufferedReader()?.readText() ?: ""
            val headers = mutableMapOf<String, String>()
            conn.headerFields.forEach { (k, vs) ->
                if (k != null) headers[k.lowercase()] = vs.joinToString(", ")
            }
            val cookies = parseCookies(conn)
            HttpResponse(statusCode, body, headers, cookies)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseCookies(conn: HttpURLConnection): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val setCookieList = conn.headerFields["Set-Cookie"] ?: return result
        for (cookie in setCookieList) {
            val firstPart = cookie.split(";").first().trim()
            val eqIdx = firstPart.indexOf('=')
            if (eqIdx > 0) {
                result[firstPart.substring(0, eqIdx).trim()] = firstPart.substring(eqIdx + 1).trim()
            }
        }
        return result
    }
}
