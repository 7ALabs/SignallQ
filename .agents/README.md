# Agentes e skills do SignallQ

A governança vive em [`AGENTS.md`](../AGENTS.md). Este diretório contém procedimentos e artefatos de trabalho; os perfis nativos do Codex vivem em [`.codex/agents/`](../.codex/agents/).

## Equipe

- **Cora** — Product Lead e persona principal da sessão.
- **Davi** — Android Engineer.
- **Ramon** — Diagnostic Systems Engineer.
- **Breno** — QA & Reliability.
- **Camillo** — Principal Engineer / System Architect transversal.

O Codex principal integra o trabalho. Não simule conversa entre personagens nem declare revisão que não ocorreu.

## Estrutura

```text
AGENTS.md                  governança do produto
.codex/agents/*.toml       especialistas delegáveis
.agents/WORKFLOW.md        fluxo operacional
.agents/architecture-plan.md plano sistêmico quando necessário
.agents/skills/            skills canônicas
.claude/skills/            espelho de compatibilidade Claude
.github/skills/            espelho de compatibilidade GitHub
```

## Skills

Skills são procedimentos. O nome do agente responsável não deve ser embutido como regra da skill; o roteamento vem do `AGENTS.md`.

### Produto e experiência

- `SignallQ-design` — referência/produção visual.
- `design-check` — checagem pontual contra Design System.
- `auditar-ux` — auditoria ampla de usabilidade e acessibilidade.
- `growth-check` — checklist de loja/ASO/superfície pública.
- `estimativa-impacto` — tamanho, risco e gatilhos de arquitetura.
- `analytics-spec` — especificação de telemetria.

### Android

- `regras-android` — APIs, permissões e particularidades Android.
- `padroes-compose` — Compose.
- `protocolo-ci-android` — pipeline Android.
- `protocolo-ktlint` — lint/formatação.

### Diagnóstico

- `motor-diagnostico` — arquitetura funcional do diagnóstico/speedtest.
- `regras-diagnostico-rede` — thresholds e regras técnicas.
- `reconhecimento-equipamento-rede` — equipamentos e identificação.

### Engenharia e operação

- `inventario` — localizar/reutilizar implementação existente.
- `verificar-modulo` — fronteiras e dependências de módulos.
- `cloudflare-d1-console` — operação D1 quando aplicável.
- `handoff` — passagem formal de contexto quando realmente necessária.
- `check-done` — evidência de conclusão.
- `checar-release` — release readiness.
- `gerar-docs` — documentação viva.
- `impeccable` — tooling de qualidade visual.

## Classificação da migração

Primeira passagem para Codex:

- **Mantidas:** todas as skills técnicas que continuam úteis.
- **Ajustadas:** `handoff`, `check-done`, `motor-diagnostico`, `design-check`, `auditar-ux`, `growth-check`, `analytics-spec`, `estimativa-impacto` e `checar-release`, removendo dependência de personas/modelos antigos.
- **Fundidas:** nenhuma nesta etapa; não há evidência suficiente de sobreposição que justifique apagar procedimento útil.
- **Aposentadas:** nenhuma skill por nome nesta etapa. Aposentadas são as regras que tratavam Claudete/Camilo/Caio como squad ativa e Haiku/Sonnet/Opus como política local.

## Fonte canônica e espelhos

`.agents/skills/` é a fonte canônica.

Depois de editar skill, execute:

```bash
./scripts/sync-skills-mirrors.sh
./scripts/sync-skills-mirrors.sh --check
```

Os espelhos não devem ganhar regras próprias.
