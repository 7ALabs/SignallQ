package io.signallq.app.diagnosticooffline

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.signallq.app.core.network.connectivity.AndroidNetworkProbeBinding
import io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine
import io.signallq.app.core.network.connectivity.ConnectivityProbeBinding
import io.signallq.app.core.network.connectivity.DnsProbe
import io.signallq.app.core.network.connectivity.DnsReachabilityProbe
import io.signallq.app.core.network.connectivity.DohFallbackProbe
import io.signallq.app.core.network.connectivity.ExternalIpProbe
import io.signallq.app.core.network.connectivity.ExternalIpReachabilityProbe
import io.signallq.app.core.network.connectivity.GatewayProbe
import io.signallq.app.core.network.connectivity.GatewayReachabilityProbe
import io.signallq.app.core.network.connectivity.HostnameProbe
import io.signallq.app.core.network.connectivity.HostnameReachabilityProbe
import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Contexto de rede resolvido para uma sondagem: o [binding] amarrado à Wi-Fi sob análise mais
 * os dados de [gatewayIp]/[dnsServers] extraídos do `LinkProperties` correspondente — o mesmo
 * subconjunto que [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisRunner] usa
 * para montar `ConnectivityDiagnosisContext`. `null` quando não há rede Wi-Fi capturável agora.
 */
data class ContextoRedeDiagnosticoOffline(
    val binding: ConnectivityProbeBinding,
    val gatewayIp: String?,
    val dnsServers: List<String>,
)

/**
 * Implementação real de [ExecutorEtapaDiagnosticoOffline] (issue #1811, Task 4/4) — conecta o
 * fluxo de apresentação passo a passo (Task 2, `DiagnosticoOfflineViewModel`) às sondagens reais
 * de [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine], incluindo o
 * [DohFallbackProbe] (Task 1) como diferenciador de DNS. Cada chamada devolve o resultado de
 * UMA etapa — progresso real, não simulado — pra UI (Task 3) poder atualizar estado a cada
 * sondagem concluída, em vez de esperar a cadeia inteira terminar.
 *
 * Deliberadamente NÃO reusa
 * [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisRunner] nem
 * `ConnectivityDiagnosisEngine.diagnosticar()`: ambos rodam a cadeia inteira numa única chamada
 * suspend e só devolvem o resultado agregado ao final — não expõem progresso etapa-a-etapa.
 * Reescrever o Runner pra expor isso arriscaria regressão nos dois consumidores de produção
 * (`AppShellMedicaoGuiada`, `ConnectivityBlockingPolicy`), o que o risco Alto da issue e a
 * regra "não alterar comportamento deles" tornam inaceitável. Este executor chama cada
 * sondagem (`GatewayReachabilityProbe`, `DnsReachabilityProbe`/`DohFallbackProbe`,
 * `ExternalIpReachabilityProbe`, `HostnameReachabilityProbe`) diretamente, na mesma ordem e
 * amarradas ao mesmo tipo de binding de rede (`AndroidNetworkProbeBinding`) — mesma
 * infraestrutura de sondagem, wiring novo e paralelo, zero mudança nos dois arquivos
 * existentes.
 *
 * [obterContexto] e as fábricas de sondagem são parâmetros injetáveis (mesmo padrão de
 * `criarEngine` em `ConnectivityDiagnosisRunner`) para permitir teste unitário JVM puro da
 * lógica de mapeamento/diferenciação DoH sem precisar de `android.net.Network` real ou
 * Robolectric — a captura de rede em si (`capturarRedeWifiPadrao`) é fino o bastante pra não
 * precisar de teste dedicado, mesmo padrão do Runner (que também não tem).
 */
