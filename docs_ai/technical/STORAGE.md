# Storage — Android Linka

**Última atualização:** 2026-05-17
**Fonte:** código real (Marcelo, 2026-05-17)

O app usa dois mecanismos de persistência: **Room** (banco SQLite) e **DataStore** (preferências chave-valor).

---

## 1. Room — Banco SQLite

**Módulo:** `:coreDatabase`
**Banco:** `LinkaDatabase`
**Fábrica:** `CoreDatabaseModulo.criarBanco(context)`

### 1.1 Tabela: `medicao` (MedicaoEntity)

Armazena todas as medições — tanto speedtests completos quanto medições do monitoramento passivo.

| Campo | Tipo Kotlin | Nullable | Descrição |
|---|---|---|---|
| `id` | String | Não (PK) | UUID único da medição |
| `timestampEpochMs` | Long | Não | Timestamp em milissegundos (epoch) |
| `connectionType` | String | Não | `"wifi"`, `"movel"`, `"ethernet"`, `"monitor"` |
| `contaminado` | Boolean | Não | Teste contaminado / descartado da análise |
| `speedtestMode` | String? | Sim | Modo: `"complete"`, `"ping_only"` |
| `specVersion` | String? | Sim | Versão do spec do speedtest |
| `downloadMbps` | Double? | Sim | Download em Mbps. `null` em medições de monitor |
| `uploadMbps` | Double? | Sim | Upload em Mbps. `null` em medições de monitor |
| `latencyMs` | Double? | Sim | Latência em ms |
| `jitterMs` | Double? | Sim | Jitter em ms |
| `perdaPercentual` | Double? | Sim | Perda de pacotes em % |
| `bufferbloatMs` | Double? | Sim | Bufferbloat em ms |
| `packetLossSource` | String? | Sim | Fase onde ocorreu a perda: `"download"` ou `"upload"` |
| `vereditoStreaming` | String? | Sim | `"good"`, `"acceptable"` ou `"poor"` |
| `vereditoGamer` | String? | Sim | `"good"`, `"acceptable"` ou `"poor"` |
| `vereditoVideoChamada` | String? | Sim | `"good"`, `"acceptable"` ou `"poor"` |
| `gargaloPrimario` | String? | Sim | Gargalo principal identificado |
| `fonte` | String? | Sim | `"web"`, `"android"` ou `"pwa"` |

**Distinção de tipos de medição:**

- **Speedtest completo:** `connectionType` = tipo real de rede, `downloadMbps` e `uploadMbps` preenchidos, `fonte = "android"`.
- **Monitoramento passivo:** `connectionType = "monitor"`, `downloadMbps = null`, `uploadMbps = null` — mede apenas latência e RSSI.

### 1.2 Tabela: `apelido_dispositivo` (ApelidoDispositivoEntity)

Armazena apelidos definidos pelo usuário para dispositivos da rede.

| Campo | Tipo Kotlin | Nullable | Descrição |
|---|---|---|---|
| `mac` | String | Não (PK) | Endereço MAC do dispositivo |
| `apelido` | String? | Sim | Nome definido pelo usuário |

**Comportamento de `apelido = null`:** MAC registrado sem apelido. Suprime a notificação de "novo dispositivo" para esse MAC — o usuário já o conhece e optou por não dar nome.

---

## 2. DataStore — Preferências

**Módulo:** `:coreDatastore`
**Arquivo:** `PreferenciasAppRepository.kt`
**Tecnologia:** `DataStore<Preferences>` (Jetpack DataStore Preferences)

### 2.1 Chaves Boolean

| Chave | Padrão | Descrição |
|---|---|---|
| `monitoramentoAtivo` | false | Monitoramento passivo em background ligado/desligado |
| `modemPermanecerConectado` | false | Manter sessão ativa no modem GPON |
| `analiseAvancada` | false | Modo de análise técnica avançada |
| `onboarding_concluido` | false | Se o onboarding foi completado. Quando `true`, nunca exibe OnboardingScreen novamente |
| `alerta_latencia_ativo` | false | Estado de histerese — latência alta. Controlado pelo worker, não pelo usuário diretamente |
| `alerta_dns_ativo` | false | Estado de histerese — DNS lento |
| `alerta_rssi_ativo` | false | Estado de histerese — RSSI fraco |
| `alerta_sem_internet_ativo` | false | Estado de histerese — sem internet |
| `notificacao_latencia_ativa` | — | Notificações de latência habilitadas pelo usuário |
| `notificacao_dns_ativa` | — | Notificações de DNS lento habilitadas pelo usuário |
| `notificacao_rssi_ativa` | — | Notificações de RSSI fraco habilitadas pelo usuário |
| `notificacao_sem_internet_ativa` | — | Notificações de sem internet habilitadas pelo usuário |

> As chaves `alerta_*` são usadas pela lógica de histerese do `MonitoramentoWorker`. As chaves `notificacao_*` são configuradas pelo usuário em Ajustes.

### 2.2 Chaves String

| Chave | Padrão | Descrição |
|---|---|---|
| `modemHost` | — | IP ou hostname do modem GPON Nokia |
| `modemUsername` | `"userAdmin"` | Usuário para login no modem |
| `modemPassword` | `""` | Senha do modem |
| `temaSelecionado` | `"sistema"` | Tema: `"sistema"`, `"claro"` ou `"escuro"` |
| `nomeUsuario` | — | Nome do perfil do usuário |
| `fotoUriUsuario` | — | URI da foto de perfil (content URI do storage Android) |
| `operadora` | — | Nome da operadora do usuário |
| `planoInternet` | — | Plano contratado (ex.: "100 Mbps") — usado para cálculo ANATEL |
| `regiao` | — | Região do usuário |

### 2.3 Chaves Int

| Chave | Padrão | Descrição |
|---|---|---|
| `limiteAlertaMbps` | 0 | Limite de velocidade para alerta. `0` = desabilitado |

### 2.4 Chaves Long

| Chave | Descrição |
|---|---|
| `ultimaVerificacaoMonitoramento` | Timestamp epoch ms da última execução do `MonitoramentoWorker` |

---

## 3. Uso Conjunto

| Dado | Onde fica | Quem escreve | Quem lê |
|---|---|---|---|
| Resultado de speedtest | Room / `medicao` | `ExecutorSpeedtest` via ViewModel | `HistoricoScreen`, `ResultadoVelocidadeScreen`, engines históricos |
| Medição passiva | Room / `medicao` | `MonitoramentoWorker` | `HistoricoScreen` (gráfico de uptime) |
| Apelidos de dispositivos | Room / `apelido_dispositivo` | `DispositivosScreen` via ViewModel | `DispositivosScreen`, `scannerDispositivos` |
| Preferências do usuário | DataStore | `AjustesScreen` via ViewModel | Múltiplas telas + worker |
| Estado do monitoramento | DataStore (`alerta_*`) | `MonitoramentoWorker` | Mesmo worker (histerese) |
| Onboarding | DataStore | `OnboardingScreen` via ViewModel | `AppShell` (decide exibir ou não) |
