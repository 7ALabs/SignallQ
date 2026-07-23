const PASSWORD_ITERATIONS = 120_000;
const PASSWORD_KEY_LENGTH = 32;
const SESSION_TTL_DIAS = 7;
const COOKIE_NAME = 'sq_admin_session';
const INVITE_TTL_HORAS = 72;
const RESET_TTL_HORAS = 2;

export type AdminAuthEnv = {
  DB?: D1Database;
  ADMIN_SUPPORT_SECRET?: string;
};

export type AdminSession = {
  tenantId: string;
  email: string;
  expiraEm: string;
};

type UsuarioIspRow = {
  id: number;
  tenant_id: string;
  email: string;
  senha_hash: string;
  criado_em: string;
};

type AdminAccessTokenRow = {
  id: number;
  tenant_id: string;
  email: string;
  purpose: 'invite' | 'reset';
  expira_em: string;
  consumido_em: string | null;
};

export type UsuarioAdminResumo = {
  id: number;
  email: string;
  criadoEm: string;
};

function bytesToBase64(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes));
}

function base64ToBytes(value: string): Uint8Array {
  return Uint8Array.from(atob(value), (char) => char.charCodeAt(0));
}

function hex(bytes: Uint8Array): string {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

function randomBytes(size: number): Uint8Array {
  const bytes = new Uint8Array(size);
  crypto.getRandomValues(bytes);
  return bytes;
}

async function sha256Hex(value: string): Promise<string> {
  const encoded = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest('SHA-256', encoded);
  return hex(new Uint8Array(digest));
}

function expiryIsoFromHours(hours: number): string {
  return new Date(Date.now() + hours * 60 * 60 * 1000).toISOString();
}

async function pbkdf2(password: string, salt: Uint8Array, iterations: number): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt, iterations, hash: 'SHA-256' },
    key,
    PASSWORD_KEY_LENGTH * 8,
  );
  return new Uint8Array(bits);
}

export async function hashSenha(password: string): Promise<string> {
  const salt = randomBytes(16);
  const derived = await pbkdf2(password, salt, PASSWORD_ITERATIONS);
  return `pbkdf2$${PASSWORD_ITERATIONS}$${bytesToBase64(salt)}$${bytesToBase64(derived)}`;
}

export async function verificarSenha(password: string, senhaHash: string): Promise<boolean> {
  const [algoritmo, iterationsRaw, saltB64, hashB64] = senhaHash.split('$');
  if (algoritmo !== 'pbkdf2' || !iterationsRaw || !saltB64 || !hashB64) return false;

  const iterations = Number(iterationsRaw);
  if (!Number.isFinite(iterations) || iterations <= 0) return false;

  const salt = base64ToBytes(saltB64);
  const esperado = base64ToBytes(hashB64);
  const atual = await pbkdf2(password, salt, iterations);
  if (atual.length !== esperado.length) return false;

  let diff = 0;
  for (let i = 0; i < atual.length; i += 1) diff |= atual[i]! ^ esperado[i]!;
  return diff === 0;
}

export async function contarUsuariosTenant(db: D1Database, tenantId: string): Promise<number> {
  const row = await db
    .prepare('SELECT COUNT(*) AS total FROM usuarios_isp WHERE tenant_id = ?1')
    .bind(tenantId)
    .first<{ total: number }>();
  return row?.total ?? 0;
}

export async function criarUsuarioIsp(
  db: D1Database,
  tenantId: string,
  email: string,
  password: string,
): Promise<void> {
  const senhaHash = await hashSenha(password);
  await db
    .prepare('INSERT INTO usuarios_isp (tenant_id, email, senha_hash) VALUES (?1, ?2, ?3)')
    .bind(tenantId, email.trim().toLowerCase(), senhaHash)
    .run();
}

