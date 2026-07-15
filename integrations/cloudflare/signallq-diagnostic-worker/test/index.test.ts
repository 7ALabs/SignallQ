import assert from "node:assert/strict";
import test from "node:test";
import { hashPassword } from "../src/auth.ts";
import worker from "../src/index.ts";
import { getBundledRuleset } from "../src/bundled-ruleset.ts";
import { validateRuleset } from "../src/diagnostic-engine.ts";
import { FakeD1Database } from "./fake-d1.ts";

function jsonRequest(url: string, body: unknown, method = "POST"): Request {
  return new Request(url, {
    method,
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

test("health exposes worker routes", async () => {
  const response = await worker.fetch(new Request("https://example.com/health"), {});
  const payload = await response.json() as { ok: boolean; routes: string[] };
  assert.equal(response.status, 200);
  assert.equal(payload.ok, true);
  assert.ok(payload.routes.includes("/diagnostic/evaluate"));
});

test("bundled ruleset is valid and contains broad diagnostic coverage", () => {
  const ruleset = getBundledRuleset();
  const validation = validateRuleset(ruleset);
  assert.equal(validation.ok, true);
  assert.ok(ruleset.rules.length >= 20);
});

test("diagnostic evaluate returns deterministic finding for slow 2.4 GHz wifi", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      wifi: { band: "2_4_GHZ", has5GhzAvailable: true },
      speed: { downloadMbps: 22 },
      quality: { latencyMs: 24, jitterMs: 4 },
    }),
    {},
  );

  const payload = await response.json() as {
    wifiResultados: Array<{ id: string; recomendacao: string | null }>;
    decisao: { status: string };
  };

  assert.equal(response.status, 200);
  assert.ok(payload.wifiResultados.some((item) => item.id === "wifi_24ghz_slow_with_5ghz_available"));
  assert.ok(payload.wifiResultados.some((item) => item.recomendacao?.includes("5 GHz")));
  assert.equal(payload.decisao.status, "attention");
});

test("diagnostic evaluate flags critical packet loss", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      speed: { downloadMbps: 100, uploadMbps: 20 },
      quality: { latencyMs: 20, jitterMs: 5, packetLossPercent: 3.2 },
    }),
    {},
  );
  const payload = await response.json() as {
    internetResultados: Array<{ id: string }>;
    decisao: { status: string };
  };
  assert.equal(response.status, 200);
  assert.ok(payload.internetResultados.some((item) => item.id === "packet_loss_critical"));
  assert.equal(payload.decisao.status, "critical");
});

test("diagnostic evaluate flags upload zero as critical", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      speed: { downloadMbps: 120, uploadMbps: 0 },
      quality: { latencyMs: 20, jitterMs: 5, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as {
    internetResultados: Array<{ id: string; recomendacao: string | null }>;
  };
  assert.equal(response.status, 200);
  assert.ok(payload.internetResultados.some((item) => item.id === "upload_zero"));
  assert.ok(payload.internetResultados.some((item) => item.recomendacao?.includes("upload")));
});

test("diagnostic evaluate derives critical bufferbloat from loaded latency", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      speed: { downloadMbps: 300, uploadMbps: 100 },
      quality: { latencyMs: 20, jitterMs: 3, packetLossPercent: 0, loadedLatencyMs: 140 },
    }),
    {},
  );
  const payload = await response.json() as { internetResultados: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.ok(payload.internetResultados.some((item) => item.id === "bufferbloat_critical"));
});

test("diagnostic evaluate flags weak 5GHz wifi using documented thresholds", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      wifi: { band: "5_GHZ", rssiDbm: -78, linkSpeedMbps: 300 },
      speed: { downloadMbps: 200, uploadMbps: 80 },
      quality: { latencyMs: 20, jitterMs: 4, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as { wifiResultados: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.ok(payload.wifiResultados.some((item) => item.id === "wifi_signal_weak_5ghz"));
});

test("diagnostic evaluate flags poor 5G mobile signal", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      mobile: { technology: "5G", rsrpDbm: -112, sinrDb: -1 },
      speed: { downloadMbps: 80, uploadMbps: 20 },
      quality: { latencyMs: 45, jitterMs: 8, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as { mobileResultados: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.ok(payload.mobileResultados.some((item) => item.id === "mobile_signal_poor_5g"));
});

test("diagnostic evaluate flags high DNS latency", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      dns: { latencyMs: 320, currentProvider: "dns.example" },
      speed: { downloadMbps: 90, uploadMbps: 30 },
      quality: { latencyMs: 35, jitterMs: 6, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as { dnsResultados: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.ok(payload.dnsResultados.some((item) => item.id === "dns_latency_high"));
});

test("diagnostic evaluate flags historical degradation with enough data", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      historical: {
        testsCount7d: 7,
        testsCount30d: 15,
        avgDownload7d: 40,
        avgDownload30d: 100,
      },
    }),
    {},
  );
  const payload = await response.json() as { historicoResultados: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.ok(payload.historicoResultados.some((item) => item.id === "derived_history_degradation_critical"));
});

test("diagnostic evaluate flags wifi channel congestion", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      wifi: { band: "2_4_GHZ", rssiDbm: -62 },
      wifiScan: {
        connectedChannel: 1,
        networks: [
          { channel: 1, rssiDbm: -55 },
          { channel: 1, rssiDbm: -58 },
          { channel: 1, rssiDbm: -60 },
          { channel: 2, rssiDbm: -61 },
          { channel: 4, rssiDbm: -63 },
          { channel: 6, rssiDbm: -80 },
        ],
      },
    }),
    {},
  );
  const payload = await response.json() as { wifiCanalResultados: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.ok(payload.wifiCanalResultados.some((item) => item.id === "derived_wifi_channel_congested"));
});

test("diagnostic evaluate detects ISP-side problem from gateway correlation", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      gateway: { rttMs: 5 },
      quality: { latencyMs: 240, jitterMs: 25, packetLossPercent: 0 },
      speed: { downloadMbps: 80, uploadMbps: 20 },
      wifi: { band: "5_GHZ", rssiDbm: -60 },
    }),
    {},
  );
  const payload = await response.json() as {
    decisao: { categoriaOrigem: string | null };
    redeResultados: Array<{ id: string }>;
  };
  assert.equal(response.status, 200);
  assert.equal(payload.decisao.categoriaOrigem, "isp");
  assert.ok(payload.redeResultados.some((item) => item.id === "derived_decisao_gw_01"));
});

