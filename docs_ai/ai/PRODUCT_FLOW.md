# Product Flow for Agents

> Documentação do fluxo de produto para consumo por agentes de IA.
> Versão: v0.16.0

## Objetivos do produto

- **Diagnóstico de rede doméstica**: mede velocidade, Wi-Fi, DNS, latência, jitter e perda de pacotes.
- **Interpretação assistida por IA**: resultados explicados em linguagem acessível via assistente SignallQ.
- **Monitoramento passivo**: WorkManager mede latência, DNS e RSSI a cada 30 minutos em background, com notificações de degradação.

## Jornada do usuário — Diagnóstico com IA

1. Usuário acessa `DiagnosticoScreen.kt` (aba Diagnóstico).
2. App coleta dados locais (Wi-Fi, velocidade, DNS, telefonia).
3. Dados enviados ao worker Cloudflare via `coreNetwork`.
4. Resposta da IA retornada e exibida como chat em `DiagnosticoScreen.kt`.
5. Usuário interage com chips contextuais e árvore de perguntas dinâmica.
6. Diagnóstico complementar gerado com contexto acumulado.

## Telas principais

- `DiagnosticoScreen.kt` — diagnóstico guiado por IA (chat, chips, análise)
- `HomeScreen.kt` — visão geral da conexão atual (RSSI, gateway, tipo de rede)
- `WifiScreen.kt` — análise de redes e canais Wi-Fi
- `SpeedtestScreen.kt` — medição de velocidade
- `DnsScreen.kt` — diagnóstico de DNS
- `DevicesScreen.kt` — dispositivos na rede
- `HistoryScreen.kt` — histórico de medições
- `AjustesScreen.kt` — configurações, ISP, monitoramento passivo

## Docs relacionados

- `functional/AI_ASSISTANT.md` — detalhes da feature de IA
- `functional/DIAGNOSTIC_FLOW.md` — fluxo de diagnóstico
- `functional/SCREENS_ANDROID.md` — mapa completo de telas
- `technical/MONITORAMENTO_PASSIVO.md` — WorkManager, notificações
