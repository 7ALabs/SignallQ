---
name: design-system-audit
description: Auditoria de design system do Linka — tokens MD3, paleta de cores, tipografia, espaçamento, contraste WCAG e estados visuais. Cobre Android (MaterialTheme/LocalLkTokens) e PWA (Tailwind/CSS tokens). Executada por Lia. Emite especificação acionável para Camilo (Android) ou Renan (PWA).
---

## Quando usar

- Antes de Camilo ou Renan implementar componente visual novo ou tela relevante
- Inconsistência visual reportada: cor, fonte ou espaçamento fora do padrão MD3
- Revisão de acessibilidade: contraste WCAG, TalkBack, ARIA, navegação por teclado
- Definição ou atualização de tokens no `MaterialTheme`, `LocalLkTokens` (Android) ou `tailwind.config`/variáveis CSS (PWA)
- UX review de fluxo: onde o usuário se perde ou onde falta feedback visual
- Quando o visual "parece errado mas ninguém sabe dizer por quê"
- Pré-release de tela nova — gate antes de Gema fechar Done

$ARGUMENTS

---

## Fonte de verdade

O design system oficial do Linka está em `.Codex/skills/linka-design/`:
- `colors_and_type.css` — tokens de cores, tipografia, espaçamento e raios
- `LinkaTheme.kt` (Android) — tema Compose com os mesmos tokens
- `HANDOFF_README.md` — tabela de equivalência CSS → Compose
- `ui_kits/android/` — componentes de referência em alta fidelidade
- `README.md` — fundações visuais, iconografia e contexto de produto

Toda auditoria deve comparar tokens implementados contra esta fonte de verdade.

---

## Passos

### 1. Mapear tokens existentes (via Marcelo)

Acionar Marcelo para extrair:

**Android:**
- Definições em `MaterialTheme` e `LocalLkTokens` (cores, tipografia, spacing, shape, elevation)
- Composables de tema: onde `LkTheme` ou `MaterialTheme` é declarado
- Tokens custom fora do MD3 canônico (hardcoded `Color(0xFF...)` fora de definições de paleta)

**PWA:**
- Variáveis CSS declaradas em `:root` ou `tailwind.config.js` (cores, fontes, espaçamentos, sombras, border-radius)
- Classes utilitárias Tailwind customizadas
- Componentes React de UI reutilizáveis já implementados

**Saída esperada:** tabela de tokens → nome, valor, uso atual, plataforma.

---

### 2. Auditar tokens de cor

#### Android (MD3 Color Roles)

| Verificação | Critério |
|---|---|
| Roles MD3 usados corretamente | `primary`, `onPrimary`, `secondary`, `surface`, `onSurface`, `surfaceVariant`, `error`, `onError` — sem inversão |
| Sem hardcoded inline | Nenhum `Color(0xFF...)` dentro de Composable fora de definições de paleta |
| Contraste texto/fundo | ≥ 4.5:1 (WCAG AA normal) ou ≥ 3:1 (texto grande ≥18sp ou ≥14sp bold) |
| Dark mode funcional | Tokens têm variante `darkColorScheme` — sem quebra de legibilidade |
| Cores de acento/estado | Máximo 2 acentos primários; hover/pressed/focused diferenciado do estado normal |

#### PWA

| Verificação | Critério |
|---|---|
| Uso consistente | Mesmo papel visual → mesmo token Tailwind/CSS (não valor hexadecimal repetido inline) |
| Hierarquia de fundo | Background base < sidebar < painel < elevated (escala legível) |
| Sem hardcoded | Nenhum `#hex` ou `rgb()` fora das declarações de variável ou `tailwind.config` |
| Dark mode funcional | Classes `dark:` cobrindo todos os elementos visíveis |
| Contraste | ≥ 4.5:1 (texto normal), ≥ 3:1 (texto grande/bold) |

---

### 3. Auditar tipografia

#### Android

| Verificação | Critério |
|---|---|
| Usa `MaterialTheme.typography.*` | Sem `fontSize` avulso ou `fontWeight` inline sem token |
| Escala de roles aplicada | `displayLarge` > `headlineMedium` > `titleMedium` > `bodyMedium` > `labelSmall` |
| Line height | ≥ 1.4 para texto corrido; 1.2 aceito para labels curtos |
| Máximo de pesos em uso | Até 3 simultâneos (ex: 400, 500, 700) |
| Consistência de papel visual | Mesmo papel → mesmo token de tipo (sem variação arbitrária por tela) |

#### PWA

| Verificação | Critério |
|---|---|
| Famílias de fonte | Máximo 2: 1 sans-serif (UI) + 1 monospace se houver código/terminal |
| Escala Tailwind usada | `text-xs`, `text-sm`, `text-base`, `text-lg`, `text-xl` — tokens, não valores avulsos |
| Line height | `leading-relaxed` (1.625) para texto corrido; `leading-tight` aceito para labels |
| Peso | Máximo 3 pesos (`font-normal`, `font-medium`, `font-bold`) em uso simultâneo |
| Consistência | Mesmo papel visual → mesma classe Tailwind (sem variação arbitrária) |

---

### 4. Auditar espaçamento

| Verificação | Critério |
|---|---|
| Escala base | 4dp/px ou 8dp/px — valores em múltiplos (4, 8, 12, 16, 24, 32, 48) |
| Android: padding mínimo de tela | ≥ 16dp horizontal, ≥ 12dp entre seções |
| Android: sem magic numbers | Nenhum `padding = 7.dp` ou `Spacer(Modifier.height(13.dp))` sem token |
| PWA: usa escala Tailwind | `p-1/2/3/4/6/8` — sem `style="padding: 7px"` inline |
| PWA: sem magic numbers | Nenhum valor de espaçamento fora da escala Tailwind/config |
| Density adequada | Listas densas (WiFi, History): espaçamento compacto. Cards de diagnóstico: breathing room |
| Touch target mínimo | ≥ 48dp (Android) / ≥ 44px (PWA) para elementos interativos |