test("diagnostic evaluate detects possible wifi interference when wifi is weak and internet has issues", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      wifi: { band: "2_4_GHZ", rssiDbm: -74, has5GhzAvailable: true },
      speed: { downloadMbps: 15, uploadMbps: 3 },
      quality: { latencyMs: 40, jitterMs: 8, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as {
    redeResultados: Array<{ id: string }>;
    decisao: { categoriaOrigem: string | null };
  };
  assert.equal(response.status, 200);
  assert.ok(payload.redeResultados.some((item) => item.id === "derived_decisao_01"));
  assert.equal(payload.decisao.categoriaOrigem, "wifi");
});

test("diagnostic evaluate returns wifi_local flow with partial Wi-Fi input", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      connection: { type: "WIFI" },
      wifi: { band: "5_GHZ", rssiDbm: -79 },
    }),
    {},
  );
  const payload = await response.json() as {
    decisao: { categoriaOrigem: string | null; status: string };
    recomendacoes: string[];
    dadosAusentes: string[];
  };
  assert.equal(response.status, 200);
  assert.equal(payload.decisao.categoriaOrigem, "wifi");
  assert.ok(["attention", "critical"].includes(payload.decisao.status));
  assert.ok(payload.recomendacoes.length >= 2);
  assert.ok(payload.dadosAusentes.includes("download_mbps"));
  assert.ok(payload.dadosAusentes.includes("latency_ms"));
});

test("diagnostic evaluate returns isp_externo flow with partial but decisive gateway evidence", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      gateway: { rttMs: 4 },
      quality: { latencyMs: 260 },
      wifi: { band: "5_GHZ", rssiDbm: -58 },
    }),
    {},
  );
  const payload = await response.json() as {
    decisao: { categoriaOrigem: string | null; mensagemUsuario: string };
    dadosAusentes: string[];
  };
  assert.equal(response.status, 200);
  assert.equal(payload.decisao.categoriaOrigem, "isp");
  assert.match(payload.decisao.mensagemUsuario, /ISP|fora de casa/i);
  assert.ok(payload.dadosAusentes.includes("download_mbps"));
  assert.ok(payload.dadosAusentes.includes("packet_loss_percent"));
});

test("diagnostic evaluate returns sem_dados_suficientes when there is no useful telemetry", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
    }),
    {},
  );
  const payload = await response.json() as {
    decisao: { status: string; podeConcluir: boolean };
    recomendacoes: Array<{ recomendacao: string | null }>;
    aiAssist: { shouldInvoke: boolean; mode: string; userPrompt: string };
  };
  assert.equal(response.status, 200);
  assert.equal(payload.decisao.status, "inconclusive");
  assert.equal(payload.decisao.podeConcluir, false);
  assert.ok(payload.recomendacoes.length >= 2);
  assert.equal(payload.aiAssist.shouldInvoke, true);
  assert.equal(payload.aiAssist.mode, "single_shot_explainer");
  assert.match(payload.aiAssist.userPrompt, /Nao faca perguntas|nao faca perguntas/i);
});

test("diagnostic evaluate returns saudavel_monitorar when core telemetry is healthy", async () => {
  const response = await worker.fetch(
    // GH#955 — fixture ajustada pra cair em faixa "excelente" em todas as
    // dimensoes do ScoreEngine ponderado (latencia<100, jitter<5, bufferbloat
    // delta=loadedLatencyMs-latencyMs<5, RSSI 5GHz>-55, download>=100) — antes
    // a formula linear generica saturava em 100 sem nenhum finding, escondendo
    // a granularidade do motor real.
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      speed: { downloadMbps: 320, uploadMbps: 160 },
      quality: { latencyMs: 18, jitterMs: 3, packetLossPercent: 0, loadedLatencyMs: 20 },
      gateway: { rttMs: 3 },
      wifi: { band: "5_GHZ", rssiDbm: -50, linkSpeedMbps: 866 },
      dns: { latencyMs: 18, currentProvider: "1.1.1.1" },
    }),
    {},
  );
  const payload = await response.json() as {
    decisao: { status: string; titulo: string };
    scoreEngineResultado: { score: number; veredictoHumano: string };
    perfisUso: Array<{ profileId: string }>;
    aiAssist: { shouldInvoke: boolean; reason: string };
  };
  assert.equal(response.status, 200);
  assert.equal(payload.decisao.titulo, "Conexão saudável no momento");
  assert.equal(payload.decisao.status, "ok");
  assert.ok(payload.scoreEngineResultado.score >= 90);
  assert.equal(payload.scoreEngineResultado.veredictoHumano, "excelente");
  assert.ok(payload.perfisUso.some((item) => item.profileId === "jogos"));
  assert.equal(payload.aiAssist.shouldInvoke, false);
  assert.match(payload.aiAssist.reason, /sem necessidade|suficientemente clara/i);
});

test("diagnostic evaluate returns plug-and-play AI prompt package for complex critical case", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      gateway: { rttMs: 5 },
      quality: { latencyMs: 240, jitterMs: 26, packetLossPercent: 3.4, loadedLatencyMs: 160 },
      speed: { downloadMbps: 18, uploadMbps: 2 },
      wifi: { band: "2_4_GHZ", rssiDbm: -74, has5GhzAvailable: true, linkSpeedMbps: 48 },
      historical: {
        testsCount7d: 7,
        testsCount30d: 18,
        avgDownload7d: 30,
        avgDownload30d: 110,
      },
    }),
    {},
  );
  const payload = await response.json() as {
    aiAssist: {
      shouldInvoke: boolean;
      mode: string;
      systemPrompt: string;
      expectedOutputSchema: { format: string; fields: string[] };
    };
    achadosSecundarios: Array<{ id: string }>;
  };
  assert.equal(response.status, 200);
  assert.equal(payload.aiAssist.shouldInvoke, true);
  assert.equal(payload.aiAssist.mode, "single_shot_explainer");
  assert.match(payload.aiAssist.systemPrompt, /Nao faca perguntas/i);
  assert.equal(payload.aiAssist.expectedOutputSchema.format, "json");
  assert.ok(payload.aiAssist.expectedOutputSchema.fields.includes("headline"));
  assert.ok(payload.achadosSecundarios.length >= 1);
});

