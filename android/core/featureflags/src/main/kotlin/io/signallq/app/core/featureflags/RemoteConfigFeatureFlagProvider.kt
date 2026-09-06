package io.signallq.app.core.featureflags

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementacao real de [FeatureFlagProvider] sobre o Firebase Remote Config.
 *
 * Recebe [FirebaseRemoteConfig] via [remoteConfigProvider] (`() -> FirebaseRemoteConfig`,
 * nao a instancia direto) de proposito -- mesmo motivo documentado em
 * `io.signallq.app.ads.AdsRemoteConfigRepository` (que usa `dagger.Lazy` para o
 * mesmo fim): `FirebaseRemoteConfig.getInstance()` exige `FirebaseApp` ja
 * inicializado, o que nao acontece em testes Robolectric, e este provider e
 * injetado eagerly via Hilt em `SignallQApplication`. Aqui usamos uma lambda
 * simples em vez de `dagger.Lazy` para nao trazer Dagger como dependencia deste
 * modulo `:core:*` -- quem monta a lambda (`AppModule`, em `:app`) e quem ja tem
 * Hilt/Dagger no classpath.
 *
 * [catalog] fornece os defaults locais -- semeiam [valoresAtuais] antes de qualquer
 * fetch, entao [isEnabled]/[observe] sempre tem um valor utilizavel mesmo offline
 * (criterio de aceite da issue #1477).
 */
class RemoteConfigFeatureFlagProvider(
    private val remoteConfigProvider: () -> FirebaseRemoteConfig,
    private val catalog: FeatureFlagCatalog,
    private val fetchTimeoutMillis: Long = DEFAULT_FETCH_TIMEOUT_MS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeatureFlagProvider {
    private val valoresAtuais = MutableStateFlow(catalog.defaultsAsValues())

    override fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue> =
        valoresAtuais
            .map { valores -> valores[key] ?: catalog.defaultValueFor(key) ?: valorDesconhecido(key) }
            .distinctUntilChanged()

    override fun isEnabled(key: FeatureFlagKey): Boolean =
        (valoresAtuais.value[key] ?: catalog.defaultValueFor(key))
            ?.raw
            ?.asBooleanOrNull()
            ?: false

    override suspend fun refresh(force: Boolean): FeatureFlagRefreshResult =
        withContext(dispatcher) {
            val rc =
                runCatching { remoteConfigProvider() }
                    .getOrElse { erro ->
                        Timber.w(erro, "Falha ao obter instancia de Remote Config para feature flags")
                        return@withContext FeatureFlagRefreshResult.Failure(FeatureFlagRefreshFailureReason.UNKNOWN, erro)
                    }

            val resultado = executarFetch(rc, force)

            // Le os valores ATIVOS independente do resultado do fetch acima -- preserva a
            // ultima configuracao valida automaticamente (o SDK sempre retorna o ultimo
            // template ativado com sucesso, mesmo se o fetch mais recente falhou/expirou).
            runCatching { lerValoresAtivos(rc) }
                .onSuccess { valoresAtuais.value = it }
                .onFailure { erro ->
                    Timber.w(erro, "Falha ao ler valores ativos de feature flags -- mantendo estado anterior em memoria")
                }

            resultado
        }

    private suspend fun executarFetch(
        rc: FirebaseRemoteConfig,
        force: Boolean,
    ): FeatureFlagRefreshResult {
        val fetchResult =
            runCatching {
                withTimeoutOrNull(fetchTimeoutMillis) {
                    if (force) {
                        rc.fetch(0).aguardar()
                        rc.activate().aguardar()
                    } else {
                        rc.fetchAndActivate().aguardar()
                    }
                }
            }

        val activated = fetchResult.getOrNull()
        return when {
            fetchResult.isFailure ->
                FeatureFlagRefreshResult.Failure(
                    reason = classificarFalha(fetchResult.exceptionOrNull()),
                    cause = fetchResult.exceptionOrNull(),
                )
            activated == null ->
                FeatureFlagRefreshResult.Failure(reason = FeatureFlagRefreshFailureReason.TIMEOUT, cause = null)
            else ->
                FeatureFlagRefreshResult.Success(
                    activated = activated,
                    fetchTimeMillis = runCatching { rc.info.fetchTimeMillis }.getOrNull(),
                )
        }
    }

    private fun classificarFalha(erro: Throwable?): FeatureFlagRefreshFailureReason =
        when {
            erro == null -> FeatureFlagRefreshFailureReason.UNKNOWN
            erro.javaClass.simpleName.contains("Throttled", ignoreCase = true) -> FeatureFlagRefreshFailureReason.THROTTLED
            else -> FeatureFlagRefreshFailureReason.NETWORK_ERROR
        }

    private fun lerValoresAtivos(rc: FirebaseRemoteConfig): Map<FeatureFlagKey, FeatureFlagValue> =
        catalog.definitions.associate { definicao ->
            val valorRemoto = rc.getValue(definicao.key.id)
            definicao.key to
                FeatureFlagValue(
                    key = definicao.key,
                    raw = lerRawValue(rc, definicao),
                    source = mapearOrigem(valorRemoto.source),
                )
        }

    private fun lerRawValue(
        rc: FirebaseRemoteConfig,
        definicao: FeatureFlagDefinition,
    ): FeatureFlagRawValue =
        when (definicao.type) {
            FeatureFlagType.BOOLEAN -> FeatureFlagRawValue.BooleanValue(rc.getBoolean(definicao.key.id))
            FeatureFlagType.STRING -> FeatureFlagRawValue.StringValue(rc.getString(definicao.key.id))
            FeatureFlagType.LONG -> FeatureFlagRawValue.LongValue(rc.getLong(definicao.key.id))
            FeatureFlagType.DOUBLE -> FeatureFlagRawValue.DoubleValue(rc.getDouble(definicao.key.id))
        }

    private fun mapearOrigem(source: Int): FeatureFlagSource =
        when (source) {
            FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> FeatureFlagSource.DEFAULT
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> FeatureFlagSource.REMOTE
            else -> FeatureFlagSource.STATIC
        }

    private fun valorDesconhecido(key: FeatureFlagKey): FeatureFlagValue =
        FeatureFlagValue(key = key, raw = FeatureFlagRawValue.BooleanValue(false), source = FeatureFlagSource.STATIC)

    companion object {
        private const val DEFAULT_FETCH_TIMEOUT_MS = 8_000L
    }
}

/** Ponte suspend minima para [Task] -- mesmo padrao de
 *  `io.signallq.app.ads.AdsRemoteConfigRepository.aguardar` (duplicado de proposito:
 *  este modulo `:core:featureflags` nao pode depender de `:app`). */
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