---

### 5. Auditar UX flows

Para cada fluxo principal do Linka:

**Home / Dashboard:**
- O usuário sabe o que fazer ao abrir o app? (CTA de diagnóstico visível e nomeado)
- Há feedback durante carregamento de dados de rede?
- Estado inicial (sem dados) tem empty state claro?

**Speedtest:**
- Progress da medição é visível e compreensível (download → upload → ping)?
- Resultado final: hierarquia clara entre métricas primárias e secundárias?
- Estado de erro de rede tem microcopy objetivo — o que falhou e o que fazer?

**Diagnóstico por IA:**
- O usuário entende que está aguardando análise de IA? (loading state distinto)
- Resultado de diagnóstico diferencia claramente: problema encontrado vs. tudo OK vs. indeterminado?
- Ações práticas visualmente hierarquizadas (primária > secundária)?

**Wi-Fi:**
- Lista de redes diferencia rede conectada das demais?
- Signal strength visualmente graduado — boa, média, ruim?
- Seção de detalhes: informação técnica (SSID, BSSID, canal) não sobrecarrega usuário comum?

**Histórico:**
- Sessões diferenciadas por data de forma scannable?
- Comparação temporal tem hierarquia visual legível?
- Empty state quando sem histórico tem call-to-action?

**Configurações:**
- Grupos de configuração visualmente separados?
- Ações destrutivas (resetar dados) têm estado de confirmação?
- Feedback visual após salvar configuração?

---

### 6. Auditar acessibilidade

#### Android (TalkBack / Accessibility)

| Verificação | Critério |
|---|---|
| Contraste WCAG AA | ≥ 4.5:1 (texto normal), ≥ 3:1 (texto ≥18sp ou ≥14sp bold) |
| `contentDescription` | Ícones sem texto têm `contentDescription` descritivo — não nulo, não vazio |
| Touch target | ≥ 48dp para elementos interativos (`Modifier.minimumInteractiveComponentSize()`) |
| Merge semantics | Grupos de informação relacionada têm `Modifier.semantics(mergeDescendants = true)` |
| Foco de teclado/acessibilidade | Ordem de foco lógica e previsível |
| Role de acessibilidade | Botões têm `Role.Button`, checkboxes têm `Role.Checkbox`, etc. |

#### PWA (WCAG / ARIA)

| Verificação | Critério |
|---|---|
| Contraste WCAG AA | ≥ 4.5:1 (texto normal), ≥ 3:1 (texto grande/bold ≥18px ou ≥14px bold) |
| Foco visível | Todos os elementos interativos têm outline de foco visível com contraste |
| ARIA em ícones | Botões com ícone sem texto têm `aria-label` descritivo |
| Semântica HTML | `<nav>`, `<main>`, `<section>`, `<header>` usados corretamente |
| Rótulos de formulário | Inputs têm `<label>` associado ou `aria-label` |
| Navegação por teclado | É possível acionar ações principais sem mouse (`Tab`, `Enter`, `Space`) |

---

### 7. Verificar consistência Android ↔ PWA

Para features com paridade declarada (ver `/android-pwa-parity`):

| Verificação | Critério |
|---|---|
| Paleta de cores | Mesmas cores de marca nas duas plataformas — divergência só se justificada por limitação de plataforma |
| Hierarquia visual | Mesma ordem de importância de informações em cada tela equivalente |
| Terminologia | Mesmo microcopy para mesmos conceitos — sem variação de nomenclatura entre plataformas |
| Estados visuais | Loading, erro, sucesso e vazio definidos nas duas plataformas |
| Ações principais | Mesmos CTAs primários disponíveis (quando possível no browser) |

---

### 8. Emitir especificação

Para cada problema encontrado:

```
Plataforma: [Android | PWA | Ambas]
Componente/token: [nome do Composable, token MD3, classe Tailwind]
Problema: [descrição objetiva]
Especificação: [token → valor → justificativa]
Prioridade: [crítico | importante | melhoria]
Responsável: [Camilo (Android) | Renan (PWA) | ambos]
```

Problemas críticos (contraste quebrado, touch target < 48dp, ARIA ausente em elemento interativo) **bloqueiam aprovação**. Demais são registrados e priorizados por Claudete.

---

## Output esperado

1. **Tabela de tokens existentes** — nome, valor, plataforma, uso atual, status (ok / inconsistente / ausente)
2. **Problemas de contraste** — elemento, razão medida, razão exigida, veredicto
3. **Problemas de tipografia e espaçamento** — magic numbers, tokens faltando, variações sem padrão
4. **UX flows auditados** — friction points encontrados, o que mudar e por quê
5. **Problemas de acessibilidade** — TalkBack/ARIA faltando, foco invisível, touch target pequeno
6. **Inconsistências Android ↔ PWA** — divergências injustificadas entre plataformas
7. **Especificação emitida** — lista de correções para Camilo/Renan, com prioridade
8. **Decisões registradas** — o que foi documentado no decision log via `/decision-log`

---

## Limites

- Não inclui auditoria de performance (recomposição, reflow, FPS) — escopo é consistência visual e UX.
- Não valida lógica de diagnóstico de rede — usar `/network-diagnostic-rules`.
- Não define novo design system do zero em uma task — auditar e propor incrementalmente.
- Contraste: calcular com referência nos valores de cor declarados — sem depender de ferramentas externas de browser.
- Não bloqueia entrega por problema de melhoria — somente crítico bloqueia.
- Para features impossíveis no browser, não exigir paridade — usar `/browser-limitations` para validar.
