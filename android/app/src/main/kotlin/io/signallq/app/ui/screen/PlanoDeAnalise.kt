package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.network.EstadoConexao

// Plano de análise — issue #1706 (2.0.09d), épico #1647.
//
// A spec 2.0 §7 define que o plano é montado a partir de **capacidades, não de telas**, e que ele
// aparece como **uma frase curta**. §8.4 acrescenta a regra que dá sentido a tudo: permissão
// recusada não encerra a jornada — o plano se adapta e **informa o limite**.
//
// ## O plano promete o que o motor entrega, e só isso
//
// A primeira versão desta fatia montou a tabela lendo a coluna "quando pode ser convocada" da spec
// §7. Caio confrontou com o `DiagnosticoGuiadoEngine` (bloqueio B3 da PR #1732) e o resultado foi
// que **3 das 10 capacidades não eram avaliadas por objetivo nenhum** — `CANAIS_WIFI`,
// `DISPOSITIVOS` e `EQUIPAMENTO_INTERNET` — e `SINAL_WIFI` aparecia em dois objetivos que o motor
// não lê. Não era divergência decorativa: era ela que fazia o app pedir permissão de localização
// para "olhar o Wi-Fi" em jornadas onde conceder não muda uma linha do resultado.
//
// A tabela agora espelha as dimensões que cada `avaliar*` de fato produz. As capacidades que a spec
// prevê e o motor ainda não implementa continuam no enum — os ids são contrato de telemetria — mas
// não são prometidas a ninguém até existirem. A convergência está registrada na issue #1733.

/**
 * As 10 capacidades da spec §7.
 *
 * [id] é contrato de telemetria: `diagnostico_plano_iniciado` envia a lista destes ids em
 * `capacidades`. Renomear quebra a série histórica — trate como nome persistido.
 *
 * [trecho] é a metade da frase do plano, escrita para caber depois de "Vamos verificar". Sem
 * jargão, e **sem "e" interno**: a frase junta dois trechos com "e", e trecho que já traz um produz
 * "A e B e C", que lê mal em voz alta (ressalva R8 de Caio na PR #1732).
 */
enum class Capacidade(
    val id: String,
    val trecho: String,
) {
    ESTADO_CONEXAO("estado_conexao", "como sua conexão está agora"),
    LATENCIA_VARIACAO("latencia_variacao", "quanto sua conexão demora para responder"),
    DOWNLOAD_UPLOAD("download_upload", "a velocidade da sua internet"),
    COMPORTAMENTO_SOB_CARGA("comportamento_sob_carga", "como ela se comporta quando fica ocupada"),
    SINAL_WIFI("sinal_wifi", "a força do sinal do Wi-Fi onde você está"),
    CANAIS_WIFI("canais_wifi", "a interferência das redes vizinhas"),
    DNS("dns", "o tempo que os sites levam para começar a carregar"),
    REDE_MOVEL("rede_movel", "o sinal da rede móvel"),
    DISPOSITIVOS("dispositivos", "quem mais está usando sua rede"),
    EQUIPAMENTO_INTERNET("equipamento_internet", "o equipamento que traz a internet até você"),
}

/**
 * O que o app sabe ao montar o plano.
 *
 * Só entram sinais que **mudam o plano**. A spec §8.3 diz "não perguntar algo que não muda motor,
 * recomendação ou confiança", e o mesmo critério vale para o que se consulta.
 */
@Stable
data class ContextoDoPlano(
    val temPermissaoLocalizacao: Boolean,
    /** Previsão do que o motor mede: espelha `snapshotRede.wifiLinkSnapshot != null`, que é a
     *  mesma condição sob a qual `DiagnosticoGuiadoEngine` produz a dimensão de RSSI Wi-Fi. */
    val conectadoPorWifi: Boolean,
    /**
     * Estado real da conexão (`snapshotRede.estadoConexao`). Existe porque `conectadoPorWifi`
     * responde "o motor vai medir o RSSI do Wi-Fi?" — uma pergunta boa para `SINAL_WIFI`, errada
     * para `REDE_MOVEL`. `REDE_MOVEL` só sai do motor quando `connectionType == mobile`
     * (`DiagnosticoGuiadoEngine.avaliarWifiVsOperadora`), e `!conectadoPorWifi` é bem mais largo
     * que isso — cobre também ethernet, desconectado, desconhecido e Wi-Fi com VPN ativa (onde
     * `wifiLinkSnapshot` vem `null` mas `estadoConexao` continua `wifi`). Bloqueio B10 de Caio na
     * PR #1732: o `else` de `WIFI_VS_OPERADORA` prometia rede móvel para quem estava em qualquer
     * um desses estados, atribuindo causa errada ao limite.
     */
    val estadoConexao: EstadoConexao = EstadoConexao.desconhecido,
)

