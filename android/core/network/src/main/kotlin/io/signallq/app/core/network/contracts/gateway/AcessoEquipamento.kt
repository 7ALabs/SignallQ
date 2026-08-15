package io.signallq.app.core.network.contracts.gateway

/**
 * Nível de acesso do SignallQ ao equipamento local (ONT/roteador) numa leitura
 * — GH#934, Fase 5 MD3 To-Be ("Equipamento de internet").
 *
 * Enum único e centralizado (fora de qualquer feature module, para poder ser
 * reutilizado por outras superfícies que mostrem acesso a gateway — ex.: um
 * futuro sheet "Roteador/Gateway" na Home) — nunca reimplementar esta mesma
 * decisão em outro lugar do app.
 *
 * A UI deve derivar este enum a partir de sinais reais (estado de
 * conexão/login, capabilities declaradas pelo driver, warnings do snapshot) —
 * nunca a partir de suposição/vendor hardcoded.
 */
enum class AcessoEquipamento {
    /** Login funcionou e todas as seções suportadas pelo driver vieram preenchidas. */
    LEITURA_COMPLETA,

    /** Login funcionou, mas ao menos uma seção suportada pelo driver não veio preenchida nesta captura. */
    LEITURA_PARCIAL,

    /**
     * Equipamento identificado na rede, mas sem driver capaz de autenticar/ler
     * dados dele (fabricante/firmware não suportado) — nunca finge dado que
     * não tem.
     */
    SOMENTE_IDENTIFICACAO,

    /**
     * Leitura completa E o driver declara capability de ação de gerência
     * (ex.: reiniciar o equipamento) — superset de [LEITURA_COMPLETA].
     */
    GERENCIAMENTO_DISPONIVEL,

    /**
     * Já houve acesso funcional com estas credenciais antes, mas a leitura
     * atual falhou por motivo de comunicação/sessão (não é indício de senha
     * errada) — retry costuma resolver sozinho.
     */
    SESSAO_EXPIRADA,

    /**
     * Nunca houve login bem-sucedido com as credenciais atuais (ou elas nunca
     * foram configuradas) — usuário precisa revisar usuário/senha.
     */
    CREDENCIAIS_NECESSARIAS,
}
