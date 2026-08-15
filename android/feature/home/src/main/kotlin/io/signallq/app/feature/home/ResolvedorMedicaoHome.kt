package io.signallq.app.feature.home

/**
 * GH#1223 RF-02/03 (auditoria da tela Início) — indica se a medição exibida veio da execução
 * ATUAL (rodando/concluída agora, [io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest])
 * ou de um resultado ANTERIOR salvo no histórico — nunca uma mistura dos dois.
 */
enum class OrigemMedicaoHome {
    ATUAL,
    ANTERIOR,
}

/**
 * Entrada genérica pra [ResolvedorMedicaoHome.resolver] — representa "uma medição, com todos
 * os campos vindos da MESMA execução". Não importa tipos de `feature/speedtest`
 * (`ResultadoSpeedtest`/`MeasurementStatus`) nem de `core/database` (`MedicaoEntity`)
 * propositalmente: a lei de dependência do repo proíbe `feature/home` → `feature/speedtest`.
 * Quem adapta os tipos reais pra esta struct genérica é o `:app` (`HomeScreen.kt`), que já
 * depende dos dois módulos.
 *
 * @param utilizavel true quando esta entrada deve ser considerada exibível como resultado —
 * ex.: `ResultadoSpeedtest.status == MeasurementStatus.COMPLETE`, ou
 * `MedicaoEntity.status == "completed"`. Uma entrada não-utilizável nunca é escolhida por
 * [ResolvedorMedicaoHome.resolver], mesmo que os campos numéricos estejam presentes.
 */
data class MetricasMedicaoHome(
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latenciaMs: Double?,
    val jitterMs: Double?,
    val perdaPercentual: Double?,
    val timestampEpochMs: Long?,
    val connectionType: String?,
    val ssid: String?,
    val vereditoGamer: String?,
    val gargaloPrimario: String?,
    val utilizavel: Boolean,
)

/** Resultado de [ResolvedorMedicaoHome.resolver] — uma única medição coerente, com a origem
 *  explícita pra permitir a UI mostrar contexto ("Resultado anterior · Wi-Fi · há 2h", RF-06). */
data class ResolvedHomeMeasurement(
    val metricas: MetricasMedicaoHome,
    val origem: OrigemMedicaoHome,
)

/**
 * GH#1223 item 1/RF-02/RF-03 — antes a Home montava um resultado "Frankenstein": download/
 * upload/timestamp vinham de uma cadeia de fallback (`resultado atual → history.firstOrNull()`)
 * e latência/jitter/perda vinham de OUTRA cadeia (`resultado atual → ultimaMedicao`) —
 * completamente independentes. Isso permitia exibir download do teste A com latência do
 * teste B, ambos com o timestamp do teste A. Esta função nunca mistura: ou usa [atual] por
 * inteiro (quando utilizável), ou usa [anterior] por inteiro, ou não mostra nada.
 */
object ResolvedorMedicaoHome {
    fun resolver(
        atual: MetricasMedicaoHome?,
        anterior: MetricasMedicaoHome?,
    ): ResolvedHomeMeasurement? =
        when {
            atual != null && atual.utilizavel -> ResolvedHomeMeasurement(atual, OrigemMedicaoHome.ATUAL)
            anterior != null && anterior.utilizavel -> ResolvedHomeMeasurement(anterior, OrigemMedicaoHome.ANTERIOR)
            else -> null
        }
}
