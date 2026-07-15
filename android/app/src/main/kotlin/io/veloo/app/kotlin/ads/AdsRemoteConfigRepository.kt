package io.signallq.app.ads

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Le o toggle remoto de anuncios nativos (issue #555) via Firebase Remote Config.
 *
 * Chave mestra `ads_native_enabled` + 4 chaves por tela. Qualquer falha de fetch
 * (rede, timeout, Remote Config nao inicializado) cai no fallback local seguro
 * [AdsFlags.DESLIGADO] -- nunca mostra anuncio por engano, nunca trava a tela
 * esperando a config (regra explicita do plano de implementacao da #555).
 *
 * Recebe [FirebaseRemoteConfig] como `dagger.Lazy` de proposito (nao `kotlin.Lazy` --
 * variancia do tipo Kotlin gera wildcard que o Dagger nao consegue casar com o binding):
 * `FirebaseRemoteConfig.getInstance()` exige `FirebaseApp` ja inicializado, o que nao
 * acontece em testes Robolectric. Como [AdsRemoteConfigRepository] e injetado eagerly
 * via Hilt em [io.signallq.app.ads.AdsFlagsManager] -> `SignallQApplication`, uma
 * chamada direta e nao-lazy no `@Provides` derrubava QUALQUER teste que instanciasse a
 * Application real (nao so os testes de ads) -- so `buscarFlags()` toca `.get()`.
 */
class AdsRemoteConfigRepository(
    private val remoteConfig: Lazy<FirebaseRemoteConfig>,
) {
    suspend fun buscarFlags(): AdsFlags =
        withContext(Dispatchers.IO) {
            val sucesso =
                runCatching {
                    withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                        remoteConfig.get().fetchAndActivate().aguardar()
                    }
                }.onFailure { e -> Timber.w(e, "Falha ao buscar Remote Config de anuncios") }
                    .getOrNull() ?: false

            if (!sucesso) return@withContext AdsFlags.DESLIGADO

            runCatching {
                val rc = remoteConfig.get()
                AdsFlags(
                    masterEnabled = rc.getBoolean(CHAVE_MASTER),
                    velocidade = rc.getBoolean(CHAVE_VELOCIDADE),
                    resultado = rc.getBoolean(CHAVE_RESULTADO),
                    dispositivos = rc.getBoolean(CHAVE_DISPOSITIVOS),
                    historico = rc.getBoolean(CHAVE_HISTORICO),
                )
            }.getOrElse { AdsFlags.DESLIGADO }
        }

    companion object {
        private const val FETCH_TIMEOUT_MS = 8_000L

        const val CHAVE_MASTER = "ads_native_enabled"
        const val CHAVE_VELOCIDADE = "ads_native_velocidade_enabled"
        const val CHAVE_RESULTADO = "ads_native_resultado_enabled"
        const val CHAVE_DISPOSITIVOS = "ads_native_dispositivos_enabled"
        const val CHAVE_HISTORICO = "ads_native_historico_enabled"

        /** Defaults locais do proprio Remote Config -- distintos do fallback de erro:
         *  aqui a config foi lida com sucesso mas as chaves ainda nao existem no console
         *  (primeiro deploy). Mantido desligado ate o Luiz criar as chaves no Firebase. */
        val DEFAULTS_REMOTE_CONFIG: Map<String, Any> =
            mapOf(
                CHAVE_MASTER to false,
                CHAVE_VELOCIDADE to false,
                CHAVE_RESULTADO to false,
                CHAVE_DISPOSITIVOS to false,
                CHAVE_HISTORICO to false,
            )
    }
}

/** Ponte suspend minima para [Task] sem trazer a dependencia inteira de
 *  kotlinx-coroutines-play-services so por causa de uma unica chamada. Mesmo padrao
 *  da implementacao oficial (`resumeWithException`, nunca `cancel` -- cancelamento e
 *  reservado para cancelamento cooperativo real, nao para propagar erro de negocio). */
private suspend fun <T> Task<T>.aguardar(): T =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val erro = task.exception
            if (erro != null) {
                cont.resumeWithException(erro)
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resume(task.result as T)
            }
        }
    }
