---
description: Guardião da documentação SignallQ — identifica quais docs atualizar após uma mudança, guia criação de novos documentos com nome/local corretos, e audita se a documentação está em dia com o código.
argument-hint: [impact <descrição da mudança>|update <tipo>|new <NomeDoc>|check <feature>]
allowed-tools: Read(*), Edit(*), Bash(*)
---

## Índice de documentação atual (lido em tempo real)

**Índice oficial:**
!`head -80 "${CLAUDE_PROJECT_DIR:-.}/docs_ai/INDICE.md" 2>/dev/null`

**Ponto de entrada:**
!`head -60 "${CLAUDE_PROJECT_DIR:-.}/docs_ai/README.md" 2>/dev/null`

---

## Sistema de documentação SignallQ

Fonte da verdade completa da política: [`.claude/rules/politica-documentacao-viva.md`](../rules/politica-documentacao-viva.md). Guardrail executável: [`scripts/validar-docs.sh`](../../scripts/validar-docs.sh) (rodado por `docs-ci` em toda PR).

### Mapa de documentos e responsabilidades

| Documento | Caminho | Atualizar quando... |
|---|---|---|
| `FUNCIONAL.md` | `docs_ai/` | Tela, fluxo, campo, ação, mensagem, regra de negócio, validação mudou |
| `TECNICO.md` | `docs_ai/` | Serviço, modelo, rota, integração, contrato, dependência, config técnica mudou |
| `DESIGN_SYSTEM.md` | `docs_ai/` | Tokens, tipografia, cor, componente MD3 mudaram |
| `ARQUITETURA/README.md` + `ARQUITETURA/MODULOS/*.md` | `docs_ai/` | Novo módulo, decisão arquitetural, mudança de dependências entre módulos |
| `CONTRATOS/openapi/*.yaml` | `docs_ai/CONTRATOS/openapi/` | Endpoint, request/response, versão de API mudaram |
| `RELEASES.md` | `docs_ai/` | Nova versão publicada, milestone alterado |
| `decisions/ADR-NNN-*.md` | `docs_ai/decisions/` | Nova decisão arquitetural imutável |
| `operations/*.md` | `docs_ai/operations/` | Novo runbook, mudança de processo de release, incidente |
| `technical/*.md` | `docs_ai/technical/` | Specs pontuais não cobertas por `TECNICO.md` |
| `INDICE.md` | `docs_ai/` | Novo doc oficial criado, pasta nova, contagem mudou |
| `AGENTS.md` | raiz do repo | Contrato de agentes, comandos essenciais, arquitetura comprovada mudou |
| `.claude/rules/*.md` | `.claude/rules/` | Regra operacional obrigatória mudou |
| `brand/` | raiz do repo | Assets de marca mudaram |

### Regra de impacto — matriz de mudança

| Tipo de mudança | Docs obrigatórios | Docs opcionais |
|---|---|---|
| Nova tela Compose | `FUNCIONAL.md` (fluxo + wireframe) | `TECNICO.md` |
| Tela existente alterada | `FUNCIONAL.md` (atualizar fluxo) | — |
| Novo serviço/repositório | `TECNICO.md` | `ARQUITETURA/MODULOS/<modulo>.md` |
| Novo módulo Gradle | `ARQUITETURA/README.md` + `ARQUITETURA/MODULOS/<modulo>.md` + `settings.gradle.kts` comentado | `TECNICO.md` |
| Nova dependência | `TECNICO.md` (seção dependências) — inventário regenera via `scripts/gerar-inventario-docs.sh` | — |
| Mudança de fluxo/navegação | `FUNCIONAL.md` | `TECNICO.md` |
| Novo Worker Cloudflare | `TECNICO.md` + `CONTRATOS/openapi/<worker>.yaml` | `ARQUITETURA/README.md` |
| Mudança de schema D1 | `CONTRATOS/schemas/README.md` + skill `/cloudflare-d1-console` | — |
| Bugfix sem impacto de UI | Apenas se regra de negócio mudou | — |
| Build/release process | `operations/RELEASE.md` ou runbook aplicável | — |
| Decisão arquitetural nova | `decisions/ADR-NNN-<slug>.md` (próximo número livre em `INDICE.md`) + `INDICE.md` | — |

### Frontmatter obrigatório (docs_ai/)

Todo documento vivo em `docs_ai/` (exceto `templates/`, `decisions/`, `pro-onhold/`) precisa de:

```yaml
---
title: "..."
description: "..."
type: "técnico | funcional | adr | runbook"
status: "ativo | draft | congelado | deprecated"
owner: "[Nome do agente/pessoa]"
last_updated: "YYYY-MM-DD"
version: "X.Y.Z"
---
```

