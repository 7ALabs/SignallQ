# Auditoria — App Android SignallQ × Spec To-Be (Fluxo de Telas)

**Data original:** 2026-07-13 · **Reauditoria (v2, esta versão):** 2026-07-13
**Referência de design:** `SignallQ App - Fluxo de Telas.dc.html` (Claude Design, projeto "SignallQ Design System", `e77ea465-291f-4bf5-930c-a267680da04e`)
**Código auditado:** `android/app/src/main/kotlin/io/signallq/app/ui/` — worktree `fix/design-tobe-alinhamento`, baseado em `origin/main` (commit `2957f054` + correções de paleta/skill desta branch)
**Método:** 7 agentes paralelos, cada um cobrindo um bloco de telas, comparando hex/tipografia/espaçamento/estrutura contra a spec extraída, com citação de arquivo:linha. Auditoria somente leitura.

## ⚠️ Nota sobre a v1 deste documento (correção de erro de processo)

A primeira versão desta auditoria (mesma data) foi rodada contra a branch `design-system/fase-2-bottom-sheets`, que estava **120 commits atrás de `origin/main`** — sem que isso fosse verificado antes de disparar os agentes. Boa parte dos achados "estruturais" da v1 relatava como **ausente** algo que já tinha sido implementado em `main` nas últimas semanas: hub Ferramentas, tela universal de Equipamento de internet, fluxo completo de Jogos, NativeAd em várias telas, remoção do código morto da IA conversacional, e até o rename de pacote pra `io.signallq.app`. Essa versão (v2) foi reauditada do zero contra a base correta e **substitui integralmente a v1** — nenhum achado da v1 deve ser usado sem reconferência aqui.

---

## Resumo executivo

O gap real de hoje contra a spec To-Be tem três naturezas bem diferentes, e cada uma pede um tipo de correção diferente:

**1. Divergência sistêmica de tema (afeta praticamente todas as telas).** `SignallQTheme.kt` (`LkColors`/`LkTokens`/`signallQTypography`) usa paleta e escala tipográfica próprias, não os tokens MD3 da spec — mesmo depois da skill/CLAUDE.md já terem sido corrigidos nesta branch para `primary=#5B21D6`. O código continua em `#6C2BFF`. Essa é a Fase 0 natural: migrar `SignallQTheme.kt` uma vez resolve a maior parte das divergências de cor/tipografia relatadas tela a tela abaixo (não repetidas por tela neste documento).

**2. Bugs de regra de negócio reais, específicos e ainda presentes:**
- **3e/3f (permissão de localização/telefonia):** o dismissal do sheet é `remember{}` — só de sessão. Reabre a cada nova sessão do app mesmo com negação permanente do Android. Confirmado por dois agentes independentes (v1 e v2).
- **2b-i (Conectar ao roteador):** segue sem validação de formato de IP/alcançabilidade antes de autenticar, e sem o botão "Ver modelos compatíveis".
- **2b-iii (Modelos compatíveis):** tela inteira ainda ausente — só existe o modelo de dados (`PublicCompatibilityCatalog.kt`), sem UI nem botão de entrada.
- **2b (Roteador):** para o caso comum (roteador Wi-Fi comum, não mesh/extensor), o tap nunca abre o sheet somente-leitura "Roteador da casa" da spec — vai direto para conectar ou para Equipamento de internet. Pode ser decisão de produto deliberada (fusão com 2b-i/5b); precisa confirmação antes de "corrigir".
- **5g (Jogos):** `NativeAd` está **completamente ausente** da tela de Resultado — a spec é explícita que essa é a única superfície onde publicidade nativa deveria aparecer.
- **6e (Novidades):** changelog carregado de asset local embutido no APK, não de CMS/JSON remoto versionado como a spec exige.

**3. Divergências de estrutura visual/componente (telas já existem e funcionam, mas não seguem o desenho exato da spec)** — a maior parte do restante deste documento. Grande parte é "componente existe mas usa forma/token diferente" (badges com `radius 4dp` em vez de pílula, Segmented virando `FilterChip` sem borda, ícones quadrados em vez de circulares, etc.) — mecânico de corrigir, mas extenso.

