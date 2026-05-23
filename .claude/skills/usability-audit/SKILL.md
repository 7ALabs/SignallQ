---
name: usability-audit
description: Avaliação de navegação e usabilidade do Linka — arquitetura de informação, fluxos de tarefas, discoverabilidade de features, eficiência de navegação, onboarding, recuperação de erros e heurísticas de usabilidade móvel. Executada por Lia em modo Sonnet. Cobre Android e PWA.
---

## Quando usar

- Antes de implementar nova estrutura de navegação ou reordenar telas
- Quando usuários relatam dificuldade em encontrar ou executar uma função
- Após adicionar feature nova: verificar se ela se encaixa na arquitetura existente
- Review de onboarding / primeiro uso do app
- Quando o app "funciona mas parece confuso" — sem problema visual específico
- Pré-release de feature de navegação (Bottom Nav, tabs, drawer, deep links)
- Revisão de fluxo completo: do objetivo do usuário até a conclusão da tarefa

$ARGUMENTS

---

## Passos

### 1. Mapear arquitetura de informação (via Marcelo)

Acionar Marcelo para listar:

**Android:** screens registradas na `NavGraph`, destinos de `BottomNavigation`, rotas de `NavController`
**PWA:** rotas React Router, componentes de navegação, menu principal

**Saída esperada:** mapa de telas com hierarquia (nível 1 = acessível pelo nav principal / nível 2+ = dentro de fluxo).

---

### 2. Avaliar arquitetura de informação

| Verificação | Critério |
|---|---|
| Profundidade máxima | Funcionalidade principal em ≤ 3 taps/cliques a partir da tela inicial |
| Agrupamento lógico | Telas relacionadas agrupadas — o usuário prevê onde está antes de navegar |
| Bottom Nav (Android) | 3–5 destinos no máximo; destinos representam tarefas principais, não features internas |
| Nomes de destinos | Nomes no nav refletem o que o usuário quer fazer — não nomes internos de feature |
| Tela inicial | O usuário sabe imediatamente o que o app faz e o que fazer a seguir |
| Hierarquia de screens | Nenhuma tela importante enterrada atrás de 3+ níveis de navegação |

---

### 3. Avaliar fluxos de tarefa críticos

Para cada fluxo principal do Linka, contar taps/cliques e identificar friction:

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

### 4. Avaliar discoverabilidade

| Verificação | Critério |
|---|---|
| Features visíveis | Funcionalidades novas têm ponto de entrada visível — não dependem de descoberta acidental |
| Affordances visuais | Elementos interativos parecem interativos (botão parece botão, lista parece scrollável) |
| Call-to-action único | Em cada tela, há no máximo 1 CTA primário — o usuário sabe o que fazer |
| Ícones com label | Ícones de navegação têm texto ou tooltip — sem adivinhação de significado |
| Features avançadas | Features avançadas acessíveis mas não em destaque — não poluem o fluxo principal |

---

### 5. Avaliar onboarding e primeiro uso

| Verificação | Critério |
|---|---|
| Tela inicial sem dado | O que o usuário vê na primeira abertura? Há CTA claro? |
| Permissões | Permissão solicitada no contexto certo — não em bloco na abertura |
| Explicação de valor | O usuário entende o que o app faz em < 10 segundos? |
| Primeiro diagnóstico | O caminho até o primeiro diagnóstico é guiado ou o usuário se perde? |
| Tutoriais/tooltips | Se existem: são contextuais (aparecem no momento certo) ou bloqueiam uso? |

---

### 6. Avaliar navegação e back stack

| Verificação | Critério |
|---|---|
| Botão Voltar (Android) | Voltar sempre leva ao lugar esperado — sem loops ou saltos inesperados |
| Back stack | Resultado de diagnóstico → tela anterior é a home, não um estado intermediário |
| Deep links | Se existirem, chegam na tela certa sem quebrar o back stack |
| Bottom Nav + back | Trocar de aba e pressionar Voltar não sai do app inesperadamente |
| PWA: botão Voltar do browser | Navegação por histórico do browser funciona coerentemente |
| Modal e bottom sheets | Fechar modal retorna ao estado anterior da tela — sem perda de posição |

---

### 7. Avaliar recuperação de erros

| Verificação | Critério |
|---|---|
| Ação destrutiva tem confirmação | Resetar dados, limpar histórico: pede confirmação antes |
| Erro de rede | Tela de erro tem botão "Tentar novamente" visível e funcional |
| Erro recuperável vs. fatal | App diferencia: "Sem conexão, tente agora" vs. "Algo deu errado, feche e reabra" |
| Formulários | Erro de validação aponta o campo específico — não apaga o que o usuário digitou |
| Teste interrompido | Se o speedtest ou diagnóstico for interrompido, o estado é limpo corretamente |
| Estado de loading travado | Há timeout — loading não fica girando para sempre sem mensagem de erro |

---

### 8. Heurísticas de usabilidade móvel (Nielsen adaptado)

| Heurística | O que verificar no Linka |
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

### 9. Verificar consistência de padrões

| Verificação | Critério |
|---|---|
| Padrão de lista | Todas as listas do app usam o mesmo componente e comportamento (swipe, tap, long press) |
| Padrão de card | Cards com ação têm o mesmo tratamento de tap e feedback |
| Padrão de loading | Loading sempre aparece no mesmo lugar e com o mesmo visual por tipo de operação |
| Padrão de confirmação | Confirmações de ação usam dialog padrão — sem variação de UI entre telas |
| Terminologia | Mesmo conceito tem o mesmo nome em todas as telas (sem "Diagnóstico" numa e "Análise" noutra) |

---

## Output esperado

1. **Mapa de arquitetura** — telas por nível de acesso, gaps identificados
2. **Fluxos de tarefa** — taps até conclusão, friction encontrado, severidade
3. **Discoverabilidade** — features enterradas, ícones sem label, CTAs ausentes
4. **Onboarding** — avaliação do primeiro uso, permissões, valor comunicado
5. **Navegação e back stack** — problemas de back, deep link, modal
6. **Recuperação de erros** — timeouts, fallbacks, confirmações faltando
7. **Score por heurística** — ok / atenção / problema para cada uma das 10
8. **Inconsistências de padrão** — variações injustificadas entre telas
9. **Lista de correções** — severidade + responsável (Camilo / Renan) + estimativa de esforço

---

## Limites

- Não valida tokens visuais, contraste ou tipografia — usar `/design-system-audit`.
- Não revisa microcopy em detalhe — usar `/ux-copy-review`.
- Não audita estados vazios isolados — usar `/empty-state-review`.
- Não avalia fluxo de diagnóstico em profundidade — usar `/diagnostic-journey`.
- Não valida regras de negócio do diagnóstico de rede — usar `/network-diagnostic-rules`.
- Testes com usuário real estão fora do escopo — esta skill avalia heurísticas, não comportamento observado.
