package io.signallq.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.ui.LkTokens

/**
 * Mapeamento UNICO de [MetricStatus] (vocabulario canonico de 6 valores, `core/diagnostico`)
 * para cor semantica e label PT-BR — GH#1221 RF-06 / GH#1225 item C.
 *
 * Antes desta issue, `ResultadoVelocidadeScreen.kt` tinha sua propria regua de 3 valores
 * (Excelente/Regular/Ruim) com limiares numericos proprios para download/upload/latencia/
 * jitter/bufferbloat, divergentes do classificador canonico usado pelo motor de diagnostico
 * e pelo restante do app. Este arquivo e o UNICO ponto de conversao MetricStatus -> UI —
 * qualquer tela que precise mostrar veredito de metrica usa isto, nao reimplementa.
 *
 * `LkTokens` so tem 3 cores semanticas (success/warning/error) — nao existe uma 4a cor
 * "critico" nem uma variante clara para "bom" distinta de "excelente" hoje no design
 * system. Mapeamento conservador: excelente/bom -> success, regular -> warning,
 * ruim/critico -> error, inconclusivo -> textTertiary (neutro, sem alarmar o usuario por
 * falta de dado).
 */
fun MetricStatus.corSemantica(c: LkTokens): Color =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> c.success
        MetricStatus.regular -> c.warning
        MetricStatus.ruim, MetricStatus.critico -> c.error
        MetricStatus.inconclusivo -> c.textTertiary
    }

fun MetricStatus.labelPt(): String =
    when (this) {
        MetricStatus.excelente -> "Excelente"
        MetricStatus.bom -> "Bom"
        MetricStatus.regular -> "Regular"
        MetricStatus.ruim -> "Ruim"
        MetricStatus.critico -> "Crítico"
        MetricStatus.inconclusivo -> "Inconclusivo"
    }

/**
 * Cor de container/conteúdo/ícone para apresentação em banner (não em lista/badge simples, que já
 * usa [corSemantica]) — issue #1749 (NDS-02b, ADR-017). Nasceu para `ContinuidadeMedicao.kt`
 * trocar seu vocabulário-alvo de `DiagnosticStatus` para `MetricStatus` (decisão registrada em
 * #1746 seção 5: `DiagnosticStatus` é aposentado como fonte de dado, `MetricStatus` vira o
 * vocabulário canônico de UI). Mesmo esquema de cor/ícone que `DiagnosticStatusUi.kt` já usava
 * (`ok/info/attention/critical/inconclusive` → agora `excelente,bom/regular/ruim,critico/
 * inconclusivo`) — reindexado nos 6 valores de [MetricStatus], não uma escolha nova de design.
 */
fun MetricStatus.corContainer(c: LkTokens): Color =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> c.successContainer
        MetricStatus.regular -> c.warningContainer
        MetricStatus.ruim, MetricStatus.critico -> c.errorContainer
        MetricStatus.inconclusivo -> c.primaryContainer
    }

fun MetricStatus.corConteudo(c: LkTokens): Color =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> c.onSuccessContainer
        MetricStatus.regular -> c.onWarningContainer
        MetricStatus.ruim, MetricStatus.critico -> c.onErrorContainer
        MetricStatus.inconclusivo -> c.onPrimaryContainer
    }

fun MetricStatus.icone(): ImageVector =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> Icons.Outlined.CheckCircle
        MetricStatus.regular -> Icons.Outlined.WarningAmber
        MetricStatus.ruim, MetricStatus.critico -> Icons.Outlined.Error
        MetricStatus.inconclusivo -> Icons.Outlined.Info
    }
