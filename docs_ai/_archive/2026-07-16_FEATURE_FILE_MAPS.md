> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: docs_ai/ARQUITETURA/MODULOS/*.md. Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# Feature File Maps — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0, versionCode 56)
**Fonte:** código real

Mapa de arquivos-chave por módulo. Todos os paths são relativos à raiz do módulo (`module/src/main/kotlin/io/veloo/app/kotlin/`).

---

## Módulo :app

**Base path:** `app/src/main/kotlin/io/veloo/app/kotlin/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SignallQApplication.kt` | Application | Inicialização, Hilt, canais de notificação |
| `MainActivity.kt` | Activity | Entry point único — setContent com SignallQTheme |
| `MainViewModel.kt` | @HiltViewModel | Orquestra todos os serviços e expõe StateFlows |
| `FeatureFlags.kt` | Object singleton | Controle de features por build type |
| `di/AppModule.kt` | Hilt @Module | Provê todas as dependências injetadas |
| `ui/SignallQTheme.kt` | Composable | Tema MD3 com ColorScheme e tokens |
| `ui/AppShell.kt` | Composable | Shell — NavigationBar 5 abas + fluxos sobrepostos |
| `ui/AppNavGraph.kt` | NavHost | Rotas das 5 abas principais |
| `ui/screen/HomeScreen.kt` | Composable screen | Aba 0 — Dashboard principal |
| `ui/screen/SpeedTestScreen.kt` | Composable screen | Aba 1 — Teste de velocidade |
| `ui/screen/SinalScreen.kt` | Composable screen | Aba 2 — Análise de sinal Wi-Fi e móvel |
| `ui/screen/HistoricoScreen.kt` | Composable screen | Aba 3 — Histórico de medições |
| `ui/screen/DispositivosScreen.kt` | Composable screen | Via aba "Mais" — Dispositivos na rede |
| `ui/screen/AjustesScreen.kt` | Composable screen | Via aba "Mais" — Configurações |
| `ui/screen/VelocidadeScreen.kt` | Composable screen | Overlay — Speedtest em execução |
| `ui/screen/ResultadoVelocidadeScreen.kt` | Composable screen | Overlay — Resultado do speedtest |
| `ui/screen/DiagnosticoScreen.kt` | Composable screen | Overlay — Diagnóstico local + laudo IA |
| `ui/screen/LLMChatScreen.kt` | Composable screen | Overlay — Chat com LLM SignallQ |
| `ui/screen/FibraScreen.kt` | Composable screen | Overlay — Leitura modem GPON |
| `ui/screen/LaudoScreen.kt` | Composable screen | Overlay — Geração de laudo PDF |
| `ui/screen/PrivacidadeScreen.kt` | Composable screen | Overlay — Política de privacidade |
| `ui/screen/NovidadesScreen.kt` | Composable screen | Overlay — Changelog do app |
| `ui/screen/OnboardingScreen.kt` | Composable screen | Primeira execução |
| `ui/viewmodel/ChatDiagnosticoIaViewModel.kt` | ViewModel | Estado do chat IA |
| `pulse/SignallQOrchestrator.kt` | Orchestrator | Fluxo SignallQ/Chat: speedtest silencioso + diagnóstico + LLM |
| `monitoramento/MonitoramentoWorker.kt` | CoroutineWorker | Background monitoring passivo (WorkManager) |
| `monitoramento/MonitoramentoScheduler.kt` | Singleton | Agenda/cancela o worker |
| `notificacao/SignallQNotificationHelper.kt` | Object singleton | Cria canais e exibe notificações |
| `speedtest/SpeedtestPersistenceCoordinator.kt` | Coordinator | Persiste resultado do speedtest em Room |

---

## Módulo :coreNetwork

**Base path:** `coreNetwork/src/main/kotlin/io/veloo/app/kotlin/core/network/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `MonitorRede.kt` | Interface | Contrato de monitoramento de conectividade |
| `MonitorRedeAndroid.kt` | Implementação | ConnectivityManager.NetworkCallback |
| `SnapshotRede.kt` | Data class | Estado atual da rede |
| `WifiLinkSnapshot.kt` | Data class | Snapshot do link Wi-Fi (RSSI, canal, freq, link speed) |
| `EstadoConexao.kt` | Enum | WIFI, MOVEL, CABO, DESCONHECIDO |
| `GatewayLatencyMeasurer.kt` | Utilitário | RTT TCP do gateway local |
| `NetworkCapabilitiesProvider.kt` | Interface | Acesso a NetworkCapabilities |
| `DispatcherProvider.kt` | Interface | Abstração de Dispatchers para testabilidade |
| `DefaultDispatcherProvider.kt` | Implementação | Dispatchers reais (IO, Main, Default) |

---

## Módulo :coreDatabase

**Base path:** `coreDatabase/src/main/kotlin/io/veloo/app/kotlin/core/database/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SignallQDatabase.kt` | Room Database | DB principal — versão 10, 4 entidades, 3 DAOs |
| `MedicaoEntity.kt` | Entity | Tabela `medicao` |
| `MedicaoDao.kt` | DAO | Queries de medições |
| `ApelidoDispositivoEntity.kt` | Entity | Tabela `apelido_dispositivo` |
| `ApelidoDispositivoDao.kt` | DAO | Queries de apelidos |
| `CoreDatabaseModulo.kt` | Object | Fábrica `criarBanco(context)` + migrações v1→v10 |
| `chat/ChatSessionEntity.kt` | Entity | Tabela `chat_sessions` |
| `chat/ChatMessageEntity.kt` | Entity | Tabela `chat_messages` |
| `chat/ChatSessionDao.kt` | DAO | Queries de sessões e mensagens |

---

## Módulo :coreDatastore

**Base path:** `coreDatastore/src/main/kotlin/io/veloo/app/kotlin/core/datastore/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `PreferenciasAppRepository.kt` | Repository | DataStore `linkaPreferencias` — todos os flows de preferências |

---

## Módulo :corePermissions

**Base path:** `corePermissions/src/main/kotlin/io/veloo/app/kotlin/core/permissions/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `GerenciadorPermissoesRede.kt` | Interface | Contrato de permissões |
| `SnapshotPermissoesRede.kt` | Data class | Estado das permissões |

---

## Módulo :coreTelephony

**Base path:** `coreTelephony/src/main/kotlin/io/veloo/app/kotlin/core/telephony/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `MonitorTelephony.kt` | Serviço | TelephonyManager — produz `MovelSnapshot` |
| `MovelSnapshot.kt` | Data class | RSRP, RSRQ, SINR, tecnologia, banda, operadora |

---

## Módulo :featureDiagnostico

**Base path:** `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/`

| Arquivo/Pacote | Tipo | Responsabilidade |
|---|---|---|
| `DiagnosticOrchestrator.kt` | Orchestrator | Sequencia engines e retorna relatório |
| `DiagnosticRunner.kt` | Object stateless | Executa todos os engines |
| `DiagnosticResult.kt` | Data class | Saída de todos os engines |
| `DiagnosticStatus.kt` | Enum | ok, info, attention, critical, inconclusive |
| `engines/WifiSignalQualityEngine.kt` | Engine | Qualidade do sinal Wi-Fi por banda |
| `engines/InternetDiagnosticEngine.kt` | Engine | Velocidade, latência, jitter, perda |
| `engines/WifiChannelDiagnosticEngine.kt` | Engine | Congestionamento de canal |
| `engines/DnsDiagnosticEngine.kt` | Engine | Qualidade do DNS em uso |
| `engines/HistoricalDegradationEngine.kt` | Engine | Degradação histórica 7d/30d |
| `engines/FibraSignalQualityEngine.kt` | Engine | Potência óptica GPON |
| `engines/MobileSignalDiagnosticEngine.kt` | Engine | Sinal 4G/5G (RSRP, RSRQ, SINR) |
| `engines/DiagnosticDecisionEngine.kt` | Engine | Decisão final consolidada |
| `ai/AiDiagnosisRepository.kt` | Repository | POST ao Cloudflare Worker |
| `ai/DiagnosisAiContextFactory.kt` | Factory | Monta payload schema v3 |
| `ai/AiFallbackFactory.kt` | Factory | Resultado local se IA falhar |
| `ai/AiModels.kt` | Data classes | Modelos de request/response da IA |
| `pulse/SignallQOrchestrator.kt` | Orchestrator | Fluxo conversacional SignallQ |
| `pulse/SignallQState.kt` | Enum | Idle, Collecting, Thinking, Analyzing, Done, Error |
| `pulse/SignallQSnapshot.kt` | Data class | Estado atual do fluxo SignallQ |
| `pulse/DynamicQuestionEngine.kt` | Engine | Perguntas contextuais para o chat |
| `chat/ChatDiagnosticoIaRepository.kt` | Repository | Persistência de sessões de chat em Room |
| `chat/CotaIaRepository.kt` | Repository | Cota diária rolling 24h |

---

## Módulo :featureWifi

**Base path:** `featureWifi/src/main/kotlin/io/veloo/app/kotlin/feature/wifi/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ScannerRedesWifi.kt` | Interface | Contrato de scan Wi-Fi |
| `SnapshotScanWifi.kt` | Data class | Estado do scan |
| `RedeVizinha.kt` | Data class | Dados de uma rede vizinha |
| `GrupoRedeWifi.kt` | Data class | Grupo de BSSIDs do mesmo SSID |
| `TopologiaWifiEngine.kt` | Engine | Classifica topologia (mesh, repetidor, roteador) |
| `MontarResumoWifiUseCase.kt` | Use case | Síntese da análise Wi-Fi |
| `MeshOuiDatabase.kt` | Object | Base de OUIs de fabricantes mesh |

---

## Módulo :featureSpeedtest

**Base path:** `featureSpeedtest/src/main/kotlin/io/veloo/app/kotlin/feature/speedtest/`

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ExecutorSpeedtest.kt` | Interface | Contrato do speedtest |
| `ExecutorSpeedtestCloudflare.kt` | Implementação | Speedtest via Cloudflare Speed CDN |
| `SnapshotExecucaoSpeedtest.kt` | Data class | Estado durante execução |
| `ResultadoSpeedtest.kt` | Data class | Resultado final |
| `EstadoExecucaoSpeedtest.kt` | Enum | idle, executando, concluido, erro, cancelado |
| `ModoSpeedtest.kt` | Enum | complete, ping_only |
| `PingExecutor.kt` | Class | 20 amostras ICMP-over-HTTP/2 |
| `PingResultado.kt` | Data class | latenciaMs, jitterMs, perdaPercentual, amostras |

---

## Módulos Mínimos

| Módulo | Arquivo principal | Observação |
|---|---|---|
| `:featureHome` | `FeatureHomeModulo.kt` | HomeScreen reside em `:app` |
| `:featureSettings` | `FeatureSettingsModulo.kt` | AjustesScreen reside em `:app` |
