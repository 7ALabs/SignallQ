# Features — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0 — versionCode 56; flags revisadas contra build.gradle.kts)
**Fonte:** código real (flags verificadas em `app/build.gradle.kts`)

Todas as telas em: `app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/` (caminho físico legado; namespace/package = `io.signallq.app`)

---

## Sistema de Controle de Features (FeatureFlags)

O SignallQ usa um sistema de **FeatureFlags por build type** para controlar quais features são visíveis ao usuário final:

- **Debug:** Todas as flags são `true` — desenvolvedores testam a full stack de features
- **Release (v0.23.0):** Flags ativas confirmadas em `app/build.gradle.kts`: FIBRA_SCREEN=true, DNS_SCREEN=true, DIAGNOSTICO_CHAT=true. Inativas: DEVICES_SCREEN_V2=false, LINKPULSE_ATIVO=false

### Status das Features

| Feature | Categoria | Debug | Release | Observações |
|---|---|---|---|---|
| **Teste de Velocidade** | MVP | ✅ | ✅ | Core do app |
| **Diagnóstico Local** | MVP | ✅ | ✅ | 8 engines especializados |
| **Diagnóstico IA (SignallQ)** | MVP | ✅ | ✅ | Cloudflare Worker, Gemini 2.0 Flash primário / Qwen3 30B fallback cloud / fallback local |
| **Análise Wi-Fi** | MVP | ✅ | ✅ | Scan, topologia, congestionamento |
| **Análise Rede Móvel** | MVP | ✅ | ✅ | 4G/5G, RSRP, RSRQ, SINR |
| **Histórico e Gráficos** | MVP | ✅ | ✅ | Uptime, narrativa, CSV, PDF |
| **Laudo Técnico PDF** | MVP | ✅ | ✅ | Relatório completo exportável |
| **Onboarding** | MVP | ✅ | ✅ | Primeira execução, slides |
| **Permissões Contextuais** | MVP | ✅ | ✅ | Localização, Telefonia |
| **Estado Offline** | MVP | ✅ | ✅ | Banners, guards, detecção automática |
| **Configurações MVP** | MVP | ✅ | ✅ | Perfil, provedor, tema, alertas |
| **Tela Privacidade** | MVP | ✅ | ✅ | Política, LGPD, dados |
| **Tela Novidades** | MVP | ✅ | ✅ | Release notes |
| **Chat IA (LLMChat)** | MVP | ✅ | ✅ | FEATURE_DIAGNOSTICO_CHAT=true em release |
| **LinkPulse Ativo** | Pós-MVP | ✅ | ❌ | Monitoramento contínuo (v0.7.1+) |
| **Notificação Inline** | Pós-MVP | ✅ | ❌ | Notif dentro do app (v0.7.1+) |
| **Widget Home Screen** | Pós-MVP | ✅ | ❌ | Quick access (v0.7.1+) |
| **Quick Settings Tile** | Pós-MVP | ✅ | ❌ | Iniciar teste rapidamente (v0.7.1+) |
| **Prova Real Completo** | Pós-MVP | ✅ | ❌ | Triplo teste extenso (Sprint 2) |
| **Diagnóstico Iterativo** | Pós-MVP | ✅ | ❌ | Refinamento via perguntas (Sprint 2) |
| **Traceroute** | Pós-MVP | ✅ | ❌ | Diagnóstico avançado de rota (Sprint 2) |
| **Fibra Screen** | MVP | ✅ | ✅ | Leitura Nokia GPON — promovida para release em v0.15.0+ |
| **DNS Screen** | MVP | ✅ | ✅ | Benchmark DoH — promovida para release em v0.15.0+ |
| **Devices Screen V2** | Pós-MVP | ✅ | ❌ | Scanner rede com IA (DEVICES_SCREEN_V2=false em release) |
| **Telefonia Avançado** | Pós-MVP | ✅ | ❌ | TelephonyManager deep dive (Sprint 3) |
| **Mapa de Calor Wi-Fi** | Pós-MVP | ✅ | ❌ | Visualização espacial RSSI (Sprint 4) |
| **Agendamento de Testes** | Pós-MVP | ✅ | ❌ | Testes periódicos automáticos (Sprint 4) |
| **LinkPulse Chat** | Pós-MVP | ✅ | ❌ | Chat do monitoramento (Sprint 4) |
| **LinkAsync** | Pós-MVP | ✅ | ❌ | Sincronização em background (Sprint 5) |
| **Backup Local** | Pós-MVP | ✅ | ❌ | Exportação automática dados (Sprint 5) |
| **Contribuição Anônima** | Pós-MVP | ✅ | ❌ | Envio de dados para rede comunitária (Sprint 5) |
| **Rate Us** | Pós-MVP | ✅ | ❌ | Google Play Store feedback (Sprint 5) |
| **Acessibilidade** | Pós-MVP | ✅ | ❌ | TalkBack, contraste, fonte aumentada (Sprint 6) |

