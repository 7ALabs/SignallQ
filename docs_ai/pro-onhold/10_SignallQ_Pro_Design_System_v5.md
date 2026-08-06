# SignallQ Pro — Design System

**Status:** ativo · **Última validação:** 19/07/2026 · **Versão:** 5.1 · **Substitui:** Design
System v3 · **Fonte de verdade visual:** projeto Claude Design "SignallQ PRO - Design System"
(`77a19317-ea64-4e47-b55c-578eca776c09`) — este doc é a reconciliação textual, o projeto online
prevalece em caso de divergência visual · **Escopo:** SignallQ Pro (`io.signallq.pro`), Android
Kotlin/Compose · **Responsável:** Lia (Especialista Sr de Produto & UX)

> **Nota de estado (19/07/2026).** Este documento descreve o design-alvo do Pro, mas o app **já
> tem código real** — Fase 0 (esqueleto `:pro:app`) e Fase 1 (MVP0) mergeadas via PR #1159/#1157
> (ver `docs_ai/pro-onhold/13_SignallQ_Pro_Arquitetura_e_Reaproveitamento_v1.md`). A paleta de
> cores do tema claro já está implementada 1:1 em
> `android/pro/app/src/main/kotlin/io/signallq/pro/ui/theme/SignallQProColor.kt` e
> `SignallQProTheme.kt`, e 9 dos 15 componentes já existem em Kotlin
> (`android/pro/core/designsystem/`). Onde este doc diverge do código real, isso é sinalizado
> explicitamente nas seções 4 (profundidade), 8 (componentes) e no mapeamento Compose — não
> presuma que "ALVO" significa "não implementado".

> **Atualização 2026-07-18 — virada de identidade.** O projeto Claude Design `77a19317` (fonte da verdade visual do Pro) passou de **teal-dominante para azul-dominante**: Primary marca `#0B6CFF`, Secondary ciano `#006B76`, Tertiary roxo `#6558E8`, escala de sinal de 6 níveis, dois temas oficiais (claro/escuro) e tokens de gráficos. O corpo abaixo já reflete essa paleta (seções 2, 3, 3.1 e 8 reescritas nesta data). O projeto online **evolui** — antes de qualquer entrega visual, reler `foundations/tokens.html`, `status-and-charts.html` e `dark-mode.html` para confirmar que este doc segue atual.

## Estado atual vs. Alvo

Este design system descreve o **SignallQ Pro** (`io.signallq.pro`), majoritariamente **🎯 ALVO** — mas o app já tem código real em `android/pro/` (Fase 0 e Fase 1/MVP0 mergeadas, ver nota de estado acima). Serve de fonte para protótipo, e cada vez mais para implementação já existente do produto.

O **✅ ATUAL** é o SignallQ consumidor, cujos tokens de marca (primary `#5B21D6`, secondary `#2851B8`) estão no código (`SignallQTheme.kt`, skill `SignallQ-design`). Consumer e Pro **compartilham fundações, tipografia e componentes de medição**, mas a matiz de marca diverge de propósito: o Pro tem identidade **azul** própria (`#0B6CFF` — atualizada de teal para azul em 2026-07-18; ver nota acima e Canônico §5.2). Os tokens `#6C2BFF` e a paleta teal-dominante anterior (`#006B73` como primary) estão **mortos**.

---

## Identidade

### 1. Fundamentos da marca

O SignallQ Pro transforma medições técnicas em um serviço profissional vendável. A experiência deve transmitir competência, clareza e confiança sem cair na estética de ferramenta corporativa velha e carregada.

**Relação com o SignallQ gratuito.** O SignallQ gratuito explica a conexão para o consumidor. O SignallQ Pro registra, compara e comprova o serviço realizado pelo profissional.

**Diretriz central.** O Pro não é uma nova identidade desconectada. É a extensão profissional da marca SignallQ, com maior densidade de informação, rastreabilidade e foco em evidências.

- **Clareza antes de densidade** — mostrar primeiro o que exige decisão; métricas avançadas ficam por expansão ou detalhe.
- **Evidência antes de opinião** — toda conclusão importante aponta a medição, foto, observação ou comparação que a sustenta.
- **Fluxo antes de dashboard** — o produto é guiado por trabalho: cliente → visita → ambiente → medição → laudo.
- **Profissional sem burocracia** — reduzir registro manual, não criar formulário infinito para justificar cada clique.

**Posicionamento.** Diagnóstico profissional de Wi-Fi e conectividade, do levantamento ao laudo, em uma única visita.

### 2. Logo e arquitetura de marca

O símbolo e o lettering SignallQ permanecem inalterados. "PRO" funciona como qualificador de produto e nunca deve competir com a marca principal.

**Regras de aplicação**

- Usar o selo PRO em caixa alta, com cantos arredondados, na cor de marca do Pro (azul `#0B6CFF`), com o roxo de apoio `#6558E8` reservado a acento pontual — nunca como cor dominante do selo.
- Manter área de proteção mínima equivalente à altura da letra "Q" ao redor do conjunto.
- Em ícones pequenos, não usar a palavra PRO; diferenciar por fundo escuro e detalhe de canto.
- Não redesenhar o símbolo, não adicionar ferramentas, antenas, maletas ou escudos ao ícone.
- Não usar "SignallQPRO" como uma palavra única.

---

## Tokens

### 3. Cores e semântica

Direção cromática do Pro (fonte: projeto Claude Design `77a19317`, `foundations/tokens.html`/`status-and-charts.html`, snapshot 2026-07-18): **azul `#0B6CFF` como cor de marca**, ciano técnico `#006B76` e roxo `#6558E8` como cores de apoio, sobre Material 3 com **dois temas oficiais** (claro e escuro, mesma estrutura e componentes — troca-se token, nunca layout). O azul domina CTAs e ações primárias; ciano e roxo são apoio (chips, gráficos, destaques secundários) e nunca competem com o azul. Cor nunca deve ser o único meio de comunicar estado — sempre ícone + rótulo + valor.

