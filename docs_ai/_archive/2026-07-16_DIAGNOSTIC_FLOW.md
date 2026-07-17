> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: FUNCIONAL.md secao 4.2 (Diagnostico assistido por IA). Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# Fluxo de Diagnóstico — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0 — versionCode 56; OrbitOrchestrator→SignallQOrchestrator; OrbitInlineQuestion→SignallQInlineQuestion; modelo Qwen3 30B; ChatScreen→SignallQScreen)
**Fonte:** código real

---

## 1. Telas Envolvidas

```
DiagnosticoScreen → DiagnosticOrchestrator → engines → DiagnosticDecisionEngine → resultado
[opcional] → SignallQScreen (diagnóstico autônomo) | LLMChatScreen (chat livre, FEATURE_DIAGNOSTICO_CHAT)
```

---

## 2. Pontos de Entrada

O diagnóstico pode ser iniciado a partir de:

| Origem | Como |
|---|---|
| `SpeedTestScreen` | Via `ExploreToolsRow` → botão "Diagnóstico" |
| `ResultadoVelocidadeScreen` | Diretamente após o speedtest |

---

## 3. Fluxo Principal

### Etapa 1 — DiagnosticoScreen (estado Idle)

Usuário chega em `DiagnosticoScreen`. Estado inicial: Idle.

Ações disponíveis:
- Iniciar diagnóstico (botão ou automático após speedtest)
- Selecionar chips contextuais
- Enviar contexto adicional

### Etapa 2 — Execução (estado Executando)

`DiagnosticOrchestrator` executa engines em sequência:

```
DiagnosticOrchestrator
├── WifiSignalQualityEngine       → qualidade do sinal Wi-Fi (RSSI → WifiQualityResult)
├── InternetDiagnosticEngine      → velocidade, latência, jitter, perda, bufferbloat
├── WifiChannelDiagnosticEngine   → congestionamento de canal Wi-Fi
├── DnsDiagnosticEngine           → qualidade do DNS em uso
├── HistoricalDegradationEngine   → degradação histórica (médias 7d vs 30d)
├── FibraSignalQualityEngine      → sinal GPON (se fibra disponível)
├── MobileSignalDiagnosticEngine  → sinal 4G/5G (se conexão móvel)
└── DiagnosticDecisionEngine      → decisão final consolidada
```

**Entrada do orchestrator (`DiagnosticInput`):**

| Campo | Tipo | Descrição |
|---|---|---|
| `connectionType` | ConnectionType | Tipo de conexão atual |
| `internet` | InternetDiagnosticInput? | Dados do speedtest |
| `wifi` | WifiDiagnosticInput? | Dados do sinal Wi-Fi |
| `fibra` | FibraDiagnosticInput? | Dados da ONT GPON |
| `mobile` | MobileDiagnosticInput? | Dados de sinal móvel |
| `dns` | DnsDiagnosticInput? | Dados do benchmark DNS |
| `historico` | HistoricalDiagnosticInput? | Resumo histórico |
| `wifiScan` | WifiScanDiagnosticInput? | Redes vizinhas detectadas |

### Etapa 3 — DiagnosticDecisionEngine (consolidação)

Recebe resultados de todos os engines e aplica regras de decisão em ordem de prioridade. Retorna um único `DiagnosticResult` principal.

**Regras de decisão:**

| ID | Condição | Status | Diagnóstico |
|---|---|---|---|
| DECISAO-DNS-01 | DNS crítico (sem outro problema crítico) | `critical` | Problema no DNS |
| DECISAO-HIST-01 | Degradação histórica detectada | `critical` ou `attention` | Degradação Recente Detectada |
| DECISAO-WIFI-CANAL | Canal Wi-Fi congestionado | `attention` | Possível Congestionamento de Wi-Fi |
| DECISAO-00 | Fibra crítica + internet ruim | `critical` | Problema na Fibra |
| DECISAO-01 | Internet ruim + Wi-Fi bom | `critical` | Problema no ISP |
| DECISAO-02 | Wi-Fi ruim + internet ok | `attention` | Problema Local (Wi-Fi) |

### Etapa 4 — DiagnosticoScreen (estado Concluído)

