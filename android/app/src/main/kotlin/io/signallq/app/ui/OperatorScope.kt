package io.signallq.app.ui

/**
 * GH#1226 item 9/RF-I — antes a divisão nacionais/regionais na tela Operadoras era uma lista
 * hardcoded de 4 IDs (`idsMajores`) dentro de `OperadoraBottomSheet.kt`, sem nenhum metadado
 * no catálogo em si. Cobertura real (nacional vs. regional/local) agora é dado explícito de
 * cada [ContatoOperadora] em [BancoOperadoras.lista], não uma lista solta na UI.
 */
enum class OperatorScope {
    NATIONAL,
    REGIONAL,
    LOCAL,
    UNKNOWN,
}
