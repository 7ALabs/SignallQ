> **Consolidado em 2026-07-23.** Este arquivo funde 7 documentos arquivados que descreviam a
> mesma entrega (Central de Testes v0.9.0, 2026-05-20), sem informação adicional entre si além de
> reformatação por público-alvo: `FEATURE_CENTRAL_DE_TESTES_2026_05_20.md` (base — mais completo),
> `FEATURE_SUMMARY_QUICK_REF.md`, `CHANGELOG_ENTRY_v0.9.0.md`, `QA_ACCEPTANCE_CHECKLIST_v0.9.0.md`,
> `INDEX_v0.9.0.md`, `DELIVERY_SUMMARY.txt`, `HANDOFF_RELEASE.md`. Os originais foram removidos do
> archive; o conteúdo funcional/técnico foi preservado abaixo, com o processo de handoff/QA
> resumido no Anexo (a versão linha-a-linha dos ~200 checkboxes de QA não foi preservada — é
> execução operacional, não decisão ou registro técnico). Preservado só como referência histórica —
> não reflete o estado atual do app (a feature evoluiu; ver `docs_ai/FUNCIONAL.md` § Central de
> Testes para o estado atual).

# Central de Testes: Grid de Ferramentas + Ping/Latência + DNS ISPs BR

**Entrega:** 2026-05-20
**Versão:** 0.8.5 → 0.9.0 (minor bump)
**Status (na época):** Implementação completa, aguardando QA/Product
**Escopo:** Grid 2×N com 3 ferramentas, novas resoluções DNS Brasil (Registro.br, CETIC.br), Ping funcional, Diagnóstico com badge "Em breve", StatusCard loading state.

---

## 1. Resumo Técnico

### Arquivos Criados

| Arquivo | Módulo | Descrição |
|---------|--------|-----------|
| `PingScreen.kt` | `:app` | Composable ModalBottomSheet + ViewModel para tela de Ping/Latência |
| `PingExecutor.kt` | `:featureSpeedtest` | Engine ICMP/HTTP sobre Cloudflare, cálculo de latência/jitter/perda |
| `PingResultado.kt` | `:featureSpeedtest` | Data class com latenciaMs, jitterMs, perdaPercentual, amostras |

### Arquivos Modificados

| Arquivo | Módulo | Mudanças |
|---------|--------|----------|
| `BenchmarkDnsDoh.kt` | `:featureDns` | +2 provedores: Registro.br, CETIC.br; separador de `-` para `·` |
| `SpeedTestScreen.kt` | `:app` | ExploreToolsRow (grid 2×N), FerramentaCard, StatusCard com loading state |
| `MainViewModel.kt` | `:app` | Acesso a `PingScreenViewModel`, callbacks para `onAbrirPing()` |

### Dependências Novas

Nenhuma. `PingExecutor` usa OkHttp (já presente).

### Padrão Visual & Design Tokens

```kotlin
// LkTokens (tema aplicado)
val c.bgCard              // Fundo do card
val c.border              // Borda 0.5dp ou 1dp
val c.textPrimary         // Título das ferramentas
val c.textSecondary       // Servidor loading state
val c.textTertiary        // Ícones, descrição

// Spacing
LkSpacing.sm              // 8dp (gaps entre cards)
LkSpacing.md              // 12dp (padding interno card)
LkSpacing.lg              // 16dp (padding horizontal seção)

// Radius
LkRadius.card             // 12dp (cards)

// Dimensões
32.dp                     // Ícone card
RoundedCornerShape(12.dp) // Cantos suavizados
```

---

## 2. Resumo Funcional

### Antes (v0.8.4)

```
SpeedTestScreen
├─ [Botão] "Central de Testes"
│  └─ ExploreToolsSheet (bottom sheet lista vertical)
│     ├─ [Ativo]   DNS Benchmark
│     ├─ [Ativo]   Diagnóstico Inteligente
│     └─ [Em breve] Ping / Latência  ← sem implementação
│
├─ DNS Benchmark
│  ├─ Cloudflare, Google, Quad9, OpenDNS, AdGuard  ← FALTAVA Registro.br, CETIC.br
│  └─ Separador: host="example.com-" (hífen confuso)
│
└─ Diagnóstico Inteligente
   └─ Chat com IA (gate por FEATURE_DIAGNOSTICO_CHAT)
```

### Depois (v0.8.5)

