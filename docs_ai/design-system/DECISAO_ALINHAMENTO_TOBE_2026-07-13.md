# Decisão: alinhamento da documentação de design ao Fluxo de Telas To-Be (correção de contradição)

**Data:** 2026-07-13
**Responsável:** Lia (conteúdo da skill, tokens, docs)

## O que aconteceu

Uma auditoria (`docs_ai/operations/AUDITORIA_DESIGN_TOBE_2026-07-13.md`) comparou o app Android
contra o documento de referência "SignallQ App - Fluxo de Telas.dc.html", do projeto Claude Design
"SignallQ Design System" (`e77ea465-291f-4bf5-930c-a267680da04e`). Esse documento To-Be usa uma
paleta MD3 **diferente** da que estava documentada em `.claude/skills/SignallQ-design/` e no
`.claude/CLAUDE.md` até esta data — que foram extraídos em 2026-07-11 de **outro** documento do
**mesmo projeto Claude Design** ("Especificação: Migração para Material Design 3 (estrito)",
`templates/md3-migration-spec/Md3MigrationSpec.dc.html`), com `primary=#6C2BFF`.

O Fluxo de Telas (mais recente — é o documento que orientou a implementação real da Fase 1/Fase 2
de bottom sheets do gêmeo digital React em `packages/design-system/`) usa `primary=#5B21D6`. Isso é
uma **contradição real dentro do mesmo projeto de design**, não um erro de extração: os dois
documentos do mesmo projeto Claude Design especificam paletas MD3 diferentes.

## Decisão

Adotar o **Fluxo de Telas como fonte de verdade atual**, por ser o documento mais recente do
projeto e o que já orientou trabalho de implementação real (bottom sheets do gêmeo digital React).
O manual de migração MD3 estrito de 2026-07-11 fica como registro histórico do que foi decidido
naquela data, mas seus valores de paleta/forma deixam de valer a partir desta correção.

## O que mudou (principais valores)

| Token/sistema | Antes (manual MD3, 2026-07-11) | Depois (Fluxo de Telas, 2026-07-13) |
|---|---|---|
| `primary` | `#6C2BFF` | `#5B21D6` |
| `secondary` | `#9284A8` (derivado da tríade HCT do primary) | `#2851B8` (azul **fixo**, não deriva mais do primary) |
| `tertiary` | `#B03A5B` (tríade HCT) | **Não definido** pelo Fluxo de Telas — mantido como alias legado, não-confirmado |
| `success` | `#22C55E` | `#146C2E` |
| `warning` | `#F5A623` | `#8A5000` |
| `error` | `#FF4D4F` | `#BA1A1A` |
| `on-surface` | `#0D0D1A` | `#1C1B1F` |
| `on-surface-variant` (+ 2º degrau `on-surface-variant-2`) | `#6B7280` + `#9CA3AF` | `#49454F` único (2º degrau removido) |
| `outline` / `outline-variant` | `#E5E7EB` / inferido | `#79747E` / `#CAC4D0` (ambos confirmados pela spec) |
| `surface-container-*` (4 níveis) | herdados de elevação tonal do manual | 5 níveis com valores próprios (`lowest/low/DEFAULT/high/highest`) |
| Escala tipográfica | 15 estilos (display-large até label-small) | 12 estilos — `display-large`, `display-medium`, `headline-medium` removidos |
| Fonte | Google Sans Flex (display/headline/title) + Roboto (body/label) | Fonte única: Google Sans Flex (fallback Google Sans → Roboto → system-ui) em todos os estilos |
| Espaçamento | 6 degraus (`xs/sm/md/lg=16/xl/xxl`) | 8 degraus (`xs/sm/md/base=16/lg=20/xl/xxl/xxxl=40`) — `lg` muda de 16 para 20 |
| Card radius | 12px (`md`) | 16px |
| Sheet/dialog radius | 16px (`lg`, compartilhado) | SheetFrame 28px · Dialog 24px (tokens separados) |
| Button radius | 12px (`md`) | 20px (token novo, fora da escala de 7 degraus) |
| Elevação tonal, state layers, motion, densidade de ícone | definidos pelo manual MD3 | **não redefinidos** pelo Fluxo de Telas — mantidos como estavam |

