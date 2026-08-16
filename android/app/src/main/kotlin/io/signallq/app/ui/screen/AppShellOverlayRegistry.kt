package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.network.AssistAbandonado
import io.signallq.app.core.network.AssistObjetivoSelecionado
import io.signallq.app.core.network.AssistPerguntaRespondida
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.feature.speedtest.ResultadoSpeedtest

/**
 * Ponto de extensão de overlays do [AppShell] (issue #1695, épico #1647).
 *
 * `AppShell.kt` cresceu em 5 das últimas 6 fatias do épico mesmo com cada uma cumprindo a
 * letra da regra de extração (higiene §4.3): a fiação de push/pop de overlay novo sempre
 * voltava para dentro do arquivo central. Esta issue generaliza o padrão que
 * `AppShellAssistOverlay.kt` (issue #1656) já provou — overlay com Composable e callbacks
 * próprios, em arquivo dedicado — para todo overlay do [AppShellOverlay.Companion] tocado
 * aqui, e cria o único ponto de agregação: este arquivo.
 *
 * ## Como plugar um overlay/rota novo SEM editar `AppShell.kt`
 *
 * 1. adicione o valor em `AppShellOverlay` (`AppShellNavigation.kt`) — continua sendo a
 *    ÚNICA fonte de verdade de quais overlays existem e como a pilha por raiz
 *    (push/pop/back/restauração) se comporta. Este arquivo NÃO compete com aquele: aqui só
 *    se decide "qual Composable desenha o overlay X", não "quando X entra/sai da pilha";
 * 2. crie `AppShellXxxOverlay.kt` com um `@Composable internal fun` que recebe **só o que
 *    precisa** (nunca a lista inteira de parâmetros do [AppShell]) — `overlayStack` mais um
 *    punhado de callbacks/dados estreitos, exatamente como `AppShellAssistOverlay.kt`;
 * 3. adicione uma chamada para ele dentro de [AppShellOverlayRegistry] abaixo — a ordem de
 *    declaração aqui NÃO decide o z-index visual (isso é [rememberOverlayZIndex], baseado na
 *    posição real em `overlayStack`; ver KDoc dela em `AppShellAssistOverlay.kt`), então a
 *    entrada nova pode ir em qualquer lugar da lista;
 * 4. se o overlay precisar de um dado que `AppShell.kt` já expõe (os grupos `AppShellXxxState`
 *    de `AppShellState.kt`, `overlayStack`, `navigator` etc.), repasse-o como parâmetro novo
 *    de [AppShellOverlayRegistry] e no único call site em `AppShell.kt` — uma linha alterada
 *    num arquivo que só assembla dependências, não a reforma de sempre.
 *
 * Overlays com estado hoisted fora da pilha (`showMonitoramentoSheet`,
 * `showEquipamentoCredenciaisSheet` etc. — sheets sem back-stack) ficam fora deste registro por
 * enquanto; migrá-los é trabalho futuro das 17 fatias restantes, não escopo desta issue.
 *
 * Migrados nesta issue (prova de conceito, 6 overlays): [AppShellOverlay.Assist] (rewire do
 * que #1656 já extraiu), [AppShellOverlay.Termos], [AppShellOverlay.Novidades],
 * [AppShellOverlay.Privacidade], [AppShellOverlay.DetalhesTecnicos], [AppShellOverlay.SinalWifi]
 * e [AppShellOverlay.Ping]. Os demais overlays (Ajustes, Perfil, Ferramentas, Dispositivos,
 * Fibra/EquipamentoInternet, Laudo, Dns, SinalCanais, ResultadoVelocidade, DiagnosticoGuiado,
 * ModoGamer) continuam inline em `AppShell.kt` — migração de cada um é responsabilidade de
 * quem tocar essa área numa fatia futura (ver `docs_ai/technical/appshell-overlay-registry.md`).
 */
@Composable
internal fun AppShellOverlayRegistry(
    overlayStack: MutableList<AppShellOverlay>,
    // Assist (issue #1656) — rewire do overlay já extraído, sem mudar seu comportamento.
    onAssistObjetivo: (AssistObjetivoSelecionado) -> Unit,
    onAssistResposta: (AssistPerguntaRespondida) -> Unit,
    onAssistAbandono: (AssistAbandonado) -> Unit,
    onPreSelecaoParaDiagnosticoGuiado: (objetivo: ObjetivoDiagnostico?, respostaPasso0: Int?) -> Unit,
    onSolicitarDiagnostico: () -> Long?,
    // Termos / Novidades / Privacidade — menu lateral (GH#1358).
    appVersion: String,
    onAbrirGerenciarDados: () -> Unit,
    // Detalhes técnicos — pós resultado de velocidade (Feature #550, issue #1475).
    resultadoSpeedtest: ResultadoSpeedtest?,
    localizacaoServidor: String?,
    localDevice: LocalNetworkDeviceSnapshot?,
    // Sinal Wi-Fi — hub Ferramentas (GH#1201).
    temPermissaoLocalizacao: Boolean,
    localizacaoBloqueadaPermanentemente: Boolean,
    onSolicitarPermissaoLocalizacao: () -> Unit,
) {
    AppShellAssistOverlay(
        overlayStack = overlayStack,
        onAssistObjetivo = onAssistObjetivo,
        onAssistResposta = onAssistResposta,
        onAssistAbandono = onAssistAbandono,
        onPreSelecaoParaDiagnosticoGuiado = onPreSelecaoParaDiagnosticoGuiado,
        onSolicitarDiagnostico = onSolicitarDiagnostico,
    )
    AppShellTermosOverlay(overlayStack = overlayStack)
    AppShellNovidadesOverlay(overlayStack = overlayStack, appVersion = appVersion)
    AppShellPrivacidadeOverlay(overlayStack = overlayStack, onAbrirGerenciarDados = onAbrirGerenciarDados)
    AppShellDetalhesTecnicosOverlay(
        overlayStack = overlayStack,
        resultadoSpeedtest = resultadoSpeedtest,
        localizacaoServidor = localizacaoServidor,
        localDevice = localDevice,
    )
    AppShellSinalWifiOverlay(
        overlayStack = overlayStack,
        temPermissaoLocalizacao = temPermissaoLocalizacao,
        localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
        onSolicitarPermissaoLocalizacao = onSolicitarPermissaoLocalizacao,
    )
    AppShellPingOverlay(overlayStack = overlayStack)
}
