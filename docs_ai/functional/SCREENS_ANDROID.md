# Telas Android — Linka

**Última atualização:** 2026-05-18
**Fonte:** código real (Marcelo, 2026-05-17)
**Arquivo de navegação:** `AppShell.kt`, `AppNavGraph.kt`
**Diretório de telas:** `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/`

---

## Visão Geral — 15 Telas

| # | Composable | Arquivo | Tipo | Entrada | Saída |
|---|---|---|---|---|---|
| 1 | `HomeScreen` | `HomeScreen.kt` | Aba 0 | Início / app launch | SpeedTestScreen, SinalScreen |
| 2 | `SpeedTestScreen` | `SpeedTestScreen.kt` | Aba 1 | HomeScreen | VelocidadeScreen, DiagnosticoScreen, DnsBenchmarkSheet |
| 3 | `SinalScreen` | `SinalScreen.kt` | Aba 2 | HomeScreen, SpeedTestScreen | — |
| 4 | `DispositivosScreen` | `DispositivosScreen.kt` | Aba 3 | NavigationBar | — |
| 5 | `HistoricoScreen` | `HistoricoScreen.kt` | Aba 4 | NavigationBar | — |
| 6 | `VelocidadeScreen` | `VelocidadeScreen.kt` | Fluxo | SpeedTestScreen (auto) | ResultadoVelocidadeScreen |
| 7 | `ResultadoVelocidadeScreen` | `ResultadoVelocidadeScreen.kt` | Fluxo | VelocidadeScreen (auto) | DiagnosticoScreen, ChatScreen, HomeScreen |
| 8 | `DiagnosticoScreen` | `DiagnosticoScreen.kt` | Fluxo | ResultadoVelocidade, SpeedTestScreen | ChatScreen |
| 9 | `ChatScreen` | `ChatScreen.kt` | Fluxo | ResultadoVelocidade, DiagnosticoScreen | ResultadoVelocidadeScreen |
| 10 | `AjustesScreen` | `AjustesScreen.kt` | Drawer | Menu/Drawer | FibraScreen, LaudoScreen, HistoricoScreen |
| 11 | `FibraScreen` | `FibraScreen.kt` | Fluxo | AjustesScreen | AjustesScreen |
| 12 | `LaudoScreen` | `LaudoScreen.kt` | Fluxo | AjustesScreen | AjustesScreen |
| 13 | `OrbitScreen` | `OrbitScreen.kt` | Inline | — | ChatScreen |
| 14 | `LinkaPulseScreen` | `LinkaPulseScreen.kt` | Overlay | — | — |
| 15 | `OnboardingScreen` | `OnboardingScreen.kt` | Especial | Primeira execução | HomeScreen |

---

## Detalhes por Tela

### HomeScreen

**Tipo:** Aba 0 da NavigationBar

**Parâmetros recebidos:** `snapshotRede`, `snapshotSpeedtest`, `history`, `ultimaMedicao`, `localIp`, `publicIp`, `ispInfo`, `gateways`, `deviceName`, `nomeUsuario`, `fotoUriUsuario`, `connectedNetwork`, `movelSnapshot`

**Cards e seções:**
- **OfflineCard (novo):** exibido como primeiro item quando offline
  - Ícone WifiOff, botão "Testar assim que voltar"
  - Monitora conectividade e auto-dispara teste ao reconectar
- Card de perfil: foto + nome do usuário + rede conectada
- Speed summary: última medição DL/UL
- Device list: lista de gateways detectados
- History chart: mini-gráfico de histórico
- Uptime narrative: texto gerado pelo engine

**Estados visuais:**
- Offline: OfflineCard exibido, resto do conteúdo atenuado
- Wi-Fi: SSID + RSSI + banda
- Dados móveis: operadora + tecnologia + RSRP
- Com histórico: gráfico + narrativa
- Sem histórico: estado vazio

**Navegação de saída:**
- → `SpeedTestScreen` (botão "Testar velocidade"; diálogo de confirmação se dados móveis)
- → `SinalScreen` (botão de redes)
- → `PerfilEditSheet` (toque no perfil)

---

### SpeedTestScreen

**Tipo:** Aba 1 da NavigationBar

**Parâmetros recebidos:** `snapshotSpeedtest`, `snapshotRede`, `ispInfo`, `localizacaoServidor`, `modoSelecionado`, `onModoSelecionado`, `onIniciarTeste`, `onCancelarTeste`, `onAbrirDnsBenchmark`, `onAbrirDiagnostico`, `onVoltar`, `conectado`

