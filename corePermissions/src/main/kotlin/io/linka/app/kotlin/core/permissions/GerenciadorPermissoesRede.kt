package io.linka.app.kotlin.core.permissions

interface GerenciadorPermissoesRede {
    fun avaliar(): SnapshotPermissoesRede

    fun listarPermissoesPendentes(): List<String>
}