**Color roles — tema claro**

| Token | Valor | Uso principal |
|---|---|---|
| Primary — marca | `#0B6CFF` | CTAs, ações primárias, identidade Pro |
| Primary Container | `#D8E7FF` | Seleção, destaque suave |
| Secondary — ciano técnico | `#006B76` | Apoio: chips, destaques secundários |
| Secondary Container | `#A9EDF3` | Superfícies informativas |
| Tertiary — roxo de apoio | `#6558E8` | Apoio: gráficos e realces pontuais |
| Tertiary Container | `#E5DEFF` | — |
| Success / Good | `#1AA25A` | Medição aprovada / melhora |
| Warning / Attention | `#E9AD27` | Risco ou recomendação |
| Error / Critical | `#D9363E` | Falha ou bloqueio |
| Error Container | `#FFDAD6` | — |
| Background | `#F7F9FC` | Fundo de tela |
| Surface | `#FFFFFF` | Cards e planos de conteúdo |
| Surface Container High | `#E7ECF3` | Superfícies elevadas |
| Outline | `#C4CBD5` | Contornos |
| Divider | `#E3E7EC` | Divisores (`--sqp-color-divider`) |
| Inverse Surface | `#252B33` | Superfícies invertidas |

**Escala de sinal — 6 níveis** (`--sqp-status-*`, usada em medições de qualidade/cobertura):

| Nível | Claro | Escuro |
|---|---|---|
| Excelente | `#16A85A` | `#32D978` |
| Bom | `#1AA25A` | `#32D978` |
| Atenção | `#E9AD27` | `#F5C451` |
| Fraco | `#ED7D2D` | `#FF964F` |
| Crítico | `#D9363E` | `#FF5F66` |
| Informação (neutro) | `#0B6CFF` | `#3D96FF` |

**Identidade de gráficos** (`--sqp-chart-*`): séries de download, upload, latência, jitter e perda, mais `grid` (grade discreta) e `reference` (linha tracejada). Linhas limpas, preenchimento suave; fundo branco no tema claro, surface no escuro, sem glow.

**Tema escuro.** Estrutura, componentes e hierarquia são **idênticos** entre os dois temas — nenhum componente é exclusivo de um tema, tudo consome tokens semânticos via `data-theme`. Exceção: o **laudo técnico é sempre claro**, independentemente do tema do app.

**Contraste.** Textos corridos devem manter contraste mínimo de 4,5:1 em ambos os temas. Estados positivos, de atenção e críticos sempre incluem ícone e rótulo textual, nunca só cor.

**Tokens mortos — nunca usar:** `#6C2BFF` (primary antigo do consumer) e a paleta teal-dominante anterior do próprio Pro (`#006B73` como primary — hoje rebaixado a Secondary `#006B76`; o antigo "elo violeta `#5B21D6`" foi substituído pelo roxo próprio do Pro `#6558E8`).

### 3.1 Relação com a paleta do SignallQ consumer

O SignallQ consumidor (✅ ATUAL, do código) usa **primary `#5B21D6`** (violeta) e **secondary `#2851B8`** (azul fixo, não derivado do primary), em Material 3 estrito, Google Sans Flex e grid 8dp. O SignallQ Pro compartilha essas **fundações, tipografia e componentes de medição**, mas tem **marca própria**: azul `#0B6CFF` como identidade, com ciano `#006B76` e roxo `#6558E8` de apoio. Os dois produtos não compartilham matiz — é intencional, para separar visualmente o produto profissional do consumidor (Canônico §5.1/§5.2).

Diferente da versão anterior deste doc (v3/primeira v5), o Pro **não** usa mais o violeta do consumer como elo de marca — a identidade azul é autônoma. O token `#6C2BFF` está morto em toda a plataforma e não pode ser citado como vivo em nenhum documento.

---

## Fundação visual

### 4. Tipografia, grid e espaçamento

**Tipografia.** Usar a família tipográfica já adotada no SignallQ (Google Sans Flex, fallback Roboto → system-ui). Na implementação Android, preferir a fonte do sistema ou a fonte oficial empacotada no projeto, sem misturar famílias por tela. Uma família em todas as telas — inclusive laudo (que é sempre claro, mas usa a mesma tipografia do app).

| Estilo | Tamanho | Peso | Altura de linha | Uso | Máx. de linhas |
|---|---|---|---|---|---|
| Display | 32 sp | Semibold | 40 sp | Conclusões e estados de sucesso (ex.: "Visita concluída") | 2 |
| Título | 24 sp | Semibold | 32 sp | Nome da tela, cabeçalho de seção principal | 1 |
| Seção | 18 sp | Semibold | 24 sp | Blocos e grupos dentro de uma tela | 2 |
| Corpo | 16 sp | Regular | 24 sp | Conteúdo principal, texto de card | 4 |
| Apoio | 14 sp | Regular | 20 sp | Metadados, ajuda, timestamps, legendas | 2 |
| Label | 12 sp | Medium | 16 sp | Chips, badges, indicadores, overlines (UPPERCASE) | 1 |

Truncar com reticências além do máximo de linhas; nunca cortar palavra no meio sem indicação visual.

**Grid e espaçamento** — base 8dp com passo fino de 4dp para ajustes de ícone/label, escala observada em código real (`android/pro/core/designsystem/`):

