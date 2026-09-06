package io.signallq.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * GH#1707 (Task 2.0.09e, épico #1647) — [MedicaoDao.buscarUltimaComparavelNaRede] é a query que
 * decide se um reteste tem par comparável: só medições concluídas na MESMA rede, anteriores no
 * tempo. Spec §8.8 — "reteste compara condições equivalentes ou declara limite" — nunca compara
 * redes diferentes com aviso.
 */
@RunWith(AndroidJUnit4::class)
class MedicaoDaoNetworkIdTest {
    private lateinit var db: SignallQDatabase
    private lateinit var dao: MedicaoDao

    @Before
    fun criarBanco() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SignallQDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.medicaoDao()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    private fun medicao(
        id: String = UUID.randomUUID().toString(),
        timestampEpochMs: Long,
        networkId: String?,
        status: String = "completed",
    ) = MedicaoEntity(
        id = id,
        timestampEpochMs = timestampEpochMs,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = "fast",
        specVersion = "3",
        downloadMbps = 100.0,
        uploadMbps = 50.0,
        latencyMs = 20.0,
        jitterMs = 5.0,
        perdaPercentual = 0.0,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        status = status,
        executionId = "exec-$id",
        networkId = networkId,
    )

    @Test
    fun encontraUltimaMedicaoComparavelNaMesmaRede() =
        runTest {
            dao.salvar(medicao(id = "original", timestampEpochMs = 1_000, networkId = "wifi-bssid:aa"))
            dao.salvar(medicao(id = "reteste", timestampEpochMs = 2_000, networkId = "wifi-bssid:aa"))

            val comparavel =
                dao.buscarUltimaComparavelNaRede(
                    networkId = "wifi-bssid:aa",
                    excluirId = "reteste",
                    antesDoTimestamp = 2_000,
                )

            assertEquals("original", comparavel?.id)
        }

    @Test
    fun redeDiferenteNaoEComparavel() =
        runTest {
            dao.salvar(medicao(id = "original", timestampEpochMs = 1_000, networkId = "wifi-bssid:aa"))
            dao.salvar(medicao(id = "reteste", timestampEpochMs = 2_000, networkId = "wifi-bssid:bb"))

            val comparavel =
                dao.buscarUltimaComparavelNaRede(
                    networkId = "wifi-bssid:bb",
                    excluirId = "reteste",
                    antesDoTimestamp = 2_000,
                )

            assertNull("rede diferente da analise original nao pode virar par comparavel", comparavel)
        }

    @Test
    fun medicaoSemNetworkIdNuncaEComparavel() =
        runTest {
            dao.salvar(medicao(id = "original-sem-rede", timestampEpochMs = 1_000, networkId = null))
            dao.salvar(medicao(id = "reteste-sem-rede", timestampEpochMs = 2_000, networkId = null))

            // Sem networkId pra consultar (chamador nao tem o que comparar) — a query exige um
            // valor nao-nulo, entao o cenario real e o caller nunca chamar a query. Aqui
            // confirmamos que mesmo se chamada com string vazia/nao correspondente, nada retorna
            // por engano (nunca um match espurio contra NULL).
            val comparavel =
                dao.buscarUltimaComparavelNaRede(
                    networkId = "",
                    excluirId = "reteste-sem-rede",
                    antesDoTimestamp = 2_000,
                )

            assertNull(comparavel)
        }

    @Test
    fun medicaoNaoConcluidaNaMesmaRedeNaoEComparavel() =
        runTest {
            dao.salvar(medicao(id = "original-parcial", timestampEpochMs = 1_000, networkId = "wifi-bssid:aa", status = "partial"))
            dao.salvar(medicao(id = "reteste", timestampEpochMs = 2_000, networkId = "wifi-bssid:aa"))

            val comparavel =
                dao.buscarUltimaComparavelNaRede(
                    networkId = "wifi-bssid:aa",
                    excluirId = "reteste",
                    antesDoTimestamp = 2_000,
                )

            assertNull("medicao parcial nao e base valida de comparacao", comparavel)
        }

    @Test
    fun escolheAMaisRecenteQuandoHaVariasComparaveis() =
        runTest {
            dao.salvar(medicao(id = "mais-antiga", timestampEpochMs = 500, networkId = "wifi-bssid:aa"))
            dao.salvar(medicao(id = "mais-recente", timestampEpochMs = 1_500, networkId = "wifi-bssid:aa"))
            dao.salvar(medicao(id = "reteste", timestampEpochMs = 2_000, networkId = "wifi-bssid:aa"))

            val comparavel =
                dao.buscarUltimaComparavelNaRede(
                    networkId = "wifi-bssid:aa",
                    excluirId = "reteste",
                    antesDoTimestamp = 2_000,
                )

            assertEquals("mais-recente", comparavel?.id)
        }
}
