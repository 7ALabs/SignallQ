---
name: auditar-ux
description: Auditoria profunda de design system (tokens MD3, cores, tipografia, contraste WCAG) e de usabilidade (arquitetura de informação, fluxos, navegação, heurísticas mobile) do SignallQ Android. Executada pela Lia em modo Sonnet.
---

## Quando usar

**Design system:**
- Antes de Camilo implementar componente visual novo ou tela relevante
- Inconsistência visual reportada: cor, fonte ou espaçamento fora do padrão MD3
- Revisão de acessibilidade: contraste WCAG, TalkBack, ARIA, navegação por teclado
- Definição ou atualização de tokens no `MaterialTheme`, `LocalLkTokens` (Android)
- Quando o visual "parece errado mas ninguém sabe dizer por quê"
- Pré-release de tela nova — gate antes de Gema fechar Done

**Usabilidade:**
- Antes de implementar nova estrutura de navegação ou reordenar telas
- Quando usuários relatam dificuldade em encontrar ou executar uma função
- Após adicionar feature nova: verificar se ela se encaixa na arquitetura existente
- Review de onboarding / primeiro uso do app
- Quando o app "funciona mas parece confuso" — sem problema visual específico
- Pré-release de feature de navegação (Bottom Nav, tabs, drawer, deep links)
- Revisão de fluxo completo: do objetivo do usuário até a conclusão da tarefa

$ARGUMENTS

---

## Fonte de verdade — Design System SignallQ

O design system oficial está em `/linka-design` (`.Codex/skills/linka-design/`):
- `colors_and_type.css` — tokens de cores, tipografia, espaçamento e raios
- `SignallQTheme.kt` (Android) — tema Compose com os mesmos tokens
- `HANDOFF_README.md` — tabela de equivalência CSS → Compose
- `ui_kits/android/` — componentes de referência em alta fidelidade
- `README.md` — fundações visuais, iconografia e contexto de produto

Toda auditoria deve comparar tokens implementados contra esta fonte de verdade. **Não copiar tokens para esta skill** — sempre consultar `/linka-design`.

---

# Auditoria de Design System

### 1. Mapear tokens existentes (via Marcelo)

Acionar Marcelo para extrair:

**Android:**
- Definições em `MaterialTheme` e `LocalLkTokens` (cores, tipografia, spacing, shape, elevation)
- Composables de tema: onde `LkTheme` ou `MaterialTheme` é declarado
- Tokens custom fora do MD3 canônico (hardcoded `Color(0xFF...)` fora de definições de paleta)

**Saída esperada:** tabela de tokens → nome, valor, uso atual.

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

---

### 4. Auditar espaçamento

| Verificação | Critério |
|---|---|
| Escala base | 4dp ou 8dp — valores em múltiplos (4, 8, 12, 16, 24, 32, 48) |
| Padding mínimo de tela | ≥ 16dp horizontal, ≥ 12dp entre seções |
| Sem magic numbers | Nenhum `padding = 7.dp` ou `Spacer(Modifier.height(13.dp))` sem token |
| Density adequada | Listas densas (WiFi, History): espaçamento compacto. Cards de diagnóstico: breathing room |
| Touch target mínimo | ≥ 48dp para elementos interativos |

---

### 5. Auditar estados visuais de UX flows

Para cada fluxo principal do SignallQ:

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

---

# Auditoria de Usabilidade

### 7. Mapear arquitetura de informação (via Marcelo)

Acionar Marcelo para listar:

**Android:** screens registradas na `NavGraph`, destinos de `BottomNavigation`, rotas de `NavController`

**Saída esperada:** mapa de telas com hierarquia (nível 1 = acessível pelo nav principal / nível 2+ = dentro de fluxo).

---

### 8. Avaliar arquitetura de informação

| Verificação | Critério |
|---|---|
| Profundidade máxima | Funcionalidade principal em ≤ 3 taps/cliques a partir da tela inicial |
| Agrupamento lógico | Telas relacionadas agrupadas — o usuário prevê onde está antes de navegar |
| Bottom Nav (Android) | 3–5 destinos no máximo; destinos representam tarefas principais, não features internas |
| Nomes de destinos | Nomes no nav refletem o que o usuário quer fazer — não nomes internos de feature |
| Tela inicial | O usuário sabe imediatamente o que o app faz e o que fazer a seguir |
| Hierarquia de screens | Nenhuma tela importante enterrada atrás de 3+ níveis de navegação |