/**
 * Plano montado.
 *
 * [removidasPorPermissao] e [removidasPorRede] são expostas, e não internas ao cálculo, porque
 * **quem pergunta precisa distinguir a causa**. Enquanto o plano só dizia "foi adaptado", o botão
 * de permissão aparecia também quando a redução tinha sido por rede — e ali conceder localização
 * devolvia zero capacidade (bloqueio B1 de Caio na PR #1732).
 */
@Stable
data class PlanoDeAnalise(
    val capacidades: List<Capacidade>,
    val removidasPorPermissao: List<Capacidade>,
    val removidasPorRede: List<Capacidade>,
    val limite: String?,
) {
    val adaptado: Boolean get() = removidasPorPermissao.isNotEmpty() || removidasPorRede.isNotEmpty()

    /** Contrato do evento `diagnostico_plano_iniciado`, propriedade `capacidades`. */
    val idsParaTelemetria: String get() = capacidades.joinToString(",") { it.id }
}

/**
 * As capacidades que cada objetivo convoca, antes de adaptação.
 *
 * Espelha as dimensões que o `DiagnosticoGuiadoEngine` **de fato produz** em cada `avaliar*` — ver
 * o cabeçalho deste arquivo. `ESTADO_CONEXAO` está em todos porque o veredito do motor é, ele
 * mesmo, o estado da conexão: não vira dimensão medida, vira a conclusão.
 */
private fun capacidadesDoObjetivo(
    objetivo: ObjetivoDiagnostico,
    contexto: ContextoDoPlano,
    respostas: List<Int?>,
): List<Capacidade> =
    when (objetivo) {
        ObjetivoDiagnostico.INSTABILIDADE_QUEDAS ->
            listOf(Capacidade.ESTADO_CONEXAO, Capacidade.LATENCIA_VARIACAO)

        ObjetivoDiagnostico.LENTIDAO_GERAL ->
            listOf(Capacidade.ESTADO_CONEXAO, Capacidade.DNS, Capacidade.LATENCIA_VARIACAO, Capacidade.DOWNLOAD_UPLOAD)

        ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.LATENCIA_VARIACAO,
                Capacidade.COMPORTAMENTO_SOB_CARGA,
                Capacidade.DOWNLOAD_UPLOAD,
                // SINAL_WIFI is included when connected to Wi-Fi.
                if (contexto.conectadoPorWifi) Capacidade.SINAL_WIFI else null
            ).filterNotNull()

        ObjetivoDiagnostico.OUTRO_PROBLEMA ->
            listOf(Capacidade.ESTADO_CONEXAO, Capacidade.LATENCIA_VARIACAO, Capacidade.DOWNLOAD_UPLOAD)
    }

/**
 * Capacidades que só existem com permissão de localização concedida.
 *
 * `SINAL_WIFI` **não** está aqui, e a primeira versão desta fatia foi construída inteira sobre a
 * suposição contrária (bloqueio B5 de Caio na PR #1732). O RSSI vem de
 * `MonitorRedeAndroid.capturarWifiLinkSnapshot`, que o lê **incondicionalmente**: o que a
 * localização gateia ali é `ssid`/`bssid`, e nem por permissão — por `locationManager
 * .isLocationEnabled`, o interruptor do sistema, que é outro sinal. O plano usava
 * `checkSelfPermission(ACCESS_FINE_LOCATION)` para prever o comportamento de algo que não olha
 * essa permissão.
 *
 * O efeito era o pior texto da fatia: no Wi-Fi sem permissão, a frase prometia o que o motor não ia
 * medir e o limite negava a única coisa que ele ia.
 *
 * Sobra `CANAIS_WIFI`, que depende de scan de redes vizinhas e aí sim exige localização. Como o
 * motor ainda não a avalia, ela não entra em plano nenhum — e por isso **não há hoje capacidade
 * recuperável por permissão**. É o que tirou a "preparação contextual" desta fatia; ver a issue #1733.
 */
private val DEPENDEM_DE_LOCALIZACAO = setOf(Capacidade.CANAIS_WIFI)

/** Índice da resposta "Cabo de rede" na primeira pergunta do roteiro de jogos. */
private const val RESPOSTA_JOGA_POR_CABO = 1

/** Capacidades que só fazem sentido com o aparelho no Wi-Fi. */
private val DEPENDEM_DE_WIFI =
    setOf(
        Capacidade.SINAL_WIFI,
        Capacidade.CANAIS_WIFI,
        Capacidade.DISPOSITIVOS,
        Capacidade.EQUIPAMENTO_INTERNET,
    )

