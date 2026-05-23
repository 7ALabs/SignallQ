package io.linka.app.kotlin.core.permissions

import android.content.Context

object CorePermissionsModulo {
    fun criarGerenciadorPermissoesRede(context: Context): GerenciadorPermissoesRede {
        return GerenciadorPermissoesRedeAndroid(context)
    }
}
