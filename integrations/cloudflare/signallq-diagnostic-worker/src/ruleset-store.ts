import type { DiagnosticRuleset } from "./contracts.ts";

// GH#1445 (parte de #952) — configuracao de rollout de um ruleset. `percent`
// controla so a participacao no shadow mode (quem tem avaliacao remota
// comparada contra o motor local), NAO quem ve o resultado remoto como
// autoritativo — essa decisao continua fora de escopo ate meta de
// equivalencia validada (ver #952, secao "Shadow mode"). Segmentacao
// implementada: versao minima do app + canal de distribuicao. Fora de escopo
// desta fatia (documentado, nao implementado): equipamento/firmware e
// regiao/provedor (ver corpo original de #952).
export interface RolloutConfig {
  percent: number;
  minVersionCode?: number | null;
  channels?: string[] | null;
}

function rolloutChannelsToColumn(channels?: string[] | null): string | null {
  if (!channels || channels.length === 0) return null;
  return channels.join(",");
}

function rolloutChannelsFromColumn(value: string | null | undefined): string[] | null {
  if (!value) return null;
  return value.split(",").filter((entry) => entry.length > 0);
}

export async function listRulesets(db: D1Database): Promise<Record<string, unknown>[]> {
  const rows = await db.prepare(
    `SELECT version, schema_version, engine_version, status, rollout_percent, rollout_min_version_code, rollout_channels, published_at, updated_at
     FROM diagnostic_rulesets
     ORDER BY version DESC
     LIMIT 20`,
  ).all<Record<string, unknown>>();
  return rows.results;
}

export async function getRuleset(db: D1Database, version: number): Promise<Record<string, unknown> | null> {
  return db.prepare(
    `SELECT version, schema_version, engine_version, status, rollout_percent, rollout_min_version_code, rollout_channels, published_at, updated_at, author, justification, rules_json
     FROM diagnostic_rulesets
     WHERE version = ?`,
  ).bind(version).first<Record<string, unknown>>();
}

/** So o status — usado por [updateRolloutConfig] pra validar que o alvo e o ruleset PUBLISHED atual. */
async function getRulesetStatus(db: D1Database, version: number): Promise<string | null> {
  const row = await db.prepare(
    "SELECT status FROM diagnostic_rulesets WHERE version = ?",
  ).bind(version).first<{ status: string }>();
  return row?.status ?? null;
}

/**
 * Status de rollout do ruleset atualmente PUBLISHED — GH#1445. Endpoint publico
 * consome isto (`GET /diagnostic/rollout-status`) para decidir, no cliente,
 * se aquela instalacao participa do shadow mode. `null` quando nao ha ruleset
 * publicado (DB nao configurado ou nenhuma publicacao ainda) — o consumidor
 * deve tratar como "nao participa" (fail closed), nunca como 100%.
 */
export async function getRolloutStatus(db: D1Database): Promise<{
  rulesetVersion: number;
  rolloutPercent: number;
  rolloutMinVersionCode: number | null;
  rolloutChannels: string[] | null;
} | null> {
  const row = await db.prepare(
    `SELECT version, rollout_percent, rollout_min_version_code, rollout_channels
     FROM diagnostic_rulesets
     WHERE status = 'PUBLISHED'
     ORDER BY version DESC
     LIMIT 1`,
  ).first<{ version: number; rollout_percent: number; rollout_min_version_code: number | null; rollout_channels: string | null }>();
  if (!row) return null;
  return {
    rulesetVersion: row.version,
    rolloutPercent: row.rollout_percent,
    rolloutMinVersionCode: row.rollout_min_version_code ?? null,
    rolloutChannels: rolloutChannelsFromColumn(row.rollout_channels),
  };
}

export async function getPublishedRulesetJson(db: D1Database): Promise<string | null> {
  const row = await db.prepare(
    "SELECT rules_json FROM diagnostic_rulesets WHERE status = 'PUBLISHED' ORDER BY version DESC LIMIT 1",
  ).first<{ rules_json: string }>();
  return row?.rules_json ?? null;
}

export async function createRulesetDraft(
  db: D1Database,
  ruleset: DiagnosticRuleset,
  actor: string,
  justification: string,
): Promise<void> {
  await db.prepare(
    `INSERT INTO diagnostic_rulesets (
      version, schema_version, engine_version, status, rollout_percent, published_at, created_at, updated_at, author, justification, rules_json
    ) VALUES (?, ?, ?, 'DRAFT', 0, NULL, ?, ?, ?, ?, ?)`,
  ).bind(
    ruleset.version,
    ruleset.schemaVersion,
    ruleset.engineVersion,
    new Date().toISOString(),
    new Date().toISOString(),
    actor,
    justification,
    JSON.stringify(ruleset),
  ).run();
}