/**
 * Monta o plano do [objetivo] adaptado ao [contexto].
 *
 * A adaptação **remove** capacidades e diz o que ficou de fora; nunca cancela a análise (§8.4).
 *
 * A ordem importa, e a primeira versão dela estava errada. Eu tinha posto permissão primeiro,
 * argumentando que é "a que a pessoa pode resolver ali mesmo" — mas o meu próprio teste derrubou:
 * em rede móvel, `SINAL_WIFI` falha nos dois critérios, e conceder localização **não devolve nada**,
 * porque o aparelho continua fora do Wi-Fi. O botão aparecia e não restaurava capacidade alguma —
 * o mesmo defeito do bloqueio B1, uma camada abaixo.
 *
 * Rede é avaliada primeiro porque é a restrição que manda: capacidade bloqueada por rede não volta
 * com permissão. Assim `removidasPorPermissao` contém só o que a permissão de fato recupera, e o
 * limite declara a causa que a pessoa precisa resolver.
 */
fun montarPlano(
    objetivo: ObjetivoDiagnostico,
    contexto: ContextoDoPlano,
    /**
     * Respostas já dadas no roteiro, **indexadas por passo, sem buraco**. Mudam o que o motor
     * avalia — ver `JOGOS_COM_LAG`. RESSALVA R9 de Caio na PR #1732: o motor recebe
     * `respostas.filterNotNull()` (`DiagnosticoGuiadoScreen.kt`), que **compacta** os índices; esta
     * função indexa a lista crua por posição (`respostas.getOrNull(0)`). Hoje os dois contratos
     * não divergem porque o botão de avançar exige seleção antes de liberar o próximo passo — não
     * há buraco antes da resposta mais recente. Se uma pergunta virar opcional, os dois lados vão
     * ler índices diferentes em silêncio.
     */
    respostas: List<Int?> = emptyList(),
): PlanoDeAnalise {
    val completo = capacidadesDoObjetivo(objetivo, contexto, respostas)

    val removidasPorRede =
        completo.filter { it in DEPENDEM_DE_WIFI && !contexto.conectadoPorWifi }
    val removidasPorPermissao =
        completo.filter { it in DEPENDEM_DE_LOCALIZACAO && !contexto.temPermissaoLocalizacao } -
            removidasPorRede.toSet()

    val capacidades = completo - removidasPorPermissao.toSet() - removidasPorRede.toSet()

    // `ESTADO_CONEXAO` está em todos os objetivos e em nenhum dos dois conjuntos de dependência,
    // então o plano nunca fica vazio — por construção, não por fallback. A versão anterior tinha
    // aqui um `ifEmpty` que lia como garantia e não garantia nada (ressalva R4 de Caio).
    check(capacidades.isNotEmpty()) {
        "plano vazio para $objetivo — ESTADO_CONEXAO deveria ser inegociável"
    }

    return PlanoDeAnalise(
        capacidades = capacidades,
        removidasPorPermissao = removidasPorPermissao,
        removidasPorRede = removidasPorRede,
        limite = limiteDoPlano(removidasPorPermissao, removidasPorRede),
    )
}

/**
 * A frase que a pessoa lê sobre o que ficou de fora. `null` quando não há nada a declarar.
 *
 * Declara as **duas** causas quando as duas acontecem. A versão anterior era um `when` que
 * retornava só a primeira, e em rede móvel sem permissão uma capacidade saía em silêncio — que é
 * literalmente o que a §8.4 proíbe (bloqueio B4 de Caio na PR #1732).
 *
 * Fala da consequência, não da permissão: "não consigo olhar o Wi-Fi" diz o que ela perde;
 * "permissão de localização negada" diz o que o Android chama aquilo.
 */
private fun limiteDoPlano(
    porPermissao: List<Capacidade>,
    porRede: List<Capacidade>,
): String? {
    val partes = mutableListOf<String>()
    if (porPermissao.isNotEmpty()) {
        partes += "sem acesso às redes próximas eu não consigo olhar o Wi-Fi"
    }
    if (porRede.isNotEmpty()) {
        partes += "você não está no Wi-Fi agora, então parte da sua rede local fica fora"
    }
    if (partes.isEmpty()) return null
    return partes.joinToString("; ").replaceFirstChar { it.uppercase() } + " — sigo com o resto."
}

/** Quantas capacidades a frase do plano nomeia antes de virar checklist. */
private const val MAXIMO_DE_TRECHOS_NA_FRASE = 2

/**
 * O plano como **frase curta** — spec §7, que proíbe checklist técnica por padrão.
 *
 * Nomeia no máximo [MAXIMO_DE_TRECHOS_NA_FRASE] capacidades, que é a forma do próprio exemplo da
 * spec ("a estabilidade da conexão **e** como ela se comporta quando fica ocupada"). As demais
 * continuam no plano e na telemetria; muda só o que a pessoa lê.
 */
fun fraseDoPlano(plano: PlanoDeAnalise): String {
    val trechos = plano.capacidades.take(MAXIMO_DE_TRECHOS_NA_FRASE).map { it.trecho }
    val corpo = if (trechos.size == 1) trechos.first() else trechos.joinToString(" e ")
    return "Vamos verificar $corpo."
}
