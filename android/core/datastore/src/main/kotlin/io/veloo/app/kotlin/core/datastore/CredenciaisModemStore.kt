package io.signallq.app.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Armazena credenciais do modem (username/password) em EncryptedSharedPreferences.
 * Usa AES-256 GCM via AndroidKeyStore — dados ilegiveis sem o device key.
 *
 * Fallback: se o AndroidKeyStore nao estiver disponivel (ex: testes unitarios com
 * Robolectric), usa SharedPreferences normal. Em device real o KeyStore sempre existe.
 */
class CredenciaisModemStore(private val context: Context) {

    private val prefs: SharedPreferences by lazy { criarPrefs() }

    private fun criarPrefs(): SharedPreferences =
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "signallq_modem_credentials",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            // AndroidKeyStore indisponivel (Robolectric) — fallback sem criptografia.
            context.getSharedPreferences("signallq_modem_credentials_fallback", Context.MODE_PRIVATE)
        }

    private val _usernameFlow = MutableStateFlow(DEFAULT_USERNAME)
    private val _passwordFlow = MutableStateFlow(DEFAULT_PASSWORD)
    private val _bssidVinculadoFlow = MutableStateFlow<String?>(null)

    private var inicializado = false

    val usernameFlow: StateFlow<String> get() {
        garantirInicializado()
        return _usernameFlow
    }

    val passwordFlow: StateFlow<String> get() {
        garantirInicializado()
        return _passwordFlow
    }

    /**
     * BSSID do gateway ao qual a credencial acima foi vinculada via "Manter conectado"
     * (GH#527). Guardado junto da credencial no store criptografado — nunca no DataStore
     * plaintext — porque, a partir da API 26, o Android trata BSSID como dado sensivel
     * de localizacao. Null = nenhum vinculo ativo (autoconexao desligada ou revogada).
     */
    val bssidVinculadoFlow: StateFlow<String?> get() {
        garantirInicializado()
        return _bssidVinculadoFlow
    }

    @Synchronized
    private fun garantirInicializado() {
        if (inicializado) return
        _usernameFlow.value = prefs.getString(CHAVE_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME
        _passwordFlow.value = prefs.getString(CHAVE_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        _bssidVinculadoFlow.value = prefs.getString(CHAVE_BSSID_VINCULADO, null)
        inicializado = true
    }

    fun salvarUsername(username: String) {
        garantirInicializado()
        prefs.edit().putString(CHAVE_USERNAME, username).apply()
        _usernameFlow.value = username
    }

    fun salvarPassword(password: String) {
        garantirInicializado()
        prefs.edit().putString(CHAVE_PASSWORD, password).apply()
        _passwordFlow.value = password
    }

    /** Vincula (ou revoga, com null) o BSSID do gateway a credencial salva. */
    fun salvarBssidVinculado(bssid: String?) {
        garantirInicializado()
        val editor = prefs.edit()
        if (bssid != null) editor.putString(CHAVE_BSSID_VINCULADO, bssid) else editor.remove(CHAVE_BSSID_VINCULADO)
        editor.apply()
        _bssidVinculadoFlow.value = bssid
    }

    /**
     * Migra credenciais plaintext do DataStore para o store criptografado.
     * Chamado uma vez pelo PreferenciasAppRepository na inicializacao.
     * Retorna true se houve migracao (para que o caller remova as chaves do DataStore).
     */
    fun migrarSeNecessario(usernamePlaintext: String?, passwordPlaintext: String?): Boolean {
        if (prefs.getBoolean(CHAVE_MIGRADO, false)) return false

        val temDadosParaMigrar = !usernamePlaintext.isNullOrBlank() || !passwordPlaintext.isNullOrBlank()
        if (temDadosParaMigrar) {
            val editor = prefs.edit()
            if (!usernamePlaintext.isNullOrBlank()) {
                editor.putString(CHAVE_USERNAME, usernamePlaintext)
                _usernameFlow.value = usernamePlaintext
            }
            if (!passwordPlaintext.isNullOrBlank()) {
                editor.putString(CHAVE_PASSWORD, passwordPlaintext)
                _passwordFlow.value = passwordPlaintext
            }
            editor.putBoolean(CHAVE_MIGRADO, true)
            editor.apply()
            inicializado = true
            return true
        }

        prefs.edit().putBoolean(CHAVE_MIGRADO, true).apply()
        return false
    }

    companion object {
        private const val CHAVE_USERNAME = "modem_username"
        private const val CHAVE_PASSWORD = "modem_password"
        private const val CHAVE_BSSID_VINCULADO = "gateway_bssid_vinculado"
        private const val CHAVE_MIGRADO = "migrado_do_datastore"
        const val DEFAULT_USERNAME = "userAdmin"
        const val DEFAULT_PASSWORD = ""
    }
}
