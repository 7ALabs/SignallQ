# Tipografia — SignallQ

**Fonte de verdade:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`  
**Escopo:** Android v0.23.0+  
**Última atualização:** 2026-07-15

---

## Família tipográfica

Fonte única do app:

- `Google Sans Flex`
- fallback técnico no código via recursos `google_sans_flex_*`

Pesos em uso no tema:

- `400` Normal
- `500` Medium
- `600` SemiBold
- `700` Bold

Nenhuma nova tela deve introduzir segunda família tipográfica.

---

## Escala tipográfica oficial (MD3)

Todos os tamanhos abaixo são a implementação oficial atual em `signallQTypography`.

| Token | Tamanho | Line height | Peso | Tracking |
| --- | --- | --- | --- | --- |
| `displayLarge` | 34 sp | 40 sp | Bold | 0 |
| `displayMedium` | 34 sp | 40 sp | Bold | 0 |
| `displaySmall` | 34 sp | 40 sp | Bold | 0 |
| `headlineLarge` | 26 sp | 32 sp | Bold | 0 |
| `headlineMedium` | 26 sp | 32 sp | Bold | 0 |
| `headlineSmall` | 22 sp | 28 sp | SemiBold | 0 |
| `titleLarge` | 20 sp | 26 sp | SemiBold | 0 |
| `titleMedium` | 16 sp | 22 sp | Medium | 0.1 |
| `titleSmall` | 14 sp | 20 sp | Medium | 0.1 |
| `bodyLarge` | 16 sp | 24 sp | Normal | 0.15 |
| `bodyMedium` | 14 sp | 20 sp | Normal | 0.2 |
| `bodySmall` | 12 sp | 16 sp | Normal | 0.25 |
| `labelLarge` | 14 sp | 20 sp | Medium | 0.1 |
| `labelMedium` | 12 sp | 16 sp | Medium | 0.3 |
| `labelSmall` | 11 sp | 16 sp | Medium | 0.4 |

---

## Regras de uso

- Preferir sempre `MaterialTheme.typography.*`.
- Evitar `fontSize = ...sp` e `letterSpacing = ...sp` em tela/componente comum.
- Só usar `TextStyle(...)` manual quando houver motivo técnico real, como canvas, chart labels ou renderização custom.
- Mesmo papel visual deve usar o mesmo token em todas as telas.

---

## Situação atual do código

- A implementação do tema já está alinhada com a spec mais recente.
- Ainda existem telas e componentes com `fontSize` e `letterSpacing` hardcoded.
- Esses hardcodes devem ser tratados como dívida de padronização, não como novo padrão.
