package io.signallq.app

// Issue #1671 (Task 2.0.23, épico #1647) — decisão pura reutilizada pelas 3 permissões
// opcionais contextuais (localização, telefonia, notificação): cada uma é solicitada só no
// ponto de uso, nunca em lote no onboarding. Extraída para função pura testável em vez de
// repetir o mesmo if/else 3x dentro de MainActivity.

/** O que fazer ao entrar numa funcionalidade que depende de uma permissão opcional. */
enum class DecisaoPermissaoContextual {
    /** Permissão já concedida — segue direto para a funcionalidade. */
    JA_CONCEDIDA,

    /** Ainda não concedida, mas o SO pode perguntar de novo — abre o diálogo real do sistema. */
    SOLICITAR,

    /** Negada permanentemente nesta sessão — reperguntar não faz nada, manda para Ajustes do app. */
    ABRIR_AJUSTES,
}

/**
 * @param concedida true se `ContextCompat.checkSelfPermission` já retorna GRANTED.
 * @param bloqueadaPermanentemente true se uma tentativa anterior, nesta sessão, já resultou em
 *   negação permanente (`shouldShowRequestPermissionRationale == false` após pedir uma vez).
 */
fun decidirPermissaoContextual(
    concedida: Boolean,
    bloqueadaPermanentemente: Boolean,
): DecisaoPermissaoContextual =
    when {
        concedida -> DecisaoPermissaoContextual.JA_CONCEDIDA
        bloqueadaPermanentemente -> DecisaoPermissaoContextual.ABRIR_AJUSTES
        else -> DecisaoPermissaoContextual.SOLICITAR
    }