test("diagnostic simulate validates custom rulesets", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/admin/diagnostic/simulate", {
      snapshot: {
        schemaVersion: 6,
        quality: { latencyMs: 120, jitterMs: 18 },
      },
      ruleset: {
        version: 7,
        schemaVersion: 6,
        engineVersion: 3,
        publishedAt: "2026-07-14T00:00:00.000Z",
        rules: [
          {
            ruleId: "high_latency",
            ruleVersion: 1,
            enabled: true,
            priority: 100,
            minimumSchemaVersion: 6,
            conditions: [
              { field: "quality.latencyMs", operator: "GT", value: 100 },
            ],
            result: {
              findingCode: "HIGH_LATENCY",
              category: "internet",
              severity: "ERROR",
              confidence: "HIGH",
              recommendationId: "RETEST_CLOSE_TO_ROUTER",
            },
          },
        ],
      },
    }),
    {},
  );

  const payload = await response.json() as {
    ok: boolean;
    result: { matchedRules: string[]; overallStatus: string };
  };

  assert.equal(response.status, 200);
  assert.equal(payload.ok, true);
  assert.ok(payload.result.matchedRules.includes("high_latency"));
  assert.equal(payload.result.overallStatus, "CRITICAL");
});

test("provider lookup by ASN returns seeded provider data", async () => {
  const response = await worker.fetch(new Request("https://example.com/providers/by-asn/28126"), {});
  const payload = await response.json() as { id: string; displayName: string };
  assert.equal(response.status, 200);
  assert.equal(payload.id, "brisanet");
  assert.equal(payload.displayName, "Brisanet");
});

test("provider search matches aliases", async () => {
  const response = await worker.fetch(new Request("https://example.com/providers/search?q=virtua"), {});
  const payload = await response.json() as { items: Array<{ id: string }> };
  assert.equal(response.status, 200);
  assert.equal(payload.items[0]?.id, "claro");
});

test("GH#971: PROVIDER_DIRECTORY_SEED_JSON=\"[]\" (config antiga do wrangler.toml) não sobrescreve o catálogo embutido", async () => {
  const response = await worker.fetch(new Request("https://example.com/providers/by-asn/28126"), {
    PROVIDER_DIRECTORY_SEED_JSON: "[]",
  });
  const payload = await response.json() as { id: string; displayName: string };
  assert.equal(response.status, 200);
  assert.equal(payload.id, "brisanet");
  assert.equal(payload.displayName, "Brisanet");
});

test("public games catalog returns seeded active games and filters by platform", async () => {
  const response = await worker.fetch(new Request("https://example.com/games/catalog?platform=PC"), {});
  const payload = await response.json() as { items: Array<{ gameId: string; platforms: string[] }> };
  assert.equal(response.status, 200);
  assert.ok(payload.items.some((item) => item.gameId === "valorant"));
  assert.ok(payload.items.every((item) => item.platforms.includes("PC")));
});

test("public games catalog version is available", async () => {
  const response = await worker.fetch(new Request("https://example.com/games/catalog/version"), {});
  const payload = await response.json() as { version: string; totalGames: number };
  assert.equal(response.status, 200);
  assert.ok(payload.version.startsWith("games-"));
  assert.ok(payload.totalGames >= 1);
});

test("GH#971: GAME_CATALOG_SEED_JSON e GAME_PROFILE_SEED_JSON=\"[]\" (config antiga do wrangler.toml) não sobrescrevem o catálogo embutido de jogos", async () => {
  const response = await worker.fetch(new Request("https://example.com/games/catalog?platform=PC"), {
    GAME_CATALOG_SEED_JSON: "[]",
    GAME_PROFILE_SEED_JSON: "[]",
  });
  const payload = await response.json() as { items: Array<{ gameId: string; platforms: string[] }> };
  assert.equal(response.status, 200);
  assert.ok(payload.items.some((item) => item.gameId === "valorant"));
  assert.ok(payload.items.every((item) => item.platforms.includes("PC")));
});

test("provider detection accepts payload without DB binding", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/ingest/provider-detection", {
      asn: 28126,
      normalizedName: "brisanet",
      detectedAt: "2026-07-14T00:00:00.000Z",
    }),
    {},
  );
  const payload = await response.json() as { ok: boolean; eligibleForEnrichment: boolean };
  assert.equal(response.status, 202);
  assert.equal(payload.ok, true);
  assert.equal(payload.eligibleForEnrichment, false);
});

test("admin bootstrap creates the first admin only once", async () => {
  const db = new FakeD1Database();
  const env = {
    DB: db as unknown as D1Database,
    ADMIN_AUTH_PEPPER: "pepper-test",
    ADMIN_BOOTSTRAP_TOKEN: "bootstrap-secret",
  };

  const bootstrapResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/bootstrap", {
      bootstrapToken: "bootstrap-secret",
      email: "owner@example.com",
      password: "secret123",
    }),
    env,
  );
  assert.equal(bootstrapResponse.status, 201);

  const secondBootstrapResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/bootstrap", {
      bootstrapToken: "bootstrap-secret",
      email: "other@example.com",
      password: "secret123",
    }),
    env,
  );
  assert.equal(secondBootstrapResponse.status, 409);
});

