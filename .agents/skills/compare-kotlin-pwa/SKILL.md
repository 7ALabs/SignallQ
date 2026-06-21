---
name: compare-kotlin-pwa
description: Compara uma feature entre Android Kotlin e PWA Linka SpeedTest. Identifica diferenças reais vs. limitações técnicas do navegador.
---

Compare a feature abaixo entre Android Kotlin e PWA:

$ARGUMENTS

Use o **Renan** para análise PWA e o **Camilo** para Android.

[Invocando Renan + Camilo — comparação de paridade Android/PWA]

---

## Checkpoint pré-análise — Limitações do browser

Antes de mapear divergências, liste explicitamente as APIs do Android sem equivalente no browser:

| API Android | Equivalente no browser | Status |
|---|---|---|
| WifiManager / WifiInfo | Nenhum (sem acesso a Wi-Fi no browser) | Impossível |
| ConnectivityManager / NetworkCallback | Network Information API (limitada) | Degradado |
| LinkProperties / DNS servers | Nenhum | Impossível |
| ForegroundService | Service Worker (limitado, sem rede contínua) | Degradado |
| TelephonyManager | Nenhum | Impossível |
| Ping nativo (ICMP) | Nenhum (apenas HTTP roundtrip) | Impossível |

Complete essa tabela com as APIs específicas da feature sendo comparada.
**Renan não implementa paridade impossível — documenta limitação com clareza.**

---

## Projetos

- Android: `linkaAndroidKotlin/`
- PWA: `linkaSpeedtestPwa/`

---

## Entregue

1. **Como funciona no Android** — comportamento atual, tela e lógica relevante
2. **Como funciona no PWA** — comportamento atual, tela e lógica relevante
3. **Diferenças justificadas por limitação técnica** — o que o navegador não permite, com explicação
4. **Diferenças que parecem erro** — divergências sem justificativa técnica
5. **O que deve ser ajustado** — lista priorizada de correções
6. **Arquivos prováveis** — caminhos reais nos dois projetos
7. **Plano de correção** — passos para alinhar os dois lados

Não implemente sem primeiro apresentar o mapa completo de diferenças.

---

## Tabela delta — resumo incremental

Ao final, entregue a tabela de paridade:

| Feature | Android | PWA | Divergência | Tipo |
|---|---|---|---|---|
| [feature] | [impl. Android] | [impl. PWA] | [diferença] | justificada / erro / impossível |

[PRÓXIMO: Camilo (correção Android) | Renan (correção PWA) — conforme o que foi identificado]
