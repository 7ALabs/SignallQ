# Autenticação do SignallQ Admin — auth própria via D1 (SIG-136)

Spec da autenticação do painel Admin: **auth própria, controlada por nós, com usuários e
sessões no Cloudflare D1**. Custo zero. Substitui a proposta anterior de Cloudflare Access.

> **Decisão (Luiz, 2026-06-23):** usar autenticação via banco (D1), controlada pelo time, em
> vez de Cloudflare Access. Motivo: controle total sobre usuários, sessões e regras, sem
> depender de IdP externo. Runbook do Access movido para `docs_ai/_archive/`.

A maior parte é **código** (Felipe, na SIG-136). As etapas manuais do Luiz estão na seção 7.

---

## 1. Modelo

`signallq-admin-worker` + D1 `signallq-admin-db`. O worker passa a ter um módulo de auth próprio.
`/ingest/*` (app Android, `INGEST_KEY`) **não muda**. Apenas `/admin/*` passa a exigir sessão válida.

```
login (email+senha) → verifica hash no D1 → cria sessão no D1 → token httpOnly cookie
requisição /admin/* → valida token contra admin_sessions → 200 ou 401
```

## 2. Schema D1 (migration aditiva — schema.sql)

```sql
CREATE TABLE IF NOT EXISTS admin_users (
  id            TEXT    PRIMARY KEY,          -- uuid
  email         TEXT    NOT NULL UNIQUE,
  password_hash TEXT    NOT NULL,             -- pbkdf2$<iter>$<saltB64>$<hashB64>
  role          TEXT    NOT NULL DEFAULT 'admin',  -- admin | viewer
  active        INTEGER NOT NULL DEFAULT 1,
  created_at    INTEGER NOT NULL,
  last_login    INTEGER
);

CREATE TABLE IF NOT EXISTS admin_sessions (
  token_hash  TEXT    PRIMARY KEY,            -- SHA-256 do token (nunca o token cru)
  user_id     TEXT    NOT NULL,
  created_at  INTEGER NOT NULL,
  expires_at  INTEGER NOT NULL,
  last_seen   INTEGER NOT NULL,
  FOREIGN KEY (user_id) REFERENCES admin_users(id)
);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_expires ON admin_sessions(expires_at);
```

Guardamos o **hash** do token de sessão, não o token cru: vazamento do D1 não dá sessões usáveis.

## 3. Hashing de senha (Web Crypto nativo — sem libs, sem custo)

PBKDF2-HMAC-SHA256, salt aleatório de 16 bytes, **≥ 150.000 iterações**. Disponível no runtime
do Worker via `crypto.subtle` — não precisa bcrypt/scrypt externo.

```
hash = PBKDF2(pepper + senha, salt, 150000, SHA-256, 32 bytes)
armazenar: "pbkdf2$150000$" + base64(salt) + "$" + base64(hash)
verificar: recomputar com o salt guardado e comparar em tempo constante
```

`pepper` é um segredo do servidor (`ADMIN_AUTH_PEPPER`), somado à senha antes do hash — assim
o vazamento só do D1 (sem o pepper) não permite brute-force offline trivial.

## 4. Sessão

- Token: 32 bytes aleatórios (`crypto.getRandomValues`), base64url. Guardar só `SHA-256(token)`.
- Validade: 7 dias (`expires_at`); `last_seen` atualizado a cada uso (rolling opcional).
- Transporte recomendado: **cookie `httpOnly; Secure; SameSite=None`** (cross-origin
  pages.dev ↔ workers.dev exige `SameSite=None; Secure` + CORS com `credentials`).
  Cookie httpOnly evita roubo de token por XSS — melhor que bearer em localStorage.
- Logout: apaga a linha de `admin_sessions`. Revogação imediata (vantagem sobre JWT stateless).

## 5. Endpoints (worker)

| Método | Rota | Auth | Função |
|---|---|---|---|
| POST | `/admin/auth/login` | pública (rate-limit) | `{email,senha}` → cria sessão, seta cookie |
| POST | `/admin/auth/logout` | sessão | apaga a sessão |
| GET  | `/admin/auth/me` | sessão | dados do usuário logado |
| POST | `/admin/auth/users` | sessão `role=admin` | cria novo usuário |
| POST | `/admin/auth/password` | sessão | troca a própria senha |
| POST | `/admin/auth/users/:id/reset` | sessão `role=admin` | reseta senha de outro usuário |

Todas as rotas `/admin/metrics/*` e `/admin/settings` passam a validar a sessão (substitui o
`authenticate()` por `ADMIN_SECRET`). Middleware único antes do router de `/admin/*`.

Proteções mínimas: rate-limit no login (ex.: contador por IP em KV, free), mensagem de erro
genérica ("e-mail ou senha inválidos"), comparação em tempo constante.

## 6. Frontend (painel)

- `LoginPage` passa a chamar `POST /admin/auth/login` com `credentials: "include"`.
- Remover `VITE_ADMIN_API_SECRET` e o token em localStorage — a sessão vive no cookie httpOnly.
- `apiClient` envia `credentials: "include"` em todas as chamadas `/admin/*`; em 401, redireciona p/ login.
- Tela de "trocar senha" (mínima) usando `/admin/auth/password`.

## 7. Etapas manuais do Luiz (uma vez)

1. **Pepper:** gerar e configurar o segredo do servidor:
   ```
   openssl rand -hex 32
   cd integrations/cloudflare/signallq-admin-worker
   npx wrangler secret put ADMIN_AUTH_PEPPER
   ```
2. **Primeiro usuário (bootstrap):** após a SIG-136 no ar, criar o admin inicial. Duas opções:
   - Endpoint de setup único protegido por um `SETUP_SECRET` (Felipe entrega), chamado uma vez; **ou**
   - Inserir direto via `npx wrangler d1 execute signallq-admin-db --remote` com o hash gerado
     por um script utilitário do worker (Felipe entrega o script).
3. Pronto. Novos usuários e resets passam a ser feitos **pelo próprio painel** (role `admin`).

## 8. Riscos registrados (auth própria vs Access)

- **Nós passamos a ser responsáveis** pela corretude do hashing, sessão e rate-limit — feito
  conforme este spec, é seguro o suficiente para um painel interno com poucos usuários.
- **Sem MFA** nativo (o Access tinha). Mitigável depois (TOTP em D1) se necessário.
- Cookie httpOnly + Secure + SameSite=None mitiga XSS/CSRF; manter CORS restrito à origem do painel.
- Vantagem: **controle total** (usuários, papéis, revogação imediata), zero dependência externa, custo zero.

Código: **SIG-136**. Decisão: Notion → Decision Log → Auditoria SignallQ Admin.
