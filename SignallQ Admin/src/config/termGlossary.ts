/**
 * GH#1341 — item 2.2.2 do plano de UX Google Play/Firebase: glossário compartilhado pro
 * `TermHint`. Mesmo termo usado em telas diferentes (ex. "ANR rate") aponta pro mesmo
 * verbete — evita descrição divergente entre fontes.
 */
export const TERM_GLOSSARY: Record<string, string> = {
  anrRate:
    "Percentual de sessões em que o app travou sem responder por tempo suficiente para o Android oferecer a opção de fechá-lo. Quanto menor, melhor — é o critério que o próprio Google Play usa para penalizar a visibilidade do app na loja.",
  crashRate:
    "Percentual de sessões em que o app fechou sozinho por um erro não tratado (crash). Diferente do ANR rate: aqui o app trava e fecha; no ANR, o app trava mas continua respondendo lentamente até o usuário forçar o fechamento. Os dois vêm da mesma fonte (Play Developer Reporting API) e são medidos separadamente.",
  handlingStatus:
    "Como esta tela classifica cada avaliação: pendente (nota baixa, sem resposta do time), atenção (nota mediana, sem resposta), respondida (já tem resposta do time). Nota alta sem resposta não precisa de badge — não é obrigatório responder toda avaliação positiva.",
  remoteConfigParameter:
    "Parâmetro de configuração remota do Firebase — um valor que pode ser alterado sem publicar uma nova versão do app (ex.: ligar/desligar uma feature, trocar um limiar). Hoje só mostramos quantos parâmetros existem e seus nomes, não o valor de cada um.",
  appCheckState:
    "Indica se este provedor de verificação de app (ex.: Play Integrity) está ativo bloqueando requisições suspeitas (ENFORCED) ou só registrando sem bloquear (UNENFORCED) — configurado no Firebase Console, não neste painel.",
};
