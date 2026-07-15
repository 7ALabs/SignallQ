import type {
  GameAuditEntry,
  GameCatalogAdminInput,
  GameCatalogRecord,
  GameCatalogVersion,
  GameProfileAdminInput,
  GameProfileRecord,
} from "./contracts.ts";

type Env = {
  DB?: D1Database;
  GAME_CATALOG_SEED_JSON?: string;
  GAME_PROFILE_SEED_JSON?: string;
};

type GameRow = {
  game_id: string;
  display_name: string;
  slug: string;
  active: number;
  profile_code: string;
  test_strategy: string;
  region_code: string;
  result_label: string;
  provider_network_mode: string;
  sort_order: number;
  icon_key: string | null;
  created_at: string;
  updated_at: string;
};

type GamePlatformRow = {
  platform_code: string;
};

type GameProfileRow = {
  profile_code: string;
  display_name: string;
  latency_good_max: number | null;
  latency_attention_max: number | null;
  jitter_good_max: number | null;
  jitter_attention_max: number | null;
  loss_good_max: number | null;
  loss_attention_max: number | null;
  download_good_min: number | null;
  download_attention_min: number | null;
  bufferbloat_good_max: number | null;
  bufferbloat_attention_max: number | null;
  wifi_policy: string | null;
  updated_at: string;
};

