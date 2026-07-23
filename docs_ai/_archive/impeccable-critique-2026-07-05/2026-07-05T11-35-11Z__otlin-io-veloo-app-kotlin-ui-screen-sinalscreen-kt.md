---
target: tela Sinal (Wi-Fi/Móvel)
total_score: 26
p0_count: 2
p1_count: 2
timestamp: 2026-07-05T11-35-11Z
slug: otlin-io-veloo-app-kotlin-ui-screen-sinalscreen-kt
---
Method: dual-agent per screen (Assessment A via sub-agent Lia; Assessment B estruturalmente N/A — app Kotlin/Compose nativo sem HTML/CSS/dev-server)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Falta "há quanto tempo" o scan foi feito |
| 2 | Match System / Real World | 2 | BSSID, MHz de largura, espectro sem tradução |
| 3 | User Control and Freedom | 3 | Sheets fecham bem, filtro reversível |
| 4 | Consistency and Standards | 2 | Dois componentes de filtro de banda distintos |
| 5 | Error Prevention | 3 | Tela read-only, sem erros destrutivos |
| 6 | Recognition Rather Than Recall | 3 | Ícone + cor + palavra na maioria dos casos |
| 7 | Flexibility and Efficiency | 2 | Mesma densidade para leigo e avançado |
| 8 | Aesthetic and Minimalist Design | 2 | Metadado técnico repetido em toda linha de lista |
| 9 | Error Recovery | 3 | CanalErroState trata bem permissão negada |
| 10 | Help and Documentation | 3 | WifiChannelGuide bom, mas possivelmente órfão |
| **Total** | | **26/40** | **Acceptable** |

## Priority Issues
- P0: Metadado técnico cru sem veredito em toda lista de redes (Banda/RSSI/Canal em toda linha)
- P0: Gráfico de espectro sem introdução textual antes da curva
- P1: BSSID exposto sem propósito acionável
- P1: Dois componentes de filtro de banda distintos para a mesma função
- P2: Wall of options no grupo multi-BSSID expandido
- P3: WifiChannelGuide possivelmente órfão (confirmar uso)

Ver relatório completo do agente Lia para detalhes de personas e observações menores (inconsistência "Ruim" vs "Fraco", emoji-glifo de check).
