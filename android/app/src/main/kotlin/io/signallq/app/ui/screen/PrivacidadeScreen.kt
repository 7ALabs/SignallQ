package io.signallq.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSheetDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadeScreen(
    onVoltar: () -> Unit,
    onAbrirGerenciarDados: () -> Unit = {},
    /**
     * A UMP tem formulário próprio para esta pessoa? — GH#1703/#1717.
     *
     * `true` só quando `privacyOptionsRequirementStatus == REQUIRED` (regiões sob GDPR). Quem
     * resolve isso é `ConsentManager.precisaOferecerOpcoesPrivacidade`, que precisa de uma
     * `Activity` — por isso chega como parâmetro, o que mantém a tela testável sem Activity real.
     *
     * **Não decide mais se o item aparece.** Até a #1717 decidia: fora do GDPR o item sumia, e
     * quem está no Brasil — a maior parte da base — não tinha nenhum controle de anúncio dentro do
     * app, embora receba anúncio personalizado. O item agora aparece para todos; o que muda é o
     * destino, e é por isso que o subtítulo depende deste flag em vez de ser fixo. Abrir um
     * formulário vazio continua sendo pior que não abrir nada — a diferença é que agora existe
     * para onde mandar quem não tem formulário.
     */
    mostrarOpcoesAnuncios: Boolean = false,
    onAbrirOpcoesAnuncios: () -> Unit = {},
    /** Mensagem a exibir quando o formulário da UMP falha ao abrir; `null` = sem erro pendente. */
    erroOpcoesAnuncios: String? = null,
    /** Chamado depois de exibir [erroOpcoesAnuncios], para o chamador limpar o estado. */
    onErroOpcoesAnunciosExibido: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(erroOpcoesAnuncios) {
        erroOpcoesAnuncios?.let {
            // `finally` porque `showSnackbar` SUSPENDE até a dispensa (~4s). Se a tela sair de
            // composição nesse intervalo — usuário fecha a Privacidade com o snackbar na tela — a
            // corrotina é cancelada no descarte e a linha seguinte nunca rodaria: o erro ficaria
            // preso não-nulo no overlay (que não é descartado, é o pai) e seria REEXIBIDO na
            // próxima abertura da tela, vindo de uma sessão anterior. Ressalva R5 de Caio na
            // PR #1709.
            //
            // Não inverter para consumir ANTES de exibir: limpar o estado muda a chave deste
            // `LaunchedEffect`, o que cancelaria o próprio `showSnackbar` em voo.
            try {
                snackbarHostState.showSnackbar(it)
            } finally {
                onErroOpcoesAnunciosExibido()
            }
        }
    }

    Scaffold(
        containerColor = c.bgPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Privacidade",
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
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // O resumo vem antes dos detalhes: é a informação que o protótipo prioriza para
            // uma pessoa decidir se quer continuar usando o app.
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg)
                            .padding(top = LkSpacing.md, bottom = LkSpacing.xl),
                ) {
                    Text(
                        text = "Seus dados, com clareza",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Text(
                        text =
                            "O SignallQ coleta somente o necessário para diagnosticar a conexão e melhorar o produto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }
            }

            item {
                ResumoPrivacidade(
                    c = c,
                    titulo = "Diagnósticos",
                    valor = "No aparelho",
                )
            }
            item {
                ResumoPrivacidade(
                    c = c,
                    titulo = "Histórico",
                    valor = "Armazenado localmente",
                )
            }
            item {
                ResumoPrivacidade(
                    c = c,
                    titulo = "Analytics",
                    valor = "Consentimento controlável",
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAbrirOpcoesAnuncios,
                    ) {
                        Text("Gerenciar consentimento")
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAbrirGerenciarDados,
                        colors = ButtonDefaults.textButtonColors(contentColor = c.primary),
                    ) {
                        Text("Excluir dados locais")
                    }
                }
            }

            item { LkSheetDivider(modifier = Modifier.padding(horizontal = LkSpacing.lg)) }

            // Detalhes preservados para quem quer entender cada categoria antes de agir.
            item {
                PrivacidadeSection(
                    titulo = "Dados que coletamos",
                    descricao = "Speedtest, scans Wi-Fi e diagnósticos. Tudo fica no Room (SQLite local).",
                    c = c,
                )
            }

            // Section: Permissões usadas
            item {
                PrivacidadeSection(
                    titulo = "Permissões usadas",
                    descricao = "Localização (para listar redes Wi-Fi), Telefonia (4G/5G), notificações (alertas).",
                    c = c,
                )
            }

            // Section: Compartilhamento opcional
            item {
                PrivacidadeSection(
                    titulo = "Compartilhamento opcional",
                    descricao = "Apenas se você acionar \"Compartilhar resultado\" ou \"Diagnóstico IA\".",
                    c = c,
                )
            }

            item { Spacer(Modifier.height(LkSpacing.lg)) }

            // Destino único para limpar histórico, apagar dados ou resetar o app --
            // antes eram dois botões diretos aqui, sem confirmação (ver critique P0).
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAbrirGerenciarDados)
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gerenciar dados e privacidade",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textPrimary,
                        )
                        Text(
                            text = "Limpar histórico, apagar dados locais ou resetar o app",
                            style = MaterialTheme.typography.bodySmall,
                            // GH#937: textTertiary sobre branco ~2.5:1 (fail AA). textSecondary ~4.8:1.
                            color = c.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = c.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // GH#1703 — a UMP exige entrada permanente para rever o consentimento de anúncios
            // depois de já tê-lo dado. Antes daquela issue o app só sabia coletar; não havia
            // caminho de volta.
            //
            // GH#1717 — e o item deixou de depender da UMP para existir. Ele aparecia só sob GDPR,
            // então quem está no Brasil recebia anúncio personalizado sem nenhum controle dentro
            // do app. O destino é que muda: formulário da UMP onde ele existe, configurações de
            // anúncios do Android onde não existe.
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAbrirOpcoesAnuncios)
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Campaign,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preferências de anúncios",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textPrimary,
                        )
                        Text(
                            text =
                                if (mostrarOpcoesAnuncios) {
                                    "Rever a escolha que você fez sobre anúncios neste aparelho"
                                } else {
                                    "Controlar a personalização dos anúncios nas configurações do Android"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = c.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            item {
                LkSheetDivider(modifier = Modifier.padding(horizontal = LkSpacing.lg))
            }

            item {
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ResumoPrivacidade(
    c: LkTokens,
    titulo: String,
    valor: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
        Text(text = valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
    }
}

@Composable
private fun PrivacidadeSection(
    titulo: String,
    descricao: String,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
    }
}