| Token | Valor | Uso típico observado |
|---|---|---|
| `spacing.xxs` | 4 dp | Gap ícone-texto em chip pequeno (`EvidenceChip`) |
| `spacing.xs` | 6 dp | Padding vertical de chip, gap de ícone em `EvidenceChip` |
| `spacing.sm` | 8 dp | Gap entre ícone e texto em banner/linha, padding vertical de banner |
| `spacing.sm2` | 10 dp | Padding vertical de `SyncBanner` |
| `spacing.md` | 12 dp | Padding de chip, gap de seção em `EnvironmentCard`, gap de `StateCard` |
| `spacing.base` | 16 dp | Margem horizontal padrão de card/linha/banner, padding de `RecommendationBlock`/`EnvironmentCard` |
| `spacing.lg` | 20 dp | Reservado — ainda não observado em componente real, usar para respiro entre seções médias |
| `spacing.xl` | 24 dp | Padding de `StateCard`, distância padrão entre blocos |

- Margem horizontal mobile: 16 dp; tablet: 24 dp.
- Distância padrão entre blocos: 24 dp.
- Ícone padrão: 24 dp (Material Symbols Outlined); ícone de chip/contexto: 14–18 dp.
- **Alvos de toque mínimos: 48 × 48 dp** — vale para todo elemento acionável (botão, chip removível, item de lista, ícone de ação), mesmo quando o conteúdo visual for menor (ex.: `EvidenceChip` com ícone de 14dp precisa de área de toque expandida até 48dp via `Modifier.minimumInteractiveComponentSize()` ou padding equivalente).

**Raios — quando usar cada um**

| Raio | Valor | Componentes reais | Quando usar |
|---|---|---|---|
| Pílula | `999.dp` | `StatusChip`, `EvidenceChip` | Estado, filtro, tag — elemento curto de leitura rápida |
| Card | `16.dp` | `StateCard`, `RecommendationBlock`, `EnvironmentCard` | Bloco de conteúdo agrupado, com ou sem interação |
| Banner/controle | `12.dp` | `SyncBanner` | Faixa de status persistente, campo de entrada, botão |

Regra de decisão card vs. borda vs. elevação vs. só diferença de superfície: usar **card com raio 16dp** quando o conteúdo precisa se destacar do fundo por agrupamento (lista de ambientes, recomendação, estado vazio/erro); usar **só diferença de tom de superfície sem card** quando o agrupamento é óbvio pelo layout (linha de lista dentro de uma seção já demarcada — `ListRow` não usa raio de card, é uma linha contínua); usar **borda (`outline`)** quando dois elementos adjacentes do mesmo tom precisam de separação sutil sem elevação (ex.: divisor entre seções); **nunca** usar borda solta sem função — todo traço deve resolver um problema de agrupamento ou hierarquia real, nunca decoração.

### 4.1 Profundidade e hierarquia de superfícies

Terreno limpo: nenhum componente ou documento do Pro definia elevação, sombra ou scrim até esta
revisão (confirmado por grep em todo `android/pro/` e na skill `signallq-pro-design`). Mesma regra
de 4 níveis aplicada ao SignallQ consumer e ao Console — só os nomes de token do Pro mudam.

**Princípio.** Profundidade comunica hierarquia e interação, nunca decoração. Um card não deve
parecer elevado se não for interativo ou prioritário. Evitar sombra forte quando existir tema
escuro — priorizar elevação tonal. Sem glow permanente, sem glassmorphism como linguagem
principal, sem gradiente em todo componente (gradiente só em ação principal, estados especiais,
marca, ou visualização de dados quando necessário). Nunca misturar borda + sombra + glow +
gradiente no mesmo elemento. Profundidade igual para componentes equivalentes. Seleção usa
diferença de superfície + cor, não só sombra. Cards aninhados no máximo 2 níveis visuais. Botão
primário pode ter leve elevação, mas sem parecer desconectado do resto da tela. Navegação inferior
(`Navbar`) é superfície acima do conteúdo sem sombra pesada. Bottom sheets e modais têm
profundidade claramente superior à tela base.

| Nível | Papel | Token | Valor (tema claro) | Regra de aplicação |
|---|---|---|---|---|
| **0 — Fundo da tela** | Plano base, não compete com conteúdo | `ProBackground` (já existe) | `#F7F9FC` | Sem sombra, sem borda. Todo conteúdo repousa sobre este tom. |
| **1 — Conteúdo agrupado** | Cards comuns, métricas, listas | `ProSurface` (reaproveitado, **sem token novo**) | `#FFFFFF` | Diferença tonal já existe entre `#F7F9FC` e `#FFFFFF` — suficiente para separar plano sem sombra. Borda (`ProOutline`) só quando dois cards brancos ficam lado a lado sem espaçamento. Nunca sombra em card não-interativo. |
| **2 — Interativo/destacado** | Selecionado, recomendação prioritária, controle interativo em foco | `ProSurfaceContainerHigh` (já existe) para hover/pressed · `ProSurfaceSelected` (**novo**) para selecionado | `#E7ECF3` · `#EAF2FF` (novo) | Contraste tonal maior que o Nível 1. Pode ter borda de destaque suave (`ProPrimary` a baixa opacidade) e sombra discreta (elevação 2–4dp). `ProSurfaceSelected` é um azul quase imperceptível (tint de `ProPrimary` a ~6% sobre branco) — mais sutil que `ProPrimaryContainer` (`#D8E7FF`, reservado a chip/selo, não a fundo de card). |
| **3 — Sobreposto** | Dialogs, bottom sheets, menus, tooltips | `ProSurfaceOverlay` (**novo**) + `ProScrim` (**novo**) | `#FFFFFF` (tema claro, igual a `ProSurface` — diferenciação vem de scrim + elevação, não de cor de fundo) · scrim `rgba(0,0,0,.5)` claro / `.6` escuro (reaproveitado do consumer para consistência de marca entre produtos) | Separação clara do conteúdo abaixo via scrim escurecendo o fundo e sombra/elevação maior no próprio overlay (elevação 8–16dp conforme profundidade Material 3). Nunca aplicar em card comum. |

