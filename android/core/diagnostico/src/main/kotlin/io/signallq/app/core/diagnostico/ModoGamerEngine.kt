package io.signallq.app.core.diagnostico

/**
 * Modo gamer — Feature #550, issue #1476. Terceira e última entrega da feature (depois de
 * #1474 protótipo e #1475 motor por objetivo). Reaproveita a MESMA infraestrutura de
 * [DiagnosticoGuiadoEngine] ([Dimensao]/[MensagensStatus]/[construirResultadoBase]/
 * [EvidenciaDiagnostico]/[DiagnosticStatus]) — nenhum vocabulário de status novo, nenhum
 * "pior faixa vence" reimplementado. [DiagnosticoGuiadoEngine.avaliarJogosComLag] (objetivo
 * "Jogos com lag ou ping alto") já mede latência+jitter+perda com [MetricClassifier]; este
 * motor generaliza a mesma ideia por [CategoriaJogoModoGamer], sem re-testar a rede — o
 * `input` é o mesmo [DiagnosticInput] do teste de velocidade já rodado (fora de escopo desta
 * issue: medição oficial de ping por servidor de jogo).
 *
 * Não reaproveita [GameReadinessClassifier] diretamente: aquele objeto tem vocabulário
 * PRÓPRIO ([GameReadinessClassifier.ReadinessStatus], "não reutilizar em outro contexto",
 * ver seu próprio kdoc) e hoje só alimenta [DiagnosticReport] (nenhuma tela consome), então
 * misturar os dois exigiria vazar `ReadinessStatus` pra dentro do container visual
 * "Medido pelo motor SignallQ" (`AiVsMotorExplainer`, que espera [EvidenciaDiagnostico]/
 * [DiagnosticStatus]) — na prática recriaria o mesmo motor com um verniz de adapter.
 */
object ModoGamerEngine {

    fun avaliar(
        categoria: CategoriaJogoModoGamer,
        device: DeviceJogo,
        input: DiagnosticInput?,
    ): ResultadoModoGamer {
        val avaliador =
            when (categoria) {
                CategoriaJogoModoGamer.FPS_COMPETITIVO -> ::avaliarFpsCompetitivo
                CategoriaJogoModoGamer.BATTLE_ROYALE -> ::avaliarBattleRoyale
                CategoriaJogoModoGamer.MOBA -> ::avaliarMoba
                CategoriaJogoModoGamer.CASUAL -> ::avaliarCasual
                CategoriaJogoModoGamer.CLOUD_GAMING -> ::avaliarCloudGaming
                CategoriaJogoModoGamer.OUTRO -> ::avaliarOutro
            }
        return avaliador(input, device)
    }

