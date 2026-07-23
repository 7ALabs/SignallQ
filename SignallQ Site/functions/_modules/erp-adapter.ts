import type { ChamadoCanônico } from '@/shared/chamado';

// Contrato estável entre o core (worker de diagnóstico) e qualquer ERP concreto (ADR 0003).
// O core nunca importa um adapter concreto (sgp, synsuite, ixc, hubspot...) — só esta interface e
// o resolver do registry (erp-registry.ts). Novo ERP = novo módulo implementando ErpAdapter, sem
// tocar em core nem nos outros adapters.

export type ErpTipo = 'sgp';

// Escopos por ação (issue #28) — menor privilégio: a credencial de um tenant concede acesso a ações
// específicas do ERP, não "tudo ou nada". Fechado por propósito, mesmo padrão de ErpTipo: uma ação
// nova no adapter (ex.: consultar título, atualizar OS) ganha seu próprio literal aqui, e o
// TypeScript força tratar o caso em qualquer switch exaustivo sobre escopo.
export type ErpEscopo = 'erp:ticket:write' | 'erp:read';

// Escopo exigido para criar chamado (ação canônica desta interface) — nomeado explicitamente para
// que o chamador (handleErpChamado) valide contra o registry ANTES de invocar o adapter, sem
// precisar hardcodar a string em mais de um lugar.
export const ESCOPO_CRIAR_CHAMADO: ErpEscopo = 'erp:ticket:write';

// Credenciais resolvidas para a chamada — hoje espelha o shape de env var por tenant (SgpEnv),
// amanhã pode vir de secret cifrado (#34) sem mudar a interface do adapter.
export type ErpCredenciais = {
  baseUrl: string;
  token: string;
  app: string;
};

export type ResultadoChamado = {
  protocolo: string;
};

export interface ErpAdapter {
  readonly tipo: ErpTipo;
  criarChamado(
    credenciais: ErpCredenciais,
    chamado: ChamadoCanônico,
    fetcher?: typeof fetch,
  ): Promise<ResultadoChamado>;
}
