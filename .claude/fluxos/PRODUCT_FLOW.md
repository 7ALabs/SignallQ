# Product Flow

> **Fonte da verdade:** [`AGENTS.md`](../../AGENTS.md) + [`.claude/agents/claudete.md`](../agents/claudete.md).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Produto SignallQ

**Diagnóstico de rede doméstica para usuário brasileiro**, exclusivamente Android + Web (`signallq.com`, repo `signallq-web`). Freemium com propaganda; recursos pagos futuros possíveis. Ver [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).

## Objetivos

- **Diagnóstico** — velocidade, Wi-Fi, DNS, latência, jitter, perda de pacotes.
- **Interpretação assistida por IA** — resultados explicados em linguagem acessível via Worker `ai-diagnosis-worker`.
- **Monitoramento passivo** — WorkManager mede latência, DNS e RSSI em background, com notificações de degradação.

## Jornada — Diagnóstico com IA

1. Usuário acessa `DiagnosticoScreen.kt`.
2. App coleta dados locais (Wi-Fi, velocidade, DNS, telefonia).
3. Dados vão pro Worker Cloudflare via `coreNetwork`.
4. Resposta da IA vira chat em `DiagnosticoScreen.kt`.
5. Usuário interage com chips e árvore dinâmica de perguntas.
6. Diagnóstico complementar com contexto acumulado.

## Telas principais

- `DiagnosticoScreen.kt` — chat de IA
- `HomeScreen.kt` — visão geral da conexão
- `WifiScreen.kt` / `SinalScreen.kt` — Wi-Fi e canais
- `SpeedtestScreen.kt` — velocidade
- `DnsScreen.kt` — DNS
- `DispositivosScreen.kt` — dispositivos na rede
- `HistoryScreen.kt` — histórico
- `AjustesScreen.kt` — configurações, ISP, monitoramento passivo
- `EquipamentoInternetScreen.kt` — status ONT/roteador

## Fora do escopo

- SignallQ Pro, ISP, Nethal — descontinuados (ADR-016).
- iOS, macOS, desktop, wearable, TV, embedded — fora permanente (ADR-016).
- Linka — produto separado, exclusivo Apple, repo próprio (`linka`, a ser criado).

## Docs relacionados

- [`docs_ai/FUNCIONAL.md`](../../docs_ai/FUNCIONAL.md) — features detalhadas
- [`docs_ai/technical/`](../../docs_ai/technical/) — WorkManager, monitoramento passivo
