---
name: map-impact
description: Mapeia impacto técnico antes de implementar uma feature, refactor ou correção no Linka. Use antes de qualquer tarefa média ou grande.
---

Mapeie o impacto da tarefa abaixo:

$ARGUMENTS

Use o **Cláudio** para planejamento técnico completo.

[Invocando Cláudio — mapeamento de impacto técnico]

---

## Regra de leitura incremental — OBRIGATÓRIO

Não abra módulo inteiro. Siga a ordem:

1. `Grep` pelo símbolo/classe relevante para encontrar o arquivo
2. `Read` apenas do arquivo encontrado (use offset + limit se grande)
3. Se precisar do módulo inteiro, leia apenas o arquivo de entrada (UseCase, ViewModel ou Composable raiz)
4. Não abra arquivos que não serão referenciados no plano

---

## Entregue

1. **Objetivo** — o que a tarefa precisa resolver
2. **Arquivos prováveis** — com caminhos reais lidos da estrutura atual
3. **Módulos afetados** — quais dos 16 módulos Android e/ou o PWA estão envolvidos
4. **Regras de negócio envolvidas** — thresholds, contratos, engines ou use cases relacionados
5. **Riscos de regressão** — o que pode quebrar em outras partes do app
6. **Plano seguro** — passos incrementais para implementar com risco controlado
7. **Testes necessários** — o que deve ser validado ao final

Não implemente código.

---

## Gatilho Otávio — OBRIGATÓRIO

Se o mapeamento identificar qualquer um destes domínios, **Otávio é obrigatório** antes de passar para Camilo:

- Permissões Android (ACCESS_FINE_LOCATION, FOREGROUND_SERVICE, CHANGE_NETWORK_STATE)
- Wi-Fi APIs (WifiManager, NetworkCapabilities, WifiInfo, ScanResults)
- DNS (LinkProperties, privateDns, InetAddress)
- Background/Foreground service
- ConnectivityManager / NetworkCallback
- OEM quirks (Samsung, Xiaomi, Moto)

**Se houver qualquer um destes:** 
`[PRÓXIMO: Otávio — validar comportamento real em device antes de Camilo implementar]`

**Se nenhum destes:** 
`[PRÓXIMO: Camilo — implementação Android segura]`

---

## Checklist de checkpoint ao final

Antes de passar para implementação, confirme:

- [ ] Todas as tasks têm critério de aceite claro?
- [ ] Alguma task toca domínio crítico Android? → Otávio obrigatório
- [ ] Tasks estão pequenas o suficiente para Camilo/Renan em uma sessão? → Se não, redividir aqui
- [ ] Há impacto visual? → Lia entra antes da implementação

[PRÓXIMO: indicar próximo agente com instrução objetiva]
