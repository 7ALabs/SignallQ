package io.veloo.app.core.permissions

interface GerenciadorPermissoesRede {
    fun avaliar(): SnapshotPermissoesRede

    fun listarPermissoesPendentes(): List<String>
}

