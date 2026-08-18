package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico

// Plano de análise — issue #1706 (2.0.09d), épico #1647.
//
// A spec 2.0 §7 define que o plano é montado a partir de **capacidades, não de telas**, e que ele
// aparece como **uma frase curta** — "Vamos verificar a estabilidade da conexão e como ela se
// comporta quando fica ocupada". A spec proíbe explicitamente checklist técnica.
//
// §8.4 acrescenta a regra que dá sentido a tudo isto: **permissão recusada não encerra a jornada**.
// O plano se adapta e **informa o limite**.
//
// ## Não é motor de decisão novo
//
// A issue proíbe criar motor paralelo, e não há um aqui: quem decide o que a análise mede continua
// sendo `DiagnosticoGuiadoEngine` com os dados que o executor produz. "Capacidade" é vocabulário de
// **apresentação** — serve para dizer à pessoa o que vai ser verificado e o que ficou de fora. Se
// esta tabela e o motor divergirem, quem está errado é esta tabela.

/**
 * As 10 capacidades da spec §7.
 *
 * [id] é contrato de telemetria: o evento `diagnostico_plano_iniciado` envia a lista destes ids em
 * `capacidades`. Renomear um id quebra a série histórica — trate como nome persistido.
 *
 * [trecho] é a metade da frase do plano, escrita para caber depois de "Vamos verificar". Sem
 * jargão: a pessoa lê "o tempo de resposta e a variação dele", não "latência e jitter".
 */
enum class Capacidade(
    val id: String,
    val trecho: String,
) {
    ESTADO_CONEXAO("estado_conexao", "como sua conexão está agora"),
    LATENCIA_VARIACAO("latencia_variacao", "o tempo de resposta e a variação dele"),
    DOWNLOAD_UPLOAD("download_upload", "a velocidade de recebimento e de envio"),
    COMPORTAMENTO_SOB_CARGA("comportamento_sob_carga", "como ela se comporta quando fica ocupada"),
    SINAL_WIFI("sinal_wifi", "a força do sinal do Wi-Fi onde você está"),
    CANAIS_WIFI("canais_wifi", "a interferência das redes vizinhas"),
    DNS("dns", "o tempo que os sites levam para começar a carregar"),
    REDE_MOVEL("rede_movel", "o sinal da rede móvel"),
    DISPOSITIVOS("dispositivos", "quem mais está usando sua rede"),
    EQUIPAMENTO_INTERNET("equipamento_internet", "o equipamento que traz a internet até você"),
}

/**
 * O que o app sabe no momento de montar o plano.
 *
 * Só entram aqui sinais que **mudam o plano**. A spec §8.3 é explícita — "não perguntar algo que
 * não muda motor, recomendação ou confiança" — e o mesmo critério vale para o que se consulta.
 */
@Stable
data class ContextoDoPlano(
    val temPermissaoLocalizacao: Boolean,
    val conectadoPorWifi: Boolean,
    /**
     * O sistema nao vai mais mostrar o dialogo de permissao (usuario marcou "nao perguntar de
     * novo"). Distingue `NEGADO` de `NEGADO_PERMANENTE` na telemetria, e decide se ainda faz
     * sentido oferecer o botao de permitir — pedir de novo o que o sistema nao vai perguntar e
     * um toque que nao faz nada.
     */
    val localizacaoBloqueadaPermanentemente: Boolean = false,
)

/**
 * Plano montado. [adaptado] é `true` quando alguma capacidade saiu por permissão ou contexto.
 *
 * [limite] é o que a pessoa lê quando isso acontece — nunca `null` com [adaptado] verdadeiro, e
 * nunca preenchido quando o plano saiu completo. É a metade de §8.4 que costuma ser esquecida:
 * adaptar sem dizer é falhar em silêncio.
 */
@Stable
data class PlanoDeAnalise(
    val capacidades: List<Capacidade>,
    val adaptado: Boolean,
    val limite: String?,
) {
    /** Contrato do evento `diagnostico_plano_iniciado`, propriedade `capacidades`. */
    val idsParaTelemetria: String get() = capacidades.joinToString(",") { it.id }
}

/**
 * As capacidades que cada objetivo convoca, antes de qualquer adaptação.
 *
 * Vem da coluna "quando pode ser convocada" da tabela da spec §7. `ESTADO_CONEXAO` está em todos
 * porque a spec diz "sempre".
 */
