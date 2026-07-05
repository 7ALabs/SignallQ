# Pipeline Autônomo SIGNALLQ — Do `/task` ao Merge

## Visão Geral

O usuário digita `/task [descrição]` no Claude Code e não precisa fazer mais nada. O pipeline executa do intake ao merge com notificações no Slack e Discord a cada etapa.

```
Usuário: /task [descrição]
    ↓
Claudete: cria issue #N + comenta + move board
    ↓
Cláudio: cria branch + plano técnico + comenta
    ↓
Camilo: implementa em commits atômicos + comenta por subtask
    ↓       ↑ (loop de correção se Gema reprovar)
Gema: revisa + aprova ou reprova
    ↓
Nina: abre PR + aguarda CI + mergea + deleta branch + fecha issue
    ↓
Done: notificação verde no Slack + Discord
```

---

## Pré-requisitos

- `gh` CLI autenticado (`gh auth status`)
- Secret `PROJECT_PAT` configurado no repo (para o workflow `auto-move-board.yml`)
- Webhooks em `.env`: `DISCORD_WEBHOOK_LINKA`, `SLACK_WEBHOOK_LINKA` (ou via MCP)
- Labels existentes por categoria:
  - **Área:** `area:arquitetura`, `area:android`, `area:docs`, `area:scripts`, `area:design-system`, `area:seguranca`, `area:ux`, `area:qualidade`, `area:energia`, `area:performance`, `area:dados`
  - **Agente:** `agent:claudete`, `agent:camilo`, `agent:claudio`, `agent:gema`, `agent:lia`, `agent:marcelo`, `agent:nina`, `agent:taisa`
  - **Validação:** `needs:bernardo`
  - **Padrão GitHub:** `bug`, `enhancement`, `documentation`, `duplicate`, `good first issue`, `help wanted`, `invalid`, `question`, `wontfix`
  - **Nota:** labels `type:*` e `status:*` não existem neste repo. Usar labels `area:*` para classificar tipo de trabalho.

---

## Como iniciar

```
/task O app trava ao abrir diagnóstico com Wi-Fi desativado em Samsung Galaxy A33
```

Claudete classifica, cria issue, dispara pipeline. O usuário acompanha pelo GitHub Project, Slack ou Discord.

---

## Etapas e comandos de handoff

### Claudete → Cláudio (intake)

```bash
bash scripts/agent-handoff.sh claudete ready N "issue criada" --para claudio
```

Cria a issue, posta comentário de kickoff, move card para "Pronta para dev".

### Cláudio → Camilo (planejamento)

```bash
bash scripts/agent-handoff.sh claudio handoff N "branch criada, plano postado" --para camilo
```

Cria a branch, posta plano técnico, move card para "Em andamento".

### Camilo → Gema (implementação pronta)

```bash
bash scripts/agent-handoff.sh camilo review N "implementação pronta, build verde"
```

Move card para "Em review".

### Gema → Camilo (reprovado)

```bash
bash scripts/agent-handoff.sh gema block N "reprovado: [motivo]" --para camilo
```

Mantém coluna, aplica `status:blocked`, notifica em vermelho.

### Gema → Nina (aprovado)

```bash
bash scripts/agent-handoff.sh gema docs N "aprovado" --para nina
```

Move card para "Docs & Higiene".

### Nina → Done (merge e fechamento)

```bash
bash scripts/agent-handoff.sh nina done N "PR mergeado, branch deletada, issue fechada"
```

Move card para "Done", notifica verde nos dois canais.

---

## Convenção de nomes de branch

```
feature/47-filtro-dns
bug/23-crash-speedtest
refactor/31-extrair-usecase
infra/12-network-security-config
docs/8-atualizar-changelog
```

Formato: `[tipo]/[N_issue]-[slug-em-portugues-com-hifens]`

> Os prefixos de branch (`feature/`, `bug/`, `refactor/`, `infra/`, `docs/`) são convenções de nomenclatura, não labels do GitHub. As labels no repo usam o esquema `area:*` descrito nos pré-requisitos.

---

## Convenção de commits

Formato: `[tipo](módulo): descrição em português #N`

