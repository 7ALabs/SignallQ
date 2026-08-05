## Contexto
Durante o download/upload do speedtest, um job paralelo dispara ping a cada 300 ms. Em 7–18 s de transferência isso adiciona 23–60 chamadas HTTP extras (~500 KB de overhead em mobile) e ocupa CPU/rádio sem ganho proporcional — para "ping sob carga" basta uma amostragem esparsa.

## Evidência
- `app/.../speedtest/ExecutorSpeedtestCloudflare.kt:587` — `while (System.nanoTime() < stopNs) { delay(max(0L, 300L - elapsed)) ... }`
- Mesmo padrão repetido em :782 e :1168

## Critério de aceite
- [ ] Intervalo de ping concorrente elevado para 1500–2000 ms (3–4 amostras durante a janela)
- [ ] Em rede mobile: pausar ping concorrente durante throughput; medir antes e depois
- [ ] Cálculo de jitter/loss não degrada perceptivelmente (validar com 5 runs comparativos)
- [ ] UI mostra "ping sob carga" com mesma resolução visual

## Como verificar
Comparar 5 execuções antes/depois em Wi-Fi e em 4G:
- bytes HTTP totais (medir via OkHttp `EventListener`)
- valor final de jitter/loss reportado (deve permanecer ±5% do baseline)

## Notas para o agente
- Skills: `signallq-arch`
- Impacto estimado: -15% energia durante speedtest, -500KB por teste em mobile
- Dependências: independente; conversa com #17
