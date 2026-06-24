# Runbook — Cloudflare Access para o SignallQ Admin (SIG-136)

Procedimento manual (dashboard Cloudflare) para proteger o painel Admin com
**Cloudflare Access (Zero Trust)** em vez de senha única. Custo zero (free tier até 50 usuários).
Executado por **Luiz**. A parte de código fica na SIG-136 (Felipe).

> Decisão registrada por Luiz em 2026-06-23. Fonte da verdade da decisão:
> Notion → Decision Log → "Auditoria SignallQ Admin" e issue SIG-136.

---

## 0. Antes de começar — a decisão que define o caminho

Cloudflare Access **não consegue proteger uma URL `*.workers.dev` diretamente** (é um
domínio compartilhado da Cloudflare, fora de qualquer zona sua). Existem dois caminhos free,
e o que você escolhe depende de uma única pergunta:

**Você tem algum domínio adicionado à sua conta Cloudflare (uma "zone")?**

| Resposta | Caminho | O que protege |
|---|---|---|
| **Sim** (ex.: `signallq.com.br`, ou qualquer domínio seu na Cloudflare) | **Caminho A — Custom Domain + Access** (recomendado, completo) | Frontend **e** API do worker |
| **Não** tenho domínio na Cloudflare | **Caminho B — Access no Pages** (free, imediato, parcial) | Só o frontend (painel) |

Se você não tem domínio mas quer a proteção completa, registrar/transferir um domínio para a
Cloudflare é a opção (custo do domínio ~R$40–60/ano — único custo possível aqui; **não é
obrigatório** para ter o painel protegido hoje via Caminho B).

Recomendação: **Caminho A se você já tiver um domínio na conta; senão Caminho B agora** e
migrar para A quando houver domínio.

---

## Caminho A — Custom Domain + Access (completo, recomendado)

Protege tanto o painel quanto a API. O worker passa a confiar no header
`Cf-Access-Jwt-Assertion` que a Cloudflare injeta nas requisições já autenticadas.

### A.1. Dar um Custom Domain ao worker da API