**Total:** 32 features. **MVP:** 13. **Pós-MVP:** 19.

---

### Como Usar FeatureFlags no Código

#### Verificação Simples

```kotlin
import io.signallq.app.FeatureFlags

// Em qualquer lugar:
if (FeatureFlags.LINKPULSE_ATIVO) {
    // Exibe LinkPulse apenas em debug
}
```

#### Em Composables

```kotlin
@Composable
fun HomeScreen() {
    Column {
        LastResultCard()
        
        if (FeatureFlags.LINKPULSE_ATIVO) {
            LinkPulseWidget()
        }
        
        HistoricoCard()
    }
}
```

#### Para Habilitar Feature no Release

1. Editar `app/build.gradle.kts`, bloco `release`, alterar a flag de `false` para `true`
2. Atualizar `versionCode` e `versionName` em `libs.versions.toml`
3. Gerar novo APK de release

**Exemplo:**
```gradle
// Antes
buildConfigField("Boolean", "FEATURE_LINKPULSE_ATIVO", "false")

// Depois
buildConfigField("Boolean", "FEATURE_LINKPULSE_ATIVO", "true")
```

---
## Features Implementadas

| Feature | Composable principal | Módulo de origem | Status |
|---|---|---|---|
| Dashboard / Home | `HomeScreen.kt` | `:app` (deps: `:featureHome`) | Implementado |
| Teste de velocidade (pré-execução) | `SpeedTestScreen.kt` | `:app` (deps: `:featureSpeedtest`) | Implementado |
| Execução do speedtest (ao vivo) | `VelocidadeScreen.kt` | `:app` | Implementado |
| Resultado do speedtest | `ResultadoVelocidadeScreen.kt` | `:app` | Implementado |
| Diagnóstico local por engines | `DiagnosticoScreen.kt` | `:app` (deps: `:featureDiagnostico`) | Implementado |
| Chat diagnóstico IA (SignallQ) | `SignallQScreen.kt` | `:app` | Implementado (ex-OrbitScreen) |
| Chat livre IA (LLM) | `LLMChatScreen.kt` | `:app` | Implementado (FEATURE_DIAGNOSTICO_CHAT) |
| Análise de redes Wi-Fi | `SinalScreen.kt` | `:app` (deps: `:featureWifi`) | Implementado |
| Scanner de dispositivos | `DispositivosScreen.kt` | `:app` (deps: `:featureDevices`) | Implementado |
| Fibra óptica GPON | `FibraModemScreen.kt` | `:app` (deps: `:featureFibra`) | Implementado |
| Histórico de medições | `HistoricoScreen.kt` | `:app` (deps: `:featureHistory`) | Implementado |
| Configurações | `AjustesScreen.kt` | `:app` (deps: `:featureSettings`) | Implementado |
| Laudo técnico | `LaudoScreen.kt` | `:app` | Implementado |
| SignallQ Pulse (monitoramento) | `SignallQPulseScreen.kt` | `:app` | Implementado |
| Onboarding (primeiro uso) | `OnboardingScreen.kt` | `:app` | Implementado |
| Permissões contextuais | `SinalScreen.kt` + sheets | `:app` | Implementado |
| Tratamento de offline | `HomeScreen`, `SpeedTestScreen`, `SinalScreen`, `DispositivosScreen` | `:app` | Implementado |

**Total:** 15 composables de tela identificados + 2 features transversais.

---

## Features por Subsistema

### Speedtest
- 3 modos: rápido, completo, triplo
- Fases medidas: LATÊNCIA (ping), DOWN (download), UP (upload)
- Métricas: download Mbps, upload Mbps, latência ms, jitter ms, perda de pacotes %, bufferbloat ms
- Comparação ANATEL RQUAL (40% mínimo / 80% normal em relação ao plano)
- Detecção de contaminação do teste
- Vereditos de uso: Streaming, Gaming, Vídeo Chamada (good / acceptable / poor)
- Persistência automática em Room (`MedicaoEntity`)

### Diagnóstico Local
- 8 engines: WifiSignal, Internet, WifiChannel, DNS, Historical, Fibra, Mobile, DecisionEngine
- 6+ regras de decisão consolidadas pelo `DiagnosticDecisionEngine`
- Resultado por engine: `DiagnosticStatus` (ok / info / attention / critical / inconclusive)
- Cards dinâmicos por resultado

### SignallQ IA (Chat)
- Speedtest silencioso integrado
- Envio ao Worker Cloudflare (Gemini 2.0 Flash primário, Qwen3 30B fallback cloud, fallback local)
- Perguntas contextuais dinâmicas (`DynamicQuestionEngine`)
- Chat em markdown com bolhas diferenciadas por tipo
- Fallback local se IA falhar (`AiFallbackFactory`)

