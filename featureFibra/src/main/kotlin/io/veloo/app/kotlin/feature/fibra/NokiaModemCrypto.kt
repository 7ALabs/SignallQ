package io.veloo.app.feature.fibra

import android.util.Base64
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Replica exatamente o fluxo crypto_page.js do Nokia G-1425G-B:
// AES-CBC com padding ISO/IEC 7816-4 (0x80 + zeros) + RSA PKCS#1 v1.5.
// Qualquer alteração pode quebrar o login silenciosamente (modem retorna err_t=[0]).
internal object NokiaModemCrypto {

    fun extractPublicKeyBase64(html: String): String? {
        val match = Regex("""var\s+pubkey\s*=\s*'([^']+)'""", RegexOption.MULTILINE)
            .find(html) ?: return null
        val pem = match.groupValues[1]
            .replace("\\\r\n", "").replace("\\\n", "")
        if (!pem.contains("-----BEGIN PUBLIC KEY-----")) return null
        val base64 = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "").replace("\r", "").trim()
        return if (base64.length > 50) base64 else null
    }

    fun extractNonce(html: String): String? =
        Regex("""var\s+nonce\s*=\s*"([^"]+)"""").find(html)?.groupValues?.get(1)

    fun extractCsrfToken(html: String): String? =
        Regex("""var\s+token\s*=\s*"([^"]+)"""").find(html)?.groupValues?.get(1)

    // RSA PKCS#1 v1.5 — compatível com JSEncrypt do firmware.
    // Input: pubkey em SubjectPublicKeyInfo/X.509 DER base64.
    // Output: base64 padrão do resultado cifrado.
    fun rsaEncryptPkcs1(base64Key: String, plaintext: String): String {
        val keyBytes = Base64.decode(base64Key, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(spec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    // AES-CBC com padding ISO/IEC 7816-4: byte 0x80 seguido de zeros até completar bloco de 16.
    // NÃO usa PKCS5/PKCS7 — o SJCL do firmware usa este esquema de padding.
    fun aesCbcEncryptSjcl(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val padLen = 16 - (plaintext.size % 16)
        val padded = ByteArray(plaintext.size + padLen)
        System.arraycopy(plaintext, 0, padded, 0, plaintext.size)
        padded[plaintext.size] = 0x80.toByte()
        // bytes restantes já são zero
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(padded)
    }

    // base64url_escape do crypto_page.js (usado em ck/enckey/enciv): + → -, / → _, = → .
    fun base64UrlEscape(b64: String): String =
        b64.replace('+', '-').replace('/', '_').replace('=', '.')

    // SJCL base64url.fromBits (usado em ct): url-safe sem padding — '=' removido.
    fun base64UrlNoPad(b64: String): String =
        b64.replace('+', '-').replace('/', '_').replace("=", "")

    fun generateSecureBytes(size: Int): ByteArray =
        ByteArray(size).also { SecureRandom().nextBytes(it) }

    fun sha256Base64(s: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
