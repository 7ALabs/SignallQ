package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.draw.clip
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
                    .background(c.bgPrimary)
                    .padding(LkSpacing.xl)
                    .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(c.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = c.error,
                    modifier = Modifier.size(36.dp),
                )
            }
            androidx.compose.foundation.layout
                .Spacer(Modifier.height(LkSpacing.xl))
            Text(
                text = "Informações não disponíveis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            androidx.compose.foundation.layout
                .Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "O SignallQ precisa de condições mínimas (ex: estar no Wi-Fi e com permissão) para exibir este diagnóstico.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            androidx.compose.foundation.layout
                .Spacer(Modifier.weight(1f))

            // Actions
            if (onMedirNovamente != null) {
                Button(
                    onClick = onMedirNovamente,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape =
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(io.signallq.app.ui.LkRadius.button),
                    colors =
                        androidx.compose.material3.ButtonDefaults
                            .buttonColors(containerColor = c.primary),
                ) {
                    Text("Tentar novamente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            androidx.compose.foundation.layout
                .Spacer(Modifier.height(LkSpacing.xl))
        }
    }
}
