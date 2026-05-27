package io.linka.app.kotlin.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.R
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import kotlinx.coroutines.launch

private const val TOTAL_SLIDES = 3

@Composable
fun OnboardingScreen(
    onConcluir: () -> Unit,
    // #128: callbacks de permissão para o slide 3
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    onSolicitarPermissaoDispositivosProximos: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val pagerState = rememberPagerState(pageCount = { TOTAL_SLIDES })
    val scope = rememberCoroutineScope()
    val alturaTelaDP = LocalConfiguration.current.screenHeightDp
    val iconeSizeDp: Dp = if (alturaTelaDP < 540) 64.dp else 88.dp
    val paginaAtual = pagerState.currentPage

    // #128: checkbox de aceite obrigatório no slide 1 (privacidade)
    var termosAceitos by remember { mutableStateOf(false) }

    // Back: slide 0 → consumir sem navegar; slides 1+ → volta ao anterior
    BackHandler {
        if (paginaAtual > 0) {
            scope.launch { pagerState.animateScrollToPage(paginaAtual - 1, animationSpec = tween(200)) }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false, // #128: impede swipe — só navegação via botões
        ) { pagina ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription =
                                "Página ${pagina + 1} de 3, ${when (pagina) {
                                    0 -> "boas-vindas"
                                    1 -> "privacidade e termos"
                                    else -> "permissões"
                                }}"
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Zona ilustração: weight responsivo por altura de tela
                val pesoIlustracao: Float =
                    when {
                        alturaTelaDP < 540 -> 0.30f
                        else -> 0.35f
                    }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(pesoIlustracao),
                    contentAlignment = Alignment.Center,
                ) {
                    when (pagina) {
                        0 -> OnboardingSlide0Visual()
                        1 -> OnboardingSlide1Visual(c = c)
                        else -> OnboardingSlide2Visual(c = c)
                    }
                }

                // Zona título + descrição: weight 0.35
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                            .padding(horizontal = LkSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    val titulo =
                        when (pagina) {
                            0 -> stringResource(R.string.onboarding_slide0_titulo)
                            1 -> stringResource(R.string.onboarding_slide1_titulo)
                            else -> stringResource(R.string.onboarding_slide2_titulo)
                        }
                    val descricao =
                        when (pagina) {
                            0 -> stringResource(R.string.onboarding_slide0_descricao)
                            1 -> stringResource(R.string.onboarding_slide1_descricao)
                            else -> stringResource(R.string.onboarding_slide2_descricao)
                        }

                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(LkSpacing.md))
                    Text(
                        text = descricao,
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )

                    // #128: Slide 1 — checkbox de aceite de termos (obrigatório)
                    if (pagina == 1) {
                        Spacer(Modifier.height(LkSpacing.lg))
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(LkRadius.card))
                                    .background(c.bgCard)
                                    .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                                    .clickable { termosAceitos = !termosAceitos }
                                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm)
                                    .semantics { contentDescription = "Aceitar termos de uso e política de privacidade" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = termosAceitos,
                                onCheckedChange = { termosAceitos = it },
                            )
                            Spacer(Modifier.width(LkSpacing.sm))
                            Text(
                                text = "Li e aceito os Termos de Uso e a Política de Privacidade",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // #128: Slide 2 — cards de permissão (localização + dispositivos próximos)
                    if (pagina == 2) {
                        Spacer(Modifier.height(LkSpacing.lg))
                        PermissaoCard(
                            icon = Icons.Outlined.LocationOn,
                            titulo = "Localização aproximada / Wi-Fi",
                            descricao = "Para identificar redes Wi-Fi ao redor e analisar canais",
                            labelBotao = "Permitir análise de Wi-Fi",
                            onClick = onSolicitarPermissaoLocalizacao,
                            c = c,
                        )
                        Spacer(Modifier.height(LkSpacing.sm))
                        PermissaoCard(
                            icon = Icons.Outlined.NearMe,
                            titulo = "Dispositivos próximos",
                            descricao = "Para detectar dispositivos na rede local (opcional)",
                            labelBotao = "Permitir",
                            onClick = onSolicitarPermissaoDispositivosProximos,
                            c = c,
                        )
                    }
                }

                // Zona dots: centralizado entre texto e botões
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = LkSpacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(TOTAL_SLIDES) { index ->
                        val largura: Dp by animateDpAsState(
                            targetValue = if (index == paginaAtual) 22.dp else 8.dp,
                            animationSpec = tween(durationMillis = 200),
                            label = "dot_width_$index",
                        )
                        val dotDesc =
                            if (index == paginaAtual) {
                                "Slide ${index + 1} de 3, selecionado"
                            } else {
                                "Slide ${index + 1} de 3"
                            }
                        Box(
                            modifier =
                                Modifier
                                    .width(largura)
                                    .height(10.dp)
                                    .background(
                                        color =
                                            if (index == paginaAtual) {
                                                LkColors.accent
                                            } else {
                                                c.textSecondary.copy(alpha = 0.55f)
                                            },
                                        shape = CircleShape,
                                    ).semantics { contentDescription = dotDesc },
                        )
                        if (index < TOTAL_SLIDES - 1) {
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }

                // Zona botões
                val alturaZonaBotoes: Dp =
                    when {
                        alturaTelaDP < 540 -> 56.dp
                        else -> 80.dp
                    }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(alturaZonaBotoes)
                            .padding(horizontal = LkSpacing.xl),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Botão ← Anterior: oculto no slide 0
                    if (pagina > 0) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(pagina - 1, animationSpec = tween(200)) }
                            },
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Voltar ao slide anterior"
                                },
                            colors = ButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_btn_anterior),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    // Slide 2: botão "Começar →"
                    // Slides 0 e 1: botão "Próximo →" (slide 1 bloqueado até aceitar termos)
                    if (pagina == TOTAL_SLIDES - 1) {
                        AnimatedVisibility(
                            visible = paginaAtual == TOTAL_SLIDES - 1,
                            enter = fadeIn(tween(300)),
                            exit = ExitTransition.None,
                        ) {
                            Button(
                                onClick = onConcluir,
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "Começar a usar o app"
                                    },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = LkColors.accent,
                                    ),
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_btn_comecar),
                                    color = LkColors.linkaTextOnDark,
                                    fontWeight = FontWeight.W600,
                                )
                            }
                        }
                    } else {
                        // #128: slide 1 requer checkbox marcado para avançar
                        val podeAvancar = pagina != 1 || termosAceitos
                        FilledTonalButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(pagina + 1, animationSpec = tween(200)) }
                            },
                            enabled = podeAvancar,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = if (podeAvancar) "Próximo slide" else "Aceite os termos para continuar"
                                },
                            colors = ButtonDefaults.filledTonalButtonColors(),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_btn_proximo),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.W600,
                            )
                        }
                    }
                }
            }
        }

        // Botão Pular: TopEnd overlay, visível apenas no slide 0 (#128: slide 1 requer aceite, não pode pular)
        if (paginaAtual == 0) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopEnd)
                        .padding(top = LkSpacing.sm, end = LkSpacing.sm),
                contentAlignment = Alignment.TopEnd,
            ) {
                TextButton(
                    onClick = onConcluir,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Pular tutorial"
                        },
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_btn_pular),
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

