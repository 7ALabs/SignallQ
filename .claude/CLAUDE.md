# Project Memory

Instructions here apply to this project and are shared with team members.

## Identidade

- App: **SignallQ** -- diagnostico de conectividade Android.
- Estrutura: **monorepo** — `android/` (Kotlin), `pwa/` (React/TS), `ios/`, `integrations/` (Cloudflare), `scripts/`, `docs_ai/`.
- Package/applicationId/namespace: **`io.signallq.app`** -- identificador tecnico, **NAO renomear jamais** (quebra Firebase/assinatura). Renomeado de `io.veloo.app` em 2026-06-28 (antes de qualquer publicacao na Play Store).
- Marca anterior: Linka -> Veloo -> **SignallQ** (rebrand em 0.16.0).
- Versao atual: **0.21.0** (versionCode 52), em `android/gradle/libs.versions.toml`. minSdk 24, targetSdk 36, compileSdk 37, JVM 17.
- **Android Stack**: Kotlin, Jetpack Compose, Hilt, Room, DataStore, WorkManager.
- **PWA Stack**: React, TypeScript, Vite, Tailwind CSS (design system SignallQ inspirado em Material 3, não Material Web).
- 15 modulos Gradle: `app` + core(5): `coreNetwork`, `coreDatabase`, `coreDatastore`, `coreTelephony`, `corePermissions` + feature(9): `featureHome`, `featureSpeedtest`, `featureWifi`, `featureDevices`, `featureDns`, `featureFibra`, `featureDiagnostico`, `featureHistory`, `featureSettings`.
- MVVM + StateFlow, Hilt DI (`AppModule.kt` + `DiagnosticoModule.kt`), Room v12 (`SignallQDatabase`), DataStore `linkaPreferencias`.
- IA: Worker Cloudflare (`integrations/cloudflare/ai-diagnosis-worker/`), URL via `BuildConfig.AI_WORKER_URL`, modelo Qwen3 30B MoE FP8 (fallback Gemini Flash), persona SignallQ.
- **Analytics**: Firebase Analytics (events) + Crashlytics (error logs). **NOT using**: Realtime DB.
- Navegacao: `AppShell.kt` -- 5 abas (Inicio, Velocidade, Sinal, Historico, Ajustes). Diagnostico/IA, Dispositivos, Fibra sao overlays, nao abas.
- Background: WorkManager `MonitoramentoWorker` (30 min).

**Identificadores tecnicos a preservar** (parecem marca, sao tecnicos): `io.signallq.app`, repo `gmmattey/linka-android`, worker `linka-ai-diagnosis-worker`, skill `linka-design`, banco `linkaKotlin.db`, canais `linka_*`, DataStore `linkaPreferencias`.

---

## Fontes da Verdade

| Dominio | Ferramenta |
|---|---|
| Execucao, backlog, cycles, prioridades | **Linear** |
| Codigo, branches, PRs, releases, historico tecnico | **GitHub** |
| Documentacao viva, decisoes consolidadas, roadmap, OS | **Notion** |
| Comunicacao e alertas | **Slack** (via integracao Linear -- nao criar fluxo manual paralelo) |
| Fluxos visuais, arquitetura, jornada, onboarding | **Miro** (so quando visual ajuda) |
| Workers, paginas publicas, infra produto | **Cloudflare** |
| Analytics, crash/logs, config Android | **Firebase / Google Cloud** |
| Pre-lancamento | **Play Console** (somente fase M3 -- nao e bloqueio atual) |

**Regra Slack:** o Linear notifica o Slack diretamente. Decisao que surgir no Slack vira issue no Linear ou pagina no Notion. Slack e saida, nao fonte da verdade.

---
## Infraestrutura e Contas Legadas

**Firebase — Projeto Novo:**
- Projeto: `signallq-app` (conta 7Agents)
- App Android: `io.signallq.app`
- Analytics: habilitado com LGPD consent gate

