/**
 * GH#1341 — item 2.2.2 do plano de UX Google Play/Firebase: glossário compartilhado pro
 * `TermHint`. Mesmo termo usado em telas diferentes (ex. "ANR rate") aponta pro mesmo
 * verbete — evita descrição divergente entre fontes.
 */
export const TERM_GLOSSARY: Record<string, string> = {
  anrRate:
    "Percentual de sessões em que o app travou sem responder por tempo suficiente para o Android oferecer a opção de fechá-lo. Quanto menor, melhor — é o critério que o próprio Google Play usa para penalizar a visibilidade do app na loja.",
  handlingStatus:
    "Como esta tela classifica cada avaliação: pendente (nota baixa, sem resposta do time), atenção (nota mediana, sem resposta), respondida (já tem resposta do time). Nota alta sem resposta não precisa de badge — não é obrigatório responder toda avaliação positiva.",
};