---

### 9. Avaliar fluxos de tarefa críticos

Para cada fluxo principal do SignallQ, contar taps/cliques e identificar friction:

**Fluxo 1 — Executar diagnóstico:**
1. O usuário sabe onde iniciar? (CTA visível na home?)
2. Quantos taps até ver o resultado?
3. O resultado entrega ação clara — o usuário sabe o que fazer a seguir?

**Fluxo 2 — Medir velocidade:**
1. O speedtest é acessível com 1 tap a partir da home?
2. O usuário entende que o teste está rodando? (progressão visível)
3. O resultado é legível imediatamente — sem precisar interpretar?

**Fluxo 3 — Analisar Wi-Fi:**
1. O usuário sabe qual rede está sendo analisada?
2. A informação técnica (canal, frequência, RSSI) tem contexto suficiente para o usuário comum?
3. O problema encontrado tem ação sugerida ou é só dado bruto?

**Fluxo 4 — Consultar histórico:**
1. O histórico é encontrável sem explorar o app?
2. O usuário consegue comparar resultados de datas diferentes?
3. Há filtro ou busca quando o histórico é longo?

**Fluxo 5 — Ajustar configurações:**
1. Configurações acessíveis em ≤ 2 taps?
2. As opções fazem sentido sem documentação?
3. Mudança de configuração tem feedback imediato?

**Para cada fluxo, registrar:**
```
Fluxo: [nome]
Taps até conclusão: [N]
Friction encontrado: [descrição]
Severidade: [crítico | importante | melhoria]
```

---

### 10. Avaliar discoverabilidade

| Verificação | Critério |
|---|---|
| Features visíveis | Funcionalidades novas têm ponto de entrada visível — não dependem de descoberta acidental |
| Affordances visuais | Elementos interativos parecem interativos (botão parece botão, lista parece scrollável) |
| Call-to-action único | Em cada tela, há no máximo 1 CTA primário — o usuário sabe o que fazer |
| Ícones com label | Ícones de navegação têm texto ou tooltip — sem adivinhação de significado |
| Features avançadas | Features avançadas acessíveis mas não em destaque — não poluem o fluxo principal |

---

### 11. Avaliar onboarding e primeiro uso

| Verificação | Critério |
|---|---|
| Tela inicial sem dado | O que o usuário vê na primeira abertura? Há CTA claro? |
| Permissões | Permissão solicitada no contexto certo — não em bloco na abertura |
| Explicação de valor | O usuário entende o que o app faz em < 10 segundos? |
| Primeiro diagnóstico | O caminho até o primeiro diagnóstico é guiado ou o usuário se perde? |
| Tutoriais/tooltips | Se existem: são contextuais (aparecem no momento certo) ou bloqueiam uso? |

---

### 12. Avaliar navegação e back stack

| Verificação | Critério |
|---|---|
| Botão Voltar (Android) | Voltar sempre leva ao lugar esperado — sem loops ou saltos inesperados |
| Back stack | Resultado de diagnóstico → tela anterior é a home, não um estado intermediário |
| Deep links | Se existirem, chegam na tela certa sem quebrar o back stack |
| Bottom Nav + back | Trocar de aba e pressionar Voltar não sai do app inesperadamente |
| Modal e bottom sheets | Fechar modal retorna ao estado anterior da tela — sem perda de posição |

---

### 13. Avaliar recuperação de erros

| Verificação | Critério |
|---|---|
| Ação destrutiva tem confirmação | Resetar dados, limpar histórico: pede confirmação antes |
| Erro de rede | Tela de erro tem botão "Tentar novamente" visível e funcional |
| Erro recuperável vs. fatal | App diferencia: "Sem conexão, tente agora" vs. "Algo deu errado, feche e reabra" |
| Formulários | Erro de validação aponta o campo específico — não apaga o que o usuário digitou |
| Teste interrompido | Se o speedtest ou diagnóstico for interrompido, o estado é limpo corretamente |
| Estado de loading travado | Há timeout — loading não fica girando para sempre sem mensagem de erro |

---

### 14. Heurísticas de usabilidade móvel (Nielsen adaptado)