test("admin can sync seeded providers into D1 and public lookup resolves from DB", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  const syncResponse = await worker.fetch(
    new Request("https://example.com/admin/providers/sync-seed", {
      method: "POST",
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const syncPayload = await syncResponse.json() as { ok: boolean; synced: number };
  assert.equal(syncResponse.status, 200);
  assert.equal(syncPayload.ok, true);
  assert.equal(syncPayload.synced >= 2, true);

  const providerResponse = await worker.fetch(
    new Request("https://example.com/providers/by-asn/28126"),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const providerPayload = await providerResponse.json() as { id: string; support: { websiteUrl: string | null } };
  assert.equal(providerResponse.status, 200);
  assert.equal(providerPayload.id, "brisanet");
  assert.equal(providerPayload.support.websiteUrl, "https://www.brisanet.com.br");
});

// GH#956 — antes, distinct_installations_approx era fabricado via MAX(x,3) no
// segundo hit de qualquer detection_key, entao 5 hits do MESMO device (sem
// installationHash real) ja enfileiravam o provedor pra revisao. Este teste
// agora usa 3 installationHash distintos pra provar que a fila so acontece
// com evidencia real de multiplas instalacoes — ver teste seguinte pra provar
// o caso negativo (mesmo installationHash repetido nao infla nada).
test("scheduled review queue is exposed after repeated provider detections", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  await worker.fetch(
    new Request("https://example.com/admin/providers", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Cookie: cookie,
      },
      body: JSON.stringify({
        provider: {
          id: "provedor-x",
          displayName: "Provedor X",
          officialDomain: "provedorx.com.br",
          providerType: "REGIONAL",
          status: "DRAFT",
          aliases: ["provedor x"],
          asns: [65001],
          support: {
            sacPhone: "0800000000",
          },
        },
      }),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );

  for (let index = 0; index < 5; index += 1) {
    await worker.fetch(
      jsonRequest("https://example.com/ingest/provider-detection", {
        providerId: "provedor-x",
        asn: 65001,
        normalizedName: "provedor x",
        installationHash: `install-${index % 3}`, // 3 installs distintos (0,1,2) espalhados nos 5 hits
        detectedAt: `2026-07-${String(10 + index).padStart(2, "0")}T00:00:00.000Z`,
      }),
      { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
    );
  }

  await worker.scheduled({} as ScheduledController, { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper });

  const queueResponse = await worker.fetch(
    new Request("https://example.com/admin/providers/review-queue", {
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const queuePayload = await queueResponse.json() as {
    items: Array<{ providerId: string | null; status: string; distinctInstallationsApprox: number; distinctDays: number }>;
  };
  assert.equal(queueResponse.status, 200);
  assert.equal(queuePayload.items[0]?.providerId, "provedor-x");
  assert.equal(queuePayload.items[0]?.status, "QUEUED");
  assert.equal(queuePayload.items[0]?.distinctInstallationsApprox, 3);
  assert.equal(queuePayload.items[0]?.distinctDays, 5);
});

// GH#956 — caso negativo: o mesmo device (installationHash repetido) batendo
// varias vezes NUNCA pode aprovar sozinho um provedor pro enrichment, mesmo
// cruzando dias diferentes e passando de 5 testes.
test("repeated detections from the same installationHash never reach enrichment eligibility alone", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  let lastDetection: { detectionKey: string; eligibleForEnrichment: boolean } | null = null;
  for (let index = 0; index < 8; index += 1) {
    const response = await worker.fetch(
      jsonRequest("https://example.com/ingest/provider-detection", {
        asn: 70999,
        normalizedName: "single device provider",
        installationHash: "same-install-always",
        detectedAt: `2026-07-${String(10 + index).padStart(2, "0")}T00:00:00.000Z`,
      }),
      { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
    );
    lastDetection = await response.json() as { detectionKey: string; eligibleForEnrichment: boolean };
  }
  assert.equal(lastDetection?.eligibleForEnrichment, false);

  await worker.scheduled({} as ScheduledController, { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper });

  const queueResponse = await worker.fetch(
    new Request("https://example.com/admin/providers/review-queue", {
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const queuePayload = await queueResponse.json() as { items: Array<{ asn: number | null }> };
  assert.equal(queueResponse.status, 200);
  assert.ok(!queuePayload.items.some((item) => item.asn === 70999));
});

test("admin can sync game seeds into D1 and deactivate a game", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  const syncResponse = await worker.fetch(
    new Request("https://example.com/admin/games/sync-seed", {
      method: "POST",
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const syncPayload = await syncResponse.json() as { ok: boolean; syncedGames: number; syncedProfiles: number };
  assert.equal(syncResponse.status, 200);
  assert.equal(syncPayload.ok, true);
  assert.ok(syncPayload.syncedGames >= 1);
  assert.ok(syncPayload.syncedProfiles >= 1);

  const deactivateResponse = await worker.fetch(
    new Request("https://example.com/admin/games/catalog/valorant/deactivate", {
      method: "POST",
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(deactivateResponse.status, 200);

  const listResponse = await worker.fetch(
    new Request("https://example.com/games/catalog?platform=PC"),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const listPayload = await listResponse.json() as { items: Array<{ gameId: string }> };
  assert.equal(listResponse.status, 200);
  assert.equal(listPayload.items.some((item) => item.gameId === "valorant"), false);
});

test("admin can upsert custom game profile and game catalog item", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  const profileResponse = await worker.fetch(
    new Request("https://example.com/admin/games/profiles", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Cookie: cookie,
      },
      body: JSON.stringify({
        profile: {
          profileCode: "CUSTOM_PROFILE",
          displayName: "Custom",
          latencyGoodMax: 45,
          latencyAttentionMax: 90,
          jitterGoodMax: 8,
          jitterAttentionMax: 18,
        },
      }),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(profileResponse.status, 201);

  const gameResponse = await worker.fetch(
    new Request("https://example.com/admin/games/catalog", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Cookie: cookie,
      },
      body: JSON.stringify({
        game: {
          gameId: "rocket-league",
          displayName: "Rocket League",
          slug: "rocket-league",
          active: true,
          profileCode: "CUSTOM_PROFILE",
          testStrategy: "REGIONAL_ESTIMATE",
          regionCode: "SOUTH_AMERICA",
          resultLabel: "Estimativa para Rocket League",
          platforms: ["PC", "PS5", "XBOX"],
          sortOrder: 5,
        },
      }),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(gameResponse.status, 201);

  const fetchResponse = await worker.fetch(
    new Request("https://example.com/games/catalog/rocket-league"),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const fetchPayload = await fetchResponse.json() as { gameId: string; profileCode: string; platforms: string[] };
  assert.equal(fetchResponse.status, 200);
  assert.equal(fetchPayload.gameId, "rocket-league");
  assert.equal(fetchPayload.profileCode, "CUSTOM_PROFILE");
  assert.ok(fetchPayload.platforms.includes("PS5"));

  const auditResponse = await worker.fetch(
    new Request("https://example.com/admin/games/audit", {
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(auditResponse.status, 200);
});

test("admin auth login and me work with D1-backed session", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(loginResponse.status, 200);
  const cookie = loginResponse.headers.get("set-cookie");
  assert.ok(cookie?.includes("session="));

  const meResponse = await worker.fetch(
    new Request("https://example.com/admin/auth/me", {
      headers: { Cookie: cookie ?? "" },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const mePayload = await meResponse.json() as { email: string; role: string };
  assert.equal(meResponse.status, 200);
  assert.equal(mePayload.email, "admin@example.com");
  assert.equal(mePayload.role, "admin");
});

test("admin ruleset draft publish and rollback work with authenticated session", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  const draftRuleset = {
    version: 7,
    schemaVersion: 6,
    engineVersion: 3,
    publishedAt: "2026-07-14T00:00:00.000Z",
    rules: [
      {
        ruleId: "draft_rule",
        ruleVersion: 1,
        enabled: true,
        priority: 50,
        minimumSchemaVersion: 6,
        conditions: [{ field: "quality.latencyMs", operator: "GT", value: 100 }],
        result: {
          findingCode: "DRAFT_RULE",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "RETEST",
        },
      },
    ],
  };

  const createResponse = await worker.fetch(
    new Request("https://example.com/admin/diagnostic/rulesets", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Cookie: cookie,
      },
      body: JSON.stringify({ ruleset: draftRuleset }),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(createResponse.status, 201);

  const publishResponse = await worker.fetch(
    new Request("https://example.com/admin/diagnostic/rulesets/7/publish", {
      method: "POST",
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(publishResponse.status, 200);

  const listResponse = await worker.fetch(
    new Request("https://example.com/admin/diagnostic/rulesets", {
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const listPayload = await listResponse.json() as { items: Array<{ version: number; status: string }> };
  assert.equal(listResponse.status, 200);
  assert.equal(listPayload.items[0]?.status, "PUBLISHED");

  const rollbackResponse = await worker.fetch(
    new Request("https://example.com/admin/diagnostic/rulesets/7/rollback", {
      method: "POST",
      headers: { Cookie: cookie },
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(rollbackResponse.status, 200);
});

// ============================================================================
// GH#955 — fronteiras de threshold (latencia/bufferbloat) alinhadas ao motor
// Android real (MetricClassifier.kt). Ver .claude/skills/regras-diagnostico-rede.
// ============================================================================

async function evaluateLatency(latencyMs: number): Promise<{ ids: string[] }> {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs, jitterMs: 2, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as { internetResultados: Array<{ id: string }> };
  return { ids: payload.internetResultados.map((item) => item.id) };
}

test("latency_high fronteira: 149ms nao dispara, 150ms nao dispara (GT estrito), 151ms dispara", async () => {
  assert.ok(!(await evaluateLatency(149)).ids.includes("latency_high"));
  assert.ok(!(await evaluateLatency(150)).ids.includes("latency_high"));
  assert.ok((await evaluateLatency(151)).ids.includes("latency_high"));
});

async function evaluateBufferbloat(loadedLatencyMs: number): Promise<{ ids: string[] }> {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 1, jitterMs: 2, packetLossPercent: 0, loadedLatencyMs },
    }),
    {},
  );
  const payload = await response.json() as { internetResultados: Array<{ id: string }> };
  return { ids: payload.internetResultados.map((item) => item.id) };
}

test("bufferbloat_elevated fronteira: 29ms nao dispara, 30ms nao dispara, 31ms dispara", async () => {
  assert.ok(!(await evaluateBufferbloat(29)).ids.includes("bufferbloat_elevated"));
  assert.ok(!(await evaluateBufferbloat(30)).ids.includes("bufferbloat_elevated"));
  assert.ok((await evaluateBufferbloat(31)).ids.includes("bufferbloat_elevated"));
});

test("bufferbloat_critical fronteira: 99ms nao dispara critical (so elevated), 100ms nao dispara critical, 101ms dispara critical", async () => {
  const at99 = await evaluateBufferbloat(99);
  assert.ok(!at99.ids.includes("bufferbloat_critical"));
  assert.ok(at99.ids.includes("bufferbloat_elevated"));

  const at100 = await evaluateBufferbloat(100);
  assert.ok(!at100.ids.includes("bufferbloat_critical"));

  const at101 = await evaluateBufferbloat(101);
  assert.ok(at101.ids.includes("bufferbloat_critical"));
});

// ============================================================================
// Fronteiras ja documentadas na skill /regras-diagnostico-rede — jitter,
// perda de pacotes e latencia DNS (thresholds do bundled-ruleset ja batiam
// com a skill, cobertura de regressao pra nao desviar no futuro).
// ============================================================================

test("jitter_elevated fronteira: 20ms nao dispara, 21ms dispara", async () => {
  const response20 = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 10, jitterMs: 20, packetLossPercent: 0 },
    }),
    {},
  );
  const payload20 = await response20.json() as { internetResultados: Array<{ id: string }> };
  assert.ok(!payload20.internetResultados.some((item) => item.id === "jitter_elevated"));

  const response21 = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 10, jitterMs: 21, packetLossPercent: 0 },
    }),
    {},
  );
  const payload21 = await response21.json() as { internetResultados: Array<{ id: string }> };
  assert.ok(payload21.internetResultados.some((item) => item.id === "jitter_elevated"));
});

test("packet loss fronteiras: 0.99% nenhum finding, 1% moderate, 3% critical (substitui moderate)", async () => {
  const below = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 10, jitterMs: 2, packetLossPercent: 0.99 },
    }),
    {},
  );
  const belowPayload = await below.json() as { internetResultados: Array<{ id: string }> };
  assert.ok(!belowPayload.internetResultados.some((item) => item.id.startsWith("packet_loss")));

  const moderate = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 10, jitterMs: 2, packetLossPercent: 1 },
    }),
    {},
  );
  const moderatePayload = await moderate.json() as { internetResultados: Array<{ id: string }> };
  assert.ok(moderatePayload.internetResultados.some((item) => item.id === "packet_loss_moderate"));

  const critical = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 10, jitterMs: 2, packetLossPercent: 3 },
    }),
    {},
  );
  const criticalPayload = await critical.json() as { internetResultados: Array<{ id: string }> };
  assert.ok(criticalPayload.internetResultados.some((item) => item.id === "packet_loss_critical"));
});

test("DNS latency fronteiras: 150ms nenhum finding, 151ms elevated, 300ms elevated (nao high), 301ms high", async () => {
  const cases: Array<[number, string | null]> = [
    [150, null],
    [151, "dns_latency_elevated"],
    [300, "dns_latency_elevated"],
    [301, "dns_latency_high"],
  ];
  for (const [latencyMs, expectedId] of cases) {
    const response = await worker.fetch(
      jsonRequest("https://example.com/diagnostic/evaluate", {
        schemaVersion: 6,
        dns: { latencyMs },
        quality: { latencyMs: 10, jitterMs: 2, packetLossPercent: 0 },
      }),
      {},
    );
    const payload = await response.json() as { dnsResultados: Array<{ id: string }> };
    if (expectedId) {
      assert.ok(payload.dnsResultados.some((item) => item.id === expectedId), `esperava ${expectedId} em ${latencyMs}ms`);
    } else {
      assert.equal(payload.dnsResultados.length, 0, `esperava nenhum finding DNS em ${latencyMs}ms`);
    }
  }
});

// ============================================================================
// GH#955 — score ponderado por tipo de conexao (wifi/fibra/movel/desconhecido)
// ============================================================================

test("score engine pondera diferente por tipo de conexao com o mesmo problema de estabilidade", async () => {
  const baseQuality = { latencyMs: 220, jitterMs: 30, packetLossPercent: 0 }; // estabilidade ruim em qualquer tipo

  const wifiResponse = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: baseQuality,
      wifi: { band: "5_GHZ", rssiDbm: -50 }, // wifiRedeLocal excelente
    }),
    {},
  );
  const wifiPayload = await wifiResponse.json() as { scoreEngineResultado: { score: number } };

  const fibraResponse = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: baseQuality,
      fiber: { rxPowerDbm: -18 }, // fibra excelente
    }),
    {},
  );
  const fibraPayload = await fibraResponse.json() as { scoreEngineResultado: { score: number } };

  const movelResponse = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: baseQuality,
      mobile: { technology: "5G", rsrpDbm: -70, sinrDb: 25 }, // sinal movel excelente
    }),
    {},
  );
  const movelPayload = await movelResponse.json() as { scoreEngineResultado: { score: number } };

  const desconhecidoResponse = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: baseQuality,
    }),
    {},
  );
  const desconhecidoPayload = await desconhecidoResponse.json() as { scoreEngineResultado: { score: number } };

  // Wi-Fi pesa estabilidade 35% (vs Fibra 30%, Movel 25%) — com estabilidade
  // ruim e a dimensao propria (wifiRedeLocal/fibra/sinalMovel) excelente, o
  // score de Wi-Fi deve ficar MENOR que os demais (estabilidade pesa mais).
  assert.ok(wifiPayload.scoreEngineResultado.score < fibraPayload.scoreEngineResultado.score);
  assert.ok(wifiPayload.scoreEngineResultado.score < movelPayload.scoreEngineResultado.score);
  // Desconhecido tem so estabilidade(40%)+velocidade(40%)+dns(15%)+historico(5%),
  // sem dimensao de "rede local" nenhuma — com so quality preenchido, cai tudo
  // no peso de estabilidade normalizado a 100% (unica dimensao disponivel).
  assert.ok(typeof desconhecidoPayload.scoreEngineResultado.score === "number");
});

