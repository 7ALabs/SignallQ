# Contrato de Eventos — Firebase Analytics

**Status:** ativo (parcialmente implementado — ver "Estado atual" abaixo)
**Última validação:** 2026-07-05 (v0.23.0, versionCode 56)
**Fonte de verdade:** código real (`AnalyticsHelper`/`FirebaseAnalyticsHelper`, `DiagnosticOrchestrator`, `SignallQOrchestrator`) — este arquivo também define o contrato-alvo para eventos ainda não instrumentados
**Escopo:** funil principal SIG-155 (7 eventos implementados) + contrato mais amplo proposto (eventos por feature ainda não instrumentados)
**Responsável:** Camilo (Backend Android)
**Property ID:** 543555227 (Firebase Analytics — Android)
**Status de implementação:** funil principal (7 eventos, ver seção "Funil
principal") instrumentado via `AnalyticsHelper` (SIG-155). Eventos do schema
SIG-134 (`feature_used`, `screen_view`, `app_session_start`, `feature_crash`,
`battery_snapshot`) instrumentados à parte via `AnalyticsTracker` — ver
`docs_ai/technical/analytics-events-schema.md`. Os demais eventos deste
contrato (`onboarding_concluido`, `speedtest_erro`, `diag_erro`,
`ia_laudo_erro`, `ia_chat_mensagem_enviada`, `wifi_*`, `historico_*`, `dns_*`,
`fibra_*`, `dispositivos_*`, `ajustes_*`) **ainda não instrumentados**.

---

## Estado atual

- **Android — funil principal (SIG-155):** instrumentado via `AnalyticsHelper`
  — interface em `core/network` (`AnalyticsHelper.kt`), implementação
  `FirebaseAnalyticsHelper` em `:app`, injetada via Hilt (`AppModule`). Distinto
  do `AnalyticsTracker` (SIG-134/`feature_used`) — ambos coexistem e
  compartilham a mesma instância de `FirebaseAnalytics`, mas com APIs públicas
  separadas. Ver seção "Funil principal" para os pontos exatos de disparo.
- **Android — demais eventos deste contrato:** ainda não instrumentados.

O contrato abaixo define os eventos que **devem ser implementados**, derivados
do modelo de domínio atual (v0.23.0). Qualquer evento novo ou alterado exige
atualização deste arquivo no mesmo PR.

---

## Convenções

### Nomenclatura

- Formato: `snake_case`, prefixo da feature + verbo no passado.
- Exemplos: `speedtest_iniciado`, `diagnostico_concluido`, `ia_laudo_gerado`.
- Sem acento, sem espaço, sem hífen.
- Prefixos reservados por feature:

| Prefixo | Feature |
|---|---|
| `speedtest_` | Teste de velocidade |
| `diag_` | Diagnóstico de rede |
| `ia_` | IA / laudo |
| `wifi_` | Tela Sinal / Wi-Fi |
| `historico_` | Histórico |
| `dns_` | DNS benchmark |
| `fibra_` | Modem fibra |
| `dispositivos_` | Scan de dispositivos |
| `ajustes_` | Configurações |
| `app_` | Ciclo de vida do app |

### Parâmetros

- Tipos permitidos: `String`, `Long`, `Double`, `Boolean`.
- Nomes em `snake_case`. Sem PII (sem SSID completo, sem IP público, sem BSSID).
- Enums enviados como String lowercase: `"wifi"`, `"mobile"`, `"ok"`, `"critical"`.
- Valores monetários ou de tamanho em unidade explícita no nome: `_ms`, `_mbps`, `_pct`.
- Parâmetros de versão sempre como String (`"0.21.0"`), não como número.

### Limites Firebase

- Máximo 25 parâmetros por evento.
- Nome do evento: até 40 caracteres.
- Valor de parâmetro String: até 100 caracteres.

---

## Eventos — Ciclo de vida do app

### `app_aberto` — implementado (SIG-155)

Disparado na primeira abertura de sessão (complementa o automático `app_open`
do Firebase, mas com contexto de versão).

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `versao_app` | String | Sim | Ex.: `"0.21.0"` |
| `version_code` | Long | Sim | Ex.: `52` |
| `tipo_conexao` | String | Sim | `"wifi"` \| `"mobile"` \| `"ethernet"` \| `"desconectado"` \| `"desconhecido"` |
| `primeira_abertura` | Boolean | Não | `true` se for a primeira vez (sem histórico local) |

**Tela:** qualquer (disparado no `MainActivity.onCreate`)
**Plataforma:** Android

**Nota de implementação:** `tipo_conexao` é lido de `MonitorRede.snapshotFlow`
no instante do `onCreate` — antes de `iniciarMonitorRede()` (chamado só em
`onStart`), então pode vir com o valor default do monitor (`desconhecido`) em
vez do estado real de conexão em alguns lançamentos. `primeira_abertura` **não
está implementado** neste PR — exigiria decidir a fonte de verdade (o app já
usa `onboardingConcluidoFlow` para um propósito diferente); ficou de fora para
não inventar heurística sem revisão. Rastrear como follow-up se for relevante
para a análise de funil.

---

### `onboarding_concluido`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `versao_app` | String | Sim | |
| `permissoes_concedidas` | String | Sim | Lista CSV das permissões aceitas: `"localizacao,telefonia"` |

**Tela:** `OnboardingScreen`
**Plataforma:** Android

---

## Eventos — Speedtest

### `speedtest_iniciado` — implementado (SIG-155)

Disparado quando o usuário toca "Iniciar teste" ou o teste silencioso começa.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `modo` | String | Sim | `"fast"` \| `"complete"` \| `"triplo"` — de `ModoSpeedtest.name` |
| `tipo_conexao` | String | Sim | `"wifi"` \| `"mobile"` \| `"ethernet"` \| `"desconectado"` \| `"desconhecido"` |
| `versao_app` | String | Sim | |

**Tela:** `SpeedTestScreen` (via `SpeedtestViewModel.reiniciarSuite` /
`confirmarSpeedtestEmMovel`)
**Plataforma:** Android

**Nota de implementação:** o valor de `modo` foi corrigido em relação à versão
anterior deste contrato — `ModoSpeedtest` no código é `fast`/`complete`/`triplo`,
não `"completo"`/`"silencioso"`. O teste silencioso disparado pelo
`SignallQOrchestrator` (fluxo guiado de IA) **não passa por este ponto de
instrumentação** — só o speedtest explícito iniciado pelo usuário via
`SpeedtestViewModel` é contado no funil, para manter o par
`speedtest_iniciado`/`speedtest_concluido` sempre correlacionado por sessão de
UI (evita eventos `concluido` órfãos de testes automáticos em background).

---

### `speedtest_concluido` — implementado (SIG-155)

Disparado quando o `ResultadoSpeedtest` da execução atual (via
`SpeedtestViewModel`) fica disponível em `ExecutorSpeedtest.snapshotFlow`.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `modo` | String | Sim | `"fast"` \| `"complete"` \| `"triplo"` |
| `tipo_conexao_inicio` | String | Sim | Tipo de conexão no início do teste |
| `tipo_conexao_fim` | String | Não | Tipo de conexão ao final (pode ter mudado) |
| `download_mbps` | Double | Sim | Velocidade de download |
| `upload_mbps` | Double | Sim | Velocidade de upload |
| `latencia_ms` | Double | Sim | Latência (ping) |
| `jitter_ms` | Double | Sim | Jitter |
| `perda_pct` | Double | Sim | Perda de pacotes em % |
| `bufferbloat_ms` | Double | Sim | |
| `severidade_bufferbloat` | String | Sim | `"nenhum"` \| `"leve"` \| `"moderado"` \| `"severo"` |
| `stability_score` | Double | Sim | 0–100 |
| `contaminado` | Boolean | Sim | `true` se o teste foi comprometido |
| `duracao_ms` | Long | Não | Duração total do teste em ms |
| `versao_app` | String | Sim | |

**Tela:** `SpeedTestScreen` (via `SpeedtestViewModel`, não mais
`ResultadoVelocidadeScreen`/`SpeedtestPersistenceCoordinator`)
**Plataforma:** Android

**Nota de implementação:** disparado no `SpeedtestViewModel` (mesmo ViewModel
de `speedtest_iniciado`), imediatamente após `ExecutorSpeedtest.executar()`
retornar — e não em `SpeedtestPersistenceCoordinator` (que persiste no Room de
forma global, inclusive testes silenciosos do fluxo de IA). Isso mantém o
funil correlacionado por sessão de UI: só speedtests explicitamente iniciados
pelo usuário entram no funil `speedtest_iniciado → speedtest_concluido`.

---

### `speedtest_erro`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `modo` | String | Sim | |
| `fase` | String | Sim | Fase em que falhou: `"ping"` \| `"download"` \| `"upload"` |
| `motivo` | String | Não | Mensagem de erro resumida (sem stack trace) |
| `versao_app` | String | Sim | |

**Tela:** `SpeedTestScreen`
**Plataforma:** Android

---

## Eventos — Diagnóstico de rede

### `diag_iniciado` — implementado (SIG-155)

Disparado no início de `DiagnosticOrchestrator.executar()`.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `tipo_conexao` | String | Sim | `"wifi"` \| `"mobile"` \| `"ethernet"` \| `"desconectado"` \| `"desconhecido"` |
| `areas_habilitadas` | String | Não | CSV das áreas ativas (`DiagnosticArea.name.lowercase()`): ex. `"velocidade,wifi_sinal,dns"` |
| `tem_speedtest` | Boolean | Sim | `true` se o diagnóstico recebeu `InternetDiagnosticInput` |
| `versao_app` | String | Sim | |

**Tela:** `DiagnosticoScreen`
**Plataforma:** Android

**Nota de implementação:** instrumentado dentro de `DiagnosticOrchestrator`
(não em cada ViewModel chamador) — é o único ponto de entrada compartilhado por
todos os fluxos de diagnóstico (`MainViewModel.iniciarDiagnostico()` e
`SignallQOrchestrator`), evitando duplicar a chamada em múltiplos call sites.
Os valores reais de `areas_habilitadas` vêm do enum `DiagnosticArea`
(`VELOCIDADE`, `WIFI_SINAL`, `LATENCIA`, `FIBRA`, `DNS`), diferente do exemplo
genérico da versão anterior deste contrato.

---

### `diag_concluido` — implementado (SIG-155)

Disparado quando `DiagnosticOrchestrator.executar()` conclui com sucesso
(equivalente a `EstadoDiagnostico.concluido` sendo emitido).

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `tipo_conexao` | String | Sim | |
| `status_geral` | String | Sim | `"ok"` \| `"info"` \| `"attention"` \| `"critical"` \| `"inconclusive"` |
| `decisao_id` | String | Sim | ID da decisão do engine: ex. `"DECISAO-04"` |
| `score_conexao` | Long | Sim | Score 0–100 |
| `confianca` | Double | Sim | 0.0–1.0 |
| `n_resultados_criticos` | Long | Não | Número de findings `critical` |
| `n_resultados_attention` | Long | Não | Número de findings `attention` |
| `versao_app` | String | Sim | |

**Tela:** `DiagnosticoScreen`
**Plataforma:** Android

**Nota de implementação:** não disparado no branch de erro (`catch`) de
`DiagnosticOrchestrator.executar()` — só no caminho de sucesso, como o nome do
evento indica. Não existe `diag_erro` implementado ainda (ver seção abaixo).

---

### `diag_erro`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `tipo_conexao` | String | Sim | |
| `motivo` | String | Não | Mensagem de erro (sem stack trace, sem dados de rede) |
| `versao_app` | String | Sim | |

**Tela:** `DiagnosticoScreen`
**Plataforma:** Android

---

## Eventos — IA / Laudo

### `ia_laudo_solicitado` — implementado (SIG-155)

Disparado quando o app envia o payload ao Worker (`AiDiagnosisRepository`),
apenas para o laudo **inicial** do funil (triggers `"initial"` e
`"initial_from_result"` do `SignallQOrchestrator`).

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `schema_version` | String | Sim | Ex.: `"5"` — de `DiagnosisAiContext.schemaVersion` |
| `prompt_version` | String | Sim | Ex.: `"diagnostico_v5_local_primary"` — de `AI_PROMPT_VERSION` |
| `status_diag_local` | String | Sim | Status do engine local antes de chamar a IA (`DiagnosticStatus.name`) |
| `tem_feedback_usuario` | Boolean | Sim | `true` se havia foco/texto do usuário associado a este laudo |
| `versao_app` | String | Sim | |

**Tela:** `SignallQPulseScreen` / `LaudoScreen` (via `SignallQOrchestrator.callAi`)
**Plataforma:** Android

**Nota de implementação:** perguntas de acompanhamento no chat (chips, texto
livre digitado após o laudo inicial, trigger `"followup_*"`/`"typed_message"`)
**não** disparam este evento — são conversa complementar sobre o mesmo laudo,
não um novo passo do funil. Também não dispara quando o toggle "Análise
avançada" (SIG-282) está desligado, porque nesse caso a IA nunca é chamada
(motor local decide sozinho). O chat separado `ChatDiagnosticoIaScreen`
(`DiagnosticoViewModel.enviarPerguntaDiagnostico`) também não está
instrumentado — é o evento `ia_chat_mensagem_enviada` (ainda não
implementado), não o funil principal.

---

### `ia_laudo_recebido` — implementado (SIG-155)

Disparado quando o resultado da chamada a `AiDiagnosisRepository.explainDiagnosis`
fica disponível (sucesso via IA, fallback local, ou timeout) — sempre pareado
com um `ia_laudo_solicitado` da mesma chamada.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `schema_version` | String | Sim | |
| `prompt_version` | String | Sim | |
| `status_ia` | String | Sim | Status normalizado retornado (`AiDiagnosisResult.status`, sem remapeamento) |
| `source` | String | Sim | `"cloud"` \| `"local"` (fallback ou timeout) |
| `modelo_ia` | String | Não | Família do modelo (`ModeloIa.familia`) — sem revelar `idInterno` |
| `prompt_tokens` | Long | Não | Tokens de entrada consumidos |
| `completion_tokens` | Long | Não | Tokens de saída gerados |
| `total_tokens` | Long | Não | Total de tokens da requisição |
| `latencia_ms` | Long | Não | Tempo entre envio e recebimento da resposta |
| `versao_app` | String | Sim | |

**Tela:** `SignallQPulseScreen` / `LaudoScreen`
**Plataforma:** Android

**Nota de implementação:** `status_ia` envia o valor exato de
`AiDiagnosisResult.status` (pode ser `"excelente"`, `"bom"`, `"regular"`,
`"ruim"`, `"critico"` ou `"inconclusivo"` — o motor de normalização
(`AiDiagnosisRepository.normalizeStatus`) aceita esse conjunto mais amplo do
que os 4 valores originalmente documentados aqui; a tabela foi ajustada para
refletir a implementação real). `latencia_ms` mede o tempo em volta da chamada
`explainDiagnosis` (inclui cache hit, chamada de rede ou fallback local).

---

### `ia_laudo_erro`

Disparado quando a chamada ao Worker falha e o fallback local é ativado.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `prompt_version` | String | Sim | |
| `tipo_erro` | String | Sim | `"timeout"` \| `"http_error"` \| `"parse_error"` \| `"sem_auth"` \| `"desconhecido"` |
| `http_status` | Long | Não | Código HTTP se disponível |
| `latencia_ms` | Long | Não | Tempo até a falha |
| `versao_app` | String | Sim | |

**Tela:** `DiagnosticoScreen`
**Plataforma:** Android

**Nota:** não implementado neste PR. O caso de fallback local já é capturado
como `source: "local"` em `ia_laudo_recebido` (ver acima) — este evento
separado adicionaria detalhe sobre a causa específica da falha, mas exigiria
propagar o tipo de erro de `AiDiagnosisRepository` (hoje só loga via Timber).

---

### `ia_chat_mensagem_enviada`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `n_mensagens_sessao` | Long | Sim | Número de mensagens trocadas na sessão até agora |
| `tem_contexto_diag` | Boolean | Sim | `true` se o chat tem contexto de diagnóstico recente |
| `versao_app` | String | Sim | |

**Tela:** `LLMChatScreen` / `ChatDiagnosticoIaScreen`
**Plataforma:** Android

---

## Eventos — Wi-Fi / Sinal

### `wifi_tela_aberta`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `tipo_conexao` | String | Sim | `"wifi"` \| `"mobile"` \| `"desconectado"` |
| `versao_app` | String | Sim | |

**Tela:** `SinalScreen`
**Plataforma:** Android

---

### `wifi_scan_concluido`

Disparado quando o scan de canais Wi-Fi retorna resultados.

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `n_redes_encontradas` | Long | Sim | Número de redes vizinhas detectadas |
| `banda` | String | Sim | `"ghz24"` \| `"ghz5"` \| `"desconhecida"` |
| `canal_atual` | Long | Não | Canal do AP conectado |
| `versao_app` | String | Sim | |

**Tela:** `SinalScreen`
**Plataforma:** Android

---

## Eventos — Histórico

### `historico_tela_aberta`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `n_registros_locais` | Long | Não | Total de testes armazenados |
| `versao_app` | String | Sim | |

**Tela:** `HistoricoScreen`
**Plataforma:** Android

---

### `historico_exportado`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `formato` | String | Sim | `"csv"` \| `"pdf"` |
| `n_registros` | Long | Sim | Quantidade de registros exportados |
| `versao_app` | String | Sim | |

**Tela:** `ExportHistoricoBottomSheet`
**Plataforma:** Android

---

## Eventos — DNS

### `dns_benchmark_concluido`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `dns_atual_provider` | String | Não | Nome do provedor DNS atual (ex.: `"Cloudflare"`) |
| `dns_atual_latencia_ms` | Long | Não | |
| `dns_melhor_provider` | String | Não | Melhor DNS encontrado no benchmark |
| `dns_melhor_latencia_ms` | Long | Não | |
| `dns_grade` | String | Não | Classificação local: `"A"` \| `"B"` \| `"C"` \| `"D"` |
| `versao_app` | String | Sim | |

**Tela:** `DnsScreen`
**Plataforma:** Android

---

## Eventos — Fibra

### `fibra_tela_aberta`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `fibra_detectada` | Boolean | Sim | `true` se o modem foi detectado |
| `versao_app` | String | Sim | |

**Tela:** `FibraModemScreen`
**Plataforma:** Android

---

## Eventos — Dispositivos

### `dispositivos_scan_iniciado`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `versao_app` | String | Sim | |

**Tela:** `DispositivosScreen`
**Plataforma:** Android

---

### `dispositivos_scan_concluido`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `n_dispositivos` | Long | Sim | Total de dispositivos encontrados na rede |
| `duracao_ms` | Long | Não | Duração do scan |
| `versao_app` | String | Sim | |

**Tela:** `DispositivosScreen`
**Plataforma:** Android

---

## Eventos — Configurações

### `ajustes_monitoramento_alterado`

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `ativado` | Boolean | Sim | `true` se o monitoramento passivo foi ligado |
| `versao_app` | String | Sim | |

**Tela:** `AjustesScreen`
**Plataforma:** Android

---

## Funil principal — implementado (SIG-155)

A sequência de eventos abaixo define o funil de engajamento central do SignallQ.
Use esta ordem para análise de drop-off no Firebase:

```
app_aberto
  → speedtest_iniciado
    → speedtest_concluido
      → diag_iniciado
        → diag_concluido
          → ia_laudo_solicitado
            → ia_laudo_recebido
```

Drop entre `speedtest_concluido` e `diag_iniciado`: usuário não quis analisar.
Drop em `ia_laudo_solicitado` sem `ia_laudo_recebido`: falha de rede ou Worker.

Todos os 7 eventos estão instrumentados via `AnalyticsHelper`
(`core/network/AnalyticsHelper.kt` + `FirebaseAnalyticsHelper` em `:app`,
injetado via Hilt em `AppModule`). Pontos de disparo:

| Evento | Classe | Método |
|---|---|---|
| `app_aberto` | `MainActivity` | `onCreate` |
| `speedtest_iniciado` | `SpeedtestViewModel` | `executarSpeedtest` (antes de `ExecutorSpeedtest.executar`) |
| `speedtest_concluido` | `SpeedtestViewModel` | `executarSpeedtest` (após `ExecutorSpeedtest.executar`, via `registrarSpeedtestConcluidoSeDisponivel`) |
| `diag_iniciado` | `DiagnosticOrchestrator` | `executar(input, enabledAreas)` |
| `diag_concluido` | `DiagnosticOrchestrator` | `executar(input, enabledAreas)` (caminho de sucesso) |
| `ia_laudo_solicitado` | `SignallQOrchestrator` | `callAi` (antes de `AiDiagnosisRepository.explainDiagnosis`, triggers `initial`/`initial_from_result`) |
| `ia_laudo_recebido` | `SignallQOrchestrator` | `callAi` (após `explainDiagnosis`, mesmos triggers) |

Testes unitários do `FirebaseAnalyticsHelper` em
`app/src/test/kotlin/io/veloo/app/kotlin/analytics/FirebaseAnalyticsHelperTest.kt`
(MockK + Robolectric, cobrem os 7 eventos e omissão correta de parâmetros
opcionais nulos).

---

## Como manter

**Regra obrigatória:** qualquer adição, remoção ou alteração de parâmetro de
evento requer atualização deste arquivo no mesmo PR que altera o código.

Checklist ao implementar um novo evento:

- [ ] Nome segue a convenção `prefixo_verbo_passado` em `snake_case`
- [ ] Sem PII nos parâmetros (sem SSID, IP, BSSID, nome de rede)
- [ ] Parâmetros dentro do limite de 25 por evento
- [ ] Tipos corretos (`String`, `Long`, `Double`, `Boolean`)
- [ ] Este arquivo atualizado com o novo evento e seus parâmetros
- [ ] Se o evento integra o funil principal, a seção "Funil principal" foi revisada

Ponto de implementação no Android: injetar `AnalyticsHelper` (funil principal,
SIG-155) ou `AnalyticsTracker` (schema SIG-134) via Hilt no ViewModel ou classe
de domínio correspondente — nunca `FirebaseAnalytics` diretamente. Não chamar
`logEvent` diretamente em Composables.