**Decisão de nomenclatura.** Optei por **reaproveitar `ProSurface` para o Nível 1** em vez de criar
um `ProSurfaceContainer` novo — a distância tonal `#F7F9FC` → `#FFFFFF` já é a diferença real usada
hoje por `EnvironmentCard`/`RecommendationBlock`/`StateCard`, e criar um terceiro tom quase idêntico
ao branco só adicionaria token sem ganho perceptível. Os únicos tokens realmente novos são
`ProSurfaceSelected` (Nível 2, estado selecionado — hoje inexistente, confundido com
`ProPrimaryContainer` que é de outro uso) e `ProSurfaceOverlay`/`ProScrim` (Nível 3, também
inexistentes).

**Tema escuro (alvo — ainda não implementado em código).** Superfícies mais elevadas devem ser
ligeiramente **mais claras** que o fundo (nunca preto absoluto em toda camada), com contraste
tonal progressivo entre os 4 níveis, sombra sutil complementar (não a única pista de elevação),
bordas com baixa opacidade quando necessárias, e cuidado para não produzir cards cinza idênticos
sobre fundo cinza idêntico — a mesma escala de 4 níveis se aplica, só troca-se o sentido da
progressão tonal (claro→escuro no lugar de escuro→claro).

**Tema claro (vigente).** Diferença de branco/cinza-claro entre planos já existe nos tokens atuais
(`#F7F9FC` → `#FFFFFF` → `#E7ECF3`); sombras, quando usadas (Nível 2 e 3), devem ser leves,
difusas e pouco opacas — evitar visual de "vários cartões flutuando"; usar borda sutil
(`ProOutline`) quando a sombra não for necessária para a distinção.

**Critérios de aceite.** Usuário percebe claramente fundo/conteúdo/seleção/sobreposição em
qualquer tela; card comum não parece modal; modal/bottom sheet se destaca visivelmente do
conteúdo abaixo; elemento interativo é identificável sem depender só de sombra; profundidade é
consistente entre todas as telas do Pro; tema escuro (quando existir) não pode parecer uma
coleção de retângulos cinza no mesmo plano.

---

## Produto

### 5. Arquitetura de informação

A navegação precisa refletir o trabalho real do profissional. O objeto principal não é "o teste"; é a **visita técnica**, que reúne contexto, ambientes, medições, evidências e resultado.

Cadeia conceitual: **01 Clientes** (quem recebe o serviço) → **02 Locais** (onde ocorre) → **03 Visitas** (quando e por quê) → **04 Ambientes** (onde foi medido) → **05 Laudo** (o que foi comprovado).

**Navegação principal**

| Destino | Conteúdo |
|---|---|
| Início | Visitas em andamento, ações rápidas, pendências e histórico recente. |
| Clientes | Cadastro, locais, contatos e histórico consolidado. |
| Nova visita | Fluxo guiado para levantamento, intervenção ou validação. |
| Relatórios | Laudos gerados, rascunhos e compartilhamentos. |
| Perfil profissional | Marca, assinatura, dados comerciais, plano e preferências. |

---

## Jornada

### 6. Fluxo principal da visita

Sequência guiada: **1 Preparar** (cliente, local e objetivo) → **2 Mapear** (ambientes e rede atual) → **3 Medir** (sinal, velocidade e estabilidade) → **4 Intervir** (mudanças e recomendações) → **5 Validar** (antes × depois) → **6 Entregar** (laudo e aceite).

**Tipos de visita**

- Diagnóstico inicial — identificar causa de lentidão, cobertura ruim, interferência ou instabilidade.
- Instalação / otimização — posicionar roteador ou mesh, ajustar canais, bandas, SSIDs e configurações.
- Validação pós-serviço — comprovar resultado após correções ou instalação.
- Vistoria técnica — registrar estado da rede e emitir parecer sem realizar intervenção.

**Status da visita**

- Rascunho — ainda sem execução.
- Em andamento — possui coleta ativa ou dados não finalizados.
- Aguardando validação — serviço executado, falta medição final ou aceite.
- Concluída — laudo final gerado.
- Cancelada — preserva motivo e histórico, mas não gera laudo final.

> **Regra.** Uma visita em andamento deve permanecer recuperável. Fechar o app ou perder conexão não pode apagar medições, fotos ou observações.

---

## Escopo

### 7. Inventário de telas do MVP

| ID | Tela | Objetivo |
|---|---|---|
| P01 | Onboarding profissional | Apresentação, cadastro e permissão de uso |
| P02 | Início | Agenda operacional e atalhos |
| P03 | Clientes | Busca, filtros e cadastro |
| P04 | Detalhe do cliente | Locais, contatos e histórico |
| P05 | Detalhe do local | Rede, equipamentos e visitas |
| P06 | Criar visita | Objetivo, escopo e dados iniciais |
| P07 | Ambientes | Lista e progresso por cômodo |
| P08 | Medição do ambiente | Sinal, velocidade, estabilidade e observações |
| P09 | Evidências | Fotos, legendas e anexos |
| P10 | Comparação | Antes × depois por ambiente e indicador |
| P11 | Resumo técnico | Achados, causas e recomendações |
| P12 | Editor de laudo | Identidade, seções e prévia |
| P13 | Laudo concluído | PDF, compartilhamento e aceite |
| P14 | Histórico | Visitas e relatórios anteriores |
| P15 | Perfil profissional | Logo, assinatura, contato e plano |

**Telas de referência no projeto online** (`foundations/screen-*.html`, claro vs. escuro lado a lado): Home, Walk Test, Medição por ambiente, Teste de velocidade, Atendimento, Histórico, Configurações. Laudo técnico é exemplificado sempre no tema claro.

---

## UI kit

### 8. Componentes essenciais

**Botões**

- Primário: uma ação dominante por tela.
- Secundário: ações alternativas sem competir com a principal.
- Texto: navegação leve, editar, ver detalhes.
- Destrutivo: exige rótulo explícito; nunca usar apenas ícone de lixeira.
- Ação persistente no rodapé apenas quando necessária para avançar no fluxo.

