## Contexto
O `ExecutorSpeedtestCloudflare` baixa até 25 MB no modo `complete` e 30 MB no modo `triplo` (3×10 MB) sem detectar se o usuário está em rede medida (4G/5G). Em planos limitados, um único teste pode consumir uma fração significativa da franquia. Não há aviso nem fallback para modo leve. Esse é o maior vazamento de dados móveis do app.

## Evidência
- `app/.../speedtest/ExecutorSpeedtestCloudflare.kt:1257` — modo `fast` 10 MB
- `app/.../speedtest/ExecutorSpeedtestCloudflare.kt:1271` — modo `complete` 25 MB
- `app/.../speedtest/ExecutorSpeedtestCloudflare.kt:1287` — modo `triplo` 3×10 MB
- Sem uso de `ConnectivityManager.NET_CAPABILITY_NOT_METERED`

## Critério de aceite
- [ ] `ExecutorSpeedtestCloudflare` recebe um `NetworkCapabilitiesProvider` (injetado) e detecta `NET_CAPABILITY_NOT_METERED` + `TRANSPORT_CELLULAR`
- [ ] Em rede medida: bloquear automaticamente `complete` e `triplo`; oferecer apenas `fast` (10 MB) com confirmação explícita
- [ ] Dialog com estimativa: "Este teste vai consumir ~10 MB de dados móveis. Continuar?"
- [ ] Preferência opcional do usuário: "Sempre permitir testes pesados em dados móveis" (default OFF)
- [ ] Telemetria local: quantos MB consumidos no mês (mostrar em Settings)
- [ ] UX preservada em Wi-Fi: nenhum dialog adicional

## Como verificar
Manual no device com SIM:
1. Wi-Fi ativo → todos os modos disponíveis sem dialog
2. Wi-Fi desligado, mobile ativo → dialog aparece; `complete`/`triplo` desabilitados
3. Configuração "permitir pesado em mobile" ON → dialog explícito antes de pesado

## Notas para o agente
- Skills: `signallq-arch`, `signallq-design` (dialog visual)
- Impacto estimado: -40 a -60% de dados móveis consumidos pelo speedtest
- Dependências: independente
