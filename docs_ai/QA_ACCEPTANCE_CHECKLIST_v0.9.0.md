# QA Acceptance Checklist — Central de Testes v0.9.0

**Entrega:** 2026-05-20  
**Responsável QA:** Gema  
**Status:** Pronto para validação  

---

## Critérios de Aceite — Feature Funcional

### Ping/Latência

- [ ] Modal abre ao clicar card "Ping / Latência"
- [ ] Estado Idle mostra botão "Iniciar teste" centralizado
- [ ] Botão "Iniciar" dispara `executarPing()` (inicia 20 amostras)
- [ ] Botão desaparece ao clicar (início da execução)
- [ ] LinearProgressIndicator visível (0% → 100%)
- [ ] Progresso avança suavemente a cada amostra
- [ ] Tempo total ~20s em rede boa (latência <50ms)
- [ ] Resultado exibe 3 linhas:
  - `Latência: XXX ms`
  - `Jitter: XX ms`
  - `Perda: X%`
- [ ] Valores são numbers (não "N/A", não "-")
- [ ] Unidades corretas (ms, ms, %)
- [ ] Botão "Voltar" / "Fechar" funciona
- [ ] Swipe down fecha modal
- [ ] Modal reabre, estado volta para Idle (resetado)

### Estado Erro

- [ ] Sem internet → erro com mensagem "Sem conexão"
- [ ] Timeout → erro com mensagem "Timeout ao executar teste"
- [ ] Botão "Tentar novamente" dispara novo teste
- [ ] Mensagem erro visível (não truncada)

### DNS Benchmark

- [ ] Lista exibe **7 resolvedores** (não 5)
- [ ] Ordem: Cloudflare, Google, Quad9, OpenDNS, AdGuard, Registro.br, CETIC.br
- [ ] Nomes são exatos (maiúsculas, sem typos)
- [ ] Benchmark executa todos 7 em sequência
- [ ] Cada resolver mostra tempo (ms) no resultado
- [ ] Resultado = ranking por tempo (mais rápido topo)
- [ ] Registro.br/CETIC.br têm latência realista (< 100ms em SP)
- [ ] Separador é `·` (ponto médio), não `-` (hífen)
- [ ] Exemplo: "Cloudflare · 23 ms" ✓ (não "Cloudflare-23ms")

### Central de Testes — Grid

- [ ] Grid 2×N visível ao entrar em SpeedTest/"Explorar ferramentas"
- [ ] **Row 1** (2 cards):
  - [ ] "DNS Benchmark" (ícone Speed)
  - [ ] "Ping / Latência" (ícone NetworkCheck)
- [ ] **Row 2** (2 cards):
  - [ ] "Diagnóstico Inteligente" (ícone Psychology + badge "Em breve")
  - [ ] 1 card vazio (espaço reservado)
- [ ] Todos cards têm descrição (labelSmall)
- [ ] Cards clicáveis abrem modais corretos
- [ ] Diagnóstico NÃO é clicável (50% opacidade, sem efeito)
- [ ] Badge "Em breve" visível em Diagnóstico
- [ ] Spacing entre cards = LkSpacing.sm (8dp)
- [ ] Padding interno = LkSpacing.md (12dp)
- [ ] Radius = LkRadius.card (12dp)

### StatusCard

- [ ] StatusCard visível acima do grid
- [ ] 2 linhas:
  - [ ] "Sua Rede: [Wi-Fi/Móvel/Offline] Conectado/Desconectado"
  - [ ] "Servidor: Cloudflare · [Localização ou "Carregando..."]"
- [ ] Ícones dinâmicos (Wi-Fi/Móvel/Offline)
- [ ] Cores dinâmicas (verde=conectado, vermelho=desconectado)
- [ ] Loading state: "Cloudflare · Carregando…" (primeira carga)
- [ ] Após ~5s: localização preenchida (ex: "Cloudflare · São Paulo, BR")
- [ ] Se nunca carregar: fallback para "Cloudflare ·" (não crash)

### Visual & Temas

- [ ] Cards usam `LkTokens.bgCard` (fundo)
- [ ] Borda = `LkTokens.border` (0.5-1dp)
- [ ] Texto título = `LkTokens.textPrimary`
- [ ] Texto descrição = `LkTokens.textSecondary` / `.textTertiary`
- [ ] Ícones = 32dp, centralizados
- [ ] Fonte títulos = bodyMedium (FontWeight.W600)
- [ ] Fonte descrição = labelSmall
- [ ] Contraste de cor atende WCAG AA
- [ ] Layout adapta em telas pequenas (não overflow)

---

## Regressão — Features Existentes

### SpeedTest (download/upload/bufferbloat)

- [ ] SpeedTest inicia normalmente
- [ ] Download executa sem erros
- [ ] Upload executa sem erros
- [ ] Bufferbloat não aparece em resultado final (ou aparece correto)
- [ ] Resultado exibe todas 3 métricas (velocidade, bufferbloat, grade)
- [ ] Botão "Testar novamente" funciona

### Home → Central de Testes

- [ ] Botão "Central de testes" leva para SpeedTestScreen
- [ ] Deep link direto funciona (se aplicável)

### Diagnóstico Inteligente

