# Monitoramento Passivo — LinkaPulse v0.6.3

**Escopo:** Background monitoring de qualidade internet, alertas inteligentes  
**Stack:** WorkManager 2+, Kotlin Coroutines, Room DAO

---

## Arquivos Principais

| Arquivo | Localização | Propósito |
| --- | --- | --- |
| `MonitoramentoScheduler.kt` | `io.linka.app.kotlin.monitoramento` | Agenda/cancela periodic work |
| `MonitoramentoWorker.kt` | `io.linka.app.kotlin.monitoramento` | Worker executa background speedtest |
| `LinkaPulseOrchestrator.kt` | `io.linka.app.kotlin.pulse` | Orquestra diagnóstico + IA |
| `LinkaPulseScreen.kt` | `ui.screen` | UI exibição resultados |
| `SnapshotLinkaPulse.kt` | `io.linka.app.kotlin.pulse` | Data class estado LinkaPulse |

---

## Comportamento

### Scheduling

**Framework:** WorkManager (Android Jetpack)

- **Período:** 30 minutos
- **Constraints:**
  - Rede conectada (`NetworkType.CONNECTED`)
  - Bateria não baixa (`requiresBatteryNotLow=true`)
- **Tipo:** `PeriodicWorkRequest` único (policy `KEEP` evita duplicatas)
- **Tag:** `linka_monitoramento_passivo`

### Execução — 3 Fases

#### Fase 1: Collecting
- Executa speedtest silencioso (sem UI)
- Coleta snapshot Wi-Fi (RSSI, freq, link speed)
- Tempo típico: 30–60 segundos

#### Fase 2: Thinking
- Executa diagnostic orchestrator (engines locais)
- Gera relatório diagnóstico
- Sem IA ainda — processamento local

#### Fase 3: Analyzing
- Chama IA via endpoint `/diagnosis`
- Gateway: `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev`
- Recebe análise estruturada

### Tipos de Alerta (4)

Gerados pelo `LinkaPulseOrchestrator`:

| Alerta | Condição | Severidade |
| --- | --- | --- |
| **Velocidade Baixa** | DL < 25 Mbps | Warn |
| **Latência Alta** | Latência > 80 ms | Warn |
| **Instabilidade** | Jitter > 50 ms OU Perda > 2% | Fail |
| **Wi-Fi Fraco** | RSSI < -70 dBm | Warn |

### Cooldown & Teto

- **Cooldown:** Não exibir alerta repetido por 2 horas
- **Teto:** Máximo 3 alertas por dia
- **Storage:** LocalDB (Room) `AlerteLinkaPulse` table

---

## Permissões & Constraints

### Permissões Obrigatórias

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### OEM Quirks

- **Samsung:** RequiresDeviceIdleExemption — pode não rodar em Doze sem configuração explícita
- **Xiaomi:** MIUI pode aguardar até 6 horas para iniciar work — documentar ao usuário
- **Moto:** Respeitam WorkManager constraints normalmente

---

## Testes (14 casos JUnit 4)

**Arquivo:** `src/test/kotlin/io/linka/app/kotlin/pulse/LinkaPulseOrchestratorTest.kt`

| # | Caso | Verifica |
| --- | --- | --- |
| 1 | Iniciar diagnóstico com sucesso | Transição Collecting → Thinking → Analyzing |
| 2 | Falha speedtest usa última medição | Se speedtest falhar, cai para BD |
| 3 | Speedtest silencioso não exibe UI | Sem callbacks de UI |
| 4 | Cooldown de 2h respeitado | Alerta repetido bloqueado |
| 5 | Teto de 3 alertas/dia | 4º alerta do dia rejeitado |
| 6 | Severidade "Velocidade Baixa" com DL < 25 | Correto |
| 7 | Severidade "Instabilidade" com Jitter > 50 | Correto |
| 8 | Wi-Fi Fraco com RSSI < -70 dBm | Correto |
| 9 | Latência Alta com latência > 80 ms | Correto |
| 10 | IA gateway endpoint chamado corretamente | URL + payload |
| 11 | Contexto acumulador build com todos os campos | Sem nulls |
| 12 | Rotating messages no estado Collecting | Interval 2.5s |
| 13 | Snapshot salvo em DB após análise | Record em AlerteLinkaPulse |
| 14 | Scheduler cancela work corretamente | Tag removido |

---

## Fluxo Resumido

```
WorkManager (30 min) 
  ↓
MonitoramentoWorker.doWork()
  ↓
LinkaPulseOrchestrator.iniciarDiagnostico()
  ├─ Fase 1: Collecting (speedtest silencioso)
  ├─ Fase 2: Thinking (diagnostico local)
  └─ Fase 3: Analyzing (IA gateway)
  ↓
Gerar Alerta? (cooldown + teto)
  ↓
Salvar em DB + Notificação
```

---

## Configuração Usuário

UI em `LinkaPulseScreen`:

- Habilitar/desabilitar monitoramento
- Intervalo (padrão 30 min, opções: 15, 30, 60 min)
- Notificações on/off
- Visualizar histórico alertas

---

## Notas

- **Background:** Não usa foreground service — apenas WorkManager periódico.
- **Battery:** Respeita constraints de bateria/rede; não desperdiça recursos.
- **Privacy:** Nenhum dado pessoal enviado — apenas métricas técnicas + config rede.
- **Play Store:** Conformidade WorkManager — sem problemas conhecidos.