**Achado à parte, não descoberto na v1:** a tela **1a (Análise detalhada)** não existe mais como componente único — o app foi deliberadamente reestruturado em dois sheets diferentes (`DiagnosticoDetalhadoSheet` + `AnaliseDetalhadaBottomSheet`), com comentários no código referenciando decisões de produto (GH#536/#931/#833). Antes de "corrigir" essa tela para bater com o protótipo, vale confirmar se a spec está desatualizada em relação a essa decisão, não o inverso.

**Achado positivo:** a remoção da IA conversacional (tela 7) já aconteceu de verdade (PR #912) — telas, rotas e write-path do banco de chat estão mortos, não só escondidos. Sobra limpeza residual pequena: 11 componentes órfãos, uma segunda via de chat morta em `MainViewModel`/`DiagnosticoViewModel`, e o flag `FEATURE_DIAGNOSTICO_CHAT=true` em release (inofensivo hoje — nada lê `FeatureFlags.DIAGNOSTICO_CHAT` — mas deveria ser removido, não deixado "true" por engano).

---

## Achados transversais (não repetidos por tela)

### Paleta — hex não bate com a spec (mesmo já corrigida na skill/CLAUDE.md desta branch)

| Token | Spec (claro) | `SignallQTheme.kt` |
|---|---|---|
| primary | `#5B21D6` | `LkColors.accent = #6C2BFF` |
| success | `#146C2E` | `#22C55E` |
| warning | `#8A5000` | `#F5A623` |
| error | `#BA1A1A` | `#FF4D4F` |
| onSurface | `#1C1B1F` | `#0D0D1A` |
| onSurfaceVariant | `#49454F` | `#6B7280` |
| outline / outlineVariant | `#79747E` / `#CAC4D0` | um único token `border = #E5E7EB` |
| surfaceContainer (5 níveis) | `#F8F5FB`…`#E6DDF2` | 2-3 níveis (`bgPrimary`/`bgCard`/`bgSecondary`), sem granularidade |

### Tipografia — escala não bate, e nem sempre os próprios tokens do app são usados

`headlineLarge` 24sp/SemiBold (spec: 26/32/700), `headlineSmall` 18sp/SemiBold (spec: 22/28/600), `titleLarge` 16sp/Medium (spec: 20/26/600), `titleMedium` 15sp (spec: 16px), `labelMedium`/`labelSmall` peso 400 (spec: peso 500). Muitas telas (Privacidade, Novidades, Laudo, Histórico, Ferramentas, Jogos) usam `fontSize`/`fontWeight` **hardcoded** em vez de `MaterialTheme.typography.*` — nem a escala própria do app é seguida de forma consistente.

### Forma — Badge/Chip não são pílula em quase nenhuma tela

Spec pede `radius 999px` para Badge/Chip. Implementado consistentemente com `RoundedCornerShape(4-6dp)` em Sinal, Dispositivos, Ferramentas, DNS — parece convenção deliberada do design system atual do app, não acidente pontual, mas diverge do documento de referência em praticamente toda tela com badge.

### Grabber duplicado (suspeita, checar visualmente)

Vários `ModalBottomSheet` (sheets de permissão 3e/3f, sheets em `HomeScreen.kt`) não passam `dragHandle = {}`, então o handle padrão do M3 pode estar sendo desenhado **junto** com o `SheetDragHandle()` customizado do app. Outras sheets do app (`NetworkDetailSheet`, `ChannelDetailSheet`) já usam o padrão correto — inconsistência pontual, fácil de confirmar com um build.

---

## Por tela

### 0 · Onboarding
`OnboardingScreen.kt`
- ✅ Fonte Google Sans Flex conforme; fluxo de 2 páginas via `HorizontalPager` bate estruturalmente com a spec.
- ⚠️ CTA da página 1 chama-se "Continuar" (spec pede "Começar"); CTA final da página 2 também "Continuar" (spec pede "Concluir"). Círculo do logo usa `Color.White` fixo em vez do token de superfície do tema (quebra em dark mode). Estados de permissão usam "Autorizado"/"Pendente" em vez de "Permitido"/"Não permitido". Ícones via Material Icons padrão, não Material Symbols Outlined (variable font) como a spec pede.
- Dialog extra "Seguir sem permissões?" não previsto na spec — aditivo, não regressão.

### 1 · Velocidade (Speed Test)
`SpeedTestScreen.kt`, `VelocidadeScreen.kt`, `ResultadoVelocidadeScreen.kt`, `GaugeCircular.kt`
- ✅ NativeAd presente e corretamente restrito ao estado idle (`NativeAdRow`); cartão "Último resultado" com 3 colunas bate estruturalmente; motor de teste compartilhado corretamente.
- ⚠️ Botão idle 210dp real (spec pede 230px); anel running 220dp/stroke 8dp (spec pede 240px/14dp); 3 pílulas de fase em vez de 4; Segmented de modo é visualmente um pill com sombra, não borda 1px MD3; 3ª opção chama-se "Triplo" (spec: "3 testes"). Cores de fase fixas (`#60A5FA`/`#34D399`/`#FBBF24`) não trocam claro/escuro e divergem do hex da spec.
- ❌ Bloco "Experiência de uso" (3 linhas Verdict) não existe na tela de Resultado — foi movido para dentro do sheet de diagnóstico; toggle "Ver métricas detalhadas" não existe na tela principal (5 MetricCards são mostrados direto).

### 1a · Análise detalhada
**Não existe como componente único.** Ver achado destacado no resumo executivo — o app tem `DiagnosticoDetalhadoSheet` (título "Diagnóstico detalhado", sem banner de veredito colorido, sem skeleton loading, sem seção "Configurações" separada) e `AnaliseDetalhadaBottomSheet` (fluxo de sintoma escolhido pelo usuário, conceito diferente do da spec). Recomendo decisão de produto antes de tratar como bug.

### 1b · Falar com a operadora
`OperadoraBottomSheet.kt`
- ✅ Botão WhatsApp `#25D366`/peso 700 exatamente conforme; botões outlined lado a lado; lista nacional/regional com selo correto; persistência de senha de roteador via Keystore (nota: essa parte é de 2b-i, citada aqui por engano do agente — confirmar).
- ⚠️ Título em `headlineMedium` (spec pede `headlineSmall`); nome da operadora em `titleLarge` (spec pede `titleMedium`).

### 2 · Início
`HomeScreen.kt`
- ✅ Trilha clicável abre os sheets corretos; "Medir agora" reaproveita o motor de teste da tela 1; cartão "Última medição" com sparkline.
- ⚠️ Nós da trilha 56dp (spec pede 52px); TopBar customizada mais rica que a spec (não é regressão, é extensão de produto); banners de CGNAT/Anatel não previstos na spec.

### 2a · Meu dispositivo
- ✅ 4 campos na ordem certa; não depende do gateway.
- ⚠️ Título com peso 700 forçado (spec pede 600); valor em `bodyMedium`+W600 em vez de `titleSmall`; sem `SheetDivider` entre linhas.

### 2b · Roteador (Gateway)
- ⚠️/❌ Ver achado no resumo executivo — sheet somente-leitura existe mas não é o destino do tap para roteador Wi-Fi comum (caso principal da spec).

### 2b-i · Conectar ao roteador
- ✅ 3 estados internos, toggle de visibilidade de senha, "Manter conectado" força "Lembrar senha", senha via `EncryptedSharedPreferences`/Keystore (`CredenciaisModemStore.kt`) — confirmado, não texto puro.
- ❌ Sem Segmented visual de estado; sem botão "Ver modelos compatíveis"; **sem validação de formato de IP/alcançabilidade antes de autenticar** (regra de negócio da spec, ainda violada).

### 2b-ii · Guia de credenciais
- ✅ 4 passos com círculo 44px + ícone + "Passo N"; conteúdo estático com fallback genérico.
- ⚠️ Gap de 16dp entre passos (spec pede 24px); "Passo N" em `labelSmall` (spec pede `labelMedium`).

### 2b-iii · Modelos compatíveis
- ❌ Continua totalmente ausente — nenhum Composable, nenhuma referência textual, nenhum botão de entrada.

### 2c · Internet / Provedor
- ✅ 5 linhas na ordem certa; DNS Privado colorido corretamente quando ativo; detecção de CGNAT real (RFC 6598), não decorativa.

### 2d · Rede móvel (chip)
- ✅ 8 campos completos; ASU/SINR calculados corretamente.
- ⚠️ Título em `headlineMedium` (spec pede `headlineSmall`).
- ❌ Sem redirecionamento para fluxo de permissão quando `READ_PHONE_STATE` ausente — não confirmado dentro do sheet (pode estar upstream, checar com Camilo).

### 2e · Medir agora
- ✅ 3 opções com badges corretos; regra "Triplo só em Wi-Fi" implementada; botão Cancelar.
- ⚠️ Ícone em bloco quadrado arredondado, não círculo; título em token diferente do `headlineSmall` esperado.

### 3 · Sinal
`SinalScreen.kt`
- ✅ **4ª tab "Dispositivos" duplicada já foi removida** (achado da v1 não se aplica mais — confirmado, `SinalTopTabRow` só lista Wi-Fi/Canal/Móvel).
- ⚠️ Segmented de banda é na prática uma linha de Chips, não o componente Segmented com borda; badges não são pílula.
- ❌ Botão "Falar com a [operadora]" ausente na aba Móvel.

### 3a · Rede vizinha
- ✅ Estrutura de 6 linhas + divisores; cartões de alerta/dica condicionais presentes.
- ⚠️ Valor em `bodyMedium` em vez de `titleSmall`; opacidades de fundo diferentes das especificadas.

### 3b · Canal Wi-Fi
- ✅ Estrutura completa (badges, Status, Análise, Detalhes Técnicos) bate bem com a spec.
- ⚠️ Título em `headlineMedium` em vez de `headlineLarge`.

### 3c · Ponto de acesso / mesh
- ✅ Estrutura quase idêntica à spec (cabeçalho, campo de apelido, seção Rede).
- ⚠️ Aviso informativo usa tom de warning/âmbar em vez do neutro que a spec pede.

### 3d · Dispositivo cliente
- ✅ Estrutura completa, incluindo "Descoberto via" com cor condicional correta.
- ⚠️ Container do ícone é quadrado (12dp radius) em vez do círculo que a spec pede especificamente para esta tela.

### 3e · Permissão de localização / 3f · Permissão de telefonia
- ✅ Ícones/CTAs corretos; 3f corretamente sem estado "bloqueada" alternável.
- ❌ **Regra "não repetir se negado permanentemente" segue violada** — dismissal é `remember{}` de sessão, não persistido (`localizacaoSheetDismissed`/`telefoniaSheetDismissed`, `SinalScreen.kt`). Grabber duplicado suspeito (falta `dragHandle = {}`).

### 4 · Histórico
`HistoricoScreen.kt`
- ✅ Persistência local via Room; NativeAd presente (via #555).
- ⚠️ NativeAd não é o primeiro item da lista (é o 4º, decisão documentada em comentário referenciando #555 — confirmar se foi aprovada); Segmented Todos/Wi-Fi/Celular é `FilterChip`, não o componente pílula da spec; cards de item mais ricos que o especificado (gráfico, médias, tendência — extensão de produto).
- ❌ Overline "Medições recentes" ausente.

### 4a · Detalhe de teste
- ✅ Todos os blocos da spec presentes (cabeçalho, métricas primárias/secundárias, linhas rotuladas, diagnóstico) — só tokens de tamanho/raio divergentes (título em 16sp em vez de 20px, métrica primária em 34sp em vez de 26px).

### 4b · Exportar histórico
- ✅ **Exportação real** (CSV/PDF locais, share sheet nativo, filtro de período de fato aplicado — nada placeholder).
- ❌ Segmented de estado "Seleção/Exportando" ausente como componente visual (só texto do botão + barra de progresso indicam o estado).

### 5 · Ferramentas (hub)
`FerramentasScreen.kt` — **já existe** (achado da v1 estava errado).
- ✅ 7 itens corretos (Dispositivos, Equipamento de internet, Ping, DNS, Laudo, Monitoramento, Jogos).
- ⚠️ Layout é grade 2 colunas, spec pede lista vertical full-width; ícone circular em vez de quadrado arredondado 12px; sem chevron trailing.

### 5a · Dispositivos
- ✅ Estrutura geral, DeviceRow com ícone/badge/IP na maioria dos casos.
- ⚠️ Sem Segmented Lista/Vazio/Erro visível; sem borda inferior no cabeçalho; badges com radius 4dp; NativeAd na última posição da seção "Dispositivos", não em `floor(total/2)` da lista completa — decisão documentada (issue #555/feedback do Luiz 2026-07-12), não bug.

### 5b · Equipamento de internet
`EquipamentoInternetScreen.kt` — **já existe como tela universal** (achado estrutural mais grave da v1 estava errado — isso já foi construído).
- ✅ **Regras de negócio corretas**: enum único "Acesso ao equipamento" com os 6 valores exatos da spec (`AcessoEquipamento.kt`, reutilizável mas hoje só consumido aqui); diálogo de confirmação antes de reiniciar; detecção de Double NAT cruzando UPnP IGD real com modo da ONT (não decorativa).
- ⚠️ Visual: `StatusCard`/`DeviceSelector`/`Topology` da spec não existem como componentes nomeados — o corpo reaproveita `LocalDeviceSection` (lista plana) em vez da estrutura visual detalhada da spec. Loading é spinner central, não skeleton pulsante.

### 5c · Ping
- ✅ ~20 amostras conforme a nota de implementação; 3 PingMetricCard lado a lado.
- ⚠️ Continua como `ModalBottomSheet`, não migrou para tela cheia como 5b/5d já fizeram (comentário no próprio `DnsScreen.kt` confirma que Ping ficou pra trás dessa migração); sem Segmented Coletando/Resultado.

### 5d · DNS
`DnsScreen.kt`
- ✅ Estrutura muito próxima da spec; acordeão "Quando vale a pena trocar DNS?" com marcador circular 5px exato; guia é só instrucional (não altera DNS via API), conforme a nota de implementação; Tabs Dispositivo/Roteador conforme.
- ⚠️ Cores/tamanhos hardcoded, próximos mas não exatos aos tokens.

### 5e · Laudo
- ✅ Estrutura de TopBar/banner/metadados/seções completa; grade de 6 métricas em 2 colunas × 3 linhas bate exatamente com a spec.
- ❌ Botão "Compartilhar laudo em PDF" no rodapé ausente — a única ação de compartilhar é o ícone da TopBar.

### 5f · Monitoramento
`MonitoramentoSheet.kt`
- ✅ InlineToggleRow com diálogo de confirmação antes de ativar (opt-in explícito, correto); sub-lista de notificações condicional presente.
- ❌ **Continua sendo sheet compartilhado**, não tela dedicada — apesar de um comentário no código dizer "Fase 7 MD3 (5f) destino único" (comentário desatualizado em relação à implementação real). Nota de bateria só aparece condicionalmente por fabricante, não incondicional como a spec sugere.

### 5g · Jogos
`JogosScreen.kt`, `JogosViewModel.kt`, `JogoConexaoEngine.kt`, `GameCatalog.kt` — **fluxo completo já existe** (achado da v1 de "0% implementado" estava errado).
- ✅ **Regras de negócio corretas e bem verificadas**: 5 etapas reais (Plataforma→Jogo→Confirmar→Progresso→Resultado); teste nunca inicia automaticamente; troca de jogo sempre descarta resultado anterior; avaliação pondera perda>jitter>estabilidade>latência (nunca por download isolado); nunca pinga site institucional do jogo (usa `REGIONAL_ESTIMATE` de fato, mesmo quando o jogo declara `PROVIDER_NETWORK`).
- ❌ **NativeAd completamente ausente na tela de Resultado** — a spec é explícita que essa é a única superfície onde publicidade nativa é permitida em todo o fluxo de Jogos.
- ⚠️ Visual diverge bastante do desenho da spec em quase toda etapa: Passo 1 sem indicador de seleção persistente; Passo 2 sem sigla monoespaçada/badge de plataforma; Passo 4 mostra 1 spinner em vez das 4 linhas de etapa com check_circle (intervalo real 3000ms, spec sugere ~800ms); Passo 5 sem banner colorido por veredito, métricas em lista em vez de grade, sem métrica "Estabilidade" explícita.

### 6 · Perfil / Ajustes
`AjustesScreen.kt`
- ⚠️ Estrutura ainda diverge da spec: 6 seções em lista plana sem cartão de fundo (spec pede 4 Sections agrupadas em cartão `surfaceContainer`); "Dados e privacidade" fragmentado em duas linhas espalhadas por seções diferentes, não uma seção própria.
- ⚠️ Avatar do cabeçalho 56dp (spec pede 52px).

### 6a · Meu perfil
- ✅ Avatar editável 80dp correto.
- ❌ Botão de câmera sobreposto ao avatar ausente.
- ⚠️ Conteúdo extra (Operadora/IP/Conexão/Localização/Versão) não previsto na spec para esta tela.

### 6b · Minha conexão
- ✅ Estrutura completa (3 SectionCard + botão Salvar) bate bem com a spec.

### 6c · Dados e privacidade
- ✅ **O ponto crítico está correto**: 3 ActionButton em gravidade crescente (warning outline → error outline → error preenchido), cada um com diálogo de confirmação, efeito real confirmado no ViewModel.

### 6d · Privacidade
- ✅ Estrutura completa (TopBar, bloco de destaque, 3 seções, linha final de gerenciar dados).
- ⚠️ Vários textos com `fontSize` hardcoded, nem batendo com os próprios tokens do app.

### 6e · Novidades
- ✅ Estrutura completa (cabeçalho, lista com selos, separadores).
- ❌ **Changelog continua vindo de asset local embutido no APK** (`context.assets.open("changelog.json")`), não de CMS/JSON remoto versionado — regra de negócio da spec ainda violada.

### 6f · Sobre o SignallQ
- ⚠️ 5 linhas em vez das 3 pedidas (Versão, Plataforma, Desenvolvido por, Suporte, Licenças de terceiros) — extensão de conteúdo, não regressão.

### 7 · SignallQ AI (descontinuada)
- ✅ **Remoção principal já aconteceu de verdade** (PR #912): telas de chat, rotas e write-path do banco de dados de chat estão mortos, não só inacessíveis — confirmado por grep completo.
- ⚠️ Resíduo de limpeza: 11 componentes de UI órfãos (`SignallQAiMessageBubble.kt` e afins), uma segunda via de chat morta conectada mas nunca renderizada (`DiagChatEntry`/lógica em `MainViewModel`/`DiagnosticoViewModel`), entidades Room de chat ainda no schema sem write-path real.
- ❌ `FEATURE_DIAGNOSTICO_CHAT=true` continua no flavor de release (`app/build.gradle.kts:198`) com comentário no código dizendo que deveria estar desligado. Mitigante: nada lê `FeatureFlags.DIAGNOSTICO_CHAT` hoje, então não liga nenhuma UI — mas é um flag órfão e enganoso que deveria ser removido junto com o resto da limpeza.

---

## Decisões que precisam de confirmação antes de "corrigir" (não são bugs óbvios)

1. **1a (Análise detalhada)** — app reestruturado deliberadamente em 2 sheets diferentes, documentado no código. A spec pode estar desatualizada.
2. **2b (Roteador)** — tap no roteador Wi-Fi comum nunca abre o sheet somente-leitura da spec; pode ser fusão intencional com 2b-i/5b.
3. **4 (Histórico)** — posição do NativeAd (4º item, não o primeiro) tem decisão documentada referenciando #555; confirmar se foi aprovação formal.
4. **5a (Dispositivos)** — posição do NativeAd (fim da seção, não `floor(total/2)` da lista completa) também tem decisão documentada (issue #555 + feedback do Luiz em 2026-07-12).
