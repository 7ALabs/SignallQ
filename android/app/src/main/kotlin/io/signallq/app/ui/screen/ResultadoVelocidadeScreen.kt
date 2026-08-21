package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ads.AdSlot
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResultadoPdfGenerator
import io.signallq.app.ui.ads.NativeAdEligibility
import io.signallq.app.ui.component.classificarBufferbloatLocal
import io.signallq.app.ui.component.classificarDownloadLocal
import io.signallq.app.ui.component.classificarJitterLocal
import io.signallq.app.ui.component.classificarLatenciaLocal
import io.signallq.app.ui.component.classificarPerdaPacotesLocal
import io.signallq.app.ui.component.classificarUploadLocal
import io.signallq.app.ui.component.corSemantica
import io.signallq.app.ui.component.labelPt
import kotlinx.coroutines.launch

/**
 * GH#1659a — mapeia o único sinal que esta tela recebe de fora ([adsEnabled]) pro contrato
 * tipado [NativeAdEligibility]. `canRequestAds`/`online` continuam derivados só desse flag: a
 * tela não recebe sinal de consentimento UMP nem de conectividade separados do Remote Config
 * (mesma limitação que `rememberNativeAd()`, o wrapper antigo, já tinha — não é regressão desta
 * migração). Diferenciar os dois sinais de verdade exigiria plumbing novo em AppShell.kt/
 * MainViewModel, fora do escopo desta fatia puramente técnica (decisão de arquitetura de ads,
 * issues #1330/#1694 — ver o mesmo limite documentado em `AppShellRootRegistryTest`).
 */
internal fun eligibilidadeAnuncioResultado(adsEnabled: Boolean): NativeAdEligibility =
    NativeAdEligibility(
        slot = AdSlot.RESULTADO,
        flagEnabled = adsEnabled,
        canRequestAds = adsEnabled,
        online = true,
    )

/**
 * GH#1659a — mensagem exibida quando `ResultadoPdfGenerator.gerarECompartilhar` lança durante o
 * compartilhamento do resultado (mesmo padrão de `LaudoScreen.compartilharLaudo`).
 */
internal fun mensagemErroCompartilhamentoResultado(erro: Throwable): String =
    "Não foi possível compartilhar o resultado: ${erro.message}"

