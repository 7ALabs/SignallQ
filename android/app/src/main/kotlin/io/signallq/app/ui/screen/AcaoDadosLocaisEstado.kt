package io.signallq.app.ui.screen

// Issue #1670 — DadosLocaisSheet disparava limpar/apagar/resetar em "fire and forget":
// a sheet fechava na hora do toque em "Confirmar", sem esperar a corrotina terminar. Uma
// falha (ex.: escrita no Room falhando) era apresentada como sucesso, o que viola tanto o
// critério de aceite de #1670 ("confirmação destrutiva/progresso/falha parcial") quanto a
// consolidação #1252 item 7 ("falha parcial não pode ser apresentada como sucesso").
//
// Estado único, observado por MainViewModel.dadosLocaisAcaoEstado, cobre as 3 ações da
// mesma sheet (LIMPAR_HISTORICO/APAGAR_DADOS_LOCAIS/RESETAR_APP) — não cria 3 StateFlows
// paralelos pra mesma preocupação.

/** As três ações destrutivas oferecidas pela DadosLocaisSheet. */
enum class TipoAcaoDadosLocais {
    LIMPAR_HISTORICO,
    APAGAR_DADOS_LOCAIS,
    RESETAR_APP,
}

/** Estado observável de uma ação destrutiva disparada pela DadosLocaisSheet. */
sealed class AcaoDadosLocaisEstado {
    data object Ocioso : AcaoDadosLocaisEstado()

    data class EmAndamento(
        val acao: TipoAcaoDadosLocais,
    ) : AcaoDadosLocaisEstado()

    data class Sucesso(
        val acao: TipoAcaoDadosLocais,
    ) : AcaoDadosLocaisEstado()

    data class Falha(
        val acao: TipoAcaoDadosLocais,
        val mensagem: String,
    ) : AcaoDadosLocaisEstado()
}
