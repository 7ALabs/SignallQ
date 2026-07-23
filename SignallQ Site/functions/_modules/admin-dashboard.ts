import { resolveReligaEnabled, type SgpEnv } from '../_shared/env';

export type AdminDashboardEnv = {
  DB?: D1Database;
  [key: string]: unknown;
};

export type MassivaResumo = {
  id: number;
  popNome: string;
  quantidade: number;
  onusOffline: number | null;
  previsao: string | null;
  status: string;
  criadoEm: string;
};

export type DashboardOverview = {
  tenantId: string;
  periodoDias: number;
  metricas: {
    n1Evitado: number;
    chamadosAbertos: number;
    massivasAbertas: number;
    avisosPendentes: number;
    taxaAutoatendimento: number | null;
    religaAtiva: boolean;
  };
  massivas: MassivaResumo[];
};

function periodoSql(periodoDias: number): string {
  const dias = Number.isFinite(periodoDias) && periodoDias > 0 ? Math.min(Math.trunc(periodoDias), 365) : 30;
  return `-${dias} days`;
}

async function countBySql(db: D1Database, sql: string, args: unknown[]): Promise<number> {
  const row = await db.prepare(sql).bind(...args).first<{ total: number }>();
  return row?.total ?? 0;
}

export async function carregarDashboardOverview(
  env: AdminDashboardEnv,
  tenantId: string,
  periodoDias: number,
): Promise<DashboardOverview> {
  if (!env.DB) throw new Error('db_ausente');

  const periodo = periodoSql(periodoDias);
  const [n1Evitado, chamadosAbertos, massivasAbertas, avisosPendentes, massivas] = await Promise.all([
    countBySql(
      env.DB,
      `SELECT COUNT(*) AS total
       FROM diagnosticos_3a
       WHERE tenant_id = ?1 AND criado_em >= datetime('now', '${periodo}')`,
      [tenantId],
    ),
    countBySql(
      env.DB,
      `SELECT COUNT(*) AS total
       FROM diagnosticos_3b
       WHERE tenant_id = ?1 AND criado_em >= datetime('now', '${periodo}')`,
      [tenantId],
    ),
    countBySql(
      env.DB,
      `SELECT COUNT(*) AS total
       FROM alertas_massiva
       WHERE tenant_id = ?1 AND status = 'aberto' AND criado_em >= datetime('now', '${periodo}')`,
      [tenantId],
    ),
    countBySql(
      env.DB,
      `SELECT COUNT(*) AS total
       FROM avisos_normalizacao
       WHERE tenant_id = ?1 AND status = 'pendente'`,
      [tenantId],
    ),
    env.DB
      .prepare(
        `SELECT id, pop_nome, quantidade, onus_offline, previsao, status, criado_em
         FROM alertas_massiva
         WHERE tenant_id = ?1
         ORDER BY criado_em DESC
         LIMIT 10`,
      )
      .bind(tenantId)
      .all<{
        id: number;
        pop_nome: string;
        quantidade: number;
        onus_offline: number | null;
        previsao: string | null;
        status: string;
        criado_em: string;
      }>(),
  ]);

  const totalDiagnosticos = n1Evitado + chamadosAbertos;

  return {
    tenantId,
    periodoDias: Number.isFinite(periodoDias) && periodoDias > 0 ? Math.min(Math.trunc(periodoDias), 365) : 30,
    metricas: {
      n1Evitado,
      chamadosAbertos,
      massivasAbertas,
      avisosPendentes,
      taxaAutoatendimento: totalDiagnosticos > 0 ? Number((n1Evitado / totalDiagnosticos).toFixed(4)) : null,
      religaAtiva: resolveReligaEnabled(env as SgpEnv, tenantId),
    },
    massivas: (massivas.results ?? []).map((item) => ({
      id: item.id,
      popNome: item.pop_nome,
      quantidade: item.quantidade,
      onusOffline: item.onus_offline,
      previsao: item.previsao,
      status: item.status,
      criadoEm: item.criado_em,
    })),
  };
}

export async function resolverMassivaManual(
  db: D1Database,
  tenantId: string,
  alertaId: number,
): Promise<boolean> {
  const alerta = await db
    .prepare('SELECT id FROM alertas_massiva WHERE id = ?1 AND tenant_id = ?2 AND status = \'aberto\' LIMIT 1')
    .bind(alertaId, tenantId)
    .first<{ id: number }>();

  if (!alerta) return false;

  await db
    .prepare('UPDATE alertas_massiva SET status = \'resolvido\' WHERE id = ?1 AND tenant_id = ?2')
    .bind(alertaId, tenantId)
    .run();

  return true;
}
