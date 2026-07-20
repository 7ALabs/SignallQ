# Changelog — SignallQ Android

Todas as mudanças notáveis do app Android serão documentadas aqui.

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/).

> Histórico de marca: Linka (até 0.14.x) → Veloo (0.15.0) → **SignallQ** (0.16.0+).
> Identificadores técnicos (`io.signallq.app`, repo `linka-android`) permanecem por compatibilidade.

---

## [Unreleased]

### Corrigido
- Perfil de rede do Speedtest (tamanho do pool HTTP para rede medida/Wi-Fi) deixa de ficar
  congelado no valor resolvido na criação do executor — agora é resolvido no início de cada
  execução, refletindo a rede atual mesmo depois de trocar sem reiniciar o app (#1221)
- Resultado de Speedtest contaminado por troca de rede, parcial ou com poucas amostras válidas
  deixa de ser tratado como diagnóstico conclusivo: novo `MeasurementStatus`
  (Completo/Parcial/Inconclusivo/Contaminado) e `executionId` vinculam Resultado, Diagnóstico,
  IA e Recomendação à mesma execução, bloqueando análise automática por IA, Recommendation
  Engine e contato com operadora quando o teste não é confiável (#1225)
- Consolidados os dois geradores de PDF divergentes (pós-Speedtest e Laudo de diagnóstico) num
  motor único (`core/relatorio`), removendo a seção regulatória desatualizada "Conformidade
  ANATEL" (mínimo garantido 40%, Resolução nº 574/2011) e renomeando o documento do app
  consumidor de "Laudo Técnico" para "Relatório de diagnóstico da conexão" (#1219)

## [0.29.0] — 2026-07-20

Auditoria de design completa contra o Design System vivo (12 lotes de varredura) e duas
features novas na aba Jogos e no hub Ferramentas.

### Adicionado
- Detecção de tipo de NAT (Aberto/Moderado/Restrito) via STUN (RFC 5389) na tela Jogos — sonda
  roda em paralelo à medição de latência, nunca bloqueia nem atrasa o resultado, card
  informativo dedicado com linguagem reconhecível por quem já viu isso em console (#1200/#1202)
- Nova ferramenta "Sinal WiFi" no hub Ferramentas: indicador dinâmico de sinal e velocidade de
  link (PHY rate) via amostragem em tempo real enquanto o usuário se move pela casa, mais
  identificação do padrão Wi-Fi (4/5/6/6E/7) e selo de suporte a MU-MIMO — versão contida do
  Walk Test do SignallQ Pro (#1201/#1203)
- Veredito textual (Excelente/Regular/Ruim) nas 6 métricas da tela de resultado do Speedtest —
  antes só a cor comunicava a faixa, agora vem com rótulo, inclusive acessibilidade (#1198/#1199)
- Monetização por anúncio nativo (AdMob) sai do modo teste: App ID e os 5 Ad Unit IDs de
  produção plugados, e as 5 telas com slot reservado (Resultado, Histórico, Jogos,
  Dispositivos, Speedtest) passam a carregar `NativeAd` real em vez do card simulado —
  decisão de monetização registrada em ADR-010 (#555/#1208/#1210)

### Corrigido
- Cor da aba ativa da navegação inferior corrigida de azul para violeta, alinhada à regra do
  acento único do Design System (#1193)
- ~30 ocorrências de borda proibida em cards removidas em 10+ arquivos (Home, Sinal, Velocidade,
  Jogos, Equipamento, Ajustes) — padrão que zerava contraste card/fundo no tema claro (#1195/#1196/#1197)
- Ícones fora do padrão Material Symbols Outlined, radius/espaçamento/alpha fora de token, cor
  de seleção do seletor de equipamento, acentuação faltando no guia de canal Wi-Fi, contraste
  quebrado no modo escuro de um badge, e inconsistência de cor/ícone da marca WhatsApp entre
  duas telas — pente fino completo do achado #1196

## [0.28.0] — 2026-07-18

Ciclo de correção funcional do motor de diagnóstico, redesign completo da tela Equipamento de internet e limpeza sistêmica de contraste no modo escuro.

### Alterado
- Redesign completo da distribuição de cards da tela Equipamento de internet: nova ordem narrativa (identidade → status → disponibilidade/uso em pares 2-col → alerta → topologia → módulos técnicos → dispositivos → ações), grid consistente (card ocupa tudo ou par simétrico, nunca card mais largo que outro), tela extraída de 1549 para 549 linhas em 6 componentes dedicados (#1116)

### Corrigido
- Latência do Speedtest passa a medir contra a sonda dedicada (mesma usada pela tela Jogos) em vez do host público, que sofria degradação silenciosa e inflava o número em até ~30x; corrige divergência entre a tela Resultado e a tela Jogos, e restaura o cálculo de atraso sob carga (bufferbloat), que antes sempre dava zero (#1118/#1125)
- Contraste de status (verde/âmbar/vermelho) corrigido em todo o app no modo escuro: texto e ícone de veredito (Excelente/Bom/Regular/Fraco) deixavam de ser legíveis sobre o próprio fundo colorido em dezenas de telas — diagnóstico, Equipamento de internet, Início, DNS, Ping, Sinal, Ajustes, Speedtest e outras (#1115/#1126/#1127/#1128/#1129)
- 5 bugs de UI: filtro de segmento cortado no Histórico (realocado abaixo do subtítulo "Medições recentes"); placeholders de protótipo "Lista/Vazio/Erro" removidos de Dispositivos; anel verde de "conectado" do roteador corrigido no Início (Caminho da sua Internet); card de Medição do Início agora navega para o Histórico; contraste dos botões de Ajustes > Dados e privacidade corrigido no modo escuro (Resetar app) (#1114)

## [0.27.0] — 2026-07-18

Ciclo curto de correções pontuais e padronização de UI sobre o redesign da 0.26.0 — TopBar, tela Sinal, Histórico, Onboarding e Equipamento de internet.

### Adicionado
- Padronização de ícone no título da TopBar por raiz de aba (#1100)

### Corrigido
- TopBar 100% estática, sem reação a scroll (#1100)
- Botão Fechar de Ajustes movido para `navigationIcon` (#1100)
- Container branco+borda para logo de operadora e arte de jogos (#1106)
- `GatewayConnectionSheet` órfã removida de `AjustesScreen` (#1099)
- Z-order de overlays empilhados e CTA de credenciais do equipamento corrigidos
- Artwork não oficial de jogos desativado, fallback de cor+sigla (#1097)
- Medições do Monitoramento passivo excluídas da lista do Histórico (#1096)
- Scroll vertical adicionado ao detalhe do teste no Histórico (#1095)
- Texto quebrando linha na aba Móvel da tela Sinal (#1089)
- Comunicação visual de permissão já concedida no toggle de onboarding
- Trava em "Sessão expirada" na tela Equipamento de internet (#1090)
- Indicador "Ao vivo" removido e aviso de canal consolidado na tela Sinal (#1088)
- Pills DNS/Ping/Diagnóstico e info técnica removidos do card Wi-Fi da Home (#1086)

## [0.26.0] — 2026-07-17

Acumula todo o trabalho desde a 0.25.0 (publicada na trilha alpha do Play Console em 10/07) — 7 dias de trabalho intenso do squad, ~100 PRs. Destaques: redesign completo de UI (Material 3 To-Be), motor de topologia de rede unificado, monetização nativa (AdMob) e integração com diagnóstico remoto.

### Adicionado
- **Redesign completo Material 3 To-Be** (8 fases, #939–#949): nova navegação (Ferramentas na tab bar, Perfil/Ajustes via avatar em vez de aba própria), Onboarding redesenhado em 2 telas, telas Início/Velocidade/Sinal/Ferramentas/Equipamento de internet (substitui Fibra/ONT)/Monitoramento redesenhadas, nova tela Jogos (teste de conexão direcionado por jogo), fundação de ícones Material Symbols (variable font, #1014)
- **Motor de topologia de rede unificado** (5 fases, #975/#976, #986–#994): substitui 3 classificadores concorrentes (Home, Sinal→Redes, Recommendation Engine) por um único motor com contratos canônicos, catálogo OUI único com nível de validação, e correlação best-effort LAN + Wi-Fi + client snapshot na tela Dispositivos
- **Monetização nativa (AdMob)**: anúncios nativos em Velocidade, Resultado, Dispositivos e Histórico, com elegibilidade por sinal contextual, consentimento UMP e fallback seguro (#555)
- **Diagnóstico remoto**: `signallq-diagnostic-worker` migrado para o monorepo e ligado ao app, com diretório de operadoras (logo/contato) e fallback local versionado (#962/#965/#966/#969/#970/#972/#973)
- Análise detalhada (tela 1a) humanizada pela IA, distinguindo laudo automático de análise pedida pelo usuário (#997/#1013/#1017)
- Captura de banda Wi-Fi (2.4/5GHz) na medição de velocidade, refletida no histórico (#1035)
- `AlertCard` acionável para sinal óptico abaixo do esperado na tela de Equipamento (#1033)
- Regra filtrada para rede móvel no catálogo local do `coreRecommendation` (#999)

### Corrigido
- 6 bugs de layout achados na rodada de QA de 2026-07-17, verificados ao vivo em emulador antes de fechar (#1075/#1076/#1077/#1078/#1079/#1080/#1081): sobreposição de texto no card "Caminho da sua internet" da Home; seletor prototype-only (resíduo de protótipo do design system) sobrando em Ping e no sheet de permissão de localização; fallback de avatar divergente ("L" hardcoded vs. "?") entre dois componentes; `TabRow` da tela Sinal renderizando bloco sólido sem texto por falta de `tabIndicatorOffset`; TopBar de Equipamento de internet e Dispositivos sem inset de status bar; título de KPI cortado pelo badge de fonte no Admin
- 6 bugs de implementação da tela Speed Test — Fase 2 da varredura de design (#1020)
- Contraste WCAG e cor de veredito no Histórico; composables mortos removidos (#1029)
- Divergências de estilo das sheets de Operadora 1a/1b vs. protótipo Fase 2 (#1021)
- Nó AP/mesh na tela Sinal roteado com dado real do scan LAN, não mock (#1038)
- Ações fantasma de `EquipamentoInternetScreen` ligadas a fluxos reais (#1037)
- Timeout do motor de diagnóstico remoto ampliado de 5s para 42s (#1011)
- Stub fake de "Modelos compatíveis" e seletor prototype-only removidos de `GatewayConnectionSheet` (#995/#1001)
- Scan de dispositivos não trava mais indefinidamente (#887/#901)
- Diversos fixes de consistência visual, cor fixa vs. token real, e código morto (#1024/#1028/#1030/#1032/#1034/#1039 e outros)

### Removido
- Código morto do chat de diagnóstico IA (`ChatDiagnosticoIaScreen`, `ChatDiagnosticoIaViewModel`, `ChatDiagnosticoIaRepository`, `CotaIaRepository`, `LLMChatScreen` e domínio associado) — decisão de produto fechada em 2026-07-12: sem chat com IA no app. Nunca teve ponto de entrada em produção (`chatDiag`/`AppShellChatDiagState` eram instanciados mas nunca renderizados a partir de `MainActivity.kt`/`AppShell.kt`, confirmado em #215, #222 e #850). Tabelas Room `chat_sessions`/`chat_messages` e `ChatSessionDao` mantidos — ainda usados pelo `AdminSyncWorker` para sync de uso de IA legado ao Admin (#912)
- Classificadores de topologia paralelos, superados pelo motor unificado (Fase 2C, #1059/#981)
- Algoritmo de amostragem de ping consolidado num único `AnalisadorAmostragemPing` (#1019/#1034)

## [0.25.0] — 2026-07-10

Acumula todo o trabalho desde a 0.24.1 (nunca publicada como release own — a 0.24.2/versionCode 59 foi bumpada mas não taggeada).

### Adicionado
- Recommendation Engine desacoplado (`coreRecommendation`) integrado à experiência pós-diagnóstico: sugestão priorizada de ação prática após cada diagnóstico, com histórico persistido e feedback do usuário (#807, #811, #812, #813/#821)
- Avaliação nativa do Google Play (in-app review) sem atrito, elegibilidade decidida por `ReviewPromptPolicy` (#664/#806)
- Guia ilustrado de credenciais do modem na sheet de conexão com o gateway (#529/#805)
- Autoconexão do gateway vinculada ao BSSID em que a sessão "Manter conectado" foi estabelecida — reconecta sozinho na mesma rede Wi-Fi (#802)
- Resumo de bandas Wi-Fi e contagem de dispositivos exibidos direto no card do gateway (#803)
- Seção "Equipamento local" (Nokia GPON): segurança do Wi-Fi, largura de canal e potência de transmissão (#843); gateway, DNS secundário e interface na seção WAN (#842); máscara de sub-rede e faixa de DHCP na LAN (#845); tensão/corrente do laser e serial da ONT (#841); indicador de frescor da leitura (#840); capability `suportaDiagnosticoNativo` (#846)
- Contrato normalizado de equipamento local (`LocalNetworkDeviceSnapshot`) com classificação de `DeviceType`/`SupportLevel`, filtro allowlist e integração ao motor de diagnóstico (#546/#794, #796, #797, #798, #539/#800, #799)
- Parser real de Wi-Fi/LAN do roteador Nokia GPON — antes esses campos existiam só no contrato, sem produtor real (#865/#866)
- Lista de dispositivos conectados usa dado real do próprio roteador (`device_cfg`/`alias_cfg`) para confirmar nome via MAC/IP, com selo "Nome confirmado pelo roteador" na tela Dispositivos na rede (#839/#844, #856, #869)
- Tela Resultado do teste simplificada: diagnóstico IA movido para sheet, com PDF completo (#536/#808)
- Árvore de rede móvel mostra Dispositivo → Operadora com logo real (#524)

### Corrigido
- Diagnóstico automático de fundo (cold start ou releitura de fibra/ONT concluindo) não abre mais o Laudo sozinho por cima da aba Velocidade — supressão anterior (#481) só cobria a primeira conclusão observada, uma segunda conclusão automática escapava (#870)
- Campo de apelido não aparecia quando o MAC do dispositivo não era resolvível (#853/#864)
- Botão de escanear rede na tela Dispositivos ficou visível/acessível (#863)
- Resumo "Minha conexão" exibe a velocidade contratada salva (#849/#860)
- Cor do Switch desabilitado ignorava o estado `checked` (#859)
- `AnatelBanner` não renderizava na Home após a medição (#847/#858)
- Gate de dados móveis passa a usar `metered` de `SnapshotRede` em vez de consulta avulsa (#838/#857)
- `categoriaOrigem` real do achado propagada até a tela de diagnóstico (#836/#837)
- Contato com a operadora exibido para categoria fibra (#832/#834)
- Lista de dias da semana e card de medição única removidos do Histórico (#828)
- Altura da barra 4 do ícone do launcher corrigida (#825/#831)
- `CardMedicoes` não reserva mais altura de gráfico sem 2+ pontos válidos (#827/#830)
- 3 bugs Android + sheet de conexão ao gateway (#789)
- Status enviado ao Admin passa a ser o veredito real da IA/motor (#776)
- Jargão "5G NSA"/"5G SA" removido de textos voltados ao usuário (#533)
- Furo do "o" no logo da Vivo agora é transparente, não branco sólido (#523)
- Mensagem de DECISAO-02/02b não fala mais de Wi-Fi em rede móvel (#519)
- Speedtest inicia direto ao confirmar aviso de dados móveis na Home (#520)
- Contraste WCAG AA do violeta sobre superfície escura corrigido (#513)
- DNS exige clique em Iniciar, igual ao Ping — antes iniciava sozinho (#511)
- Copy contraditória entre canal estável e troca de canal corrigida (#510)
- TopAppBar da Home mantém-se sólido durante fade de scroll (#509)
- Gráfico de histórico não quebra mais com 1 única medição (#508)
- Uptime de 7 dias não reporta mais 100% sem dado medido (#507)
- Nome de gateway travado e fallback sem rótulo claro corrigidos (#219/#506)
- UX de tipo de equipamento e leitura de fibra clareada entre Nokia e roteador genérico (#538/#801)

### Alterado
- Hierarquia visual do diagnóstico detalhado simplificada (#833/#835)
- Fonte `routerActive` reservada e proveniência do nome indicada na UI de dispositivos (#804)

### Removido
- `DiagnosticoScreen` órfã e componentes exclusivos dela (`DiagActionFooter`, `DiagImpactCard`, `DiagMetricsGrid`, `DiagRecommendationCard`, `DiagRootCauseCard`, `SignalToggleCard`) — código morto, tela substituída pelo Laudo em sheet (#868)

## [0.24.1] — 2026-07-05

### Corrigido
- Card "Análise por IA" no resultado do speedtest agora exibe as ações recomendadas (até 2, priorizadas) além do diagnóstico — antes só mostrava o texto de leitura/causa provável, sem a ação prática que a IA já retornava (#498)

## [0.24.0] — 2026-07-05

### Corrigido
- Número de WhatsApp da TIM inválido na tela de contato da operadora (#485, #486)
- Cor semântica real nos cards de resultado do speedtest — Download/Upload/Latência agora refletem a severidade medida em vez de cor fixa, tanto no resultado detalhado quanto no card "Último resultado" (#490, #497)
- Metadado técnico cru (Banda/RSSI/Canal/BSSID) removido da lista principal de redes Wi-Fi, substituído por veredito humano; detalhe técnico continua disponível no sheet (#491)
- `UptimeGridChart` (grid de uptime de 7 dias) nunca era renderizado na tela Histórico apesar do dado já existir — bug funcional corrigido (#495)
- Exportação de histórico agora respeita o filtro de conexão/operadora ativo em vez de sempre exportar tudo (#495)
- Ação destrutiva (apagar dados locais/resetar app) na tela de Privacidade agora exige confirmação, igual ao resto do app (#496)
- Cancelar um teste de velocidade em andamento agora sempre pede confirmação, pelo botão ou pelo gesto de voltar (#493)

### Alterado
- Veredito de qualidade (Excelente/Bom/Regular/Fraco) ao lado de Download/Upload na Home (#489)
- Alerta de canal Wi-Fi congestionado e recomendação de troca de canal na tela Sinal (#491)
- Três pontos de "apagar/limpar/resetar dados" consolidados num único destino, com ações escalonadas por gravidade (#496)
- "Minha conexão" migrou de tela cheia para bottom sheet, no mesmo padrão do Perfil (#496)
- Jargão técnico traduzido (Bufferbloat com veredito, badge de IA autoexplicativo, "Contaminado" → "Pode não ser confiável") no Histórico (#495, #497)

### Documentação
- `PRODUCT.md` e `DESIGN.md` adicionados na raiz do repo, formalizando o design system em complemento à skill `linka-design`

## [0.23.0] — 2026-07-05

### Adicionado
- Logos oficiais reais de operadoras e catálogo local de badges (SIG-292) (#472, #467)
- Exibição dos canais oficiais da operadora identificada no diagnóstico (#466)
- Instrumentação de analytics: funil principal com 7 eventos via `AnalyticsHelper` (#473) e `feature_used` em Wi-Fi, DNS, Fibra e Histórico (#469)

### Corrigido
- Ingest de speedtest via Wi-Fi agora inclui a operadora/ISP identificada (#468)
- Quebras de layout com fonte grande do sistema (#470)
- Matcher de operadora não confunde mais "Oi" com "Nio" (#465)
- Exceção ao invocar `executarSpeedtest` agora é logada em vez de engolida silenciosamente (#433)
- Sinal › Dispositivos exibe "Dispositivo <Fabricante>" (ex.: "Dispositivo Samsung") via fallback de fabricante por OUI do MAC quando o hostname não é resolvido, em vez do rótulo genérico "Dispositivo" sem nenhuma pista (#394). Resolução completa de nome via mDNS/NetBIOS fica para issue de feature separada.

### Alterado
- Upgrade coordenado de toolchain: AGP 9.2.1, Kotlin 2.3.21, Gradle 9.4.1 (#445)

## [0.22.1] — 2026-07-03

### Publicação
- Primeira publicação automatizada na Play Console (trilha de teste fechado) via gradle-play-publisher no release por tag.

### Corrigido
- Roteador dual-band único (mesmo SSID/OUI em 2.4 GHz e 5 GHz) não é mais classificado como sistema mesh nem rotulado como "Nó #N"; agora aparece como "Roteador dual-band" com as bandas identificadas (#356)
- Chip "Conectado" no card da rede em Sinal › Wi-Fi não quebra mais caractere a caractere com SSID longo; o SSID trunca com reticências (#355)
- Aba "Dispositivos" na barra de abas da tela Sinal não quebra mais em duas linhas (#354)

---

## [0.22.0] — 2026-06-29

### Adicionado
- Item "Fale conosco" na tela de Ajustes

### Alterado
- Ícone do app atualizado para o símbolo SignallQ (barras de sinal roxo→azul)

### Corrigido
- Acessibilidade TalkBack e compatibilidade com dark theme
- Baseline Profile atualizado para `io.signallq.app`

### Melhorias de desempenho
- Startup time reduzido
- Consumo de bateria do worker de monitoramento otimizado
- Tamanho do APK reduzido

---

## [0.21.0] — 2026-06-22

### Adicionado
- Novo ícone do app em todas as densidades com ícone adaptativo (SIG-7/SIG-8)
- Worker de privacidade para conformidade de dados pessoais
- CI com Ktlint, Detekt, testes unitários e build APK em cada PR (SIG-28/SIG-37)

### Corrigido
- Painel admin: erro de telemetria no Overview agora exibe mensagem clara
- Painel admin: dados de teste vazando para produção removidos (SIG-9)

### Alterado
- Formatação Ktlint corrigida em ~30 arquivos
- Toolchain: AGP 9.1.1, Gradle 9.3.1, compileSdk 37, Kotlin 2.2.20

---

## [0.20.0] — 2026-06-18

### Adicionado
- Painel administrativo SignallQ (React/TS) com login, métricas e gestão
- Ingest de diagnósticos no D1 via worker Cloudflare
- Integração Firebase Analytics com GA4
- Multi-provider de IA (Qwen3 30B + fallback Gemini Flash)
- Laudo de diagnóstico como resultado primário, IA como explicador
- HiltWorkerFactory para injeção de dependência nos Workers

### Alterado
- Diagnóstico reformulado: laudo estruturado em vez de chat livre

---

## [0.19.0] — 2026-06-15

### Alterado
- Refatoração completa da arquitetura: decomposição de telas em composables independentes
- ViewModels extraídos por funcionalidade (Dispositivos, Speedtest, Diagnóstico)
- Módulos de feature sem dependências cruzadas
- Orquestrador de diagnóstico movido para `featureDiagnostico`
- Cache de IA com expiração (TTL 5 min)
- OkHttpClient singleton para UPnP e scan de dispositivos
- Overlay de diagnóstico antigo removido

---

## [0.16.0] — 2026-06-01

### Alterado — Rebrand para SignallQ
- Toda a UI, copy e documentação renomeados de Linka/Veloo/Orbit para **SignallQ**
- Assistente de IA passa a se chamar SignallQ (não mais "Orbit")
- Identificadores técnicos preservados por compatibilidade de infraestrutura

### Adicionado
- Tela de dispositivos: nomes via jmDNS + SSDP, scan concorrente, detecção por MAC

---

## [0.15.1] — 2026-06-12

### Corrigido
- Nó "Roteador" na trilha de rede mesh exibia dado vazio em vez do placeholder "—"

---

## [0.15.0] — 2026-05-30

### Alterado — Rebrand Linka → Veloo
- Identidade visual, package name e configurações Firebase atualizados para Veloo
- Tela de novidades v0.15.0

---

## [0.14.0] — 2026-05-29

### Adicionado
- Novo fluxo de diagnóstico por laudo (setup → análise → resultado) em vez de chat
- Assistente de IA em tela standalone com chat livre sobre conexão
- 8 componentes UI novos para o laudo (veredito, causa-raiz, impacto, recomendações, métricas)
- Seleção de sinais para análise direcionada (velocidade, Wi-Fi, latência, fibra, DNS)

### Alterado
- Botão "Diagnóstico IA" na Home agora abre o fluxo de laudo
- Chat inline removido da tela de diagnóstico

---

## [0.13.3] — 2026-05-28

### Corrigido
- Fibra: abertura automática da tela ao iniciar o app removida
- IP público do Wi-Fi não aparece mais na sheet de rede móvel
- Detecção de 5G NSA corrigida com fallbacks adicionais

---

## [0.13.2] — 2026-05-28

### Corrigido
- Nome da operadora agora vem do snapshot móvel, não do ISP externo
- Card SIM duplicado removido em dispositivos single-SIM
- SSID Wi-Fi nulo no Android 12+ corrigido
- Filtro de banda 2.4GHz exibe banner correto quando conectado em 5GHz

### Alterado
- SimCard redesenhado com operadora em destaque e badge "EM USO"
- Cards de contexto removidos da tela de velocidade

---

## [0.13.0] — 2026-05-28

### Alterado — Mockup v2 UI
- Home: ordem fixa dos cards, sinal com qualidade em palavra (Forte/Regular/Fraco), chips de SIM compactos
- Sinal: 3 abas fixas (Wi-Fi, Canal, Móvel) sempre visíveis
- Velocidade: subtítulo com plano contratado, seletor de modo neutro, métricas em 3 colunas
- Resultado: nota em círculo removida, avaliação em texto direto

---

## [0.12.0] — 2026-05-28

### Adicionado
- Chat de diagnóstico IA com estilo conversa moderna (Material 3)
- 3 opções rápidas: analisar último teste, executar novo teste, analisar histórico
- Streaming SSE de respostas da IA com auto-scroll
- Persistência de sessões de chat via Room (criar, apagar, renomear)
- Cota diária de IA com janela rolling de 24h e banner de renovação
- Mensagens humanizadas para 5 cenários de erro
- TopBar contextual com SSID/operadora na Home
- Card "Sua Conexão" destacado na tela Sinal
- Mini-cards DNS, Ping e Diagnóstico na Home
- Seletor Android/Roteador na aba Canal
- Chip de segurança Wi-Fi (WPA3/WPA2/WPA/Open)
- Card de rede móvel com suporte a dual SIM

### Corrigido
- Cards de informação movidos da Home para a tela de Resultado
- Operadora nula em medições via chat corrigida

---

## [0.11.4] — 2026-05-27

### Corrigido
- DNS do provedor não reconhecia Registro.br e CETIC.br no comparativo
- Filtro de latência excluía DNS rápidos de operadora em redes 5G/4G

---

## [0.11.3] — 2026-05-27

### Corrigido
- Trilha de rede mostrava "sem internet" enquanto buscava IP público

---

## [0.11.2] — 2026-05-27

### Corrigido
- Crash na abertura do app (NullPointerException) por ordem de inicialização

---

## [0.11.1] — 2026-05-27

### Corrigido
- Detecção de 5G NSA em Samsung Exynos e Xiaomi MIUI 14+
- Histórico vazio ao filtrar por rede móvel
- Botão voltar fechava o app na aba Histórico
- Trilha de rede intermitente durante handoff Wi-Fi → Móvel

---

## [0.11.0] — 2026-05-26

### Adicionado
- Tela Fibra/Modem com análise de RX/TX, temperatura e status óptico
- Tela DNS com benchmark de 7 provedores, recomendação e guia de configuração
- Onboarding com aceite obrigatório de termos e cards de permissão
- Mensagens humanizadas para estados vazios

### Corrigido
- MAC address parcialmente mascarado na tela de dispositivos (privacidade)

---

## [0.10.0] — 2026-05-26

### Adicionado
- Chat inline com IA na tela de diagnóstico (Gemma 4 26B via Cloudflare Workers)
- Histórico com gráfico de testes e cards de velocidade média
- Diagnóstico redesenhado com 5 cards (status, problema, ações, evidências, chat)
- Classificação automática de topologia Wi-Fi (roteador, mesh, repetidor)
- Streaming SSE no chat com cursor pulsante em tempo real
- Detecção de padrões de uptime (horários recorrentes, interrupções longas, tendência)
- Exportação de histórico em PDF com paginação automática

---

## [0.9.0] — 2026-05-20

### Adicionado
- Central de Testes com grid de ferramentas (DNS, Ping, Diagnóstico)
- Ping/Latência: 20 amostras ICMP com mediana, jitter e perda de pacotes
- DNS Benchmark: Registro.br e CETIC.br adicionados (7 provedores total)
- Proteção de dados móveis antes de testes pesados
- Preferência para sempre permitir testes em dados móveis
- Consumo mensal de dados em testes

### Alterado
- Separador visual com ponto médio no DNS Benchmark
- Ferramentas movidas de bottom sheet para grid na tela de Velocidade

---

## [0.8.4] — 2026-05-19

### Corrigido
- App mostrava "4G" em redes 5G NSA por falha na detecção por OEM

---

## [0.8.3] — 2026-05-19

### Alterado
- Sinal móvel redesenhado com gauge de RSRP, textos humanizados e diagnóstico
- Canal Wi-Fi com destaque e recomendação em linguagem natural
- Monitoramento de uptime com lista de eventos por dia
- Nó mesh na trilha de rede com ícone Hub

### Corrigido
- "Diagnóstico Inteligente" oculto em release via feature flag
- Label de internet duplicada em conexão móvel
- Nó móvel sem IP com tecnologia como sublabel
- Operadora correta no perfil em conexão móvel
- Item duplicado "Dados usados" removido dos Ajustes

---

## [0.8.2] — 2026-05-19

### Corrigido
- Navbar fixa (animação de scroll-hide causava espaço vazio)
- Trilha de rede proporcional à tela
- "Modo Gamer" renomeado para "Jogar Online"

### Alterado
- Avatar do perfil exibe dados de conexão em vez de nome do aparelho
- Gráfico de monitoramento reorientado para leitura horizontal

---

## [0.8.1] — 2026-05-19

### Corrigido
- 10 correções visuais/UX (modem dinâmico, títulos, cores, labels, acessibilidade)
- Classificação de sinal Wi-Fi distingue 2.4GHz de 5GHz
- Tipo de conexão real enviado para IA (era Wi-Fi fixo)
- Limiar de RX Power alinhado ao padrão ITU-T G.984

---

## [0.8.0] — 2026-05-18

### Adicionado
- Tela Fibra/Modem visível em produção
- DNS Benchmark visível em produção
- Chat com IA disponível apenas em debug

### Corrigido
- Botão "Testar Novamente" duplicado removido
- Overlays protegidos por feature flags

---

Versões anteriores a 0.8.0 não possuem registro neste arquivo.
