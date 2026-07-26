package io.signallq.app.core.datastore

/**
 * Combinação jogo+device salva como padrão do Modo gamer (Feature #550, issue #1476) —
 * "Salvar como padrão" na Etapa 3/3. IDs crus (não depende do enum `CategoriaJogoModoGamer`/
 * `DeviceJogo` de `core/diagnostico` — mesmo princípio de baixo acoplamento de
 * [ConnectionProfilePersistido], que também guarda strings livres em vez de importar tipos
 * de outro módulo).
 *
 * [jogoId] nulo com [categoriaFallback] preenchida = o usuário salvou um jogo fora do
 * catálogo (categoria genérica escolhida na tela de fallback). Os dois nunca ficam nulos ao
 * mesmo tempo — quem grava garante isso.
 */
data class ModoGamerPadraoPersistido(
    val jogoId: String?,
    val categoriaFallback: String?,
    val deviceId: String,
)