- [ ] Card aparece, não é clicável
- [ ] Badge "Em breve" visível
- [ ] 50% opacidade confirmada
- [ ] Nenhum side-effect ao tentar clicar (sem crash, sem log de erro)
- [ ] Feature flag FEATURE_DIAGNOSTICO_CHAT = false em release

### DNS Screen Anterior

- [ ] DNS Benchmark modal abre
- [ ] Título correto ("DNS Benchmark")
- [ ] Feature flag FEATURE_DNS_SCREEN = true

---

## Permissões & Conectividade

- [ ] Sem internet (WiFi desligado) → teste falha gracefully com erro
- [ ] Com internet WiFi → teste executa, resultado válido
- [ ] Com internet Móvel → teste executa, resultado válido
- [ ] Conexão cai durante teste → erro capturado, não crash
- [ ] Permissão INTERNET verificada (AndroidManifest)
- [ ] No permission dialogs (não novo permissões adicionadas)

---

## Acessibilidade (TalkBack / WCAG)

- [ ] Cada card tem `contentDescription` (ex: "DNS Benchmark, Mede servidores DNS")
- [ ] Botões têm label legível (ex: "Iniciar teste")
- [ ] ícones têm `contentDescription = null` (skip, já descrito em Text)
- [ ] Estados anunciados: "Ping ocioso", "Ping em progresso", "Resultado"
- [ ] Progresso anunciado (ex: "50%")
- [ ] Cores não são único indicador (texto também muda)
- [ ] Contrast ratio ≥ 4.5:1 para texto normal, ≥ 3:1 para grande

---

## Performance

- [ ] Ping executa ~20s (tolerance: 18-25s)
- [ ] DNS executa ~15s (7 provedores, teste rápido cada um)
- [ ] Modal abre < 300ms (sem lag)
- [ ] Grid renderiza < 500ms (não stutter)
- [ ] Progresso atualiza suavemente (60 fps)
- [ ] Nenhuma ANR (Application Not Responding)
- [ ] Bateria: teste consome < 50mAh (rede é baixa energia)
- [ ] Memória: sem leak (verificar com Memory Profiler)

---

## Compatibilidade & Dispositivos

| Dispositivo | API | Status |
|------------|-----|--------|
| Samsung Galaxy A52 | API 31 | [ ] Testado |
| Pixel 6 | API 33 | [ ] Testado |
| Moto G9 | API 30 | [ ] Testado |
| iPhone (via PWA) | iOS 15+ | N/A (Android-only feature) |
| Tablet (10") | API 32 | [ ] Testado |

- [ ] Sem crashes em nenhum dispositivo
- [ ] Layout adapta (não overflow em pequenas telas)
- [ ] Rotação device (portrait ↔ landscape) não perde estado

---

## Build & Release

- [ ] Build debug apk sem erros
- [ ] Build release apk sem erros
- [ ] Play Store lint OK (nenhuma warning crítica)
- [ ] versionCode incrementado (24 → 25)
- [ ] versionName = "0.9.0" (CHANGELOG atualizado)
- [ ] AndroidManifest.xml sem conflitos
- [ ] ProGuard rules aplicadas (se existente)
- [ ] Gradle build < 3min
- [ ] Sem unused imports
- [ ] Sem deprecated API (OkHttp, AndroidX, etc.)

---

## Integração com Paperclip

- [ ] Task marcada como `done` (quando API Paperclip for acionada)
- [ ] Relacionamento com tasks anteriores documentado
- [ ] Nenhuma task bloqueada por esta entrega

---

## Documentação

- [ ] CHANGELOG.md atualizado (v0.9.0)
- [ ] User guide criado (functional/)
- [ ] Technical architecture documentado (technical/)
- [ ] Inline comments presentes em código crítico
- [ ] Javadoc em classes públicas
- [ ] README.md atualizado (se aplicável)

---

## Decisões de Design Documentadas

- [ ] Por que Cloudflare para Ping? (sem autenticação, geográfico, payload zero)
- [ ] Por que 20 amostras? (balanço tempo×precisão)
- [ ] Por que descartar 1ª amostra? (warmup: DNS+TLS)
- [ ] Por que filtro 3× mediana? (remove outliers sem perder validade)
- [ ] Por que grid 2×N? (descoberta, visual, menos toque)
- [ ] Por que Diagnóstico "Em breve"? (FEATURE_DIAGNOSTICO_CHAT = false)

---

## Sign-Off

| Papel | Nome | Data | Assinatura |
|------|------|------|-----------|
| QA Lead | Gema | 2026-05-20 | [ ] Aprovado |
| Product Manager | Claudete | 2026-05-20 | [ ] Validado |
| Developer (Android) | Camilo | 2026-05-20 | [ ] Implementado |
| Designer | Lia | 2026-05-20 | [ ] UX validado |

---

## Notas de QA

**Encontrados:**
- (Listar bugs encontrados durante validação)

**Recomendações:**
- Considerar botão "Cancel" durante execução do teste (futuro)
- Considerar histórico de testes (futuro)
- Monitorar se Registro.br/CETIC.br mantêm disponibilidade

**Passou com:**
- [ ] 0 bloqueadores críticos
- [ ] 0 bloqueadores altos
- [ ] X bloqueadores médios (listar)
- [ ] X bloqueadores baixos (listar)

