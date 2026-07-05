# Storage — Android SignallQ

**Última atualização:** 2026-06-21 (v0.16.0)
**Fonte:** código real

O app usa dois mecanismos de persistência: **Room** (banco SQLite) e **DataStore** (preferências chave-valor).

---

## 1. Room — Banco SQLite

**Módulo:** `:coreDatabase`
**Banco:** `SignallQDatabase` — versão 10
**Fábrica:** `CoreDatabaseModulo.criarBanco(context)`
**Migrações:** v1 → v10. A v10 adicionou as tabelas `chat_sessions` e `chat_messages`.

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
| `fonte` | String? | Sim | `"web"`, `"android"` ou `"pwa"` (legado — PWA descontinuado, valor pode existir apenas em registros históricos) |

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

### 1.3 Tabela: `chat_sessions` (ChatSessionEntity)

Armazena sessões do Chat IA (Diagnóstico IA com LLM). Adicionada na migration v10.

| Campo | Tipo Kotlin | Nullable | Descrição |
|---|---|---|---|
| `id` | String | Não (PK) | UUID único da sessão |
| `timestampCriacaoMs` | Long | Não | Timestamp de criação em epoch ms |
| `contextoResumido` | String? | Sim | Contexto serializado do diagnóstico que originou a sessão |

### 1.4 Tabela: `chat_messages` (ChatMessageEntity)

Armazena mensagens individuais de cada sessão de chat. Adicionada na migration v10.

| Campo | Tipo Kotlin | Nullable | Descrição |
|---|---|---|---|
| `id` | String | Não (PK) | UUID único da mensagem |
| `sessaoId` | String | Não (FK → `chat_sessions.id`) | Sessão à qual a mensagem pertence |
| `role` | String | Não | `"user"` ou `"assistant"` |
| `conteudo` | String | Não | Texto da mensagem |
| `timestampMs` | Long | Não | Timestamp da mensagem em epoch ms |

**DAO:** `ChatSessionDao` — exposto por `SignallQDatabase.chatSessionDao()`.

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

### 2.1.1 Chaves Boolean adicionadas em AJ-B

| Chave | Padrão | Descrição |
|---|---|---|
| `ispConfirmado` | false | Se o usuário já confirmou ou dispensou o banner de ISP detectado |

### 2.2 Chaves String

| Chave | Padrão | Descrição |
|---|---|---|
| `modemHost` | — | IP ou hostname do modem GPON Nokia |
| `modemUsername` | `"userAdmin"` | Usuário para login no modem |
| `modemPassword` | `""` | Senha do modem |
| `temaSelecionado` | `"sistema"` | Tema: `"sistema"`, `"claro"` ou `"escuro"` |
| `nomeUsuario` | — | Nome do perfil do usuário |
| `fotoUriUsuario` | — | URI da foto de perfil (content URI do storage Android) |
| `operadora` | — | Nome da operadora do usuário (ISP/provedor fixo) |
| `operadoraMovel` | — | Operadora de dados móveis (adicionada em AJ-B) |
| `planoInternet` | — | Plano contratado — apenas dígitos, máx 4 chars, ex.: `"300"`, `"1000"` (reformatado em AJ-D) |
| `estadoUf` | — | UF do estado — sigla de 2 letras, ex.: `"SP"` (substituiu `regiao` em AJ-C) |
| `cidadeNome` | — | Nome do município — fonte IBGE API (adicionada em AJ-C) |
| `ultimaVersaoVista` | — | Versão do changelog mais recente vista pelo usuário, ex.: `"0.7.0"` (adicionada em AJ-F) |

> `regiao` (String livre) foi substituída pelas chaves estruturadas `estadoUf` + `cidadeNome` no bloco AJ-C. Valores legados de `regiao` não são migrados automaticamente.

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
