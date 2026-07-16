---
name: arquitetura-android
description: Regras de ouro da arquitetura modular do SignallQ Android. Consultar antes de criar módulos, repositórios, ViewModels, DI ou qualquer decisão de dependência entre camadas.
---

## Arquitetura SignallQ — Regras de Ouro

1. Features NAO dependem de outras features (apenas de core*)
2. AiDiagnosisRepository é @Singleton — nunca instanciar manualmente
3. OkHttpClient UPnP/scan é @Singleton via AppModule
4. SignallQOrchestrator vive em featureDiagnostico (não em :app)
5. URL do worker em BuildConfig, nunca hardcoded
6. Cada feature tem seu próprio ViewModel (sem god ViewModel)

Referência: docs/ARCHITECTURE_REVIEW.md

---

## Módulos Gradle (15 no total)

**:app** — shell, DI raiz (AppModule), AppShell.kt, navegação

**Core (5):**
- `:coreNetwork` — OkHttp, ConnectivityManager, NetworkCallback
- `:coreDatabase` — Room, SignallQDatabase (v10), DAOs, migrações
- `:coreDatastore` — DataStore `linkaPreferencias`
- `:coreTelephony` — TelephonyManager, leituras de sinal celular
- `:corePermissions` — abstrações de permissão, state holders

**Feature (9):**
- `:featureHome`, `:featureSpeedtest`, `:featureWifi`, `:featureDevices`
- `:featureDns`, `:featureFibra`, `:featureDiagnostico`, `:featureHistory`, `:featureSettings`

---

## Dependências permitidas

```
:featureX → :coreY   (permitido)
:featureX → :featureY (PROIBIDO)
:app → qualquer módulo (permitido — ponto de montagem)
:coreX → :coreY       (permitido com cuidado — evitar ciclos)
```

---

## Navegação

- Bottom bar: 5 abas (índice 0-4): Início, Velocidade, Sinal, Histórico, Ajustes.
- Diagnóstico/IA, Dispositivos, Fibra, Laudo: overlays via `overlayStack` em AppShell.kt.
- `navigation/AppNavGraph.kt` tem constantes legadas — NAO reflete a nav atual.

---

## DI e instanciação

- Hilt em toda a cadeia. `@Singleton` para repositórios e clientes HTTP.
- ViewModel por feature via `@HiltViewModel`.
- `MainViewModel` e `ChatDiagnosticoIaViewModel` são os dois ViewModels transversais — justificativa: estado de nav e chat de IA respectivamente. Nenhum outro god ViewModel.

---

## IA e Worker Cloudflare

- Fluxo: App → worker Cloudflare (`linka-ai-diagnosis-worker...workers.dev`)
- Modelo padrão: `@cf/qwen/qwen3-30b-a3b-fp8`
- Fallback local: `AiFallbackFactory` — sem IA, sem crash
- Persona: "SignallQ"
- `SignallQOrchestrator` vive em `:featureDiagnostico` — nunca mover para `:app`

---

## Persistência

- Room `SignallQDatabase` versão 10 — entidades: Medicao, ApelidoDispositivo, ChatSession, ChatMessage
- DataStore: `linkaPreferencias` (nome técnico — não renomear)
- Background: WorkManager `MonitoramentoWorker` (30 min, histerese)

---

## Identificadores técnicos — NAO renomear

| Identificador | Motivo |
|---|---|
| `io.veloo.app` | package/applicationId — quebra Firebase/assinatura |
| `linkaKotlin.db` | nome do banco Room |
| `linkaPreferencias` | DataStore |
| `linka-ai-diagnosis-worker` | nome do worker Cloudflare — infra |
| `linka_*` | canais de notificação |

> Criado em 2026-06-21. Referência: docs/ARCHITECTURE_REVIEW.md