```
SpeedTestScreen
├─ [Aba] "Explorar ferramentas"
│  │
│  └─ ExploreToolsRow (grid 2×N adaptativo)
│     │
│     ├─ [Row 1] (2 cards lado a lado)
│     │  ├─ [Ativo]  ◇ DNS Benchmark  (ícone Speed + descrição)
│     │  └─ [Ativo]  ◇ Ping / Latência (ícone NetworkCheck + descrição)
│     │
│     └─ [Row 2] (2 cards lado a lado)
│        ├─ [Desabilitado] ◇ Diagnóstico (ícone Psychology + badge "Em breve")
│        └─ [Vazio]  (espaço reservado)
│
├─ StatusCard
│  ├─ [Topo] Ícone + conexão (Wi-Fi/Móvel/Offline)
│  ├─ [Separador]
│  └─ [Servidor] "Cloudflare · Carregando..." (estado loading)
│
├─ PingScreen (modal)
│  ├─ Estado Idle → "Iniciar teste"
│  ├─ Estado Executando(progresso) → barra com progresso (0-100%)
│  ├─ Estado Resultado → latência / jitter / perda
│  └─ Estado Erro → mensagem de erro + tentar novamente
│
├─ DNS Benchmark (com novos ISPs)
│  ├─ Cloudflare, Google, Quad9, OpenDNS, AdGuard, Registro.br ✓ NOVO, CETIC.br ✓ NOVO
│  └─ Separador: "Cloudflare · Carregando..." (ponto meio, não hífen)
│
└─ Diagnóstico Inteligente
   └─ Desabilitado (não confunde mais usuário) — reavivar quando FEATURE_DIAGNOSTICO_CHAT estiver ativo
```

### Fluxo do Usuário (resumo)

- **Acessar:** tela SpeedTest → "Explorar ferramentas" → grid 2×N (4 cards) + StatusCard carregando localização.
- **Ping/Latência:** abrir modal → "Iniciar teste" → progresso 0-100% (~20s) → latência/jitter/perda → fechar (voltar/swipe).
- **DNS Benchmark:** abrir card → 7 resolvedores testados → ranking por tempo.
- **Diagnóstico:** card desabilitado (50% opacidade), badge "Em breve", não clicável.

---

## 3. Guia do Usuário (resumo)

**Ping/Latência** — mede tempo de resposta em 20 amostras HTTP/2 sobre Cloudflare Speed (~20s):

| Métrica | O que é | Bom é… | Péssimo é… |
|---------|---------|--------|-----------|
| **Latência** | Tempo médio (mediana) em ms | < 50 ms | > 200 ms |
| **Jitter** | Variação de latência entre amostras | < 10 ms | > 50 ms |
| **Perda** | Pacotes que não retornaram (%) | 0% | > 5% |

**DNS Benchmark** — passou de 5 para 7 resolvedores com a adição de **Registro.br** (Fapesp, ideal para SP) e **CETIC.br** (resolver público nacional de baixa latência).

**Diagnóstico Inteligente** — card visível mas apagado (50%) com badge "Em breve"; ativa quando `FEATURE_DIAGNOSTICO_CHAT = true` em release.

---

## 4. Changelog Entry (como copiado para `CHANGELOG.md` na época)

### Added

- **Central de Testes — Grid 2×N:** 3 ferramentas (DNS Benchmark, Ping/Latência, Diagnóstico) em grid adaptativo 2 colunas × N linhas. Cards com ícone, título, descrição, state visual (ativo/desabilitado) + badge "Em breve".
- **Ping/Latência — Ferramenta nova:** 20 amostras ICMP sobre Cloudflare Speed (http/2, 4s timeout). Calcula latência (mediana, ms), jitter (std dev, ms) e perda de pacotes (%). Estados: Idle → Executando → Resultado → Erro.
- **DNS Benchmark — ISPs brasileiros:** +Registro.br, +CETIC.br (7 resolvedores, era 5).
- **StatusCard — Loading state:** "Cloudflare · Carregando…" enquanto `localizacaoServidor` é null.

### Fixed

- **Diagnóstico Inteligente — Desabilitado por padrão:** badge "Em breve", 50% opacidade, não clicável.

### Changed

- **DNS Benchmark — Separador:** `-` (hífen) → `·` (ponto médio).
- **ExploreToolsSheet → ExploreToolsRow:** bottom sheet vertical substituída por grid 2×N.

### Technical

- `PingExecutor` (novo): OkHttp2 + HTTP/1.1 fallback, 1ª amostra descartada (aquecimento), mediana + filtro de outliers (≤3× mediana), jitter = std dev, perda = timeouts/amostras.
- `FerramentaCard` (novo): padrão reutilizável (icon, title, desc, badge, state), opacity/graphicsLayer para disabled.
- Temas: `LkTokens.bgCard/.border/.textPrimary/.textSecondary/.textTertiary`, `LkSpacing.sm/.md/.lg`, `LkRadius.card`.