> **Proibido.** Card inteiro clicável com vários botões escondidos, ícones sem rótulo para ações críticas e três CTAs primários brigando na mesma tela.

### 8.1 Catálogo completo (15 componentes)

Fonte visual: projeto Claude Design `77a19317` (bundle React `window.SignallQPRODesignSystem_77a193`). Coluna "Implementação real" reflete o estado em `android/pro/core/designsystem/` — 9 de 15 já existem em Kotlin; os 6 restantes só existem no protótipo React e são débito registrado (ver fechamento deste documento).

| Componente | Finalidade | Variantes | Estados | Token de profundidade | Implementação real (Kotlin) |
|---|---|---|---|---|---|
| `TopBar` | Cabeçalho de tela, título + ação contextual | Padrão, com voltar, com ação de busca | Padrão, com badge de pendência | Nível 0/1 (integrado ao fundo, sem sombra) | **Não implementado** — só no bundle React |
| `Navbar` | Navegação inferior (Início · Atendimentos · Clientes · Ajustes) | — | Item ativo/inativo, com badge | Nível 1 (superfície acima do conteúdo, sem sombra pesada) | `Navbar.kt` |
| `Button` | Ação primária/secundária/texto/destrutiva | Primário, secundário, texto, destrutivo | Padrão, pressed, disabled, loading | Nível 1; primário pode ter leve elevação (2dp) | **Não implementado** — só no bundle React |
| `TextField` | Entrada de texto/número | Padrão, com ícone, multilinha | Vazio, preenchido, erro, disabled, foco | Nível 1, raio de controle 12dp | **Não implementado** — só no bundle React |
| `StatusChip` | Status de atendimento/visita/medição, sempre via preset `ATENDIMENTO_STATUS` | 6 presets de status (ver tabela abaixo) | — | Nível 1, pílula 999dp | `StatusChip.kt` |
| `ListRow` | Linha de lista (cliente, local, histórico) | Com ícone, com chevron, com metadado à direita | Padrão, pressed, disabled | Nível 1 (linha contínua, sem raio de card) | `ListRow.kt` |
| `VisitCard` | Cliente, local, objetivo, status, horário, ação de continuar | Rascunho, em andamento, aguardando validação, concluída, cancelada | Padrão, pressed | Nível 1; card raio 16dp | **Não implementado** — só no bundle React |
| `EnvironmentCard` | Nome do ambiente, tipo, progresso, resumo de métricas, estado | Com/sem foto, com/sem alerta | Não iniciado, em progresso, concluído | Nível 1; card raio 16dp | `EnvironmentCard.kt` |
| `QualityGauge` | Valor de medição, faixa, interpretação, contexto | Sinal, velocidade, latência, jitter, perda | Excelente/Bom/Atenção/Fraco/Crítico/Informação | Nível 1 (dentro de card) | `QualityGauge.kt` |
| `ComparisonBlock` | Antes × depois: valor, variação absoluta, percentual, conclusão | Por ambiente, por indicador agregado | Melhora, piora, inalterado | Nível 1; card raio 16dp | **Não implementado** — só no bundle React |
| `EvidenceChip` | Foto, observação, equipamento ou configuração vinculada | Com ícone de tipo (foto/nota/config) | Padrão, removível | Nível 1, pílula 999dp | `EvidenceChip.kt` |
| `SyncBanner` | Estado offline, itens pendentes, última sincronização | Offline, pendente, sincronizado | Persistente até resolver | Nível 1/2 (banner 12dp, destaca-se do fundo quando há pendência) | `SyncBanner.kt` |
| `RecommendationBlock` | Problema, impacto, ação sugerida, prioridade | Alta/média/baixa prioridade | Pendente, resolvida | Nível 1, pode subir a Nível 2 quando é a recomendação prioritária da tela | `RecommendationBlock.kt` |
| `SignatureBlock` | Assinatura/aceite: nome, data, confirmação, observação opcional | Assinatura digital, aceite por toque | Vazio, preenchido, confirmado | Nível 1; se em bottom sheet, Nível 3 | **Não implementado** — só no bundle React |
| `StateCard` | Estado vazio/erro/carregando/concluído de uma tela ou seção | Vazio, erro, carregando, concluído | — | Nível 1; card raio 16dp | `StateCard.kt` |

**Chips de status do atendimento** — usar sempre os presets do componente `StatusChip` (`ATENDIMENTO_STATUS`, bundle `window.SignallQPRODesignSystem_77a193` no projeto `77a19317`), nunca hex avulso:

| Estado | Uso visual | Ação |
|---|---|---|
| Solicitado | Chip neutro (Outline) | Confirmar ou propor horário |
| Confirmado | Chip de marca (Primary, azul) | Abrir visita / adicionar ao calendário |
| A caminho | Chip de apoio (Secondary ou Tertiary, discreto) | Iniciar deslocamento |
| Em atendimento | Chip de atenção (Warning) | Continuar visita |
| Concluído | Chip de sucesso (Success) | Gerar laudo e cobrar |
| Cancelado / não compareceu | Chip crítico (Error) | Registrar motivo |

---

## Dados

### 9. Apresentação das medições

O Pro pode mostrar mais informação que o app gratuito, mas isso não significa despejar telemetria crua. Cada métrica precisa responder: qual o valor, o que significa e o que fazer.

| Métrica | Unidade | Exibição mínima |
|---|---|---|
| Sinal Wi-Fi | dBm | Valor atual + faixa + banda + SSID/BSSID |
| Velocidade | Mbps | Download, upload e referência do plano |
| Latência | ms | Média, variação e destino do teste |
| Jitter | ms | Estabilidade para voz, vídeo e jogos |
| Perda | % | Impacto e duração da amostra |
| Canal | número / MHz | Ocupação, largura e interferência |
| Dispositivo | modelo / IP | Origem do dado e nível de confiança |