    // ── FPS competitivo (Valorant, CODM, EA FC) ─────────────────────────────
    // Mesmas 3 dimensões de DiagnosticoGuiadoEngine.avaliarJogosComLag (latência + jitter +
    // perda) — jogo de precisão em tempo real, mesma prioridade de métrica.
    private fun avaliarFpsCompetitivo(input: DiagnosticInput?, device: DeviceJogo): ResultadoModoGamer {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.latencyMs?.let { dims += Dimensao("Latência sob carga", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it)) }
        internet?.jitterMs?.let { dims += Dimensao("Jitter", "%.0f ms".format(it), MetricClassifier.classificarJitter(it)) }
        internet?.perdaPercentual?.let { dims += Dimensao("Perda estimada", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it)) }
        return montarResultadoModoGamer(
            categoria = CategoriaJogoModoGamer.FPS_COMPETITIVO,
            device = device,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Latência, jitter e perda dentro da faixa recomendada para FPS competitivo.",
                atencao = "Latência ou jitter no limite — pode haver atraso perceptível na mira/registro de tiros.",
                critica = "Latência sob carga está prejudicando suas partidas — faixa crítica para FPS competitivo.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) emptyList() else listOf("Jogue com cabo de rede em vez de Wi-Fi", "Ative priorização de jogos (QoS) no roteador, se disponível")
            },
        )
    }

    // ── Battle royale (Free Fire, Fortnite) ─────────────────────────────────
    // Lobby grande + assets carregados em tempo real: além de latência/jitter, download
    // entra como 3ª dimensão (diferente de FPS_COMPETITIVO, que prioriza perda).
    private fun avaliarBattleRoyale(input: DiagnosticInput?, device: DeviceJogo): ResultadoModoGamer {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.latencyMs?.let { dims += Dimensao("Latência sob carga", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it)) }
        internet?.jitterMs?.let { dims += Dimensao("Jitter", "%.0f ms".format(it), MetricClassifier.classificarJitter(it)) }
        internet?.downloadMbps?.let { dims += Dimensao("Download", "%.1f Mbps".format(it), MetricClassifier.classificarDownload(it)) }
        return montarResultadoModoGamer(
            categoria = CategoriaJogoModoGamer.BATTLE_ROYALE,
            device = device,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Latência, jitter e download dentro do esperado para battle royale.",
                atencao = "Latência, jitter ou download no limite — pode haver atraso ao entrar na partida ou durante o combate.",
                critica = "Latência ou download comprometidos — isso costuma atrasar o carregamento da partida e travar o combate.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) emptyList() else listOf("Jogue com cabo de rede em vez de Wi-Fi", "Feche downloads em segundo plano antes de entrar na partida")
            },
        )
    }

    // ── MOBA (League of Legends) ────────────────────────────────────────────
    // Partidas longas, sensíveis a oscilação constante mais do que a picos isolados —
    // jitter entra primeiro na leitura, perda continua relevante (teamfight decide em ms).
    private fun avaliarMoba(input: DiagnosticInput?, device: DeviceJogo): ResultadoModoGamer {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.jitterMs?.let { dims += Dimensao("Jitter", "%.0f ms".format(it), MetricClassifier.classificarJitter(it)) }
        internet?.latencyMs?.let { dims += Dimensao("Latência sob carga", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it)) }
        internet?.perdaPercentual?.let { dims += Dimensao("Perda estimada", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it)) }
        return montarResultadoModoGamer(
            categoria = CategoriaJogoModoGamer.MOBA,
            device = device,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Jitter, latência e perda dentro do esperado para MOBA.",
                atencao = "Oscilação ou latência no limite — pode haver atraso perceptível em teamfights.",
                critica = "Oscilação ou latência prejudicando a partida — faixa crítica para MOBA competitivo.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) emptyList() else listOf("Priorize a rede 5GHz perto do roteador", "Evite downloads/uploads simultâneos durante a partida")
            },
        )
    }

    // ── Jogo casual ou mobile (Minecraft, Roblox, Genshin Impact) ───────────
    // Menos sensível a precisão de tiro em tempo real — download (mundo/assets) e latência
    // básica bastam; sem exigir jitter/perda tão estritos quanto os perfis competitivos.
    private fun avaliarCasual(input: DiagnosticInput?, device: DeviceJogo): ResultadoModoGamer {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.downloadMbps?.let { dims += Dimensao("Download", "%.1f Mbps".format(it), MetricClassifier.classificarDownload(it)) }
        internet?.latencyMs?.let { dims += Dimensao("Latência", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it)) }
        return montarResultadoModoGamer(
            categoria = CategoriaJogoModoGamer.CASUAL,
            device = device,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Download e latência dentro do esperado para este tipo de jogo.",
                atencao = "Download ou latência no limite — pode haver demora ao carregar conteúdo ou pequenos atrasos.",
                critica = "Download ou latência comprometidos — isso costuma travar o carregamento do jogo.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) emptyList() else listOf("Feche downloads/uploads em segundo plano", "Aproxime-se do roteador ou use a rede 5GHz")
            },
        )
    }

    // ── Cloud gaming (TV/Cloud gaming) ──────────────────────────────────────
    // Vídeo em tempo real: download alto + baixo atraso sob carga (bufferbloat), mesmas 2
    // dimensões de DiagnosticoGuiadoEngine.avaliarVideosTravam, mais latência.
    private fun avaliarCloudGaming(input: DiagnosticInput?, device: DeviceJogo): ResultadoModoGamer {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.downloadMbps?.let { dims += Dimensao("Download", "%.1f Mbps".format(it), MetricClassifier.classificarDownload(it)) }
        internet?.bufferbloatMs?.let { dims += Dimensao("Atraso sob carga", "%.0f ms".format(it), MetricClassifier.classificarBufferbloat(it)) }
        internet?.latencyMs?.let { dims += Dimensao("Latência", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it)) }
        return montarResultadoModoGamer(
            categoria = CategoriaJogoModoGamer.CLOUD_GAMING,
            device = device,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Download e atraso sob carga dentro do esperado — a rede aguenta streaming de jogo em boa qualidade.",
                atencao = "Download ou atraso sob carga no limite — pode haver perda de qualidade de imagem ou engasgos.",
                critica = "Download baixo com atraso alto sob carga — isso costuma travar o stream do jogo.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) emptyList() else listOf("Use a rede 5GHz/6GHz perto do roteador, evite a faixa 2.4GHz", "Pause downloads em segundo plano durante a sessão")
            },
        )
    }

    // ── Outro tipo de jogo (fallback quando o jogo não está no catálogo) ────
    // Mesmas 3 dimensões de FPS_COMPETITIVO — é a régua "genérica" citada na mensagem de
    // fallback (nunca inventa um perfil mais permissivo só por não conhecer o jogo).
    private fun avaliarOutro(input: DiagnosticInput?, device: DeviceJogo): ResultadoModoGamer {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.latencyMs?.let { dims += Dimensao("Latência sob carga", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it)) }
        internet?.jitterMs?.let { dims += Dimensao("Jitter", "%.0f ms".format(it), MetricClassifier.classificarJitter(it)) }
        internet?.perdaPercentual?.let { dims += Dimensao("Perda estimada", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it)) }
        return montarResultadoModoGamer(
            categoria = CategoriaJogoModoGamer.OUTRO,
            device = device,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Latência, jitter e perda dentro do esperado para jogos online em geral.",
                atencao = "Latência ou jitter no limite — pode haver atraso perceptível em momentos de disputa.",
                critica = "Latência ou jitter prejudicando a partida — faixa crítica para jogos online.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) emptyList() else listOf("Jogue com cabo de rede em vez de Wi-Fi, quando possível", "Evite downloads/uploads simultâneos durante a partida")
            },
        )
    }

    // ── Compartilhado ────────────────────────────────────────────────────────

    private fun montarResultadoModoGamer(
        categoria: CategoriaJogoModoGamer,
        device: DeviceJogo,
        dims: List<Dimensao>,
        mensagens: MensagensStatus,
        acoes: (DiagnosticStatus) -> List<String>,
    ): ResultadoModoGamer {
        val base = construirResultadoBase(dims, mensagens)
        // Linha informativa de contexto (device testado) — sempre presente, mesmo sem
        // métricas (dadosInsuficientes), pra deixar claro em qual aparelho o usuário está
        // avaliando. Nunca influencia severidade/status (não é Dimensao).
        val evidenciaDevice = EvidenciaDiagnostico("Conexão do teste", device.label, MetricStatus.inconclusivo)
        return ResultadoModoGamer(
            categoria = categoria,
            device = device,
            status = base.status,
            mensagemMotor = base.mensagem,
            evidencias = base.evidencias + evidenciaDevice,
            // Sem dados suficientes nunca sugere ação — mesmo princípio de
            // DiagnosticoGuiadoEngine.montarResultado.
            acoes = if (base.dadosInsuficientes) emptyList() else acoes(base.status),
            dadosInsuficientes = base.dadosInsuficientes,
        )
    }
}