1. [dash.cloudflare.com](https://dash.cloudflare.com) → **Workers & Pages**
2. Abra o worker **`signallq-admin`**
3. Aba **Settings** → **Domains & Routes** → **Add** → **Custom Domain**
4. Informe um subdomínio do seu domínio, ex.: `admin-api.seudominio.com`
5. Confirme. A Cloudflare cria o registro DNS e o certificado automaticamente (1–2 min)
6. Anote a nova URL: `https://admin-api.seudominio.com`

### A.2. (Opcional, recomendado) Custom Domain para o painel

Se o painel está em `*.pages.dev`, dê também um custom domain a ele:

1. **Workers & Pages** → projeto **`signallq-admin-panel`** (Pages)
2. **Custom domains** → **Set up a custom domain** → ex.: `admin.seudominio.com`
3. Confirme (DNS + cert automáticos)

### A.3. Criar a Application no Access (protege a API)

1. Menu lateral → **Zero Trust** → na primeira vez, defina um **team name** (ex.: `signallq`) — free
2. **Access** → **Applications** → **Add an application** → **Self-hosted**
3. Preencha:
   - **Application name:** `SignallQ Admin API`
   - **Session duration:** `24 hours`
   - **Application domain:** `admin-api.seudominio.com`
   - **Path:** deixe em branco para proteger tudo **OU** `admin` para proteger só `/admin/*`
     > ⚠️ **Importante:** as rotas `/ingest/*` (app Android) **não podem** ficar atrás do Access,
     > senão o app para de enviar telemetria. Se proteger por path, use `admin` e deixe `ingest` livre.
     > Se o Access não permitir granularidade de path suficiente, mantenha o worker validando a
     > `INGEST_KEY` em `/ingest/*` (já é o caso hoje) e proteja só `/admin/*`.
4. **Next**

### A.4. Policy de acesso

1. **Policy name:** `Acesso autorizado`
2. **Action:** `Allow`
3. **Configure rules** → **Include** → **Emails** → adicione `giammattey.luiz@gmail.com`
   (e qualquer outro e-mail do time que deva ter acesso)
4. Salvar

### A.5. Identity provider (login)

1. Em **Zero Trust → Settings → Authentication** → **Login methods**
2. Adicione **Google** (recomendado) — ou deixe **One-time PIN** (envia código por e-mail, zero setup)
3. Salvar

### A.6. Apontar o painel para a nova API e remover o secret do bundle

No projeto Pages **`signallq-admin-panel`** → **Settings → Environment variables → Production**:

- `VITE_ADMIN_API_BASE_URL` = `https://admin-api.seudominio.com`
- `VITE_ENABLE_MOCKS` = `false`
- **Remover** `VITE_ADMIN_API_SECRET` (não deve mais existir — o Access faz a autenticação)

> A parte de código (remover `LoginPage`, parar de mandar `Authorization`, opcionalmente validar
> o JWT do Access no worker) está na **SIG-136**, feita pelo Felipe.

### A.7. Validação (checklist)

- [ ] Abrir `https://admin-api.seudominio.com/admin/metrics/overview` anônimo → redireciona para a tela de login do Access
- [ ] Login com `giammattey.luiz@gmail.com` → acesso concedido
- [ ] Login com e-mail não autorizado → negado
- [ ] App Android continua enviando diagnóstico (rodar 1 diagnóstico e conferir nova linha no D1)
- [ ] Painel carrega dados reais após login
- [ ] `VITE_ADMIN_API_SECRET` não existe mais no build

---

## Caminho B — Access no Pages (free, imediato, sem domínio próprio)

Protege o **painel** (frontend) usando a integração nativa do Cloudflare Pages com o Access,
sem precisar de domínio. A **API** (`*.workers.dev`) continua protegida pela `ADMIN_SECRET`
até existir um domínio para migrar ao Caminho A — por isso o passo B.4 (tirar o secret do
bundle) é **obrigatório** aqui para não vazar o token.

### B.1. Criar o team Zero Trust

1. **Zero Trust** → definir **team name** (ex.: `signallq`) — free

### B.2. Proteger o projeto Pages com Access

1. **Workers & Pages** → projeto **`signallq-admin-panel`**
2. **Settings** → role até **Access policy** (integração nativa do Pages)
3. **Enable Access policy** (cobre produção e previews `*.pages.dev`)
4. Isso cria uma Application no Access automaticamente

### B.3. Policy e login

1. **Zero Trust → Access → Applications** → abra a aplicação do painel criada no passo anterior
2. **Policies** → **Allow** → **Include → Emails →** `giammattey.luiz@gmail.com`
3. **Settings → Authentication** → habilitar **Google** ou **One-time PIN**

### B.4. Tirar o secret do bundle (obrigatório neste caminho)

No Pages **`signallq-admin-panel`** → **Settings → Environment variables → Production**:

- `VITE_ENABLE_MOCKS` = `false`
- `VITE_ADMIN_API_BASE_URL` = `https://signallq-admin.giammattey-luiz.workers.dev`
- **Garantir que `VITE_ADMIN_API_SECRET` NÃO está definido no build**

O painel passa a pedir o secret via a tela de login (digitado pelo usuário, guardado só no
`localStorage` do navegador já autenticado pelo Access) — nunca embutido no JS público.

### B.5. Validação

- [ ] Abrir a URL `*.pages.dev` anônimo → tela do Cloudflare Access antes de carregar o painel
- [ ] Login autorizado → painel abre; login não autorizado → negado
- [ ] `VITE_ADMIN_API_SECRET` ausente do build (conferir no bundle de produção)
- [ ] App Android continua enviando telemetria normalmente (rotas `/ingest/*` intactas)

---

## Rollback

- **Caminho A:** remover a Application do Access (Zero Trust → Access → Applications → Delete);
  o worker volta a responder direto. Reverter as env vars do Pages se necessário.
- **Caminho B:** desabilitar a **Access policy** no projeto Pages.

Nenhum dos caminhos altera D1, `INGEST_KEY` ou o app Android.

---

## Resumo

| | Caminho A (Custom Domain) | Caminho B (Pages Access) |
|---|---|---|
| Protege | Painel + API | Só o painel |
| Precisa de domínio | Sim (já na Cloudflare) | Não |
| Custo | Zero (domínio já existente) | Zero |
| Risco residual | Nenhum | API ainda depende da `ADMIN_SECRET` (mas fora do bundle) |
| Recomendado quando | Você tem domínio na conta | Não tem domínio ainda |

Código relacionado: **SIG-136**. Decisão: Notion → Decision Log → Auditoria SignallQ Admin.
