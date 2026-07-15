export async function hashPassword(password: string, pepper: string): Promise<string> {
  const enc = new TextEncoder();
  const keyMat = await crypto.subtle.importKey("raw", enc.encode(pepper + password), "PBKDF2", false, ["deriveBits"]);
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const hash = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations: 100000, hash: "SHA-256" },
    keyMat,
    256,
  );
  const b64 = (buf: ArrayBuffer) => btoa(String.fromCharCode(...new Uint8Array(buf)));
  return `pbkdf2$100000$${b64(salt.buffer)}$${b64(hash)}`;
}

export async function verifyPassword(password: string, storedHash: string, pepper: string): Promise<boolean> {
  const parts = storedHash.split("$");
  if (parts.length !== 4) return false;
  const iterations = Number.parseInt(parts[1] ?? "0", 10);
  const saltB64 = parts[2] ?? "";
  const hashB64 = parts[3] ?? "";
  const enc = new TextEncoder();
  const salt = Uint8Array.from(atob(saltB64), (char) => char.charCodeAt(0));
  const expectedHash = Uint8Array.from(atob(hashB64), (char) => char.charCodeAt(0));
  const keyMat = await crypto.subtle.importKey("raw", enc.encode(pepper + password), "PBKDF2", false, ["deriveBits"]);
  const computed = new Uint8Array(await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
    keyMat,
    256,
  ));
  if (computed.length !== expectedHash.length) return false;
  let diff = 0;
  for (let index = 0; index < computed.length; index += 1) diff |= computed[index]! ^ expectedHash[index]!;
  return diff === 0;
}

export async function sha256Hex(input: string): Promise<string> {
  const buffer = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  return Array.from(new Uint8Array(buffer)).map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function generateToken(): string {
  const buffer = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...buffer)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

export async function createSession(userId: string, db: D1Database): Promise<string> {
  const token = generateToken();
  const tokenHash = await sha256Hex(token);
  const now = Math.floor(Date.now() / 1000);
  const expiresAt = now + 7 * 24 * 3600;
  await db.prepare(
    "INSERT INTO admin_sessions (token_hash, user_id, created_at, expires_at, last_seen) VALUES (?, ?, ?, ?, ?)",
  ).bind(tokenHash, userId, now, expiresAt, now).run();
  return token;
}

export async function validateSession(token: string, db: D1Database): Promise<{ userId: string; role: string } | null> {
  const tokenHash = await sha256Hex(token);
  const now = Math.floor(Date.now() / 1000);
  const row = await db.prepare(
    "SELECT s.user_id, u.role FROM admin_sessions s JOIN admin_users u ON u.id = s.user_id WHERE s.token_hash = ? AND s.expires_at > ? AND u.active = 1",
  ).bind(tokenHash, now).first<{ user_id: string; role: string }>();
  if (!row) return null;
  await db.prepare("UPDATE admin_sessions SET last_seen = ? WHERE token_hash = ?").bind(now, tokenHash).run();
  return { userId: row.user_id, role: row.role };
}

export async function revokeSession(token: string, db: D1Database): Promise<void> {
  const tokenHash = await sha256Hex(token);
  await db.prepare("DELETE FROM admin_sessions WHERE token_hash = ?").bind(tokenHash).run();
}
