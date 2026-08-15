---
name: design-check
description: Valida um arquivo .kt ou tela contra o design system SignallQ — tokens (LkColors/LocalLkTokens, LkSpacing), MaterialTheme.typography, contraste WCAG e hierarquia visual. Absorve a checagem pontual de design que antes seria pedida à Juliana (agora skill, não agente permanente — ADR-016). Camilo roda durante implementação; Caio roda no gate antes de aprovar PR com mudança visual.
argument-hint: "<caminho/Arquivo.kt | nome-da-tela>"
allowed-tools: Bash(grep *), Bash(find *), Read
---

## Quando usar

- Camilo, durante a implementação de um Composable novo ou alterado, antes de abrir a PR.
- Caio, no gate de revisão, quando a PR toca `ui/component/` ou `ui/screen/`.
- Claudete, quando quer confirmar que uma tela pronta está aderente ao design system antes de
  aprovar o critério de aceite visual.

**Não usar para:** gerar UI nova do zero (isso é `/SignallQ-design`), auditoria multi-tela
profunda de usabilidade/arquitetura de informação (isso é `/auditar-ux`), ou revisão automática
de todo diff em Edit/Write (isso já é o hook `impeccable` — ver seção "Overlap" abaixo).

## Escopo

Um arquivo `.kt` ou uma tela específica por invocação — não roda no repositório inteiro. Para
achado repetido em várias telas (ex.: dp hardcoded disperso), ver regra de higiene §4.11: não
vira tarefa desta skill, vira correção oportunista arquivo a arquivo.

## Checks

### 1. Cor hardcoded em vez de token

```bash
ALVO="$1"
grep -noE 'Color\(0x[0-9A-Fa-f]{6,8}\)' "$ALVO"
```

Cada ocorrência: é cor de marca de terceiro (operadora, WhatsApp) ou gráfico técnico com paleta
própria justificada (permitido, ver `docs_ai/DESIGN_SYSTEM.md` seção 2, "Regras de uso")? Se não,
`FAIL` — deveria usar `LkColors`/`LocalLkTokens.current`/`MaterialTheme.colorScheme`.

### 2. Espaçamento mágico em vez de `LkSpacing`

```bash
grep -noE '\.(padding|size|width|height|offset)\([^)]*[0-9]+\.dp' "$ALVO" \
  | grep -v 'LkSpacing\.'
```

Cada ocorrência sem `LkSpacing.*` é candidato a `WARN` (ver regra §4.11 — volume disperso não é
bug único, mas arquivo tocado deve trocar o `.dp` literal local por token).

### 3. Tipografia fora do token

```bash
grep -noE 'fontSize\s*=\s*[0-9]+\.sp|TextStyle\(' "$ALVO"
```

Qualquer ocorrência fora de `MaterialTheme.typography.*` é `FAIL` — auditoria de 2026-07-26
(regra §4.11) confirmou tipografia limpa no app; não regredir.

### 4. Contraste WCAG mínimo

Grep não calcula contraste — a skill lista as combinações fundo/texto encontradas (cor de
superfície do Composable pai + cor de texto do filho) e aponta para a tabela de hex em
`docs_ai/DESIGN_SYSTEM.md` (Apêndice A) para checagem manual contra WCAG AA (4.5:1 texto normal,
3:1 texto grande). Reporta como `WARN — validar manualmente` quando não consegue confirmar
automaticamente; nunca declara `PASS` de contraste sem confirmação.

### 5. Hierarquia visual

Heurística simples: lista todos os `MaterialTheme.typography.*` usados no arquivo. Se a tela tem
mais de 3 blocos de texto visíveis e usa o mesmo estilo em todos, `WARN — hierarquia plana,
revisar se todo texto deveria ter o mesmo peso`.

## Saída padrão

```
=== /design-check — HomeScreen.kt ===
1. Cor hardcoded:       PASS (nenhuma ocorrência fora de exceção documentada)
2. Espaçamento mágico:  WARN — 3 ocorrências de .dp literal (linhas 142, 210, 340)
3. Tipografia:          PASS
4. Contraste WCAG:      WARN — validar manualmente: texto onSurfaceVariant sobre surfaceContainerHigh
5. Hierarquia visual:   PASS

Resultado: WARN — 2 itens para revisar antes do gate do Caio.
```

## Overlap com `impeccable` e `auditar-ux` (documentado, não é sobreposição real)

- **`SignallQ-design`**: gera UI SignallQ nova (create/prototype) — ponto de partida.
- **`impeccable`**: hook automático que roda em todo Edit/Write/MultiEdit — flag issues no diff,
  sem precisar ser chamado.
- **`design-check`** (esta skill): checagem pontual e explícita contra os tokens documentados,
  sob demanda de Camilo ou Caio, com saída PASS/WARN/FAIL citável em PR/handoff.
- **`auditar-ux`**: auditoria manual profunda (tokens MD3 + usabilidade + arquitetura de
  informação), sob demanda de Claudete/Caio, escopo maior que um arquivo.

Entrada, momento de invocação e profundidade diferem — não consolidar.

## O que a skill NÃO faz

- Não corrige o arquivo — só reporta.
- Não decide se um `WARN` bloqueia o merge — isso é o Caio no gate.
- Não substitui `impeccable` (automático) nem `auditar-ux` (profundo).

## Referências

- [`docs_ai/DESIGN_SYSTEM.md`](../../../docs_ai/DESIGN_SYSTEM.md) — fonte de verdade dos tokens.
- Skill peer: [`/SignallQ-design`](../SignallQ-design/SKILL.md) — geração.
- Skill peer: [`/auditar-ux`](../auditar-ux/SKILL.md) — auditoria profunda.
- [Regra de higiene §4.11](../../rules/higiene-e-padronizacao-repositorio.md) — espaçamento hardcoded.
- Persona: [Camilo](../../agents/camilo.md), [Caio](../../agents/caio.md), [Claudete](../../agents/claudete.md)