Cards dinâmicos exibidos por resultado de engine:
- Ícone do status
- `status badge`: OK / INFO / ATTENTION / CRITICAL
- Mensagem para o usuário
- Recomendação de ação

---

## 4. Modelo de Resultado

Cada engine retorna `DiagnosticResult`:

```
DiagnosticResult(
    id: String,                    // ex.: "DECISAO-01", "WIFI-03"
    titulo: String,                // ex.: "Problema no ISP"
    status: DiagnosticStatus,      // ok | info | attention | critical | inconclusive
    mensagemUsuario: String,       // texto em linguagem natural
    recomendacao: String?,         // ação sugerida
    categoria: String              // ex.: "internet", "wifi", "dns", "fibra"
)
```

---

## 5. IA Conversacional (SignallQ) — Passo Opcional

Após o diagnóstico, o usuário pode iniciar uma sessão com o SignallQ IA via `SignallQScreen`.

```
SignallQScreen → SignallQOrchestrator
    ├── Coleta dados da rede atual
    ├── Speedtest silencioso (sem abrir VelocidadeScreen)
    ├── Monta payload DiagnosisAiContext (schema v3)
    └── Envia ao Worker Cloudflare
        └── Gemini 2.0 Flash (primário) / Qwen3 30B (fallback cloud) / fallback local
            └── AiDiagnosisResult (JSON)
                └── SignallQScreen exibe resposta em markdown
```

**API:** Worker Cloudflare (URL configurada em `AiDiagnosisRepository`)

**Máx. turnos:** 5 (configurado no `SignallQOrchestrator`). Detecção de off-topic ativa.

**Estados do SignallQ:**
- `Idle`: aguardando início
- `Collecting`: coletando dados de rede
- `Thinking`: aguardando resposta do Worker
- `Analyzing`: processando resultado
- `AwaitingChipSelection`: aguardando escolha de chip pelo usuário
- `AwaitingAnswer`: aguardando resposta a pergunta dinâmica
- `Result`: diagnóstico concluído
- `Erro`: falha na IA

**Se a IA falhar:** `AiFallbackFactory` gera diagnóstico local sem IA.

### DynamicQuestionEngine

Gera perguntas contextuais baseadas no estado atual da rede para refinar o diagnóstico. Exibidas como chips de resposta rápida em `SignallQInlineQuestion` (ex-`OrbitInlineQuestion`).

### ContextAccumulator

Acumula as respostas do usuário durante a sessão. O contexto acumulado é enviado ao Worker em iterações subsequentes para diagnóstico progressivo.

---

## 6. Diagnóstico Silencioso no SignallQ

Quando o usuário inicia o SignallQ sem ter feito um speedtest manual, o `SignallQOrchestrator` executa:
1. Coleta de dados da rede (Wi-Fi, móvel, histórico)
2. Speedtest silencioso — sem abrir `VelocidadeScreen`, sem gauge visível
3. Envio de todos os dados ao Worker Cloudflare
4. Exibição do resultado diretamente no `SignallQScreen`

---

## 7. Engines — Referência Rápida

| Engine | Entrada principal | Saída |
|---|---|---|
| `WifiSignalQualityEngine` | RSSI, frequência, link speed | `WifiQualityResult` + `DiagnosticResult` |
| `InternetDiagnosticEngine` | DL, UL, latência, jitter, perda, bufferbloat | `DiagnosticResult` |
| `WifiChannelDiagnosticEngine` | Redes vizinhas + canal conectado | `DiagnosticResult` |
| `DnsDiagnosticEngine` | IP DNS, latência, grade, melhor alternativa | `DiagnosticResult` |
| `HistoricalDegradationEngine` | Médias 7d / 30d, contagem de testes | `DiagnosticResult` |
| `FibraSignalQualityEngine` | rxPower, txPower, temperatura, isUp | `DiagnosticResult` |
| `MobileSignalDiagnosticEngine` | Tecnologia, RSRP, RSRQ, SINR, banda | `DiagnosticResult` |
| `DiagnosticDecisionEngine` | Todos os resultados acima + RTT gateway | `DiagnosticResult` único |