export async function atualizarSenhaUsuarioIsp(
  db: D1Database,
  tenantId: string,
  email: string,
  password: string,
): Promise<boolean> {
  const usuario = await buscarUsuarioIsp(db, tenantId, email);
  if (!usuario) return false;

  const senhaHash = await hashSenha(password);
  await db
    .prepare('UPDATE usuarios_isp SET senha_hash = ?1 WHERE tenant_id = ?2 AND email = ?3')
    .bind(senhaHash, tenantId, email.trim().toLowerCase())
    .run();
  return true;
}

async function buscarUsuarioIsp(db: D1Database, tenantId: string, email: string): Promise<UsuarioIspRow | null> {
  return db
    .prepare('SELECT id, tenant_id, email, senha_hash, criado_em FROM usuarios_isp WHERE tenant_id = ?1 AND email = ?2 LIMIT 1')
    .bind(tenantId, email.trim().toLowerCase())
    .first<UsuarioIspRow>();
}

export async function listarUsuariosTenant(db: D1Database, tenantId: string): Promise<UsuarioAdminResumo[]> {
  const rows = await db
    .prepare('SELECT id, email, criado_em FROM usuarios_isp WHERE tenant_id = ?1 ORDER BY email ASC')
    .bind(tenantId)
    .all<{ id: number; email: string; criado_em: string }>();

  return (rows.results ?? []).map((row) => ({
    id: row.id,
    email: row.email,
    criadoEm: row.criado_em,
  }));
}

function sessionExpiryIso(): string {
  return new Date(Date.now() + SESSION_TTL_DIAS * 24 * 60 * 60 * 1000).toISOString();
}

function buildCookie(token: string): string {
  return `${COOKIE_NAME}=${token}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${SESSION_TTL_DIAS * 24 * 60 * 60}; Secure`;
}

export function clearSessionCookie(): string {
  return `${COOKIE_NAME}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0; Secure`;
}

function parseCookie(cookieHeader: string | null, name: string): string | null {
  if (!cookieHeader) return null;
  const entries = cookieHeader.split(';');
  for (const entry of entries) {
    const [rawName, ...rest] = entry.trim().split('=');
    if (rawName === name) return rest.join('=');
  }
  return null;
}

export async function criarSessaoAdmin(
  db: D1Database,
  tenantId: string,
  email: string,
): Promise<{ token: string; cookie: string; expiraEm: string }> {
  const token = hex(randomBytes(24));
  const expiraEm = sessionExpiryIso();
  const tokenHash = await sha256Hex(token);

  await db
    .prepare('INSERT INTO admin_sessions (tenant_id, email, token_hash, expira_em) VALUES (?1, ?2, ?3, ?4)')
    .bind(tenantId, email.trim().toLowerCase(), tokenHash, expiraEm)
    .run();

  return { token, cookie: buildCookie(token), expiraEm };
}

async function buscarSessaoPorToken(db: D1Database, token: string): Promise<AdminSession | null> {
  const tokenHash = await sha256Hex(token);
  const row = await db
    .prepare(
      `SELECT tenant_id, email, expira_em
       FROM admin_sessions
       WHERE token_hash = ?1 AND expira_em >= datetime('now')
       LIMIT 1`,
    )
    .bind(tokenHash)
    .first<{ tenant_id: string; email: string; expira_em: string }>();

  if (!row) return null;
  return { tenantId: row.tenant_id, email: row.email, expiraEm: row.expira_em };
}

export async function resolverSessaoAdmin(request: Request, env: AdminAuthEnv): Promise<AdminSession | null> {
  if (!env.DB) return null;
  const token = parseCookie(request.headers.get('Cookie'), COOKIE_NAME);
  if (!token) return null;
  return buscarSessaoPorToken(env.DB, token);
}

export async function logoutSessaoAdmin(request: Request, db: D1Database): Promise<void> {
  const token = parseCookie(request.headers.get('Cookie'), COOKIE_NAME);
  if (!token) return;
  const tokenHash = await sha256Hex(token);
  await db.prepare('DELETE FROM admin_sessions WHERE token_hash = ?1').bind(tokenHash).run();
}

