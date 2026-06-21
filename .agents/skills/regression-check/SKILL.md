---
description: Checklist de regressão — verifica que flows existentes não foram quebrados por uma mudança recente. Executado por Gema antes de aprovar entrega.
---

## Quando usar
Após qualquer mudança que toque módulos core (`:coreNetwork`, `:coreDatabase`, `:coreDatastore`) ou múltiplas features.

## Flows críticos para testar (Android)

| Flow | O que verificar |
|---|---|
| Home → Speedtest | Inicia e conclui sem crash |
| Home → Wi-Fi | Lista redes, exibe RSSI, identifica banda |
| Home → Diagnóstico | Completa diagnóstico, exibe resultado |
| Home → Histórico | Lista medições anteriores corretamente |
| Home → DNS | Consulta DNS, exibe latência e status |
| Speedtest → Resultado | Salva no banco, exibe no histórico |
| Cold start com permissão negada | App não crasha, solicita permissão contextualmente |
| Modo offline | App não crasha, exibe estado offline |

## Flows críticos para testar (PWA)

| Flow | O que verificar |
|---|---|
| Speedtest completo | Inicia, mede download/upload/ping, exibe resultado |
| Resultado → Histórico | Resultado aparece no histórico local |
| Offline → Online | App se recupera sem refresh manual |
| PWA instalada | Funciona como app standalone |

## Método de verificação

Para cada flow:
1. Executar o flow manualmente ou verificar que o código dos fluxos não foi alterado.
2. Se código foi alterado: verificar que a mudança não quebra contrato de interface.
3. Verificar logs de crash se disponíveis.

## Regras de severidade

| Severidade | Critério | Ação |
|---|---|---|
| Crítico | Flow não inicia ou crasha | BLOQUEAR entrega |
| Alto | Flow completo mas com dado errado | BLOQUEAR entrega |
| Médio | Flow funciona mas com UX degradada | Registrar, entregar com nota |
| Baixo | Problema cosmético | Registrar como follow-up |

## Limites
- Não testa código que não foi tocado — foca no diff da entrega.
- Gema não implementa fix — bloqueia e devolve para Camilo/Renan.
