# Design Tokens — Android

**Escopo:** SignallQ Android  
**Fonte oficial:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`  
**Última atualização:** 2026-07-15

---

## Fonte de verdade

Os tokens oficiais do app vivem em:

- `LkColors`
- `LkTokens`
- `LkSpacing`
- `LkRadius`
- `signallQTypography`

Toda nova tela, sheet, sub-tela ou variante deve consumir esta base antes de introduzir qualquer estilo local.

---

## Espaçamento oficial

| Token | Valor |
| --- | --- |
| `xs` | `4.dp` |
| `sm` | `8.dp` |
| `md` | `12.dp` |
| `base` | `16.dp` |
| `lg` | `20.dp` |
| `xl` | `24.dp` |
| `xxl` | `32.dp` |
| `xxxl` | `40.dp` |
| `cardContent` | `16.dp` |

---

## Raios oficiais

| Token | Valor |
| --- | --- |
| `card` | `16.dp` |
| `button` | `20.dp` |
| `input` | `12.dp` |
| `sheet` | `28.dp` |
| `pill` | `999.dp` |

---

## Regras de implementação

- Não usar hardcode visual quando já houver token equivalente.
- Sempre preferir componentes compartilhados antes de montar estrutura manual.
- Quando um valor novo parecer necessário, ele deve primeiro virar token ou componente-base.

---

## Estado atual

- A fundação do tema está consistente com a spec recente.
- Ainda há divergência de consumo em várias telas, principalmente por:
  - `fontSize` hardcoded
  - `Color(0x...)` fora do tema
  - espaçamentos em `dp` fora da escala-base
  - duplicação de padrões de card/row/badge/sheet