**Cards e seções:**
- `SpeedTestCircle`: gauge central animado
- **Indicador Offline (novo):** "Sem conexão — teste indisponível" abaixo do círculo (se offline)
- `ModeSelector`: pills de seleção de modo (rápido / completo / triplo) — desativado se offline
- `LastResultCard`: último resultado DL/UP
- `CardContextoUso`: avaliação de suporte a videochamada, streaming HD, jogos, home-office
- `CardRqualAnatel`: comparação com 40% mínimo e 80% normal ANATEL em relação ao plano contratado
- `CardBufferbloat`: severidade (none / mild / moderate / severe)
- `CardRodadasTriplo`: expandível com 3 rodadas individuais (modo triplo)
- `ExploreToolsRow`: acesso a DNS Benchmark e Diagnóstico
- `StatusCard`: status Wi-Fi / operadora / servidor

**Diálogos:** confirmação de uso de dados móveis com estimativa de consumo

**Estados visuais:**
- Offline: indicador visível, botão inativo
- Idle: pulse animado
- Executando: círculo de progresso + velocidade ao vivo
- Concluído: check

**Navegação de saída:**
- → `VelocidadeScreen` (teste iniciado)
- → `DiagnosticoScreen` (via ExploreToolsRow)
- → `DnsBenchmarkBottomSheet` (via ExploreToolsRow)

---

### SinalScreen

**Tipo:** Aba 2 da NavigationBar

**Parâmetros recebidos:** `snapshotWifi`, `connectedNetwork`, `temPermissaoLocalizacao`, `onSolicitarPermissaoLocalizacao`, `temPermissaoTelefonia`, `onSolicitarPermissaoTelefonia`, `conectado`, callbacks para scan e refresh

**Sheets de permissão (novo):**
- `PermissaoLocalizacaoContextoSheet`: exibida ao entrar em Wi-Fi sem `ACCESS_FINE_LOCATION`
  - Oferece opções: "Agora não" ou "Entendi, conceder"
  - Se dismissada, banner clicável reabre o sheet
- `PermissaoTelefoniaContextoSheet`: exibida ao entrar em Móvel sem `READ_PHONE_STATE`
  - Oferece opções: "Agora não" ou "Entendi, conceder"
  - Se dismissada, banner clicável reabre o sheet

**Cards e seções:**
- `OfflineBanner` (novo): exibido no topo quando sem conectividade
- `TabRow`: filtro por banda (Todas / 2.4GHz / 5GHz / 6GHz)
- `RedeCard` por rede: SSID + RSSI + canal + segurança + OUI (fabricante)
- `WifiChannelGuide`: visual de congestionamento de canais
- `BottomSheet`: análise de topologia e recomendações

**Dados por rede vizinha (`RedeVizinha`):** SSID, BSSID, RSSI (dBm), canal, frequência (MHz), segurança, OUI

---

### DispositivosScreen

**Tipo:** Aba 3 da NavigationBar

**O que exibe:** 
- `OfflineBanner` (novo): exibido no topo quando sem conectividade
- Lista de dispositivos na rede com apelidos customizáveis por MAC

**Ações:** dar/editar apelido (salvo em Room), refresh da lista.

**Estados visuais:** Loading / Lista / Vazio / Offline / Erro

---

### HistoricoScreen

**Tipo:** Aba 4 da NavigationBar

**Cards e seções:**
- Gráfico de histórico / uptime
- Uptime narrative (texto gerado pelo `UptimeNarrativaEngine`)
- Resumo de medições

---

### VelocidadeScreen

**Tipo:** Fluxo secundário (sobreposto)

**Parâmetros recebidos:** `snapshot`, `localizacaoServidor`, `ispInfo`, `onCancelar`, `onReiniciar`

**Componentes:**
- `GaugeCircular`: gauge central com progresso global + fase atual (LATÊNCIA/DOWN/UP/CONCLUÍDO) + velocidade em Mbps
- `MiniGrafico`: gráfico ao vivo de `PontoAoVivo`
- `PillsFase`: status de cada fase com checkmark ao concluir
- `LinhaServidor`: localização + ISP
- `ErroContent`: botões "Testar Novamente" e "Cancelar" (só em erro)

**Transições:** haptics entre fases

**Navegação de saída:** → `ResultadoVelocidadeScreen` (automático ao concluir)

---

### ResultadoVelocidadeScreen

