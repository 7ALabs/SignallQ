package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.EstadoScanDispositivos
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.feature.devices.TipoDispositivo
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de caracterização da issue #1663 (épico #1647, Task 2.0.15) — provam que a extração de
 * `DispositivosScreen.kt` (1381 linhas, dívida crítica §7 da regra de higiene) em
 * `DispositivosScreen.kt` (composição), `DispositivosLista.kt` (lista/empty states) e
 * `DispositivoDetalheSheet.kt` (sheets de detalhe) não muda nenhum comportamento observável.
 *
 * Cobre os estados citados na issue: varredura em andamento, lista vazia, permissão/rede
 * incompatível, erro parcial, "este aparelho", dispositivo desconhecido e dispositivo apelidado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DispositivosScreenExtracaoCaracterizacaoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotRedeWifi =
        SnapshotRede(
            estadoConexao = EstadoConexao.wifi,
            conectado = true,
            timestampEpochMs = 0L,
            wifiLinkSnapshot = null,
            privateDnsAtivo = false,
            privateDnsHostname = null,
            dnsServidores = emptyList(),
        )

    private val snapshotRedeMovel =
        SnapshotRede(
            estadoConexao = EstadoConexao.movel,
            conectado = true,
            timestampEpochMs = 0L,
            wifiLinkSnapshot = null,
            privateDnsAtivo = false,
            privateDnsHostname = null,
            dnsServidores = emptyList(),
        )

    private fun snapshotDevices(
        estado: EstadoScanDispositivos,
        dispositivos: List<DispositivoRede> = emptyList(),
        progresso: Int = 0,
        erro: String? = null,
    ) = SnapshotScanDispositivos(
        estado = estado,
        progressoPercentual = progresso,
        dispositivos = dispositivos,
        erroMensagem = erro,
    )

    @Test
    fun `varredura em andamento mostra estado de procurando`() {
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices = snapshotDevices(EstadoScanDispositivos.varrendo, progresso = 40),
                    snapshotRede = snapshotRedeWifi,
                    onRefresh = {},
                    apelidos = emptyMap(),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Procurando dispositivos...").assertExists()
    }

    @Test
    fun `lista vazia sem erro mostra nenhum dispositivo encontrado`() {
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices = snapshotDevices(EstadoScanDispositivos.concluido),
                    snapshotRede = snapshotRedeWifi,
                    onRefresh = {},
                    apelidos = emptyMap(),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nenhum dispositivo encontrado").assertExists()
    }

    @Test
    fun `rede incompativel sem wifi mas com dados moveis mostra fallback especifico`() {
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices = snapshotDevices(EstadoScanDispositivos.idle),
                    snapshotRede = snapshotRedeMovel,
                    onRefresh = {},
                    apelidos = emptyMap(),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dispositivos da rede").assertExists()
    }

    @Test
    fun `erro parcial mostra aviso de resultado parcial sem esconder a lista`() {
        val dispositivo =
            DispositivoRede(
                id = "1",
                ip = "192.168.0.10",
                mac = "aa:bb:cc:dd:ee:ff",
                nomeExibicao = "Notebook da sala",
                fonteNome = "mdns",
                tipoDispositivo = TipoDispositivo.computador,
            )
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices =
                        snapshotDevices(
                            EstadoScanDispositivos.concluidoParcial,
                            dispositivos = listOf(dispositivo),
                        ),
                    snapshotRede = snapshotRedeWifi,
                    onRefresh = {},
                    apelidos = emptyMap(),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Resultado parcial — uma etapa da varredura não respondeu").assertExists()
        composeRule.onNodeWithText("Notebook da sala").assertExists()
    }

    @Test
    fun `este aparelho aparece com subtitulo proprio`() {
        val esteDispositivo =
            DispositivoRede(
                id = "self",
                ip = "192.168.0.5",
                mac = "11:22:33:44:55:66",
                nomeExibicao = "Meu celular",
                fonteNome = "arp",
                tipoDispositivo = TipoDispositivo.smartphone,
                esteDispositivo = true,
            )
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices =
                        snapshotDevices(EstadoScanDispositivos.concluido, dispositivos = listOf(esteDispositivo)),
                    snapshotRede = snapshotRedeWifi,
                    onRefresh = {},
                    apelidos = emptyMap(),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("192.168.0.5 · Este aparelho").assertExists()
    }

    @Test
    fun `dispositivo desconhecido sem fabricante recebe rotulo generico honesto`() {
        val desconhecido =
            DispositivoRede(
                id = "unknown",
                ip = "192.168.0.20",
                mac = null,
                nomeExibicao = "192.168.0.20",
                fonteNome = "subnet",
                fabricante = null,
                tipoDispositivo = TipoDispositivo.desconhecido,
            )
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices =
                        snapshotDevices(EstadoScanDispositivos.concluido, dispositivos = listOf(desconhecido)),
                    snapshotRede = snapshotRedeWifi,
                    onRefresh = {},
                    apelidos = emptyMap(),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        // Regra de produto (issue #1663, decisão do Luiz 2026-08-19): dispositivo não
        // confirmado usa rótulo genérico honesto — nunca chuta marca/tipo.
        composeRule.onNodeWithText("Dispositivo desconhecido").assertExists()
    }

    @Test
    fun `dispositivo apelidado mostra o apelido no lugar do nome descoberto`() {
        val dispositivo =
            DispositivoRede(
                id = "2",
                ip = "192.168.0.30",
                mac = "de:ad:be:ef:00:01",
                nomeExibicao = "android-abc123",
                fonteNome = "arp",
                tipoDispositivo = TipoDispositivo.smartphone,
            )
        composeRule.setContent {
            SignallQTheme {
                DispositivosScreen(
                    snapshotDevices =
                        snapshotDevices(EstadoScanDispositivos.concluido, dispositivos = listOf(dispositivo)),
                    snapshotRede = snapshotRedeWifi,
                    onRefresh = {},
                    apelidos = mapOf("de:ad:be:ef:00:01" to "Celular da Ana"),
                    onSalvarApelido = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Celular da Ana").assertExists()
    }
}
