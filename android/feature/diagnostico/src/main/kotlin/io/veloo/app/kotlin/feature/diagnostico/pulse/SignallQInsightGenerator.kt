package io.veloo.app.feature.diagnostico.pulse

/**
 * Gera snippets conversacionais locais a partir dos dados medidos já disponíveis
 * na [IntelligentDiagnosticSession], antes de a IA (Gemma) responder.
 *
 * Regras:
 * - Usa APENAS dados que existem na sessão — sem inventar valores.
 * - Mínimo 2 insights, máximo 4.
 * - Se não há dados suficientes para um insight, esse insight é omitido.
 */
object SignallQInsightGenerator {

    fun generate(session: IntelligentDiagnosticSession): List<String> {
        val insights = mutableListOf<String>()

        // --- Download ---
        session.speedtestDownloadMbps?.let { dl ->
            val comentario = when {
                dl < 10.0 -> "Velocidade abaixo do esperado para a maioria dos serviços."
                dl < 50.0 -> "Adequado para streaming e navegação."
                else -> "Velocidade boa para uso doméstico."
            }
            insights += "Download de ${"%.0f".format(dl)} Mbps medido. $comentario"
        }

        // --- Latência ---
        session.speedtestLatencyMs?.let { lat ->
            val comentario = when {
                lat < 20.0 -> "excelente para jogos e chamadas."
                lat < 60.0 -> "dentro do esperado para navegação."
                else -> "pode causar lentidão em chamadas e jogos."
            }
            insights += "Latência de ${"%.0f".format(lat)} ms — $comentario"
        }

        // --- Wi-Fi (só se tiver frequência e RSSI) ---
        val freq = session.wifiFrequencyMhz
        val rssi = session.wifiRssiDbm
        if (freq != null && rssi != null) {
            val band = if (freq >= 5000) "5 GHz" else "2,4 GHz"
            val intensidade = when {
                rssi > -60 -> "boa"
                rssi > -75 -> "razoável"
                else -> "fraca"
            }
            insights += "Conectado em Wi-Fi $band com intensidade $intensidade."
        }

        // --- Upload (se tiver download mas não houver já 4 insights) ---
        if (insights.size < 4) {
            session.speedtestUploadMbps?.let { ul ->
                val comentario = when {
                    ul < 5.0 -> "Upload limitado — pode afetar videochamadas e uploads de arquivos."
                    ul < 20.0 -> "Upload adequado para uso cotidiano."
                    else -> "Upload satisfatório."
                }
                insights += "Upload de ${"%.0f".format(ul)} Mbps. $comentario"
            }
        }

        // Garante mínimo de 2 e máximo de 4
        return insights.take(4).let { list ->
            if (list.size >= 2) list else list
        }
    }
}