// ============================================================================
// GH#954 — evaluationSource reflete a origem real do ruleset
// ============================================================================

test("evaluationSource e BUNDLED_LOCAL sem D1 e sem DIAGNOSTIC_RULESET_JSON", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 20, jitterMs: 3, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as { evaluationSource: string };
  assert.equal(payload.evaluationSource, "BUNDLED_LOCAL");
});

test("evaluationSource e REMOTE quando DIAGNOSTIC_RULESET_JSON valido esta configurado", async () => {
  const remoteRuleset = JSON.stringify(getBundledRuleset());
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 20, jitterMs: 3, packetLossPercent: 0 },
    }),
    { DIAGNOSTIC_RULESET_JSON: remoteRuleset },
  );
  const payload = await response.json() as { evaluationSource: string };
  assert.equal(payload.evaluationSource, "REMOTE");
});

test("evaluationSource e REMOTE quando ha ruleset PUBLISHED no D1", async () => {
  const db = new FakeD1Database();
  const bundled = getBundledRuleset();
  db.diagnosticRulesets.set(bundled.version, {
    version: bundled.version,
    schema_version: bundled.schemaVersion,
    engine_version: bundled.engineVersion,
    status: "PUBLISHED",
    rollout_percent: 100,
    published_at: bundled.publishedAt,
    created_at: bundled.publishedAt,
    updated_at: bundled.publishedAt,
    author: "seed",
    justification: "",
    rules_json: JSON.stringify(bundled),
  });
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 20, jitterMs: 3, packetLossPercent: 0 },
    }),
    { DB: db as unknown as D1Database },
  );
  const payload = await response.json() as { evaluationSource: string };
  assert.equal(payload.evaluationSource, "REMOTE");
});

