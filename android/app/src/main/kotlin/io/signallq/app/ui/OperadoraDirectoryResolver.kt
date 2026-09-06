package io.signallq.app.ui

import androidx.compose.ui.graphics.Color
import io.signallq.app.core.network.FeatureFlagProvider
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryRepository
import io.signallq.app.feature.diagnostico.remote.RemoteProviderInfo
import javax.inject.Inject
import javax.inject.Singleton

/** Status de curadoria considerado "confirmado" pelo worker `signallq-diagnostic` —
 *  vocabulario fixo definido la (GH#951), nao reinterpretado aqui. */
private const val PROVIDER_STATUS_VERIFIED = "VERIFIED"

/** Sempre habilitado — default do parametro [OperadoraDirectoryResolver.featureFlagProvider]
 *  quando nao ha Hilt (ex.: instanciacao direta em teste), preservando o comportamento
 *  anterior a GH#1464 (diretorio remoto sempre tentado). */
private object AlwaysEnabledFeatureFlagProvider : FeatureFlagProvider {
    override fun isEnabled(key: String): Boolean = true
}

/** De onde veio a identidade/contato resolvido — usado so para telemetria/debug, nunca para logica de UI. */
enum class OperadoraSource { LOCAL, REMOTE, FALLBACK }

/**
 * Identidade visual final de uma operadora, ja resolvida pela cadeia de
 * fallback (GH#965). [logoRes] (drawable bundled) e [logoUrl] (asset remoto)
 * sao mutuamente exclusivos — no maximo um dos dois e nao-nulo.
 */
data class ResolvedOperadoraIdentity(
    val displayName: String,
    val monograma: String,
    val corMarca: Color?,
    val logoRes: Int?,
    val logoUrl: String?,
    val source: OperadoraSource,
    /** GH#1464 (parte de #951) — status de curadoria do registro no worker
     *  `signallq-diagnostic` (`ProviderRecord.status`), so preenchido para
     *  [OperadoraSource.REMOTE]. `null` para LOCAL/FALLBACK (nao ha curadoria remota). */
    val status: String? = null,
)

/** Contato final de uma operadora, ja resolvido pela cadeia de fallback (GH#965). */
data class ResolvedOperadoraContact(
    val displayName: String,
    val sacPhone: String?,
    val whatsapp: String?,
    val site: String?,
    val source: OperadoraSource,
    /** Termo de busca do app na Play Store — so preenchido para [OperadoraSource.LOCAL]
     *  (as ~12 operadoras principais); o diretorio remoto (GH#965) nao tem esse dado,
     *  nunca inventamos um app pra operadora regional desconhecida (GH#970). */
    val grupo: String? = null,
    /** GH#1464 (parte de #951) — ver kdoc de [ResolvedOperadoraIdentity.status]. */
    val status: String? = null,
) {
    val hasAnyContact: Boolean get() = sacPhone != null || whatsapp != null || site != null
}

/** GH#1464 (parte de #951) — indica que o dado veio do diretorio remoto e ainda nao passou
 *  pela curadoria manual (`status != "VERIFIED"`). Usado so pra UI mostrar o rotulo "nao
 *  verificado" e pra nunca abrir contato automaticamente — o dado continua sendo exibido,
 *  so marcado. Nunca `true` para LOCAL/FALLBACK (catalogo local e sempre curado). */
val ResolvedOperadoraIdentity.isNaoVerificado: Boolean
    get() = source == OperadoraSource.REMOTE && status != PROVIDER_STATUS_VERIFIED

/** Ver kdoc de [ResolvedOperadoraIdentity.isNaoVerificado]. */
val ResolvedOperadoraContact.isNaoVerificado: Boolean
    get() = source == OperadoraSource.REMOTE && status != PROVIDER_STATUS_VERIFIED

