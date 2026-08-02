package io.signallq.app.ui.screen

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.signallq.app.R
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ConfirmacaoDialog
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSurfaceCard
import kotlinx.coroutines.launch

private const val TOTAL_SLIDES = 2

private enum class OnboardingOverlay { TERMOS, PRIVACIDADE }

/**
 * Onboarding em 2 telas (redesign GH#128, design da Lia):
 * 1. Bem-vindo — logo + aceite obrigatorio de Termos/Privacidade (links reais)
 * 2. Permitir acesso — 4 permissoes opcionais com toggle individual + "permitir tudo"
 *
 * Nenhuma das 4 permissoes bloqueia o avanco; se o usuario seguir sem conceder nenhuma,
 * mostra um aviso (nao bloqueante) antes de concluir.
 */
@Composable
fun OnboardingScreen(
    onConcluir: () -> Unit,
    onPermissoesConcedidas: (Set<String>) -> Unit = {},
    onPermissoesSolicitadas: (Set<String>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { TOTAL_SLIDES })
    val scope = rememberCoroutineScope()
    val alturaTelaDP = LocalConfiguration.current.screenHeightDp
    val paginaAtual = pagerState.currentPage

    var termosAceitos by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<OnboardingOverlay?>(null) }
    var mostrarAvisoSemPermissao by remember { mutableStateOf(false) }
    var permissoesMarcadasAposSolicitacao by remember { mutableStateOf<OnboardingPermissoesMarcadas?>(null) }

    var permissoesConcedidas by remember {
        mutableStateOf(
            estadoInicialPermissoesOnboarding(
                possuiPermissao = { perm ->
                    ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                },
            ),
        )
    }
    var permissoesMarcadas by remember {
        mutableStateOf(
            OnboardingPermissoesMarcadas(
                wifiPerto = permissoesConcedidas.wifiPerto,
                dispositivosRede = permissoesConcedidas.dispositivosRede,
                sinalChip = permissoesConcedidas.sinalChip,
                notificacoes = permissoesConcedidas.notificacoes,
            ),
        )
    }

    val solicitarPermissoesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
            val atualizadas = aplicarResultadoPermissoesOnboarding(permissoesConcedidas, resultado)
            permissoesConcedidas = atualizadas
            permissoesMarcadasAposSolicitacao?.let { alvo ->
                permissoesMarcadas =
                    OnboardingPermissoesMarcadas(
                        wifiPerto = alvo.wifiPerto && atualizadas.wifiPerto,
                        dispositivosRede = alvo.dispositivosRede && atualizadas.dispositivosRede,
                        sinalChip = alvo.sinalChip && atualizadas.sinalChip,
                        notificacoes = alvo.notificacoes && atualizadas.notificacoes,
                    )
            }
            permissoesMarcadasAposSolicitacao = null
            // #1182 -- reporta TODAS as permissoes que o SO efetivamente perguntou nesta rodada
            // (concedidas ou nao), pra quem precisa saber que o dialogo real ja apareceu -- ao
            // contrario de onPermissoesConcedidas, que so reporta as concedidas.
            if (resultado.keys.isNotEmpty()) onPermissoesSolicitadas(resultado.keys)
            val concedidasNestaRodada = resultado.filterValues { it }.keys
            if (concedidasNestaRodada.isNotEmpty()) onPermissoesConcedidas(concedidasNestaRodada)
        }

    fun solicitarPermissoesDoOnboarding(marcadasDesejadas: OnboardingPermissoesMarcadas) {
        val paraSolicitar =
            permissoesAndroidParaSolicitar(marcadasDesejadas)
                .filter { perm -> ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED }
        if (paraSolicitar.isEmpty()) {
            permissoesMarcadas = marcadasDesejadas
            return
        }
        permissoesMarcadasAposSolicitacao = marcadasDesejadas
        solicitarPermissoesLauncher.launch(paraSolicitar.toTypedArray())
    }

    fun continuarDaTelaPermissoes() {
        if (permissoesConcedidas.nenhumaConcedida) {
            mostrarAvisoSemPermissao = true
        } else {
            onConcluir()
        }
    }

    // Back: overlay aberto -> fecha overlay; tela 2 -> volta pra tela 1; tela 1 -> consome sem navegar
    BackHandler {
        when {
            overlay != null -> overlay = null
            paginaAtual > 0 -> scope.launch { pagerState.animateScrollToPage(paginaAtual - 1, animationSpec = tween(200)) }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .safeDrawingPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false, // navegacao so via botao Continuar
        ) { pagina ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription =
                                if (pagina == 0) {
                                    "Página 1 de 2, boas-vindas e termos"
                                } else {
                                    "Página 2 de 2, permissões"
                                }
                        },
            ) {
                when (pagina) {
                    0 ->
                        OnboardingTelaBemVindo(
                            c = c,
                            termosAceitos = termosAceitos,
                            onTermosAceitosChange = { termosAceitos = it },
                            onAbrirTermos = { overlay = OnboardingOverlay.TERMOS },
                            onAbrirPrivacidade = { overlay = OnboardingOverlay.PRIVACIDADE },
                            alturaTelaDP = alturaTelaDP,
                            onContinuar = {
                                scope.launch { pagerState.animateScrollToPage(1, animationSpec = tween(200)) }
                            },
                        )
                    else ->
                        OnboardingTelaPermissoes(
                            c = c,
                            permissoesConcedidas = permissoesConcedidas,
                            permissoesMarcadas = permissoesMarcadas,
                            onMarcadasChange = { marcadas ->
                                solicitarPermissoesDoOnboarding(marcadas)
                            },
                            onContinuar = { continuarDaTelaPermissoes() },
                        )
                }
            }
        }

        overlay?.let { tela ->
            Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
                when (tela) {
                    OnboardingOverlay.TERMOS -> TermosDeUsoScreen(onVoltar = { overlay = null })
                    OnboardingOverlay.PRIVACIDADE -> PrivacidadeScreen(onVoltar = { overlay = null })
                }
            }
        }

        if (mostrarAvisoSemPermissao) {
            ConfirmacaoDialog(
                titulo = "Seguir sem permissões?",
                mensagem =
                    "Sem elas, algumas análises ficam incompletas — Wi-Fi, dispositivos na rede, sinal do " +
                        "chip ou notificações de queda podem não funcionar. Você pode ativar a qualquer " +
                        "momento em Ajustes.",
                textoBotaoConfirmar = "Continuar mesmo assim",
                textoBotaoCancelar = "Revisar permissões",
                onConfirmar = {
                    mostrarAvisoSemPermissao = false
                    onConcluir()
                },
                onCancelar = { mostrarAvisoSemPermissao = false },
            )
        }
    }
}