// ============================================================================
// GH#959 — fallback minimo garantido em qualquer falha total do motor
// ============================================================================

test("diagnostic evaluate nunca retorna 500 cru: D1 indisponivel cai pra payload inconclusivo valido", async () => {
  const brokenDb = {
    prepare: () => {
      throw new Error("D1 indisponivel (simulado)");
    },
  } as unknown as D1Database;

  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 20, jitterMs: 3, packetLossPercent: 0 },
    }),
    { DB: brokenDb },
  );
  const payload = await response.json() as {
    decisao: { status: string; podeConcluir: boolean; mensagemUsuario: string };
    scoreEngineResultado: { score: number; veredictoHumano: string };
    evaluationSource: string;
  };
  assert.equal(response.status, 200);
  assert.equal(payload.decisao.status, "inconclusive");
  assert.equal(payload.decisao.podeConcluir, false);
  assert.match(payload.decisao.mensagemUsuario, /Nao conseguimos concluir|D1 indisponivel/i);
  assert.equal(payload.scoreEngineResultado.veredictoHumano, "fraco");
});

// ============================================================================
// GH#957 — game readiness usa o catalogo real (4 perfis de game-catalog.ts),
// nunca fps/moba/casual inventados.
// ============================================================================

test("gameReadiness expoe os 4 perfis reais do catalogo, nunca fps/moba/casual", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/diagnostic/evaluate", {
      schemaVersion: 6,
      quality: { latencyMs: 20, jitterMs: 3, packetLossPercent: 0 },
    }),
    {},
  );
  const payload = await response.json() as { gameReadiness: Array<{ profileCode: string; status: string }> };
  const codes = payload.gameReadiness.map((item) => item.profileCode).sort();
  assert.deepEqual(codes, ["COMPETITIVE", "COMPETITIVE_EXTREME", "MULTIPLAYER_MODERATE", "SPORTS_COMPETITIVE"]);
  assert.ok(!codes.includes("fps") && !codes.includes("moba") && !codes.includes("casual"));
  assert.ok(payload.gameReadiness.every((item) => ["bom", "atencao", "ruim", "sem_dados"].includes(item.status)));
});

