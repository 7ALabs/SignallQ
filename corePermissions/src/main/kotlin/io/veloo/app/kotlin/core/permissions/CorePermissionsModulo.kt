package io.veloo.app.core.permissions

import android.content.Context

object CorePermissionsModulo {
    fun criarGerenciadorPermissoesRede(context: Context): GerenciadorPermissoesRede {
        return GerenciadorPermissoesRedeAndroid(context)
    }
}
