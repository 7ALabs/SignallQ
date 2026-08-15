package io.signallq.app.core.permissions

interface GerenciadorPermissoesRede {
    fun avaliar(): SnapshotPermissoesRede

    fun listarPermissoesPendentes(): List<String>
}