// ─── Tela 1 — Bem-vindo ───────────────────────────────────────────────────────

@Composable
private fun OnboardingTelaBemVindo(
    c: LkTokens,
    termosAceitos: Boolean,
    onTermosAceitosChange: (Boolean) -> Unit,
    onAbrirTermos: () -> Unit,
    onAbrirPrivacidade: () -> Unit,
    alturaTelaDP: Int,
    onContinuar: () -> Unit,
) {
    val logoSizeDp: Dp = if (alturaTelaDP < 540) 88.dp else 120.dp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.32f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(logoSizeDp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_signallq_logo),
                    contentDescription = "Logo SignallQ",
                    modifier = Modifier.size(logoSizeDp * 0.72f),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(horizontal = LkSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_tela1_titulo),
                style = MaterialTheme.typography.headlineSmall,
                color = c.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                text = stringResource(R.string.onboarding_tela1_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(LkSpacing.xl))

            val textoTermos =
                buildAnnotatedString {
                    append("Li e aceito os ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "termos_de_uso",
                            styles = TextLinkStyles(style = SpanStyle(color = c.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)),
                        ) { onAbrirTermos() },
                    ) {
                        append("Termos de Uso")
                    }
                    append(" e a ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "politica_privacidade",
                            styles = TextLinkStyles(style = SpanStyle(color = c.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)),
                        ) { onAbrirPrivacidade() },
                    ) {
                        append("Política de Privacidade")
                    }
                }

            LkSurfaceCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTermosAceitosChange(!termosAceitos) },
                outlined = false,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = termosAceitos,
                        onCheckedChange = onTermosAceitosChange,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Aceitar termos de uso e política de privacidade"
                            },
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = textoTermos,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(0.18f)
                    .padding(horizontal = LkSpacing.xl)
                    .navigationBarsPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onContinuar,
                enabled = termosAceitos,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (termosAceitos) "Começar" else "Aceite os termos para continuar"
                        },
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_tela1_btn_comecar),
                    color = c.onPrimary,
                )
            }
        }
    }
}

// ─── Tela 2 — Permitir acesso ─────────────────────────────────────────────────