**Tipo:** Fluxo secundário (sobreposto)

**Parâmetros recebidos:** `resultado`, `snapshotDiagnostico`, `onTestarNovamente`, `onIrParaHome`, `onAbrirChat`, `gemmaAvailable`

**Layout em ordem:**
1. Grade circle: classificação A / B / C / D / ? com cor
2. Título + mensagem de diagnóstico
3. Cards DL + UL (Mbps)
4. Cards latência + jitter
5. Chip de contaminação (se detectado)
6. Cards perda de pacotes + bufferbloat
7. Seção EXPERIÊNCIA DE USO: vereditos para Streaming, Gaming, Vídeo Chamada (good/acceptable/poor)
8. DNS Info: provedor + latência
9. Detalhes Avançados (expansível): pico DL/UL, latência com carga, estabilidade
10. `RecomendacaoCard`: ação baseada no diagnóstico

**Botões:** "Conversar com IA", "Testar Upload Novamente", "Ir para o início", "Testar novamente"

**Navegação de saída:**
- → `ChatScreen` (botão "Conversar com IA")
- → `DiagnosticoScreen`
- → `HomeScreen`

---

### DiagnosticoScreen

**Tipo:** Fluxo secundário (sobreposto)

**Parâmetros recebidos:** `snapshotDiagnostico`, `resultado`, callbacks para iniciar, selecionar chips, enviar contexto

**O que exibe:** cards dinâmicos por engine com ícone + `status badge` (OK/INFO/ATTENTION/CRITICAL) + mensagem + recomendação

**Estados:** Idle / Executando (loader) / Concluído

**Navegação de saída:** → `ChatScreen`

---

### ChatScreen (Orbit IA)

**Tipo:** Fluxo secundário (sobreposto)

**Parâmetros recebidos:** `uiState` (Idle/Thinking/AwaitingInput/Error), `onNavigateBack`, `onIniciarOrbit`, `onResetOrbit`, `onSelecionarChip`, `onResponderPergunta`, `onEnviarMensagemTexto`

**Componentes:**
- `OrbitUserMessageBubble`: bolha do usuário
- `OrbitThinkingBubble`: animação "pensando..."
- `OrbitAiMessageBubble`: resposta IA em markdown
- `OrbitInlineQuestion`: chips de resposta rápida
- `OrbitInputArea`: campo de texto + envio
- `AiModelFooter`: info do modelo IA
- `LinkaIaHeader`: cabeçalho da sessão

**API:** `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev`

**Navegação de saída:** → `ResultadoVelocidadeScreen` (voltar)

---

### AjustesScreen

**Tipo:** Drawer/Menu

**Seções e configurações:**

| Seção | Configuração | Tipo de controle |
|---|---|---|
| Perfil | Nome e foto | Campo + upload |
| Provedor | Operadora, plano, região | Campos de texto |
| Tema | Sistema / Claro / Escuro | Seletor (3 opções) |
| Monitoramento | Ativo/inativo | Toggle |
| Notificações | Latência, DNS, RSSI, sem internet | 4 toggles individuais |
| Alerta de velocidade | Limite em Mbps | Campo numérico |
| Análise avançada | On/Off | Toggle |
| Fibra | Host, usuário, senha, manter conectado | Formulário + Toggle |

**Navegação de saída:** → `FibraScreen`, → `LaudoScreen`, → `HistoricoScreen`

---

### FibraScreen

**Tipo:** Fluxo a partir de AjustesScreen

**O que exibe (quando conectado):**
- Status GPON: up/down, Rx (dBm), Tx (dBm), temperatura (°C), corrente laser (mA), voltagem, serial, modo
- Status WAN: IP, máscara, gateway
- Status PPP (se aplicável)
- Informações do dispositivo (modelo ONT)
- Gateway IP detectado automaticamente
- Formulário de configuração: host, usuário, senha
- Toggle "Permanecer conectado"

---

### LaudoScreen

**Tipo:** Fluxo a partir de AjustesScreen

Relatório visual do diagnóstico completo.

---

### OrbitScreen

**Tipo:** Exibição do símbolo animado de Orbit.

---

### LinkaPulseScreen

**Tipo:** Dashboard de monitoramento contínuo com símbolo LinkaPulse animado.

---

### OnboardingScreen

**Tipo:** Exibido apenas na primeira execução (`onboarding_concluido = false` no DataStore).

Fluxo de boas-vindas com slides. Após conclusão, nunca é exibido novamente.
