package io.signallq.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity

/**
 * Resolve a identidade de uma operadora local-primeiro (GH#970). [resolveLocal] e puro/
 * sincrono (nivel 1 de [io.signallq.app.ui.OperadoraDirectoryResolver]) — quando ha match,
 * o resultado ja esta pronto na primeira composicao, **sem** flicker/loading pras ~12
 * operadoras catalogadas localmente (nenhuma mudanca de comportamento pra elas).
 *
 * So quando o catalogo local nao acha e ha nome pra buscar, [resolveRemoteOrFallback]
 * (cadeia completa: local -> diretorio remoto -> fallback generico) roda numa corrotina —
 * a composicao nunca trava esperando rede; o retorno fica `null` ate a corrotina concluir
 * (chamador decide o placeholder, normalmente nenhum badge ou o badge generico).
 */
@Composable
fun rememberResolvedOperadoraIdentity(
    ispNomeBruto: String?,
    viaMovel: Boolean,
    resolveLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveRemoteOrFallback: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
): ResolvedOperadoraIdentity? {
    val local = remember(ispNomeBruto, viaMovel) { resolveLocal(ispNomeBruto, viaMovel) }
    if (local != null) return local
    if (ispNomeBruto.isNullOrBlank()) return null

    var resolvido by remember(ispNomeBruto, viaMovel) { mutableStateOf<ResolvedOperadoraIdentity?>(null) }
    LaunchedEffect(ispNomeBruto, viaMovel) {
        resolvido = resolveRemoteOrFallback(ispNomeBruto, viaMovel)
    }
    return resolvido
}

/** Mesma estrategia local-primeiro de [rememberResolvedOperadoraIdentity], para contato. */
@Composable
fun rememberResolvedOperadoraContact(
    ispNomeBruto: String?,
    viaMovel: Boolean,
    resolveLocal: (String?, Boolean) -> ResolvedOperadoraContact?,
    resolveRemoteOrFallback: suspend (String?, Boolean) -> ResolvedOperadoraContact,
): ResolvedOperadoraContact? {
    val local = remember(ispNomeBruto, viaMovel) { resolveLocal(ispNomeBruto, viaMovel) }
    if (local != null) return local
    if (ispNomeBruto.isNullOrBlank()) return null

    var resolvido by remember(ispNomeBruto, viaMovel) { mutableStateOf<ResolvedOperadoraContact?>(null) }
    LaunchedEffect(ispNomeBruto, viaMovel) {
        resolvido = resolveRemoteOrFallback(ispNomeBruto, viaMovel)
    }
    return resolvido
}
