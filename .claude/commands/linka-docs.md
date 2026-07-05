---
description: Guardião da documentação LINKA — identifica quais docs atualizar após uma mudança, guia criação de novos documentos com nome/local corretos, e audita se a documentação está em dia com o código.
argument-hint: [impact <descrição da mudança>|update <tipo>|new <NomeDoc>|check <feature>]
allowed-tools: Read(*), Edit(*), Bash(*)
---

## Índice de Documentação Atual (lido dos arquivos agora)

**Índice oficial:**
!`cat "C:/Projetos/SignallQ Android/docs/IndiceDocumentacao.md" 2>/dev/null | head -80`

**Documentação consolidada (mapa de navegação):**
!`cat "C:/Projetos/SignallQ Android/DOCUMENTACAO_CONSOLIDADA.md" 2>/dev/null | head -60`

**Pendências técnicas abertas (top 20 linhas):**
!`cat "C:/Projetos/SignallQ Android/docs/PendenciasSanitizacaoCodigo.md" 2>/dev/null | head -20`

---

## Sistema de Documentação LINKA

### Mapa de Documentos e Responsabilidades

| Documento | Caminho | Atualizar quando... |
|-----------|---------|---------------------|
| `DocumentacaoFuncionalSistema.md` | `docs/` | Tela, fluxo, campo, ação, mensagem, regra de negócio, validação mudou |
| `DocumentacaoTecnicaSistema.md` | `docs/` | Serviço, modelo, rota, integração, contrato, dependência, config técnica mudou |
| `GuiaReleaseBuild.md` | `docs/` | Processo de build, versionamento, assinatura, config Android mudou |
| `GuiaVersioning.md` | `docs/` | Política SemVer mudou ou nova versão foi criada |
| `PendenciasSanitizacaoCodigo.md` | `docs/` | Novo débito técnico identificado; débito existente resolvido |
| `IndiceDocumentacao.md` | `docs/` | Novo doc oficial criado ou doc existente removido/renomeado |
| `arquiteturaAndroidKotlin.md` | `docs/` | Novo módulo adicionado, módulo removido, decisão arquitetural tomada |
| `GuiaOrganizacaoPastas.md` | `docs/` | Nova pasta criada, pasta renomeada, nova categoria de arquivo |
| `tarefasMigracaoKotlin.md` | `docs/` | Item de migração Flutter→Kotlin concluído ou adicionado |
| `DOCUMENTACAO_CONSOLIDADA.md` | raiz | Novo doc central adicionado; sequência de leitura mudou |
| `CLAUDE.md` | raiz | Nova skill adicionada; referência rápida mudou |
| `_AGENTS_KOTLIN.md` | raiz | Novas regras de comportamento de agente; novos gatilhos de skill |
| `docs/branding/linka_branding_guidelines.md` | `docs/branding/` | Design system mudou (cores, tipografia, componentes) |

### Regra de Impacto — Matriz de Mudança

| Tipo de mudança | Docs obrigatórios | Docs opcionais |
|-----------------|-------------------|----------------|
| Nova tela Compose | `DocumentacaoFuncionalSistema.md` (wireframe ASCII + fluxo) | `DocumentacaoTecnicaSistema.md` |
| Tela existente alterada | `DocumentacaoFuncionalSistema.md` (atualizar wireframe) | — |
| Novo serviço / repositório | `DocumentacaoTecnicaSistema.md` | `arquiteturaAndroidKotlin.md` |
| Novo módulo Gradle | `arquiteturaAndroidKotlin.md` + `settings.gradle.kts` comentado | `GuiaOrganizacaoPastas.md` |
| Nova dependência | `DocumentacaoTecnicaSistema.md` (seção dependências) | — |
| Mudança de fluxo/navegação | `DocumentacaoFuncionalSistema.md` | `DocumentacaoTecnicaSistema.md` |
| Bug fix sem impacto de UI | Apenas se regra de negócio mudou | `PendenciasSanitizacaoCodigo.md` (remover débito) |
| Migração Flutter→Kotlin concluída | `tarefasMigracaoKotlin.md` + `ComparativoFlutterKotlin.md` | — |
| Build/release process mudou | `GuiaReleaseBuild.md` | `GuiaVersioning.md` |
| Novo débito técnico | `PendenciasSanitizacaoCodigo.md` | — |

### Padrão de Wireframe ASCII (para DocumentacaoFuncionalSistema.md)

```
┌─────────────────────────────────┐
│ ← SignallQ               [⚙️]     │  ← TopAppBar
├─────────────────────────────────┤
│                                 │
│  [ Título da Seção ]            │  ← sectionTitle (20sp/600)
│                                 │
│  ┌─────────────────────────┐   │
│  │  Métrica Principal      │   │  ← Card com bgCard
│  │  ████████  42 Mbps      │   │
│  └─────────────────────────┘   │
│                                 │
│  [ Seção Secundária ]           │
│  ┌──────────┐ ┌──────────┐    │
│  │ Card 1   │ │ Card 2   │    │
│  └──────────┘ └──────────┘    │
│                                 │
└─────────────────────────────────┘
```

