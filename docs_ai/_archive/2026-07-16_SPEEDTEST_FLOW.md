> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: FUNCIONAL.md secao 4.1 (Velocidade / Speedtest). Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# Fluxo do Speedtest — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0 — versionCode 56; ChatScreen→SignallQScreen/LLMChatScreen; FEATURE_DIAGNOSTICO_CHAT ativo em release)
**Fonte:** código real

---

## 1. Telas Envolvidas

```
SpeedTestScreen → VelocidadeScreen → ResultadoVelocidadeScreen
```

Telas secundárias (opcionais a partir do resultado):
- `DiagnosticoScreen`
- `LLMChatScreen` (Chat IA livre — `FEATURE_DIAGNOSTICO_CHAT`, ativo em release)
- `SignallQScreen` (Chat diagnóstico autônomo)

---

## 2. Modos de Teste

| Modo | Descrição |
|---|---|
| Rápido | Medição mais curta; menos amostras |
| Completo | Medição padrão completa |
| Triplo | 3 rodadas completas; exibe `CardRodadasTriplo` expansível com resultados individuais |

Seleção via `ModeSelector` (pills) em `SpeedTestScreen`.

---

## 3. Fluxo Completo

### Etapa 1 — SpeedTestScreen (pré-execução)

1. Usuário chega em SpeedTestScreen (aba 1 ou via HomeScreen).
2. Seleciona modo: rápido / completo / triplo.
3. Visualiza informações de contexto: último resultado, servidor, status de rede.
4. Toca "Iniciar Teste".
   - Se conexão for dados móveis: exibe diálogo de confirmação com estimativa de consumo de dados. Usuário confirma ou cancela.
5. Teste iniciado → transição para `VelocidadeScreen`.

### Etapa 2 — VelocidadeScreen (execução)

Fases em ordem:

| Fase | O que mede | Exibição |
|---|---|---|
| LATÊNCIA | Ping (RTT) em ms | Gauge + pill "LATÊNCIA" |
| DOWN | Download em Mbps (ao vivo) | Gauge + pill "DOWN" + MiniGrafico |
| UP | Upload em Mbps (ao vivo) | Gauge + pill "UP" + MiniGrafico |
| CONCLUÍDO | — | Checkmarks em todas as pills + haptic |

Durante cada fase:
- `GaugeCircular` mostra progresso global + fase atual + valor em Mbps em tempo real.
- `MiniGrafico` exibe gráfico ao vivo de `PontoAoVivo` (amostras de velocidade).
- `PillsFase` mostra status de cada fase; ao concluir, a pill recebe checkmark.
- Haptics disparam na transição entre fases.
- `LinhaServidor` exibe localização do servidor + nome do ISP.

Ações disponíveis durante execução:
- Cancelar → interrompe e volta para SpeedTestScreen.
- Reiniciar → recomeça o teste do início.

Em caso de erro:
- `ErroContent` exibe botões "Testar Novamente" e "Cancelar".

### Etapa 3 — ResultadoVelocidadeScreen (resultado)

Layout em ordem de exibição:

1. **Grade circle:** classificação A / B / C / D / ? com cor correspondente.
2. **Título + mensagem:** diagnóstico resumido em linguagem natural.
3. **Cards de métricas primárias:** Download (Mbps) + Upload (Mbps).
4. **Cards de métricas secundárias:** Latência (ms) + Jitter (ms).
5. **Chip de contaminação:** exibido se o teste foi marcado como contaminado.
6. **Cards de qualidade:** Perda de pacotes (%) + Bufferbloat (ms).
7. **Experiência de uso:** vereditos individuais para:
   - Streaming: good / acceptable / poor
   - Gaming: good / acceptable / poor
   - Vídeo Chamada: good / acceptable / poor
8. **DNS Info:** provedor DNS em uso + latência.
9. **Detalhes Avançados** (expansível): pico DL, pico UL, latência com carga, estabilidade.
10. **RecomendacaoCard:** ação recomendada baseada no diagnóstico.

**Botões disponíveis:**
- "Conversar com IA" → `LLMChatScreen` (FEATURE_DIAGNOSTICO_CHAT ativo em release)
- "Testar Upload Novamente" → reinicia apenas a fase de upload
- "Ir para o início" → `HomeScreen`
- "Testar novamente" → `SpeedTestScreen`

---

## 4. Métricas Medidas

| Métrica | Unidade | Campo em MedicaoEntity |
|---|---|---|
| Download | Mbps | `downloadMbps` |
| Upload | Mbps | `uploadMbps` |
| Latência | ms | `latencyMs` |
| Jitter | ms | `jitterMs` |
| Perda de pacotes | % | `perdaPercentual` |
| Bufferbloat | ms | `bufferbloatMs` |
| Pico download | Mbps | — (exibido em Detalhes Avançados) |
| Pico upload | Mbps | — (exibido em Detalhes Avançados) |

**Fonte de perda de pacotes** (`packetLossSource`): `"download"` ou `"upload"` — indica em qual fase a perda foi detectada.

---

## 5. Persistência do Resultado

Ao concluir o teste, o resultado é salvo automaticamente em Room na tabela `medicao` (`MedicaoEntity`):

```
MedicaoEntity(
    id = UUID gerado,
    timestampEpochMs = timestamp atual,
    connectionType = "wifi" | "movel" | "ethernet",
    speedtestMode = "complete" | "ping_only" | ...,
    downloadMbps = <valor>,
    uploadMbps = <valor>,
    latencyMs = <valor>,
    jitterMs = <valor>,
    perdaPercentual = <valor>,
    bufferbloatMs = <valor>,
    packetLossSource = "download" | "upload" | null,
    vereditoStreaming = "good" | "acceptable" | "poor",
    vereditoGamer = "good" | "acceptable" | "poor",
    vereditoVideoChamada = "good" | "acceptable" | "poor",
    contaminado = true | false,
    fonte = "android"
)
```

---

## 6. Avaliação de Qualidade (Referências)

| Métrica | Limiar de atenção | Referência |
|---|---|---|
| Latência | > 100ms | ANATEL RQUAL |
| Jitter | > 20ms | Diagnóstico interno |
| Perda de pacotes | ≥ 1% atenção / ≥ 3% crítico | Diagnóstico interno |
| Bufferbloat | > 30ms atenção / > 100ms crítico | DSLReports / Waveform |
| Download | < 25 Mbps | Diagnóstico interno |
| Upload | < 5 Mbps atenção / 0 Mbps crítico | Diagnóstico interno |
| ANATEL mínimo | 40% do plano contratado | Resolução ANATEL |
| ANATEL normal | 80% do plano contratado | Resolução ANATEL |

---

## 7. Bufferbloat — Severidades

| Nível | Valor de bufferbloat |
|---|---|
| none | < 5ms |
| mild | 5 – 30ms |
| moderate | 30 – 100ms |
| severe | > 100ms |

---

## 8. Fluxo com Dados Móveis

Quando `snapshotRede.estadoConexao == movel`, ao iniciar o teste:
1. `SpeedTestScreen` exibe diálogo de confirmação.
2. Diálogo mostra estimativa de consumo de dados.
3. Usuário escolhe: "Continuar mesmo assim" ou "Cancelar".
4. Se continuar: fluxo normal de execução.
