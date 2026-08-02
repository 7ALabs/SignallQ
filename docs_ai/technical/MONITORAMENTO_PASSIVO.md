# Monitoramento Passivo — MonitoramentoWorker

**Status:** ativo
**Última validação:** 2026-07-23 (contra `android/app/src/main/kotlin/io/veloo/app/kotlin/monitoramento/`)
**Fonte de verdade:** código real — `MonitoramentoWorker.kt`, `MonitoramentoScheduler.kt`, `HisteresiHelper.kt`
**Escopo:** background monitoring de qualidade de rede (latência, DNS, Wi-Fi) e notificações de alerta
**Responsável:** Camilo (Backend Android)

> Este documento substitui uma versão anterior (v0.16.0) que descrevia um fluxo de 3 fases
> (Collecting/Thinking/Analyzing) com chamada a IA e uma tabela `AlerteLinkaPulse`. Nenhum dos
> dois existe no código atual — foram removidos/nunca migrados para o `MonitoramentoWorker`
> real. A descrição abaixo reflete o worker de fato implementado.

---

## 1. Objetivo técnico

Detectar degradação de conectividade em background (sem o usuário abrir o app) e notificar
apenas em transições de estado (ok → alerta), evitando spam de notificação.

## 2. Visão geral da solução

```
WorkManager (PeriodicWorkRequest, 30 min, policy KEEP)
  ↓
MonitoramentoWorker.doWork()
  ├─ medirLatenciaHttp()   — 3 amostras paralelas a speed.cloudflare.com, mediana
  ├─ medirDnsResolveTime() — InetAddress.getByName("cloudflare.com"), timeout 5s
  ├─ medirRssiWifi()       — RSSI do Wi-Fi conectado (ConnectivityManager/WifiManager)
  ↓
aplicarHisterese(latencia, dns, rssi) — HisteresiHelper (lógica pura)
  ↓
persistirMedicaoMonitor() — grava MedicaoEntity (fonte="monitor") no Room via MedicaoDao
```

Não há chamada a IA nem orquestração de fases neste worker — é uma medição direta + histerese +
persistência. O fluxo de IA (`SignallQOrchestrator`, worker `linka-ai-diagnosis-worker`) é
acionado pelo usuário na tela de diagnóstico, não pelo monitoramento passivo — ver
`docs_ai/technical/AI_FLOW.md`.

## 3. Scheduling

**Framework:** WorkManager (`MonitoramentoScheduler.kt`)

- **Período:** 30 minutos (`PeriodicWorkRequestBuilder<MonitoramentoWorker>(30, TimeUnit.MINUTES)`)
- **Tag:** `linka_monitoramento_passivo`
- **Policy:** `ExistingPeriodicWorkPolicy.KEEP` (evita duplicar o work já agendado)
- **Toggle do usuário:** `PreferenciasAppRepository.monitoramentoAtivoFlow`

## 4. Medições por execução

| Medição | Método | Timeout | Detalhe |
|---|---|---|---|
| Latência HTTP | `medirLatenciaHttp()` | 10s/amostra (connect 5s + read 5s) | 3 amostras paralelas (`async`/`awaitAll`) contra `https://speed.cloudflare.com/__down?bytes=0`, usa a mediana |
| DNS | `medirDnsResolveTime()` | 5s | `InetAddress.getByName("cloudflare.com")`, mede tempo de resolução |
| RSSI Wi-Fi | `medirRssiWifi()` | — | Via `ConnectivityManager`/`WifiInfo` (Android 12+) ou `WifiManager` (legado). Retorna motivo (`SemWifi`/`SemPermissao`/`Invalido`) quando não há valor |

## 5. Histerese e thresholds (`aplicarHisterese`)

Notifica **apenas na transição** ok→alerta (nunca repete enquanto o estado permanecer em
alerta). Estados anteriores/atuais persistidos via `PreferenciasAppRepository` (DataStore).

| Alerta | Entra em alerta | Sai do alerta | Notificação |
|---|---|---|---|
| Latência alta | > 400ms | < 300ms | `notificarLatenciaAlta` |
| DNS lento | > 2500ms | < 1800ms | `notificarDnsLento` |
| Wi-Fi fraco | RSSI < -75dBm | RSSI > -68dBm | `notificarWifiFraco` |
| Sem internet | sem latência **e** sem DNS | qualquer um voltando | `notificarSemInternet` (prioridade sobre os demais) |

Métrica `null` (ex.: Doze Mode interrompeu a medição) mantém o estado anterior — não força
transição. Cada tipo de notificação tem um controle granular próprio do usuário
(`notificacaoLatenciaAtivaFlow`, `notificacaoDnsAtivaFlow`, `notificacaoRssiAtivaFlow`,
`notificacaoSemInternetAtivaFlow`) — o usuário pode desligar um tipo sem desligar o
monitoramento inteiro.

**Não existe** cooldown temporal fixo nem teto de N alertas/dia — o único mecanismo de
contenção é a histerese por transição de estado.

## 6. Persistência

`persistirMedicaoMonitor()` grava uma `MedicaoEntity` no Room (`MedicaoDao`) com
`fonte = "monitor"` e `connectionType = "monitor"` — usada para compor o gráfico de
uptime/histórico junto com as medições de speedtest completo. `downloadMbps`/`uploadMbps`
ficam `null` (o monitor não mede throughput, só latência/DNS/RSSI).

Não existe tabela dedicada de alertas (`AlerteLinkaPulse`, citada em versão anterior deste
documento, não existe no schema atual do Room).

## 7. Permissões & Constraints

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### OEM Quirks

- **Samsung:** pode não rodar em Doze sem exceção de bateria configurada pelo usuário.
- **Xiaomi:** MIUI pode atrasar o disparo do work por horas — comportamento da plataforma, não
  do app.
- **Moto/AOSP:** respeitam as constraints padrão do WorkManager.

## 8. Configuração do usuário

Toggle de monitoramento e notificações vivem em Ajustes/Perfil (overlay `Overlay.Perfil`,
GH#936 — ver `docs_ai/technical/SCREEN_MAP.md`), não em uma tela dedicada `LinkaPulseScreen`
(citada em versão anterior deste documento — não existe mais como tela própria).

## 9. Testes

`android/app/src/test/kotlin/io/veloo/app/kotlin/monitoramento/`:
`MonitoramentoWorkerHistereseTest.kt` (transições de estado/thresholds) e
`MonitoramentoWorkerMedicaoTest.kt` (persistência da medição sintética). Não confirmado o
número exato de casos em cada um — `[a confirmar]` se precisar do total exato.

## 10. Riscos técnicos

- Sem foreground service: em dispositivos muito agressivos com Doze/battery-saver (alguns OEMs
  Android), o intervalo real pode variar bem além dos 30 minutos nominais — comportamento da
  plataforma, não um bug do worker.
- Medição de latência/DNS usa domínio fixo (`speed.cloudflare.com`, `cloudflare.com`) — uma
  degradação isolada da Cloudflare (rara, mas possível) apareceria como falso alerta de rede do
  usuário.
