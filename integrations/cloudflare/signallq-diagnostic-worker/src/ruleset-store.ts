import type { DiagnosticRuleset } from "./contracts.ts";

export async function listRulesets(db: D1Database): Promise<Record<string, unknown>[]> {
  const rows = await db.prepare(
    `SELECT version, schema_version, engine_version, status, rollout_percent, published_at, updated_at
     FROM diagnostic_rulesets
     ORDER BY version DESC
     LIMIT 20`,
  ).all<Record<string, unknown>>();
  return rows.results;
}

export async function getRuleset(db: D1Database, version: number): Promise<Record<string, unknown> | null> {
  return db.prepare(
    `SELECT version, schema_version, engine_version, status, rollout_percent, published_at, updated_at, author, justification, rules_json
     FROM diagnostic_rulesets
     WHERE version = ?`,
  ).bind(version).first<Record<string, unknown>>();
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

export async function publishRuleset(db: D1Database, version: number, actor: string): Promise<void> {
  const now = new Date().toISOString();
  await db.prepare("UPDATE diagnostic_rulesets SET status = 'ROLLED_BACK', updated_at = ? WHERE status = 'PUBLISHED'").bind(now).run();
  await db.prepare(
    "UPDATE diagnostic_rulesets SET status = 'PUBLISHED', rollout_percent = 100, published_at = ?, updated_at = ?, author = COALESCE(author, ?) WHERE version = ?",
  ).bind(now, now, actor, version).run();
  await db.prepare(
    "INSERT INTO diagnostic_rule_audit_log (id, ruleset_version, action, actor, created_at, details_json) VALUES (?, ?, 'publish', ?, ?, ?)",
  ).bind(crypto.randomUUID(), version, actor, now, JSON.stringify({ version })).run();
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
