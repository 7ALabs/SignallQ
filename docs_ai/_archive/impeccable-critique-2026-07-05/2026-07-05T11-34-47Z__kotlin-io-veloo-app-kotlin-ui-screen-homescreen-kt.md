---
target: tela Início (HomeScreen)
total_score: 24
p0_count: 1
p1_count: 1
timestamp: 2026-07-05T11-34-47Z
slug: kotlin-io-veloo-app-kotlin-ui-screen-homescreen-kt
---
Method: dual-agent per screen (Assessment A via sub-agent Lia; Assessment B estruturalmente N/A — app Kotlin/Compose nativo sem HTML/CSS/dev-server; detect.mjs rodado contra o arquivo, confirmado 0 achados por incompatibilidade de formato, não por qualidade)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Loading/erro do connector bem sinalizados |
| 2 | Match System / Real World | 2 | IPs/dBm crus quebram tradução |
| 3 | User Control and Freedom | 3 | Sheets dismissáveis, cancelar nos modais |
| 4 | Consistency and Standards | 2 | Accent usado decorativamente no mini-gráfico |
| 5 | Error Prevention | 3 | Distinção clara entre carregando/erro real |
| 6 | Recognition Rather Than Recall | 2 | Diagrama de topologia exige interpretação |
| 7 | Flexibility and Efficiency | 3 | Toques abrem detalhe para quem quer se aprofundar |
| 8 | Aesthetic and Minimalist Design | 1 | 6 blocos + ~10 alvos de toque |
| 9 | Error Recovery | 2 | Erro visual sem copy de próximo passo |
| 10 | Help and Documentation | 3 | Mini-card Diagnóstico (IA) dá saída natural |
| **Total** | | **24/40** | **Acceptable** |

## Priority Issues
- P0: HeroSpeed sem veredito humano ao lado do número (Download/Upload)
- P1: Densidade da tela (6 blocos, ~10 alvos de toque)
- P2: IPs crus como default no NetworkPath
- P2: Violeta usado decorativamente no MiniLineChart (deveria ser phase-download)
- P3: Métricas de sinal empacotadas longe do veredito

Ver relatório completo do agente Lia para detalhes de personas, carga cognitiva e observações menores.
