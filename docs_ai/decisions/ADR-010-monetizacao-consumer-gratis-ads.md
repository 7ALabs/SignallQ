# ADR-010 — Monetização do SignallQ consumer: grátis, só anúncios

**Data:** 2026-07-20
**Status:** Aceito

## Contexto

O Luiz perguntou se valeria cobrar pelo app (único, mensal, anual) ou manter grátis. Levantamento
feito antes da decisão:

- Todo concorrente direto de diagnóstico de rede (Speedtest/Ookla, Fast.com, Wifi Analyzer, Fing) é
  grátis — cobrar entrada contraria a expectativa da categoria e derruba instalação.
- Custo real de operação hoje é quase zero (`docs_ai/operations/INFRASTRUCTURE_COSTS.md`: R$0-200/mês
  até 10k usuários, tudo em free tier) — não há pressão de custo recorrente que justifique
  assinatura só para cobrir infraestrutura.
- O motor de monetização (`:coreRecommendation`, issue #790) já foi arquitetado para
  `native_ad_fallback`/`affiliate_product`/`partner_offer`/`operator_offer`, mas nunca foi plugado
  em AdMob/afiliados reais — decisão de monetização por anúncio+afiliado já estava implícita no
  desenho técnico, não é novidade desta conversa.

Recomendação apresentada: manter o app grátis, com opção de camada paga leve (remover anúncio via
IAP único + assinatura "Plus" para features recorrentes tipo monitoramento/IA/laudo ilimitado).

## Decisão

**O Luiz decidiu seguir só com o caminho mais simples: app grátis, monetização só por anúncio.**
Sem paywall, sem assinatura, sem compra avulsa, sem tier premium por ora. Os tipos
`affiliate_product`/`partner_offer`/`operator_offer` que já existem no enum `RecommendationType` de
`:coreRecommendation` não são a direção ativa de monetização agora — ficam no código (não é decisão
de remover a capacidade), só não são o que se implementa em seguida.

## Consequências

- Não implementar paywall, IAP de remoção de anúncio, nem assinatura em nenhuma tela — se alguém
  propuser isso depois, é mudança de decisão que exige novo ADR, não é execução direta de squad.
- Trabalho de monetização daqui pra frente prioriza plugar anúncio nativo real (AdMob) nos pontos
  já preparados com placeholder — ex.: `SimulatedOfferCard` em `JogosScreen.kt` (TODO já registrado
  no código: "substituir este card SIMULADO por rememberNativeAd + NativeAdCard"), e demais usos de
  `SimulatedOfferCard`/`NativeAdCard` no app.
- `RecommendationType.affiliate_product`/`partner_offer`/`operator_offer` continuam existindo no
  contrato da engine (não removidos), mas não entram em nenhum plano de implementação sem decisão
  nova do Luiz.
- Reavaliar preço/modelo pago é decisão de negócio, não técnica — se as métricas de receita por
  anúncio (pós-lançamento) não sustentarem o produto, essa reavaliação volta pra discussão, mas não
  é o caminho planejado hoje.