/**
 * As 6 categorias de jogo do Modo gamer — mesmas do protótipo #1474
 * (`diagnostico-guiado.jsx`, `CATEGORIAS_GENERICAS`, issue #1483). [OUTRO] é o fallback
 * quando o jogo específico não está em [CatalogoJogosModoGamer] — nunca é erro, sempre cai
 * numa categoria com thresholds reais (regra de produto da issue #1476).
 */
enum class CategoriaJogoModoGamer(val label: String) {
    FPS_COMPETITIVO("FPS competitivo"),
    BATTLE_ROYALE("Battle royale"),
    MOBA("MOBA"),
    CASUAL("Jogo casual ou mobile"),
    CLOUD_GAMING("Streaming em nuvem (cloud gaming)"),
    OUTRO("Outro tipo de jogo"),
}

/** Os 7 aparelhos do Modo gamer — mesmos do protótipo #1474 (`DEVICES`). Puramente
 *  informativo no motor (linha de evidência "Conexão do teste"), nunca muda thresholds —
 *  o protótipo não acopla device a categoria/thresholds, só registra o contexto. */
enum class DeviceJogo(val label: String) {
    PLAYSTATION("PS5 / PS4"),
    XBOX("Xbox"),
    PC("PC"),
    ANDROID("Android"),
    IPHONE("iPhone"),
    SWITCH("Switch"),
    TV_CLOUD("TV / Cloud gaming"),
}

