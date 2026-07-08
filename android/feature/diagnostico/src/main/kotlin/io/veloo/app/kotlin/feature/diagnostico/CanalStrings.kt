package io.signallq.app.feature.diagnostico

data class CanalStrings(
    val bandaCongestionada: (banda: String) -> String,
    val bandaQuaseVazia: (banda: String) -> String,
    val canalAtualCongestionado: (canalAtual: Int, canalRecomendado: Int) -> String,
    val canalRecomendadoLivre: (canal: Int, banda: String) -> String,
    val canalRecomendadoModerado: (canal: Int, banda: String) -> String,
    val canalAtualLivreComAlternativa: (canalAtual: Int, banda: String) -> String,
    val semDados: () -> String,
) {
    companion object {
        val PadraoPortugues = CanalStrings(
            bandaCongestionada = { banda ->
                "Sua faixa $banda está congestionada. Há muitas redes competindo pelos mesmos canais, o que pode causar interferência e lentidão."
            },
            bandaQuaseVazia = { banda ->
                "Sua faixa $banda está quase vazia. Há poucas redes por aqui — ótima condição para uma conexão estável."
            },
            canalAtualCongestionado = { canalAtual, canalRecomendado ->
                "O canal $canalAtual que você usa está muito congestionado. Migrar para o canal $canalRecomendado pode melhorar significativamente sua conexão."
            },
            canalRecomendadoLivre = { canal, banda ->
                "O canal $canal é o mais livre da faixa $banda. Migrar para ele pode melhorar sua conexão."
            },
            canalRecomendadoModerado = { canal, banda ->
                "O canal $canal tem o menor congestionamento na faixa $banda. Considere migrar para melhorar a estabilidade."
            },
            canalAtualLivreComAlternativa = { canalAtual, banda ->
                "Seu canal $canalAtual já está livre na faixa $banda. Você está bem posicionado para uma conexão estável — não é necessário trocar de canal."
            },
            semDados = {
                "Não há dados suficientes para analisar os canais. Tente um novo scan."
            },
        )
    }
}
