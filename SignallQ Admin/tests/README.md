# Plano de Testes — SignallQ Admin (Console)

> **Painel:** SignallQ Admin · **Stack:** React + TypeScript + Vite + Tailwind · **Backend:** Worker `signallq-admin` (D1 `signallq-admin-db`)
> **Criado em:** 2026-07-17 · **Caderno:** [`caderno-completo-testes-admin.xlsx`](./caderno-completo-testes-admin.xlsx)

Este documento descreve o plano de testes manuais/E2E do SignallQ Admin, executado via
Playwright contra a URL de produção real. Espelha a estrutura de
[`android/tests/README.md`](../../android/tests/README.md), adaptado para uma stack web
(sem ADB/coordenadas de toque — navegação por hash route e seletor DOM).

---

## 1. URLs — atenção à divergência

| URL | Status |
|---|---|
| `https://signallq-admin-panel.pages.dev/` | **Produção real.** Projeto Cloudflare Pages Git-connected — cada push em `main` dispara build/deploy. |
| `https://signallq.pages.dev/console/` | **Legada, desativada em 2026-07-16.** Responde 200 mas serve um build congelado, anterior à migração de tokens `md3-tobe`. Nunca mais recebe deploy. Usar essa URL para QA produz falso-negativo — já aconteceu 2x na mesma semana (ver issue [#1068](https://github.com/7ALabs/SignallQ/issues/1068)). |

Todo teste deste caderno deve rodar contra a URL de produção real.

## 2. Conta de QA

Existe uma conta dedicada para automação de testes, criada diretamente na tabela
`admin_users` do D1 (`signallq-admin-db`) em 2026-07-17:

- **E-mail:** `qa.playwright@signallq.internal`
- **Role:** `admin`
- **Senha:** não versionada neste repositório — foi entregue diretamente ao Luiz fora do
  Git. Para resetar, gerar um novo hash PBKDF2 com o pepper real (`ADMIN_AUTH_PEPPER`,
  secret do Worker) e rodar `UPDATE admin_users SET password_hash = ? WHERE email =
  'qa.playwright@signallq.internal'` via `wrangler d1 execute signallq-admin-db --remote`.
- **Como foi criada:** não existe endpoint de bootstrap sem sessão prévia
  (`POST /admin/auth/users` exige `role='admin'` já autenticado — ver
  `integrations/cloudflare/signallq-admin-worker/src/index.ts:185`). A criação usou uma
  rota temporária adicionada localmente, deployada via `npx wrangler deploy`, chamada uma
  única vez, e revertida (`git checkout --`) antes de qualquer commit — nunca existiu no
  histórico do Git nem ficou em produção além do tempo da chamada.

**Nunca usar credencial pessoal de um operador real (Luiz, Claudete, Camilo, Lia,
Rhodolfo) para rodar este caderno.**

## 3. Telas cobertas

10 rotas reais (`SignallQ Admin/src/App.tsx`), duas delas fusões de slugs legados que
continuam funcionando (GH#552 Fase 2):

| Rota | Aba no caderno | Observação |
|---|---|---|
| `/overview` | `03_Overview` | |
| `/product-analytics` | `04_Uso_App` | |
| `/diagnostics` | `05_Diagnosticos` | |
| `/networks` (+ alias `/operators`) | `06_Redes_Operadoras` | Mesmo componente `NetworksTab` nos dois hashes |
| `/ai-cost` | `07_IA_Custos` | |
| `/errors` | `08_Problemas_Incidentes` | |
| `/app-versions` | `09_Releases_Qualidade` | |
| `/system-health` | `10_Saude_Sistema` | |
| `/settings` (+ alias `/feature-flags`) | `11_Configuracoes` | Mesmo componente `SettingsTab` nos dois hashes |
| `/tools` | `12_Ferramentas` | |

Mais: `02_Login_Navegacao` (autenticação, AppLayout, GlobalFilters, tema) e
`13_Fluxos_E2E` (jornadas cruzando múltiplas telas).

## 4. Dado real vs. gap conhecido — não confundir com bug

Firebase Analytics/Crashlytics e Google Play Console **não têm billing/API habilitados em
produção** (decisão do Luiz de não assumir custo novo, ver `.claude/CLAUDE.md`). Várias
métricas em `08_Problemas_Incidentes` e `09_Releases_Qualidade` mostram "não
disponível"/"não implementado" **de propósito** — o painel declara a fonte e o motivo
explicitamente em vez de fabricar número (North Star "The Operator's Console", ver
`DESIGN.md`). Isso não é bug.

Já `Cloudflare Workers AI` (IA & Custos) e `D1` (Diagnósticos, Releases &
Qualidade/sessões por versão) são dado real e devem ser tratados como tal na validação.

**Achado a confirmar (não presumido, ver caso `CFG-003`):** a seção "Acesso da equipe" em
Configurações parece mostrar uma lista estática (Claudete/Camilo/Lia/Rhodolfo) que não
reflete `admin_users` real — a conta de QA criada nesta sessão não aparece lá. Confirmar
contra o código de `SettingsTab.tsx` antes de abrir bug.

## 5. Como rodar

1. Abrir sessão Playwright, navegar para `https://signallq-admin-panel.pages.dev/`.
2. Logar com a conta de QA (seção 2).
3. Seguir a ordem do caderno: `02_Login_Navegacao` → `03_Overview` → ... → `13_Fluxos_E2E`.
4. Para cada caso: executar os passos, comparar com o resultado esperado, marcar `Status`
   na coluna correspondente (`Não iniciado` / `Em execução` / `Passou` / `Falhou` /
   `Bloqueado` / `N/A`).
5. `01_Resumo` agrega automaticamente via fórmulas `COUNTIF`/`COUNTA` — não preencher à
   mão.
6. Falha reproduzida 2x, não explicada por gap conhecido (seção 4) ou ambiente → registrar
   em `15_Bugs_GitHub` e abrir issue.

## 6. Limitação desta primeira versão

O caderno foi gerado em uma única sessão (2026-07-17), com **47 casos** cobrindo login,
navegação global, as 10 telas e 4 fluxos E2E — grounded em conteúdo real observado ao vivo
via Playwright contra produção, não em suposição. É uma primeira passada, não cobertura
exaustiva de cada filtro/interação possível em cada tela. Ampliar por rodada de execução
real: cada `Falhou`/gap encontrado que não virar bug imediato deve virar caso novo na
próxima revisão do caderno.

As fórmulas do workbook não puderam ser recalculadas automaticamente nesta máquina (sem
LibreOffice instalado) — abrir uma vez no Excel/Google Sheets para que os totais de
`01_Resumo` sejam computados.