### Wi-Fi
- Scan de redes vizinhas com SSID, BSSID, RSSI, canal, frequência, segurança, OUI
- Filtro por banda: Todas / 2.4GHz / 5GHz / 6GHz
- Análise de congestionamento de canais (`WifiChannelDiagnosticEngine`)
- Análise de topologia: ROTEADOR_MESH, NO_MESH, ROTEADOR, REPETIDOR (`TopologiaWifiEngine`)
- Guia visual de canais (`WifiChannelGuide`)

### Dispositivos
- Descoberta via ARP, mDNS e port scan
- Classificação por OUI e serviços mDNS
- Apelidos customizáveis por MAC (persistido em Room)
- Supressão de notificação de "novo dispositivo" para MACs registrados sem apelido

### Fibra GPON (Nokia)
- Leitura via HTTP ao modem local (NokiaModemClient)
- Status GPON: up/down, potência Rx/Tx (dBm), temperatura, corrente laser, voltagem, serial
- Status WAN e PPP
- Detecção automática de gateway IP
- Toggle "manter conectado"

### Monitoramento Passivo (Background)
- WorkManager (CoroutineWorker)
- Mede: latência HTTP, tempo DNS resolve, RSSI Wi-Fi
- Persiste em Room com `connectionType = "monitor"` (sem DL/UL)
- Histerese para evitar spam de notificações
- 4 tipos de alerta configuráveis individualmente: latência, DNS, RSSI, sem internet

### Histórico
- Gráfico de uptime (`UptimeGridChart`)
- Narrativa textual (`UptimeNarrativaEngine`)
- Exportação CSV e PDF

### DNS Benchmark
- Comparação via DoH (DNS over HTTPS)
- Grades: A (≤15ms), B (≤30ms), C (≤50ms), D (>50ms)
- Badge "atual" e "recomendado"
- Guia de configuração de DNS privado no Android e no roteador

### Configurações (DataStore)
- Tema: sistema / claro / escuro
- Perfil: nome e foto do usuário
- Dados do provedor: operadora, plano, região
- Monitoramento e alertas configuráveis
- Configuração de modem GPON
- Análise avançada

### Permissões Contextuais

**Localização (ACCESS_FINE_LOCATION):**
- Solicitada ao entrar em SinalScreen com Wi-Fi ativo
- Fluxo: `PermissaoLocalizacaoContextoSheet` → sheet bottom sheet explicativa → botões "Agora não" / "Entendi, conceder"
- Se dismissada: `LocPermissaoBanner` no topo da aba (clicável para reaabrir sheet)
- Se concedida: Wi-Fi scan continua, dados completos disponíveis
- Implementação: `LaunchedEffect` monitora estado + MainActivity verifica `shouldShowRequestPermissionRationale`

**Telefonia (READ_PHONE_STATE):**
- Solicitada ao entrar em SinalScreen com dados móveis ativos
- Fluxo: `PermissaoTelefoniaContextoSheet` → sheet bottom sheet → botões "Agora não" / "Entendi, conceder"
- Se dismissada: `MovelSemPermissaoBanner` substitui as métricas de sinal
- Se concedida: `MonitorTelephony` é iniciado → `movelSnapshot` preenchido → `MobileSignalCard` exibido
- Implementação: LaunchedEffect + MainActivity + callbacks

**Contextos:** Nunca bloqueia, nunca impede o scan/teste. Apenas oculta dados derivados da permissão.

### Tratamento de Offline

**HomeScreen:**
- `OfflineCard` como primeiro item do LazyColumn quando `!snapshotRede.conectado`
- Exibe: ícone WifiOff + texto "Sem conexão de internet" + botão "Testar assim que voltar"
- Comportamento: ao tocar o botão, registra `ConnectivityManager.NetworkCallback` via `DisposableEffect`
- Quando a conexão volta: auto-dispara `onNovoTeste()` (inicia speedtest)
- Cleanup automático quando composable sai de composição

**SpeedTestScreen:**
- Guard offline em `onIniciarTesteComAviso`: `if (!conectado) return`
- Indicador visual abaixo do `SpeedTestCircle`: "Sem conexão — teste indisponível"
- `ModeSelector` e botão de teste ficam inativos (disabled state)

**SinalScreen:**
- `OfflineBanner` no topo quando `!conectado`
- Aplicável a ambas as abas (Wi-Fi e Móvel)

**DispositivosScreen:**
- `OfflineBanner` no topo quando `!conectado`

**Implementação:** `conectado` derivado de `snapshotRede.conectado` passado via props. Sem dependency em ConnectivityManager direto na UI.

---

## Dados Persistidos

| Dados | Mecanismo | Entidade/Chave |
|---|---|---|
| Medições de velocidade e monitor | Room | `MedicaoEntity` (tabela `medicao`) |
| Apelidos de dispositivos | Room | `ApelidoDispositivoEntity` (tabela `apelido_dispositivo`) |
| Preferências do usuário | DataStore | `PreferenciasAppRepository` (12 chaves Boolean, 9 String, 1 Int, 1 Long) |