**Breaking changes:** nenhuma — todos callbacks opcionais com defaults.

---

## 5. Versão Sugerida (SemVer)

**De 0.8.5 para 0.9.0** (minor bump). Justificativa: Ping/Latência é ferramenta completamente nova; DNS Benchmark ganha 2 provedores brasileiros; grid é redesign visual significativo; nenhuma breaking change.

---

## 6. Arquivos de Implementação

```
signallq-android-kotlin/
├─ app/src/main/kotlin/io/signallq/app/kotlin/ui/screen/
│  ├─ PingScreen.kt          NOVO
│  ├─ SpeedTestScreen.kt     MODIFICADO (ExploreToolsRow, FerramentaCard)
│  └─ MainViewModel.kt       MODIFICADO (callback onAbrirPing)
├─ featureSpeedtest/.../feature/speedtest/
│  ├─ PingExecutor.kt        NOVO
│  └─ PingResultado.kt       NOVO
└─ featureDns/.../feature/dns/
   └─ BenchmarkDnsDoh.kt     MODIFICADO (+Registro.br, +CETIC.br)
```

---

## 7. Testes de Aceitação (critérios principais)

- Ping: modal abre, estado Idle → Executando (progresso 0-100%) → Resultado (3 métricas) ou Erro (com retry).
- DNS: 7 resolvedores testados, ranking por tempo, separador `·`.
- Grid: 4 cards visíveis (DNS/Ping ativos, Diagnóstico desabilitado + badge, 1 vazio).
- StatusCard: "Carregando…" → localização real após ~5s.
- Regressão: SpeedTest download/upload/bufferbloat não afetados; HomeScreen → Central de testes funciona; sem crash com permissão de internet negada.

---

## 8. Riscos & Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|--------|-----------|
| Ping timeout > 20s em conexão lenta | Média | Alto | Timeout OkHttp + botão cancel (planejado) |
| Registro.br/CETIC.br offline/lento | Baixa | Médio | Fallback para próximo resolver, erro com retry |
| Grid não cabe em telas pequenas (< 4") | Baixa | Médio | `weight(1f)` + layout responsivo |
| StatusCard "Carregando..." nunca preenche | Baixa | Médio | Timeout implícito ~5s, fallback |

---

## 9. Documentação Relacionada (histórica)

- `ANDROID_FUNCIONAL.md` § Telas → Central de Testes, Ping/Latência
- `ANDROID_TECNICO.md` § `:featureSpeedtest`, `:featureDns` → PingExecutor, BenchmarkDnsDoh
- `technical/PING_EXECUTOR_ARCHITECTURE.md` (arquitetura detalhada do Ping — se ainda existir)
- `functional/CENTRAL_DE_TESTES_USER_GUIDE.md` (guia de usuário; ver também a versão posterior v0.23.0 em `docs_ai/_archive/2026-07-16_CENTRAL_DE_TESTES_USER_GUIDE.md`)

---

## Anexo — Processo de QA e Handoff da época (condensado)

Fluxo real seguido para essa entrega (papéis já descontinuados/renomeados desde então — Gema foi
substituída por Rhodolfo em 2026-07-10, Taisa não existe mais como papel dedicado):

1. **Taisa (Docs)** empacotava a documentação e fazia handoff para QA/Product/Dev.
2. **Gema (QA)** executava um checklist de aceite cobrindo: critérios funcionais (Ping, DNS, Grid,
   StatusCard, temas), regressão (SpeedTest, Home, Diagnóstico, DNS anterior), permissões e
   conectividade, acessibilidade (TalkBack/WCAG — contentDescription, contraste ≥ 4.5:1),
   performance (tempo de execução, ANR, memória, bateria), compatibilidade entre dispositivos
   (Galaxy A52, Pixel 6, Moto G9, tablet 10"), build & release (versionCode/versionName, lint,
   ProGuard) e sign-off formal (QA/Product/Dev/Design).
3. **Claudete (Product)** aprovava o versionamento (0.9.0) e dava green light.
4. **Camilo (Dev)** validava build local, testes unitários/UI e code review.
5. Release: atualizar `versionCode`/`versionName`, copiar changelog, build release, smoke test,
   upload Play Store.

Sign-off registrado na época: QA (Gema), Product (Claudete), Dev (Camilo), Design (Lia) — todos
pendentes de assinatura no documento original, sem registro de bloqueador crítico encontrado.