export async function autenticarUsuarioIsp(
  db: D1Database,
  tenantId: string,
  email: string,
  password: string,
): Promise<UsuarioIspRow | null> {
  const usuario = await buscarUsuarioIsp(db, tenantId, email);
  if (!usuario) return null;
  return (await verificarSenha(password, usuario.senha_hash)) ? usuario : null;
}

export async function criarTokenAcessoAdmin(
  db: D1Database,
  tenantId: string,
  email: string,
  purpose: 'invite' | 'reset',
): Promise<{ token: string; expiraEm: string }> {
  const token = hex(randomBytes(24));
  const tokenHash = await sha256Hex(token);
  const expiraEm = expiryIsoFromHours(purpose === 'invite' ? INVITE_TTL_HORAS : RESET_TTL_HORAS);

  await db
    .prepare(
      `INSERT INTO admin_access_tokens (tenant_id, email, purpose, token_hash, expira_em)
       VALUES (?1, ?2, ?3, ?4, ?5)`,
    )
    .bind(tenantId, email.trim().toLowerCase(), purpose, tokenHash, expiraEm)
    .run();

  return { token, expiraEm };
}

async function buscarTokenAcesso(
  db: D1Database,
  token: string,
  purpose: 'invite' | 'reset',
): Promise<AdminAccessTokenRow | null> {
  const tokenHash = await sha256Hex(token);
  return db
    .prepare(
      `SELECT id, tenant_id, email, purpose, expira_em, consumido_em
       FROM admin_access_tokens
       WHERE token_hash = ?1 AND purpose = ?2 AND consumido_em IS NULL AND expira_em >= datetime('now')
       LIMIT 1`,
    )
    .bind(tokenHash, purpose)
    .first<AdminAccessTokenRow>();
}

export async function consumirTokenAcessoAdmin(
  db: D1Database,
  token: string,
  purpose: 'invite' | 'reset',
): Promise<{ tenantId: string; email: string } | null> {
  const row = await buscarTokenAcesso(db, token, purpose);
  if (!row) return null;

  await db
    .prepare('UPDATE admin_access_tokens SET consumido_em = datetime(\'now\') WHERE id = ?1')
    .bind(row.id)
    .run();

  return { tenantId: row.tenant_id, email: row.email };
}

export async function autenticarSuporte7Agents(
  request: Request,
  env: AdminAuthEnv,
  tenantId: string,
  suporteEmail: string,
): Promise<{ cookie: string; expiraEm: string } | null> {
  if (!env.DB || !env.ADMIN_SUPPORT_SECRET) return null;
  const providedSecret = request.headers.get('X-Admin-Support-Secret');
  if (!providedSecret || providedSecret !== env.ADMIN_SUPPORT_SECRET) return null;

  const sessao = await criarSessaoAdmin(env.DB, tenantId, suporteEmail);
  await registrarAuditoriaAdmin(env.DB, {
    tenantId,
    atorEmail: suporteEmail,
    origem: '7agents',
    acao: 'acesso_suporte',
    alvoTipo: 'tenant',
    alvoId: tenantId,
  });
  return { cookie: sessao.cookie, expiraEm: sessao.expiraEm };
}

export async function registrarAuditoriaAdmin(
  db: D1Database,
  payload: {
    tenantId: string;
    atorEmail: string;
    origem: 'isp' | '7agents';
    acao: string;
    alvoTipo: string;
    alvoId: string;
    detalhes?: Record<string, unknown>;
  },
): Promise<void> {
  await db
    .prepare(
      `INSERT INTO auditoria_suporte
       (tenant_id, ator_email, origem, acao, alvo_tipo, alvo_id, detalhes_json)
       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)`,
    )
    .bind(
      payload.tenantId,
      payload.atorEmail.trim().toLowerCase(),
      payload.origem,
      payload.acao,
      payload.alvoTipo,
      payload.alvoId,
      payload.detalhes ? JSON.stringify(payload.detalhes) : null,
    )
    .run();
}