// ─── Card de permissão para o slide 3 ────────────────────────────────────────

@Composable
private fun PermissaoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descricao: String,
    labelBotao: String,
    onClick: () -> Unit,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        FilledTonalButton(
            onClick = onClick,
            colors = ButtonDefaults.filledTonalButtonColors(),
            contentPadding = PaddingValues(horizontal = LkSpacing.sm, vertical = 4.dp),
        ) {
            Text(text = labelBotao, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─── Ilustrações dos slides ───────────────────────────────────────────────────

@Composable
private fun OnboardingSlide0Visual() {
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(180.dp),
            color = LkColors.success,
            strokeWidth = 6.dp,
            trackColor = LkColors.success.copy(alpha = 0.15f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "147",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = LkColors.success,
            )
            Text(
                "Mbps",
                style = MaterialTheme.typography.bodySmall,
                color = LkColors.success.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LkColors.success.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    "Download",
                    style = MaterialTheme.typography.labelSmall,
                    color = LkColors.success,
                    fontWeight = FontWeight.W600,
                )
            }
        }
    }
}

@Composable
private fun OnboardingSlide1Visual(c: LkTokens) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = LkColors.accent,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Este dispositivo",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LkColors.accent.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    "Local",
                    style = MaterialTheme.typography.labelSmall,
                    color = LkColors.accent,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = c.border, thickness = 0.5.dp)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                "IP local",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "192.168.•.•",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                "DNS",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Cloudflare",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
        }
    }
}

@Composable
private fun OnboardingSlide2Visual(c: LkTokens) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Velocidade aprovada",
                modifier = Modifier.size(18.dp),
                tint = LkColors.success,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Streaming HD",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Ótimo",
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.success,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = c.border)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = "Atenção",
                modifier = Modifier.size(18.dp),
                tint = LkColors.warning,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Jogos online",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Atenção",
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.warning,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = c.border)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = "Problema detectado",
                modifier = Modifier.size(18.dp),
                tint = LkColors.error,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Videochamada",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Instável",
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.error,
            )
        }
    }
}