/**
 * GH#1226 item D — única função de normalização de WhatsApp cru (DDD+número local, sem `55`
 * nem `+`/espaços/hífens/parênteses) pra URL `wa.me` acionável. Antes, [whatsappUrl] e
 * `OutraOperadoraRow` (`OperadoraBottomSheet.kt`) faziam essa conta cada um por conta própria
 * de forma diferente — esta função é a única fonte, usada pelos dois. Filtra tudo que não for
 * dígito primeiro (protege contra número já vindo com `+55`, `(11) 99999-9999` etc. de uma
 * fonte remota futura) e só então decide se precisa prefixar `55`.
 */
fun normalizarWhatsappLocal(numeroCru: String?): String? {
    val digitos = numeroCru?.filter { it.isDigit() }?.takeIf { it.isNotBlank() } ?: return null
    val comCodigoPais = if (digitos.startsWith("55")) digitos else "55$digitos"
    return "https://wa.me/$comCodigoPais"
}

/**
 * Normaliza [ResolvedOperadoraContact.whatsapp] pra URL abrivel — o campo carrega numero
 * cru (DDD+numero, sem `55`/`https://wa.me/`) quando [OperadoraSource.LOCAL] ([BancoOperadoras])
 * e URL completa quando [OperadoraSource.REMOTE] (formato ja devolvido pelo worker
 * `signallq-diagnostic`, ver [io.signallq.app.feature.diagnostico.remote.RemoteProviderInfo.whatsappUrl]).
 */
fun ResolvedOperadoraContact.whatsappUrl(): String? =
    whatsapp?.let { if (source == OperadoraSource.LOCAL) normalizarWhatsappLocal(it) else it }

/**
 * Resolve identidade visual (logo) e contato de uma operadora a partir do nome
 * bruto detectado (ISP fixo ou operadora movel) — GH#965.
 *
 * ## Ordem de resolucao (decisao de produto 2026-07-14, ver issue #965)
 * 1. **Catalogo local** ([BancoOperadoras] / [OperadoraLogoCatalog]) — as ~12
 *    operadoras principais, sempre offline, NUNCA alteradas por esta classe.
 * 2. **Diretorio remoto** ([ProviderDirectoryRepository], worker
 *    `signallq-diagnostic`) — cauda longa (regionais/menores), so tentado
 *    quando (1) nao achou E ha nome pra buscar. Falha de rede/timeout aqui
 *    NUNCA propaga excecao — despenca pro proximo nivel.
 * 3. **Fallback final** — badge generico pra logo (`logoRes`/`logoUrl` nulos,
 *    a Composable de UI decide o visual padrao) e "sem contato disponivel"
 *    pra suporte (`hasAnyContact == false`).
 *
 * Cada metodo publico e independente e NUNCA lanca excecao — pode ser chamado
 * offline sem quebrar a tela.
 */
