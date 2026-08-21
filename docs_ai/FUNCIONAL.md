---
title: "SignallQ Consumer — Documentação Funcional"
description: "O que o app Android SignallQ (io.signallq.app) entrega ao usuário final: navegação real, telas, funcionalidades por domínio, permissões e limitações."
type: "funcional"
status: "ativo"
owner: "Claudete"
last_updated: "2026-08-20"
---

- **Fonte de verdade:** o código do app consumer em `android/app/src/main/kotlin/io/signallq/app/`
  (caminho físico legado; o package declarado é `io.signallq.app` — dívida conhecida, ver
  `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.1), com os módulos `:core*` e
  `:feature*` consumidos por ele. Este documento foi reescrito do zero em 2026-08-06 lendo o código,
  e substitui integralmente a revisão anterior (2026-07-23).
- **Escopo:** app consumer Android `io.signallq.app` — telas, navegação, funcionalidades,
  permissões e limitações visíveis ao usuário final.
- **Fora do escopo:** SignallQ Pro (descontinuado permanentemente, ver ADR-016), painel
  Admin (repositório `buildea-admin`), site/PWA (repositório `signallq-web`) e arquitetura interna
  (ver `docs_ai/TECNICO.md`).
- **Responsável:** Claudete (documentação funcional). Implementação: Camilo. Revisão independente:
  Caio.
- **Versão validada:** versionName `0.31.0`, versionCode `72` (`android/gradle/libs.versions.toml:5-6`);
  `applicationId = "io.signallq.app"` (`android/app/build.gradle.kts:85`).

---

## 1. Objetivo

O SignallQ é um app Android gratuito que mede a internet do usuário (velocidade, latência, jitter,
estabilidade, bufferbloat), analisa a rede em volta dele (Wi-Fi, canais, rede móvel, dispositivos
conectados, DNS, equipamento de fibra) e traduz tudo em veredito humano e próximo passo concreto —
sem exigir que a pessoa saiba interpretar Mbps, dBm ou milissegundos. Toda decisão de veredito vem
de um motor local determinístico rodando no aparelho; a IA remota só escreve a explicação em prosa
do que o motor já concluiu, nunca decide o status.

---

## 2. Contexto e problema

Quando a internet está ruim, o usuário residencial não tem como saber onde está a falha: pode ser o
Wi-Fi do cômodo, o canal congestionado do prédio, o roteador, a ONT da operadora, o DNS, a rede
móvel ou o próprio provedor. Os apps de velocímetro entregam números crus e param aí — o usuário
fica com "42 Mbps" e nenhuma conclusão. O resultado prático é que ele não consegue nem melhorar a
própria rede, nem abrir um chamado embasado com a operadora.

O SignallQ ataca esse vazio combinando três coisas na mesma jornada: medição real feita no aparelho,
um motor de diagnóstico determinístico que classifica cada dimensão e escolhe a causa dominante, e
uma camada de linguagem simples (rótulos, explicação por IA, ações recomendadas) que diz o que
fazer. O app é deliberadamente honesto sobre os próprios limites — rotula estimativa como
estimativa, recusa declarar vencedor em empate técnico de DNS e diz explicitamente quando não
conseguiu medir algo.

---

## 3. Personas e casos de uso

**Usuário residencial não técnico** — é o público-alvo padrão. Abre o app quando algo está ruim
("está lento", "o vídeo trava") e quer uma resposta em português. Usa: teste de velocidade →
resultado com veredito → diagnóstico guiado por objetivo.

**Usuário técnico / entusiasta** — quer o número cru além do veredito: RSSI por BSSID, ocupação de
canal, RSRP/SINR do chip, potência óptica Rx/Tx da ONT, latência por servidor DNS. Usa: aba Sinal
(as três abas), Detalhes técnicos, Equipamento de internet, DNS, Ping, Dispositivos.

**Jogador** — não quer saber de Mbps genérico; quer saber se dá para jogar aquele jogo específico
naquele aparelho. Usa: Modo gamer.

**Usuário em conflito com a operadora** — precisa de documento. Usa: Laudo (relatório de diagnóstico
exportável em PDF) e exportação do histórico (CSV/PDF).

Casos de uso cobertos hoje: medir velocidade e entender o resultado; descobrir a causa provável de
um sintoma específico; escolher canal Wi-Fi; ver quem está na rede local; ler o status da ONT de
fibra; comparar servidores DNS; avaliar a conexão para um jogo; consultar e exportar histórico;
deixar a conexão monitorada em segundo plano com alertas.

---

## 4. Navegação

Não existe Navigation Compose graph. `AppShellNavigation.kt` mantém a raiz selecionada e uma pilha
de overlays independente por raiz, salva por `rememberSaveable`; `AppShell.kt` faz apenas o wiring
das telas e callbacks existentes. O estado restaurado sobrevive à recriação do processo sem mover
regras de negócio ou ViewModels para a shell.

### 4.1 Barra inferior — Jornada 2.0 e fallback legado

`AppShellBottomBar.kt` implementa os dois conjuntos reversíveis. A flag canônica
`consumer_app_shell_guided_2_enabled` seleciona o modo; seu default local é `false`, portanto o
fallback offline seguro é `Legacy`. Quando ativado via configuração já persistida pelo provider,
`Guided2` expõe quatro raízes e abre em Início. O modo legado mantém cinco abas e cold start em
Velocidade, sem depender de rede para rollback.

| Índice compatível | Rótulo | Tela | Jornada 2.0 |
|---|---|---|---|
| 0 | Início | `Inicio2Screen` no shell guiado; `HomeScreen` no fallback Legacy | cold start |
| 1 | Velocidade | `SpeedTestScreen` | raiz |
| 2 | Sinal | `SinalScreen` | somente fallback legado; no 2.0, Wi-Fi é fluxo profundo |
| 3 | Histórico | `HistoricoScreen` | raiz; back volta para Início |
| 4 | Ferramentas | `FerramentasScreen` | raiz; nunca bloqueada por feature flag |

Na Jornada 2.0, a barra fica oculta em qualquer overlay e durante a execução do speedtest. Trocar de
raiz preserva a pilha da raiz anterior; Voltar desempilha apenas a raiz atual. Perfil é acessado
pela ação da app bar nas quatro raízes. O drawer e a aba Sinal permanecem alcançáveis no fallback
legado. Os nomes de analytics existentes (`home`, `speedtest`, `sinal_wifi`, `historico` e
`ferramentas`) foram preservados.

### 4.2 Overlays

Lista exata de `AppShellOverlay` (`AppShellNavigation.kt`) — 18 valores, todos empilháveis:

| Valor do enum | Tela renderizada | Aberto a partir de |
|---|---|---|
| `Laudo` | `LaudoScreen` | Ferramentas; Ajustes; CTA "Executar diagnóstico" no Equipamento de internet |
| `Ping` | `PingScreen` | Ferramentas; SpeedTestScreen |
| `Privacidade` | `PrivacidadeScreen` | Perfil; menu lateral legado; Ajustes |
| `Novidades` | `NovidadesScreen` | Perfil; Ajustes |
| `ResultadoVelocidade` | `ResultadoVelocidadeScreen` | automático ao concluir um teste (`AppShell.kt:615-630`); link "Ver resultado" em Velocidade |
| `Fibra` | `EquipamentoInternetScreen` | nó do gateway na Início; linha do roteador em Ajustes (`onAbrirGatewayDetalhe`, `AppShell.kt:483-488`) |
| `Dispositivos` | `DispositivosScreen` | Ferramentas; CTA dentro do Equipamento de internet |
| `EquipamentoInternet` | `EquipamentoInternetScreen` | Ferramentas (card "Equipamento de internet") |
| `Ferramentas` | `FerramentasScreen` | apenas o card contextual do diagnóstico guiado (`onAbrirFerramentaSugeridaOverlay`, `AppShell.kt:475-478`) — a aba 4 usa a tela direto, sem passar por este overlay |
| `Dns` | `DnsScreen` | Ferramentas; SpeedTestScreen |
| `Perfil` | `PerfilScreen` | ação de perfil na app bar 2.0; menu lateral e entradas legadas equivalentes |
| `Ajustes` | `AjustesScreen` | Perfil; preserva conexão, monitoramento e dados locais existentes |
| `SinalCanais` | `SinalScreen` | Ferramentas; Wi-Fi, canais e rede móvel em fluxo profundo 2.0 |
| `SinalWifi` | `SinalWifiScreen` | Ferramentas |
| `Termos` | `TermosDeUsoScreen` | menu lateral |
| `DiagnosticoGuiado` | `DiagnosticoGuiadoScreen` | CTA "Descobrir o que está acontecendo" no resultado do teste. Desde a #1704 o fluxo **não exige medição anterior**: sem resultado disponível ele abre na escolha do sintoma e mede sozinho na rota `Analise` (§8.5 da spec 2.0) antes de concluir. Desde a #1705 a conclusão distingue os 5 valores de `MeasurementStatus` — parcial, contaminado, inconclusivo e cancelado têm explicação própria e ação concreta, em vez de um banner único sem saída. Desde a #1707 (Task 2.0.09e) a conclusão também mostra o rótulo de confiança em texto ("confiança alta/média/baixa", §14.4 — nunca número) e, quando a IA recomenda reteste (`AiAcaoRecomendada.tipo == "reteste"`), o CTA "Testar novamente" **vinculado** à mesma análise (§8.8): dispara uma medição nova de verdade e resolve em "Melhorou"/"Não mudou"/"Piorou"/"Comparação inconclusiva" (§14.6) — nunca "recomeçar do zero" (isso é outro CTA, em `ResultadoVelocidadeScreen`), nunca compara redes diferentes com aviso |
| `DetalhesTecnicos` | `DetalhesTecnicosScreen` | CTA "Ver detalhes da conexão" no resultado do teste |
| `ModoGamer` | `ModoGamerScreen` | 3 entradas: card "Modo Jogos" em Ferramentas, CTA no resultado do teste, botão no resultado do diagnóstico guiado (objetivo "Jogos atrasam ou travam") |

Notas de comportamento:

- `Fibra` e `EquipamentoInternet` renderizam **a mesma** `EquipamentoInternetScreen`
  (`AppShell.kt:1062` e `AppShell.kt:1108`) — são dois pontos de entrada históricos para o mesmo
  destino, não duas telas.
- Back físico desempilha um overlay por vez (`AppShell.kt:633-641`); fechar `Laudo` por back conta
  como "laudo fechado" para elegibilidade do prompt de avaliação da Play Store.
- O z-order de desenho segue a posição real na pilha, não a ordem no arquivo
  (`rememberOverlayZIndex`, `AppShell.kt:174-183`).
- `ResultadoVelocidade`, `DiagnosticoGuiado` e `DetalhesTecnicos` só renderizam se existir um
  resultado de speedtest em memória (`AppShell.kt:885`, `:921`, `:985`).
- Uma tela **existe no diretório mas não é roteada**: `MinhaConexaoScreen.kt` — seu conteúdo é
  consumido como bottom sheet dentro de `AjustesScreen`, não como destino próprio.

### 4.3 Menu lateral (fallback legado)

`AppNavigationDrawerContent`, aberto pelo hambúrguer nas cinco telas quando `shellMode = Legacy`.
Cinco itens fixos mais a versão do app: **Ajustes** (`AppShellOverlay.Perfil`, que mantém Ajustes
como primeiro destino), **Ajuda e
suporte** (`SimpleInfoSheet` com `suporte@signallq.com`, `AppShell.kt:1371-1379`), **Privacidade**
(`Overlay.Privacidade`), **Termos de uso** (`Overlay.Termos`) e **Sobre o SignallQ**
(`SobreSheet`). A navegação inferior não é duplicada aqui.

### 4.3.1 Perfil 2.0

`PerfilScreen` é o centro administrativo, sem conta, autenticação, foto ou avatar remoto. Reúne
**Ajustes**, **Privacidade**, **Novidades**, **Ajuda e suporte**, **Termos de uso** e **Sobre o
SignallQ**, além da versão real de `BuildConfig`. Ajustes continua sendo a tela existente, agora em
overlay próprio, para não duplicar nem mover regras de conexão, monitoramento, dados locais ou
ações destrutivas. Voltar fecha primeiro o destino interno e depois Perfil; a pilha é restaurável.
Ajuda tenta abrir o cliente de e-mail por `mailto:` e mantém endereço/copiar como fallback quando
não existe handler. Consentimento AdMob e exclusão/reset permanecem nas superfícies legadas
responsáveis e não foram redesenhados nesta fatia.

### 4.3.2 Início 2.0

Quando a flag canônica `consumer_app_shell_guided_2_enabled` está ativa, a raiz Início usa
`Inicio2Screen`; com o default local `false`, `HomeScreen` permanece como fallback Legacy completo.
A troca não cria estado paralelo: `Inicio2UiStateMapper` adapta `SnapshotRede`,
`SnapshotDiagnostico` e a medição escolhida pelo mesmo `resolverMedicaoHome` usado pela Home antiga.

A composição 2.0 mostra conexão Wi-Fi, móvel, Ethernet, offline ou ainda sendo identificada; estado
sem análise, último estado conhecido em memória, medição persistida explicitamente tratada como
resultado anterior, carregamento e análise interrompida; e exatamente um CTA **Analisar minha conexão**.
Os timestamps e o contexto de rede disponíveis não possuem contrato canônico de validade para o
veredito da Início, portanto essa superfície não classifica resultados como válidos ou expirados. O CTA
reutiliza `onIniciarDiagnostico`, portanto mantém motor e analytics existentes sem evento paralelo.
O ciclo single-flight vem do `DiagnosticOrchestrator`: cada solicitação aceita recebe uma geração
monotônica e termina como concluída, erro ou cancelada, inclusive quando a preparação do input falha.
O terminal só é publicado depois do fechamento da telemetria e da liberação atômica do gate. O
Pulse consome o resultado tipado da própria geração e abandona uma solicitação rejeitada, sem aguardar
terminal global. A UI mantém apenas uma guarda transitória até observar a aceitação canônica; ela não
persiste geração em recriação do produtor. Assim, recomposição e veredito repetido não duplicam nem
bloqueiam a próxima sessão.
O `Job` proprietário também registra cleanup idempotente: cancelamento antes do primeiro dispatch
publica `cancelado` e libera apenas a mesma reserva, sem afetar uma geração posterior.
Ainda no modo Guided2, a Início projeta trilhas próprias para Wi-Fi, rede móvel, Ethernet, offline
e transporte ainda desconhecido a partir de `SnapshotRede`. Apenas a trilha Wi-Fi consulta
`SnapshotScanWifi` e o `TopologiaRedeEngine`; scan anterior nunca produz mesh fora desse transporte.
O nó mesh só aparece para `NO_MESH` com confiança `ALTA`, sem conflito **e** com confirmação
independente de roteador central; SSID igual, BSSID único com OUI de fabricante mesh,
`SISTEMA_MESH_PROVAVEL` e confiança média/baixa nunca viram afirmação na Início. Sem permissão,
durante scan, em erro ou offline a trilha permanece parcial e textual. Equipamento, Wi-Fi e sinal
móvel reutilizam overlays canônicos somente quando aplicáveis. Como não existe detalhe dedicado do
aparelho local, “Este aparelho” permanece textual nesta fatia em vez de abrir a lista geral da LAN.
No Guided2, o CTA principal abre o **SignallQ Assist**, um fluxo guiado — não chat e não IA — que
reutiliza os sete `ObjetivoDiagnostico` e oferece também a escolha neutra de verificar a conexão.
O Assist só apresenta pergunta contextual quando a resposta é consumida pelo motor atual: conexão
usada para jogos ou melhora ao desligar o Wi-Fi. As demais situações seguem direto para a análise,
sem perguntas decorativas. Objetivo, resposta fechada e abandono explícito sobrevivem à recriação
da UI sem repetir eventos. O objetivo (e a resposta da pergunta contextual, quando existir) seguem
para o "plano existente": se o usuário depois abrir o diagnóstico guiado pós-teste
(`DiagnosticoGuiadoScreen`), a lista de objetivos e a primeira pergunta já chegam pré-preenchidas
com o que foi respondido no Assist — a pessoa não escolhe o mesmo objetivo nem responde a mesma
pergunta duas vezes, e a resposta muda de fato as dimensões/ações do
`DiagnosticoGuiadoEngine` para Jogos com lag e Wi-Fi vs. operadora. Essa pré-seleção expira ao
testar de novo ou voltar ao início. O Legacy mantém seu diagnóstico guiado existente sem o Assist.
Não há grade técnica, catálogo de ferramentas, diagnóstico completo nem placement AdMob na Início.
A issue #1601 continua responsável pelo acesso direto ao resultado persistido exato; esta fatia
somente apresenta sua existência sem duplicar essa navegação.

### 4.4 Hub de Ferramentas

`FerramentasScreen.kt`. A Jornada 2.0 apresenta os nove destinos de `CatalogoFerramentas.todos`
como lista aberta, em um toque, sem grid ou catálogo visual concorrente:

| Card | Descrição exibida | Destino |
|---|---|---|
| Sinal e canais | "Wi-Fi, canais e rede móvel" | `SinalScreen` em `Overlay.SinalCanais` |
| Sinal Wi-Fi ao vivo | "Intensidade enquanto você anda pela casa" | `SinalWifiScreen` |
| Dispositivos | "Quem está na sua rede" | `DispositivosScreen` |
| Equipamento de internet | "Status do modem/ONT da operadora" | `EquipamentoInternetScreen` |
| Ping | "Teste de latência para um endereço" | `PingScreen` |
| DNS | "Compare servidores e troque o seu" | `DnsScreen` |
| Laudo | "Laudo técnico completo da sua conexão" | `LaudoScreen` |
| Monitoramento | "Análise avançada e alertas em segundo plano" | `MonitoramentoSheet` |
| Modo Jogos | "Teste sua conexão para 21 jogos, em qualquer dispositivo" | `ModoGamerScreen` |

Quando o usuário chega ao hub pelo card contextual do diagnóstico guiado, a ferramenta apontada
recebe o prefixo textual "Recomendado para você" — que é limpo ao sair da tela, nunca fica
estático. Permissão ausente navega para a superfície que explica/solicita a permissão; flag remota
desligada mantém o gate canônico e seu evento `feature_blocked_remote`; offline não abre engine e
informa reconexão como próximo passo; ferramentas ocultas são filtradas antes da composição, sem
UI, semântica, foco ou callback. Uma abertura permitida registra exatamente um `screen_view` pela
taxonomia existente (os dois destinos de sinal usam o nome canônico `sinal_wifi`); remoto, offline
e oculto não registram abertura. Nenhuma dessas condições produz affordance inerte. O modo
Legacy continua usando a mesma lista e callbacks. O placement nativo de Jogos não foi movido nem
migrado nesta fatia.

### 4.5 Sheets modais fora da pilha

Controlados por flag local no `AppShell`, não empilhados: `MonitoramentoSheet`, `DadosLocaisSheet`,
`GatewayConnectionSheet` (credenciais do equipamento), `SimpleInfoSheet` (ajuda), `SobreSheet`, mais
dois diálogos — `ForaDoWifiDialog` (aviso de consumo em rede móvel, `AppShell.kt:1299-1309`) e
`DiagnosticoConectividadeDialog` (speedtest interrompido por Wi-Fi sem internet,
`AppShell.kt:1314-1319`).

### 4.6 Antes do shell: onboarding e consentimento

`RotaInicialApp.kt` (função pura `rotaInicialApp`, consumida em `MainActivity.kt`) decide, nesta
ordem: enquanto o DataStore não responde, tela vazia (evita o onboarding "piscar" a cada cold
start); se o onboarding não foi concluído, `OnboardingScreen`; se foi concluído mas não há resposta
de LGPD, `LgpdConsentDialog`; só então o `AppShell`. Usuário existente (ambos os flags já
persistidos) nunca vê Onboarding de novo.

Desde a issue #1671 (Task 2.0.23, épico #1647), o onboarding tem **1 tela só**
(`OnboardingScreen.kt`): boas-vindas com checkbox de aceite dos Termos de Uso e Política de
Privacidade — o botão "Começar" fica desabilitado até o aceite, único bloqueio do fluxo; os dois
documentos abrem como overlay interno. A tela comunica diagnóstico de internet, não speed test, e
não lista catálogo de ferramentas. Nenhuma permissão é pedida em lote aqui — as antigas quatro
permissões da tela 2 (Wi-Fi por perto/localização, dispositivos na rede, sinal do chip/telefonia,
notificações) viraram **contextuais**: cada uma é solicitada só quando o usuário entra na
funcionalidade que precisa dela (aba Wi-Fi/Canal pede localização, aba Móvel pede telefonia, ativar
o monitoramento pede notificação — ver `MainActivity.kt` `solicitarPermissao*Contextual()` e
`onAtivarMonitoramento`, e a decisão pura `DecisaoPermissaoContextual.kt`). Nenhuma delas bloqueia o
uso básico do app.

### 4.7 Bloqueio remoto de rotas

Nove módulos do consumer podem ser desligados remotamente por Firebase Remote Config
(`ConsumerFeatureModuleIds`, `AppShellFeatureGating.kt:30-40`): home, speedtest, wifi, devices, dns,
fibra, diagnostico, history, settings. **Todas as flags nascem ligadas** (fail-open,
`consumer-catalog.json`). Com a flag desligada, a aba fica não clicável e o overlay não abre — o
usuário vê o snackbar neutro "Recurso temporariamente indisponível." (`AppShell.kt:411`). Ferramentas
(hub), Privacidade e Termos nunca passam pelo gate, por decisão explícita de não esconder obrigação
legal.

---

## 5. Funcionalidades por domínio

### 5.1 Medição de velocidade

**Telas:** `SpeedTestScreen` (aba 1) → `VelocidadeScreen` (execução em tela cheia) →
`ResultadoVelocidadeScreen` (overlay).

**O que o usuário faz.** Toca no círculo central "Iniciar teste" (aba Velocidade) ou no card
"Medições" da Início — sem escolher modo. **GH#1737 (épico #1647):** desde a 2.0, o modo é
automático por tipo de rede, decidido em `modoAutomaticoPara` (`feature/speedtest/ModoSpeedtest.kt`)
no ponto de disparo em `AppShell.kt`: rede móvel usa **Rápido** (somente download, ~10 MB); Wi-Fi
(e demais conexões) usa **Completo** (download e upload). O antigo seletor manual de pills
(`ModeSelector`) e a sheet "Tipo de medição" (`MedicaoTipoSheet`) foram removidos, junto com o modo
**Triplo** (3 medições consecutivas com médias) — irrelevante para a pessoa ver ou decidir.

**O que o usuário vê durante.** A `VelocidadeScreen` cobre a tela inteira com um gauge circular
animado, as quatro fases em pills (LATÊNCIA → DOWNLOAD → UPLOAD → CONCLUÍDO), uma frase narrativa
por fase e haptics nas transições. Durante o upload, o download já concluído continua visível.
Cancelar durante a execução exige confirmação e encerra o teste sem produzir resultado — a mesma
confirmação usada pelo `BackHandler` do sistema. Se a medição conclui sem status `COMPLETE` (parcial,
contaminada ou inconclusiva — `MeasurementStatus`, GH#1738), a `VelocidadeScreen` mostra título,
ícone e explicação próprios do status antes de a tela de resultado assumir, reusando
`continuidadeDaMedicao` (a mesma ponte que o fluxo guiado já usava desde a #1705) em vez de tratar
toda conclusão não-`erro` como sucesso.

**O que o usuário vê depois.** O resultado abre sozinho ao concluir. Título e mensagem vêm da decisão
do motor de diagnóstico, não de texto fixo (`ResultadoVelocidadeScreen.kt:306-321`). Dois cards
principais (download e upload); um toggle "Ver detalhes da conexão" revela mais quatro: tempo de
resposta, variação do tempo de resposta, "falhas estimadas na conexão" (rótulo deliberadamente
honesto — a medição é taxa de timeout de probes HTTP, não perda de pacotes IP,
`ResultadoVelocidadeScreen.kt:397-402`) e "lentidão com a rede ocupada" (bufferbloat). Abaixo, a
seção "Como sua internet deve funcionar" traduz o resultado em três usos práticos: vídeos em alta
qualidade, jogos online, chamadas de vídeo.

O app avisa quando o próprio resultado é suspeito: callout se o upload não foi detectado, e texto
distinto quando o teste foi contaminado por mudança de rede ("O teste foi interrompido porque a
conexão caiu ou mudou durante a medição.") versus interferência genérica de outros apps
(`ResultadoVelocidadeScreen.kt:445-470`).

**Guardas.** Iniciar teste em rede móvel abre o `ForaDoWifiDialog` com aviso de consumo; confirmar
ali pula o segundo gate de rede medida (`AppShell.kt:1385-1394`). Se o Wi-Fi estiver conectado mas
sem internet, o speedtest é interrompido e o app mostra a conclusão do diagnóstico local em vez de
travar em "executando" (`AppShell.kt:1397-1405`) — o conteúdo desse diálogo
(`DiagnosticoConectividadeDialog`) migrou para tokens do design system 2.0 em `VelocidadeScreen.kt`
(GH#1738); o gatilho (quando bloquear, antes de `executando` publicar) continua em
`MainViewModel`/`SpeedtestViewModel`, sem mudança.

**Saídas.** Compartilhar o resultado gera um PDF. Todo resultado é persistido no histórico.

### 5.2 Sinal móvel

**Tela:** aba Móvel dentro de `SinalScreen` (índice 2 das abas internas), renderizada por
`MovelTab` em `SinalMovelSection.kt` (extraído de `SinalScreen.kt` na Task 2.0.12/#1660). É
auto-selecionada quando a conexão ativa não é Wi-Fi (`SinalScreen.kt`).

Um card por SIM ativo, rotulado "Chip 1", "Chip 2", com logo real da operadora e badge "EM USO" no
SIM padrão de dados (dual SIM suportado, nunca confunde SIM ativa com SIM de dados — GH#1206).
Abaixo, três cards fixos em linguagem natural — **conclusão antes da sigla técnica** (spec design
2.0 §4.3): **Qualidade do sinal**, **Tipo de conexão** e **Experiência esperada**, cada um com
badge colorido vindo dos classificadores de `SinalMovelClassificacao.kt`. Um quarto card opcional
"Detalhes técnicos" (RSRP/RSRQ/SINR em dBm/dB, tecnologia) só aparece depois da conclusão e só
quando há alguma métrica bruta disponível. No rodapé, o CTA de contato ("Falar com a {operadora}"
ou, quando a operadora não foi identificada, "Falar com sua operadora") sempre tem destino: usa o
site do catálogo local (`BancoOperadoras`) quando reconhece a operadora, ou cai numa busca genérica
quando não reconhece — nunca fica desabilitado ou escondido (Task 2.0.14/#1662).

Sem a permissão de telefonia (`READ_PHONE_STATE`), a aba **continua útil de forma reduzida** em vez
de bloquear: `MonitorTelephonyImpl` emite um snapshot com só a operadora (via APIs do
`TelephonyManager` que não exigem a permissão), a aba mostra um banner explicando o que falta e por
quê, com atalho para conceder a permissão — e só cai no estado vazio de bloqueio quando não há
literalmente nenhum dado (sem SIM, emulador). Dados de sinal móvel mais crus (ASU, roaming, MCC/MNC)
seguem também na `CellularInfoSheet` da Início (`HomeScreen.kt`).

### 5.3 Wi-Fi (redes e canais)

**Telas:** abas Wi-Fi (0) e Canal (1) de `SinalScreen`, mais `SinalWifiScreen` (overlay).

**Aba Wi-Fi.** Lista as redes ao redor com filtro por banda (Todos / 2.4 / 5 / 6 GHz). O bloco "SUA
CONEXÃO" desenha uma **árvore de topologia** do próprio SSID: o nó conectado agora, mais os BSSIDs
que o motor de topologia confirmou como parte da mesma infraestrutura — SSID igual sem evidência de
fabricante/banda cai em "outras redes" (`SinalWifiSection.kt`, `GrupoRedeTree`). Quando o motor não
tem confiança alta sobre a estrutura (mesh incerto), a tela fica em silêncio — decisão de produto
(issue #1661, 2026-08-19): nunca afirma a incerteza, nem com nota de rodapé; antes da migração 2.0
havia um aviso textual "estrutura estimada por fabricante/sinal, sem confirmação de rota de rede",
removido nesta fatia. Abaixo, as redes de terceiros agrupadas por SSID, expansíveis quando o SSID
tem múltiplos pontos. Tocar em qualquer rede abre uma sheet com sinal, banda, canal, largura,
segurança e BSSID; se o nó corresponder a um dispositivo real encontrado no scan da LAN, abre a
sheet de AP mesh no lugar.

**Aba Canal.** Explicação em linguagem simples (há interferência? vale trocar de canal?) seguida da
lista "Ocupação dos canais" ordenada por congestionamento, com status Livre/Moderado/Congestionado
por canal — sem visualização gráfica de espectro/ocupação. Decisão de produto (issue #1661,
2026-08-19): o gráfico técnico de canais (curvas de espectro desenhadas em Canvas, GH#1131) foi
**removido**, não escondido atrás de opção secundária — divergência deliberada do protótipo do
Design System 2.0, que ainda cita "gráficos técnicos" como extensão de domínio futura (ver
`docs_ai/design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md`, seção 5). Um único bloco de aviso por vez,
mutuamente exclusivo (`SinalCanalSection.kt`, `CanalTab`): canal congestionado, canal limpo, ou canal
recomendado para migração. Card de band steering quando você está em 2.4 GHz e existe um nó do mesmo
SSID em 5 GHz. O próprio rótulo da aba ganha ícone de alerta quando o canal conectado está
congestionado.

Ambas as abas fazem auto-refresh a cada 30 s enquanto visíveis e em foreground
(`SinalScreen.kt:268-278`), e mostram estado vazio "Você está usando a internet do chip" quando não
há Wi-Fi.

**Sinal WiFi (ferramenta separada).** `SinalWifiScreen` é o indicador em tempo real, pensado para o
usuário andar pela casa. Migrada para a ferramenta 2.0 na issue #1668 (épico #1647): categoria
simples em destaque (Excelente/Bom/Regular/Fraco, mesmo motor de classificação de
`signalQuality`/`ClassificacaoMetricaLocal.kt` usado nas outras telas de sinal) com o dBm como
detalhe técnico secundário, barras de sinal ampliadas, velocidade do link e um card com o padrão
Wi-Fi (4/5/6/6E/7 ou "Não identificado") e badge de suporte a MU-MIMO. Um selo "Ao vivo" indica
atualização contínua; com "Remover animações" ativo nas opções de acessibilidade do Android
(`animacoesDoSistemaDesativadas`), o pulso decorativo vira estático sem tirar nenhum dado da tela.

Estados tratados via `SignallQStatefulScreen`: Wi-Fi desligado (botão "Ligar Wi-Fi", que abre o
painel do sistema `Settings.Panel.ACTION_WIFI` sem sair do app — ou liga direto via
`WifiManager.setWifiEnabled` em Android < 10), permissão de localização negada (botão "Conceder
permissão"/"Abrir ajustes do Android" quando bloqueada permanentemente) e Wi-Fi ligado sem rede
associada ("Sem conexão Wi-Fi"). A amostragem só roda com a tela em foreground
(`repeatOnLifecycle(RESUMED)`, cancela sozinha ao sair) e zera o estado anterior sempre que o
Wi-Fi é desligado ou a rede é perdida, para nunca mostrar uma leitura antiga como se fosse atual.

### 5.4 Dispositivos conectados

**Tela:** `DispositivosScreen` (overlay via Ferramentas) — composição pura desde a issue #1663
(épico #1647, Task 2.0.15). A lista e os estados vazio/sem-Wi-Fi vivem em `DispositivosLista.kt`;
as sheets de detalhe (`DeviceDetailSheet`, `MeshApSheet`) e os helpers de rótulo/ícone
compartilhados vivem em `DispositivoDetalheSheet.kt`. Extração mecânica, sem mudança de
comportamento (`DispositivosScreenExtracaoCaracterizacaoTest.kt`).

A tela só funciona em Wi-Fi — em rede móvel ou offline, mostra um fallback explicativo em vez da
lista (`DispositivosScreen.kt`, `SemWifiFallback` em `DispositivosLista.kt`). A varredura roda com
pull-to-refresh e avisa quando o resultado é parcial ("uma etapa da varredura não respondeu").

A lista vem em três seções: **Infraestrutura** (o gateway, com subtítulo composto "IP · bandas Wi-Fi
· N clientes"), **Pontos de acesso** (nós mesh, badge "AP Mesh") e **Dispositivos**. Cada aparelho
mostra nome resolvido ou fabricante, e "Este aparelho" no próprio celular. Dispositivo sem nome
resolvido e sem fabricante confirmado nunca recebe marca/tipo inventado — mostra o rótulo genérico
honesto "Dispositivo desconhecido" (ou "Dispositivo <Fabricante>" quando o fabricante É confirmado
via OUI/UPnP/mDNS) com ícone genérico (decisão de produto do Luiz, issue #1663, 2026-08-19). Tocar
abre uma sheet com IP, MAC mascarado, fabricante, tipo e — só quando há correlação confirmada de
topologia — conexão física e papel na rede.

O usuário pode dar **apelido** a qualquer dispositivo; o apelido é persistido por MAC, com fallback
para IP+nome quando o Android não resolve o MAC via ARP (`DeviceDetailSheet` em
`DispositivoDetalheSheet.kt`). IP/MAC/SSID nunca são enviados a analytics — a telemetria do scan
(`DevicesViewModel`/`MainViewModel`) só emite contagem de dispositivos, nunca os valores.

A sheet de AP mesh é explicitamente honesta sobre o limite: "Sinal, banda e clientes conectados não
estão disponíveis via varredura passiva. Para métricas detalhadas, acesse o painel do seu roteador
mesh." (`MeshApSheet` em `DispositivoDetalheSheet.kt`).

### 5.4b Ping (tempo de resposta)

**Tela:** `PingScreen` (overlay via Ferramentas) — migrada para tela cheia roteada na issue #1665
(épico #1647, Task 2.0.17), mesmo padrão de `DnsScreen` (GH#933 Fase 4): antes era
`ModalBottomSheet`, agora usa `Scaffold`+`CenterAlignedTopAppBar` com botão Voltar. Motor
(`PingExecutor`, `feature/speedtest`) e telemetria preservados — a mudança é de apresentação e
navegação, não de medição.

Título e descrição lideram com significado, não com o nome do protocolo: "Tempo de resposta" em
vez de "Teste de Latência" — segue a tradução de `docs_ai/design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md`
§4.3 (Ping → "Tempo de resposta"). A tela mede latência HTTPS contra um endpoint (nunca ICMP — ver
seção 7) e declara isso explicitamente no resultado ("Via HTTPS · <host>").

**Destino sugerido + opção avançada (decisão de produto do Luiz, 2026-08-19):** a tela sempre
executa contra um destino sugerido por padrão (`speed.cloudflare.com`, o mesmo alvo histórico) e
mostra esse destino de forma visível. Quem quer testar outro endereço abre "Testar outro
endereço" (opção avançada, escondida por padrão — nunca removida) e digita um host; o campo nunca
fica exposto por padrão para não repetir a queixa de "ferramenta de rede parece um terminal". A
validação do host digitado reaproveita `DetectorEnderecoIpPrivado` (o mesmo detector já usado por
`DnsScreen`/`feature/dns`) — endereços privados/locais (RFC 1918, loopback, link-local, ULA IPv6)
são recusados antes de qualquer teste, sem criar um segundo validador. `PingScreen` aceita um
`destinoContextual` opcional para pré-preencher o destino a partir de um diagnóstico futuro; hoje
nenhum chamador ainda produz esse valor (fica `null`, preservando o comportamento padrão) — plumbing
deliberadamente adiado até existir uma fonte real de contexto, para não inventar dado fictício.

Cancelamento (voltar da tela) encerra a coleta de amostras via ciclo de vida estrutural do Compose
(`LaunchedEffect`/`rememberCoroutineScope` cancelam a coroutine ao sair de composição) — o mesmo
mecanismo que já existia na versão em sheet, agora também coberto por teste de caracterização em
`PingScreenViewModelTest`.

### 5.5 DNS

**Tela:** `DnsScreen` (overlay via Ferramentas ou Velocidade). Já operava como tela cheia roteada
desde GH#933 (Fase 4) e já liderava com significado ("DNS afeta a abertura de sites, não a
velocidade da sua conexão") antes da issue #1665 — revisada como parte da Task 2.0.17 e já
conforme aos critérios da Jornada 2.0, sem mudança de código necessária. DNS não tem um "destino"
customizável análogo ao do Ping: a ferramenta compara resolvedores DNS conhecidos, não testa um
host escolhido pelo usuário — por isso não ganhou a mesma opção avançada.

**O app não troca o DNS.** A tela diz isso ao usuário na cara: "Isso não troca o DNS
automaticamente. Para alterar, você precisa configurar no Android ou no roteador."
(`DnsScreen.kt:452-456`).

Quatro blocos. **Seu DNS atual** — nome resolvido e IP do resolvedor, com a latência omitida quando o
DNS é o próprio roteador (o app explica que o roteador só repassa as consultas). **Benchmark** —
botão "Comparar servidores DNS" mede sete provedores públicos via DNS-over-HTTPS: Cloudflare, Google
DNS, Quad9, OpenDNS, AdGuard, Control D e CleanBrowsing
(`feature/dns/.../BenchmarkDnsDoh.kt:366-375`); cada linha mostra tempo em ms, nota A/B/C/D e badges
"atual"/"mais rápido". **Recomendação** — declara o vencedor, ou recusa declarar: quando os melhores
ficam dentro de 10 ms, a tela diz "Empate técnico entre os servidores mais rápidos nesta conexão."
(`DnsScreen.kt:480-490`). **Guia** — colapsável "Quando vale a pena trocar DNS?", com o passo a
passo real de configuração em duas abas (Dispositivo, 5 passos; Roteador, 6 passos), cada uma
declarando o escopo do efeito.

### 5.6 Fibra / equipamento de internet

**Tela:** `EquipamentoInternetScreen`, alcançada pelo card em Ferramentas ou pelo nó do gateway na
Início.

A tela é composta por capacidade do equipamento, em cards separados: status e disponibilidade,
módulos técnicos (fibra/WAN/LAN/Wi-Fi/dispositivos), topologia, seletor de dispositivo, informação
técnica e ações. Quando o app tem acesso ao equipamento, mostra o estado GPON com potência óptica
Rx/Tx classificada por perfil versionado, dados de WAN/PPP, e o alerta de Double NAT quando o
diagnóstico de topologia detecta CGNAT. As ações disponíveis dependem do que o driver suporta —
reiniciar o equipamento só aparece quando há gerenciamento disponível.

Três CTAs saem daqui para o resto do app: "Ver dispositivos", "Executar diagnóstico" (Laudo) e "Ver
detalhes do Wi-Fi" — este último fecha o overlay e leva à aba Sinal, em vez de empilhar mais uma
tela (`AppShell.kt:555-561`).

**Modelo não suportado.** Quando o app identifica um equipamento na rede mas não sabe ler os dados
dele (`AcessoEquipamento.SOMENTE_IDENTIFICACAO`), a tela não se limita a avisar "não suportado" —
oferece dois próximos passos concretos (decisão de produto, Luiz, 2026-08-19, issue #1664): "Abrir
configurações no navegador" (só quando há um host configurado — abre `http://<host>` via
`ExternalActionLauncher`) e "Falar com o suporte" (`abrirEmailSuporte`, `suporte@signallq.com`).

