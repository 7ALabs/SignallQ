// Ciclo de massiva (issues #95–#98): o produto registra cada diagnóstico "lado do provedor" por
// POP no D1 e usa esses registros como fonte da verdade para detectar e informar massivas. O SGP
// não expõe endpoint server-to-server de massiva no escopo do token URA (gap validado na issue
// #87, ver docs/erp/sgp/api-reference.md §1.9) — por isso a base é própria.

import { resolveGenieAcsCreds, type GenieAcsEnv } from '../_shared/env';
import { contarOnusOffline } from './genieacs';

export type RegistroDiagnostico3b = {
  tenantId: string;
  popNome: string | null;
  diagnosticoId: string;
  protocolo: string;
};

export async function registrarDiagnostico3b(
  db: D1Database,
  registro: RegistroDiagnostico3b,
): Promise<void> {
  await db
    .prepare(
      'INSERT INTO diagnosticos_3b (tenant_id, pop_nome, diagnostico_id, protocolo) VALUES (?1, ?2, ?3, ?4)',
    )
    .bind(registro.tenantId, registro.popNome, registro.diagnosticoId, registro.protocolo)
    .run();
}

export type MassivaAtiva = { ativa: false } | { ativa: true; previsao?: string };

// Sem painel administrativo (M4) para o ISP marcar "resolvido", um alerta 'aberto' expira sozinho
// após ttlHoras sem novo diagnóstico "provedor" do mesmo POP (issue #98, gap reportado por Luiz:
// "quando vai sair da massiva?"). Quem consulta e encontra o alerta vencido já o marca como
// 'expirado' (limpeza preguiçosa) — assim um futuro painel/auditoria vê o estado real sem
// depender de um cron separado.
export async function consultarMassivaAtiva(
  db: D1Database,
  tenantId: string,
  popNome: string,
  ttlHoras: number,
): Promise<MassivaAtiva> {
  const alerta = await db
    .prepare(
      `SELECT id, previsao FROM alertas_massiva
       WHERE tenant_id = ?1 AND pop_nome = ?2 AND status = 'aberto'
         AND criado_em >= datetime('now', '-${ttlHoras} hours')
       LIMIT 1`,
    )
    .bind(tenantId, popNome)
    .first<{ id: number; previsao: string | null }>();

  if (!alerta) {
    await expirarAlertasVencidos(db, tenantId, popNome, ttlHoras);
    return { ativa: false };
  }
  return { ativa: true, ...(alerta.previsao ? { previsao: alerta.previsao } : {}) };
}

async function expirarAlertasVencidos(
  db: D1Database,
  tenantId: string,
  popNome: string,
  ttlHoras: number,
): Promise<void> {
  await db
    .prepare(
      `UPDATE alertas_massiva SET status = 'expirado'
       WHERE tenant_id = ?1 AND pop_nome = ?2 AND status = 'aberto'
         AND criado_em < datetime('now', '-${ttlHoras} hours')`,
    )
    .bind(tenantId, popNome)
    .run();
}

export type PedidoAvisoNormalizacao = {
  tenantId: string;
  contratoId: string;
  popNome: string;
};

// Idempotente por tenant+contrato+POP enquanto houver pedido pendente (issue #96) — o assinante
// pode passar pela tela de massiva mais de uma vez durante o mesmo incidente sem duplicar a fila.
export async function registrarAvisoNormalizacao(
  db: D1Database,
  pedido: PedidoAvisoNormalizacao,
): Promise<'registrado' | 'ja_pendente'> {
  const existente = await db
    .prepare(
      "SELECT id FROM avisos_normalizacao WHERE tenant_id = ?1 AND contrato_id = ?2 AND pop_nome = ?3 AND status = 'pendente' LIMIT 1",
    )
    .bind(pedido.tenantId, pedido.contratoId, pedido.popNome)
    .first<{ id: number }>();

  if (existente) return 'ja_pendente';

  await db
    .prepare('INSERT INTO avisos_normalizacao (tenant_id, contrato_id, pop_nome) VALUES (?1, ?2, ?3)')
    .bind(pedido.tenantId, pedido.contratoId, pedido.popNome)
    .run();

  return 'registrado';
}

const JANELA_DETECCAO_MINUTOS = 30;
const MINIMO_DIAGNOSTICOS_MASSIVA = 3;

// Detecção de possível massiva (issue #98): 3+ diagnósticos "lado do provedor" do mesmo
// tenant+POP em 30 min → registra alerta no D1 + log estruturado. Roda em fire-and-forget
// (waitUntil) depois da resposta ao assinante — nunca lança: qualquer falha é logada e engolida.
// Sem canal externo nesta fase: alerta = registro no D1 + log; notificação ativa ao ISP é
// pendência do M4. O alerta aberto alimenta /api/massiva/ativa — os próximos assinantes do POP
// caem no ramo de massiva e o produto para de gerar chamados sobre o incidente que ele detectou.
export async function detectarPossivelMassiva(
  db: D1Database,
  env: GenieAcsEnv,
  tenantId: string,
  popNome: string,
  ttlHoras: number,
  fetcher: typeof fetch = fetch,
): Promise<void> {
  try {
    const linha = await db
      .prepare(
        `SELECT COUNT(*) AS total FROM diagnosticos_3b WHERE tenant_id = ?1 AND pop_nome = ?2 AND criado_em >= datetime('now', '-${JANELA_DETECCAO_MINUTOS} minutes')`,
      )
      .bind(tenantId, popNome)
      .first<{ total: number }>();

    const total = linha?.total ?? 0;
    if (total < MINIMO_DIAGNOSTICOS_MASSIVA) return;

    // Só um alerta AINDA DENTRO do TTL bloqueia um novo — um expirado não pode mascarar uma
    // massiva nova no mesmo POP (ex.: reincidência dias depois).
    const alertaAberto = await db
      .prepare(
        `SELECT id FROM alertas_massiva
         WHERE tenant_id = ?1 AND pop_nome = ?2 AND status = 'aberto'
           AND criado_em >= datetime('now', '-${ttlHoras} hours')
         LIMIT 1`,
      )
      .bind(tenantId, popNome)
      .first<{ id: number }>();

    if (alertaAberto) return;
    await expirarAlertasVencidos(db, tenantId, popNome, ttlHoras);

    // Enriquecimento, não pré-condição: contagem bruta de devices offline do ACS do tenant
    // (só com GENIEACS_ENABLED_<TENANT>); falha/timeout/tenant sem ACS → NULL.
    const creds = resolveGenieAcsCreds(env, tenantId);
    const onusOffline = creds ? await contarOnusOffline(creds.baseUrl, creds, fetcher) : null;

    await db
      .prepare(
        'INSERT INTO alertas_massiva (tenant_id, pop_nome, quantidade, janela_minutos, onus_offline) VALUES (?1, ?2, ?3, ?4, ?5)',
      )
      .bind(tenantId, popNome, total, JANELA_DETECCAO_MINUTOS, onusOffline)
      .run();

    console.warn('[massiva] possível massiva detectada — alerta registrado', {
      tenant_id: tenantId,
      pop_nome: popNome,
      quantidade: total,
      janela_minutos: JANELA_DETECCAO_MINUTOS,
      onus_offline: onusOffline,
    });
  } catch (err) {
    console.error('[massiva] falha na detecção — fluxo do assinante não afetado', {
      tenant_id: tenantId,
      pop_nome: popNome,
      erro: err instanceof Error ? err.message : 'erro_desconhecido',
    });
  }
}