`scripts/validar-docs.sh` reprova PR que altera `.md` em `docs_ai/` sem esses campos. Ver [política de docs viva §2](../rules/politica-documentacao-viva.md).

### Padrão de nomenclatura de documentos

- Português-BR.
- Documentos novos em `docs_ai/`: `kebab-case`, salvo convenções (`README.md`, `INDICE.md`, `ADR-NNN-*.md`).
- Documentos existentes só são renomeados se todos os links e consumidores forem atualizados na mesma mudança.

### Localização — o que o `docs-ci` reprova

Ver [`.claude/rules/politica-documentacao-viva.md` §0](../rules/politica-documentacao-viva.md) para a lista executável. Resumo:

- `.md` fora da árvore permitida (`docs_ai/`, `.claude/`, `.github/`, `android/`, `integrations/`, `scripts/`, `brand/`, `docs/`).
- Pasta nova em `docs_ai/` não citada em `INDICE.md` nem `README.md`.
- `.md` na raiz de `docs_ai/` sem citação nominal no índice.
- Contagem declarada no `INDICE.md` divergente do disco.
- Espelhos de skill (`.agents/skills/`, `.github/skills/`) fora de sincronia com `.claude/skills/`.

### Sem pasta de arquivo

Documento substituído é **removido** (o git é o arquivo), não movido para `_archive/`. Registrar substituição no doc que substituiu e citar o SHA anterior no commit. Ver [regra de higiene §10 "Remoção"](../rules/higiene-e-padronizacao-repositorio.md).

Exceção: produto pausado (`docs_ai/pro-onhold/`) fica no lugar com README de selagem.

---

## Sua tarefa

**Argumento recebido:** $ARGUMENTS

### Modo `impact <descrição da mudança>`

Dado o que foi implementado ou alterado:

1. **Docs obrigatórios** — lista com prioridade.
2. **O que exatamente mudar** — seção, conteúdo novo, wireframe se tela nova.
3. **Docs opcionais** — lista com justificativa.
4. **Frontmatter** — verificar se os docs afetados têm todos os campos obrigatórios; sinalizar faltas.
5. **Inventário** — se a mudança afeta versão, módulo, Worker, tabela D1, contrato ou dependência: rodar `bash scripts/gerar-inventario-docs.sh` após implementar.

Apresente o mapeamento antes de qualquer edição. Pergunte se quer que as atualizações sejam feitas automaticamente.

### Modo `update <tipo>`

Tipos: `funcional`, `tecnico`, `design`, `arquitetura`, `contratos`, `operations`, `indice`, `adr`.

1. Leia o documento correspondente.
2. Pergunte o que mudou (se não informado).
3. Gere o conteúdo novo seguindo a estrutura existente (pt-BR, sem quebrar seções).
4. Mostre o diff antes de aplicar.
5. Aplique com confirmação do usuário. Atualize `last_updated` no frontmatter.

### Modo `new <NomeDoc>`

1. Valide nome (kebab-case, pt-BR, sem hifens em convenções).
2. Determine a pasta correta baseado no tipo.
3. Verifique se já existe documento com mesmo propósito.
4. Gere o documento com frontmatter obrigatório e estrutura inicial padrão.
5. Adicione entrada em `INDICE.md` e/ou `README.md`.
6. Rode `bash scripts/validar-docs.sh --base HEAD` antes de commitar.

### Modo `check <feature>`

Dado o nome de uma feature ou módulo, audite se a documentação está em dia:

1. Leia `docs_ai/FUNCIONAL.md` para achar a seção da feature.
2. Leia `docs_ai/TECNICO.md` e `docs_ai/ARQUITETURA/MODULOS/` para os serviços.
3. Compare com o código atual em `android/feature/<nome>/` ou `android/core/<nome>/`.
4. Identifique divergências: telas/serviços que existem no código mas não na doc, ou vice-versa.
5. Gere relatório de lacunas com sugestão de atualização.

### Sem argumento — modo consultor

Pergunte ao usuário:
- Acabou de implementar algo e quer saber quais docs atualizar?
- Quer criar um novo documento?
- Quer auditar a documentação de uma feature?
- Tem dúvida sobre onde um documento deve ficar?

## Agentes canônicos ([ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md))

Documentação é conduzida por **Claudete** (funcional/produto), **Camilo** (técnica/arquitetura), **Juliana** (design/UX), **Gustavo** (operações/dados) e **Caio** (revisão independente + prontidão de release).
