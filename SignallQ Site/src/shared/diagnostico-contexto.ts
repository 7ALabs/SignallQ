import type { GenieACSResult } from './genieacs';

// Sinais externos ao browser, já resolvidos em pontos anteriores do fluxo, propagados até o
// classificador de diagnóstico (issue #102). Nenhum sinal aqui dispara nova chamada de rede — o
// resultado do GenieACS já veio da checagem de fibra (Estado 1→2, issue #66) e a flag de massiva já
// veio do check do Estado 0c (issue #95). Campo ausente = sinal desconhecido/indisponível, nunca
// inferido.
export type ContextoDiagnostico = {
  genieacs?: GenieACSResult;
  massiva_ativa?: boolean;
};
