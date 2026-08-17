package io.signallq.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * Estado vazio para overlay restaurado sem o resultado da medição — issue #1714.
 *
 * ## O problema que isto resolve
 *
 * `AppShellNavigator.Saver` faz a **pilha de overlays sobreviver ao process death**. O
 * `ResultadoSpeedtest` não sobrevive: vem de `ExecutorSpeedtest.snapshotFlow`, que é `@Singleton`
 * em memória. Na volta, três overlays ficavam com `X in overlayStack == true` e `resultado == null`
 * ao mesmo tempo, e o `AnimatedVisibility` simplesmente **não compunha nada**.
 *
 * O usuário via a tela de trás e, ao tocar voltar, consumia um `pop()` que não correspondia a nada
 * visível — um toque que aparentemente não faz nada. Não é crash e não perde dado, mas é
 * inexplicável para quem usa.
 *
 * ## Por que estado vazio e não reidratação
 *
 * Reidratar o `ResultadoSpeedtest` do Room produziria um objeto **sintético**: a `MedicaoEntity`
 * tem 29 colunas contra 35 campos, faltando `stabilityScore`, `peakDownloadMbps`,
 * `severidadeBufferbloat`, `diagnosticoQualidade`, `diagnosticoFases`, entre outros. Mostrar um
 * resultado com metade dos campos inventados é pior que admitir que ele não está mais disponível —
 * é a mesma escolha de honestidade que o KDoc de `DiagnosticoGuiadoEstado` já registra.
 *
 * ## Uma tela, três consumidores
 *
 * `ResultadoVelocidade`, `DiagnosticoGuiado` e `DetalhesTecnicos` compartilham a mesma causa. Uma
 * tela só, parametrizada pelo título, evita que a próxima correção conserte um e deixe dois — que
 * é exatamente o que a issue aponta como risco.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultadoIndisponivelScreen(
    titulo: String,
    onVoltar: () -> Unit,
    onMedirNovamente: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LkSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.HistoryToggleOff,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "Este resultado não está mais disponível",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = LkSpacing.lg),
            )
            Text(
                // Enuncia a CONSEQUÊNCIA, não a causa. A versão anterior afirmava "o aplicativo
                // foi fechado em segundo plano e..." — o que só é verdade se process death for o
                // único caminho para chegar aqui, e ninguém provou isso. Afirmar causa com certeza
                // é o mesmo defeito que esta sessão corrigiu em dois documentos hoje (ressalva 3
                // de Caio na PR #1718). O tom continua: sem jargão, sem culpar o usuário.
                text =
                    "Os detalhes desta medição não ficam guardados depois que o aplicativo é " +
                        "fechado. Você pode medir de novo a qualquer momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = LkSpacing.sm),
            )
            if (onMedirNovamente != null) {
                Button(
                    onClick = onMedirNovamente,
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                    modifier = Modifier.padding(top = LkSpacing.xl),
                ) {
                    Text("Medir agora")
                }
            }
        }
    }
}
