---
description: Agente autônomo de observabilidade — analisa diff do PR atual, identifica problemas e melhorias em arquivos adjacentes, e toma ação (auto-fix trivial ou abre issue no GitHub). Chamado por Gema após review aprovado. Posta no Slack apenas quando abre issue ou corrige algo.
---

## Quando usar

Gema chama esta skill ao final de TODA review aprovada, antes de encerrar sua etapa.
Também pode ser invocada manualmente: `/observe-and-act`

## O que este agente faz

1. Lê o diff do PR atual (arquivos alterados vs main)
2. Lê 2–3 arquivos adjacentes **fora do escopo da task** (mesmo módulo ou tela relacionada)
3. Classifica cada achado: **AUTO-FIX** | **ISSUE** | **IGNORAR**
4. Age de forma autônoma — sem perguntar, sem pausar

## Passos

### 1. Obter contexto do PR atual

```bash
# Branch atual
BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Arquivos alterados
git diff --name-only $(git merge-base HEAD origin/main) HEAD

# Diff completo (limitar a 300 linhas)
git diff $(git merge-base HEAD origin/main) HEAD -- '*.kt' | head -300
```

### 2. Ler arquivos adjacentes (fora do escopo)

Identificar 2–3 arquivos do mesmo módulo ou tela que **não** foram modificados.
Ler apenas as seções relevantes (não o arquivo inteiro).

### 3. Classificar achados

| Tipo | Critério | Ação |
|---|---|---|
| String técnica em Text() | .name/.toString()/.message direto na UI | AUTO-FIX |
| Import unused | IntelliJ warning óbvio | AUTO-FIX |
| TODO/FIXME sem issue | Comentário sem referência | ISSUE (tech-debt) |
| Cor hardcoded sem token | Color(0x...), Color.White etc | ISSUE |
| UX inconsistente | Estado vazio sem mensagem, botão sem feedback | ISSUE |
| Padrão arquitetural errado | Lógica de negócio no Composable | ISSUE |
| Algo ambíguo | Qualquer coisa que exija decisão de produto | IGNORAR |

### 4. AUTO-FIX (apenas para casos de baixo risco)

Condições para auto-fix:
- Mudança < 5 linhas
- Zero risco de regressão
- Não altera comportamento — apenas corrige string ou remove import

Após fix:
```bash
git add <arquivo>
git commit -m "fix(observe): <descrição curta> [auto-fix]"
```

**Nunca auto-fix:** lógica de negócio, fluxo de navegação, chamadas de API, layouts complexos.

### 5. ISSUE — abrir no GitHub

```bash
gh issue create \
  --title "[TIPO] <descrição curta> — detectado por observe-and-act" \
  --label "type:bug" \  # ou type:feature, type:tech-debt
  --body "## Detectado por observe-and-act

Arquivo: \`<caminho>\`
Contexto: PR #<N> — <título da issue atual>

<descrição do problema>

## Por que é um problema
<explicação concisa>

## Critérios de aceite
- [ ] <critério 1>
- [ ] <critério 2>

---
*Aberto automaticamente pelo agente de observabilidade após review do PR.*"
```

**Antes de abrir:** verificar se já existe issue similar:
```bash
gh issue list --state open --search "<palavras-chave>" --limit 3
```
Se existir → não duplicar, apenas comentar na issue existente.

### 6. Registrar observação

Salvar log em `.claude/agent-observations/YYYY-MM/obs-YYYY-MM-DD-<branch>.md`:

```markdown
# Observe-and-Act — <data> — <branch>

## Arquivos analisados
- <lista de arquivos do diff>
- <lista de arquivos adjacentes lidos>

## Achados

### AUTO-FIX: <descrição>
Arquivo: <caminho>:<linha>
Ação: <o que foi corrigido>
Commit: <hash>

### ISSUE aberta: #<N>
Título: <título>
Motivo: <por quê foi aberta>

### IGNORADO: <descrição>
Motivo: <por quê foi ignorado>

## Resumo
- Auto-fixes: N
- Issues abertas: N
- Ignorados: N
```

### 7. Slack — apenas se agiu

Se abriu issue OU fez auto-fix, usar MCP `mcp__claude_ai_Slack__slack_send_message`:

```
channel_id: C0B4NSGSK1D
message: 🔍 *observe-and-act* detectou e agiu:
• Issues abertas: N (#X, #Y)
• Auto-fixes: N
• PR analisado: feat/<N>-<slug>
```

Se não encontrou nada → **não postar no Slack** (não gerar ruído).

## Limites — o que este agente NUNCA faz

- Não altera arquivos de teste sem issue aprovada
- Não refatora código funcional (apenas corrige violações óbvias)
- Não abre mais de 3 issues por execução (priorizar as mais críticas)
- Não posta no Slack quando não há nada a reportar
- Não duplica issues existentes

## Integração no pipeline

Gema chama esta skill **após** postar o veredicto APROVADO e **antes** de passar para Nina.

Exemplo de uso por Gema:
> "Review aprovada. Executando observe-and-act nos adjacentes antes de passar para Nina."
