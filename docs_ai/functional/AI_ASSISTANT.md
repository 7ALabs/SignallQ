# Assistente IA SignallQ — Funcionalidade

**Última atualização:** 2026-07-05 (v0.23.0 — versionCode 56; modelo Qwen3 30B; SignallQScreen; LLMChatScreen)
**Fonte:** código real
**Público-alvo:** desenvolvedor humano e agentes de IA

---

## 1. Papel da IA no Produto

O assistente IA do SignallQ tem **dois modos de atuação** que coexistem:

| Modo | Tela | Flag | Estado release |
|---|---|---|---|
| Diagnóstico autônomo (pipeline) | `SignallQScreen` | — (sempre presente) | Ativo |
| Chat diagnóstico livre (LLM) | `LLMChatScreen` | `FEATURE_DIAGNOSTICO_CHAT` | Ativo (true em release) |

---

## 2. SignallQScreen — Diagnóstico Autônomo

**Arquivo:** `app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/SignallQScreen.kt`

O `SignallQOrchestrator` executa um pipeline multi-turno de diagnóstico:

1. Coleta dados da rede atual (Wi-Fi, móvel, histórico, speedtest silencioso se necessário)
2. Monta `DiagnosisAiContext` (schema v3)
3. Envia ao Worker Cloudflare
4. Exibe resultado estruturado com chips de follow-up
5. Máx. 5 turnos; detecção de off-topic ativa

**Modelo padrão:** Gemini 2.0 Flash (primário, quando `GEMINI_API_KEY` configurada) / Qwen3 30B (fallback cloud)  
**Fallback:** `AiFallbackFactory` — diagnóstico local sem IA se o Worker falhar  
**API:** Worker Cloudflare (URL em `AiDiagnosisRepository`)

**Estados do pipeline (`SignallQUiState`):**
- `Idle` — tela de boas-vindas (`SignallQWelcomeState`)
- `Collecting` — coletando dados de rede
- `Thinking` — aguardando resposta do Worker
- `Analyzing` — processando resultado
- `AwaitingChipSelection` — aguardando escolha de chip de follow-up
- `AwaitingAnswer` — aguardando resposta a pergunta dinâmica (`DynamicQuestionEngine`)
- `Result` — diagnóstico concluído; chips de próximo turno disponíveis
- `Erro` — falha com mensagem e botão "Tentar novamente"

**Componentes de UI (todos `SignallQ*`):**
- `SignallQTopBar` — header com estado atual
- `SignallQWelcomeState` — idle state
- `SignallQUserMessageBubble` — bolha do usuário
- `SignallQAiMessageBubble` — bolha da IA (markdown renderizado)
- `SignallQThinkingBubble` — animação "Analisando..."
- `SignallQInlineQuestion` — chips de resposta rápida
- `SignallQInputArea` — campo de texto livre + chips no rodapé
- `AiModelFooter` — exibe nome/versão do modelo IA

---

## 3. LLMChatScreen — Chat Livre com Contexto

**Arquivo:** `app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/LLMChatScreen.kt`

Chat livre com contexto do diagnóstico e histórico injetados. O usuário conversa com o modelo sobre sua conexão.

**Ativação:** botão "Tirar dúvidas" no footer da `DiagnosticoScreen`, ou "Conversar com IA" em `ResultadoVelocidadeScreen`.

**Características:**
- Streaming de resposta (texto progressivo)
- Seção "Thinking" expansível com tokens de raciocínio visíveis
- Sessão persistida em Room (`chat_sessions` + `chat_messages`)
- Cota diária rolling 24h via `CotaDiariaRepository`
- Retomada de conversa (follow-up com contexto anterior)

**Estados:** Idle / Thinking / AwaitingInput / Error / Timeout (com UI de retry)

---

## 4. Contexto Enviado à IA

O `DiagnosisAiContext` inclui:
- Tipo de conexão real (`wifi`, `movel`, `ethernet`) — não fixo
- Resultado do speedtest (DL, UL, latência, jitter, perda, bufferbloat)
- Snapshot Wi-Fi (RSSI, canal, banda) se disponível
- Snapshot móvel (RSRP, SINR, tecnologia) se disponível
- Snapshot fibra GPON (Rx, Tx, temperatura) se disponível
- Histórico resumido de medições
- Respostas anteriores do usuário na sessão (acumuladas pelo `ContextAccumulator`)

---

## 5. Worker Cloudflare

**Localização do código:** `integrations/cloudflare/ai-diagnosis-worker/src/`  
**Deploy:** `npx wrangler deploy` (deve ser feito ANTES de commit quando há mudanças no worker)  
**Modelo padrão:** Qwen3 30B  
**Fallback local:** `AiFallbackFactory` (sem depender de rede)

---

## 6. Superfície Visual da IA

Todos os componentes `SignallQ*` usam paleta escura fixa:
- Background: `#0D0D1A`
- Surface: `#1A0B2E`
- Card: `#1E1130`
- Texto: `#F3F4F6`

Independente do tema claro/escuro configurado pelo usuário no sistema.
