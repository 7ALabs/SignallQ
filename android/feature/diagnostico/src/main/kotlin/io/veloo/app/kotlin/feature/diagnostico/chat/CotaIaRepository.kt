package io.veloo.app.feature.diagnostico.chat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// =============================================================================
// DataStore isolado para cota de IA — não compartilha namespace com
// PreferenciasAppRepository (linkaPreferencias) para evitar acoplamento.
// =============================================================================

private val Context.cotaIaDataStore: DataStore<Preferences> by preferencesDataStore(name = "cota_ia")

// =============================================================================
// Data classes de cota
// =============================================================================

/**
 * Snapshot reativo do estado da cota no ciclo atual.
 *
 * @property analisesNoCiclo Quantas análises foram realizadas no ciclo atual.
 * @property limiteCiclo Limite máximo de análises por ciclo (default 10).
 * @property cicloInicioEpochMs Timestamp de início do ciclo atual; null se ciclo nunca iniciou.
 * @property renovacaoEpochMs Timestamp em que a cota renova; null se ciclo não iniciou.
 */
data class CotaSnapshot(
    val analisesNoCiclo: Int,
    val limiteCiclo: Int,
    val cicloInicioEpochMs: Long?,
    val renovacaoEpochMs: Long?,
) {
    val restantes: Int get() = (limiteCiclo - analisesNoCiclo).coerceAtLeast(0)
    val excedida: Boolean get() = analisesNoCiclo >= limiteCiclo
}

/** Resultado da verificação de cota antes de uma análise. */
sealed class ResultadoCota {
    /** Cota disponível — análise pode prosseguir. */
    data object Disponivel : ResultadoCota()

    /**
     * Cota excedida — análise bloqueada.
     * @property renovacaoEpochMs Quando a cota renova (epoch ms, timezone-agnostic).
     */
    data class Excedida(val renovacaoEpochMs: Long) : ResultadoCota()
}

// =============================================================================
// Repository
// =============================================================================

/**
 * Persistência de cota diária de diagnóstico IA via DataStore isolado.
 *
 * Regra de ciclo rolling 24h:
 * - A cota renova 24h após a **primeira análise** do ciclo (não à meia-noite).
 * - Ao expirar, o contador zera e um novo ciclo começa na próxima análise.
 *
 * O parâmetro [clock] é injetável para facilitar testes unitários sem Thread.sleep.
 *
 * Não usa Hilt diretamente pois featureDiagnostico não tem módulo Hilt ainda;
 * o caller (ViewModel em :app) passa o Context e constrói esta classe.
 */
class CotaIaRepository(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        const val LIMITE_DEFAULT = 10
        const val CICLO_DURACAO_MS = 24L * 60 * 60 * 1000 // 24h em ms

        private val CHAVE_ANALISES = intPreferencesKey("ia_analises_no_ciclo")
        private val CHAVE_CICLO_INICIO = longPreferencesKey("ia_ciclo_inicio_epoch_ms")
        private val CHAVE_LIMITE = intPreferencesKey("ia_limite_ciclo")
    }

    // -------------------------------------------------------------------------
    // Leitura reativa
    // -------------------------------------------------------------------------

    /**
     * Fluxo reativo do snapshot de cota.
     * [CotaSnapshot.renovacaoEpochMs] é calculado on-the-fly a partir de
     * [cicloInicioEpochMs] + [CICLO_DURACAO_MS] — não persiste separado
     * para evitar inconsistência.
     */
    fun observarCota(): Flow<CotaSnapshot> = context.cotaIaDataStore.data.map { prefs ->
        val analises = prefs[CHAVE_ANALISES] ?: 0
        val limite = prefs[CHAVE_LIMITE] ?: LIMITE_DEFAULT
        val cicloInicio = prefs[CHAVE_CICLO_INICIO]
        val renovacao = cicloInicio?.let { it + CICLO_DURACAO_MS }
        CotaSnapshot(
            analisesNoCiclo = analises,
            limiteCiclo = limite,
            cicloInicioEpochMs = cicloInicio,
            renovacaoEpochMs = renovacao,
        )
    }

    // -------------------------------------------------------------------------
    // Verificação e registro
    // -------------------------------------------------------------------------

    /**
     * Verifica se há cota disponível para uma nova análise.
     *
     * Lógica:
     * 1. Se ciclo nunca iniciou → Disponivel.
     * 2. Se ciclo expirou (>= 24h desde início) → resetar e retornar Disponivel.
     * 3. Se análises >= limite → Excedida com timestamp de renovação.
     * 4. Senão → Disponivel.
     */
    suspend fun podeAnalisar(): ResultadoCota {
        val prefs = context.cotaIaDataStore.data.first()
        val analises = prefs[CHAVE_ANALISES] ?: 0
        val cicloInicio = prefs[CHAVE_CICLO_INICIO]
        val limite = prefs[CHAVE_LIMITE] ?: LIMITE_DEFAULT

        // Ciclo nunca iniciou
        if (cicloInicio == null) return ResultadoCota.Disponivel

        val agora = clock()
        val cicloExpirou = (agora - cicloInicio) >= CICLO_DURACAO_MS

        // Ciclo expirado → resetar e liberar
        if (cicloExpirou) {
            resetarCicloInterno()
            return ResultadoCota.Disponivel
        }

        // Dentro do ciclo → verificar se excedeu
        return if (analises >= limite) {
            ResultadoCota.Excedida(renovacaoEpochMs = cicloInicio + CICLO_DURACAO_MS)
        } else {
            ResultadoCota.Disponivel
        }
    }

    /**
     * Registra que uma análise foi realizada.
     *
     * - Se ciclo nunca iniciou ou expirou → inicia novo ciclo com analises=1.
     * - Se dentro do ciclo → incrementa analises.
     */
    suspend fun registrarAnalise() {
        val prefs = context.cotaIaDataStore.data.first()
        val cicloInicio = prefs[CHAVE_CICLO_INICIO]
        val analises = prefs[CHAVE_ANALISES] ?: 0
        val agora = clock()

        val deveIniciarNovoCiclo = cicloInicio == null ||
            (agora - cicloInicio) >= CICLO_DURACAO_MS

        if (deveIniciarNovoCiclo) {
            context.cotaIaDataStore.edit { ds ->
                ds[CHAVE_CICLO_INICIO] = agora
                ds[CHAVE_ANALISES] = 1
            }
        } else {
            context.cotaIaDataStore.edit { ds ->
                ds[CHAVE_ANALISES] = analises + 1
            }
        }
    }

    /**
     * Reseta a cota para o estado inicial (útil para testes e modo debug).
     * Remove ciclo e zera análises — próxima chamada a [podeAnalisar] retorna Disponivel.
     */
    suspend fun resetar() {
        context.cotaIaDataStore.edit { ds ->
            ds.remove(CHAVE_CICLO_INICIO)
            ds.remove(CHAVE_ANALISES)
            ds.remove(CHAVE_LIMITE)
        }
    }

    // -------------------------------------------------------------------------
    // Privados
    // -------------------------------------------------------------------------

    private suspend fun resetarCicloInterno() {
        context.cotaIaDataStore.edit { ds ->
            ds.remove(CHAVE_CICLO_INICIO)
            ds[CHAVE_ANALISES] = 0
        }
    }
}