> **Importante.** Os limites devem considerar contexto, tipo de uso e banda. Um único corte universal produz diagnóstico burro.

### 9.1 Catálogo de estados semânticos

A escala cromática oficial do Pro (`--sqp-status-*`, seção 3) tem 6 níveis: Excelente, Bom,
Atenção, Fraco, Crítico, Informação. Para o contexto de diagnóstico técnico/visita, esse catálogo
se estende com dois estados **não-cromáticos** de disponibilidade de dado — que não competem com
a escala de qualidade, apenas indicam ausência ou incerteza:

| Estado | Cor (claro) | Cor (escuro) | Ícone | Texto | Fundo | Borda | Quando usar |
|---|---|---|---|---|---|---|---|
| Excelente | `#16A85A` | `#32D978` | check_circle | "Excelente" | `Success` container tonal | — | Medição supera o objetivo declarado com folga |
| Bom | `#1AA25A` | `#32D978` | check | "Bom" | `Success` container tonal | — | Atende ao objetivo declarado da visita |
| Atenção (≈"Regular") | `#E9AD27` | `#F5C451` | warning | "Atenção" | `Warning` container tonal | — | Funciona, mas apresenta risco ou margem pequena |
| Fraco (≈"Ruim") | `#ED7D2D` | `#FF964F` | error_outline | "Fraco" | `Warning`/`Error` intermediário | — | Compromete parte do uso, abaixo do esperado mas não crítico |
| Crítico | `#D9363E` | `#FF5F66` | cancel | "Crítico" | `Error` container tonal | — | Compromete o uso ou viola o critério definido |
| Informação (neutro) | `#0B6CFF` | `#3D96FF` | info | "Informação" | `Primary` container tonal | — | Dado neutro, não é veredito de qualidade |
| Indisponível | `ProOutline #C4CBD5` | — (alvo) | cloud_off / signal_cellular_off | "Indisponível" | `ProSurface`/`ProBackground`, sem tonal | `ProOutline` | Não foi possível coletar o dado (sem permissão, sem sinal, equipamento offline) |
| Desconhecido | `ProOutline #C4CBD5` | — (alvo) | help_outline | "Não avaliado" | `ProSurface`/`ProBackground`, sem tonal | `ProOutline` | Dado não coletado ainda ou amostra insuficiente para conclusão — não fingir certeza |

Cor nunca é o único sinal — todo estado combina ícone + rótulo textual + valor. "Indisponível" e
"Desconhecido" nunca usam as cores da escala de qualidade (verde/âmbar/vermelho/azul), para não
serem confundidos com um veredito real de medição.

---

## Confiabilidade

### 10. Evidências e rastreabilidade

Toda medição deve registrar contexto suficiente para ser auditável e comparável. A interface precisa fazer isso automaticamente sempre que possível.

| Elemento | Conteúdo |
|---|---|
| Contexto automático | Data, hora, local, ambiente, rede, banda, dispositivo e versão do app. |
| Foto | Imagem original, miniatura, legenda e vínculo com ambiente ou recomendação. |
| Observação | Texto curto, ditado opcional e categoria. |
| Alteração técnica | O que mudou, valor anterior, valor novo e responsável. |
| Confiança | Origem do dado e limitações da medição. |
| Sincronização | Identificador local, estado e registro do envio. |

**Modo offline**

- Salvar localmente imediatamente após cada coleta.
- Exibir claramente o que ainda não foi sincronizado.
- Não bloquear conclusão da visita por falta temporária de internet.
- Gerar PDF local quando tecnicamente possível; sincronizar depois.

---

## Entrega

### 11. Laudo profissional

O laudo é o produto final do trabalho. Ele deve ser compreensível para o cliente e tecnicamente defensável para o profissional.

**Estrutura mínima**

- Capa com marca SignallQ Pro e identidade do profissional.
- Cliente, endereço/local e dados da visita.
- Objetivo e escopo do serviço.
- Resumo executivo em linguagem simples.
- Ambientes avaliados e resultados principais.
- Problemas encontrados e evidências.
- Ações realizadas.
- Comparação antes/depois.
- Recomendações pendentes e prioridades.
- Limitações da avaliação.
- Assinatura ou aceite do cliente.

**Personalização.** Permitir logo, nome comercial, documento profissional ou empresarial, telefone, e-mail e assinatura. A personalização não pode apagar a indicação "Gerado com SignallQ Pro".

> **Regra editorial.** O resumo executivo fala com o cliente. O anexo técnico preserva as métricas completas. Misturar os dois deixa o laudo incompreensível para um e raso para o outro.

---

## Conteúdo

### 12. Linguagem e microcopy

A interface deve ser direta, profissional e explicativa. Evitar termos técnicos quando não agregam decisão; quando forem necessários, explicar no próprio contexto. Copy em PT-BR com "você", sentence case em títulos, sem emoji.

**Tom**

- Não culpar o usuário.
- Não prometer causa exata quando os dados só indicam hipótese.
- Distinguir claramente fato medido, inferência e recomendação.
- Usar verbos de ação em botões: Medir, Salvar ambiente, Gerar laudo.

| Preferir | Evitar |
|---|---|
| "O sinal neste cômodo está fraco e pode causar travamentos em vídeo." | "RSSI abaixo do threshold operacional detectado." |
| "Medição salva neste aparelho. Será sincronizada quando houver internet." | "Falha 503 no endpoint de persistência." |
| "Refaça o teste próximo ao ponto onde o cliente usa a rede." | "Amostra inválida. Tente novamente." |

---

## Padrões

### 13. Acessibilidade e qualidade