/**
 * Publica um ruleset com percentual de rollout explicito — GH#1445. Antes
 * (achado da auditoria #1441) `rollout_percent` era sempre forcado a 100 aqui,
 * apesar da coluna existir desde a migration 001 — nunca havia rollout real.
 *
 * `rollout.percent` OMITIDO no body da requisicao (nao no `rollout` param —
 * ver [handleRulesetPublish] em `index.ts`) vira 0 (publica em modo totalmente
 * dark, so DRAFT->PUBLISHED, ninguem no shadow mode ainda) — decisao
 * deliberadamente conservadora: exposicao remota exige escolha explicita, nunca
 * default silencioso para 100 como antes.
 */
export async function publishRuleset(db: D1Database, version: number, actor: string, rollout: RolloutConfig): Promise<void> {
  const now = new Date().toISOString();
  await db.prepare("UPDATE diagnostic_rulesets SET status = 'ROLLED_BACK', updated_at = ? WHERE status = 'PUBLISHED'").bind(now).run();
  await db.prepare(
    "UPDATE diagnostic_rulesets SET status = 'PUBLISHED', rollout_percent = ?, rollout_min_version_code = ?, rollout_channels = ?, published_at = ?, updated_at = ?, author = COALESCE(author, ?) WHERE version = ?",
  ).bind(
    rollout.percent,
    rollout.minVersionCode ?? null,
    rolloutChannelsToColumn(rollout.channels),
    now,
    now,
    actor,
    version,
  ).run();
  await db.prepare(
    "INSERT INTO diagnostic_rule_audit_log (id, ruleset_version, action, actor, created_at, details_json) VALUES (?, ?, 'publish', ?, ?, ?)",
  ).bind(
    crypto.randomUUID(),
    version,
    actor,
    now,
    JSON.stringify({ version, rolloutPercent: rollout.percent, rolloutMinVersionCode: rollout.minVersionCode ?? null, rolloutChannels: rollout.channels ?? null }),
  ).run();
}

/**
 * Atualiza SO o rollout (percentual + segmentacao) do ruleset PUBLISHED atual,
 * em vigor — sem criar nova versao, sem novo deploy, sem trocar o conteudo do
 * ruleset. GH#1445, criterio "reduzir rollout_percent de volta pra 0 deve
 * remover instalacoes do grupo sem redeploy". So aceita agir sobre o ruleset
 * que esta HOJE com status PUBLISHED — atualizar rollout de um ruleset
 * DRAFT/ROLLED_BACK nao faz sentido (nao esta sendo servido a ninguem).
 */
export async function updateRolloutConfig(
  db: D1Database,
  version: number,
  rollout: RolloutConfig,
  actor: string,
): Promise<{ ok: true } | { ok: false; error: string }> {
  const status = await getRulesetStatus(db, version);
  if (!status) return { ok: false, error: "Ruleset not found." };
  if (status !== "PUBLISHED") {
    return { ok: false, error: "Only the currently published ruleset can have its rollout updated." };
  }

  const now = new Date().toISOString();
  await db.prepare(
    "UPDATE diagnostic_rulesets SET rollout_percent = ?, rollout_min_version_code = ?, rollout_channels = ?, updated_at = ? WHERE version = ?",
  ).bind(rollout.percent, rollout.minVersionCode ?? null, rolloutChannelsToColumn(rollout.channels), now, version).run();
  await db.prepare(
    "INSERT INTO diagnostic_rule_audit_log (id, ruleset_version, action, actor, created_at, details_json) VALUES (?, ?, 'rollout_update', ?, ?, ?)",
  ).bind(
    crypto.randomUUID(),
    version,
    actor,
    now,
    JSON.stringify({ version, rolloutPercent: rollout.percent, rolloutMinVersionCode: rollout.minVersionCode ?? null, rolloutChannels: rollout.channels ?? null }),
  ).run();
  return { ok: true };
}

export async function rollbackRuleset(db: D1Database, version: number, actor: string): Promise<void> {
  const now = new Date().toISOString();
  await db.prepare("UPDATE diagnostic_rulesets SET status = 'ROLLED_BACK', updated_at = ? WHERE version = ?").bind(now, version).run();
  const previous = await db.prepare(
    "SELECT version FROM diagnostic_rulesets WHERE version < ? ORDER BY version DESC LIMIT 1",
  ).bind(version).first<{ version: number }>();
  if (previous) {
    await db.prepare(
      "UPDATE diagnostic_rulesets SET status = 'PUBLISHED', rollout_percent = 100, updated_at = ?, published_at = COALESCE(published_at, ?) WHERE version = ?",
    ).bind(now, now, previous.version).run();
  }
  await db.prepare(
    "INSERT INTO diagnostic_rule_audit_log (id, ruleset_version, action, actor, created_at, details_json) VALUES (?, ?, 'rollback', ?, ?, ?)",
  ).bind(crypto.randomUUID(), version, actor, now, JSON.stringify({ version, restoredVersion: previous?.version ?? null })).run();
}
