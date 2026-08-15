---
title: "ADR-015 — SignallQ é exclusivo Android e Webapp (signallq.com)"
description: "Declara Android + Webapp como as únicas plataformas do SignallQ (consumer, Pro, Nethal e futuros produtos da marca). Fecha permanentemente iOS/macOS/desktop/wearable/embedded."
type: "adr"
status: "ativo"
owner: "Luiz (CEO)"
last_updated: "2026-08-15"
version: "1.0.0"
---

# ADR-015 — SignallQ é exclusivo Android e Webapp (signallq.com)

> **⚠️ CONSOLIDADO em [ADR-016](ADR-016-portfolio-buildea.md) em 2026-08-15.**
> A decisão de plataforma (Android + Web para o SignallQ) permanece — ADR-016
> consolida com a decisão simétrica do Linka (Apple exclusivo), a política de
> descontinuados (Pro/ISP/Nethal) e a squad de 3 agentes num único documento de
> portfólio. Consultar ADR-016 como fonte canônica.

- **Status:** Superseded (2026-08-15) — originalmente Aceito em 2026-08-15 (mesmo dia)
- **Data:** 2026-08-15
- **Autor:** Luiz (CEO)
- **Escopo:** toda a marca SignallQ — Consumer, Pro, Nethal e qualquer futuro produto que carregue a marca.
- **Reforça:** [ADR-007](ADR-007-ios-scaffolding-sem-agente.md) (já superseded por ADR-014). A decisão de "iOS adiado" passa a ser "iOS fora, permanentemente" no nível da marca.

## Contexto

O portfólio SignallQ acumulou hipóteses de plataforma implícitas ao longo do tempo:

- **Consumer** Android + PWA (`signallq-web` respondendo por `signallq.com`).
- **Pro** Android (13 módulos Gradle em `:pro:*`, hoje `on hold` desde 2026-08-06).
- **iOS** — scaffolding criado em 2026-06-24, descontinuado em 2026-07-04 ([ADR-007](ADR-007-ios-scaffolding-sem-agente.md)), mas o ADR original deixava a porta aberta para retomada ("Quando criar o agente iOS").
- **Nethal** — mencionado em [DECISAO_CONSOLIDACAO_SQUAD_7ALABS_2026-07-23](DECISAO_CONSOLIDACAO_SQUAD_7ALABS_2026-07-23.md) como "segundo produto possível" da marca, sem plataforma definida.
- Nunca houve declaração explícita fechando desktop, macOS, Windows, wearable, smart TV ou embedded.

Sem essa declaração, cada retomada de discussão ("e se fizermos o iOS?", "e se colocarmos no Apple TV?") reabre esforço e planejamento em cima de algo que já foi decidido implicitamente. Documentar fecha a porta.

## Decisão

**A marca SignallQ opera exclusivamente em duas plataformas:**

1. **Android** — app nativo Kotlin/Compose, distribuído via Play Store.
2. **Webapp** — servida em `signallq.com`, hoje implementada pelo repositório [`signallq-web`](https://github.com/buildea-labs/signallq-web) como PWA.

Aplica-se a **todos os produtos** que carregam a marca SignallQ, presentes e futuros:

| Produto | Android | Webapp | Outros |
|---|---|---|---|
| SignallQ Consumer | ✅ (este repo, `android/`) | ✅ (`signallq-web`) | ❌ |
| SignallQ Pro | ✅ (este repo, `android/pro/*` — hoje on hold) | ✅ quando retomado | ❌ |
| SignallQ Nethal | ✅ | ✅ | ❌ |
| Qualquer futuro produto SignallQ | ✅ ou ✅ | ✅ ou ✅ | ❌ |

**Ficam permanentemente fora:**

- iOS (iPhone, iPad).
- macOS, Windows, Linux nativo.
- watchOS, Wear OS, qualquer wearable.
- tvOS, Android TV, smart TV.
- Embedded (router firmware, ONT, IoT).
- Extensões de navegador, quiosque, kiosk mode.

## Consequências

### Imediatas (este ADR)

- [ADR-007](ADR-007-ios-scaffolding-sem-agente.md) passa de "adiado" para "fora do escopo permanente". Nota adicionada no header do ADR-007.
- `AGENTS.md` deste repo passa a referenciar ADR-015 na seção de escopo.
- `INDICE.md` atualizado: contagem de ADRs 14 → 15, próximo número livre 015 → 016.

### Implicação para outros repos (follow-up, não bloqueador desta ADR)

- **`signallq-web`** — AGENTS.md desse repo deve referenciar ADR-015 e declarar "Webapp exclusivo do SignallQ, servindo `signallq.com`". Follow-up numa PR dedicada nesse repo.
- **`ai-governance/`** — se o Luiz quiser políticas de plataforma centralizadas, criar `ai-governance/policies/product-platforms.md` linkando este ADR. Opcional.
- **Repos hipotéticos que não devem ser criados**: nada de `signallq-ios`, `signallq-mac`, `signallq-wearable`, `signallq-embedded`. Se aparecerem, checar contra ADR-015 antes de criar.

### Implicação para specs do Pro (on hold)

- Nada muda na congelação do Pro em `docs_ai/pro-onhold/` — permanece congelado como está.
- Quando o Pro for descongelado, a decisão de plataforma já está feita: Android + Webapp. Sem reabrir esse ponto.

### Implicação para o SignallQ Nethal

- Se Nethal virar produto ativo, nasce Android + Webapp. Não abrir esforço de descoberta em outras plataformas.

### O que essa decisão NÃO fecha

- Stack de implementação do Webapp: continua com o repo `signallq-web` (hoje PWA). Se um dia migrar de Next.js para outra coisa, a decisão de plataforma está separada da decisão de stack — este ADR não trava a stack, só o produto.
- Distribuição do Android: Play Store hoje. F-Droid, Aptoide, Amazon Appstore, sideload — decisões separadas de distribuição, não de plataforma.
- Backend/Workers Cloudflare — infraestrutura, não plataforma cliente. Não afetado.

## Reabertura

Este ADR só é reaberto por decisão explícita do Luiz num novo ADR (ADR-NNN) que o superseda. Sem novo ADR, discussão de plataforma nova é descartada de plano, e o proponente é orientado a apresentar business case direto ao Luiz antes de qualquer trabalho técnico.