```
feat(featureDiagnostico): adicionar filtro DNS por categoria #47
fix(coreNetwork): corrigir NPE no speedtest em API 26 #23
refactor(featureSpeedtest): extrair lógica de medição para UseCase #31
chore(ci): atualizar action para ubuntu-latest #12
docs(changelog): registrar entrega v0.9.3 #8
```

---

## Consultas laterais (bloqueio técnico)

Qualquer agente pode acionar um especialista antes de avançar. Durante a consulta, posta bloqueio temporário:

```bash
bash scripts/agent-handoff.sh [agente] block N "aguardando validação de [especialista]: [pergunta]"
```

Após receber resposta, retoma com novo handoff apropriado.

**Especialistas disponíveis:**
- **Otávio** — validação de comportamento em device real, OEM quirks, APIs de sistema (Wi-Fi, permissões, background). Para tasks simples, Camilo pode usar a skill `/android-platform-rules` diretamente sem acionar Otávio.
- **Lia** — validação de UX/UI, estados visuais, microcopy, Material Design 3
- **Bernardo** — lógica de diagnóstico de rede, thresholds de sinal, CGNAT, GPON. Label `needs:bernardo` indica que a issue requer validação dele antes de implementar.
- **Marcelo** — busca em codebase (sempre acionar antes de Read/Grep em agentes Sonnet)
- **Cláudio** — planejamento técnico, breakdown de tasks (disponível como agente; parte do papel foi absorvido pela Claudete)

---

## Protocolo de bloqueio (vermelho)

Bloqueios param o fluxo completamente:

```bash
bash scripts/agent-handoff.sh [agente] block N "[motivo claro em uma frase]"
```

Isso:
1. Mantém o card na coluna atual
2. Aplica `status:blocked`
3. Notifica Discord + Slack em vermelho (warning)

O pipeline **não avança** até o usuário ou Claudete resolver. Para retomar, o agente responsável faz o handoff correto para o próximo estado.

---

## Fluxo de revisão Gema → Camilo (loop)

Pode ocorrer múltiplas vezes. A cada ciclo:

1. Gema posta `Gema: Reprovado. [problema específico].` + `block` → Camilo
2. Camilo corrige em novo(s) commit(s) atômicos na mesma branch
3. Camilo reenvia: `bash scripts/agent-handoff.sh camilo review N "corrigido: [o que mudou]"`
4. Gema revisa novamente

Sem limite de ciclos — mas Gema deve ser específica a cada reprovação.

---

## Merge pela Nina

Nina nunca força merge. Se CI falhar:

```bash
bash scripts/agent-handoff.sh nina block N "CI falhou: [erro]"
```

O usuário ou Camilo resolve o CI. Nina retoma quando CI ficar verde.

Nina usa `--squash` como padrão (um commit de merge limpo). O PR deve ter `Closes #N` no corpo para fechar a issue automaticamente.

---

## Board do GitHub Project

**Project #8** — SIGNALLQ Android Roadmap (`gmmattey/signallq-android`)

| Coluna | Quando |
|--------|--------|
| Backlog | Issue criada sem label de ready |
| Triagem | Needs refinement |
| Pronta para dev | `status:agent-ready` |
| Em andamento | `status:in-progress` |
| Em review | `status:waiting-review` |
| Docs & Higiene | Aprovado pela Gema |
| Done | Issue fechada |

O workflow `.github/workflows/auto-move-board.yml` move cards automaticamente como safety net.

---

## Debugging

**Board não moveu:** verificar se `PROJECT_PAT` está configurado no repo. Mover manualmente: `bash scripts/issue-move.sh N [coluna]`

**Notificação não chegou:** verificar `.env` para `DISCORD_WEBHOOK_LINKA` e `SLACK_WEBHOOK_LINKA`. Testar: `bash scripts/notify.sh teste "mensagem" info`

**PR não fechou a issue:** verificar se o corpo do PR contém `Closes #N`. Fechar manualmente: `gh issue close N --repo gmmattey/signallq-android`

**Agente não recebeu handoff:** o handoff é por subagente invocado explicitamente no Claude Code — verificar se o agente anterior chamou o próximo via `Task`. Retomar manualmente invocando o agente com o prompt correto.