### Padrão de Nomenclatura de Documentos

**Obrigatório:**
- Português-BR
- PascalCase: `DocumentacaoFuncionalSistema.md`, `GuiaReleaseBuild.md`
- Sem hifens, underscores ou espaços no nome do arquivo
- Sufixo descritivo: `Guia*`, `Documentacao*`, `Manifesto*`, `Plano*`, `Indice*`

**Proibido:**
- `README.md` em subpastas (sem necessidade clara)
- Arquivos soltos na raiz do repositório
- Nomes em inglês para docs do domínio (exceto contratos com parceiros externos)
- Duplicar conteúdo de doc existente

### Localização Correta por Tipo de Documento

| Tipo | Pasta | Exemplo |
|------|-------|---------|
| Documentação oficial | `docs/` | `DocumentacaoFuncionalSistema.md` |
| Contratos cross-client | `docs/contratos/` | `SpeedTestEspecificacao.md` |
| Documentação técnica de modems | `docs/tecnicos/<modem>/` | `nokia-gpon/MapeamentoCGI.md` |
| Branding e design | `docs/branding/` | `linka_branding_guidelines.md` |
| Evidências (prints, logs) | `evidencias/` (gitignored) | `2026-05-11-captura-nokia.png` |
| Scripts de automação | `scripts/<categoria>/` | `scripts/build/buildReleaseKotlin.ps1` |
| Secrets e keystores | `segredos/` (gitignored) | `SignallQ.jks` |
| Temporários | `tmp/` (gitignored) | — |

**NUNCA criar arquivo em:** raiz do repo (exceto `CLAUDE.md`, `AGENTS.md`, `GEMINI.md`, `ANTIGRAVITY.md`, `_AGENTS_KOTLIN.md`, `DOCUMENTACAO_CONSOLIDADA.md`), dentro de `lib/` ou `linka-android-kotlin/` (sem ser código).

### Documentação Imutável (não tocar)

- `source/devices/compativeis/**` — documentação de fornecedor (Nokia, TP-Link, Huawei, ZTE, Sagemcom, Kaon, Humax, Intelbras)
- `source/app/assets/modems/**` — assets de modem
- Nunca renomear, mover, editar ou deletar esses arquivos

---

## Sua Tarefa

**Argumento recebido:** $ARGUMENTS

### Modo `impact <descrição da mudança>`

Dado o que foi implementado ou alterado, identifique:

1. **Quais documentos PRECISAM ser atualizados** (lista com prioridade)
2. **O que exatamente mudar** em cada documento (seção, conteúdo novo, wireframe ASCII se tela)
3. **Quais documentos PODEM ser impactados** (lista com justificativa)
4. **Se há novos débitos** que devem ir para `PendenciasSanitizacaoCodigo.md`

Apresente o mapeamento antes de qualquer edição. Pergunte se quer que as atualizações sejam feitas automaticamente.

### Modo `update <tipo>`

**Tipos:** `funcional`, `tecnico`, `release`, `pendencias`, `arquitetura`, `indice`

1. Leia o documento correspondente
2. Pergunte o que mudou (se não informado)
3. Gere o conteúdo novo seguindo o padrão do documento existente (mesma estrutura, pt-BR, sem quebrar seções existentes)
4. Mostre o diff antes de aplicar
5. Aplique com confirmação do usuário

### Modo `new <NomeDoc>`

1. Valide o nome contra os padrões (PascalCase, pt-BR, sem hifens, sufixo correto)
2. Determine a pasta correta baseado no tipo de conteúdo
3. Verifique se já existe documento com mesmo propósito
4. Gere o documento com estrutura inicial padrão
5. Adicione entrada em `IndiceDocumentacao.md`
6. Adicione referência em `DOCUMENTACAO_CONSOLIDADA.md` se for documento central

### Modo `check <feature>`

Dado o nome de uma feature ou módulo, audite se a documentação está em dia:

1. Leia `DocumentacaoFuncionalSistema.md` para encontrar a seção da feature
2. Leia `DocumentacaoTecnicaSistema.md` para encontrar os serviços da feature
3. Compare com o código atual em `linka-android-kotlin/feature<Nome>/`
4. Identifique divergências: telas que existem no código mas não na doc, ou vice-versa
5. Gere relatório de lacunas com sugestão de atualização

### Sem argumento — modo consultor

Pergunte ao usuário:
- Acabou de implementar algo e quer saber quais docs atualizar?
- Quer criar um novo documento?
- Quer auditar a documentação de uma feature?
- Tem dúvida sobre onde um documento deve ficar?