**Confirmação antes de agir no equipamento.** A única ação que efetivamente muda o estado do
modem/roteador do usuário nesta tela é "Reiniciar equipamento" — sempre passa por um diálogo de
confirmação explicando o que vai acontecer (fica sem internet por 1-3 minutos) antes de executar
(`ReiniciarEquipamentoDialog`, `EquipamentoAcoesCard.kt`). As outras ações listadas ("Ver
dispositivos", "Executar diagnóstico", "Ver detalhes do Wi-Fi") são só navegação — não mexem no
equipamento, por isso não pedem confirmação (decisão de produto, Luiz, 2026-08-19, issue #1664, item
2).

**Credenciais.** O CTA "Configure o acesso ao equipamento" abre a `GatewayConnectionSheet` — a mesma
sheet do nó do gateway na Início. Existe um toggle "manter conectado" que vincula a sessão ao BSSID
atual, para não reautenticar a cada retorno à mesma rede.

**Aviso importante sobre o estado real desta funcionalidade:** o serviço genérico de conexão a
gateway está em modo indisponível em produção. `GatewayConnectionServiceIndisponivelPadrao` nunca
retorna sucesso — só "Indisponível" — porque o mock anterior fingia autenticar e persistia
credencial sem nenhuma validação real (BUG#1511, documentado em `AppShell.kt:418-424`). Na prática,
`gatewaySessaoValida` é sempre `false` e o nó do gateway sempre reabre a sheet manual. A leitura
real de equipamento hoje passa só pelo driver Nokia (ver seção 7).

### 5.7 Diagnóstico com IA

**Telas:** `DiagnosticoGuiadoScreen` (overlay sobre o resultado do teste) e `DetalhesTecnicosScreen`.

O fluxo é **guiado por objetivo, nunca chat livre**. Depois de um teste de velocidade, o CTA
"Descobrir o que está acontecendo" abre uma lista de **7 objetivos fechados**
(`core/diagnostico/.../ObjetivoDiagnostico.kt:14-41`): a internet cai ou fica instável; vídeos travam
ou ficam carregando; jogos atrasam ou travam; chamadas de vídeo travam; sites demoram para abrir; a
velocidade está abaixo do plano; não sei onde está o problema.

Escolhido o objetivo, o app faz **2 perguntas fechadas** (single-select, com barra de progresso) e
mostra o resultado. Se o resultado do speedtest não for válido para conclusão, a tela nem entra no
fluxo — pede para refazer o teste na mesma rede (`DiagnosticoGuiadoScreen.kt:203-206`).

O resultado separa visualmente o que foi medido do que foi narrado, em duas caixas: **"DADOS MEDIDOS
PELO SIGNALLQ"** (label → valor, colorido por status) e **"EXPLICAÇÃO DO RESULTADO"** (a parte da
IA). Rodapé fixo: "A explicação ajuda a entender o resultado. A avaliação é feita com os dados
medidos no seu aparelho." Se a IA falhar, o app diz "Não consegui carregar a explicação. O resultado
acima continua válido." — a IA nunca decide o status, só escreve a prosa
(`ui/component/DiagnosticoResultadoComponents.kt:86-186`).

Complementos do resultado:

- **Card "Próximo passo"** — aponta **uma** ferramenta, quando o objetivo mapeia para alguma
  (`TipoFerramenta.kt:55-64`): sites lentos e velocidade abaixo do plano → DNS; internet instável →
  Monitoramento; "não sei onde está o problema" → Sinal Wi-Fi. Vídeos travando, jogos com lag e
  chamadas congelando **não recebem card**, por regra explícita de não empurrar sugestão fraca.
- **Contato da operadora** — só quando a causa aponta para ISP ou fibra; abre a sheet de canais
  oficiais.
- **Sugestão** (motor de recomendação) — card rotulado por tipo (DICA, TUTORIAL, AJUSTE
  RECOMENDADO, PRODUTO SUGERIDO, OFERTA DE PARCEIRO, OFERTA DA OPERADORA, PUBLICIDADE), com feedback
  em três botões: Útil / Não útil / Ocultar.
- Para o objetivo de jogos, um botão "Analisar um jogo específico" leva ao Modo gamer.

**Detalhes técnicos** é o caminho paralelo, sem IA e sem recomendação
(`DetalhesTecnicosScreen.kt:39-46`): texto explicativo sobre o tipo de conexão e a lista de dados
medidos com rótulos em linguagem comum ("lentidão com a rede ocupada", "tempo para localizar sites",
"estabilidade da conexão"), mais o servidor usado no teste e o equipamento de internet.

**Laudo.** `LaudoScreen` é o documento. O título exibido é "Relatório de diagnóstico", não "laudo
técnico" — o nome pericial está reservado ao Pro (`LaudoScreen.kt:153-160`). Traz banner de status
com score, resumo, grade de seis métricas (download, upload, latência, jitter, perda, bufferbloat) e
recomendação. Exporta em PDF pelo ícone do TopBar ou pelo botão no rodapé. No PDF, o nome do usuário
é deliberadamente omitido e SSID/IPs vão mascarados. Se o diagnóstico em memória for de outra
execução que a medição exibida, o app recusa combinar os dois e avisa
(`LaudoScreen.kt:265-287`).

### 5.8 Histórico

**Tela:** `HistoricoScreen` (aba 3).

Lista de medições passadas. Desde a issue #1669 (Task 2.0.21, épico #1647), cada linha prioriza
**conclusão e objetivo** sobre a métrica crua: ícone de rede, uma frase de conclusão em destaque
(ex.: "Bom para streaming", "Gargalo identificado: Bufferbloat", "Velocidade lenta"), o objetivo
inferido ("Diagnóstico por IA" / "Diagnóstico guiado" / "Teste de velocidade") + data logo abaixo, e
o Mbps de download como detalhe secundário à direita (não mais o valor dominante do card). A
conclusão/objetivo é derivada em `HistoricoConclusaoMapper.kt` — função pura,
**sem migration de schema**: usa campos que `MedicaoEntity` já persistia (vereditos de
streaming/games/vídeo chamada, gargalo primário, texto de diagnóstico, status, fonte). Registro
legado (colunas novas nulas, status "completed" default) sempre cai num fallback determinístico
baseado só na velocidade de download — nunca quebra nem fica ilegível.

Tocar num item abre uma sheet de detalhe com download/upload em destaque, latência/oscilação/perda,
e linhas condicionais: tipo de rede, aviso de resultado contaminado, bufferbloat, vereditos de
streaming/games/vídeo chamada, gargalo identificado, e o texto do diagnóstico com selo "Gerado por
IA" ou "Diagnóstico local".

**Pendente (fora desta fatia):** comparação entre duas medições (rota
`session-detail → comparison`) e recusa de comparar condições incompatíveis (ex. Wi-Fi vs. 4G) —
decisão de produto do Luiz (#1669, comentário de 2026-08-19) exige a recusa quando implementada.
Adiado deliberadamente para reduzir o risco desta PR (mexe em apresentação de dado persistido, não
em schema); acompanhar em issue de continuação referenciando #1669.

**Filtro:** um só na UI — pills Todos / Wi-Fi / Rede móvel. As medições sintéticas do monitoramento
em segundo plano são excluídas da lista de testes reais.

**Exportação:** ícone no TopBar (desabilitado com lista vazia) abre a
`ExportHistoricoBottomSheet` — período (7 dias, padrão / 30 dias / Tudo) e formato (CSV, padrão /
PDF). O arquivo é gerado no cache e compartilhado via FileProvider. O que é exportado é sempre a
lista já filtrada, recortada pelo período.

**Divergências reais no código:** o parâmetro `resumoHistorico` é declarado
(`HistoricoScreen.kt:297`) mas nunca usado — **não existe seção de resumo/médias na tela**. O filtro
por operadora existe na lógica (`HistoricoScreen.kt:302-304`) mas **não tem nenhum controle
renderizado** para escolhê-lo.

### 5.9 Monitoramento em segundo plano

**Entrada:** `MonitoramentoSheet`, alcançada pelo card "Monitoramento" no hub Ferramentas.

A sheet se chama "Diagnóstico avançado" e tem dois toggles principais, ambos pedindo confirmação
para ligar (não para desligar): **Análise avançada** (coleta sinais extras, avisa que pode aumentar
consumo de bateria) e **Monitoramento passivo**. O subtítulo do toggle de monitoramento comunica a
frequência real da checagem ("Ativo · verifica a cada 30 minutos e pode enviar alertas") em vez de
prometer acompanhamento contínuo — issue #1666 (épico #1647, Task 2.0.18), decisão de produto do
Luiz de 2026-08-19: a UI deve respeitar a limitação real de bateria/WorkManager do Android em vez
de usar linguagem vaga. Com o monitoramento ativo, revelam-se quatro alertas individuais: **Sem
internet**, **Latência alta**, **DNS lento** e **Sinal Wi-Fi fraco**. Em fabricantes conhecidos por
matar processos em background, a sheet mostra um aviso pedindo para manter o SignallQ sem
restrição de bateria.

**Como funciona na prática.** Um Worker roda a cada **30 minutos**
(`MonitoramentoScheduler.INTERVALO_MINUTOS`), com as condições de rede conectada e bateria não
baixa. Ele mede latência HTTP (mediana de 3 amostras), tempo de resolução DNS e RSSI do Wi-Fi, e
grava uma medição sintética no histórico (marcada como `fonte = "monitor"`, só com latência) que
alimenta o gráfico de uptime.

**Gráfico de uptime no Histórico (issues #1666/#1520).** `HistoricoScreen.kt` religa
`UptimeChartUseCase`/`UptimeNarrativaEngine`/`UptimeGridChart` (`feature/history` + `UptimeGridChart.kt`
em `:app`): agrupa as medições dos últimos 7 dias em blocos de 30 min, classifica cada bloco
(OK/LENTO/LATENCIA_ALTA/OFFLINE/SEM_DADO — latência alta nunca é rotulada como offline, GH#1518) e
mostra uma lista de eventos por dia com narrativa textual, não mais o grid de quadrados antigo. A
seção "Estabilidade da conexão · últimos 7 dias" só aparece quando há pelo menos um bloco medido;
se não houver nenhum teste manual nem dado real de monitoramento, a tela volta ao estado vazio
padrão. Este wiring esteve órfão (zero consumidor em produção) desde uma refatoração anterior —
ver #1518/#1520 para o histórico da regressão.

Os alertas usam histerese e **só notificam na transição de ok para alerta**, nunca repetidamente:
latência entra acima de 400 ms e sai abaixo de 300 ms; DNS entra acima de 2500 ms e sai abaixo de
1800 ms; RSSI entra abaixo de −75 dBm e sai acima de −68 dBm. "Sem internet" suprime os outros
alertas. Há teto de **3 notificações por dia** e cooldowns por tipo (DNS 4 h, Wi-Fi fraco 8 h, sem
internet 30 min). Tudo em um único canal de notificação, "Monitoramento de rede"
(`SignallQNotificationHelper.kt:15,38-45`).

Existe ainda uma notificação de **dispositivo novo na rede**, disparada pelo app (não pelo Worker),
com cooldown de 1 h.

### 5.10 Ajustes

**Tela:** `AjustesScreen`, aberta como overlay pelo menu lateral. Não é mais uma aba.

Seis seções: **Perfil** (nome, via `PerfilEditSheet` — o seletor de foto foi removido do app);
**Minha conexão** (operadora, plano contratado em Mbps down/up, cidade/UF, todas abrindo a mesma
sheet); **Aparência** (tema Sistema/Claro/Escuro); **Notificações** (limite mínimo de download para
alertas de qualidade); **Dados e privacidade** (tela de Privacidade e `DadosLocaisSheet`); **Sobre**
(Novidades e versão do app).

A tela de **Privacidade** tem ainda a entrada **"Preferências de anúncios"** (GH#1703), que reabre o
formulário de consentimento da User Messaging Platform para o usuário rever a escolha que já fez.
Ela só aparece onde a própria UMP exige (`privacyOptionsRequirementStatus == REQUIRED`, regiões sob
GDPR); fora disso o formulário não teria o que mostrar e a entrada fica oculta. Não é preferência de
produto: é obrigação da plataforma, e até a #1703 o app sabia **coletar** o consentimento sem
oferecer caminho para revisá-lo.

O perfil de conexão é **por rede**, não global. Quando o app detecta um provedor diferente do
cadastrado, mostra um banner "Detectamos {provedor} nesta rede. / Usar este provedor?" — mas isso só
acontece se o usuário já tinha confirmado explicitamente o valor salvo; sem confirmação prévia, o
app atualiza silenciosamente (`AjustesScreen.kt:209-247`).

`DadosLocaisSheet` concentra as três ações destrutivas, escalonadas por gravidade e **todas com
diálogo de confirmação**: limpar histórico de testes, apagar dados locais, resetar o app. A partir
da issue #1670, cada ação é observável — `MainViewModel.dadosLocaisAcaoEstado`
(`AcaoDadosLocaisEstado`: Ocioso/EmAndamento/Sucesso/Falha) mostra progresso nos botões, mantém o
sheet aberto e exibe o erro em caso de falha (nunca apresenta falha como sucesso), e só fecha o
sheet quando a ação termina com sucesso. O texto do sheet também esclarece que apagar dados locais
**não remove dados já enviados a servidores do SignallQ** (ex.: Diagnóstico por IA, compartilhar
resultado) — decisão de produto (Luiz, 2026-08-19): só explica a diferença, sem oferecer um segundo
caminho de contato/suporte para pedir remoção remota.

**Divergências reais no código:** `AjustesScreen` recebe os estados de monitoramento e de dados
móveis (permitir teste pesado em rede móvel, MB consumidos no mês) mas **não renderiza nenhuma linha
para eles** (`AjustesScreen.kt:90-99,122,136-138`) — monitoramento só é configurável pelo hub
Ferramentas, e a preferência de dados móveis não tem ponto de entrada na UI hoje. `DiagnosticoAppSheet`
(implementado sem nenhum ponto de entrada) foi removido em #1670, junto com o `SettingItem` órfão.

### 5.11 Modo gamer

**Tela:** `ModoGamerScreen` + `ModoGamerConfigResultadoSection`, três pontos de entrada (ver 4.2). É
o **único** fluxo de jogos do app: a `JogosScreen` legada foi removida em 2026-07-26 (issue #1487) e
fundida aqui.

**Etapa 1 — jogo.** Busca e lista de **21 jogos** de catálogo fechado
(`core/diagnostico/.../ModoGamerEngine.kt:326-356`), cobrindo battle royale, FPS competitivo, MOBA e
casual. Jogo fora da lista nunca vira erro: o rodapé "Meu jogo não está na lista" leva a **6
categorias genéricas** de fallback.

**Etapa 2 — aparelho.** Sete opções (PS5/PS4, Xbox, PC, Android, iPhone, Switch, TV/Cloud gaming). É
puramente contextual — **não altera os limiares do motor**.

**Etapa 3 — salvar.** "Salvar para os próximos testes" (marcada por padrão) ou "Usar apenas agora".
Se houver padrão salvo, as próximas aberturas pulam direto para o resultado
(`ModoGamerViewModel.kt:90-103`). Nesta etapa também fica a medição extra opcional "Medir o tempo de
resposta agora", que não bloqueia o fluxo.

**Resultado.** Abre com uma headline direta e simples ("Bom pra jogar" / "Pode ter atrasos" / "Não
recomendado" / "Sem dados suficientes" — issue #1667, decisão do Luiz 2026-08-19: linguagem direta
em vez de fraseado de probabilidade), depois o mesmo banner de status e o mesmo bloco "Medido pelo
SignallQ / Explicação por IA" do diagnóstico guiado, mais "O que fazer agora" com as ações do
motor. Se o jogo veio do fallback, um aviso amarelo declara isso. Se o usuário pediu a medição
extra, aparece uma linha informativa sobre conexão direta com outros jogadores (NAT UDP) — que é
**puramente informativa e nunca rebaixa o veredito** (`ModoGamerConfigResultadoSection.kt:344-347`).
Uma faixa final confirma se a escolha virou padrão ou foi usada só desta vez.

**Convergência com o objetivo guiado "Jogos atrasam ou travam" (issue #1667).** A pergunta guiada
`JOGOS_COM_LAG` (`DiagnosticoGuiadoEngine.avaliarJogosComLag`) e os perfis competitivo/genérico do
Modo gamer (`ModoGamerEngine.avaliarFpsCompetitivo`/`avaliarOutro`) priorizam as mesmas três
métricas — tempo de resposta, variação e falhas na conexão — a partir da mesma função
`dimsLatenciaJitterPerda` (`core/diagnostico/.../DiagnosticoGuiadoEngine.kt`), único ponto de
leitura para esse trio em qualquer um dos dois pontos de entrada. Da tela de resultado do objetivo
guiado, o botão "Analisar um jogo específico" leva ao mesmo `ModoGamerScreen`/`ModoGamerViewModel`
usado pela entrada direta (hub Ferramentas) — um único fluxo, dois pontos de entrada.

### 5.12 Início (visão consolidada)

**Tela:** `HomeScreen` (aba 0). Não é um domínio próprio — é a vitrine que costura os outros.

Cinco blocos: o **"CAMINHO DA SUA INTERNET"**, uma trilha de três nós ligados por conectores
animados (seu aparelho → roteador ou operadora → provedor), com rodapé de veredito em três estados;
um **banner de CGNAT** quando detectado; o card **"MEDIÇÕES"** com o último resultado e o CTA "Medir
velocidade"; o card de **sinal Wi-Fi** (só em Wi-Fi); e o card **"CHIP MÓVEL"** com uma linha por
SIM ativo.

Cada nó da trilha abre uma sheet de detalhe: seu aparelho (`DeviceInfoSheet`), roteador
(`GatewayInfoSheet`, com tipo detectado, sinal, banda, canal, segurança), provedor
(`InternetInfoSheet`, com IP público, DNS privado e servidores DNS). Tocar no nó do roteador sem
sessão válida abre a sheet de credenciais.

**Divergência real no código:** o **banner Anatel não é exibido**. O composable `AnatelBanner`
existe (`HomeScreen.kt:628`) e recebe os parâmetros encadeados desde o ViewModel
(`AppShell.kt:743`), mas **nunca é chamado** — é código morto. O mesmo vale para outros
composables declarados e nunca invocados no arquivo (`BufferbloatCard`, `SignalQualitySheet`,
entre outros). `MobileSignalCard`, `CardMovelDualSim` e `SimChipCompact` estavam nessa lista e
foram removidos em #1261 (2026-08-06).

---

## 6. Permissões

Todas declaradas em `android/app/src/main/AndroidManifest.xml:4-19`. **Nenhuma permissão bloqueia o
uso do app** — a ausência oculta ou degrada o dado dependente, nunca produz tela de erro.

| Permissão | Para quê | Se negada |
|---|---|---|
| `INTERNET` | Todo o produto: speedtest, DNS, IP público, IA remota, analytics | Normal (concedida na instalação, não é runtime) |
| `ACCESS_NETWORK_STATE` | Detectar tipo de conexão (Wi-Fi/móvel/Ethernet), validação de internet, capabilities | Normal (não é runtime) |
| `ACCESS_WIFI_STATE` | Ler a rede conectada (SSID, RSSI, link speed, banda) e listar redes vizinhas | Normal (não é runtime) |
| `ACCESS_FINE_LOCATION` | Exigência do Android para ler `ScanResult`/`WifiInfo`: listar redes vizinhas e analisar canais. Pedida de forma contextual, na primeira entrada na aba Wi-Fi/Canal (issue #1671) | Abas Wi-Fi e Canal mostram sheet contextual e banner; sem ela não há varredura de redes nem análise de canal. Bloqueio permanente troca o CTA por "Abrir ajustes do Android" |
| `ACCESS_COARSE_LOCATION` | Pedida junto com a anterior no mesmo grupo, no mesmo ponto de uso contextual | Mesmo efeito acima |
| `NEARBY_WIFI_DEVICES` (`neverForLocation`, API 33+) | Pedida junto com localização (mesmo ponto de uso — issue #1671): identificar aparelhos na rede local sem usar localização | Descoberta de dispositivos fica degradada; a tela Dispositivos continua abrindo |
| `READ_PHONE_STATE` | Coletar operadora, tecnologia (4G/5G), RSRP/SINR/banda/cellId do chip. Pedida de forma contextual, na primeira entrada na aba Móvel (`AndroidManifest.xml`, issue #1671) | Aba Móvel mostra estado vazio explicativo e sheet contextual ("Não acessamos chamadas, mensagens ou dados pessoais") |
| `POST_NOTIFICATIONS` (API 33+) | Alertas do monitoramento passivo e de dispositivo novo na rede. Pedida de forma contextual, no momento em que o usuário liga o monitoramento (issue #1671) | O monitoramento continua medindo e gravando no histórico, mas nenhum alerta chega ao usuário |
| `CHANGE_WIFI_MULTICAST_STATE` | Descoberta de dispositivos por mDNS na varredura da rede local | Não determinado nesta revisão qual é o comportamento degradado exato |

Fora do manifesto, existe um consentimento de **LGPD** exibido depois do onboarding
(`MainActivity.kt`): a coleta de analytics nasce desabilitada e só é ligada quando o consentimento é
positivo (`SignallQApplication.kt:123-126`).

---

## 7. Limitações conhecidas

### 7.0 Contrato central de anúncios nativos

Os cinco slots existentes permanecem `VELOCIDADE`, `RESULTADO`, `DISPOSITIVOS`, `HISTORICO` e
`JOGOS`; não há anúncio na Início. `NativeAdLoadState` distingue inelegível por flag ou
consentimento, loading, fill, no-fill, erro recuperável e offline. Somente `Fill` produz UI:
loading/no-fill/erro/offline ocupam zero espaço e nunca bloqueiam conteúdo ou CTA.

`rememberNativeAdState` preserva uma sessão por chave estável de slot/configuração, evita novo
request por recomposição e destrói o `NativeAd` quando a composição sai ou a chave muda. Não há
cache global nem preload: um fill pertence somente ao lifecycle do placement que o solicitou. O
wrapper `rememberNativeAd` permanece temporariamente para os cinco callsites legados; cada tela o
migrará em sua própria fatia.

Remote Config continua fail-safe desligado e `canRequestAds` da UMP precede todo request. O estado
sem consentimento mantém a funcionalidade integral. Sinais contextuais continuam limitados ao slot
e vocabulário fechado sanitizado, sem SSID, IP, identificador de dispositivo ou texto livre.

Não existe consumidor autorizado de paid event/revenue no contrato atual. A especificação
preliminar seria `anuncio_receita_registrada` (`valor_micros: Long`, `moeda: String`,
`precisao: String`), disparada uma vez pelo callback pago do SDK e nunca em request/load/impressão.
Como não há tracker/contrato aprovado para esse dado, o evento **não foi implementado**; não se
inventou backend, conversão monetária ou propriedade identificadora.

**Equipamento de fibra — só Nokia.** O único driver com implementação real é o **Nokia G-1425G-B**:
cliente HTTP autenticado, parsers de GPON/WAN/PPP/Wi-Fi/LAN/clientes, perfil óptico versionado com
classificador de Rx/Tx e ação de reboot — tudo em
`android/feature/fibra/src/main/kotlin/.../fibra/`, com testes. TP-Link (Archer C20, Archer C6,
genérico luci/stok) e o perfil mesh genérico existem **apenas como entradas de reconhecimento
documental** no `DeviceDriverCatalog` (`core/network/.../gateway/DeviceDriverCatalog.kt:59-118`) —
metadados de vendor/modelo/banner, sem nenhum cliente HTTP ou parser. **Intelbras não tem driver nem
entrada de catálogo de equipamento**: aparece só como OUI de fabricante para classificação de
topologia.

**Autenticação genérica em gateway não funciona.** `GatewayConnectionServiceIndisponivelPadrao`
nunca retorna sucesso, por decisão deliberada (BUG#1511) — o mock anterior fingia autenticar e
persistia credencial sem validação. A sheet de conexão existe e persiste o que o usuário digita, mas
não há autenticação real fora do caminho Nokia.

**O app não altera nada no sistema.** Não troca o DNS, não muda canal do Wi-Fi, não reconfigura o
roteador. Ele mede, classifica e orienta — as instruções de "como alterar meu DNS" são passo a passo
manual.

**A IA não diagnostica.** A explicação por IA é uma camada de prosa sobre uma decisão que o motor
local já tomou. Se o serviço remoto falhar, o veredito continua válido e o app diz isso. Não existe
chat livre nem conversa multi-turno.

**Métricas rotuladas como estimativa.** "Falhas estimadas na conexão" é taxa de timeout de probes
HTTP, não perda de pacotes IP. O `PingScreen` mede latência HTTPS, não ICMP, e declara isso ao
usuário. A árvore de topologia Wi-Fi é estimada por fabricante e sinal; quando a confiança não é
alta (mesh incerto), a tela de Sinal fica em silêncio em vez de afirmar a incerteza — não há mais
nota de rodapé sobre isso (decisão de produto, issue #1661, 2026-08-19).

**Varredura passiva tem teto.** Para APs mesh, o app não consegue ler sinal, banda nem clientes
conectados — e recomenda o painel do roteador em vez de inventar o dado.

**Dispositivos só em Wi-Fi.** A tela de dispositivos conectados não opera em rede móvel nem offline.

**Funcionalidades parcialmente entregues (código presente, UI ausente):**

- Banner Anatel — composable existe, nunca é chamado (`HomeScreen.kt:628`).
- Resumo/médias no Histórico — parâmetro recebido, nunca usado (`HistoricoScreen.kt:297`).
- Filtro por operadora no Histórico — lógica presente, sem controle na UI (`HistoricoScreen.kt:302-304`).
- Monitoramento e preferência de dados móveis dentro de Ajustes — estados recebidos, sem linha
  renderizada (`AjustesScreen.kt:90-99`).
- `MinhaConexaoScreen.kt` — arquivo de tela não roteado por `AppShell.kt`; o conteúdo vive como
  sheet dentro de Ajustes.

**Sobre métricas de sucesso.** Este documento não define KPIs de negócio. O que existe no código são
eventos de analytics: `app_aberto`, `app_session_start`, `app_session_end`, `screen_view`,
`feature_used`, `feature_crash`, `feature_blocked_remote`, `battery_snapshot`,
`speedtest_iniciado`, `speedtest_concluido`, `diag_iniciado`, `diag_concluido`,
`ia_laudo_solicitado`, `ia_laudo_recebido`, `analytics_outbox_delivery`, e a família
`recommendation_*` (`eligible`, `shown`, `clicked`, `dismissed`, `feedback`,
`fallback_ad_shown`). Fontes: `analytics/FirebaseAnalyticsHelper.kt`,
`analytics/FirebaseAnalyticsTracker.kt`, `analytics/AnalyticsOutboxFunnelTracker.kt`,
`core/recommendation/.../RecommendationAnalytics.kt`.

**Dívida estrutural que afeta quem lê o código:** todo o app consumer ainda mora fisicamente em
`io/signallq/app/...` apesar do package declarado ser `io.signallq.app`. Não é problema
funcional para o usuário, mas confunde qualquer navegação por caminho.

---

## 8. Fora de escopo

- **SignallQ Pro** — produto **descontinuado permanentemente** (ADR-016). Nada do Pro é
  descrito aqui, mesmo quando uma ferramenta do consumer é declarada no código como "versão contida"
  de um recurso que existiu no Pro (caso do Sinal WiFi / Walk Test).
- **Painel Admin** — repositório `buildea-admin`. O worker `signallq-admin-worker` é deste
  repositório, mas o painel que o consome não é.
- **Site e PWA** — repositório `signallq-web`.
- **Arquitetura interna, contratos e engines** — ver `docs_ai/TECNICO.md`,
  `docs_ai/ARQUITETURA/` e `docs_ai/CONTRATOS/`.
- **Design system e tokens** — ver `docs_ai/DESIGN_SYSTEM.md`.