**Firebase — Projeto Legado (ABANDONADO):**
- Projeto: `device-streaming-ef179de4` (conta pessoal do Luiz)
- App Android: `io.linka.app.kotlin` (package antigo, deprecated)
- Status: **NÃO É MAIS USADO**
- Limpeza manual requerida: Luiz deve ir em Firebase Console → projeto `device-streaming-ef179de4` e remover o app Android `io.linka.app.kotlin`, ou arquivar o projeto inteiro se não tiver outros apps.
- Rastreamento: SIG-220

---


## Milestones

| Milestone | Data |
|---|---|
| M0 -- Fundacao e Setup | 27/06/2026 |
| M1 -- App pronto para Beta | 17/07/2026 |
| M2 -- Beta Fechado | 31/07/2026 |
| M3 -- Lancamento Play Store | 07/08/2026 |

---

## Design System

Toda UI segue o **SignallQ Design System** (`.claude/skills/linka-design/`).
Antes de criar ou editar telas/componentes, consulte a skill `linka-design` e use os tokens de `colors_and_type.css` / `SignallQTheme.kt` como fonte de verdade.

Nao-negociaveis:
- Material 3 claro, acento violeta `#6C2BFF`, semantica de status verde/ambar/vermelho
- Icones Material Symbols (Outlined), tipo Roboto, grid 8dp, card radius 16dp, flat (sem sombras pesadas)
- Superficies SignallQ (IA) sempre escuras (`#0D0D1A` / `#1A0B2E` / `#1E1130`)
- Copy em PT-BR com voce, sentence case em titulos, UPPERCASE em overlines, SEM emoji
- Metrica crua sempre acompanhada de veredito humano (Excelente/Bom/Regular/Fraco/Forte)
- Separador inline: ponto medio

Referencia rapida de tokens: `.claude/skills/linka-design/HANDOFF_README.md`.

---

## Release Process

Quando o usuario pedir para subir/deploy/publicar no Firebase, seguir OBRIGATORIAMENTE nesta ordem:

1. **Commit** -- stage todos os arquivos modificados, commit com mensagem descritiva
2. **Push** -- `git push origin main`
3. **Clean build** -- `.\android\gradlew.bat clean assembleRelease --no-build-cache` (NUNCA usar cache em release)
4. **Upload** -- `.\android\gradlew.bat appDistributionUploadRelease`

Nunca pular etapas. Nunca fazer assembleRelease sem clean + --no-build-cache antes. O cache do Gradle ja causou builds desatualizados no Firebase.

Worker Cloudflare: quando houver mudancas em `integrations/cloudflare/ai-diagnosis-worker/src/`, fazer `npx wrangler deploy` ANTES do commit.

---

## Autonomia dos Agentes

### Pode fazer sem aprovacao do Luiz
- Organizar issues dentro das regras
- Atualizar descricoes/comentarios/checklist no Linear
- Criar subissues tecnicas de issue aprovada
- Propor melhorias de fluxo
- Atualizar documentacao operacional
- Criar branch para issue aprovada
- Abrir PR pequeno ou medio
- Corrigir bug evidente dentro do escopo
- Documentar mudanca tecnica
- Consolidar informacao duplicada
- Registrar decisao ja tomada

### Precisa de aprovacao do Luiz
- Custo novo ou assinatura/plano pago
- Mudanca de escopo
- Alteracao arquitetural relevante
- Exclusao destrutiva
- Publicacao em loja
- Uso de conta pessoal/sensivel
- Mudanca de package (`io.signallq.app` -- nunca)
- Mudanca de marca
- Alteracao de cronograma principal
- Cancelamento de entrega relevante
- Automacao que envie mensagem externa ou execute acao irreversivel

---

## Modo Piloto Automatico

Quando a tarefa for bem delimitada, os agentes operam em piloto automatico:

1. Entender a issue
2. Planejar
3. Executar
4. Validar
5. Atualizar Linear
6. Abrir PR se houver codigo
7. Registrar resumo
8. Comunicar via Linear/Slack se aplicavel
9. Pedir intervencao apenas se houver bloqueio real