class DiagnosticoOfflineExecutorReal(
    context: Context,
    private val criarGatewayProbe: (ConnectivityProbeBinding) -> GatewayProbe = { GatewayReachabilityProbe(it) },
    private val criarDnsProbe: (ConnectivityProbeBinding) -> DnsProbe = { DnsReachabilityProbe(it) },
    private val criarDohProbe: () -> DnsProbe = { DohFallbackProbe() },
    private val criarExternalIpProbe: (ConnectivityProbeBinding) -> ExternalIpProbe = { ExternalIpReachabilityProbe(it) },
    private val criarHostnameProbe: (ConnectivityProbeBinding) -> HostnameProbe = { HostnameReachabilityProbe(it) },
    private val dnsHostnamesParaTeste: List<String> = ConnectivityDiagnosisEngine.DNS_HOSTNAMES_PADRAO,
    private val obterContexto: () -> ContextoRedeDiagnosticoOffline? = { capturarContextoRedeWifiPadrao(context) },
) : ExecutorEtapaDiagnosticoOffline {
    override suspend fun executar(etapa: EtapaDiagnosticoOffline): ResultadoEtapaDiagnosticoOffline =
        withContext(Dispatchers.IO) {
            when (etapa) {
                EtapaDiagnosticoOffline.GATEWAY -> executarGateway()
                EtapaDiagnosticoOffline.DNS -> executarDns()
                EtapaDiagnosticoOffline.ROTA_EXTERNA -> executarRotaExterna()
                EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL -> executarHostname()
            }
        }

    private suspend fun executarGateway(): ResultadoEtapaDiagnosticoOffline {
        val contexto = obterContexto() ?: return falha(EtapaDiagnosticoOffline.GATEWAY, SEM_REDE_WIFI)
        val gatewayIp = contexto.gatewayIp ?: return falha(EtapaDiagnosticoOffline.GATEWAY, "gateway não configurado nesta rede")
        val resultado = criarGatewayProbe(contexto.binding).probe(gatewayIp)
        return mapear(EtapaDiagnosticoOffline.GATEWAY, resultado)
    }

    private suspend fun executarDns(): ResultadoEtapaDiagnosticoOffline {
        val contexto = obterContexto() ?: return falha(EtapaDiagnosticoOffline.DNS, SEM_REDE_WIFI)
        if (contexto.dnsServers.isEmpty()) return falha(EtapaDiagnosticoOffline.DNS, "nenhum servidor DNS configurado nesta rede")

        val resultadoDnsRede = criarDnsProbe(contexto.binding).probe(dnsHostnamesParaTeste)
        if (resultadoDnsRede is ProbeResult.Success) {
            return ResultadoEtapaDiagnosticoOffline.Sucesso(EtapaDiagnosticoOffline.DNS)
        }

        // Diferenciador (Task 1, DohFallbackProbe): DoH público, independente do resolvedor
        // da rede -- distingue "resolvedor da rede quebrado" de "sem rota externa nenhuma".
        val resultadoDoh = criarDohProbe().probe(dnsHostnamesParaTeste)
        val motivo =
            if (resultadoDoh is ProbeResult.Success) {
                "o resolvedor DNS desta rede não respondeu, mas a resolução externa (DoH) funciona " +
                    "-- o problema é o DNS configurado na rede, não a internet em si"
            } else {
                "sem resolução DNS -- nem pelo resolvedor da rede nem por DoH externo"
            }
        return ResultadoEtapaDiagnosticoOffline.Falha(EtapaDiagnosticoOffline.DNS, motivo)
    }

    private suspend fun executarRotaExterna(): ResultadoEtapaDiagnosticoOffline {
        val contexto = obterContexto() ?: return falha(EtapaDiagnosticoOffline.ROTA_EXTERNA, SEM_REDE_WIFI)
        val resultado = criarExternalIpProbe(contexto.binding).probe()
        return mapear(EtapaDiagnosticoOffline.ROTA_EXTERNA, resultado)
    }

    private suspend fun executarHostname(): ResultadoEtapaDiagnosticoOffline {
        val contexto = obterContexto() ?: return falha(EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL, SEM_REDE_WIFI)
        val outcome = criarHostnameProbe(contexto.binding).probe()
        if (outcome.captivePortalSuspeito) {
            return ResultadoEtapaDiagnosticoOffline.Falha(
                EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL,
                "possível portal cativo detectado -- a rede está redirecionando as requisições",
            )
        }
        return mapear(EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL, outcome.result)
    }

    private fun mapear(
        etapa: EtapaDiagnosticoOffline,
        resultado: ProbeResult,
    ): ResultadoEtapaDiagnosticoOffline =
        when (resultado) {
            is ProbeResult.Success -> ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
            is ProbeResult.Failure -> falha(etapa, descreverFalha(resultado.reason))
            is ProbeResult.Timeout -> falha(etapa, "sem resposta dentro do tempo limite")
            is ProbeResult.NotExecuted -> falha(etapa, resultado.reason)
            is ProbeResult.Unavailable -> falha(etapa, resultado.reason)
        }

    private fun descreverFalha(motivo: ProbeFailureReason): String =
        when (motivo) {
            ProbeFailureReason.DNS_RESOLUTION_FAILED -> "falha na resolução DNS"
            ProbeFailureReason.HOST_UNREACHABLE -> "host inalcançável"
            ProbeFailureReason.UNEXPECTED_RESPONSE -> "resposta inesperada"
            ProbeFailureReason.UNKNOWN -> "falha desconhecida"
        }

    private fun falha(
        etapa: EtapaDiagnosticoOffline,
        motivo: String,
    ) =
        ResultadoEtapaDiagnosticoOffline.Falha(etapa, motivo)

    private companion object {
        const val SEM_REDE_WIFI = "sem rede Wi-Fi ativa para diagnosticar"
    }
}

/**
 * Captura real de rede/`LinkProperties` -- mesma estratégia de
 * [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisRunner.capturarRedeWifi]:
 * `allNetworks` explícito (nunca `activeNetwork`, que pode já ter migrado pra dados móveis).
 * Sem teste dedicado (mesmo padrão do Runner) -- é integração fina com `ConnectivityManager`,
 * a lógica de diferenciação testável fica isolada em [DiagnosticoOfflineExecutorReal].
 */
@SuppressLint("MissingPermission")
private fun capturarContextoRedeWifiPadrao(context: Context): ContextoRedeDiagnosticoOffline? {
    val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val redeWifi =
        try {
            connectivityManager.allNetworks.firstOrNull { rede ->
                connectivityManager.getNetworkCapabilities(rede)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        } catch (_: SecurityException) {
            null
        } ?: return null

    val linkProperties = connectivityManager.getLinkProperties(redeWifi)
    val gatewayIp =
        linkProperties
            ?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway != null }
            ?.gateway
            ?.hostAddress
            ?.takeIf { it.isNotBlank() }
    val dnsServers =
        linkProperties
            ?.dnsServers
            ?.mapNotNull { it.hostAddress?.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

    return ContextoRedeDiagnosticoOffline(
        binding = AndroidNetworkProbeBinding(redeWifi),
        gatewayIp = gatewayIp,
        dnsServers = dnsServers,
    )
}
