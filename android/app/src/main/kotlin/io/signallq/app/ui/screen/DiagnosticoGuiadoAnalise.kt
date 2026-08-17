package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest

// Rota `Analise` do fluxo guiado — issue #1704 (2.0.09b, parte 4/4), épico #1647.
//
// ## Por que a rota existe
//
// Até aqui o fluxo guiado só podia ser aberto DEPOIS de um teste de velocidade: o overlay exigia
// `snapshotSpeedtest.resultado != null` para compor. Esse resultado vive em memória (o executor é
// `@Singleton`, não persiste), então na volta de process death a pilha de overlays sobrevivia e o
// resultado não — o usuário caía num estado vazio (GH#1714) sem caminho de volta ao diagnóstico.
//
// A correção escolhida (via B, decisão de Luiz em 2026-08-17) não é fazer o resultado sobreviver:
// é **remover a dependência**. A spec 2.0 §8.5 já define a análise como etapa DO fluxo —
// sintoma (§8.2) → contexto (§8.3) → preparação (§8.4) → **análise (§8.5)** → resultado (§8.6).
// Um fluxo que mede por conta própria não precisa de medição anterior, e a persistência do
// resultado deixa de ser problema em vez de ser resolvida.
//
// A via descartada (persistir `executionId` + `MedicaoDao.buscarPorExecutionId`) reidratava um
// `ResultadoSpeedtest` sintético: a `MedicaoEntity` tem 29 colunas contra 35 campos. E consertaria
// um overlay de três — `ResultadoVelocidade` e `DetalhesTecnicos` continuariam no estado vazio,
// porque consomem o resultado inteiro. Ver o comentário da decisão na issue #1704.

/**
 * Estado da rota `Analise`, derivado do snapshot do executor.
 *
 * Existe como tipo próprio em vez de a tela ler o [SnapshotExecucaoSpeedtest] direto por dois
 * motivos: o snapshot tem 12 campos dos quais a rota usa 4, e a tradução de fase técnica para
 * linguagem humana (§8.5: "Etapa atual em linguagem humana") é regra testável — não deve nascer
 * dentro de um `@Composable`, onde só teste instrumentado a alcança.
 */
sealed interface EstadoAnaliseGuiada {
    /** Nenhuma medição em curso e nenhum resultado — a rota precisa disparar uma. */
    data object NaoIniciada : EstadoAnaliseGuiada

    /** Medição em curso. [progresso] em 0f..1f; [etapa] já em linguagem de usuário. */
    data class EmAndamento(
        val progresso: Float,
        val etapa: String,
    ) : EstadoAnaliseGuiada

    /** Há resultado utilizável — o fluxo pode seguir para a conclusão (§8.6). */
    data object Concluida : EstadoAnaliseGuiada

    /** Medição terminou sem resultado. [mensagem] é o texto que o executor produziu. */
    data class Falhou(
        val mensagem: String,
    ) : EstadoAnaliseGuiada
}

/**
 * Traduz o snapshot do executor no estado da rota.
 *
 * A ordem das cláusulas importa e não é arbitrária:
 *
 * 1. **resultado presente vence tudo.** O executor volta a `estado = idle` preservando o
 *    `resultado` — `ExecutorSpeedtestCloudflare.cancelar()` faz exatamente isso ao destravar a
 *    tela de erro (GH#374), republicando `estado = idle` com `resultado = value.resultado`.
 *    Checar `estado` primeiro faria uma análise concluída regredir para [NaoIniciada] e disparar
 *    uma segunda medição.
 * 2. **`erro` só sem resultado** — erro com resultado parcial é tratado adiante pela validade do
 *    próprio resultado (`status.liberaConclusaoCompleta`), não aqui.
 *
 * A ordem entre `executando` e `erro` **não** importa, e a redação anterior desta lista afirmava
 * que sim. As duas cláusulas testam o mesmo campo (`estado`) com valores mutuamente exclusivos —
 * trocá-las de lugar não muda nenhum resultado, e o mutante que faz essa troca sobreviveu à
 * suíte. Só a cláusula do `resultado` depende de posição.
 */
fun estadoAnaliseGuiada(snapshot: SnapshotExecucaoSpeedtest): EstadoAnaliseGuiada =
    when {
        snapshot.resultado != null -> EstadoAnaliseGuiada.Concluida
        snapshot.estado == EstadoExecucaoSpeedtest.executando ->
            EstadoAnaliseGuiada.EmAndamento(
                progresso = snapshot.progressoGlobal.coerceIn(0f, 1f),
                etapa = etapaEmLinguagemHumana(snapshot.faseAtual),
            )
        snapshot.estado == EstadoExecucaoSpeedtest.erro ->
            EstadoAnaliseGuiada.Falhou(
                mensagem = snapshot.erroMensagem ?: MENSAGEM_FALHA_GENERICA,
            )
        else -> EstadoAnaliseGuiada.NaoIniciada
    }

/**
 * Nome da etapa como a pessoa entende — spec §8.5, "Verificando o tempo de resposta".
 *
 * Sem jargão de rede: "ping", "download" e "upload" descrevem o protocolo, não o que a pessoa
 * quer saber. O mesmo critério de linguagem que `ResultadoIndisponivelScreen` já aplica.
 */
fun etapaEmLinguagemHumana(fase: FaseSpeedtest): String =
    when (fase) {
        FaseSpeedtest.idle -> "Preparando a análise"
        FaseSpeedtest.ping -> "Verificando o tempo de resposta"
        FaseSpeedtest.download -> "Medindo a velocidade de recebimento"
        FaseSpeedtest.upload -> "Medindo a velocidade de envio"
        FaseSpeedtest.concluido -> "Fechando a análise"
    }

const val MENSAGEM_FALHA_GENERICA = "Não consegui concluir a análise agora."

/**
 * O que a tela guiada precisa para conduzir a rota `Analise`.
 *
 * Grupo único em vez de três parâmetros soltos, pelo mesmo motivo que a #1698 estabeleceu para os
 * registros do shell: uma fatia futura que mexa só na análise toca um campo, não a assinatura da
 * tela — que já tem 24 parâmetros.
 *
 * [onIniciar] e [onCancelar] são do shell porque só ele alcança o `ExecutorSpeedtest`; a tela não
 * conhece o executor e não deve conhecer.
 */
@Stable
data class AnaliseGuiadaContrato(
    val estado: EstadoAnaliseGuiada,
    val onIniciar: () -> Unit,
    val onCancelar: () -> Unit,
)
