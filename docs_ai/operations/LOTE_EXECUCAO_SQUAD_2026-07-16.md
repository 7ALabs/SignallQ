# Lote de execução do squad — 2026-07-16

**Status:** ativo
**Última validação:** 2026-07-17 (Rhodolfo, verificação real via `git`/`gh` antes de registrar)
**Fonte de verdade:** este documento
**Escopo:** lote de 6 grupos de issues despachado por Luiz em 2026-07-16 para execução em paralelo,
cada grupo em worktree própria
**Responsável:** Claudete (dono do processo), execução por Camilo/Lia conforme grupo, fechamento e
higiene por Rhodolfo

Segue o precedente de formato de `docs_ai/operations/PROCESSO_PR_E_AGENTES_2026-07-16.md`.

---

## 1. Contexto

Luiz pediu a execução de um lote com **6 grupos de issues** em paralelo, cada grupo isolado em
worktree própria, commitado, testado e com PR aberta (sem merge automático — decisão de merge fica
com quem revisar). Este documento registra o resultado real dos 5 grupos executados e o motivo pelo
qual o 6º grupo ficou deliberadamente de fora desta rodada.

### Os 6 grupos originais do lote

1. **Console/Admin** — auditoria backend (issues #1042, #1043, #1054, #881)
2. **Motor diagnóstico/recomendação Android** — issues #998, #999, #1000
3. **Topologia Wi-Fi** — Fase 2C do épico de unificação (issue #981, relacionada ao épico-mãe #975)
4. **Design/copy pontual** — copy de onboarding + realinhamento de doc do Admin (issues #974, #1040)
5. **Dívida técnica de design system Android** — auditoria visual 1:1 da Lia (issues #1026, #1022,
   #1009, #1010)
6. **Features futuras já bem adiantadas** — dois épicos (issues #952, #951)

Grupos 1–5 foram executados nesta rodada. O grupo 6 foi deliberadamente deixado de fora — ver seção 3.

---

## 2. Os 5 grupos executados

### 2.1 Console/Admin — auditoria backend

- **Branch:** `fix/console-admin-auditoria-2026-07-16`
- **PR:** [#1064](https://github.com/gmmattey/linka-android/pull/1064)
- **Issues fechadas:** #1042, #1043, #1054, #881
- **Commits:**
  - `fix(admin-worker): filtrar metricas de producao por play_track (#1042)`
  - `fix(admin): expor environmentScope do Crashlytics na aba Versoes (#1042)`
  - `docs(admin): atualizar admin-api-schema e data-architecture (#1043)`
  - `docs(admin-worker): corrigir comentario de score/status em schema.sql (#1043)`
  - `fix(admin): validar content-type e corpo real em /admin/auth/me (#1054)`
  - `fix(admin): unificar vocabulario de issue de diagnostico Android x Admin (#881)`
- **Testes:** `npm run lint` (tsc --noEmit), `npx vitest run`, `npm run build` no SignallQ Admin;
  `npx wrangler deploy --dry-run` + `npx tsc --noEmit` no signallq-admin-worker.
- **Resultado:** Admin com lint limpo, 60 testes (16 arquivos) passando, build de produção OK
  (aviso de chunk size pré-existente, não relacionado). Worker: dry-run compilou sem erro (nenhum
  deploy real executado); erros de `tsc --noEmit` são pré-existentes (ausência de
  `@cloudflare/workers-types`, fora do fluxo normal de validação).
- **Pendências relevantes:**
  - **Deploy do `signallq-admin-worker` (`npx wrangler deploy`) pendente pós-merge** — não
    executado nesta sessão por instrução explícita de não fazer deploy real fora do fluxo de
    release.
  - Reexecutar `POST /admin/integrations/google-play/tracks/backfill` em produção após o deploy
    (idempotente, resolve atraso do backfill anterior).
  - Filtro de `play_track` só passa a ter efeito prático quando existir trilha `production` real no
    Play Console (pós-M3) — decisão de produto, não bloqueio técnico.
  - `docs_ai/operations/AUDITORIA_ADMIN_2026-07-10.md` (referenciada pela issue #881) não existe
    mais no repo — provavelmente consolidada na reorganização de docs; PR usou só o corpo da issue
    como fonte, não recriou o documento.

### 2.2 Motor diagnóstico/recomendação Android

- **Branch:** `fix/motor-diagnostico-recomendacao-2026-07-16`
- **PR:** [#1063](https://github.com/gmmattey/linka-android/pull/1063)
- **Issues fechadas:** #998, #999, #1000
- **Commits:**
  - `fix(featureDiagnostico): migrar WifiSignalQualityEngine e MobileSignalDiagnosticEngine para MetricClassifier (#998)`
  - `feat(coreRecommendation): adicionar regra filtrada para rede movel no catalogo local (#999)`
  - `docs(featureDiagnostico): corrigir kdoc desatualizado de 12 para 14 regras (#1000)`
- **Testes:** `./gradlew.bat :featureDiagnostico:test :coreRecommendation:test`,
  `./gradlew.bat app:ktlintCheck app:detekt`, suite completa `./gradlew.bat test` (rodada em
  worktree isolado após incidente de contaminação — ver pendência abaixo).
- **Resultado:** todos verdes, suite completa dos 16 módulos com BUILD SUCCESSFUL. Achados de
  detekt no `app` são pré-existentes, em arquivos fora do escopo desta PR.
- **Pendências relevantes (destacadas, não escondidas):**
  - **Incidente de processo:** a execução começou no diretório principal compartilhado
    `C:/Projetos/SignallQ` (não numa worktree isolada), até `git status` mostrar `On branch main`
    com um arquivo untracked de outra sessão — outro agente trocou a branch do diretório
    compartilhado durante a execução. O trabalho já estava commitado e pushado nesse ponto (sem
    perda de dado), mas foi criado um worktree isolado só para reverificar a suite completa e a
    integridade do diff antes de abrir a PR (já removido). **Recomendação registrada pelo grupo:**
    confirmar se existe mecanismo real de isolamento por worktree antes do dispatch — neste caso
    não havia.
  - **#998 muda comportamento real de classificação de RSSI por banda** no
    `WifiSignalQualityEngine` (antes tratava 2.4GHz e 5GHz igual). Nenhum teste existente cobria a
    saída real desse engine antes desta PR; foram adicionados testes de caracterização, mas **QA
    visual da tela Sinal em Wi-Fi 2.4GHz e 5GHz real é recomendado antes do próximo release.**
  - A issue #998 também citava `InternetDiagnosticEngine` e `SinalScreen.kt` (módulo `app`) como
    call sites que reimplementavam thresholds, mas o corpo da issue só trazia evidência concreta
    para os dois engines migrados. Esses dois arquivos **continuam não migrados**, registrado no
    kdoc do `MetricClassifier`.

### 2.3 Topologia Wi-Fi — Fase 2C

- **Branch:** `chore/topologia-wifi-fase2c-2026-07-16`
- **PR:** [#1059](https://github.com/gmmattey/linka-android/pull/1059)
- **Issues fechadas:** #981
- **Commits:** `chore(topologia): remove classificadores de topologia paralelos (Fase 2C, #981)`
- **Testes:** `ktlintCheck`, `detekt`, `test` (suite completa), `assembleDebug` — todos na worktree,
  após copiar `local.properties` do diretório principal (SDK Android não é herdado por worktree
  novo).
- **Resultado:** tudo verde. Achados de detekt pré-existentes e fora da área tocada. Suite completa
  sem falhas, `assembleDebug` gerou APK sem erro.
- **Pendências relevantes:**
  - **PR aberta, NÃO mergeada** — decisão de merge pendente.
  - **Ressalva de soak period:** entre o merge da Fase 2B (2026-07-15) e esta Fase 2C
    (2026-07-16) passou menos de 24h — não necessariamente "um ciclo" de soak em produção real como
    a issue #981 pedia explicitamente ("bloqueada de propósito... revisitar depois desse período de
    soak"). Análise estática confirma zero consumidor de produção dos motores antigos, mas **quem
    decidir o merge deve avaliar explicitamente se o soak period foi satisfeito ou conscientemente
    dispensado** — não está implícito nem decidido por esta execução.
  - **Issue #975 (épico-mãe) NÃO foi fechada** — critérios de aceite parecem satisfeitos pelas
    fases já mergeadas, mas foi encontrado um **quarto motor concorrente**
    (`MeshDetector`/`TopologyDiagnostic`, em `:featureDiagnostico`) com heurística própria que pode
    divergir do motor canônico. Issue **#1058** foi aberta para rastrear esse achado. Fica pendente
    a decisão de fechar #975 agora ou só depois de #1058.
  - `docs_ai/tests/README.md` ("Total atual: 37 classes de teste") já estava desatualizado antes
    desta mudança (contagem real é 118 arquivos de teste) — a PR corrigiu só as 2 linhas que
    referenciavam testes removidos por ela, não a contagem geral (fora de escopo desta task).

### 2.4 Design/copy pontual — onboarding + DESIGN.md Admin

- **Branch:** `fix/copy-onboarding-e-design-admin-docs-2026-07-16`
- **PR:** [#1056](https://github.com/gmmattey/linka-android/pull/1056)
- **Issues fechadas:** #974, #1040
- **Commits:**
  - `fix(onboarding): alinhar copy da tela 1 (subtitulo e botao) a spec To-Be (#974)`
  - `docs(admin): realinhar DESIGN.md ao prototipo signallq-admin-md3-tobe (#1040)`
- **Testes:** `./gradlew.bat :app:ktlintCheck` (verde);
  `./gradlew.bat :app:testDebugUnitTest --tests "*Onboarding*"` (passou, inclui
  `OnboardingPermissoesTest`); revisão manual do `DESIGN.md` contra
  `docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md` (doc-only, sem teste
  automatizado aplicável).
- **Resultado:** verde. `ktlintCheck` sem violação nos arquivos tocados, testes de onboarding
  passaram. Detekt (pre-push hook) só apontou achados pré-existentes fora do escopo.
- **Pendências relevantes:**
  - Migração real de código do Admin (`SignallQ Admin/src/index.css` + `Sidebar.tsx`/nav) para os
    tokens md3-tobe fechados no `DESIGN.md` ainda não existia como issue — issue **#1057** aberta
    para rastrear (fora do escopo de #1040, que era doc-only).
  - Botão da tela 1 do onboarding agora usa string dedicada `onboarding_tela1_btn_comecar` em vez de
    reaproveitar `onboarding_btn_continuar` (que continua servindo só a tela 2/permissões) —
    decisão técnica do grupo para não vazar a mudança de copy para a tela 2.
  - **Risco de ambiente compartilhado:** a task iniciou com `git switch -c` direto no diretório
    principal `C:/Projetos/SignallQ` antes de perceber que outra sessão estava ativa lá (branch
    `fix/motor-diagnostico-recomendacao-2026-07-16` com WIP não commitado). O grupo reverteu só os
    3 arquivos próprios, devolveu o diretório principal ao estado original e migrou o trabalho para
    worktree isolada nova. Confirmado via reflog que o WIP da outra sessão foi commitado por ela
    mesma (`d363953f`/#998) sem perda de dado. Registrado como lição de processo, não bloqueio.
  - Token de "Info" (status) do Console sem mapeamento dedicado confirmado no protótipo md3-tobe —
    usado `secondary` como stand-in até nova auditoria.
  - Breakpoint exato do `Md3NavRail` não definido no protótipo — decisão de produto pendente com
    Claudete/Camilo.

### 2.5 Dívida técnica de design system Android

- **Branch:** `chore/design-system-debt-tokens-2026-07-16`
- **PR:** [#1065](https://github.com/gmmattey/linka-android/pull/1065)
- **Issues fechadas:** #1026, #1022, #1009, #1010
- **Commits:**
  - `fix(android-design-system): substituir LkColors.accent hardcoded por c.primary reativo ao tema (#1026)`
  - `refactor(android-design-system): consolidar drag handle e SheetInfoRow duplicados em HomeScreen (#1022)`
  - `fix(android-design-system): consolidar uso de tokens LkRadius/LkSpacing (#1009)`
  - `docs(design-system): realinhar PRODUCT.md e DESIGN.md (raiz) ao design system TO-BE (#1010)`
- **Testes:** `ktlintCheck`, `detekt`, `test`, `assembleDebug` — todos após implementar as 4 issues,
  antes de commitar/abrir PR.
- **Resultado:** todos BUILD SUCCESSFUL. Detekt só com warnings pré-existentes fora da área tocada.
  Suite completa (391 tasks) sem falhas. `assembleDebug` gerou o APK debug sem erro.
- **Pendências relevantes:**
  - **Validação visual da Lia pendente** nas 4 sheets de `HomeScreen.kt` afetadas por #1022 — o
    `SheetDragHandle` público (32dp) troca o handle privado (40dp) anterior, e o `LkSheetInfoRow`
    público muda espaçamento (padding vertical simétrico em vez de só embaixo, ~12dp a mais de
    respiro) e peso de fonte do valor (titleSmall/Medium/0.1sp em vez de bodyMedium/SemiBold/0.2sp)
    — mudança perceptível, registrada explicitamente no commit e na PR, não escondida.
  - `AjustesScreen.kt` tem `RoundedCornerShape(4.dp)` e `RoundedCornerShape(2.dp)` que não mapeiam
    para nenhum `LkRadius` existente (menor token é `input`=12dp) — decisão de criar token novo fica
    para a Lia, sem issue nova aberta por conta própria.
  - `OperadoraLogoCatalog.kt`/`OperadoraBadge.kt` continuam usando `LkColors.accent` como cor de
    marca fallback — julgamento registrado de que é categoria diferente (cor de marca de operadora,
    não token de acento de UI); vale Rhodolfo/Lia confirmarem esse julgamento no review.
  - `SignallQ Admin/DESIGN.md` e `SignallQ Admin/PRODUCT.md` não foram tocados (fora do escopo que a
    própria issue #1010 delimitou).

---

## 3. Grupo 6 — "Features futuras já bem adiantadas" (#952, #951): deliberadamente fora desta rodada

As issues **#952** (motor de diagnóstico remoto no Cloudflare com fallback local versionado) e
**#951** (diretório remoto de provedores com canais oficiais e atualização automática) **não**
foram executadas nesta rodada. Motivo:

1. **Ambas se declaram explicitamente como épico no próprio corpo da issue** — cada uma traz a nota
   "quebrar em issues menores antes da implementação" e deixa claro que "não deve ser implementada
   em um único PR". Executar código diretamente a partir do texto do épico contrariaria a própria
   issue.
2. **Ambas envolvem infraestrutura Cloudflare nova e custo novo**: a arquitetura original de #952
   propunha D1 + KV ou R2; a de #951 propunha D1 + R2 + Cron Trigger + Queue. Introduzir esses
   componentes é **custo novo de infraestrutura** e **alteração arquitetural relevante** —
   conforme `.claude/CLAUDE.md`, seção "Autonomia dos Agentes", ambos exigem aprovação explícita do
   Luiz antes de qualquer código, e não estão na lista do que os agentes podem decidir sozinhos.

Por isso o grupo 6 ficou de fora do lote de execução paralela: não havia decisão de arquitetura nem
aprovação de custo para nenhum agente prosseguir com implementação.

### Desenvolvimento relacionado, fora desta execução (verificado em 2026-07-17)

Ao verificar o estado real do repositório para este documento, foi confirmado que — em paralelo a
este lote, fora dos 5 grupos aqui descritos — Luiz já aprovou (2026-07-16) uma arquitetura reduzida
para o grupo 6, cortando todo componente Cloudflare que introduzisse custo novo além do D1 (já em
uso no projeto):

- **ADR-008** (`docs_ai/decisions/ADR-008-grupo6-features-futuras-d1-only.md`) registra a decisão:
  ruleset publicado de #952 vira linha em tabela D1 em vez de objeto KV/R2; diretório de provedores
  de #951 vira D1-only (logo aponta para URL direta da fonte oficial em vez de cópia em R2), sem
  Cron Trigger nem Queue.
- Branch `docs/adr-008-grupo6-d1-only`, **PR #1062** (`docs(decisions): ADR-008 - grupo 6 usa
  D1-only, sem custo novo`) — **aberta, ainda não mergeada**, `mergeable: MERGEABLE`.
- Essa aprovação **desbloqueou apenas o planejamento**: issues **#1060** ("Task - Motor de
  diagnóstico remoto Fase 0: contrato, inventário e schema D1-only", parte de #952) e **#1061**
  ("Task - Diretório de provedores Fase 1: schema D1 e leitura por ASN (sem R2/Cron/Queue)", parte
  de #951) foram criadas como primeiras fatias executáveis, ambas explicitamente delimitadas para
  não incluir execução remota real nem Worker em produção.
- **#952 e #951 continuam abertas como épicos** — não foram fechadas, nem por este lote nem pelo
  ADR. Nenhuma implementação de código para #1060/#1061 existe ainda.

Ou seja: a barreira de aprovação que motivou deixar o grupo 6 fora desta rodada já foi
parcialmente resolvida (arquitetura aprovada, escopo cortado, primeiras issues de execução
desenhadas), mas **nenhum código foi implementado** — o grupo 6 segue corretamente fora do lote de
5 PRs aqui documentado. PR #1062 (só documentação da decisão) está pendente de merge; #1060/#1061
seguem como trabalho futuro de implementação.

---

## 4. Destaque de pendências que exigem atenção (não escondidas)

Resumo das pendências mais relevantes dos 5 grupos, para visibilidade rápida:

| Grupo | Pendência | Tipo |
|---|---|---|
| Console/Admin (#1064) | Deploy do `signallq-admin-worker` pendente pós-merge; reexecutar backfill de tracks depois | Ação pós-merge |
| Console/Admin (#1064) | Doc `AUDITORIA_ADMIN_2026-07-10.md` referenciada pela #881 não existe mais no repo | Documentação órfã |
| Motor diagnóstico (#1063) | Incidente de diretório compartilhado durante a execução (sem perda de dado, já mitigado) | Processo |
| Motor diagnóstico (#1063) | QA visual real recomendado para mudança de comportamento de RSSI por banda (Wi-Fi 2.4/5GHz) | QA pendente |
| Topologia Wi-Fi (#1059) | Soak period da Fase 2B possivelmente não cumprido (menos de 24h) — decisão de merge deve avaliar isso explicitamente | Decisão de release |
| Topologia Wi-Fi (#1059) | Quarto motor concorrente (MeshDetector/TopologyDiagnostic) achado, issue #1058 aberta; #975 não fechada | Débito técnico rastreado |
| Design/copy (#1056) | Risco de ambiente compartilhado no início da execução (sem perda de dado, já mitigado) | Processo |
| Design/copy (#1056) | Migração real de código do Admin para tokens md3-tobe ainda não existe (#1057 aberta) | Débito técnico rastreado |
| Dívida design system (#1065) | Validação visual da Lia pendente em 4 sheets de HomeScreen (mudança perceptível de handle/espaçamento/fonte) | QA pendente |
| Grupo 6 (#952/#951) | Épicos seguem abertos; ADR-008 aprovado mas PR #1062 não mergeada; #1060/#1061 são só planejamento, zero código | Arquitetura/aprovação |

Nenhuma dessas 5 PRs foi mergeada nesta execução — decisão de merge de cada uma fica para quem
revisar (Claudete/Camilo/Luiz conforme o caso), não automática.

---

## 5. Limpeza de worktree (fechamento desta tarefa)

Executada por Rhodolfo em 2026-07-17, conforme `.claude/CLAUDE.md` (seção "Disciplina de Branches e
PRs" → "Limpeza de worktree é parte de FECHAR a tarefa"). Para cada worktree: confirmado
`git status` limpo (sem mudança não commitada) e `git status -sb` sem `ahead` (branch já pushada)
antes de remover — nenhuma branch local ou remota foi apagada, já que as 5 PRs seguem abertas, sem
merge.

| Worktree | Branch | Resultado |
|---|---|---|
| `C:/Projetos/SignallQ_worktrees/fix-console-admin-auditoria-2026-07-16` | `fix/console-admin-auditoria-2026-07-16` | Removida (limpa e sincronizada com origin) |
| `C:/Projetos/SignallQ_worktrees/topologia-fase2c-2026-07-16` | `chore/topologia-wifi-fase2c-2026-07-16` | Removida (limpa e sincronizada com origin) |
| `C:/Projetos/SignallQ_worktrees/fix-copy-onboarding-design-admin-docs-2026-07-16` | `fix/copy-onboarding-e-design-admin-docs-2026-07-16` | Removida (limpa e sincronizada com origin) |
| `C:/Projetos/SignallQ_worktrees/chore-design-system-debt-tokens-2026-07-16` | `chore/design-system-debt-tokens-2026-07-16` | Removida (limpa e sincronizada com origin) |

O grupo "Motor diagnóstico/recomendação" (#1063) não tinha worktree isolada própria para remover —
rodou no diretório principal compartilhado por causa do incidente de processo descrito na seção
2.2; o worktree ad-hoc criado só para reverificação já havia sido removido pelo próprio grupo antes
deste fechamento.

---

## 6. Referências

- `docs_ai/operations/PROCESSO_PR_E_AGENTES_2026-07-16.md` — disciplina de PR/branch/dispatch que
  motivou a exigência de isolamento por worktree e checagem real antes de declarar.
- `.claude/CLAUDE.md`, seções "Autonomia dos Agentes" e "Disciplina de Branches e PRs".
- `docs_ai/decisions/ADR-008-grupo6-features-futuras-d1-only.md` (branch
  `docs/adr-008-grupo6-d1-only`, PR #1062) — decisão de arquitetura do grupo 6.
- PRs desta rodada: [#1064](https://github.com/gmmattey/linka-android/pull/1064),
  [#1063](https://github.com/gmmattey/linka-android/pull/1063),
  [#1059](https://github.com/gmmattey/linka-android/pull/1059),
  [#1056](https://github.com/gmmattey/linka-android/pull/1056),
  [#1065](https://github.com/gmmattey/linka-android/pull/1065).
