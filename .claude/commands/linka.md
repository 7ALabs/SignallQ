---
description: Kickoff de sessão LINKA — orienta Claude sobre o estado atual do projeto (versão, pendências, fase, skills disponíveis). Use no início de qualquer sessão nova.
allowed-tools: Read(*)
---

## Estado Atual do Projeto (lido dos arquivos agora)

**Versão Kotlin (ativa):**
!`cat "E:/Projetos/Linka/linkaAndroidKotlin/gradle/libs.versions.toml" 2>/dev/null | grep -E "versionName|versionCode"`

**Último release:**
!`cat "E:/Projetos/Linka/linkaAndroidKotlin/apk/releases/CHANGELOG.md" 2>/dev/null | head -20`

**Pendências abertas (top 15 linhas):**
!`cat "E:/Projetos/Linka/linkaAndroidKotlin/docs/PendenciasSanitizacaoCodigo.md" 2>/dev/null | head -15`

**Fase atual do plano:**
!`cat "E:/Projetos/Linka/linkaAndroidKotlin/docs/LINKA — Plano Faseado por Custo-Benefício.md" 2>/dev/null | head -30`

---

## Sua Tarefa

Com base nas informações acima, apresente ao usuário um briefing de sessão conciso:

1. **Versão atual** — versionName + versionCode
2. **Último release** — data e o que foi entregue (do CHANGELOG)
3. **Top 3 pendências abertas** — as mais críticas de PendenciasSanitizacaoCodigo.md
4. **Fase atual** — em qual fase do plano o projeto está

Em seguida, pergunte: **"Em que vamos trabalhar hoje?"**

---

## Documentação Cross-Platform (workspace `E:\Projetos\Linka\docs\`)

Documentação que cobre **Android e PWA** simultaneamente. Leia antes de tomar decisões que impactam comportamento compartilhado (thresholds, diagnóstico, design system).

| Documento | Quando ler |
|---|---|
| [`../docs/FUNCIONAL_CROSSPLATFORM.md`](../docs/FUNCIONAL_CROSSPLATFORM.md) | Feature nova, mudança de comportamento, paridade Android ↔ PWA |
| [`../docs/TECNICO_CROSSPLATFORM.md`](../docs/TECNICO_CROSSPLATFORM.md) | Arquitetura, módulos, fluxos técnicos, APIs externas compartilhadas |
| [`../docs/DESIGN_SYSTEM_CROSSPLATFORM.md`](../docs/DESIGN_SYSTEM_CROSSPLATFORM.md) | Tokens, componentes, estados visuais, paridade visual |
| [`../docs/MATERIAL_DESIGN_3.md`](../docs/MATERIAL_DESIGN_3.md) | Cores, tipografia, espaçamento, componentes MD3 em uso |
| [`../docs/PADROES_UI_UX.md`](../docs/PADROES_UI_UX.md) | Princípios iOS-Calma, nomenclatura de métricas, estados visuais |
| [`../docs/VISAO_GERAL_LINKA.md`](../docs/VISAO_GERAL_LINKA.md) | Visão geral do produto, os dois projetos, onde encontrar cada informação |
| [`../docs/GUIA_DESENVOLVIMENTO_IA.md`](../docs/GUIA_DESENVOLVIMENTO_IA.md) | Como trabalhar em código com IA: sequência de leitura, classificação de task |

---

## Skills Disponíveis (para referência sua — não exibir ao usuário)

| Skill | Quando usar automaticamente |
|-------|-----------------------------|
| `/linka` | Início de sessão nova sem contexto anterior |
| `/linka-design create` | Criar nova tela ou componente Compose |
| `/linka-design review` | Editar arquivo em `ui/screen/` ou `ui/component/` |
| `/linka-design tokens` | Dúvida sobre cor, espaçamento ou tipografia |
| `/linka-arch create` | Criar módulo, ViewModel, DAO, serviço ou repositório |
| `/linka-arch review` | Revisão arquitetural de arquivo Kotlin |
| `/linka-arch map` | Dúvida sobre onde implementar algo |
| `/linka-docs impact` | **Após qualquer mudança de código** — sempre |
| `/linka-docs update` | Atualizar doc específico |
| `/linka-docs new` | Criar novo documento oficial |
| `/linka-docs check` | Auditar docs de uma feature |
| `/linka-build` | Usuário pede build, APK, compilar |
| `/linka-version` | Usuário menciona versão, bump, semver |
| `/linka-release` | Usuário quer release completo ou publicar |
