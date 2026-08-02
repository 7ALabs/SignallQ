import type { DiagnosticDivergenceInput } from "./contracts.ts";

// GH#1444 (parte de #952) — persistencia da tabela `diagnostic_divergences`
// (migration 007). Recriada apos remocao em GH#961 (schema sem wiring) — desta
// vez com endpoint de ingest (`registerDivergence`) e endpoint de leitura
// admin (`listDivergences`/`summarizeDivergences`) conectados desde o commit
// que introduz a tabela, nunca schema solto.

export async function registerDivergence(db: D1Database, input: DiagnosticDivergenceInput): Promise<string> {
  const id = crypto.randomUUID();
  const createdAt = input.createdAt ?? new Date().toISOString();
  await db.prepare(
    `INSERT INTO diagnostic_divergences (
      id, execution_id, snapshot_hash, classification, local_status, remote_status,
      local_score, remote_score, local_flow, remote_flow, local_source, remote_source,
      local_duration_ms, remote_duration_ms, app_version, version_code, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
  ).bind(
    id,
    input.executionId,
    input.snapshotHash,
    input.classification,
    input.localStatus ?? null,
    input.remoteStatus ?? null,
    input.localScore ?? null,
    input.remoteScore ?? null,
    input.localFlow ?? null,
    input.remoteFlow ?? null,
    input.localSource ?? null,
    input.remoteSource ?? null,
    input.localDurationMs ?? null,
    input.remoteDurationMs ?? null,
    input.appVersion ?? null,
    input.versionCode ?? null,
    createdAt,
  ).run();
  return id;
}

export async function listDivergences(
  db: D1Database,
  opts: { classification?: string; limit?: number } = {},
): Promise<Record<string, unknown>[]> {
  const limit = Math.min(Math.max(opts.limit ?? 50, 1), 200);
  if (opts.classification) {
    const rows = await db.prepare(
      `SELECT * FROM diagnostic_divergences WHERE classification = ? ORDER BY created_at DESC LIMIT ?`,
    ).bind(opts.classification, limit).all<Record<string, unknown>>();
    return rows.results;
  }
  const rows = await db.prepare(
    `SELECT * FROM diagnostic_divergences ORDER BY created_at DESC LIMIT ?`,
  ).bind(limit).all<Record<string, unknown>>();
  return rows.results;
}

export async function summarizeDivergences(db: D1Database): Promise<Record<string, number>> {
  const rows = await db.prepare(
    `SELECT classification, COUNT(*) as count FROM diagnostic_divergences GROUP BY classification`,
  ).all<{ classification: string; count: number }>();
  const summary: Record<string, number> = {};
  for (const row of rows.results) {
    summary[row.classification] = row.count;
  }
  return summary;
}