## O que foi atualizado nesta passagem

- `.claude/skills/SignallQ-design/colors_and_type.css` — paleta clara/escura, escala tipográfica
  (12 estilos), escala de espaçamento (8 degraus), tokens de forma por componente.
- `.claude/skills/SignallQ-design/SKILL.md` — non-negotiables.
- `.claude/skills/SignallQ-design/README.md` — seção "VISUAL FOUNDATIONS".
- `.claude/skills/SignallQ-design/HANDOFF_README.md` — tabelas de referência rápida de tokens.
- `docs_ai/design-system/COLORS.md`, `DESIGN_TOKENS.md`, `TYPOGRAPHY.md`, `SPACING.md`,
  `MD3_GUIDELINES.md` — valores atualizados para a nova fonte de verdade, com nota explícita de
  que `SignallQTheme.kt` (código Android) ainda não foi atualizado.
- `docs_ai/design-system/COMPONENTS_ANDROID.md` — nota sobre a descontinuação da superfície
  SignallQ AI (tela 7 do Fluxo de Telas).
- `.claude/CLAUDE.md` — seção "Design System" (hex do acento, radius de card, path da skill
  corrigido de `linka-design` para `SignallQ-design`).
- `.agents/skills/SignallQ-design/` e `.github/skills/SignallQ-design/` — mirrors resincronizados
  com o conteúdo final de `.claude/skills/SignallQ-design/` (o mirror `.agents/skills/` ainda
  usava o nome antigo `linka-design`, renomeado nesta passagem).
- `.claude/agents/lia.md`, `camilo.md`, `rhodolfo.md` — checados por hex hardcoded da paleta
  antiga (ver arquivos alterados na entrega para o resultado exato).

## O que ficou de fora por decisão de escopo

Esta é uma correção de **documentação/skill**, não de implementação. Os seguintes artefatos ainda
usam os tokens antigos (`primary=#6C2BFF`, escala de 15 estilos, espaçamento de 6 degraus, radius
antigo) e serão corrigidos em fase separada de implementação, não nesta passagem:

- `packages/design-system/` (gêmeo digital React) — Fase 1/Fase 2 de bottom sheets já mergeadas
  usando os valores antigos; precisa de rodada de correção de tokens.
- `android/**/SignallQTheme.kt` (`LkColors`, `LkSpacing`, `LkRadius`, `signallQTypography`) —
  código de produção Android, fora do escopo de uma tarefa de documentação.
- `.claude/skills/SignallQ-design/preview/*.html` — cards de referência visual do DS, ainda
  renderizam com os tokens antigos (herdam de `colors_and_type.css` só se forem re-gerados).
- `.claude/skills/SignallQ-design/ui_kits/android/*.jsx` — já estava sinalizado como não migrado
  na renomeação de 2026-07-11 (usa os aliases deprecados); continua não migrado.
- `SignallQ Admin/` (Console) e `integrations/cloudflare/signallq-admin-worker/` — fora do escopo
  de qualquer tarefa da Lia por regra fixa do squad (design entregue, implementação é do Camilo).

## Lacunas sinalizadas (não inventadas — pendentes de decisão futura)

- **`tertiary`** — o Fluxo de Telas não define esse role. Token mantido em `colors_and_type.css`
  com o hex anterior, mas marcado como **não-confirmado**; não usar em artefato novo sem validar
  com a Lia se/quando uma tela real precisar de um terceiro acento.
- **Elevação tonal, state layers, motion, densidade de ícone** — o Fluxo de Telas é uma spec de
  telas, não um manual de tokens sistêmicos; esses sistemas continuam valendo os valores da
  migração de 2026-07-11 (só os *tints* de elevação foram realinhados aos novos hex de
  surface-container). Se o projeto Claude Design vier a publicar uma spec sistêmica nova, revisar
  de novo.
