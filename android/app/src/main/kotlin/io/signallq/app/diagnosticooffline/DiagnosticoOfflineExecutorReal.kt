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
import io.signallq.app.feature.dns.DiagnosticoCoerenciaDns
import io.signallq.app.feature.dns.NivelAlertaCoerenciaDns
import io.signallq.app.feature.dns.OrientadorConfiguracaoDns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
    private val orientadorDns: OrientadorConfiguracaoDns = OrientadorConfiguracaoDns(),
) : ExecutorEtapaDiagnosticoOffline {
    // Ressalva 2 da revisão do Caio na PR #1814 (registrada como "vira bloqueio na PR que
    // plugar o executor real" -- esta é essa PR): o ViewModel converte QUALQUER Throwable vindo
    // daqui em Falha genérica ("sua rede falhou"), sem log nem Crashlytics -- o que esconderia
    // um bug de programação real (NPE, IllegalState) atrás de um diagnóstico de rede plausível.
    // Em vez de mudar o catch do ViewModel (contrato de estado já aprovado na #1814, reabrir
    // ele reabriria aquela revisão), o log entra aqui, no ponto de wiring que introduz a chance
    // real de exceção -- rethrow preserva o comportamento e o contrato já aprovados: o
    // ViewModel continua vendo exatamente a mesma exceção e convertendo do mesmo jeito.
    // Timber.e é o padrão já usado pra erro real em outros pontos do app (ver
    // MainViewModel.kt:2167, "analisarProblema falhou") -- SignallQApplication planta
    // ReleaseTree em build de release, que encaminha pra Firebase Crashlytics.
    override suspend fun executar(etapa: EtapaDiagnosticoOffline): ResultadoEtapaDiagnosticoOffline =
        withContext(Dispatchers.IO) {
            try {
                when (etapa) {
                    EtapaDiagnosticoOffline.GATEWAY -> executarGateway()
                    EtapaDiagnosticoOffline.DNS -> executarDns()
                    EtapaDiagnosticoOffline.ROTA_EXTERNA -> executarRotaExterna()
                    EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL -> executarHostname()
                }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Throwable) {
                Timber.e(erro, "DiagnosticoOfflineExecutorReal: falha inesperada na etapa $etapa")
                throw erro
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
        val dohFuncionou = resultadoDoh is ProbeResult.Success
        val motivo =
            if (dohFuncionou) {
                "o resolvedor DNS desta rede não respondeu, mas a resolução externa (DoH) funciona " +
                    "-- o problema é o DNS configurado na rede, não a internet em si"
            } else {
                "sem resolução DNS -- nem pelo resolvedor da rede nem por DoH externo"
            }
        // Issue #1819: quando o DoH funcionou, já temos evidência real e concreta de que a
        // Cloudflare pública resolve nomes nesta rede (é o provedor que DohFallbackProbe
        // consulta) -- suficiente para pedir a recomendação estruturada real ao
        // OrientadorConfiguracaoDns (sem duplicar a lógica dele: só fornecemos o insumo que já
        // coletamos). `provedorAtivo = null` porque não sabemos o nome do provedor configurado
        // na rede, só os IPs em ContextoRedeDiagnosticoOffline.dnsServers -- mapear IP -> nome
        // de provedor é fora de escopo desta issue (o orientador já lida com `null` tratando
        // como "sem preferência atual", nunca suprime a sugestão por isso). Sem histórico de
        // coerência: este é um diagnóstico avulso, não uma sessão de monitoramento contínuo, daí
        // `NivelAlertaCoerenciaDns.none` -- estado inicial legítimo de `AvaliadorCoerenciaDns`,
        // não um valor inventado.
        // Bloqueio de revisão do Caio na PR #1822: `provedorAtivo = null` desligava a guarda de
        // "não sugerir o que a rede já usa" dentro de `OrientadorConfiguracaoDns.sugerir()` --
        // numa rede já configurada com 1.1.1.1 (ex.: UDP/53 bloqueado, mas DoH sobre 443 passa),
        // o app recomendava trocar para o que a pessoa já tinha. `contexto.dnsServers` já está
        // em escopo aqui (linha acima) -- só faltava usá-lo pra identificar o provedor ativo
        // quando o IP configurado bate com um provedor público conhecido.
        val recomendacao =
            if (dohFuncionou) {
                orientadorDns.sugerir(
                    melhorProvedor = "cloudflare",
                    provedorAtivo = provedorPublicoConhecido(contexto.dnsServers),
                    diagnosticoCoerencia = DiagnosticoCoerenciaDns(NivelAlertaCoerenciaDns.none, 0, 0, 0, 0.0),
                )
            } else {
                null
            }
        return ResultadoEtapaDiagnosticoOffline.Falha(EtapaDiagnosticoOffline.DNS, motivo, recomendacao)
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

// Mesmos pares nome->IP que `OrientadorConfiguracaoDns.mapearProvedor` conhece -- duplicado aqui
// só como tabela de BUSCA REVERSA (IP->nome) porque essa função é privada no orientador. Se a
// lista de provedores dele mudar, esta tabela precisa acompanhar (nenhum teste de caracterização
// cobre a divergência entre as duas hoje -- risco residual pequeno, aceito por ora).
private val IPS_POR_PROVEDOR_PUBLICO: Map<String, String> =
    mapOf(
        "1.1.1.1" to "cloudflare",
        "1.0.0.1" to "cloudflare",
        "8.8.8.8" to "google",
        "8.8.4.4" to "google",
        "9.9.9.9" to "quad9",
        "149.112.112.112" to "quad9",
        "208.67.222.222" to "opendns",
        "208.67.220.220" to "opendns",
        "94.140.14.14" to "adguard",
        "94.140.15.15" to "adguard",
    )

/**
 * Identifica se algum dos servidores DNS configurados na rede é um provedor público conhecido
 * (mesma tabela usada por `OrientadorConfiguracaoDns`). `null` quando nenhum bate -- resolvedor
 * do roteador/ISP, provedor desconhecido, ou IP privado -- e é um `null` legítimo, não ausência
 * de dado: `OrientadorConfiguracaoDns.sugerir()` trata `provedorAtivo = null` como "sem
 * preferência atual conhecida", nunca suprime a sugestão por causa disso.
 */
private fun provedorPublicoConhecido(dnsServers: List<String>): String? =
    dnsServers.firstNotNullOfOrNull { IPS_POR_PROVEDOR_PUBLICO[it] }

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