@Singleton
class OperadoraDirectoryResolver
    @Inject
    constructor(
        private val providerDirectoryRepository: ProviderDirectoryRepository,
        // GH#1464 (parte de #951) — kill switch do nivel 2 (diretorio remoto). Default
        // preserva o comportamento anterior (sempre tentado) pra instanciacao direta em
        // teste; producao recebe a implementacao real via Hilt (mesmo binding usado por
        // DiagnosticDivergenceReporter, ver kdoc de FeatureFlagProvider).
        private val featureFlagProvider: FeatureFlagProvider = AlwaysEnabledFeatureFlagProvider,
    ) {
        /**
         * So o nivel 1 (catalogo local), sincrono — nunca faz I/O. Usado pela camada de
         * UI (Compose) para o caminho rapido, sem flicker: quando ha match local, o
         * resultado esta disponivel na primeira composicao, sem esperar corrotina/rede.
         */
        fun resolveLocalIdentity(
            ispNomeBruto: String?,
            viaMovel: Boolean = false,
        ): ResolvedOperadoraIdentity? {
            val local = resolverLocal(ispNomeBruto, viaMovel) ?: return null
            val visual = OperadoraLogoCatalog.identidadePara(local)
            return ResolvedOperadoraIdentity(
                displayName = local.nome,
                monograma = visual.monograma,
                corMarca = visual.corMarca,
                logoRes = visual.logoRes,
                logoUrl = null,
                source = OperadoraSource.LOCAL,
            )
        }

        /** So o nivel 1 (catalogo local), sincrono — ver kdoc de [resolveLocalIdentity]. */
        fun resolveLocalContact(
            ispNomeBruto: String?,
            viaMovel: Boolean = false,
        ): ResolvedOperadoraContact? {
            val local = resolverLocal(ispNomeBruto, viaMovel) ?: return null
            return ResolvedOperadoraContact(
                displayName = local.nome,
                sacPhone = local.sac,
                whatsapp = local.whatsapp,
                site = local.site,
                source = OperadoraSource.LOCAL,
                grupo = local.grupo,
            )
        }

        suspend fun resolveIdentity(
            ispNomeBruto: String?,
            viaMovel: Boolean = false,
        ): ResolvedOperadoraIdentity {
            val local = resolveLocalIdentity(ispNomeBruto, viaMovel)
            if (local != null) return local

            val remote = buscarRemoto(ispNomeBruto)
            if (remote?.logoUrl != null) {
                return ResolvedOperadoraIdentity(
                    displayName = remote.displayName,
                    monograma = remote.displayName.firstOrNull()?.uppercase() ?: "?",
                    corMarca = null,
                    logoRes = null,
                    logoUrl = remote.logoUrl,
                    source = OperadoraSource.REMOTE,
                    status = remote.status,
                )
            }

            return ResolvedOperadoraIdentity(
                displayName = remote?.displayName ?: ispNomeBruto?.takeIf { it.isNotBlank() } ?: "Operadora",
                monograma = (remote?.displayName ?: ispNomeBruto)?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        }

        suspend fun resolveContact(
            ispNomeBruto: String?,
            viaMovel: Boolean = false,
        ): ResolvedOperadoraContact {
            val local = resolveLocalContact(ispNomeBruto, viaMovel)
            if (local != null) return local

            val remote = buscarRemoto(ispNomeBruto)
            if (remote != null && (remote.sacPhone != null || remote.whatsappUrl != null || remote.websiteUrl != null)) {
                return ResolvedOperadoraContact(
                    displayName = remote.displayName,
                    sacPhone = remote.sacPhone,
                    whatsapp = remote.whatsappUrl,
                    site = remote.websiteUrl,
                    source = OperadoraSource.REMOTE,
                    status = remote.status,
                )
            }

            return ResolvedOperadoraContact(
                displayName = remote?.displayName ?: ispNomeBruto?.takeIf { it.isNotBlank() } ?: "Operadora",
                sacPhone = null,
                whatsapp = null,
                site = null,
                source = OperadoraSource.FALLBACK,
            )
        }

        private fun resolverLocal(
            ispNomeBruto: String?,
            viaMovel: Boolean,
        ): ContatoOperadora? =
            if (viaMovel) BancoOperadoras.resolverMovel(ispNomeBruto) else BancoOperadoras.resolver(ispNomeBruto)

        /** `null` quando nao ha nome pra buscar, a flag `feature_provider_directory_enabled`
         *  (GH#1464) esta desligada, OU a chamada remota falhou/nao achou nada. Com a flag
         *  desligada, o resolver pula direto pro fallback final — nivel 1 (catalogo local)
         *  nunca e afetado, ja resolvido antes deste ponto ser alcancado. */
        private suspend fun buscarRemoto(ispNomeBruto: String?): RemoteProviderInfo? {
            if (ispNomeBruto.isNullOrBlank()) return null
            if (!featureFlagProvider.isProviderDirectoryEnabled()) return null
            return providerDirectoryRepository.searchByName(ispNomeBruto)
        }
    }