### Classificacao de tamanho

| Tamanho | Criterio | Modo |
|---|---|---|
| **Pequena** | Correcao localizada, doc, ajuste UI, refactor, organizacao | Piloto automatico |
| **Media** | Feature delimitada, fluxo simples, integracao prevista | Planejar, executar, registrar |
| **Grande** | Mudanca arquitetural, feature ampla, multiplos modulos | Propor plano, pedir aprovacao antes |
| **Sensivel** | Custo, conta, publicacao, seguranca, Play Console, package | Parar e pedir decisao do Luiz |

---

## Metodo de Trabalho do Luiz

- Luiz atua como CEO/fundador.
- Recebe decisoes claras, poucas, objetivas.
- Nao perguntar o obvio. Nao pedir aprovacao para tarefa operacional no escopo.
- Escalar com recomendacao + motivo, nao com pergunta aberta.
- Atualizacoes: curtas, praticas, orientadas a decisao.
- Visibilidade via Views no Linear e documentacao executiva no Notion.

---

## Agentes

Squad enxuto: 6 agentes. Validacao de device/rede e planejamento tecnico viraram skills (`/regras-android`, `/regras-diagnostico-rede`); busca de codigo e documentacao sao nativas/skill (`/gerar-docs`).

**Claudete / PM & Tech Lead**
- Manter Linear limpo, organizar backlog, priorizar, quebrar issues grandes; planejamento tecnico e decisao de arquitetura (absorveu Claudio)
- Cuidar de milestones e cycles, decidir fluxo operacional
- Ferramentas: Linear, Notion, Slack via Linear, Miro, GitHub para PR/release

**Camilo / Dev Android**
- Implementar Android (Kotlin/Compose), criar branches, abrir PRs, corrigir bugs
- Ferramentas: GitHub, Linear, Firebase/Cloudflare quando aplicavel

**Renan / Dev PWA**
- Implementar o PWA (React/TS), garantir paridade com Android
- Ferramentas: GitHub, Linear, Cloudflare

**Felipe / Admin & Dados**
- Implementar o SignallQ Admin (React/TS) e analise de dados de app
- Ferramentas: GitHub, Linear, Cloudflare, Firebase

**Lia / UX & Design**
- Propor fluxos, revisar telas, manter coerencia Material 3 + design system
- Ferramentas: Miro, Notion, Linear

**Gema / QA, Release & Higiene**
- Validar criterios de aceite, testar fluxos, apontar regressoes, gate de Done, release, higiene e documentacao (absorveu Nina/Taisa)
- Ferramentas: Linear, GitHub, Firebase/Crashlytics, Notion

---

## Rotinas Ativas

| Rotina | Frequencia | Responsavel | Saida |
|---|---|---|---|
| Daily Assincrona do Linear | Dias uteis | Claudete | Comentario no Linear + Slack via integracao |
| Weekly Planning / Grooming | Semanal | Claudete | Cycle pronto no Linear |
| Cycle Review | Final do cycle | Claudete | Resumo no Linear + Notion executivo |
| Review de Bloqueios | 2-3x por semana | Claudete | Lista curta com recomendacao para o Luiz |
| Release Readiness | Por milestone/release | Claudete + Gema | Checklist no Linear/Notion |
| Docs Sync | Semanal ou por milestone | Gema | Notion atualizado |

Rotinas que NAO devem existir: email diario, automacao Slack fora do Linear, dashboards pagos, Play Console antes de M3.

---

## Contexto Tecnico

> Estado do codigo -- atualizado em 2026-06-23 (v0.21.0).

**Testes**
- ~37 classes de teste unitario. JUnit4 + Robolectric + coroutines-test + room-testing em `android/*/src/test/`. 3 androidTest de Room/DAO. Rodar: `.\android\gradlew.bat test`.

**Documentacao**
- Doc viva em `docs_ai/` (ai/, design-system/, functional/, operations/, technical/). Obsoleto em `docs/_archive/` e `docs_ai/_archive/`. Indice: `docs_ai/README.md`.