private fun capacidadesDoObjetivo(objetivo: ObjetivoDiagnostico): List<Capacidade> =
    when (objetivo) {
        ObjetivoDiagnostico.INTERNET_CAI_OSCILA ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.LATENCIA_VARIACAO,
                Capacidade.SINAL_WIFI,
                Capacidade.EQUIPAMENTO_INTERNET,
            )

        ObjetivoDiagnostico.VIDEOS_TRAVAM ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.DOWNLOAD_UPLOAD,
                Capacidade.COMPORTAMENTO_SOB_CARGA,
                Capacidade.LATENCIA_VARIACAO,
            )

        ObjetivoDiagnostico.JOGOS_COM_LAG ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.LATENCIA_VARIACAO,
                Capacidade.COMPORTAMENTO_SOB_CARGA,
                Capacidade.SINAL_WIFI,
            )

        ObjetivoDiagnostico.CHAMADAS_CONGELAM ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.LATENCIA_VARIACAO,
                Capacidade.DOWNLOAD_UPLOAD,
                Capacidade.SINAL_WIFI,
            )

        ObjetivoDiagnostico.SITES_DEMORAM ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.DNS,
                Capacidade.LATENCIA_VARIACAO,
            )

        ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.DOWNLOAD_UPLOAD,
                Capacidade.DISPOSITIVOS,
                Capacidade.EQUIPAMENTO_INTERNET,
            )

        ObjetivoDiagnostico.WIFI_VS_OPERADORA ->
            listOf(
                Capacidade.ESTADO_CONEXAO,
                Capacidade.SINAL_WIFI,
                Capacidade.CANAIS_WIFI,
                Capacidade.REDE_MOVEL,
            )
    }

/** Capacidades que só existem com permissão de localização concedida. */
private val DEPENDEM_DE_LOCALIZACAO =
    setOf(Capacidade.SINAL_WIFI, Capacidade.CANAIS_WIFI)

/** Capacidades que só fazem sentido com o aparelho no Wi-Fi. */
private val DEPENDEM_DE_WIFI =
    setOf(Capacidade.SINAL_WIFI, Capacidade.CANAIS_WIFI, Capacidade.DISPOSITIVOS, Capacidade.EQUIPAMENTO_INTERNET)

/**
 * Monta o plano do [objetivo] adaptado ao [contexto].
 *
 * A adaptação **remove** capacidades e diz o que ficou de fora; nunca cancela a análise. §8.4:
 * "permissão recusada não encerra a jornada — o plano se adapta e informa o limite".
 *
 * `ESTADO_CONEXAO` nunca sai: a spec a marca como "sempre", e um plano vazio não é plano.
 */
fun montarPlano(
    objetivo: ObjetivoDiagnostico,
    contexto: ContextoDoPlano,
): PlanoDeAnalise {
    val completo = capacidadesDoObjetivo(objetivo)

    val removidasPorPermissao =
        completo.filter { it in DEPENDEM_DE_LOCALIZACAO && !contexto.temPermissaoLocalizacao }
    val removidasPorRede =
        completo.filter { it in DEPENDEM_DE_WIFI && !contexto.conectadoPorWifi } - removidasPorPermissao.toSet()

    val restantes = completo - removidasPorPermissao.toSet() - removidasPorRede.toSet()
    val capacidades = restantes.ifEmpty { listOf(Capacidade.ESTADO_CONEXAO) }

    return PlanoDeAnalise(
        capacidades = capacidades,
        adaptado = capacidades.size < completo.size,
        limite = limiteDoPlano(removidasPorPermissao, removidasPorRede),
    )
}

/**
 * A frase que a pessoa lê. `null` quando não há nada a declarar.
 *
 * Fala da **consequência**, não da permissão: "não vou conseguir olhar o Wi-Fi" diz o que ela perde;
 * "permissão de localização negada" diz o que o Android chama aquilo. O mesmo critério de linguagem
 * que `ResultadoIndisponivelScreen` e `etapaEmLinguagemHumana` já aplicam.
 */
private fun limiteDoPlano(
    porPermissao: List<Capacidade>,
    porRede: List<Capacidade>,
): String? =
    when {
        porPermissao.isNotEmpty() ->
            "Sem acesso às redes próximas eu não consigo olhar o Wi-Fi — sigo com o resto."
        porRede.isNotEmpty() ->
            "Você não está no Wi-Fi agora, então vou olhar só o que dá pela rede móvel."
        else -> null
    }

/** Quantas capacidades a frase do plano nomeia antes de virar checklist. */
private const val MAXIMO_DE_TRECHOS_NA_FRASE = 2

/**
 * O plano como **frase curta** — spec §7, que proíbe checklist técnica por padrão.
 *
 * Nomeia no máximo [MAXIMO_DE_TRECHOS_NA_FRASE] capacidades. Um plano de 4 vira "Vamos verificar A
 * e B" e não "Vamos verificar A, B, C e D" — a segunda forma é a checklist que a spec recusa, mesmo
 * escrita em prosa. As capacidades que não entram na frase continuam no plano e na telemetria; o
 * que muda é só o que a pessoa lê.
 */
fun fraseDoPlano(plano: PlanoDeAnalise): String {
    val trechos = plano.capacidades.take(MAXIMO_DE_TRECHOS_NA_FRASE).map { it.trecho }
    val corpo =
        when (trechos.size) {
            0 -> Capacidade.ESTADO_CONEXAO.trecho
            1 -> trechos.first()
            else -> trechos.dropLast(1).joinToString(", ") + " e " + trechos.last()
        }
    return "Vamos verificar $corpo."
}
