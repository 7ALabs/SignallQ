---
description: Kickoff de sessão SignallQ — orienta Claude sobre o estado atual do projeto (versão, pendências, milestone, skills disponíveis). Use no início de qualquer sessão nova.
allowed-tools: Read(*), Bash(*)
---

## Estado atual do projeto (lido dos arquivos agora)

**Versão Android:**
!`grep -E "versionName|versionCode" "${CLAUDE_PROJECT_DIR:-.}/android/gradle/libs.versions.toml" 2>/dev/null | head -6`

**Últimas entradas do CHANGELOG:**
!`head -25 "${CLAUDE_PROJECT_DIR:-.}/android/CHANGELOG.md" 2>/dev/null`

**Milestones:** ver [`docs_ai/RELEASES.md`](../../docs_ai/RELEASES.md) e a skill [`estimativa-impacto`](../skills/estimativa-impacto/) para o mapa vigente.

---

## Sua tarefa

Com base nas informações acima, apresente ao usuário um briefing de sessão conciso:

1. **Versão atual** — `versionName` + `versionCode` (consumer). Se relevante, também `proVersionName`/`proVersionCode`.
2. **Último release** — data e o que foi entregue (do CHANGELOG).
3. **Milestone atual** — comparar hoje com os alvos e dizer em qual estamos.
4. **Pendências críticas** — consultar `gh issue list --repo buildea-labs/signallq --state open --limit 20` (bugs e tasks abertos) e listar as 3 mais críticas.

Em seguida, pergunte: **"Em que vamos trabalhar hoje?"**

---

## Skills e comandos disponíveis (referência sua — não exibir ao usuário)

| Skill / Comando | Quando usar automaticamente |
|-----|-----|
| `/signallq` | Início de sessão nova sem contexto anterior |
| `/task <descrição>` | Registrar demanda: cria issue e faz kickoff do squad |
| `/SignallQ-design create` | Criar nova tela ou componente Compose |
| `/SignallQ-design review` | Editar arquivo em `ui/screen/` ou `ui/component/` |
| `/SignallQ-design tokens` | Dúvida sobre cor, espaçamento ou tipografia |
| `/signallq-arch create` | Criar módulo, ViewModel, DAO, serviço ou repositório |
| `/signallq-arch review` | Revisão arquitetural de arquivo Kotlin |
| `/signallq-arch map` | Dúvida sobre onde implementar algo |
| `/signallq-docs impact` | Após qualquer mudança de código — sempre |
| `/signallq-docs update` | Atualizar doc específico |
| `/signallq-docs new` | Criar novo documento oficial |
| `/signallq-docs check` | Auditar docs de uma feature |
| `/estimativa-impacto` | Avaliar tamanho/risco/milestone de uma issue antes do breakdown |
| `/checar-release` | Checklist pré-release (Android + Cloudflare) |
| `/gerar-docs` | Gerar ou atualizar documentação funcional/técnica/testes |
| `/auditar-ux` | Auditoria profunda de design system e usabilidade (invocada por Claudete/Caio) |
| `/motor-diagnostico` | Trabalho no engine de diagnóstico, speedtest ou IA |
| `/cloudflare-d1-console` | Antes de mexer em schema/migration/query do Admin Worker |
| `/regras-android` | Antes de mexer em permissão, Wi-Fi, DNS ou background |
| `/regras-diagnostico-rede` | Thresholds e padrões técnicos brasileiros |
| `/padroes-compose` | Padrões de Screen/ViewModel/StateFlow Compose |
| `/protocolo-ci-android` | Falha de CI ou dependabot travado |
| `/protocolo-ktlint` | Violação Ktlint ou supressão no editorconfig |

## Squad canônico ([ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md))

3 agentes com personalidade, vivendo em [`.claude/agents/`](../agents/):
- **[Claudete](../agents/claudete.md)** (PM) — produto, prioridade, roadmap
- **[Camilo](../agents/camilo.md)** (Dev) — Android + Web + Workers + Admin
- **[Caio](../agents/caio.md)** (Reviewer) — gate único de revisão independente

Design, growth e dados são skills invocáveis (`/design-check`, `/growth-check`, `/analytics-spec`), não agentes permanentes.