/** Um jogo catalogado — [gameId] estável (nunca reaproveitar id de jogo removido; ver
 *  [CatalogoJogosModoGamer]). */
data class JogoCatalogoModoGamer(
    val gameId: String,
    val nome: String,
    val categoria: CategoriaJogoModoGamer,
)

/**
 * Catálogo dos 9 jogos do Modo gamer — mesma lista e ordem do protótipo #1474
 * (`diagnostico-guiado.jsx`, `GAMES`, issue #1483). Lista fechada por decisão de produto
 * (issue #1476, "fora de escopo: lista infinita de jogos") — jogo fora daqui cai em
 * [CategoriaJogoModoGamer.OUTRO] (ou na categoria genérica escolhida pelo usuário na tela de
 * fallback), nunca em erro.
 */
object CatalogoJogosModoGamer {
    val jogos: List<JogoCatalogoModoGamer> = listOf(
        JogoCatalogoModoGamer("freefire", "Free Fire", CategoriaJogoModoGamer.BATTLE_ROYALE),
        JogoCatalogoModoGamer("valorant", "Valorant", CategoriaJogoModoGamer.FPS_COMPETITIVO),
        JogoCatalogoModoGamer("codm", "Call of Duty Mobile", CategoriaJogoModoGamer.FPS_COMPETITIVO),
        JogoCatalogoModoGamer("ea_fc", "EA FC", CategoriaJogoModoGamer.FPS_COMPETITIVO),
        JogoCatalogoModoGamer("fortnite", "Fortnite", CategoriaJogoModoGamer.BATTLE_ROYALE),
        JogoCatalogoModoGamer("league_of_legends", "League of Legends", CategoriaJogoModoGamer.MOBA),
        JogoCatalogoModoGamer("minecraft", "Minecraft", CategoriaJogoModoGamer.CASUAL),
        JogoCatalogoModoGamer("roblox", "Roblox", CategoriaJogoModoGamer.CASUAL),
        JogoCatalogoModoGamer("genshin_impact", "Genshin Impact", CategoriaJogoModoGamer.CASUAL),
    )

    fun porId(gameId: String): JogoCatalogoModoGamer? = jogos.find { it.gameId == gameId }
}

/**
 * Resultado do Modo gamer para uma [categoria] (do jogo catalogado ou da categoria genérica
 * de fallback) + [device]. Mesmo contrato de evidências/status de
 * [ResultadoDiagnosticoGuiado] — o container visual "Medido pelo motor SignallQ" /
 * "Explicado por IA" (`AiVsMotorExplainer`, extraído em #1476 pra `ui/component`) é
 * literalmente o mesmo Composable dos dois motores.
 */
data class ResultadoModoGamer(
    val categoria: CategoriaJogoModoGamer,
    val device: DeviceJogo,
    val status: DiagnosticStatus,
    val mensagemMotor: String,
    val evidencias: List<EvidenciaDiagnostico>,
    val acoes: List<String>,
    val dadosInsuficientes: Boolean,
)
