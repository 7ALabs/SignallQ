# Product Flow for Agents

> **Fonte da verdade do squad e do fluxo:** [`ai-governance/agents/`](../../../ai-governance/agents/) + [`AGENTS.md`](../../AGENTS.md) do repo. Este arquivo resume o fluxo de produto do app; para o fluxo de trabalho dos agentes, ver [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) e [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Objetivos do produto

- **Diagnóstico de rede doméstica**: mede velocidade, Wi-Fi, DNS, latência, jitter e perda de pacotes.
- **Interpretação assistida por IA**: resultados explicados em linguagem acessível via assistente SignallQ (Worker `ai-diagnosis-worker`).
- **Monitoramento passivo**: WorkManager mede latência, DNS e RSSI em background, com notificações de degradação.

## Jornada do usuário — Diagnóstico com IA

1. Usuário acessa `DiagnosticoScreen.kt` (aba Diagnóstico).
2. App coleta dados locais (Wi-Fi, velocidade, DNS, telefonia).
3. Dados enviados ao Worker Cloudflare via `coreNetwork`.
4. Resposta da IA retornada e exibida como chat em `DiagnosticoScreen.kt`.
5. Usuário interage com chips contextuais e árvore de perguntas dinâmica.
6. Diagnóstico complementar gerado com contexto acumulado.

## Telas principais

- `DiagnosticoScreen.kt` — diagnóstico guiado por IA (chat, chips, análise)
- `HomeScreen.kt` — visão geral da conexão atual (RSSI, gateway, tipo de rede)
- `WifiScreen.kt` / `SinalScreen.kt` — análise de redes e canais Wi-Fi
- `SpeedtestScreen.kt` — medição de velocidade
- `DnsScreen.kt` — diagnóstico de DNS
- `DispositivosScreen.kt` — dispositivos na rede
- `HistoryScreen.kt` — histórico de medições
- `AjustesScreen.kt` — configurações, ISP, monitoramento passivo
- `EquipamentoInternetScreen.kt` — status do equipamento (ONT/roteador)

## Docs relacionados

- [`docs_ai/FUNCIONAL.md`](../../docs_ai/FUNCIONAL.md) — feature de IA, fluxo de diagnóstico, mapa completo de telas
- [`docs_ai/technical/`](../../docs_ai/technical/) — WorkManager, monitoramento passivo, especificações pontuais
