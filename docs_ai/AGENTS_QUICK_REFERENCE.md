# Guia Rápido — Agentes SignallQ

**Última atualização:** 2026-07-05 · v0.23.0

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`. Este guia é resumo apontador.

Referência rápida de quem é quem e quando acionar. Squad enxuto de 5 agentes.

---

## Squad Ativo

| Agente | Modelo | Cargo | Quando acionar |
|---|---|---|---|
| Claudete | Sonnet | PM & Tech Lead | Intake, prioridade, refino, task breakdown, arquitetura, WIP, decisão Done/Not Done |
| Camilo | Sonnet | Dev Android | Implementação Android: Kotlin, Compose, ViewModel, diagnóstico nativo, integração IA |
| Felipe | Sonnet | Admin Panel & Dados | Painel admin React/TS e análise de dados de app (Play Console, Firebase, custo de IA) |
| Lia | Sonnet/Haiku | UX & Design | UX/UI, Material 3, estados visuais, microcopy, acessibilidade |
| Gema | Haiku (Sonnet se pesado) | QA, Release & Higiene | Review, bugs, regressões, gate único de Done, release, higiene, changelog |

Papéis de arquitetura (Cláudio), docs/versão (Nina/Taisa) e busca/APIs (Marcelo/Otávio/Bernardo) foram **absorvidos** por agentes remanescentes ou viraram skills (`/regras-android`, `/regras-diagnostico-rede`, `/gerar-docs`). Busca de código = ferramentas nativas (Read/Grep/Glob).

---

## Fluxo (resumo)

```
Demanda
    ↓
Claudete: refina, roteia (bug → GitHub Issues; feature/task → Linear), quebra em tasks
    ↓
Lia: revisa ANTES só se a mudança é visual/de fluxo (senão pula)
    ↓
Camilo / Felipe / Lia: implementam em trilhas paralelas independentes
    ↓       ↑ (loop máx. 2 rodadas; 3ª escala p/ Claudete)
Gema: gate único de Done — review + QA + release + higiene
    ↓
Done: PR mergeado, issue fechada, changelog e versão atualizados
```

- WIP: máximo 1 task In Progress por agente.
- Handoff vive no Linear (status) + GitHub (PR); o Linear notifica o Slack direto. Scripts `agent-handoff.sh`/Discord estão depreciados.

---

## Responsabilidades por Agente

### Claudete — PM & Tech Lead
Refina feature bruta em user story com critérios de aceite; quebra em tasks pequenas e independentes; decide prioridade; controla WIP; planejamento técnico e decisão de arquitetura; decide Done/Not Done. **Acionar:** qualquer feature/mudança que precise de refino antes da implementação.

### Camilo — Dev Android
Implementa Android (Kotlin, Compose, ViewModel, StateFlow); refactors seguros; corrige bugs Android; integra IA; otimiza engines de diagnóstico. Skills: `/regras-android`, `/padroes-compose`, `/regras-diagnostico-rede`, `/motor-diagnostico`.

### Felipe — Admin Panel & Dados
Implementa e mantém o SignallQ Admin (React/TS/Vite/Tailwind); analisa dados de app (Play Console, Firebase Analytics/Crashlytics, retenção, crash rate, custo de IA); gera mocks realistas. Análise sempre com achado + contexto + implicação.

### Lia — UX & Design
Define e valida estados visuais, hierarquia, microcopy e acessibilidade conforme MD3 e design system (`/SignallQ-design`). **Obrigatória** quando: tela nova, estado visual novo, microcopy visível, mudança de fluxo. Dispensada em bug/lógica pura e `:core*` sem reflexo visual.

### Gema — QA, Release & Higiene
Gate único de Done: review de código, bugs, regressões, risco técnico, testes; higiene (versionamento em `libs.versions.toml`, CHANGELOG, docs); abre bug no **GitHub Issues** (`[BUG]`). Haiku por padrão, Sonnet em review técnico pesado. Skills: `/checar-entrega`, `/checar-release`, `/higiene`.

---

## Labels do GitHub

| Prefixo | Exemplos | Uso |
|---|---|---|
| `area:` | `area:android`, `area:admin`, `area:ux`, `area:qualidade` | Tipo de trabalho |
| `agent:` | `agent:camilo`, `agent:felipe`, `agent:lia`, `agent:gema` | Agente responsável |
| Padrão GitHub | `bug`, `enhancement`, `documentation` | Classificação geral |

---

## Roteamento

- **Bug** → GitHub Issues (formato `[BUG]`).
- **Feature / task / daily** → Linear (projeto SignallQ).
- Decisão que surge no Slack vira issue no Linear ou página no Notion — Slack é saída, não fonte da verdade.