| Critério | Regra | Alvo |
|---|---|---|
| Contraste de texto | Mínimo 4,5:1 corpo/label, 3:1 para texto grande (≥18sp semibold) | Ambos os temas, incl. estados semânticos (seção 9.1) |
| Alvo de toque | Mínimo 48×48dp em todo elemento acionável, mesmo com ícone/conteúdo visual menor | Todos os componentes interativos (seção 4) |
| TalkBack / leitor de tela | Todo controle, chip, gráfico (`QualityGauge`) e card de conteúdo tem `contentDescription` ou `semantics{}` coerente; gráficos expõem valor+veredito em texto, não só a forma visual | 100% dos componentes de `core/designsystem/` |
| Ordem de foco | Segue a ordem de leitura visual (topo→baixo, esquerda→direita em PT-BR) | Toda tela nova |
| Reduced motion | Respeitar preferência de sistema; animações de transição/loading têm alternativa estática | Transições de tela, `StateCard` carregando |
| Escala de fonte (font scaling) | Sem corte ou sobreposição até 200% | Todo texto, especialmente `Título`/`Seção` em telas com espaço apertado |
| Cor não é único sinal | Todo estado semântico combina ícone + rótulo textual + valor (seção 9.1) | Estados de medição e de visita |
| Texto alternativo em foto | Toda foto relevante no laudo tem descrição textual | `EvidenceChip`, seção do laudo (11) |
| Feedback háptico | Opcional, nunca único retorno de conclusão de medição | Fim de teste/medição |
| Áreas seguras Android | Conteúdo nunca fica sob barra de navegação/status nativa | Toda tela, especialmente com `Navbar` fixo |
| Modo escuro (alvo) | Contraste validado por token, nunca mera inversão de cor | Ver seção 4.1 — ainda não implementado em código |

### 13.1 Responsividade

- **Tamanhos de tela.** Telefone é o alvo primário (visita em campo, uma mão); tablet é secundário — margem horizontal sobe de 16dp para 24dp (seção 4), mas a hierarquia de componentes não muda.
- **Densidade.** Ícones e alvos de toque em `dp`, nunca `px` fixo — já é a prática em todo `core/designsystem/`.
- **Gesture vs. button navigation.** Respeitar a área seguem do sistema (edge-to-edge com `WindowInsets`); `Navbar` nunca sobrepõe a barra de gestos.
- **Teclado.** Formulários (cadastro de cliente, observação de medição) devem manter o campo ativo visível acima do teclado; `TextField` (quando implementado) precisa de `imePadding()`.
- **Orientação.** Fluxo principal é retrato (visita em campo); paisagem é aceitável em telas de visualização de gráfico/comparação, não obrigatório suportar em formulários longos.

**Conteúdo simulado.** Diferente do consumer (que tem oferta/produto de afiliado simulado em
recomendação), o Pro **não tem** conteúdo comercial simulado no MVP — `RecommendationBlock` é
100% gerado a partir de medição real da visita, sem inventário de oferta/anúncio. Não aplicável
até que monetização de terceiros seja decidida (fora de escopo hoje).

**Checklist de aceite visual**

- Tela implementada comparada com a especificação em dispositivo-alvo.
- Estados vazio, carregando, erro, offline e conteúdo longo testados.
- Botões e campos funcionais, não apenas decorativos.
- Navegação de retorno e persistência da visita validadas.
- Capturas anexadas à evidência de teste.

---

## Manutenção

### 14. Governança e handoff

O design system deve ser uma fonte viva e versionada junto da plataforma. Componentes duplicados e decisões escondidas em telas isoladas viram dívida rapidamente.

**Fonte da verdade**

- Tokens visuais compartilhados entre SignallQ e SignallQ Pro (fundações comuns; matiz de marca divergente).
- Componentes comuns em módulo reutilizável; variações Pro configuráveis por propriedades.
- Especificações funcionais vinculadas às telas e componentes.
- Mudanças relevantes registradas em changelog curto.
- Componentes depreciados marcados antes da remoção.

**Critério para criar componente novo**

- O padrão aparece ou aparecerá em pelo menos três contextos.
- A variação não pode ser resolvida com propriedades de um componente existente.
- A função e os estados estão definidos, não apenas a aparência.
- Há responsabilidade clara de manutenção e teste.

**Decisão arquitetural.** SignallQ e SignallQ Pro devem compartilhar fundações, tokens e componentes de medição. Fluxos, entidades e navegação podem divergir. Copiar tudo e manter duas versões seria pedir para a inconsistência começar.

**Regras de governança adicionais (mesmas do consumer, aplicadas ao Pro):**

- Nenhum componente novo sem antes checar `core/designsystem/` (9 já implementados, seção 8.1) e o bundle React do projeto `77a19317`.
- Nenhum valor de cor, raio ou espaçamento hardcoded em tela — sempre via `MaterialTheme.colorScheme.*` ou token `Pro*` (já é o padrão observado nos 9 componentes reais).
- O design system é fonte única da verdade — divergência entre protótipo React e Kotlin real vira débito registrado, nunca duas implementações "quase iguais" convivendo.
- Protótipo e implementação usam os mesmos nomes de componente e de token (`ProSurfaceSelected`, `ProSurfaceOverlay`, `ProScrim` precisam nascer com esse nome em Kotlin quando implementados, não sinônimos).

### 14.1 Mapeamento Jetpack Compose

