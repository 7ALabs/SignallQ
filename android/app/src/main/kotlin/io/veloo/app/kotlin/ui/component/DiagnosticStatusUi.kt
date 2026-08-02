package io.signallq.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.ui.LkTokens

/**
 * Mapeamento UNICO de [DiagnosticStatus] (vocabulario de 5 valores usado pelos motores de
 * diagnostico guiado/Modo gamer/equipamento local/Laudo, `core/diagnostico`) para cor
 * semantica, cor de container/conteudo, label PT-BR e icone — GH#1228 Fase 0, item P0-8
 * (Fatia 8).
 *
 * Antes desta fatia, 4 componentes reimplementavam esse mapeamento de forma divergente:
 * `DiagnosticoStatusBanner` (`DiagnosticoResultadoComponents.kt`) tratava `attention` com o
 * mesmo peso visual (vermelho) de `critical`, enquanto `EquipamentoModuloTecnicoCard`,
 * `LocalDeviceSection` e `LaudoScreen` ja usavam laranja/`warning` para `attention` — o mesmo
 * achado podia parecer "critico" numa tela e "moderado" em outra. Este arquivo e o UNICO
 * ponto de conversao `DiagnosticStatus` -> UI, irmao de [MetricStatusUi] (que cobre o
 * vocabulario `MetricStatus`, de 6 valores, usado pelas metricas de speedtest/sinal).
 *
 * Decisao do mapeamento (nao unanime nas 4 implementacoes anteriores, escolhido por
 * alinhamento com o design system e maioria):
 * - `ok` -> `success` (unanime nas 4 implementacoes anteriores).
 * - `info` -> `primary` (maioria: 3 de 4 ja usavam `primary`/`primaryContainer` para
 *   "informacao"; so o banner tratava `info` como sinonimo de `ok`/verde).
 * - `attention` -> `warning` (maioria: 3 de 4 ja usavam laranja; o banner era o unico
 *   divergente tratando como vermelho — exatamente o defeito do P0-8).
 * - `critical` -> `error` (unanime).
 * - `inconclusive` -> `primary` (mesmo tratamento de `info`, neutro/nao alarmante — segue o
 *   mesmo principio ja documentado em [MetricStatusUi] para `MetricStatus.inconclusivo`:
 *   ausencia de dado nao deve ter o mesmo peso visual de um problema real detectado. E o
 *   unico dos 4 mapeamentos anteriores que ja fazia essa distincao (`LocalDeviceSection`).
 */
fun DiagnosticStatus.corSemantica(c: LkTokens): Color =
    when (this) {
        DiagnosticStatus.ok -> c.success
        DiagnosticStatus.info -> c.primary
        DiagnosticStatus.attention -> c.warning
        DiagnosticStatus.critical -> c.error
        DiagnosticStatus.inconclusive -> c.primary
    }

fun DiagnosticStatus.corContainer(c: LkTokens): Color =
    when (this) {
        DiagnosticStatus.ok -> c.successContainer
        DiagnosticStatus.info -> c.primaryContainer
        DiagnosticStatus.attention -> c.warningContainer
        DiagnosticStatus.critical -> c.errorContainer
        DiagnosticStatus.inconclusive -> c.primaryContainer
    }

fun DiagnosticStatus.corConteudo(c: LkTokens): Color =
    when (this) {
        DiagnosticStatus.ok -> c.onSuccessContainer
        DiagnosticStatus.info -> c.onPrimaryContainer
        DiagnosticStatus.attention -> c.onWarningContainer
        DiagnosticStatus.critical -> c.onErrorContainer
        DiagnosticStatus.inconclusive -> c.onPrimaryContainer
    }

fun DiagnosticStatus.labelPt(): String =
    when (this) {
        DiagnosticStatus.ok -> "Conexão saudável"
        DiagnosticStatus.info -> "Informação"
        DiagnosticStatus.attention -> "Atenção"
        DiagnosticStatus.critical -> "Problema detectado"
        DiagnosticStatus.inconclusive -> "Inconclusivo"
    }

fun DiagnosticStatus.icone(): ImageVector =
    when (this) {
        DiagnosticStatus.ok -> Icons.Outlined.CheckCircle
        DiagnosticStatus.info -> Icons.Outlined.Info
        DiagnosticStatus.attention -> Icons.Outlined.WarningAmber
        DiagnosticStatus.critical -> Icons.Outlined.Error
        DiagnosticStatus.inconclusive -> Icons.Outlined.Info
    }