const BUILTIN_GAME_PROFILES: GameProfileRecord[] = [
  {
    profileCode: "COMPETITIVE_EXTREME",
    displayName: "Competitivo extremo",
    latencyGoodMax: 30,
    latencyAttentionMax: 80,
    jitterGoodMax: 5,
    jitterAttentionMax: 20,
    lossGoodMax: 0,
    lossAttentionMax: 1,
    downloadGoodMin: null,
    downloadAttentionMin: null,
    bufferbloatGoodMax: null,
    bufferbloatAttentionMax: null,
    wifiPolicy: "STRICT_WIFI_5GHZ_PREFERRED",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    profileCode: "COMPETITIVE",
    displayName: "Competitivo",
    latencyGoodMax: 50,
    latencyAttentionMax: 120,
    jitterGoodMax: 10,
    jitterAttentionMax: 30,
    lossGoodMax: 0,
    lossAttentionMax: 1,
    downloadGoodMin: null,
    downloadAttentionMin: null,
    bufferbloatGoodMax: null,
    bufferbloatAttentionMax: null,
    wifiPolicy: "WIFI_5GHZ_RECOMMENDED",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    profileCode: "SPORTS_COMPETITIVE",
    displayName: "Esporte competitivo",
    latencyGoodMax: 40,
    latencyAttentionMax: 100,
    jitterGoodMax: 5,
    jitterAttentionMax: 20,
    lossGoodMax: 0,
    lossAttentionMax: 1,
    downloadGoodMin: null,
    downloadAttentionMin: null,
    bufferbloatGoodMax: null,
    bufferbloatAttentionMax: null,
    wifiPolicy: "LOW_JITTER_REQUIRED",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    profileCode: "MULTIPLAYER_MODERATE",
    displayName: "Multiplayer moderado",
    latencyGoodMax: 60,
    latencyAttentionMax: 150,
    jitterGoodMax: 10,
    jitterAttentionMax: 30,
    lossGoodMax: 0,
    lossAttentionMax: 1,
    downloadGoodMin: null,
    downloadAttentionMin: null,
    bufferbloatGoodMax: null,
    bufferbloatAttentionMax: null,
    wifiPolicy: "BALANCED",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
];

const BUILTIN_GAME_CATALOG: GameCatalogRecord[] = [
  {
    gameId: "fortnite",
    displayName: "Fortnite",
    slug: "fortnite",
    active: true,
    profileCode: "COMPETITIVE",
    testStrategy: "REGIONAL_ESTIMATE",
    regionCode: "SOUTH_AMERICA",
    resultLabel: "Estimativa para Fortnite",
    providerNetworkMode: "fallback_regional",
    sortOrder: 10,
    iconKey: null,
    platforms: ["PC", "PS5", "XBOX"],
    createdAt: "2026-07-14T00:00:00.000Z",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    gameId: "warzone",
    displayName: "Call of Duty: Warzone",
    slug: "call-of-duty-warzone",
    active: true,
    profileCode: "COMPETITIVE",
    testStrategy: "REGIONAL_ESTIMATE",
    regionCode: "SOUTH_AMERICA",
    resultLabel: "Estimativa para Call of Duty: Warzone",
    providerNetworkMode: "fallback_regional",
    sortOrder: 20,
    iconKey: null,
    platforms: ["PC", "PS5", "XBOX"],
    createdAt: "2026-07-14T00:00:00.000Z",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    gameId: "valorant",
    displayName: "VALORANT",
    slug: "valorant",
    active: true,
    profileCode: "COMPETITIVE_EXTREME",
    testStrategy: "PROVIDER_NETWORK",
    regionCode: "BRAZIL",
    resultLabel: "Latência até a rede da Riot",
    providerNetworkMode: "fallback_regional",
    sortOrder: 30,
    iconKey: null,
    platforms: ["PC"],
    createdAt: "2026-07-14T00:00:00.000Z",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    gameId: "cs2",
    displayName: "Counter-Strike 2",
    slug: "counter-strike-2",
    active: true,
    profileCode: "COMPETITIVE_EXTREME",
    testStrategy: "PROVIDER_NETWORK",
    regionCode: "SOUTH_AMERICA",
    resultLabel: "Latência até a rede do Counter-Strike 2",
    providerNetworkMode: "fallback_regional",
    sortOrder: 40,
    iconKey: null,
    platforms: ["PC"],
    createdAt: "2026-07-14T00:00:00.000Z",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    gameId: "ea-sports-fc",
    displayName: "EA Sports FC",
    slug: "ea-sports-fc",
    active: true,
    profileCode: "SPORTS_COMPETITIVE",
    testStrategy: "REGIONAL_ESTIMATE",
    regionCode: "SOUTH_AMERICA",
    resultLabel: "Estimativa para EA Sports FC",
    providerNetworkMode: "fallback_regional",
    sortOrder: 50,
    iconKey: null,
    platforms: ["PC", "PS5", "XBOX"],
    createdAt: "2026-07-14T00:00:00.000Z",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
  {
    gameId: "dead-by-daylight",
    displayName: "Dead by Daylight",
    slug: "dead-by-daylight",
    active: true,
    profileCode: "MULTIPLAYER_MODERATE",
    testStrategy: "REGIONAL_ESTIMATE",
    regionCode: "SOUTH_AMERICA",
    resultLabel: "Estimativa para Dead by Daylight",
    providerNetworkMode: "fallback_regional",
    sortOrder: 60,
    iconKey: null,
    platforms: ["PC", "PS5", "XBOX"],
    createdAt: "2026-07-14T00:00:00.000Z",
    updatedAt: "2026-07-14T00:00:00.000Z",
  },
];

function nowIso(): string {
  return new Date().toISOString();
}

function normalizePlatform(value: string): string {
  return value.trim().toUpperCase();
}

async function loadSeedProfiles(env: Env): Promise<GameProfileRecord[]> {
  if (!env.GAME_PROFILE_SEED_JSON) return BUILTIN_GAME_PROFILES;
  try {
    const parsed = JSON.parse(env.GAME_PROFILE_SEED_JSON) as GameProfileRecord[];
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : BUILTIN_GAME_PROFILES;
  } catch {
    return BUILTIN_GAME_PROFILES;
  }
}

async function loadSeedCatalog(env: Env): Promise<GameCatalogRecord[]> {
  if (!env.GAME_CATALOG_SEED_JSON) return BUILTIN_GAME_CATALOG;
  try {
    const parsed = JSON.parse(env.GAME_CATALOG_SEED_JSON) as GameCatalogRecord[];
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : BUILTIN_GAME_CATALOG;
  } catch {
    return BUILTIN_GAME_CATALOG;
  }
}

function mapProfileRow(row: GameProfileRow): GameProfileRecord {
  return {
    profileCode: row.profile_code,
    displayName: row.display_name,
    latencyGoodMax: row.latency_good_max,
    latencyAttentionMax: row.latency_attention_max,
    jitterGoodMax: row.jitter_good_max,
    jitterAttentionMax: row.jitter_attention_max,
    lossGoodMax: row.loss_good_max,
    lossAttentionMax: row.loss_attention_max,
    downloadGoodMin: row.download_good_min,
    downloadAttentionMin: row.download_attention_min,
    bufferbloatGoodMax: row.bufferbloat_good_max,
    bufferbloatAttentionMax: row.bufferbloat_attention_max,
    wifiPolicy: row.wifi_policy,
    updatedAt: row.updated_at,
  };
}

async function loadPlatforms(db: D1Database, gameId: string): Promise<string[]> {
  const { results } = await db.prepare(
    "SELECT platform_code FROM game_platforms WHERE game_id = ? ORDER BY platform_code ASC",
  ).bind(gameId).all<GamePlatformRow>();
  return results.map((row) => row.platform_code);
}

async function loadGameFromDb(db: D1Database, gameId: string): Promise<GameCatalogRecord | null> {
  const row = await db.prepare(
    `SELECT game_id, display_name, slug, active, profile_code, test_strategy, region_code,
            result_label, provider_network_mode, sort_order, icon_key, created_at, updated_at
     FROM game_catalog
     WHERE game_id = ?`,
  ).bind(gameId).first<GameRow>();
  if (!row) return null;
  return {
    gameId: row.game_id,
    displayName: row.display_name,
    slug: row.slug,
    active: row.active === 1,
    profileCode: row.profile_code,
    testStrategy: row.test_strategy,
    regionCode: row.region_code,
    resultLabel: row.result_label,
    providerNetworkMode: row.provider_network_mode,
    sortOrder: row.sort_order,
    iconKey: row.icon_key,
    platforms: await loadPlatforms(db, row.game_id),
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

async function writeAudit(
  db: D1Database,
  entityType: string,
  entityId: string,
  action: string,
  actor: string,
  beforeJson: string | null,
  afterJson: string | null,
): Promise<void> {
  await db.prepare(
    `INSERT INTO game_catalog_audit (id, entity_type, entity_id, action, actor, before_json, after_json, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
  ).bind(
    crypto.randomUUID(),
    entityType,
    entityId,
    action,
    actor,
    beforeJson,
    afterJson,
    nowIso(),
  ).run();
}

export async function listGameCatalog(env: Env, platform?: string | null): Promise<GameCatalogRecord[]> {
  if (env.DB) {
    const { results } = await env.DB.prepare(
      `SELECT game_id
       FROM game_catalog
       WHERE (? IS NULL OR game_id IN (
         SELECT game_id FROM game_platforms WHERE platform_code = ?
       ))
       ORDER BY sort_order ASC, display_name ASC`,
    ).bind(platform ? normalizePlatform(platform) : null, platform ? normalizePlatform(platform) : null).all<{ game_id: string }>();
    const items = await Promise.all(results.map((row) => loadGameFromDb(env.DB!, row.game_id)));
    return items.filter((item): item is GameCatalogRecord => Boolean(item)).filter((item) => item.active);
  }

  const catalog = await loadSeedCatalog(env);
  return catalog
    .filter((item) => item.active)
    .filter((item) => !platform || item.platforms.includes(normalizePlatform(platform)))
    .sort((left, right) => left.sortOrder - right.sortOrder || left.displayName.localeCompare(right.displayName));
}

export async function getGameCatalogItem(env: Env, gameId: string): Promise<GameCatalogRecord | null> {
  if (env.DB) {
    return loadGameFromDb(env.DB, gameId);
  }
  const catalog = await loadSeedCatalog(env);
  return catalog.find((item) => item.gameId === gameId) ?? null;
}

export async function getGameCatalogVersion(env: Env): Promise<GameCatalogVersion> {
  const catalog = await listGameCatalog(env);
  const latest = catalog.reduce((max, item) => item.updatedAt > max ? item.updatedAt : max, "1970-01-01T00:00:00.000Z");
  return {
    version: `games-${latest}`,
    totalGames: catalog.length,
    generatedAt: nowIso(),
  };
}

export async function listGameProfiles(env: Env): Promise<GameProfileRecord[]> {
  if (env.DB) {
    const { results } = await env.DB.prepare(
      `SELECT profile_code, display_name, latency_good_max, latency_attention_max, jitter_good_max,
              jitter_attention_max, loss_good_max, loss_attention_max, download_good_min,
              download_attention_min, bufferbloat_good_max, bufferbloat_attention_max, wifi_policy, updated_at
       FROM game_profiles
       ORDER BY profile_code ASC`,
    ).all<GameProfileRow>();
    return results.map(mapProfileRow);
  }
  return loadSeedProfiles(env);
}

export async function upsertGameProfile(env: Env, input: GameProfileAdminInput, actor: string): Promise<{ profileCode: string }> {
  if (!env.DB) throw new Error("DB binding not configured.");
  const timestamp = nowIso();
  const previous = await env.DB.prepare(
    `SELECT profile_code, display_name, latency_good_max, latency_attention_max, jitter_good_max,
            jitter_attention_max, loss_good_max, loss_attention_max, download_good_min,
            download_attention_min, bufferbloat_good_max, bufferbloat_attention_max, wifi_policy, updated_at
     FROM game_profiles WHERE profile_code = ?`,
  ).bind(input.profileCode).first<GameProfileRow>();

  await env.DB.prepare(
    `INSERT INTO game_profiles (
      profile_code, display_name, latency_good_max, latency_attention_max, jitter_good_max,
      jitter_attention_max, loss_good_max, loss_attention_max, download_good_min,
      download_attention_min, bufferbloat_good_max, bufferbloat_attention_max, wifi_policy, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(profile_code) DO UPDATE SET
      display_name = excluded.display_name,
      latency_good_max = excluded.latency_good_max,
      latency_attention_max = excluded.latency_attention_max,
      jitter_good_max = excluded.jitter_good_max,
      jitter_attention_max = excluded.jitter_attention_max,
      loss_good_max = excluded.loss_good_max,
      loss_attention_max = excluded.loss_attention_max,
      download_good_min = excluded.download_good_min,
      download_attention_min = excluded.download_attention_min,
      bufferbloat_good_max = excluded.bufferbloat_good_max,
      bufferbloat_attention_max = excluded.bufferbloat_attention_max,
      wifi_policy = excluded.wifi_policy,
      updated_at = excluded.updated_at`,
  ).bind(
    input.profileCode,
    input.displayName,
    input.latencyGoodMax ?? null,
    input.latencyAttentionMax ?? null,
    input.jitterGoodMax ?? null,
    input.jitterAttentionMax ?? null,
    input.lossGoodMax ?? null,
    input.lossAttentionMax ?? null,
    input.downloadGoodMin ?? null,
    input.downloadAttentionMin ?? null,
    input.bufferbloatGoodMax ?? null,
    input.bufferbloatAttentionMax ?? null,
    input.wifiPolicy ?? null,
    timestamp,
  ).run();

  const current = await env.DB.prepare(
    `SELECT profile_code, display_name, latency_good_max, latency_attention_max, jitter_good_max,
            jitter_attention_max, loss_good_max, loss_attention_max, download_good_min,
            download_attention_min, bufferbloat_good_max, bufferbloat_attention_max, wifi_policy, updated_at
     FROM game_profiles WHERE profile_code = ?`,
  ).bind(input.profileCode).first<GameProfileRow>();

  await writeAudit(
    env.DB,
    "game_profile",
    input.profileCode,
    previous ? "UPSERT_PROFILE" : "CREATE_PROFILE",
    actor,
    previous ? JSON.stringify(mapProfileRow(previous)) : null,
    current ? JSON.stringify(mapProfileRow(current)) : null,
  );

  return { profileCode: input.profileCode };
}

export async function upsertGameCatalogItem(env: Env, input: GameCatalogAdminInput, actor: string): Promise<{ gameId: string }> {
  if (!env.DB) throw new Error("DB binding not configured.");
  const timestamp = nowIso();
  const previous = await loadGameFromDb(env.DB, input.gameId);

  await env.DB.prepare(
    `INSERT INTO game_catalog (
      game_id, display_name, slug, active, profile_code, test_strategy, region_code,
      result_label, provider_network_mode, sort_order, icon_key, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(game_id) DO UPDATE SET
      display_name = excluded.display_name,
      slug = excluded.slug,
      active = excluded.active,
      profile_code = excluded.profile_code,
      test_strategy = excluded.test_strategy,
      region_code = excluded.region_code,
      result_label = excluded.result_label,
      provider_network_mode = excluded.provider_network_mode,
      sort_order = excluded.sort_order,
      icon_key = excluded.icon_key,
      updated_at = excluded.updated_at`,
  ).bind(
    input.gameId,
    input.displayName,
    input.slug,
    input.active === false ? 0 : 1,
    input.profileCode,
    input.testStrategy,
    input.regionCode,
    input.resultLabel,
    input.providerNetworkMode ?? "fallback_regional",
    input.sortOrder ?? 0,
    input.iconKey ?? null,
    previous?.createdAt ?? timestamp,
    timestamp,
  ).run();

  await env.DB.prepare("DELETE FROM game_platforms WHERE game_id = ?").bind(input.gameId).run();
  for (const platform of [...new Set(input.platforms.map(normalizePlatform))]) {
    await env.DB.prepare(
      "INSERT INTO game_platforms (game_id, platform_code) VALUES (?, ?)",
    ).bind(input.gameId, platform).run();
  }

  const current = await loadGameFromDb(env.DB, input.gameId);
  await writeAudit(
    env.DB,
    "game_catalog",
    input.gameId,
    previous ? "UPSERT_GAME" : "CREATE_GAME",
    actor,
    previous ? JSON.stringify(previous) : null,
    current ? JSON.stringify(current) : null,
  );

  return { gameId: input.gameId };
}

export async function setGameCatalogItemActive(env: Env, gameId: string, active: boolean, actor: string): Promise<{ gameId: string; active: boolean }> {
  if (!env.DB) throw new Error("DB binding not configured.");
  const previous = await loadGameFromDb(env.DB, gameId);
  if (!previous) throw new Error("Game not found.");
  await env.DB.prepare(
    "UPDATE game_catalog SET active = ?, updated_at = ? WHERE game_id = ?",
  ).bind(active ? 1 : 0, nowIso(), gameId).run();
  const current = await loadGameFromDb(env.DB, gameId);
  await writeAudit(
    env.DB,
    "game_catalog",
    gameId,
    active ? "ACTIVATE_GAME" : "DEACTIVATE_GAME",
    actor,
    JSON.stringify(previous),
    current ? JSON.stringify(current) : null,
  );
  return { gameId, active };
}

export async function listGameAudit(env: Env): Promise<GameAuditEntry[]> {
  if (!env.DB) return [];
  const { results } = await env.DB.prepare(
    `SELECT id, entity_type, entity_id, action, actor, before_json, after_json, created_at
     FROM game_catalog_audit
     ORDER BY created_at DESC
     LIMIT 100`,
  ).all<{
    id: string;
    entity_type: string;
    entity_id: string;
    action: string;
    actor: string;
    before_json: string | null;
    after_json: string | null;
    created_at: string;
  }>();
  return results.map((row) => ({
    id: row.id,
    entityType: row.entity_type,
    entityId: row.entity_id,
    action: row.action,
    actor: row.actor,
    beforeJson: row.before_json,
    afterJson: row.after_json,
    createdAt: row.created_at,
  }));
}

export async function syncGameSeedToDb(env: Env, actor = "seed-sync"): Promise<{ syncedGames: number; syncedProfiles: number }> {
  if (!env.DB) throw new Error("DB binding not configured.");
  const profiles = await loadSeedProfiles(env);
  const games = await loadSeedCatalog(env);

  for (const profile of profiles) {
    await upsertGameProfile(env, profile, actor);
  }
  for (const game of games) {
    await upsertGameCatalogItem(env, game, actor);
  }

  return {
    syncedGames: games.length,
    syncedProfiles: profiles.length,
  };
}
