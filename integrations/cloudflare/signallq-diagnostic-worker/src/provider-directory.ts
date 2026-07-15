import type { ProviderDetectionInput, ProviderRecord, ProviderSupport } from "./contracts.ts";

type SeedEnv = {
  DB?: D1Database;
  PROVIDER_DIRECTORY_SEED_JSON?: string;
  // GH#965 (revisado) — base publica usada pra montar a URL absoluta de
  // `GET /providers/:id/logo` (a propria rota deste worker serve o binario
  // gravado no D1 — ver `uploadProviderLogo`/`getProviderLogoBinary`). R2 foi
  // descartado por decisao de produto (Cloudflare exige cartao mesmo no tier
  // gratis). Sem esta var, `logo.url` fica so o path relativo.
  PROVIDER_LOGO_PUBLIC_BASE_URL?: string;
};

type ProviderLogoEnv = SeedEnv;

const MAX_LOGO_BYTES = 500 * 1024;

function buildLogoUrl(env: SeedEnv, providerId: string): string {
  const base = (env.PROVIDER_LOGO_PUBLIC_BASE_URL ?? "").replace(/\/$/, "");
  return `${base}/providers/${providerId}/logo`;
}

function bytesToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
}

function base64ToBytes(base64: string): Uint8Array {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

type ProviderAdminInput = {
  id: string;
  displayName: string;
  legalName?: string | null;
  cnpj?: string | null;
  officialDomain?: string | null;
  providerType?: string;
  status?: string;
  verifiedAt?: string | null;
  nextReviewAt?: string | null;
  aliases?: string[];
  asns?: number[];
  support?: Partial<ProviderSupport>;
  logo?: {
    sourceUrl?: string | null;
    r2Key?: string | null;
    version?: number;
  } | null;
};

type ProviderReviewInput = {
  status?: string;
  verifiedAt?: string | null;
  nextReviewAt?: string | null;
  notes?: string | null;
};

type ProviderReviewQueueItem = {
  detectionKey: string;
  providerId: string | null;
  providerDisplayName: string | null;
  asn: number | null;
  normalizedName: string | null;
  testCountTotal: number;
  testCountSinceReview: number;
  distinctInstallationsApprox: number;
  distinctDays: number;
  firstSeenAt: string;
  lastSeenAt: string;
  lastEnrichmentQueuedAt: string | null;
  status: string;
};

type DetectionAggregateRow = {
  detection_key: string;
  provider_id: string | null;
  asn: number | null;
  raw_name_sample: string | null;
  normalized_name: string | null;
  test_count_total: number;
  test_count_since_review: number;
  distinct_installations_approx: number;
  distinct_days: number;
  first_seen_at: string;
  last_seen_at: string;
  last_enrichment_queued_at: string | null;
  last_review_at: string | null;
};

type ProviderRow = {
  id: string;
  display_name: string;
  legal_name: string | null;
  cnpj: string | null;
  provider_type: string;
  status: string;
  official_domain: string | null;
  logo_version: number;
  last_verified_at: string | null;
  next_review_at: string | null;
};

type ProviderIdentifierRow = {
  identifier_type: string;
  original_value: string;
};

type ProviderChannelRow = {
  channel_type: string;
  value: string;
};

type ProviderAssetRow = {
  source_url: string | null;
  r2_key: string;
  version: number;
  created_at: string;
};

const SEEDED_PROVIDERS: ProviderRecord[] = [
  {
    id: "claro",
    displayName: "Claro",
    legalName: "Claro S.A.",
    cnpj: null,
    officialDomain: "claro.com.br",
    providerType: "NATIONAL",
    status: "VERIFIED",
    logo: {
      url: "https://assets.signallq.com/providers/claro/logo-square-v1.webp",
      version: 1,
      updatedAt: "2026-07-14T00:00:00.000Z",
    },
    support: {
      sacPhone: "10621",
      technicalSupportPhone: null,
      whatsappUrl: null,
      websiteUrl: "https://www.claro.com.br",
      customerAreaUrl: "https://minhaclaro.claro.com.br",
      ombudsmanPhone: null,
    },
    aliases: ["claro", "net", "net virtua", "claro fibra"],
    asns: [28573],
    verifiedAt: "2026-07-14T00:00:00.000Z",
    cacheVersion: 1,
    cacheExpiresAt: "2026-07-21T00:00:00.000Z",
  },
  {
    id: "brisanet",
    displayName: "Brisanet",
    legalName: "Brisanet Serviços de Telecomunicações S.A.",
    cnpj: null,
    officialDomain: "brisanet.com.br",
    providerType: "REGIONAL",
    status: "VERIFIED",
    logo: {
      url: "https://assets.signallq.com/providers/brisanet/logo-square-v3.webp",
      version: 3,
      updatedAt: "2026-07-14T00:00:00.000Z",
    },
    support: {
      sacPhone: "10517",
      technicalSupportPhone: null,
      whatsappUrl: "https://wa.me/55803001217",
      websiteUrl: "https://www.brisanet.com.br",
      customerAreaUrl: null,
      ombudsmanPhone: null,
    },
    aliases: ["brisanet"],
    asns: [28126],
    verifiedAt: "2026-07-14T00:00:00.000Z",
    cacheVersion: 18,
    cacheExpiresAt: "2026-07-21T00:00:00.000Z",
  },
];

function nowIso(): string {
  return new Date().toISOString();
}

function normalize(value: string): string {
  return value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .replace(/[^\p{Letter}\p{Number}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function dedupeStrings(values: Array<string | null | undefined>): string[] {
  return [...new Set(values.map((value) => value?.trim()).filter((value): value is string => Boolean(value)))];
}

function toDetectionGroupKey(row: DetectionAggregateRow): string {
  if (row.provider_id) return `provider:${row.provider_id}`;
  if (row.asn != null) return `asn:${row.asn}`;
  return `normalized:${row.normalized_name ?? "unknown"}`;
}

// GH#956 — distinctInstallationsApprox do grupo (cross-day) precisa vir de uma
// contagem DISTINCT real de installation_hash cruzando os dias do grupo, nao
// de uma soma dos totais por-dia (que dobraria a contagem de uma mesma
// instalacao vista em mais de um dia). `override` e passado pelo caller
// (listProviderReviewQueue/enqueuePendingProviderReviews) apos consultar
// provider_detection_installations; sem DB (modo seed-only) cai pra 0.
function summarizeDetectionGroup(rows: DetectionAggregateRow[], distinctInstallationsOverride = 0) {
  const sorted = [...rows].sort((left, right) => right.last_seen_at.localeCompare(left.last_seen_at));
  const latest = sorted[0]!;
  const uniqueDays = new Set(rows.map((row) => row.last_seen_at.slice(0, 10)));
  return {
    detectionKey: latest.detection_key,
    providerId: latest.provider_id,
    asn: latest.asn,
    normalizedName: latest.normalized_name,
    testCountTotal: rows.reduce((sum, row) => sum + row.test_count_total, 0),
    testCountSinceReview: rows.reduce((sum, row) => sum + row.test_count_since_review, 0),
    distinctInstallationsApprox: distinctInstallationsOverride,
    distinctDays: uniqueDays.size,
    firstSeenAt: rows.reduce((min, row) => row.first_seen_at < min ? row.first_seen_at : min, rows[0]!.first_seen_at),
    lastSeenAt: latest.last_seen_at,
    lastEnrichmentQueuedAt: latest.last_enrichment_queued_at,
  };
}

async function loadDetectionRows(db: D1Database): Promise<DetectionAggregateRow[]> {
  const { results } = await db.prepare(
    `SELECT detection_key, provider_id, asn, raw_name_sample, normalized_name,
            test_count_total, test_count_since_review, distinct_installations_approx,
            distinct_days, first_seen_at, last_seen_at, last_enrichment_queued_at, last_review_at
     FROM provider_detection_stats`,
  ).all<DetectionAggregateRow>();
  return results;
}

// GH#956 — conta installationHash distintos cruzando todos os detection_key
// (dias) de um mesmo grupo provider/asn/normalizedName, direto da tabela de
// evidencia real (nao do campo pre-calculado, que e por-dia).
async function countDistinctInstallations(db: D1Database, detectionKeys: string[]): Promise<number> {
  if (detectionKeys.length === 0) return 0;
  const placeholders = detectionKeys.map(() => "?").join(", ");
  const row = await db.prepare(
    `SELECT COUNT(DISTINCT installation_hash) as count
     FROM provider_detection_installations
     WHERE detection_key IN (${placeholders})`,
  ).bind(...detectionKeys).first<{ count: number }>();
  return row?.count ?? 0;
}

async function loadSeedProviders(env: { PROVIDER_DIRECTORY_SEED_JSON?: string }): Promise<ProviderRecord[]> {
  if (!env.PROVIDER_DIRECTORY_SEED_JSON) {
    return SEEDED_PROVIDERS;
  }
  try {
    const parsed = JSON.parse(env.PROVIDER_DIRECTORY_SEED_JSON) as ProviderRecord[];
    // GH#971: array vazio (ex.: var configurada como "[]") não conta como seed
    // real — cai no catálogo embutido em vez de subir o diretório vazio.
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : SEEDED_PROVIDERS;
  } catch {
    return SEEDED_PROVIDERS;
  }
}

function mapChannelsToSupport(channels: ProviderChannelRow[]): ProviderSupport {
  const findValue = (channelType: string) => channels.find((channel) => channel.channel_type === channelType)?.value ?? null;
  return {
    sacPhone: findValue("SAC_PHONE"),
    technicalSupportPhone: findValue("TECH_SUPPORT_PHONE"),
    whatsappUrl: findValue("WHATSAPP_URL"),
    websiteUrl: findValue("WEBSITE_URL"),
    customerAreaUrl: findValue("CUSTOMER_AREA_URL"),
    ombudsmanPhone: findValue("OMBUDSMAN_PHONE"),
  };
}

async function loadProviderFromDb(db: D1Database, providerId: string, env: SeedEnv): Promise<ProviderRecord | null> {
  const row = await db.prepare(
    `SELECT id, display_name, legal_name, cnpj, provider_type, status, official_domain,
            logo_version, last_verified_at, next_review_at
     FROM providers
     WHERE id = ?`,
  ).bind(providerId).first<ProviderRow>();
  if (!row) return null;

  const identifiers = await db.prepare(
    `SELECT identifier_type, original_value
     FROM provider_identifiers
     WHERE provider_id = ? AND is_active = 1`,
  ).bind(providerId).all<ProviderIdentifierRow>();
  const channels = await db.prepare(
    `SELECT channel_type, value
     FROM provider_channels
     WHERE provider_id = ? AND is_active = 1
     ORDER BY priority DESC, id DESC`,
  ).bind(providerId).all<ProviderChannelRow>();
  const assets = await db.prepare(
    `SELECT source_url, r2_key, version, created_at
     FROM provider_assets
     WHERE provider_id = ? AND asset_type = 'LOGO_SQUARE'
     ORDER BY version DESC, id DESC`,
  ).bind(providerId).all<ProviderAssetRow>();

  const aliases = identifiers.results
    .filter((entry) => entry.identifier_type === "ALIAS")
    .map((entry) => entry.original_value);
  const asns = identifiers.results
    .filter((entry) => entry.identifier_type === "ASN")
    .map((entry) => Number(entry.original_value))
    .filter((value) => Number.isFinite(value));
  const logo = assets.results[0]
    ? {
        url: assets.results[0].source_url ?? buildLogoUrl(env, providerId),
        version: assets.results[0].version,
        updatedAt: assets.results[0].created_at,
      }
    : null;

  return {
    id: row.id,
    displayName: row.display_name,
    legalName: row.legal_name,
    cnpj: row.cnpj,
    officialDomain: row.official_domain,
    providerType: row.provider_type,
    status: row.status,
    logo,
    support: mapChannelsToSupport(channels.results),
    aliases,
    asns,
    verifiedAt: row.last_verified_at,
    cacheVersion: row.logo_version,
    cacheExpiresAt: row.next_review_at ?? row.last_verified_at ?? nowIso(),
  };
}

async function queryProviderIdsBySearch(db: D1Database, query: string): Promise<string[]> {
  const like = `%${normalize(query)}%`;
  const direct = await db.prepare(
    `SELECT id
     FROM providers
     WHERE lower(display_name) LIKE ?
        OR lower(COALESCE(official_domain, '')) LIKE ?
     ORDER BY display_name ASC
     LIMIT 20`,
  ).bind(like, like).all<{ id: string }>();
  const identifiers = await db.prepare(
    `SELECT DISTINCT provider_id AS id
     FROM provider_identifiers
     WHERE is_active = 1 AND normalized_value LIKE ?
     LIMIT 20`,
  ).bind(like).all<{ id: string }>();
  return [...new Set([...direct.results, ...identifiers.results].map((row) => row.id))];
}

async function findProviderByAsnInDb(db: D1Database, asn: number, env: SeedEnv): Promise<ProviderRecord | null> {
  const identifier = await db.prepare(
    `SELECT provider_id
     FROM provider_identifiers
     WHERE identifier_type = 'ASN' AND is_active = 1 AND original_value = ?
     LIMIT 1`,
  ).bind(String(asn)).first<{ provider_id: string }>();
  return identifier ? loadProviderFromDb(db, identifier.provider_id, env) : null;
}

function toProviderAdminInput(provider: ProviderRecord): ProviderAdminInput {
  return {
    id: provider.id,
    displayName: provider.displayName,
    legalName: provider.legalName ?? null,
    cnpj: provider.cnpj ?? null,
    officialDomain: provider.officialDomain ?? null,
    providerType: provider.providerType,
    status: provider.status,
    verifiedAt: provider.verifiedAt,
    nextReviewAt: provider.cacheExpiresAt,
    aliases: provider.aliases,
    asns: provider.asns,
    support: provider.support,
    logo: provider.logo
      ? {
          sourceUrl: provider.logo.url,
          r2Key: `external://${provider.id}/logo-square-v${provider.logo.version}`,
          version: provider.logo.version,
        }
      : null,
  };
}

export async function getProviderById(env: SeedEnv, providerId: string): Promise<ProviderRecord | null> {
  if (env.DB) {
    const record = await loadProviderFromDb(env.DB, providerId, env);
    if (record) return record;
  }
  const providers = await loadSeedProviders(env);
  return providers.find((provider) => provider.id === providerId) ?? null;
}

export async function searchProviders(env: SeedEnv, query: string): Promise<ProviderRecord[]> {
  const normalizedQuery = normalize(query);
  if (!normalizedQuery) return [];

  if (env.DB) {
    const ids = await queryProviderIdsBySearch(env.DB, normalizedQuery);
    const found = await Promise.all(ids.map((id) => loadProviderFromDb(env.DB!, id, env)));
    if (found.length > 0) {
      return found.filter((provider): provider is ProviderRecord => Boolean(provider));
    }
  }

  const providers = await loadSeedProviders(env);
  return providers.filter((provider) =>
    normalize(provider.displayName).includes(normalizedQuery)
    || provider.aliases.some((alias) => normalize(alias).includes(normalizedQuery))
    || normalize(provider.officialDomain ?? "").includes(normalizedQuery)
  );
}

export async function getProviderByAsn(env: SeedEnv, asn: number): Promise<ProviderRecord | null> {
  if (env.DB) {
    const record = await findProviderByAsnInDb(env.DB, asn, env);
    if (record) return record;
  }
  const providers = await loadSeedProviders(env);
  return providers.find((provider) => provider.asns.includes(asn)) ?? null;
}

export async function upsertProvider(env: SeedEnv, input: ProviderAdminInput): Promise<{ providerId: string; syncedIdentifiers: number }> {
  if (!env.DB) {
    throw new Error("DB binding not configured.");
  }

  const timestamp = nowIso();
  const providerType = input.providerType ?? "UNKNOWN";
  const status = input.status ?? "DRAFT";
  const verifiedAt = input.verifiedAt ?? null;
  const nextReviewAt = input.nextReviewAt ?? null;
  const logoVersion = input.logo?.version ?? 0;

  await env.DB.prepare(
    `INSERT INTO providers (
      id, display_name, legal_name, cnpj, provider_type, status, official_domain,
      logo_version, last_verified_at, next_review_at, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      display_name = excluded.display_name,
      legal_name = excluded.legal_name,
      cnpj = excluded.cnpj,
      provider_type = excluded.provider_type,
      status = excluded.status,
      official_domain = excluded.official_domain,
      logo_version = excluded.logo_version,
      last_verified_at = excluded.last_verified_at,
      next_review_at = excluded.next_review_at,
      updated_at = excluded.updated_at`,
  ).bind(
    input.id,
    input.displayName,
    input.legalName ?? null,
    input.cnpj ?? null,
    providerType,
    status,
    input.officialDomain ?? null,
    logoVersion,
    verifiedAt,
    nextReviewAt,
    timestamp,
    timestamp,
  ).run();

  await env.DB.prepare("UPDATE provider_identifiers SET is_active = 0 WHERE provider_id = ?").bind(input.id).run();
  await env.DB.prepare("UPDATE provider_channels SET is_active = 0 WHERE provider_id = ?").bind(input.id).run();

  const aliases = dedupeStrings([input.displayName, ...(input.aliases ?? [])]);
  const asns = [...new Set((input.asns ?? []).filter((value) => Number.isInteger(value)))];

  for (const alias of aliases) {
    await env.DB.prepare(
      `INSERT INTO provider_identifiers (
        provider_id, identifier_type, original_value, normalized_value, confidence, source_url, is_active, verified_at
      ) VALUES (?, 'ALIAS', ?, ?, 'HIGH', NULL, 1, ?)`,
    ).bind(input.id, alias, normalize(alias), verifiedAt ?? timestamp).run();
  }

  for (const asn of asns) {
    await env.DB.prepare(
      `INSERT INTO provider_identifiers (
        provider_id, identifier_type, original_value, normalized_value, confidence, source_url, is_active, verified_at
      ) VALUES (?, 'ASN', ?, ?, 'HIGH', NULL, 1, ?)`,
    ).bind(input.id, String(asn), String(asn), verifiedAt ?? timestamp).run();
  }

  if (input.officialDomain) {
    await env.DB.prepare(
      `INSERT INTO provider_identifiers (
        provider_id, identifier_type, original_value, normalized_value, confidence, source_url, is_active, verified_at
      ) VALUES (?, 'DOMAIN', ?, ?, 'HIGH', NULL, 1, ?)`,
    ).bind(input.id, input.officialDomain, normalize(input.officialDomain), verifiedAt ?? timestamp).run();
  }

  const supportEntries: Array<[string, string | null | undefined]> = [
    ["SAC_PHONE", input.support?.sacPhone],
    ["TECH_SUPPORT_PHONE", input.support?.technicalSupportPhone],
    ["WHATSAPP_URL", input.support?.whatsappUrl],
    ["WEBSITE_URL", input.support?.websiteUrl],
    ["CUSTOMER_AREA_URL", input.support?.customerAreaUrl],
    ["OMBUDSMAN_PHONE", input.support?.ombudsmanPhone],
  ];

  for (const [channelType, value] of supportEntries) {
    if (!value) continue;
    await env.DB.prepare(
      `INSERT INTO provider_channels (
        provider_id, channel_type, label, value, source_url, verification_status,
        verified_at, next_review_at, priority, is_active
      ) VALUES (?, ?, NULL, ?, NULL, 'VERIFIED', ?, ?, 100, 1)`,
    ).bind(input.id, channelType, value, verifiedAt ?? timestamp, nextReviewAt).run();
  }

  if (input.logo?.sourceUrl || input.logo?.r2Key) {
    await env.DB.prepare(
      `INSERT INTO provider_assets (
        provider_id, asset_type, r2_key, source_url, file_hash, version, verification_status, data_base64, content_type, created_at
      ) VALUES (?, 'LOGO_SQUARE', ?, ?, NULL, ?, 'VERIFIED', NULL, NULL, ?)`,
    ).bind(
      input.id,
      input.logo.r2Key ?? `manual://${input.id}/logo-square-v${input.logo.version ?? 1}`,
      input.logo.sourceUrl ?? null,
      input.logo.version ?? 1,
      timestamp,
    ).run();
  }

  return {
    providerId: input.id,
    syncedIdentifiers: aliases.length + asns.length + (input.officialDomain ? 1 : 0),
  };
}

export async function syncSeedProvidersToDb(env: SeedEnv): Promise<{ synced: number }> {
  const providers = await loadSeedProviders(env);
  for (const provider of providers) {
    await upsertProvider(env, toProviderAdminInput(provider));
  }
  return { synced: providers.length };
}

export async function listProviderReviewQueue(env: SeedEnv): Promise<ProviderReviewQueueItem[]> {
  if (!env.DB) {
    return [];
  }
  const rows = await loadDetectionRows(env.DB);
  const grouped = new Map<string, DetectionAggregateRow[]>();
  for (const row of rows) {
    const key = toDetectionGroupKey(row);
    const bucket = grouped.get(key) ?? [];
    bucket.push(row);
    grouped.set(key, bucket);
  }

  const items: ProviderReviewQueueItem[] = [];
  for (const bucket of grouped.values()) {
    const distinctInstallations = await countDistinctInstallations(env.DB, bucket.map((row) => row.detection_key));
    const summary = summarizeDetectionGroup(bucket, distinctInstallations);
    if (summary.testCountTotal < 5 || summary.distinctInstallationsApprox < 3 || summary.distinctDays < 2) {
      continue;
    }
    const providerName = summary.providerId
      ? (await env.DB.prepare("SELECT id, display_name, legal_name, cnpj, provider_type, status, official_domain, logo_version, last_verified_at, next_review_at FROM providers WHERE id = ?")
        .bind(summary.providerId)
        .first<ProviderRow>())?.display_name ?? null
      : null;
    items.push({
      detectionKey: summary.detectionKey,
      providerId: summary.providerId,
      providerDisplayName: providerName,
      asn: summary.asn,
      normalizedName: summary.normalizedName,
      testCountTotal: summary.testCountTotal,
      testCountSinceReview: summary.testCountSinceReview,
      distinctInstallationsApprox: summary.distinctInstallationsApprox,
      distinctDays: summary.distinctDays,
      firstSeenAt: summary.firstSeenAt,
      lastSeenAt: summary.lastSeenAt,
      lastEnrichmentQueuedAt: summary.lastEnrichmentQueuedAt,
      status: summary.lastEnrichmentQueuedAt ? "QUEUED" : "PENDING_REVIEW",
    });
  }

  return items.sort((left, right) => right.lastSeenAt.localeCompare(left.lastSeenAt)).slice(0, 100);
}

export async function listStaleProviders(env: SeedEnv): Promise<ProviderRecord[]> {
  if (!env.DB) {
    return [];
  }
  const { results } = await env.DB.prepare(
    `SELECT id
     FROM providers
     WHERE status IN ('STALE', 'DRAFT')
        OR (next_review_at IS NOT NULL AND next_review_at <= ?)
     ORDER BY updated_at DESC
     LIMIT 100`,
  ).bind(nowIso()).all<{ id: string }>();
  const providers = await Promise.all(results.map((row) => loadProviderFromDb(env.DB!, row.id, env)));
  return providers.filter((provider): provider is ProviderRecord => Boolean(provider));
}

export async function reviewProvider(env: SeedEnv, providerId: string, input: ProviderReviewInput): Promise<{ ok: true; providerId: string }> {
  if (!env.DB) {
    throw new Error("DB binding not configured.");
  }
  const timestamp = nowIso();
  await env.DB.prepare(
    `UPDATE providers
     SET status = ?, last_verified_at = ?, next_review_at = ?, updated_at = ?
     WHERE id = ?`,
  ).bind(
    input.status ?? "VERIFIED",
    input.verifiedAt ?? timestamp,
    input.nextReviewAt ?? null,
    timestamp,
    providerId,
  ).run();
  await env.DB.prepare(
    `UPDATE provider_detection_stats
     SET test_count_since_review = 0,
         last_review_at = ?
     WHERE provider_id = ?`,
  ).bind(timestamp, providerId).run();
  await env.DB.prepare(
    `UPDATE provider_enrichment_jobs
     SET status = 'COMPLETED',
         completed_at = ?,
         error_message = ?
     WHERE provider_id = ?
       AND status IN ('PENDING', 'PENDING_REVIEW', 'RUNNING')`,
  ).bind(timestamp, input.notes ?? null, providerId).run();
  return { ok: true, providerId };
}

export async function enqueuePendingProviderReviews(env: SeedEnv): Promise<{ queued: number }> {
  if (!env.DB) {
    return { queued: 0 };
  }

  const rows = await loadDetectionRows(env.DB);
  const grouped = new Map<string, DetectionAggregateRow[]>();
  for (const row of rows) {
    const key = toDetectionGroupKey(row);
    const bucket = grouped.get(key) ?? [];
    bucket.push(row);
    grouped.set(key, bucket);
  }

  let queued = 0;
  const timestamp = nowIso();
  for (const [groupKey, bucket] of grouped.entries()) {
    const distinctInstallations = await countDistinctInstallations(env.DB, bucket.map((row) => row.detection_key));
    const summary = summarizeDetectionGroup(bucket, distinctInstallations);
    if (summary.testCountTotal < 5 || summary.distinctInstallationsApprox < 3 || summary.distinctDays < 2) {
      continue;
    }
    if (summary.lastEnrichmentQueuedAt) continue;
    const jobId = crypto.randomUUID();
    const reason = summary.providerId ? "scheduled-review" : "scheduled-discovery";
    const idempotencyKey = `provider-review:${groupKey}`;
    await env.DB.prepare(
      `INSERT OR IGNORE INTO provider_enrichment_jobs (
        id, provider_id, detection_key, reason, status, idempotency_key,
        attempt_count, started_at, completed_at, error_message, created_at
      ) VALUES (?, ?, ?, ?, 'PENDING_REVIEW', ?, 0, NULL, NULL, NULL, ?)`,
    ).bind(jobId, summary.providerId, summary.detectionKey, reason, idempotencyKey, timestamp).run();
    for (const row of bucket) {
      await env.DB.prepare(
        `UPDATE provider_detection_stats
         SET last_enrichment_queued_at = ?
         WHERE detection_key = ?`,
      ).bind(timestamp, row.detection_key).run();
    }
    queued += 1;
  }

  return { queued };
}

export async function registerProviderDetection(
  env: SeedEnv,
  input: ProviderDetectionInput,
): Promise<{ detectionKey: string; eligibleForEnrichment: boolean }> {
  const day = (input.detectedAt ?? nowIso()).slice(0, 10);
  const providerKey = input.providerId ?? `asn:${input.asn ?? "unknown"}:${input.normalizedName ?? "unknown"}`;
  const detectionKey = `${providerKey}:${day}`;

  if (!env.DB) {
    return { detectionKey, eligibleForEnrichment: false };
  }

  // GH#956 — distinct_installations_approx e distinct_days NAO sao mais
  // fabricados via MAX(x,3)/MAX(x,2) no conflito. distinct_days de uma linha
  // e sempre 1 (detection_key ja e escopado a um unico dia via `day` acima —
  // a contagem cross-day real acontece em summarizeDetectionGroup, que
  // agrupa varias linhas/dias). distinct_installations_approx e recalculado
  // logo abaixo a partir da evidencia real em provider_detection_installations.
  await env.DB.prepare(
    `INSERT INTO provider_detection_stats (
      detection_key,
      provider_id,
      asn,
      raw_name_sample,
      normalized_name,
      test_count_total,
      test_count_since_review,
      distinct_installations_approx,
      distinct_days,
      first_seen_at,
      last_seen_at,
      last_enrichment_queued_at,
      last_review_at
    ) VALUES (?, ?, ?, ?, ?, 1, 1, 0, 1, ?, ?, NULL, NULL)
    ON CONFLICT(detection_key) DO UPDATE SET
      test_count_total = test_count_total + 1,
      test_count_since_review = test_count_since_review + 1,
      last_seen_at = excluded.last_seen_at`,
  ).bind(
    detectionKey,
    input.providerId ?? null,
    input.asn ?? null,
    input.rawNameSample ?? null,
    input.normalizedName ?? null,
    input.detectedAt ?? nowIso(),
    input.detectedAt ?? nowIso(),
  ).run();

  // installationHash e a unica evidencia que conta pra distinct_installations —
  // sem ele, o hit ainda soma test_count_total mas nunca aproxima elegibilidade
  // sozinho (fecha o vetor de abuso descrito na #956: 1 device batendo varias
  // vezes nao inflava mais nada alem do proprio contador de testes).
  if (input.installationHash) {
    await env.DB.prepare(
      `INSERT OR IGNORE INTO provider_detection_installations (detection_key, installation_hash, first_seen_at)
       VALUES (?, ?, ?)`,
    ).bind(detectionKey, input.installationHash, input.detectedAt ?? nowIso()).run();
  }

  const installationCount = await env.DB.prepare(
    `SELECT COUNT(*) as count FROM provider_detection_installations WHERE detection_key = ?`,
  ).bind(detectionKey).first<{ count: number }>();
  const distinctInstallationsApprox = installationCount?.count ?? 0;

  await env.DB.prepare(
    `UPDATE provider_detection_stats SET distinct_installations_approx = ? WHERE detection_key = ?`,
  ).bind(distinctInstallationsApprox, detectionKey).run();

  const stats = await env.DB.prepare(
    `SELECT test_count_total, distinct_days
     FROM provider_detection_stats
     WHERE detection_key = ?`,
  ).bind(detectionKey).first<{ test_count_total: number; distinct_days: number }>();

  // Elegibilidade real (cross-day) so e avaliada em listProviderReviewQueue /
  // enqueuePendingProviderReviews. Este retorno e uma estimativa otimista do
  // proprio hit (single-day), mantida so pra compatibilidade do contrato de
  // resposta do endpoint — na pratica so fica true se o mesmo dia sozinho ja
  // tivesse 5+ testes E 3+ installs distintos, o que nunca cobre distinct_days>=2.
  const eligibleForEnrichment = Boolean(
    stats
    && stats.test_count_total >= 5
    && distinctInstallationsApprox >= 3
    && stats.distinct_days >= 2,
  );

  return { detectionKey, eligibleForEnrichment };
}

// GH#965 — mapa canal->coluna de ProviderSupport, reaproveitando os mesmos
// CHANNEL_TYPE ja usados por upsertProvider/mapChannelsToSupport (nao inventa
// vocabulario novo).
const SUPPORT_CHANNEL_MAP: Array<[string, keyof ProviderSupport]> = [
  ["SAC_PHONE", "sacPhone"],
  ["TECH_SUPPORT_PHONE", "technicalSupportPhone"],
  ["WHATSAPP_URL", "whatsappUrl"],
  ["WEBSITE_URL", "websiteUrl"],
  ["CUSTOMER_AREA_URL", "customerAreaUrl"],
  ["OMBUDSMAN_PHONE", "ombudsmanPhone"],
];

/**
 * GH#965 — endpoint admin dedicado para editar SO os campos de contato
 * (telefone/WhatsApp/site/etc.) de um provedor ja existente, sem exigir o
 * payload completo de `upsertProvider` (displayName, aliases, asns...).
 *
 * Atualizacao parcial de verdade: campo AUSENTE no `support` recebido fica
 * intocado (nao apaga o que ja estava cadastrado); campo presente com valor
 * `null`/vazio DESATIVA o canal (remove); campo presente com valor novo
 * substitui o anterior. Retorna `null` quando o provedor nao existe (o
 * caller em index.ts converte para 404).
 */
export async function updateProviderSupport(
  env: SeedEnv,
  providerId: string,
  support: Partial<ProviderSupport>,
): Promise<{ ok: true; providerId: string } | null> {
  if (!env.DB) {
    throw new Error("DB binding not configured.");
  }

  const existing = await env.DB.prepare("SELECT id FROM providers WHERE id = ?")
    .bind(providerId)
    .first<{ id: string }>();
  if (!existing) return null;

  const timestamp = nowIso();

  for (const [channelType, field] of SUPPORT_CHANNEL_MAP) {
    if (!(field in support)) continue; // campo ausente do payload -> nao mexe

    await env.DB.prepare(
      "UPDATE provider_channels SET is_active = 0 WHERE provider_id = ? AND channel_type = ?",
    ).bind(providerId, channelType).run();

    const value = support[field];
    if (!value) continue; // valor null/vazio explicito -> so desativa (remove)

    await env.DB.prepare(
      `INSERT INTO provider_channels (
        provider_id, channel_type, label, value, source_url, verification_status,
        verified_at, next_review_at, priority, is_active
      ) VALUES (?, ?, NULL, ?, NULL, 'VERIFIED', ?, NULL, 100, 1)`,
    ).bind(providerId, channelType, value, timestamp).run();
  }

  await env.DB.prepare("UPDATE providers SET updated_at = ? WHERE id = ?")
    .bind(timestamp, providerId)
    .run();

  return { ok: true, providerId };
}

/**
 * GH#965 (revisado 2026-07-14) — upload de logo por `providerId`, gravado
 * como BLOB base64 direto no D1 (`provider_assets.data_base64`).
 *
 * R2 foi descartado por decisao de produto: a Cloudflare exige cartao de
 * credito cadastrado pra habilitar R2 mesmo no tier gratis, fricção que o
 * Luiz decidiu nao pagar agora (logos de operadora sao pequenas, poucos KB,
 * volume baixo — D1 aguenta). `GET /providers/:id/logo` (`index.ts`) serve o
 * binario de volta a partir daqui.
 */
export async function uploadProviderLogo(
  env: ProviderLogoEnv,
  providerId: string,
  bytes: ArrayBuffer,
  contentType: string,
): Promise<{ ok: true; url: string; version: number } | { ok: false; status: number; error: string }> {
  if (!env.DB) {
    return { ok: false, status: 500, error: "DB binding not configured." };
  }
  if (bytes.byteLength > MAX_LOGO_BYTES) {
    return {
      ok: false,
      status: 413,
      error: `Logo too large. Max ${MAX_LOGO_BYTES / 1024}KB, got ${Math.ceil(bytes.byteLength / 1024)}KB.`,
    };
  }

  const provider = await getProviderById(env, providerId);
  if (!provider) {
    return { ok: false, status: 404, error: "Provider not found." };
  }

  const extension = contentType === "image/png"
    ? "png"
    : contentType === "image/svg+xml"
    ? "svg"
    : contentType === "image/jpeg"
    ? "jpg"
    : "webp";
  const nextVersion = (provider.logo?.version ?? 0) + 1;
  // Vestigial: nao e mais um path de R2, so um identificador descritivo do
  // asset (coluna `r2_key` continua NOT NULL no schema, ver migration 006).
  const assetKey = `providers/${providerId}/logo-square-v${nextVersion}.${extension}`;
  const timestamp = nowIso();
  const dataBase64 = bytesToBase64(bytes);
  const url = buildLogoUrl(env, providerId);

  await env.DB.prepare(
    `INSERT INTO provider_assets (
      provider_id, asset_type, r2_key, source_url, file_hash, version, verification_status, data_base64, content_type, created_at
    ) VALUES (?, 'LOGO_SQUARE', ?, NULL, NULL, ?, 'VERIFIED', ?, ?, ?)`,
  ).bind(providerId, assetKey, nextVersion, dataBase64, contentType, timestamp).run();

  await env.DB.prepare(
    "UPDATE providers SET logo_version = ?, updated_at = ? WHERE id = ?",
  ).bind(nextVersion, timestamp, providerId).run();

  return { ok: true, url, version: nextVersion };
}

/**
 * GH#965 — busca o binario da logo mais recente de `providerId` gravado via
 * [uploadProviderLogo], pra `GET /providers/:id/logo` (`index.ts`) servir de
 * volta. `null` quando o provedor nao tem logo com blob no D1 (ex.: logo
 * seeded local, ou aponta pra `sourceUrl` externo via `upsertProvider`).
 */
export async function getProviderLogoBinary(
  env: SeedEnv,
  providerId: string,
): Promise<{ bytes: Uint8Array; contentType: string } | null> {
  if (!env.DB) return null;
  const row = await env.DB.prepare(
    `SELECT data_base64, content_type
     FROM provider_assets
     WHERE provider_id = ? AND asset_type = 'LOGO_SQUARE' AND data_base64 IS NOT NULL
     ORDER BY version DESC, id DESC
     LIMIT 1`,
  ).bind(providerId).first<{ data_base64: string; content_type: string | null }>();
  if (!row) return null;
  return { bytes: base64ToBytes(row.data_base64), contentType: row.content_type ?? "application/octet-stream" };
}