test("gameReadiness COMPETITIVE_EXTREME fronteiras de latencia (good=30 attention=80)", async () => {
  async function statusFor(latencyMs: number): Promise<string> {
    const response = await worker.fetch(
      jsonRequest("https://example.com/diagnostic/evaluate", {
        schemaVersion: 6,
        quality: { latencyMs, jitterMs: 1, packetLossPercent: 0 },
      }),
      {},
    );
    const payload = await response.json() as { gameReadiness: Array<{ profileCode: string; status: string }> };
    return payload.gameReadiness.find((item) => item.profileCode === "COMPETITIVE_EXTREME")!.status;
  }

  assert.equal(await statusFor(30), "bom");
  assert.equal(await statusFor(31), "atencao");
  assert.equal(await statusFor(80), "atencao");
  assert.equal(await statusFor(81), "ruim");
});

test("gameReadiness MULTIPLAYER_MODERADO (perfil mais tolerante) fronteiras de latencia (good=60 attention=150)", async () => {
  async function statusFor(latencyMs: number): Promise<string> {
    const response = await worker.fetch(
      jsonRequest("https://example.com/diagnostic/evaluate", {
        schemaVersion: 6,
        quality: { latencyMs, jitterMs: 1, packetLossPercent: 0 },
      }),
      {},
    );
    const payload = await response.json() as { gameReadiness: Array<{ profileCode: string; status: string }> };
    return payload.gameReadiness.find((item) => item.profileCode === "MULTIPLAYER_MODERATE")!.status;
  }

  assert.equal(await statusFor(60), "bom");
  assert.equal(await statusFor(61), "atencao");
  assert.equal(await statusFor(150), "atencao");
  assert.equal(await statusFor(151), "ruim");
});

// ============================================================================
// GH#961 — teste negativo de auth (senha errada, sem cookie, sessao expirada)
// ============================================================================

test("admin auth: senha errada retorna 401 e nao cria sessao", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const response = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "senha-errada",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(response.status, 401);
  assert.equal(response.headers.get("set-cookie"), null);
});

test("admin auth: rota /admin/* sem cookie retorna 401", async () => {
  const db = new FakeD1Database();
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/review-queue"),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" },
  );
  assert.equal(response.status, 401);
});

test("admin auth: sessao expirada retorna 401", async () => {
  const db = new FakeD1Database();
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  const cookie = loginResponse.headers.get("set-cookie") ?? "";

  // Forca a sessao a expirar manualmente (sem esperar 7 dias de verdade).
  for (const session of db.adminSessions.values()) {
    (session as { expires_at: number }).expires_at = Math.floor(Date.now() / 1000) - 10;
  }

  const meResponse = await worker.fetch(
    new Request("https://example.com/admin/auth/me", { headers: { Cookie: cookie } }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  assert.equal(meResponse.status, 401);
});

// ============================================================================
// GH#961 — /ingest/provider-detection valida shape antes de tocar D1
// ============================================================================

test("provider detection rejeita payload null com 400 tratado (nao 500 cru)", async () => {
  const response = await worker.fetch(
    new Request("https://example.com/ingest/provider-detection", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "null",
    }),
    {},
  );
  assert.equal(response.status, 400);
  const payload = await response.json() as { error: string };
  assert.match(payload.error, /Invalid provider detection payload/i);
});

test("provider detection rejeita asn com tipo errado (string em vez de number)", async () => {
  const response = await worker.fetch(
    jsonRequest("https://example.com/ingest/provider-detection", {
      asn: "not-a-number",
      normalizedName: "provedor",
    }),
    {},
  );
  assert.equal(response.status, 400);
});

test("provider detection rejeita body nao-JSON (string crua) com 400", async () => {
  const response = await worker.fetch(
    new Request("https://example.com/ingest/provider-detection", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "isso nao e json valido {{{",
    }),
    {},
  );
  assert.equal(response.status, 400);
});

// ============================================================================
// GH#960 — CORS
// ============================================================================

test("CORS: preflight OPTIONS responde 204 com Access-Control-Allow-Origin", async () => {
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/review-queue", { method: "OPTIONS" }),
    { ALLOWED_ORIGIN: "https://signallq-admin-panel.pages.dev" },
  );
  assert.equal(response.status, 204);
  assert.equal(response.headers.get("Access-Control-Allow-Origin"), "https://signallq-admin-panel.pages.dev");
});

test("CORS: respostas normais tambem carregam Access-Control-Allow-Origin configurado", async () => {
  const response = await worker.fetch(
    new Request("https://example.com/health"),
    { ALLOWED_ORIGIN: "https://signallq-admin-panel.pages.dev" },
  );
  assert.equal(response.headers.get("Access-Control-Allow-Origin"), "https://signallq-admin-panel.pages.dev");
});

// ============================================================================
// GH#965 — diretorio remoto de operadora de cauda longa: logo + contato
// ============================================================================

