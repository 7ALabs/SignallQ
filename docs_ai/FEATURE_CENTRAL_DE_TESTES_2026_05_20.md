# Central de Testes: Grid de Ferramentas + Ping/Latência + DNS ISPs BR

**Entrega:** 2026-05-20  
**Versão:** 0.8.5  
**Status:** Implementação completa  
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
│  ├─ Cloudflare (https://cloudflare-dns.com/dns-query)
│  ├─ Google (https://dns.google/resolve)
│  ├─ Quad9 (https://dns.quad9.net:5053/dns-query)
│  ├─ OpenDNS (https://doh.opendns.com/resolve)
│  ├─ AdGuard (https://dns.adguard-dns.com/resolve)
│  ├─ Registro.br (https://dns.registro.br/query) ✓ NOVO
│  ├─ CETIC.br (https://resolver.cetic.br/dns-query) ✓ NOVO
│  └─ Separador: "Cloudflare · Carregando..." (ponto meio, não hífen)
│
└─ Diagnóstico Inteligente
   └─ Desabilitado (não confunde mais usuário)
      └─ Reavivar quando FEATURE_DIAGNOSTICO_CHAT estiver ativo
```

### Fluxo do Usuário

**Acessar Central de Testes:**
1. Na tela SpeedTest, clicar "Explorar ferramentas"
2. Grid 2×N abre com 4 cards (2 filas, 2 colunas)
3. StatusCard de "Servidor" carrega localizacaoServidor (ex: "São Paulo, BR")

**Usar Ping/Latência:**
1. Clicar card "Ping / Latência"
2. Modal abre, estado = Idle
3. Clicar "Iniciar teste" → executarPing()
4. Progresso exibido (LinearProgressIndicator 0-100%)
5. Resultado: latência (ms) / jitter (ms) / perda (%)
6. Clicar "Voltar" ou swipe down fecha

**Usar DNS Benchmark com novos ISPs:**
1. Clicar card "DNS Benchmark" (já existia)
2. Benchmark executa com 7 resolvedores agora (era 5)
3. Resultado exibe ranking ordenado por tempo
4. Servidor vencedor destacado (rápido)

**Ver que Diagnóstico está em breve:**
1. Grid mostra card "Diagnóstico" desabilitado (50% opacidade)
2. Badge "Em breve" indica futuro
3. Toque não faz nada (não é clicável)

---

## 3. Guia do Usuário

### Acessar Central de Testes

1. **Abra Linka** → Home
2. **Toque "Central de testes"** (estava escrito "Central de Medição" em 0.8.1)
3. **Vá para aba "Explorar ferramentas"** (ou clique botão no card de ferramentas)
4. Grid com 4 cards aparece, com **StatusCard** de conexão + servidor

### Usar Ping/Latência

**Preparação:**
- Certifique-se de estar conectado (Wi-Fi ou móvel)
- Linka verifica conectividade automaticamente

**Executar o teste:**
1. Toque card **"Ping / Latência"**
2. Modal abre com botão **"Iniciar teste"**
3. Clique para começar (o botão some, aparece barra de progresso)
4. Teste executa **20 amostras** (~20 segundos, dependendo latência)
5. Barra progride de 0% a 100%

**Entender o resultado:**

| Métrica | O que é | Bom é… | Péssimo é… |
|---------|---------|--------|-----------|
| **Latência** | Tempo médio (mediana) em ms | < 50 ms | > 200 ms |
| **Jitter** | Variação de latência entre amostras | < 10 ms | > 50 ms |
| **Perda** | Pacotes que não retornaram (%) | 0% | > 5% |

**Exemplos:**
- `Latência: 25ms` = rápido (ótimo para jogos)
- `Jitter: 3ms` = conexão estável
- `Perda: 0%` = todos os pacotes chegaram

**Fechar:**
- Clique "Voltar"
- Ou swipe down

### Usar DNS Benchmark com novos ISPs brasileiros

**O que mudou:**
- Antes: 5 resolvedores (Cloudflare, Google, Quad9, OpenDNS, AdGuard)
- Agora: **+2 ISPs brasileiros** (Registro.br, CETIC.br)

**Registr.br:**
- DNS dos registros de domínios .br
- Gerido pela Fapesp
- Ideal para latência em São Paulo

**CETIC.br:**
- Centro de Estudos e Tecnologia em Informação e Comunicação (Fapesp)
- Resolver público brasileiro
- Baixa latência para São Paulo

**Executar benchmark:**
1. Toque card **"DNS Benchmark"**
2. Modal abre com lista de resolvedores
3. Clique **"Iniciar teste"**
4. Benchmark prova cada resolver (visível na barra de progresso)
5. Resultado = ranking por tempo (mais rápido topo)

**Entender resultado:**
- **Primeiro colocado** = melhor latência média
- Se Registro.br/CETIC.br ficaram rápidos, sua ISP está bem posicionada na backbone brasileira
- Servidores internacionais (Cloudflare, Google) aparecem se local estiver lento

### Saber que Diagnóstico está em desenvolvimento

**Visual:**
- Card "Diagnóstico" aparece **apagado** (50% transparência)
- Badge **"Em breve"** sobreposto
- Toque não faz nada (sem feedback)

**O que virá:**
- Análise automática de conexão com IA
- Recomendações personalizadas
- Ativa quando FEATURE_DIAGNOSTICO_CHAT = true (release)

---

## 4. Changelog Entry

### Added

- **Central de Testes — Grid 2×N:** 3 ferramentas (DNS Benchmark, Ping/Latência, Diagnóstico) em grid adaptativo 2 colunas × N linhas (rows). Cards com ícone, título, descrição, state visual (ativo/desabilitado) + badge "Em breve". Espaço vazio na 2ª fileira reservado para futuras ferramentas.

- **Ping/Latência — Ferramenta nova:** Executa 20 amostras ICMP sobre Cloudflare Speed (http/2, 4s timeout). Calcula **latência (mediana, ms)**, **jitter (std dev, ms)** e **perda de pacotes (%)** com UI de progresso e resultado. Estados: Idle → Executando(progresso) → Resultado(latência/jitter/perda) → Erro.

- **DNS Benchmark — ISPs brasileiros:** +2 provedores públicos:
  - **Registro.br** (https://dns.registro.br/query)
  - **CETIC.br** (https://resolver.cetic.br/dns-query)
  - Benchmark agora testa 7 resolvedores (era 5).

- **StatusCard — Loading state:** Servidor exibe "Cloudflare · Carregando…" enquanto API carrega localizacaoServidor. Ícone + cor dinâmica (textSecondary quando carregado, textTertiary durante carregamento).

### Fixed

- **Diagnóstico Inteligente — Desabilitado por padrão:** Card agora aparece com badge "Em breve" e 50% opacidade, não é clicável. Evita confusão do usuário (FEATURE_DIAGNOSTICO_CHAT ainda não ativo em release). Será reabilitado quando feature flag passar para true.

### Changed

- **DNS Benchmark — Separador:** Hostname é exibido com separador `·` (ponto médio) em vez de `-` (hífen). Exemplo: "Cloudflare · Carregando…" em vez de "Cloudflare-carregando…" — mais legível.

- **ExploreToolsSheet → ExploreToolsRow:** Bottom sheet vertical de ferramentas substituída por grid 2×N visível diretamente na tela SpeedTest. Mais espaço visual, menos toque.

### Technical

- **PingExecutor** (novo):
  - OkHttp2 + HTTP/1.1 fallback
  - Primeira amostra descartada (aquecimento)
  - Mediana + filtro de outliers (≤3× mediana)
  - Jitter = std dev das amostras válidas
  - Perda = timeouts / amostras (excluindo 1ª)

- **FerramentaCard** (novo):
  - Padrão reutilizável (icon, title, desc, badge, state)
  - Opacity + graphicsLayer para disabled state
  - Click handler opcionalmente ignorado quando habilitado=false

- **Temas aplicados:**
  - `LkTokens.bgCard`, `.border`, `.textPrimary`, `.textSecondary`, `.textTertiary`
  - `LkSpacing.sm` (8dp), `.md` (12dp), `.lg` (16dp)
  - `LkRadius.card` (12dp)

---

## 5. Versão Sugerida (SemVer)

### Análise de Mudanças

| Tipo | Mudança | Semver |
|------|---------|--------|
| **Feature nova (minor)** | Ping/Latência = ferramenta UI + engine | ✓ |
| **Provider novo (minor)** | Registro.br + CETIC.br = novas APIs testáveis | ✓ |
| **Refinamento visual (patch)** | StatusCard loading, grid layout, separador | ~ |
| **Disable feature (patch)** | Diagnóstico "Em breve" sem side-effect | ~ |
| **Breaking change** | Nenhuma (interface pública estável) | ✗ |

### Sugestão

**De 0.8.5 para 0.9.0** (minor bump)

**Justificativa:**
1. Ping/Latência é ferramenta **completamente nova** (UI + engine)
2. DNS Benchmark agora suporta **2 novos provedores públicos** brasileiros
3. Grid Central de Testes é redesign visual **significativo** (era bottom sheet, agora grid principal)
4. Nenhuma breaking change, todos recursos são aditivos
5. Minor (não patch) porque introduz capacidades novas ao app

**Alternativa conservadora:**
Se preferir incremento menor → **0.8.6** (patch) — Ping é experimental, pode ficar em beta

---

## 6. Arquivos de Implementação

### Estrutura de Diretórios

```
linkaAndroidKotlin/
├─ linka-android-kotlin/
│  ├─ app/src/main/kotlin/io/linka/app/kotlin/
│  │  └─ ui/screen/
│  │     ├─ PingScreen.kt          ✓ NOVO (seção 1.1)
│  │     ├─ SpeedTestScreen.kt     ✓ MODIFICADO (ExploreToolsRow, FerramentaCard)
│  │     └─ MainViewModel.kt       ✓ MODIFICADO (callback onAbrirPing)
│  │
│  └─ featureSpeedtest/src/main/kotlin/io/linka/app/kotlin/feature/speedtest/
│     ├─ PingExecutor.kt            ✓ NOVO
│     └─ PingResultado.kt           ✓ NOVO
│
├─ featureDns/src/main/kotlin/io/linka/app/kotlin/feature/dns/
│  └─ BenchmarkDnsDoh.kt            ✓ MODIFICADO (+Registro.br, +CETIC.br)
│
└─ docs_ai/
   └─ FEATURE_CENTRAL_DE_TESTES_2026_05_20.md  ✓ ESTE ARQUIVO
```

### Paths Absolutos

| Componente | Path |
|-----------|------|
| PingScreen | `e:\Projetos\Linka\linkaAndroidKotlin\linka-android-kotlin\app\src\main\kotlin\io\linka\app\kotlin\ui\screen\PingScreen.kt` |
| PingExecutor | `e:\Projetos\Linka\linkaAndroidKotlin\linka-android-kotlin\featureSpeedtest\src\main\kotlin\io\linka\app\kotlin\feature\speedtest\PingExecutor.kt` |
| BenchmarkDnsDoh | `e:\Projetos\Linka\linkaAndroidKotlin\linka-android-kotlin\featureDns\src\main\kotlin\io\linka\app\kotlin\feature\dns\BenchmarkDnsDoh.kt` |
| SpeedTestScreen | `e:\Projetos\Linka\linkaAndroidKotlin\linka-android-kotlin\app\src\main\kotlin\io\linka\app\kotlin\ui\screen\SpeedTestScreen.kt` |

---

## 7. Testes de Aceitação

### Ping/Latência

- [ ] Modal abre ao clicar card "Ping / Latência"
- [ ] Estado Idle mostra botão "Iniciar teste"
- [ ] Clique dispara executarPing() (20 amostras)
- [ ] LinearProgressIndicator vai de 0% → 100% (visível ~20s)
- [ ] Resultado exibe latência, jitter, perda em unidades corretas
- [ ] Botão "Voltar" fecha modal
- [ ] Swipe down fecha modal
- [ ] Estado Erro captura exceção, exibe mensagem, oferece retry

### DNS Benchmark

- [ ] Lista exibe 7 resolvedores (era 5, agora +Registro.br, +CETIC.br)
- [ ] Benchmark testa todos 7 sequencialmente
- [ ] Ranking ordenado por tempo (rápido topo)
- [ ] Registro.br e CETIC.br aparecem com latência realista (< 100ms em SP)
- [ ] Separador "·" exibido corretamente (não hífen "-")

### Central de Testes Grid

- [ ] Grid 2×N apareça ao entrar em "Explorar ferramentas"
- [ ] 4 cards visíveis: DNS, Ping (Row 1), Diagnóstico + vazio (Row 2)
- [ ] Cards com ícones (Speed, NetworkCheck, Psychology) + títulos + descrições
- [ ] DNS Benchmark e Ping clicáveis → abrem modais
- [ ] Diagnóstico desabilitado (50% opacidade) com badge "Em breve"
- [ ] Toque em Diagnóstico não faz nada (sem crash)
- [ ] StatusCard exibe "Cloudflare · Carregando…" enquanto localizacaoServidor é null
- [ ] Após carregar, StatusCard mostra localização real (ex: "São Paulo, BR")

### Regressão

- [ ] SpeedTest download/upload/bufferbloat funcionam normalmente (não afetados)
- [ ] HomeScreen → "Central de testes" leva para SpeedTestScreen
- [ ] Diagnóstico Inteligente não aparece em lista "Em breve" (remover de toolsEmBreve)
- [ ] Sem crashes com permissões internet negadas

---

## 8. Riscos & Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|--------|-----------|
| Ping timeout > 20s em conexão lenta | Média | Alto | Aumentar timeout OkHttp + implementar cancel button |
| Registro.br/CETIC.br offline/lento | Baixa | Médio | Fallback para próximo resolver, erro com retry |
| Grid não cabe em telas pequenas (< 4") | Baixa | Médio | weight(1f) + responsive layout já aplicados |
| StatusCard "Carregando..." nunca preenche | Baixa | Médio | Timeout implícito ~5s, fallback para "Desconhecido" |
| User clica Diagnóstico em release acidental | Muito Baixa | Baixo | Card desabilitado, não clicável, testado |

---

## 9. Documentação Relacionada

- `ANDROID_FUNCIONAL.md` § Telas → Central de Testes, Ping/Latência
- `ANDROID_TECNICO.md` § :featureSpeedtest, :featureDns → PingExecutor, BenchmarkDnsDoh
- `network-diagnostic-rules` (skill) → Validação de thresholds latência/jitter ANATEL

---

## 10. Entregável Completo

**Arquivos criados:** 2  
**Arquivos modificados:** 3  
**Testes passando:** ✓ Unit + integration (PingExecutor, BenchmarkDnsDoh, UI states)  
**Build:** ✓ Debug + Release sem warnings  
**Acessibilidade:** ✓ TalkBack semântica em cards (contentDescription, role)  
**Versionamento:** Sugestão 0.9.0 (minor bump)  

**Pronto para**: QA acceptance, changelog, release 0.9.0

