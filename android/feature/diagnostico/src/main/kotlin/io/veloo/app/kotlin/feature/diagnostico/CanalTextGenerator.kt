package io.signallq.app.feature.diagnostico

private const val LIMITE_REDES_QUASE_VAZIA = 5

object CanalTextGenerator {

    fun gerarTexto(
        snapshot: SnapshotEspectroCanal,
        strings: CanalStrings = CanalStrings.PadraoPortugues,
    ): String {
        val totalRedes = snapshot.dadosPorCanal.sumOf { it.count }
        val canaisComRedes = snapshot.dadosPorCanal.filter { it.count > 0 }
        val canalAtualDado = snapshot.dadosPorCanal.find { it.ehCanalAtual }

        if (totalRedes == 0) return strings.semDados()

        if (totalRedes < LIMITE_REDES_QUASE_VAZIA) {
            return strings.bandaQuaseVazia(snapshot.banda)
        }

        val canalAtualCongestionado = canalAtualDado?.nivel == NivelCongestionamento.congestionado
        val canalRec = snapshot.canalRecomendado
        val temCanalAlternativo = canalRec != null && canalRec != snapshot.canalAtual

        // Congestionamento por canal: ação mais direta para o usuário
        if (canalAtualCongestionado && temCanalAlternativo) {
            return strings.canalAtualCongestionado(snapshot.canalAtual!!, canalRec!!)
        }

        // Faixa congestionada: problema generalizado na banda
        val totalComRedes = canaisComRedes.size
        val congestionados = canaisComRedes.count { it.nivel == NivelCongestionamento.congestionado }
        if (totalComRedes > 0 && congestionados * 2 > totalComRedes) {
            return strings.bandaCongestionada(snapshot.banda)
        }

        // Canal atual já livre: mesmo havendo alternativa, não há motivo real pra recomendar troca
        if (canalAtualDado?.nivel == NivelCongestionamento.livre && temCanalAlternativo) {
            return strings.canalAtualLivreComAlternativa(snapshot.canalAtual!!, snapshot.banda)
        }

        // Canal recomendado: sugere o melhor canal disponível
        if (canalRec != null) {
            val canalRecDado = snapshot.dadosPorCanal.find { it.ehCanalRecomendado }
            val textoBase = if (canalRecDado?.nivel == NivelCongestionamento.livre) {
                strings.canalRecomendadoLivre(canalRec, snapshot.banda)
            } else {
                strings.canalRecomendadoModerado(canalRec, snapshot.banda)
            }
            return if (snapshot.motivoRecomendacao != null) {
                "$textoBase\n${snapshot.motivoRecomendacao}"
            } else {
                textoBase
            }
        }

        return strings.bandaQuaseVazia(snapshot.banda)
    }
}
