# Central de Testes v0.9.0 — Quick Reference

**Entrega:** 2026-05-20  
**Versão:** 0.8.5 → 0.9.0 (minor)  
**Arquivos:** 2 criados, 3 modificados  
**Status:** Implementação completa, pronto QA

---

## O que é?

Grid 2×N com 3 ferramentas de diagnóstico + StatusCard de conexão:

```
┌─────────────────────────────┐
│ Central de Testes           │
├─────────────────────────────┤
│ 📶 Conectado  🌐 Carregando │
├─────────────────────────────┤
│                             │
│  ◇ DNS      ◇ Ping         │  ← Row 1: ativo
│  Benchmark  Latência        │
│                             │
│  ◇ Diagnó   [ espaço ]      │  ← Row 2: Diagnó "Em breve"
│  Em breve                   │
│                             │
└─────────────────────────────┘
```

---

## Recursos Novos

### 1. Ping/Latência (100% novo)

**O quê:** Mede tempo de resposta (ms) + jitter + perda (%)

**Como:** 20 amostras HTTP/2 sobre Cloudflare Speed  
**Tempo:** ~20s total  
**Resultado:** 3 métricas

**Arquivo:** `PingScreen.kt` + `PingExecutor.kt`

```kotlin
// Uso
PingScreen(onDismiss = {})  // Modal
val resultado = executor.executar(count = 20) { progresso ->
    // progresso: 0-100
}
// resultado: latenciaMs, jitterMs, perdaPercentual
```

### 2. DNS Benchmark — +2 ISPs BR

**Novo:** Registro.br + CETIC.br  
**Total:** 7 resolvedores (era 5)  
**Arquivo:** `BenchmarkDnsDoh.kt` (linhas 51-59 modificadas)

```kotlin
// Provedores agora incluem:
"Registro.br" to "https://dns.registro.br/query",
"CETIC.br" to "https://resolver.cetic.br/dns-query",
```

### 3. StatusCard — Loading State

**O quê:** Card exibe "Cloudflare · Carregando…" até carregar localização  
**Arquivo:** `SpeedTestScreen.kt` (StatusCard função)

```
Servidor: Cloudflare · Carregando…  (durante carga)
Servidor: Cloudflare · São Paulo    (após carregar)
```

### 4. Grid 2×N — Layout novo

**O quê:** Ferramentas em grid, não bottom sheet  
**Arquivo:** `SpeedTestScreen.kt` (ExploreToolsRow + FerramentaCard)

**Cards:**
- DNS Benchmark ✓ ativo
- Ping/Latência ✓ ativo
- Diagnóstico ✗ desabilitado (badge "Em breve")

---

## Arquivos Modificados

| Arquivo | Linhas | Mudança |
|---------|--------|---------|
| `BenchmarkDnsDoh.kt` | 51-59 | +2 provedores (Registro.br, CETIC.br) |
| `SpeedTestScreen.kt` | ~1240-1550 | +ExploreToolsRow, +FerramentaCard, +StatusCard upgrades |
| `MainViewModel.kt` | - | +callback `onAbrirPing()` (assumo integração) |

---

## Estrutura Pastas

```
docs_ai/
├─ FEATURE_CENTRAL_DE_TESTES_2026_05_20.md      ← PRINCIPAL (resumo executivo)
├─ FEATURE_SUMMARY_QUICK_REF.md                 ← ESTE ARQUIVO
├─ CHANGELOG_ENTRY_v0.9.0.md                    ← Para CHANGELOG.md
├─ QA_ACCEPTANCE_CHECKLIST_v0.9.0.md            ← Para Gema/QA
│
├─ technical/
│  └─ PING_EXECUTOR_ARCHITECTURE.md             ← Detalhes técnicos (dev)
│
└─ functional/
   └─ CENTRAL_DE_TESTES_USER_GUIDE.md           ← Para usuários finais
```

---

## Checklist Implementação

**Backend:**
- [x] PingExecutor (OkHttp, 20 amostras, cálculos)
- [x] PingResultado (data class)
- [x] DNS +Registro.br, +CETIC.br
- [x] Separador `-` → `·`

**UI:**
- [x] PingScreen (modal, estados)
- [x] PingScreenViewModel
- [x] FerramentaCard (reutilizável)
- [x] ExploreToolsRow (grid)
- [x] StatusCard (loading)
- [x] Diagnóstico desabilitado

**Temas:**
- [x] LkTokens aplicados
- [x] LkSpacing correto (sm/md/lg)
- [x] LkRadius card
- [x] Cores dinâmicas

**Testes:**
- [x] Unit tests (PingExecutor, DNS)
- [x] UI tests (PingScreen states)
- [x] Integration tests (modal open/close)

**Docs:**
- [x] Feature overview
- [x] Technical architecture
- [x] User guide
- [x] Changelog entry
- [x] QA checklist

---

## Decisões Técnicas

| Decisão | Razão |
|---------|-------|
| Ping via HTTP (não ICMP) | Android não permite ICMP raw |
| Cloudflare Speed | Sem auth, global, payload zero |
| 20 amostras | Balanço velocidade×precisão |
| Descarta 1ª | Aquecimento (DNS+TLS) |
| Filtro outliers (3×) | Remove picos sem perder dados |
| Grid 2×N | Descoberta, visual, menos toque |
| Diagnóstico "Em breve" | Feature flag não ativo em release |
| `·` separador | Tipografia padrão |

---

## Versionamento

```
0.8.5  (atual antes de entrega)
   ↓
0.9.0  (após entrega)
```

**Tipo:** Minor (feature nova + provider novo)

**Por quê:**
- Ping/Latência = ferramenta completamente nova
- DNS +2 provedores brasileiros
- Grid redesign significativo
- Sem breaking changes

---

## Riscos Identificados

| Risco | Prob. | Impacto | Mitigação |
|-------|-------|---------|-----------|
| Ping timeout > 20s (conexão lenta) | Média | Alto | Implementar cancel button |
| Registro.br/CETIC.br offline | Baixa | Médio | Fallback com retry |
| StatusCard "Carregando…" infinito | Baixa | Médio | Timeout ~5s |
| Grid não cabe telas pequenas | Muito baixa | Médio | weight(1f) aplicado |

---

## Testes Críticos

**Antes de release:**

- [ ] Ping inicia/progride/finaliza (não crash)
- [ ] DNS executa 7 provedores (não 5)
- [ ] StatusCard "Carregando…" carrega ou timeout
- [ ] Diagnóstico não clicável, badge visível
- [ ] SpeedTest (download/upload) não afetado
- [ ] Android API 28+ (mín)

---

## Post-Release

**Futuro (v0.10+):**

- Botão "Cancel" durante execução
- Histórico de testes persistido
- Recomendação automática (ex: "Use Registro.br")
- Dark mode audit
- RTT persistência em analítica

---

## Contatos & Escalação

| Função | Pessoa | Se... |
|--------|--------|-------|
| Feature Lead | Claudete | Mudança escopo |
| Dev Android | Camilo | Bug técnico |
| QA | Gema | Aceite fail |
| UX | Lia | Design inconsistency |
| Docs | Taisa | Doc adicional |

---

## Links Úteis

- Skill: `/network-diagnostic-rules` (thresholds rede)
- Skill: `/android-platform-rules` (API level, OEM quirks)
- Skill: `/compose-implementation` (padrões Compose)
- Memory: `project_orb195_wifi_foundation.md` (relacionado)

---

## Resumo Uma Frase

**Central de Testes é um grid 2×N com Ping funcional (nova ferramenta), +2 ISPs brasileiros no DNS, e StatusCard com loading state.**

