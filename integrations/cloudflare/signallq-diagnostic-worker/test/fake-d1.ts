type Row = Record<string, unknown>;

type ProviderIdentifierRow = {
  provider_id: string;
  identifier_type: string;
  original_value: string;
  normalized_value: string;
  is_active: number;
  verified_at: string | null;
};

type ProviderChannelRow = {
  provider_id: string;
  channel_type: string;
  value: string;
  priority: number;
  is_active: number;
};

type ProviderAssetRow = {
  provider_id: string;
  asset_type: string;
  r2_key: string;
  source_url: string | null;
  version: number;
  data_base64: string | null;
  content_type: string | null;
  created_at: string;
};

type DetectionStatsRow = {
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

type EnrichmentJobRow = {
  id: string;
  provider_id: string | null;
  detection_key: string;
  reason: string;
  status: string;
  idempotency_key: string;
  attempt_count: number;
  started_at: string | null;
  completed_at: string | null;
  error_message: string | null;
  created_at: string;
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

type GameCatalogRow = {
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
  game_id: string;
  platform_code: string;
};

type GameAuditRow = {
  id: string;
  entity_type: string;
  entity_id: string;
  action: string;
  actor: string;
  before_json: string | null;
  after_json: string | null;
  created_at: string;
};

function normalize(sql: string): string {
  return sql.replace(/\s+/g, " ").trim().toLowerCase();
}

class FakeStatement {
  private bindings: unknown[] = [];
  private readonly db: FakeD1Database;
  private readonly sql: string;

  constructor(db: FakeD1Database, sql: string) {
    this.db = db;
    this.sql = sql;
  }

  bind(...values: unknown[]): FakeStatement {
    this.bindings = values;
    return this;
  }

  async run(): Promise<{ success: boolean }> {
    this.db.execute(this.sql, this.bindings);
    return { success: true };
  }

  async first<T>(): Promise<T | null> {
    return this.db.first(this.sql, this.bindings) as T | null;
  }

  async all<T>(): Promise<{ results: T[] }> {
    return { results: this.db.all(this.sql, this.bindings) as T[] };
  }
}

export class FakeD1Database {
  adminUsers = new Map<string, Row>();
  adminUsersByEmail = new Map<string, string>();
  adminSessions = new Map<string, Row>();
  authRateLimit = new Map<string, Row>();
  diagnosticRulesets = new Map<number, Row>();
  diagnosticRuleAuditLog: Row[] = [];

  providers = new Map<string, Row>();
  providerIdentifiers: ProviderIdentifierRow[] = [];
  providerChannels: ProviderChannelRow[] = [];
  providerAssets: ProviderAssetRow[] = [];
  providerDetectionStats = new Map<string, DetectionStatsRow>();
  // GH#956 — (detection_key, installation_hash) -> first_seen_at.
  providerDetectionInstallations = new Map<string, string>();
  providerEnrichmentJobs = new Map<string, EnrichmentJobRow>();
  providerEnrichmentJobsByIdempotency = new Map<string, string>();
  gameProfiles = new Map<string, GameProfileRow>();
  gameCatalog = new Map<string, GameCatalogRow>();
  gamePlatforms: GamePlatformRow[] = [];
  gameAudit: GameAuditRow[] = [];

  prepare(sql: string): FakeStatement {
    return new FakeStatement(this, sql);
  }

  execute(sql: string, bindings: unknown[]): void {
    const q = normalize(sql);

    if (q.startsWith("insert into admin_users")) {
      const [id, email, passwordHash, createdAt] = bindings;
      this.adminUsers.set(String(id), {
        id, email, password_hash: passwordHash, role: "admin", active: 1, created_at: createdAt, last_login: null,
      });
      this.adminUsersByEmail.set(String(email), String(id));
      return;
    }

    if (q.startsWith("update admin_users set last_login")) {
      const [lastLogin, id] = bindings;
      const row = this.adminUsers.get(String(id));
      if (row) row.last_login = lastLogin;
      return;
    }

    if (q.startsWith("insert into admin_sessions")) {
      const [tokenHash, userId, createdAt, expiresAt, lastSeen] = bindings;
      this.adminSessions.set(String(tokenHash), { token_hash: tokenHash, user_id: userId, created_at: createdAt, expires_at: expiresAt, last_seen: lastSeen });
      return;
    }

    if (q.startsWith("update admin_sessions set last_seen")) {
      const [lastSeen, tokenHash] = bindings;
      const row = this.adminSessions.get(String(tokenHash));
      if (row) row.last_seen = lastSeen;
      return;
    }

    if (q.startsWith("delete from admin_sessions")) {
      const [tokenHash] = bindings;
      this.adminSessions.delete(String(tokenHash));
      return;
    }

    if (q.startsWith("insert or replace into auth_rate_limit")) {
      const [ip, count, windowStart] = bindings;
      this.authRateLimit.set(String(ip), { ip, count, window_start: windowStart });
      return;
    }

    if (q.startsWith("update auth_rate_limit set count = count + 1")) {
      const [ip] = bindings;
      const row = this.authRateLimit.get(String(ip));
      if (row) row.count = Number(row.count ?? 0) + 1;
      return;
    }

    if (q.startsWith("insert into diagnostic_rulesets")) {
      const [version, schemaVersion, engineVersion, createdAt, updatedAt, author, justification, rulesJson] = bindings;
      this.diagnosticRulesets.set(Number(version), {
        version,
        schema_version: schemaVersion,
        engine_version: engineVersion,
        status: "DRAFT",
        rollout_percent: 0,
        published_at: null,
        created_at: createdAt,
        updated_at: updatedAt,
        author,
        justification,
        rules_json: rulesJson,
      });
      return;
    }

    if (q.startsWith("update diagnostic_rulesets set status = 'rolled_back'") && q.includes("where status = 'published'")) {
      const [updatedAt] = bindings;
      for (const row of this.diagnosticRulesets.values()) {
        if (row.status === "PUBLISHED") {
          row.status = "ROLLED_BACK";
          row.updated_at = updatedAt;
        }
      }
      return;
    }

    if (q.startsWith("update diagnostic_rulesets set status = 'published'")) {
      const [publishedAt, updatedAt, actor, version] = bindings;
      const row = this.diagnosticRulesets.get(Number(version));
      if (row) {
        row.status = "PUBLISHED";
        row.rollout_percent = 100;
        row.published_at = publishedAt;
        row.updated_at = updatedAt;
        row.author = row.author ?? actor;
      }
      return;
    }

    if (q.startsWith("update diagnostic_rulesets set status = 'rolled_back'") && q.includes("where version = ?")) {
      const [updatedAt, version] = bindings;
      const row = this.diagnosticRulesets.get(Number(version));
      if (row) {
        row.status = "ROLLED_BACK";
        row.updated_at = updatedAt;
      }
      return;
    }

    if (q.startsWith("insert into diagnostic_rule_audit_log")) {
      const [id, rulesetVersion, action, actor, createdAt, detailsJson] = bindings;
      this.diagnosticRuleAuditLog.push({ id, ruleset_version: rulesetVersion, action, actor, created_at: createdAt, details_json: detailsJson });
      return;
    }

    if (q.startsWith("insert into providers")) {
      const [
        id,
        displayName,
        legalName,
        cnpj,
        providerType,
        status,
        officialDomain,
        logoVersion,
        lastVerifiedAt,
        nextReviewAt,
        createdAt,
        updatedAt,
      ] = bindings;
      const existing = this.providers.get(String(id));
      this.providers.set(String(id), {
        id,
        display_name: displayName,
        legal_name: legalName,
        cnpj,
        provider_type: providerType,
        status,
        official_domain: officialDomain,
        logo_version: logoVersion,
        last_verified_at: lastVerifiedAt,
        next_review_at: nextReviewAt,
        created_at: existing?.created_at ?? createdAt,
        updated_at: updatedAt,
      });
      return;
    }

    if (q.startsWith("update provider_identifiers set is_active = 0 where provider_id = ?")) {
      const [providerId] = bindings;
      this.providerIdentifiers = this.providerIdentifiers.map((row) =>
        row.provider_id === String(providerId) ? { ...row, is_active: 0 } : row,
      );
      return;
    }

    if (q.startsWith("update provider_channels set is_active = 0 where provider_id = ? and channel_type = ?")) {
      const [providerId, channelType] = bindings;
      this.providerChannels = this.providerChannels.map((row) =>
        row.provider_id === String(providerId) && row.channel_type === String(channelType)
          ? { ...row, is_active: 0 }
          : row,
      );
      return;
    }

    if (q.startsWith("update provider_channels set is_active = 0 where provider_id = ?")) {
      const [providerId] = bindings;
      this.providerChannels = this.providerChannels.map((row) =>
        row.provider_id === String(providerId) ? { ...row, is_active: 0 } : row,
      );
      return;
    }

    if (q.startsWith("update providers set updated_at = ? where id = ?")) {
      const [updatedAt, providerId] = bindings;
      const row = this.providers.get(String(providerId));
      if (row) row.updated_at = updatedAt;
      return;
    }

    if (q.startsWith("update providers set logo_version = ?, updated_at = ? where id = ?")) {
      const [logoVersion, updatedAt, providerId] = bindings;
      const row = this.providers.get(String(providerId));
      if (row) {
        row.logo_version = Number(logoVersion);
        row.updated_at = updatedAt;
      }
      return;
    }

    if (q.startsWith("insert into provider_identifiers")) {
      const [providerId, originalValue, normalizedValue, verifiedAt] = bindings;
      const typeMatch = sql.match(/values \(\?, '([^']+)'/i);
      this.providerIdentifiers.push({
        provider_id: String(providerId),
        identifier_type: typeMatch?.[1] ?? "ALIAS",
        original_value: String(originalValue),
        normalized_value: String(normalizedValue),
        is_active: 1,
        verified_at: (verifiedAt as string | null) ?? null,
      });
      return;
    }

    if (q.startsWith("insert into provider_channels")) {
      const [providerId, channelType, value] = bindings;
      this.providerChannels.push({
        provider_id: String(providerId),
        channel_type: String(channelType),
        value: String(value),
        priority: 100,
        is_active: 1,
      });
      return;
    }

    if (q.startsWith("insert into provider_assets")) {
      if (bindings.length === 6) {
        // GH#965 (D1 direto) — uploadProviderLogo: providerId, assetKey, version, dataBase64, contentType, createdAt
        const [providerId, r2Key, version, dataBase64, contentType, createdAt] = bindings;
        this.providerAssets.push({
          provider_id: String(providerId),
          asset_type: "LOGO_SQUARE",
          r2_key: String(r2Key),
          source_url: null,
          version: Number(version),
          data_base64: String(dataBase64),
          content_type: String(contentType),
          created_at: String(createdAt),
        });
        return;
      }
      // upsertProvider — ponteiro manual de logo externo: providerId, r2Key, sourceUrl, version, createdAt
      const [providerId, r2Key, sourceUrl, version, createdAt] = bindings;
      this.providerAssets.push({
        provider_id: String(providerId),
        asset_type: "LOGO_SQUARE",
        r2_key: String(r2Key),
        source_url: (sourceUrl as string | null) ?? null,
        version: Number(version),
        data_base64: null,
        content_type: null,
        created_at: String(createdAt),
      });
      return;
    }

    if (q.startsWith("insert into provider_detection_stats")) {
      const [detectionKey, providerId, asn, rawNameSample, normalizedName, firstSeenAt, lastSeenAt] = bindings;
      const existing = this.providerDetectionStats.get(String(detectionKey));
      if (!existing) {
        this.providerDetectionStats.set(String(detectionKey), {
          detection_key: String(detectionKey),
          provider_id: (providerId as string | null) ?? null,
          asn: asn == null ? null : Number(asn),
          raw_name_sample: (rawNameSample as string | null) ?? null,
          normalized_name: (normalizedName as string | null) ?? null,
          test_count_total: 1,
          test_count_since_review: 1,
          // GH#956 — distinct_installations_approx nao e mais fabricado aqui;
          // fica 0 ate a evidencia real de installationHash ser contada
          // (ver "update provider_detection_stats set distinct_installations_approx").
          distinct_installations_approx: 0,
          distinct_days: 1,
          first_seen_at: String(firstSeenAt),
          last_seen_at: String(lastSeenAt),
          last_enrichment_queued_at: null,
          last_review_at: null,
        });
      } else {
        existing.test_count_total += 1;
        existing.test_count_since_review += 1;
        existing.last_seen_at = String(lastSeenAt);
      }
      return;
    }

    if (q.startsWith("insert or ignore into provider_detection_installations")) {
      const [detectionKey, installationHash, firstSeenAt] = bindings;
      const key = `${String(detectionKey)}::${String(installationHash)}`;
      if (!this.providerDetectionInstallations.has(key)) {
        this.providerDetectionInstallations.set(key, String(firstSeenAt));
      }
      return;
    }

    if (q.startsWith("update provider_detection_stats set distinct_installations_approx = ?")) {
      const [count, detectionKey] = bindings;
      const row = this.providerDetectionStats.get(String(detectionKey));
      if (row) row.distinct_installations_approx = Number(count);
      return;
    }

    if (q.startsWith("update provider_detection_stats set last_enrichment_queued_at = ?")) {
      const [timestamp, detectionKey] = bindings;
      const row = this.providerDetectionStats.get(String(detectionKey));
      if (row) row.last_enrichment_queued_at = String(timestamp);
      return;
    }

    if (q.startsWith("update provider_detection_stats set test_count_since_review = 0")) {
      const [timestamp, providerId] = bindings;
      for (const row of this.providerDetectionStats.values()) {
        if (row.provider_id === String(providerId)) {
          row.test_count_since_review = 0;
          row.last_review_at = String(timestamp);
        }
      }
      return;
    }

    if (q.startsWith("insert or ignore into provider_enrichment_jobs")) {
      const [id, providerId, detectionKey, reason, idempotencyKey, createdAt] = bindings;
      if (this.providerEnrichmentJobsByIdempotency.has(String(idempotencyKey))) {
        return;
      }
      this.providerEnrichmentJobs.set(String(id), {
        id: String(id),
        provider_id: (providerId as string | null) ?? null,
        detection_key: String(detectionKey),
        reason: String(reason),
        status: "PENDING_REVIEW",
        idempotency_key: String(idempotencyKey),
        attempt_count: 0,
        started_at: null,
        completed_at: null,
        error_message: null,
        created_at: String(createdAt),
      });
      this.providerEnrichmentJobsByIdempotency.set(String(idempotencyKey), String(id));
      return;
    }

    if (q.startsWith("update provider_enrichment_jobs set status = 'completed'")) {
      const [timestamp, errorMessage, providerId] = bindings;
      for (const row of this.providerEnrichmentJobs.values()) {
        if (row.provider_id === String(providerId) && ["PENDING", "PENDING_REVIEW", "RUNNING"].includes(row.status)) {
          row.status = "COMPLETED";
          row.completed_at = String(timestamp);
          row.error_message = (errorMessage as string | null) ?? null;
        }
      }
      return;
    }

    if (q.startsWith("update providers set status = ?,")) {
      const [status, lastVerifiedAt, nextReviewAt, updatedAt, providerId] = bindings;
      const row = this.providers.get(String(providerId));
      if (row) {
        row.status = status;
        row.last_verified_at = lastVerifiedAt;
        row.next_review_at = nextReviewAt;
        row.updated_at = updatedAt;
      }
      return;
    }

    if (q.startsWith("update providers set status = 'stale'")) {
      const [now] = bindings;
      for (const row of this.providers.values()) {
        if (row.next_review_at && String(row.next_review_at) <= String(now)) {
          row.status = "STALE";
        }
      }
      return;
    }

    if (q.startsWith("insert into game_profiles")) {
      const [
        profileCode,
        displayName,
        latencyGoodMax,
        latencyAttentionMax,
        jitterGoodMax,
        jitterAttentionMax,
        lossGoodMax,
        lossAttentionMax,
        downloadGoodMin,
        downloadAttentionMin,
        bufferbloatGoodMax,
        bufferbloatAttentionMax,
        wifiPolicy,
        updatedAt,
      ] = bindings;
      this.gameProfiles.set(String(profileCode), {
        profile_code: String(profileCode),
        display_name: String(displayName),
        latency_good_max: latencyGoodMax == null ? null : Number(latencyGoodMax),
        latency_attention_max: latencyAttentionMax == null ? null : Number(latencyAttentionMax),
        jitter_good_max: jitterGoodMax == null ? null : Number(jitterGoodMax),
        jitter_attention_max: jitterAttentionMax == null ? null : Number(jitterAttentionMax),
        loss_good_max: lossGoodMax == null ? null : Number(lossGoodMax),
        loss_attention_max: lossAttentionMax == null ? null : Number(lossAttentionMax),
        download_good_min: downloadGoodMin == null ? null : Number(downloadGoodMin),
        download_attention_min: downloadAttentionMin == null ? null : Number(downloadAttentionMin),
        bufferbloat_good_max: bufferbloatGoodMax == null ? null : Number(bufferbloatGoodMax),
        bufferbloat_attention_max: bufferbloatAttentionMax == null ? null : Number(bufferbloatAttentionMax),
        wifi_policy: (wifiPolicy as string | null) ?? null,
        updated_at: String(updatedAt),
      });
      return;
    }

    if (q.startsWith("insert into game_catalog")) {
      const [
        gameId,
        displayName,
        slug,
        active,
        profileCode,
        testStrategy,
        regionCode,
        resultLabel,
        providerNetworkMode,
        sortOrder,
        iconKey,
        createdAt,
        updatedAt,
      ] = bindings;
      const existing = this.gameCatalog.get(String(gameId));
      this.gameCatalog.set(String(gameId), {
        game_id: String(gameId),
        display_name: String(displayName),
        slug: String(slug),
        active: Number(active),
        profile_code: String(profileCode),
        test_strategy: String(testStrategy),
        region_code: String(regionCode),
        result_label: String(resultLabel),
        provider_network_mode: String(providerNetworkMode),
        sort_order: Number(sortOrder),
        icon_key: (iconKey as string | null) ?? null,
        created_at: existing?.created_at ?? String(createdAt),
        updated_at: String(updatedAt),
      });
      return;
    }

    if (q.startsWith("delete from game_platforms where game_id = ?")) {
      const [gameId] = bindings;
      this.gamePlatforms = this.gamePlatforms.filter((row) => row.game_id !== String(gameId));
      return;
    }

    if (q.startsWith("insert into game_platforms")) {
      const [gameId, platformCode] = bindings;
      this.gamePlatforms.push({
        game_id: String(gameId),
        platform_code: String(platformCode),
      });
      return;
    }

    if (q.startsWith("insert into game_catalog_audit")) {
      const [id, entityType, entityId, action, actor, beforeJson, afterJson, createdAt] = bindings;
      this.gameAudit.push({
        id: String(id),
        entity_type: String(entityType),
        entity_id: String(entityId),
        action: String(action),
        actor: String(actor),
        before_json: (beforeJson as string | null) ?? null,
        after_json: (afterJson as string | null) ?? null,
        created_at: String(createdAt),
      });
      return;
    }

    if (q.startsWith("update game_catalog set active = ?")) {
      const [active, updatedAt, gameId] = bindings;
      const row = this.gameCatalog.get(String(gameId));
      if (row) {
        row.active = Number(active);
        row.updated_at = String(updatedAt);
      }
    }
  }

  first(sql: string, bindings: unknown[]): Row | null {
    const q = normalize(sql);

    if (q.startsWith("select data_base64, content_type from provider_assets")) {
      const [providerId] = bindings;
      const row = this.providerAssets
        .filter((entry) => entry.provider_id === String(providerId) && entry.asset_type === "LOGO_SQUARE" && entry.data_base64 != null)
        .sort((left, right) => right.version - left.version)[0];
      return row ? { data_base64: row.data_base64, content_type: row.content_type } : null;
    }

    if (q.startsWith("select count, window_start from auth_rate_limit")) {
      const [ip] = bindings;
      return this.authRateLimit.get(String(ip)) ?? null;
    }

    if (q.startsWith("select id, password_hash, role from admin_users where email = ?")) {
      const [email] = bindings;
      const id = this.adminUsersByEmail.get(String(email));
      if (!id) return null;
      const row = this.adminUsers.get(id);
      if (!row || row.active !== 1) return null;
      return { id: row.id, password_hash: row.password_hash, role: row.role };
    }

    if (q.startsWith("select s.user_id, u.role from admin_sessions s join admin_users u")) {
      const [tokenHash, now] = bindings;
      const session = this.adminSessions.get(String(tokenHash));
      if (!session || Number(session.expires_at) <= Number(now)) return null;
      const user = this.adminUsers.get(String(session.user_id));
      if (!user || user.active !== 1) return null;
      return { user_id: session.user_id, role: user.role };
    }

    if (q.startsWith("select email, role from admin_users where id = ?")) {
      const [id] = bindings;
      const row = this.adminUsers.get(String(id));
      return row ? { email: row.email, role: row.role } : null;
    }

    if (q.startsWith("select count(*) as count from admin_users")) {
      return { count: this.adminUsers.size };
    }

    if (q.startsWith("select version, schema_version, engine_version, status, rollout_percent, published_at, updated_at, author, justification, rules_json from diagnostic_rulesets where version = ?")) {
      const [version] = bindings;
      return this.diagnosticRulesets.get(Number(version)) ?? null;
    }

    if (q.startsWith("select rules_json from diagnostic_rulesets where status = 'published'")) {
      const row = [...this.diagnosticRulesets.values()]
        .filter((entry) => entry.status === "PUBLISHED")
        .sort((left, right) => Number(right.version) - Number(left.version))[0];
      return row ? { rules_json: row.rules_json } : null;
    }

    if (q.startsWith("select version from diagnostic_rulesets where version < ? order by version desc limit 1")) {
      const [version] = bindings;
      const previous = [...this.diagnosticRulesets.keys()].filter((key) => key < Number(version)).sort((a, b) => b - a)[0];
      return typeof previous === "number" ? { version: previous } : null;
    }

    if (q.startsWith("select id, display_name, legal_name, cnpj, provider_type, status, official_domain,")) {
      const [providerId] = bindings;
      return this.providers.get(String(providerId)) ?? null;
    }

    if (q.startsWith("select id from providers where id = ?")) {
      const [providerId] = bindings;
      const row = this.providers.get(String(providerId));
      return row ? { id: row.id } : null;
    }

    if (q.startsWith("select provider_id from provider_identifiers where identifier_type = 'asn'")) {
      const [asn] = bindings;
      const row = this.providerIdentifiers.find((entry) =>
        entry.identifier_type === "ASN" && entry.is_active === 1 && entry.original_value === String(asn),
      );
      return row ? { provider_id: row.provider_id } : null;
    }

    if (q.startsWith("select test_count_total, distinct_installations_approx, distinct_days from provider_detection_stats")) {
      const [detectionKey] = bindings;
      const row = this.providerDetectionStats.get(String(detectionKey));
      return row
        ? {
            test_count_total: row.test_count_total,
            distinct_installations_approx: row.distinct_installations_approx,
            distinct_days: row.distinct_days,
          }
        : null;
    }

    if (q.startsWith("select test_count_total, distinct_days from provider_detection_stats")) {
      const [detectionKey] = bindings;
      const row = this.providerDetectionStats.get(String(detectionKey));
      return row ? { test_count_total: row.test_count_total, distinct_days: row.distinct_days } : null;
    }

    if (q.startsWith("select count(*) as count from provider_detection_installations where detection_key = ?")) {
      const [detectionKey] = bindings;
      const prefix = `${String(detectionKey)}::`;
      const count = [...this.providerDetectionInstallations.keys()].filter((key) => key.startsWith(prefix)).length;
      return { count };
    }

    if (q.startsWith("select count(distinct installation_hash) as count from provider_detection_installations where detection_key in")) {
      const keys = new Set(bindings.map((value) => String(value)));
      const count = new Set(
        [...this.providerDetectionInstallations.keys()]
          .filter((key) => keys.has(key.slice(0, key.lastIndexOf("::"))))
          .map((key) => key.slice(key.lastIndexOf("::") + 2)),
      ).size;
      return { count };
    }

    if (q.startsWith("select profile_code, display_name, latency_good_max, latency_attention_max, jitter_good_max,")) {
      const [profileCode] = bindings;
      return profileCode != null ? (this.gameProfiles.get(String(profileCode)) ?? null) : null;
    }

    if (q.startsWith("select game_id, display_name, slug, active, profile_code, test_strategy, region_code,")) {
      const [gameId] = bindings;
      return gameId != null ? (this.gameCatalog.get(String(gameId)) ?? null) : null;
    }

    return null;
  }

  all(sql: string, bindings: unknown[]): Row[] {
    const q = normalize(sql);

    if (q.startsWith("select version, schema_version, engine_version, status, rollout_percent, published_at, updated_at from diagnostic_rulesets")) {
      return [...this.diagnosticRulesets.values()].sort((a, b) => Number(b.version) - Number(a.version));
    }

    if (q.startsWith("select identifier_type, original_value from provider_identifiers")) {
      const [providerId] = bindings;
      return this.providerIdentifiers
        .filter((row) => row.provider_id === String(providerId) && row.is_active === 1)
        .map((row) => ({ identifier_type: row.identifier_type, original_value: row.original_value }));
    }

    if (q.startsWith("select channel_type, value from provider_channels")) {
      const [providerId] = bindings;
      return this.providerChannels
        .filter((row) => row.provider_id === String(providerId) && row.is_active === 1)
        .sort((left, right) => right.priority - left.priority)
        .map((row) => ({ channel_type: row.channel_type, value: row.value }));
    }

    if (q.startsWith("select source_url, r2_key, version, created_at from provider_assets")) {
      const [providerId] = bindings;
      return this.providerAssets
        .filter((row) => row.provider_id === String(providerId) && row.asset_type === "LOGO_SQUARE")
        .sort((left, right) => right.version - left.version)
        .map((row) => ({
          source_url: row.source_url,
          r2_key: row.r2_key,
          version: row.version,
          created_at: row.created_at,
        }));
    }

    if (q.startsWith("select id from providers where lower(display_name) like ?")) {
      const [likeDisplay, likeDomain] = bindings;
      const displayQuery = String(likeDisplay).replace(/%/g, "");
      const domainQuery = String(likeDomain).replace(/%/g, "");
      return [...this.providers.values()]
        .filter((row) =>
          String(row.display_name).toLowerCase().includes(displayQuery)
          || String(row.official_domain ?? "").toLowerCase().includes(domainQuery),
        )
        .sort((left, right) => String(left.display_name).localeCompare(String(right.display_name)))
        .slice(0, 20)
        .map((row) => ({ id: row.id }));
    }

    if (q.startsWith("select distinct provider_id as id from provider_identifiers")) {
      const [like] = bindings;
      const search = String(like).replace(/%/g, "");
      return [...new Set(this.providerIdentifiers
        .filter((row) => row.is_active === 1 && row.normalized_value.includes(search))
        .map((row) => row.provider_id))]
        .map((id) => ({ id }));
    }

    if (q.startsWith("select detection_key, provider_id, asn, normalized_name, last_enrichment_queued_at from provider_detection_stats")) {
      return [...this.providerDetectionStats.values()]
        .filter((row) => row.test_count_total >= 5 && row.distinct_installations_approx >= 3 && row.distinct_days >= 2)
        .map((row) => ({
          detection_key: row.detection_key,
          provider_id: row.provider_id,
          asn: row.asn,
          normalized_name: row.normalized_name,
          last_enrichment_queued_at: row.last_enrichment_queued_at,
        }));
    }

    if (q.startsWith("select detection_key, provider_id, asn, raw_name_sample, normalized_name,")) {
      return [...this.providerDetectionStats.values()].map((row) => ({
        detection_key: row.detection_key,
        provider_id: row.provider_id,
        asn: row.asn,
        raw_name_sample: row.raw_name_sample,
        normalized_name: row.normalized_name,
        test_count_total: row.test_count_total,
        test_count_since_review: row.test_count_since_review,
        distinct_installations_approx: row.distinct_installations_approx,
        distinct_days: row.distinct_days,
        first_seen_at: row.first_seen_at,
        last_seen_at: row.last_seen_at,
        last_enrichment_queued_at: row.last_enrichment_queued_at,
        last_review_at: row.last_review_at,
      }));
    }

    if (q.startsWith("select pds.detection_key,")) {
      return [...this.providerDetectionStats.values()]
        .filter((row) => row.test_count_total >= 5 && row.distinct_installations_approx >= 3 && row.distinct_days >= 2)
        .sort((left, right) => String(right.last_seen_at).localeCompare(String(left.last_seen_at)))
        .map((row) => {
          const provider = row.provider_id ? this.providers.get(row.provider_id) : null;
          const job = [...this.providerEnrichmentJobs.values()].find((entry) => entry.detection_key === row.detection_key);
          return {
            detection_key: row.detection_key,
            provider_id: row.provider_id,
            provider_display_name: provider?.display_name ?? null,
            asn: row.asn,
            normalized_name: row.normalized_name,
            test_count_total: row.test_count_total,
            test_count_since_review: row.test_count_since_review,
            distinct_installations_approx: row.distinct_installations_approx,
            distinct_days: row.distinct_days,
            first_seen_at: row.first_seen_at,
            last_seen_at: row.last_seen_at,
            last_enrichment_queued_at: row.last_enrichment_queued_at,
            status: job?.status ?? "PENDING_REVIEW",
          };
        });
    }

    if (q.startsWith("select id from providers where status in ('stale', 'draft')")) {
      const [now] = bindings;
      return [...this.providers.values()]
        .filter((row) =>
          row.status === "STALE"
          || row.status === "DRAFT"
          || (row.next_review_at && String(row.next_review_at) <= String(now)),
        )
        .map((row) => ({ id: row.id }));
    }

    if (q.startsWith("select platform_code from game_platforms where game_id = ?")) {
      const [gameId] = bindings;
      return this.gamePlatforms
        .filter((row) => row.game_id === String(gameId))
        .sort((left, right) => left.platform_code.localeCompare(right.platform_code))
        .map((row) => ({ platform_code: row.platform_code }));
    }

    if (q.startsWith("select game_id from game_catalog where (? is null or game_id in")) {
      const [platform1] = bindings;
      const platform = platform1 == null ? null : String(platform1);
      return [...this.gameCatalog.values()]
        .filter((row) =>
          platform == null
          || this.gamePlatforms.some((entry) => entry.game_id === row.game_id && entry.platform_code === platform),
        )
        .sort((left, right) => left.sort_order - right.sort_order || left.display_name.localeCompare(right.display_name))
        .map((row) => ({ game_id: row.game_id }));
    }

    if (q.startsWith("select profile_code, display_name, latency_good_max, latency_attention_max, jitter_good_max")) {
      return [...this.gameProfiles.values()].sort((left, right) => left.profile_code.localeCompare(right.profile_code));
    }

    if (q.startsWith("select id, entity_type, entity_id, action, actor, before_json, after_json, created_at from game_catalog_audit")) {
      return [...this.gameAudit.values()].sort((left, right) => right.created_at.localeCompare(left.created_at));
    }

    return [];
  }
}