@Composable
private fun OnboardingTelaPermissoes(
    c: LkTokens,
    permissoesConcedidas: OnboardingPermissoesConcedidas,
    permissoesMarcadas: OnboardingPermissoesMarcadas,
    onMarcadasChange: (OnboardingPermissoesMarcadas) -> Unit,
    onContinuar: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(LkSpacing.xl))
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = stringResource(R.string.onboarding_tela2_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = c.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LkSpacing.xl),
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = stringResource(R.string.onboarding_tela2_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LkSpacing.xl),
        )
        Spacer(Modifier.height(LkSpacing.lg))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            PermissaoToggleCard(
                icon = Icons.Outlined.Wifi,
                titulo = "Wi-Fi por perto",
                descricao = "Para encontrar e analisar as redes Wi-Fi ao seu redor.",
                marcado = permissoesMarcadas.wifiPerto,
                concedida = permissoesConcedidas.wifiPerto,
                onMarcadoChange = { onMarcadasChange(permissoesMarcadas.copy(wifiPerto = it)) },
                c = c,
            )
            PermissaoToggleCard(
                icon = Icons.Outlined.DevicesOther,
                titulo = "Dispositivos na rede",
                descricao = "Para identificar outros aparelhos conectados à sua rede local. Opcional.",
                marcado = permissoesMarcadas.dispositivosRede,
                concedida = permissoesConcedidas.dispositivosRede,
                onMarcadoChange = { onMarcadasChange(permissoesMarcadas.copy(dispositivosRede = it)) },
                c = c,
            )
            PermissaoToggleCard(
                icon = Icons.Outlined.CellTower,
                titulo = "Sinal do chip",
                descricao = "Para identificar sua operadora, o tipo de rede (4G, 5G) e a força do sinal.",
                marcado = permissoesMarcadas.sinalChip,
                concedida = permissoesConcedidas.sinalChip,
                onMarcadoChange = { onMarcadasChange(permissoesMarcadas.copy(sinalChip = it)) },
                c = c,
            )
            PermissaoToggleCard(
                icon = Icons.Outlined.Notifications,
                titulo = "Notificações",
                descricao = "Para avisar quando detectarmos quedas ou problemas na sua conexão.",
                marcado = permissoesMarcadas.notificacoes,
                concedida = permissoesConcedidas.notificacoes,
                onMarcadoChange = { onMarcadasChange(permissoesMarcadas.copy(notificacoes = it)) },
                c = c,
            )

            Spacer(Modifier.height(LkSpacing.xs))

            val alternarPermitirTudo: () -> Unit = {
                val marcarTudo = !permissoesMarcadas.todasMarcadas
                onMarcadasChange(
                    OnboardingPermissoesMarcadas(
                        wifiPerto = marcarTudo || permissoesConcedidas.wifiPerto,
                        dispositivosRede = marcarTudo || permissoesConcedidas.dispositivosRede,
                        sinalChip = marcarTudo || permissoesConcedidas.sinalChip,
                        notificacoes = marcarTudo || permissoesConcedidas.notificacoes,
                    ),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .clickable(onClick = alternarPermitirTudo)
                        .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs)
                        .semantics { contentDescription = "Permitir tudo" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // onCheckedChange = null: o toggle e so visual aqui, quem trata o clique e a Row inteira
                Checkbox(checked = permissoesMarcadas.todasMarcadas, onCheckedChange = null)
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Permitir tudo",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.onSurface,
                )
            }
            Spacer(Modifier.height(LkSpacing.md))
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.md)
                    .navigationBarsPadding(),
        ) {
            Button(
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Continuar" },
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_btn_continuar),
                    color = c.onPrimary,
                )
            }
        }
    }
}

@Composable
internal fun PermissaoToggleCard(
    icon: ImageVector,
    titulo: String,
    descricao: String,
    marcado: Boolean,
    concedida: Boolean,
    onMarcadoChange: (Boolean) -> Unit,
    c: LkTokens,
) {
    LkSurfaceCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(c.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(LkSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(text = descricao, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (concedida) "Permitido" else "Não permitido",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (concedida) c.success else c.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(LkSpacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                if (concedida) {
                    LkPillBadge(
                        text = "Concedida",
                        containerColor = c.success.copy(alpha = 0.14f),
                        contentColor = c.success,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Switch(
                    checked = marcado,
                    onCheckedChange = onMarcadoChange,
                    enabled = !concedida,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = c.onPrimary,
                            checkedTrackColor = c.primary,
                            uncheckedThumbColor = c.outline,
                            uncheckedTrackColor = c.surfaceContainerHighest,
                            uncheckedBorderColor = c.outline,
                            disabledCheckedThumbColor = c.onPrimary,
                            disabledCheckedTrackColor = c.primary.copy(alpha = 0.6f),
                            disabledUncheckedThumbColor = c.outline.copy(alpha = 0.6f),
                            disabledUncheckedTrackColor = c.surfaceContainerHighest.copy(alpha = 0.6f),
                        ),
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (concedida) {
                                    "$titulo: concedida, ative pelas configurações do sistema para alterar"
                                } else {
                                    "$titulo: ${if (marcado) "marcado" else "não marcado"}"
                                }
                        },
                )
            }
        }
    }
}
