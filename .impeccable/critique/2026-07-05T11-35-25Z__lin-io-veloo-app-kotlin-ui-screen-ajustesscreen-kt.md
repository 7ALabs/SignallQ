---
target: tela Ajustes
total_score: 24
p0_count: 1
p1_count: 2
timestamp: 2026-07-05T11-35-25Z
slug: lin-io-veloo-app-kotlin-ui-screen-ajustesscreen-kt
---
Method: dual-agent per screen (Assessment A via sub-agent Lia; Assessment B estruturalmente N/A — app Kotlin/Compose nativo sem HTML/CSS/dev-server)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Falta loading state visível ao salvar toggle |
| 2 | Match System / Real World | 3 | "Fibra óptica" expõe host técnico |
| 3 | User Control and Freedom | 2 | Ações destrutivas sem confirmação em PrivacidadeScreen |
| 4 | Consistency and Standards | 1 | Mesma ação (apagar dados) com 2 comportamentos de confirmação diferentes |
| 5 | Error Prevention | 1 | PrivacidadeScreen não previne toque acidental em ação irreversível |
| 6 | Recognition Rather Than Recall | 3 | Ícone + label + subtítulo cobrem bem |
| 7 | Flexibility and Efficiency | 3 | Estado vazio com CTA claro |
| 8 | Aesthetic and Minimalist Design | 3 | Cards flat, hierarquia por seção |
| 9 | Error Recovery | 2 | Sem confirmação em Privacidade, sem desfazer |
| 10 | Help and Documentation | 3 | Subtítulos funcionam como ajuda contextual |
| **Total** | | **24/40** | **Acceptable** |

## Priority Issues
- P0: Ação destrutiva sem confirmação em PrivacidadeScreen (apagar dados/resetar app direto ao toque)
- P1: Três lugares distintos para a mesma família de ação (limpar/apagar/resetar dados)
- P1: Padrão de navegação inconsistente entre "editar perfil" (sheet) e "editar conexão" (tela cheia)
- P2: "Fibra óptica" expõe host técnico ao usuário leigo
- P3: Seção "Dados móveis" com item único gera seção subdimensionada

Ver relatório completo do agente Lia para detalhes de personas e observações menores.
