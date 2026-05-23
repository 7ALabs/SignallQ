---
description: Regras e checklist para implementar ou modificar o fluxo de speedtest no Linka Android — download, upload, latência, jitter, perda de pacotes e engine de diagnóstico.
---

## Quando usar
Antes de implementar ou alterar qualquer parte do fluxo de speedtest no Android (`:featureSpeedtest`, `:coreNetwork`).

## Arquitetura do fluxo

```
UI (SpeedtestScreen) → SpeedtestViewModel → SpeedtestUseCase → NetworkEngine → Results
```

- ViewModel expõe `StateFlow<SpeedtestUiState>` — nunca valores crus.
- Engine roda em `Dispatchers.IO` — nunca na Main thread.
- Resultados persistidos no Room (`:coreDatabase`) após conclusão.

## Estados obrigatórios da UI

| Estado | O que exibir |
|---|---|
| Idle | Botão de início, última medição |
| Preparing | "Preparando teste..." + spinner |
| DownloadRunning | Velocidade atual em tempo real + barra de progresso |
| UploadRunning | Velocidade atual em tempo real + barra de progresso |
| LatencyRunning | "Medindo latência..." |
| Completed | Resultado completo (down/up/ping/jitter/loss) |
| Error | Mensagem do erro + botão retry |

## Métricas obrigatórias

| Métrica | Unidade | Precisão |
|---|---|---|
| Download | Mbps | 2 casas decimais |
| Upload | Mbps | 2 casas decimais |
| Latência | ms | inteiro |
| Jitter | ms | 1 casa decimal |
| Perda de pacotes | % | 1 casa decimal |

## Thresholds de resultado (Brasil)
Consulte `/network-diagnostic-rules` para thresholds completos de qualidade.

## Anti-padrões
- ❌ Bloquear Main thread durante medição.
- ❌ Exibir Mbps em vez de Mbps/s durante medição em tempo real.
- ❌ Salvar resultado parcial no banco (apenas resultado final completo).
- ❌ Omitir estado de erro — sempre oferecer retry.
- ❌ Calcular jitter sem mínimo de 10 amostras.

## Limites
- Esta skill orienta, não implementa.
- Implementação → Camilo.
