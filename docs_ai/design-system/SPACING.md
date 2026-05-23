# Espaçamento — Linka

**Fonte:** `LinkaTheme.kt` (LkSpacing) | Padrão grid 8 dp  
**Escopo:** Android v0.6.3

---

## Token de Espaçamento

Linka segue grid 8 dp do Material Design 3. Tokens definidos em `LkSpacing`:

| Token | Valor | Uso |
| --- | --- | --- |
| `xs` | 4 dp | Ajustes finos, pequenos gaps |
| `sm` | 8 dp | Unidade fundamental (padding interno, gaps) |
| `md` | 12 dp | Espaçamento padrão interno |
| `lg` | 16 dp | Padding de telas, seções |
| `xl` | 24 dp | Divisões significativas entre áreas |
| `xxl` | 32 dp | Espaçamento entre grandes seções |
| `cardContent` | 16 dp | Padding interno de cards |

---

## Raios de Borda

| Token | Valor | Uso |
| --- | --- | --- |
| `card` | 16 dp | Cards, superfícies principais |
| `button` | 12 dp | Botões |
| `input` | 12 dp | Campos de entrada |

---

## Validação em Código

Não há token centralizado de espaçamento em `dimens.xml`. Valores são aplicados diretamente via `Modifier.padding()` em composables.

Para validar espaçamento em uso, execute:

```bash
grep -r "padding(" \
  linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/component/ \
  linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ \
  | grep -E "(4|8|12|16|24|32)\.dp"
```

---

## Aplicação

Preferir `LkSpacing` ao invés de hardcoding valores:

```kotlin
// Correto
Box(modifier = Modifier.padding(LkSpacing.lg))

// Evitar
Box(modifier = Modifier.padding(16.dp))
```

---

## Material Design 3 — Princípios

- Espaçamento cria ritmo vertical e harmonia visual.
- Grid 8 dp reduz decisões arbitrárias.
- Acessibilidade: 56 dp mínimo para hit targets (toque).