/**
 * Tela "Resultado do teste" — GH#536.
 *
 * Escopo reduzido de propósito: mostra só o essencial pra responder "minha internet
 * está boa? o que está ruim? o que eu faço agora?" — diagnóstico geral, badge de rede
 * e os 5 cards principais (Download/Upload/Latência/Oscilação/Perda).
 *
 * Resumo pós-teste enxuto, sem despejo de dado técnico (Feature #550, issue #1475):
 * as 3 CTAs abaixo do card "Como sua internet deve funcionar" abrem cada uma seu próprio destino —
 * [DiagnosticoGuiadoScreen] (7 objetivos fechados, motor local + explicação por IA),
 * modo gamer (fluxo próprio, issue #1476) e [DetalhesTecnicosScreen] (métricas cruas).
 * Antes dessa issue, um único CTA abria a sheet automática "Análise detalhada"
 * (`DiagnosticoDetalhadoSheet`, retirada) com banner de IA e recomendação abertos
 * sozinhos ao entrar aqui — decisão do Luiz (comentário de #1474, 2026-07-26): o
 * banner/recomendação só aparece depois que o usuário escolhe um objetivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoVelocidadeScreen(
    resultado: ResultadoSpeedtest,
    snapshotDiagnostico: SnapshotDiagnostico,
    onTestarNovamente: () -> Unit,
    onIrParaHome: () -> Unit,
    onVoltar: () -> Unit = {},
    /** GH#784 — etapa "compartilhou" do funil do teste de velocidade (Uso do App,
     *  admin-worker). Disparado junto do compartilhamento real do PDF, nao antes. */
    onCompartilhar: () -> Unit = {},
    localizacaoServidor: String? = null,
    ispInfo: IspInfo? = null,
    operadoraMovel: String? = null,
    /** Só alimenta o PDF compartilhado (última análise de IA disponível, se houver)
     *  — não dispara mais análise automática nesta tela (issue #1475). */
    analisadorState: AnalisadorState = AnalisadorState.Inativo,
    /** Iniciar diagnóstico guiado por objetivo — issue #1475 ([DiagnosticoGuiadoScreen]). */
    onIniciarDiagnosticoGuiado: () -> Unit = {},
    /** Iniciar modo gamer (jogo → device → resultado) — issue #1476, ainda não
     *  implementado; o CTA já aparece no resumo por paridade com o protótipo #1474. */
    onIniciarModoGamer: () -> Unit = {},
    /** Ver detalhes técnicos (métricas cruas, sem IA/recomendação) — issue #1475
     *  ([DetalhesTecnicosScreen]). */
    onVerDetalhesTecnicos: () -> Unit = {},
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current
    val scrollState = rememberScrollState()
    val decisao = snapshotDiagnostico.relatorio?.decisao
    val decisaoTitulo = decisao?.titulo
    val decisaoMensagem = decisao?.mensagemUsuario
    var compartilhando by remember { mutableStateOf(false) }
    // GH#1659a — visível pra quem tocou compartilhar; nunca deixa o spinner travado sem
    // explicação quando ResultadoPdfGenerator.gerarECompartilhar lança (mesmo padrão de erro
    // de LaudoScreen.compartilharLaudo).
    var erroCompartilhamento by remember { mutableStateOf<String?>(null) }
    var metricasDetalhadasAbertas by remember { mutableStateOf(false) }
    // Issue #555 -- dispensar o anuncio e estado de sessao (some ate o proximo resultado
    // recompor a tela do zero); nunca persistido, nunca conta como feedback de recomendacao.
    var nativeAdDismissedResultado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // GH#1221 RF-06 / GH#1225 item C — classificador UNICO (core/diagnostico), a tela nao
    // mantem mais sua propria regua numerica (antes divergia do motor de diagnostico: 3
    // faixas Excelente/Regular/Ruim aqui vs. 6 faixas canonicas em MetricClassifier, com
    // limiares numericos diferentes para a MESMA metrica). "Perda" e rotulada como
    // ESTIMADA — GH#1221 RF-04, o metodo e por timeout de probes HTTP, nao medicao direta
    // de perda de pacotes IP.
    //
    // NDS-02d (#1752, ADR-017) — os 6 cards abaixo pararam de chamar MetricClassifier
    // direto e passaram a delegar pro seam ClassificacaoMetricaLocal.kt (mesmo padrao da
    // NDS-02b), hoje com a MESMA matematica local (comportamento identico, provado por
    // teste de caracterizacao em ClassificacaoMetricaLocalTest). Sem chamada viva ao NDS
    // ainda — a orquestracao real (quando disparar uma avaliacao, tratar loading/erro) e
    // trabalho da fatia final NDS-02k/MainViewModel.
    //
    // GH#1521 (P0-1 da auditoria #1228) tinha introduzido comSeveridadeConciliada() pra
    // impedir que o card de latencia/upload mostrasse veredito melhor do que o achado
    // ativo do InternetDiagnosticEngine (banner desta MESMA tela). A NDS-02d REMOVEU essa
    // mecanica por completo (decisao registrada no inventario de #1746): ela dependia de
    // duas reguas numericas paralelas (MetricClassifier vs. InternetDiagnosticEngine) e de
    // uma taxonomia de ID de achado (prefixo "IN-NORMAL-04"/"IN-NORMAL-05") que o NDS nao
    // reproduz — vira codigo morto assim que a fonte de veredito migra pro NDS, entao sai
    // agora em vez de ser carregada pro seam. Os cards voltam a mostrar so a classificacao
    // isolada; unificar as duas reguas globalmente segue sendo escopo da issue #1466.

    val statusDownload = remember(resultado.downloadMbps) { classificarDownloadLocal(resultado.downloadMbps) }
    val corDownload = statusDownload.corSemantica(c)
    val veredictoDownload = statusDownload.labelPt()

    val statusUpload =
        remember(resultado.uploadMbps, resultado.uploadNaoDetectado) {
            if (resultado.uploadNaoDetectado) {
                MetricStatus.inconclusivo
            } else {
                classificarUploadLocal(resultado.uploadMbps)
            }
        }
    val corUpload = statusUpload.corSemantica(c)
    val veredictoUpload = statusUpload.labelPt()

    val statusPerda = remember(resultado.perdaPercentual) { classificarPerdaPacotesLocal(resultado.perdaPercentual) }
    val corPerda = statusPerda.corSemantica(c)
    val veredictoPerda = statusPerda.labelPt()

    val statusLatencia = remember(resultado.latenciaMs) { classificarLatenciaLocal(resultado.latenciaMs) }
    val corLatencia = statusLatencia.corSemantica(c)
    val veredictoLatencia = statusLatencia.labelPt()

    val statusJitter = remember(resultado.jitterMs) { classificarJitterLocal(resultado.jitterMs) }
    val corJitter = statusJitter.corSemantica(c)
    val veredictoJitter = statusJitter.labelPt()

    val statusBufferbloat = remember(resultado.bufferbloatMs) { classificarBufferbloatLocal(resultado.bufferbloatMs) }
    val corBufferbloat = statusBufferbloat.corSemantica(c)
    val veredictoBufferbloat = statusBufferbloat.labelPt()

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Resultado do teste", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                    }
                },
                actions = {
                    if (compartilhando) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = c.primary,
                        )
                    } else {
                        IconButton(onClick = {
                            compartilhando = true
                            erroCompartilhamento = null
                            scope.launch {
                                try {
                                    ResultadoPdfGenerator.gerarECompartilhar(
                                        context = context,
                                        resultado = resultado,
                                        snapshotDiagnostico = snapshotDiagnostico,
                                        analisadorState = analisadorState,
                                        ispInfo = ispInfo,
                                        operadoraMovel = operadoraMovel,
                                        localizacaoServidor = localizacaoServidor,
                                    )
                                    onCompartilhar()
                                } catch (e: Exception) {
                                    erroCompartilhamento = mensagemErroCompartilhamentoResultado(e)
                                } finally {
                                    compartilhando = false
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Compartilhar resultado",
                                tint = c.textPrimary,
                            )
                        }
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
                    .verticalScroll(scrollState)
                    .padding(LkSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero do Veredito (result-verdict)
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(c.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Speed, contentDescription = null, tint = c.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                text = decisaoTitulo ?: "Boa velocidade, resposta instável",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = decisaoMensagem ?: "Downloads devem funcionar bem. Chamadas e jogos podem oscilar quando a rede está ocupada.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(LkSpacing.xxl))

            // O que medimos
            Text(
                text = "O que medimos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(LkSpacing.md))

            // Metric-row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "%.0f Mbps".format(resultado.downloadMbps), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = corDownload)
                    Text(text = "Download · ${veredictoDownload.lowercase()}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    val labelUp = if (resultado.uploadNaoDetectado) "Não detectado" else "%.0f Mbps".format(resultado.uploadMbps)
                    Text(text = labelUp, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = corUpload)
                    Text(text = "Upload · ${veredictoUpload.lowercase()}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }

            Spacer(Modifier.height(LkSpacing.xl))

            // Metric-list
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.HorizontalDivider(color = c.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tempo de resposta", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                    Text("${resultado.latenciaMs} ms · ${veredictoLatencia.lowercase()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = corLatencia)
                }
                androidx.compose.material3.HorizontalDivider(color = c.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Text("Rede ocupada", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                    Text("${resultado.bufferbloatMs ?: "-"} ms · ${veredictoBufferbloat.lowercase()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = corBufferbloat)
                }
                androidx.compose.material3.HorizontalDivider(color = c.outlineVariant)
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(LkSpacing.xxl))

            // Actions
            androidx.compose.material3.Button(
                onClick = onIniciarDiagnosticoGuiado,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors =
                    androidx.compose.material3.ButtonDefaults
                        .buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text("Entender o que está acontecendo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(LkSpacing.sm))
            TextButton(
                onClick = onVerDetalhesTecnicos,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ver detalhes técnicos", style = MaterialTheme.typography.titleMedium, color = c.textTertiary)
            }
        }
    }
}
