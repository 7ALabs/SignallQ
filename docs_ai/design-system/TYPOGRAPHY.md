# Tipografia — Linka

**Fonte:** `LinkaTheme.kt` (linkaTypography)  
**Escopo:** Android v0.6.3

---

## Material Design 3 — Escala Padrão

Todos os tamanhos em `sp` para respeitar `fontScale` do sistema (acessibilidade).

| Estilo | Tamanho | Peso | Uso |
| --- | --- | --- | --- |
| `displayLarge` | 34 sp | Bold | Heading de destaque, hero text |
| `headlineLarge` | 24 sp | SemiBold | Títulos principais de seção |
| `headlineMedium` | 20 sp | SemiBold | Subtítulos de seção |
| `headlineSmall` | 18 sp | SemiBold | Headings menores, card titles |
| `titleLarge` | 16 sp | Medium | Títulos de features |
| `titleMedium` | 15 sp | Medium | Títulos secundários |
| `titleSmall` | 14 sp | Medium | Labels de componentes |
| `bodyLarge` | 16 sp | Normal | Texto principal, descrições longas |
| `bodyMedium` | 14 sp | Normal | Corpo padrão |
| `bodySmall` | 12 sp | Normal | Texto menor, suplementar |
| `labelLarge` | 14 sp | Medium | Labels de botões/chips |
| `labelMedium` | 12 sp | Normal | Hints, captions |
| `labelSmall` | 11 sp | Normal | Footnotes, muito pequeno |

---

## Componentes de Animação Tipográfica

Linka inclui dois componentes especiais para animação de texto:

### TypewriterText
Anima texto entrando caractere por caractere, criando efeito de digitação.

**Uso:** Respostas da IA no Orbit, quando o texto é gerado.  
**Arquivo:** `linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/component/TypewriterText.kt`

### RotatingMessageText
Rotaciona entre múltiplas mensagens em loop, com fade in/out.

**Uso:** Perguntas contextuais, hints dinâmicos no LinkaPulse.  
**Arquivo:** `linka-android-kotlin/app/src/main/kotlin/io/linka/app/kotlin/ui/component/RotatingMessageText.kt`

---

## Uso em Composables

```kotlin
Text("Seu texto", style = MaterialTheme.typography.bodyMedium)
Text("Headline", style = MaterialTheme.typography.headlineMedium)
```

---

## Acessibilidade

- **Mínimo para corpo:** `bodyMedium` (14 sp) atende WCAG AA.
- **Respeto ao sistema:** Todos os tamanhos usam `sp`, não `dp`, para adaptar à configuração de zoom do usuário.
- **Contrast:** Material Design 3 garante contraste 4.5:1 em cores padrão.
