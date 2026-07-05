# Docs Refresh + Redesign do Squad — 2026-07-05

Execução autônoma (Claudete, piloto automático) do refresh completo da documentação para
o estado real do código (v0.23.0, versionCode 56) e redesign do fluxo do squad de agentes.
PR [#482](https://github.com/gmmattey/linka-android/pull/482) — mergeada em `main`.

## O que mudou na documentação (64 arquivos)

- **Versão**: 0.21.0 → 0.23.0 (versionCode 52 → 56) em `.claude/CLAUDE.md`, `README.md`,
  `docs_ai/README.md` e ~30 docs com metadados defasados (eram v0.16.0/2026-06-21).
- **Namespace real corrigido** (achado crítico): o package/applicationId/namespace do código
  é `io.signallq.app` (renomeado de `io.veloo.app` em 2026-06-28). Muitos docs diziam que o
  identificador "preservava io.veloo.app" — errado. Nuance documentada: o **caminho físico**
  do código do `:app` ainda é `app/src/main/kotlin/io/veloo/app/kotlin/`, mas o **package
  declarado** nos arquivos é `io.signallq.app` (sem sufixo `.kotlin`). Core = `io.signallq.app.core.*`,
  feature = `io.signallq.app.feature.*`.
- **Funcional/telas**: inventário reconciliado com o diretório real (32 `.kt`); nomes corretos
  (`SignallQPulseScreen`, `FibraModemScreen`, `AjustesScreen`, `LLMChatScreen`); navegação
  correta (5 abas: Início/Velocidade/Sinal/Histórico/Ajustes + overlays Diagnóstico/Dispositivos/Fibra;
  não existe aba "Mais").
- **Design system**: tokens `linka*` → `signallQ*`, tema `SignallQTheme.kt`, `signallQTypography`;
  valores (acento #6C2BFF, grid 8dp, radius 16dp) confirmados corretos; TODO de MD3 variants resolvido.
- **Cloudflare/IA**: 3 workers documentados (incl. `signallq-privacy-worker`, antes ausente);
  endpoints reais do admin-worker (feature-flags, alerts, metrics/errors, ai-usage);
  modelo Qwen3 30B MoE / Gemini corrigido (Gemini vira primário quando `GEMINI_API_KEY` setada).
- **CHANGELOG raiz**: consolidado — aponta `android/CHANGELOG.md` como autoritativo e adiciona
  as seções 0.22.0/0.22.1/0.23.0 que faltavam.
- **Históricos**: `ARCHITECTURE_REVIEW.md`, `ARCHITECTURE_AUDIT.md` e `MIGRACAO_ARQUITETURA_2026.md`
  marcados como documentos históricos (não fingem ser estado atual).

## Limpeza / reorganização

- Removido lixo `pwa/` (`eitam.md`, `henrique.md`) sobrado do commit `0c486b4c`.
- `docs_ai/operations/PIPELINE_AUTONOMO.md` (fluxo aspiracional nunca implementado) arquivado
  em `docs_ai/_archive/PIPELINE_AUTONOMO_2026-07-05.md`.
- Removido um arquivo-lixo untracked que vazou do scratchpad para `android/`.

## Redesign do fluxo do squad (ADR-006)

`docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`. Principais decisões:

1. **Fonte única da verdade**: `.claude/CLAUDE.md` + `.claude/agents/*`. `docs_ai/ai/*` viram
   resumos apontadores (elimina a deriva que criou docs de squad antigo com 9+ agentes).
2. **Fluxo paralelo**: Claudete quebra → Camilo/Felipe/Lia em trilhas independentes → Gema (gate único de Done).
3. **Gate de UX condicional**: Lia revisa antes só quando a mudança é visual/de fluxo; bug/lógica pula.
4. **Loop de review limitado**: Gema→implementador máx. 2 rodadas; 3ª escala para Claudete.
5. **WIP 1/agente**; **handoff via Linear + GitHub** (scripts Discord depreciados).
6. `docs_ai/ai/*` e `AGENTS_QUICK_REFERENCE.md` reescritos para o squad de 5.

## Sincronização de rastreadores

- **GitHub Issues**: fechada #315 (daily obsoleta de 2026-06-26, citava PWA/Renan). Bugs ativos
  #480, #478, #219 mantidos (tocados hoje). 
- **Linear**: projeto "SignallQ | Android" coerente (0 issues In Progress órfãs, fluxo via PRs).
  Projeto "SignallQ | WebApp" (PWA) — 16 issues todas Done, **produto descontinuado**; comentário
  adicionado recomendando cancelamento. Projeto "SignallQ Admin Panel" com target vencido (27/06)
  mas ainda ativo (trabalho recente no changelog).

## PENDENTE — precisa de decisão/autorização do Luiz

- **Notion / Miro**: sincronização **bloqueada** — exigem OAuth não completável em sessão
  não-interativa. Para sincronizar depois (autorizar conectores via claude.ai ou `claude mcp`):
  - **Notion**: publicar docs funcionais/roadmap consolidados desta atualização (estado v0.23.0,
    fluxo do squad ADR-006, descontinuação PWA).
  - **Miro**: diagrama de arquitetura dos 15 módulos e do fluxo de 5 agentes (ADR-006).
- **Linear**: cancelar o projeto "SignallQ | WebApp" (PWA descontinuado) — não cancelei por conta
  própria (cancelamento de entrega é decisão do Luiz).
- **Higiene de branches**: 74 branches remotas (13 `worktree-*` órfãs) e 2 locais além de main.
  Não deletei em run autônomo — requer verificação por-branch (`git diff main..<branch>`) via
  skill `higiene`. Listar e limpar num passe dedicado.
- **Personas**: `.claude/agents/*.md` ainda têm blocos "Discord — Notificações" / "Pipeline
  Autônomo" com scripts depreciados; limpar para consistência com ADR-006 num próximo passe.

## Verificação

- `main` local == `origin/main` (`da79485f`), working tree limpo.
- CI da PR #482: Detekt, Ktlint, Unit Tests, Build Debug APK, Cloudflare Pages — todos verde.
