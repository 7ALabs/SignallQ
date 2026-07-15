# Espaçamento — SignallQ

**Fonte oficial:** `LkSpacing` em `SignallQTheme.kt`  
**Escopo:** Android v0.23.0+  
**Última atualização:** 2026-07-15

---

## Escala oficial

| Token | Valor | Uso |
| --- | --- | --- |
| `xs` | 4 dp | ajustes finos, separações mínimas |
| `sm` | 8 dp | gaps simples, paddings pequenos |
| `md` | 12 dp | espaçamento padrão interno |
| `base` | 16 dp | padding horizontal principal e cards |
| `lg` | 20 dp | separações de bloco e grupos mais densos |
| `xl` | 24 dp | separações claras entre seções |
| `xxl` | 32 dp | grandes blocos ou respiros |
| `xxxl` | 40 dp | grandes aberturas verticais |

---

## Regras

- Preferir `LkSpacing` em vez de `16.dp`, `24.dp`, etc.
- Valores fora da escala devem ser exceção técnica, não padrão visual.
- Medidas como `3.dp`, `5.dp`, `10.dp`, `11.dp`, `13.dp`, `14.dp` e similares devem ser tratadas como dívida visual quando usadas em layout comum.

---

## Raios relacionados

| Token | Valor |
| --- | --- |
| `LkRadius.card` | 16 dp |
| `LkRadius.button` | 20 dp |
| `LkRadius.input` | 12 dp |
| `LkRadius.sheet` | 28 dp |
| `LkRadius.pill` | 999 dp |

---

## Observação

O app já possui uma escala de espaçamento bem definida na fundação, mas ainda não está plenamente aplicada em todas as telas e componentes. A meta do saneamento visual é transformar esta escala em padrão obrigatório de consumo.