| Heurística | O que verificar no SignallQ |
|---|---|
| **Visibilidade do status** | O usuário sempre sabe o que o app está fazendo (teste rodando, resultado carregando, erro ocorrido) |
| **Correspondência com o real** | Termos usados condizem com o que o usuário conhece ("velocidade", não "throughput") |
| **Controle e liberdade** | O usuário pode cancelar operações em andamento |
| **Consistência** | Mesma ação → mesmo resultado em qualquer tela |
| **Prevenção de erros** | Ações destrutivas são difíceis de acionar por acidente |
| **Reconhecimento > memorização** | O usuário não precisa lembrar onde estava — contexto visível na tela |
| **Flexibilidade** | Usuário avançado consegue mais detalhes; usuário comum não se perde neles |
| **Design minimalista** | Cada tela tem só o necessário — sem informação que compete com o conteúdo principal |
| **Ajuda na recuperação** | Mensagens de erro em linguagem humana com caminho de saída |
| **Documentação** | O app não precisa de manual — mas se houver ajuda, está onde o usuário precisa |

---

### 15. Verificar consistência de padrões

| Verificação | Critério |
|---|---|
| Padrão de lista | Todas as listas do app usam o mesmo componente e comportamento (swipe, tap, long press) |
| Padrão de card | Cards com ação têm o mesmo tratamento de tap e feedback |
| Padrão de loading | Loading sempre aparece no mesmo lugar e com o mesmo visual por tipo de operação |
| Padrão de confirmação | Confirmações de ação usam dialog padrão — sem variação de UI entre telas |
| Terminologia | Mesmo conceito tem o mesmo nome em todas as telas (sem "Diagnóstico" numa e "Análise" noutra) |

---

# Emitir especificação

Para cada problema encontrado:

```
Categoria: [Design System | Usabilidade]
Componente/token/fluxo: [nome do Composable, token MD3, fluxo, tela]
Problema: [descrição objetiva]
Especificação: [token → valor → justificativa | mudança de fluxo/navegação]
Prioridade: [crítico | importante | melhoria]
Responsável: Camilo
```

Problemas críticos (contraste quebrado, touch target < 48dp, ARIA ausente em elemento interativo, funcionalidade principal enterrada além de 3 taps, ação destrutiva sem confirmação, loading sem timeout) **bloqueiam aprovação**. Demais são registrados e priorizados por Claudete.

---

## Output esperado

**Design system:**
1. **Tabela de tokens existentes** — nome, valor, uso atual, status (ok / inconsistente / ausente)
2. **Problemas de contraste** — elemento, razão medida, razão exigida, veredicto
3. **Problemas de tipografia e espaçamento** — magic numbers, tokens faltando, variações sem padrão
4. **Estados visuais de UX flows** — friction points encontrados, o que mudar e por quê
5. **Problemas de acessibilidade** — TalkBack/ARIA faltando, foco invisível, touch target pequeno

**Usabilidade:**
6. **Mapa de arquitetura** — telas por nível de acesso, gaps identificados
7. **Fluxos de tarefa** — taps até conclusão, friction encontrado, severidade
8. **Discoverabilidade** — features enterradas, ícones sem label, CTAs ausentes
9. **Onboarding** — avaliação do primeiro uso, permissões, valor comunicado
10. **Navegação e back stack** — problemas de back, deep link, modal
11. **Recuperação de erros** — timeouts, fallbacks, confirmações faltando
12. **Score por heurística** — ok / atenção / problema para cada uma das 10
13. **Inconsistências de padrão** — variações injustificadas entre telas

**Consolidado:**
14. **Especificação emitida** — lista de correções para Camilo, com prioridade e esforço estimado
15. **Decisões registradas** — o que foi documentado no decision log do projeto

---

## Limites

- Não inclui auditoria de performance (recomposição, reflow, FPS) — escopo é consistência visual, UX e usabilidade.
- Não valida lógica de diagnóstico de rede — usar `/regras-diagnostico-rede`.
- Não define novo design system do zero em uma task — auditar e propor incrementalmente.
- Contraste: calcular com referência nos valores de cor declarados — sem depender de ferramentas externas de browser.
- Não revisa microcopy em detalhe — usar `/revisar-ux`.
- Não audita estados vazios isolados — usar `/revisar-ux`.
- Não avalia fluxo de diagnóstico em profundidade — usar `/motor-diagnostico`.
- Testes com usuário real estão fora do escopo — esta skill avalia heurísticas, não comportamento observado.
- Não bloqueia entrega por problema de melhoria — somente crítico bloqueia.