async function loginAsAdmin(db: FakeD1Database): Promise<string> {
  const pepper = "pepper-test";
  const passwordHash = await hashPassword("secret123", pepper);
  db.adminUsers.set("user-1", {
    id: "user-1",
    email: "admin@example.com",
    password_hash: passwordHash,
    role: "admin",
    active: 1,
    created_at: 1,
    last_login: null,
  });
  db.adminUsersByEmail.set("admin@example.com", "user-1");

  const loginResponse = await worker.fetch(
    jsonRequest("https://example.com/admin/auth/login", {
      email: "admin@example.com",
      password: "secret123",
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: pepper },
  );
  return loginResponse.headers.get("set-cookie") ?? "";
}

test("provider support: edicao parcial atualiza so os campos enviados, sem apagar o resto", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const env = { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" };

  // seed inicial via upsert completo
  await worker.fetch(
    new Request("https://example.com/admin/providers", {
      method: "POST",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({
        provider: {
          id: "regional_teste",
          displayName: "Regional Teste",
          support: { sacPhone: "10000", websiteUrl: "https://old.example.com" },
        },
      }),
    }),
    env,
  );

  // edicao parcial: so troca o site, sacPhone deve continuar intocado
  const editResponse = await worker.fetch(
    new Request("https://example.com/admin/providers/regional_teste/support", {
      method: "PUT",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({ websiteUrl: "https://novo.example.com" }),
    }),
    env,
  );
  assert.equal(editResponse.status, 200);

  const providerResponse = await worker.fetch(
    new Request("https://example.com/providers/regional_teste"),
    env,
  );
  const payload = await providerResponse.json() as { support: { sacPhone: string | null; websiteUrl: string | null } };
  assert.equal(payload.support.sacPhone, "10000");
  assert.equal(payload.support.websiteUrl, "https://novo.example.com");
});

test("provider support: valor null explicito remove o canal (nao so ignora)", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const env = { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" };

  await worker.fetch(
    new Request("https://example.com/admin/providers", {
      method: "POST",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({
        provider: { id: "regional_teste2", displayName: "Regional Teste 2", support: { whatsappUrl: "https://wa.me/5511999999999" } },
      }),
    }),
    env,
  );

  const editResponse = await worker.fetch(
    new Request("https://example.com/admin/providers/regional_teste2/support", {
      method: "PUT",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({ whatsappUrl: null }),
    }),
    env,
  );
  assert.equal(editResponse.status, 200);

  const providerResponse = await worker.fetch(new Request("https://example.com/providers/regional_teste2"), env);
  const payload = await providerResponse.json() as { support: { whatsappUrl: string | null } };
  assert.equal(payload.support.whatsappUrl, null);
});

test("provider support: provedor inexistente retorna 404 (nao 500 cru)", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/nao_existe/support", {
      method: "PUT",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({ sacPhone: "123" }),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" },
  );
  assert.equal(response.status, 404);
});

test("provider support: sem cookie de sessao retorna 401", async () => {
  const db = new FakeD1Database();
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/qualquer/support", {
      method: "PUT",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ sacPhone: "123" }),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" },
  );
  assert.equal(response.status, 401);
});

test("provider logo upload: grava BLOB base64 no D1 (sem R2) e ProviderLogo.url resolve pra rota propria do worker", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const env = {
    DB: db as unknown as D1Database,
    ADMIN_AUTH_PEPPER: "pepper-test",
    PROVIDER_LOGO_PUBLIC_BASE_URL: "https://signallq-diagnostic.example.workers.dev",
  };

  await worker.fetch(
    new Request("https://example.com/admin/providers", {
      method: "POST",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({ provider: { id: "regional_teste4", displayName: "Regional Teste 4" } }),
    }),
    env,
  );

  const uploadResponse = await worker.fetch(
    new Request("https://example.com/admin/providers/regional_teste4/logo", {
      method: "POST",
      headers: { "content-type": "image/webp", Cookie: cookie },
      body: new Uint8Array([1, 2, 3, 4]),
    }),
    env,
  );
  assert.equal(uploadResponse.status, 201);
  const uploadPayload = await uploadResponse.json() as { ok: boolean; url: string; version: number };
  assert.equal(uploadPayload.ok, true);
  assert.equal(uploadPayload.version, 1);
  assert.equal(uploadPayload.url, "https://signallq-diagnostic.example.workers.dev/providers/regional_teste4/logo");

  const providerResponse = await worker.fetch(new Request("https://example.com/providers/regional_teste4"), env);
  const providerPayload = await providerResponse.json() as { logo: { url: string; version: number } | null };
  assert.equal(providerPayload.logo?.url, "https://signallq-diagnostic.example.workers.dev/providers/regional_teste4/logo");
  assert.equal(providerPayload.logo?.version, 1);
});

test("GET /providers/:id/logo serve o binario gravado no D1 com o content-type real do upload", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const env = { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" };

  await worker.fetch(
    new Request("https://example.com/admin/providers", {
      method: "POST",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({ provider: { id: "regional_teste5", displayName: "Regional Teste 5" } }),
    }),
    env,
  );
  const originalBytes = new Uint8Array([137, 80, 78, 71, 1, 2, 3, 4, 5]);
  await worker.fetch(
    new Request("https://example.com/admin/providers/regional_teste5/logo", {
      method: "POST",
      headers: { "content-type": "image/png", Cookie: cookie },
      body: originalBytes,
    }),
    env,
  );

  const logoResponse = await worker.fetch(new Request("https://example.com/providers/regional_teste5/logo"), env);
  assert.equal(logoResponse.status, 200);
  assert.equal(logoResponse.headers.get("content-type"), "image/png");
  const returnedBytes = new Uint8Array(await logoResponse.arrayBuffer());
  assert.deepEqual([...returnedBytes], [...originalBytes]);
});

test("GET /providers/:id/logo de provedor sem logo com blob retorna 404 (nunca 500 cru)", async () => {
  const db = new FakeD1Database();
  const env = { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" };
  const response = await worker.fetch(new Request("https://example.com/providers/regional_sem_logo/logo"), env);
  assert.equal(response.status, 404);
});

test("provider logo upload: acima de 500KB retorna 413 tratado (nunca 500 cru, nunca finge sucesso)", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const env = { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" };
  await worker.fetch(
    new Request("https://example.com/admin/providers", {
      method: "POST",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: JSON.stringify({ provider: { id: "regional_teste6", displayName: "Regional Teste 6" } }),
    }),
    env,
  );

  const oversized = new Uint8Array(501 * 1024);
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/regional_teste6/logo", {
      method: "POST",
      headers: { "content-type": "image/webp", Cookie: cookie },
      body: oversized,
    }),
    env,
  );
  assert.equal(response.status, 413);
  const payload = await response.json() as { error: string };
  assert.ok(payload.error.includes("500KB"));
});

test("provider logo upload: provedor inexistente retorna 404", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/nao_existe/logo", {
      method: "POST",
      headers: { "content-type": "image/webp", Cookie: cookie },
      body: new Uint8Array([1, 2, 3, 4]),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" },
  );
  assert.equal(response.status, 404);
});

test("provider logo upload: Content-Type que nao e image/* retorna 400", async () => {
  const db = new FakeD1Database();
  const cookie = await loginAsAdmin(db);
  const response = await worker.fetch(
    new Request("https://example.com/admin/providers/qualquer/logo", {
      method: "POST",
      headers: { "content-type": "application/json", Cookie: cookie },
      body: new Uint8Array([1, 2, 3, 4]),
    }),
    { DB: db as unknown as D1Database, ADMIN_AUTH_PEPPER: "pepper-test" },
  );
  assert.equal(response.status, 400);
});