| Componente | Finalidade | Props principais | Token de cor | Implementação real |
|---|---|---|---|---|
| `TopBar` | Cabeçalho de tela | título, ação de voltar, ação contextual | `MaterialTheme.colorScheme.background/onBackground` | Não implementado |
| `Navbar` | Navegação inferior | destino ativo, badge | `ProSurface`, `ProPrimary` (item ativo) | `Navbar.kt` |
| `Button` | Ação | label, variante, loading, enabled | `ProPrimary`/`ProSecondary`/`ProError` | Não implementado |
| `TextField` | Entrada | value, label, erro, ícone | `ProOutline`, `ProError` | Não implementado |
| `StatusChip` | Status de atendimento | preset `ATENDIMENTO_STATUS` | `ProPrimary`/`ProSecondary`/`ProTertiary`/`ProWarning`/`ProSuccess`/`ProError` | `StatusChip.kt` |
| `ListRow` | Linha de lista | ícone, título, subtítulo, trailing | `ProSurface`, `ProOutline` (divisor) | `ListRow.kt` |
| `VisitCard` | Card de visita | cliente, local, status, horário | `ProSurface`, `StatusChip` embutido | Não implementado |
| `EnvironmentCard` | Card de ambiente | nome, tipo, progresso, estado | `ProSurface`, `ProSurfaceContainerHigh` (progresso) | `EnvironmentCard.kt` |
| `QualityGauge` | Medidor de qualidade | valor, faixa, veredito | Escala de sinal 6 níveis (seção 3) | `QualityGauge.kt` |
| `ComparisonBlock` | Antes/depois | valor anterior, valor novo, delta | `ProSuccess`/`ProError` (variação) | Não implementado |
| `EvidenceChip` | Evidência vinculada | tipo, legenda, removível | `ProSurface`, `ProOutline` | `EvidenceChip.kt` |
| `SyncBanner` | Sincronização | estado, pendentes, timestamp | `ProWarning` (pendente), `ProSuccess` (sincronizado) | `SyncBanner.kt` |
| `RecommendationBlock` | Recomendação | problema, impacto, ação, prioridade | `ProWarning`/`ProError` conforme prioridade | `RecommendationBlock.kt` |
| `SignatureBlock` | Assinatura/aceite | nome, data, confirmação | `ProSurface`, `ProOutline` | Não implementado |
| `StateCard` | Estado de tela/seção | tipo (vazio/erro/carregando/concluído) | `ProOutline` (vazio), `ProError` (erro), `ProSuccess` (concluído) | `StateCard.kt` |

---

## Evolução do produto

### 15. Conta, acesso e primeiro uso

O SignallQ Pro exige identidade persistente para sincronizar clientes, visitas, assinatura, recibos e configurações profissionais. O acesso deve ser simples, mas não pode depender exclusivamente de uma conta externa.

- Usar Google Credential Manager no Android para login Google.
- Permitir vincular Google e senha local à mesma conta (Identidade / `IdentityProvider`).
- Nunca exibir diferença visual que faça a conta local parecer inferior ou insegura.

| Tela | Conteúdo principal | Ação primária |
|---|---|---|
| Boas-vindas | Benefício do produto, privacidade e opções de acesso | Continuar com Google / Entrar com e-mail |
| Criar conta | Nome, e-mail, senha e aceite dos termos | Criar conta |
| Verificar e-mail | Código ou link de confirmação | Confirmar e-mail |
| Perfil profissional | Nome público, documento opcional, contato, logo e cidade | Concluir perfil |
| Recuperar acesso | E-mail e redefinição segura de senha | Enviar instruções |

### 16. Planos, limites e assinatura

O produto possui dois níveis de acesso: Free e Pro. A interface deve explicar o valor do upgrade no contexto da tarefa, sem bloquear o fluxo com pop-ups agressivos.

- Exibir mensal e anual como duas opções do mesmo plano Pro.
- Mostrar preço anual equivalente por mês, desconto total e cobrança anual claramente (**valores pendentes — preço do Pro não definido, Canônico §8.1**).
- Usar estados ACTIVE, GRACE_PERIOD, PAUSED e EXPIRED; evitar um simples `isPro`.

| Capacidade | Free | Pro |
|---|---|---|
| Clientes ativos | Até 3 | Ilimitados |
| Visitas mensais | Até 3 | Ilimitadas ou política de uso justo |
| Laudo | Modelo padrão com marca SignallQ Pro | Completo e personalizado |
| Fotos e evidências | Limitadas | Completas |
| Histórico | Janela reduzida | Completo |
| Agenda integrada | Adicionar ao calendário | Sincronização e gestão avançada |
| Recibos e Pix | Incluídos | Incluídos com histórico financeiro completo |
| Nuvem e backup | Básico | Completo |

### 17. Agenda e origem via WhatsApp

O SignallQ Pro controla o ciclo do atendimento, mas não recria um calendário completo no MVP. A agenda externa organiza horários; o aplicativo transforma o compromisso em cliente, local, visita, laudo, pagamento e histórico.

- MVP1: criar evento por intent no calendário escolhido pelo técnico.
- MVP2: conectar Google Agenda e manter vínculo por `externalEventId`.
- Futuro: link público compartilhável no WhatsApp para solicitar atendimento.

### 18. Pagamentos, Pix e recibo digital

O Pix no MVP é estático e gerado localmente. Não existe confirmação bancária automática. O técnico declara manualmente o recebimento e o aplicativo emite o recibo com rastreabilidade.

- Recibos emitidos são imutáveis; correção ocorre por cancelamento e nova emissão.
- Permitir pagamento parcial e saldo pendente.
- Não enviar chave Pix, CPF/CNPJ ou dados financeiros para analytics ou logs.

| Tela | Elementos obrigatórios |
|---|---|
| Configurar Pix | Tipo de chave, chave, recebedor, cidade, descrição padrão e opção de ocultação |
| Cobrança | Valor, descrição, QR Code, código copia e cola, compartilhar e marcar como pago |
| Confirmar recebimento | Valor, método, data, integral/parcial e confirmação explícita |
| Recibo | Número, pagador, profissional, serviço, valor por extenso, método, visita, hash e status |

> **Regra de confiança.** O aplicativo deve escrever "pagamento informado pelo profissional" e nunca "pagamento confirmado pelo banco" sem integração financeira.

---

*SignallQ Pro — diagnóstico que vira serviço; medição que vira evidência.*

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, eventos, tokens e decisões (prevalece sobre este).
- `08_SignallQ_Pro_Especificacao_Funcional_v5.md` — visão, entidades, módulos e regras de negócio.
- `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` — jornada e catálogo de telas.
- `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` — fases, gates e sequência de implementação.
