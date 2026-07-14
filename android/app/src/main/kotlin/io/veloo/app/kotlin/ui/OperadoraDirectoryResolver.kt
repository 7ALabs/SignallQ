package io.signallq.app.ui

import androidx.compose.ui.graphics.Color
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryRepository
import io.signallq.app.feature.diagnostico.remote.RemoteProviderInfo
import javax.inject.Inject
import javax.inject.Singleton

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
)

/** Contato final de uma operadora, ja resolvido pela cadeia de fallback (GH#965). */
data class ResolvedOperadoraContact(
    val displayName: String,
    val sacPhone: String?,
    val whatsapp: String?,
    val site: String?,
    val source: OperadoraSource,
) {
    val hasAnyContact: Boolean get() = sacPhone != null || whatsapp != null || site != null
}

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
    ) {
        suspend fun resolveIdentity(
            ispNomeBruto: String?,
            viaMovel: Boolean = false,
        ): ResolvedOperadoraIdentity {
            val local = resolverLocal(ispNomeBruto, viaMovel)
            if (local != null) {
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

            val remote = buscarRemoto(ispNomeBruto)
            if (remote?.logoUrl != null) {
                return ResolvedOperadoraIdentity(
                    displayName = remote.displayName,
                    monograma = remote.displayName.firstOrNull()?.uppercase() ?: "?",
                    corMarca = null,
                    logoRes = null,
                    logoUrl = remote.logoUrl,
                    source = OperadoraSource.REMOTE,
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
            val local = resolverLocal(ispNomeBruto, viaMovel)
            if (local != null) {
                return ResolvedOperadoraContact(
                    displayName = local.nome,
                    sacPhone = local.sac,
                    whatsapp = local.whatsapp,
                    site = local.site,
                    source = OperadoraSource.LOCAL,
                )
            }

            val remote = buscarRemoto(ispNomeBruto)
            if (remote != null && (remote.sacPhone != null || remote.whatsappUrl != null || remote.websiteUrl != null)) {
                return ResolvedOperadoraContact(
                    displayName = remote.displayName,
                    sacPhone = remote.sacPhone,
                    whatsapp = remote.whatsappUrl,
                    site = remote.websiteUrl,
                    source = OperadoraSource.REMOTE,
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

        /** `null` quando nao ha nome pra buscar OU a chamada remota falhou/nao achou nada. */
        private suspend fun buscarRemoto(ispNomeBruto: String?): RemoteProviderInfo? {
            if (ispNomeBruto.isNullOrBlank()) return null
            return providerDirectoryRepository.searchByName(ispNomeBruto)
        }
    }
