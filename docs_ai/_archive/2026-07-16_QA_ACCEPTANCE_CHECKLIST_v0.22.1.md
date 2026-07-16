> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: checklist especifico de v0.22.1, ja superado por releases posteriores; sem substituto formal, referencia historica. Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# QA Acceptance Checklist — SignallQ v0.22.1 (versionCode 55)

**Data de Execução:** 2026-07-03  
**Responsável QA:** Gema  
**Status:** Automação Executada / Pendente de Device Real  

---

## RESUMO DE AUTOMAÇÃO

### ✅ O QUE PASSOU

- [x] Versionamento: versionName = 0.22.1, versionCode = 55 (confirmado em libs.versions.toml)
- [x] Compilação: compileSdk 37, minSdk 24, targetSdk 36 (correto)
- [x] Testes unitários: `:coreNetwork:test`, `:featureDevices:test`, `:featureDiagnostico:test` — BUILD SUCCESSFUL
- [x] APK release anterior: existe em app/build/outputs/apk/release/

### ⚠️ WARNINGS (NÃO-BLOQUEANTES)

- 29 deprecation warnings em Kotlin (Icons AutoMirrored, Locale constructor, PulseState typealias)
- Gradle features deprecated (android.builtInKotlin, android.newDsl)

---

## PENDÊNCIAS MANUAIS PARA TESTERS ALPHA (PRIORIDADE)

### 🔴 CRÍTICO — Deve passar para beta M2

| Issue | Scope | Devices | Tempo |
|-------|-------|---------|-------|
| **SIG-217** | Fresh install + onboarding + update v0.21.0→v0.22.1 | Pixel 7a, Galaxy A14, Emulator API 24 | 30min |
| **SIG-219** | Speedtest end-to-end (Rápido/Completo/Cancelamento) | Pixel 7a, Moto G54, Galaxy A14 | 45min |
| **SIG-222** | Diagnóstico local + IA + chat + fallback offline | Pixel 7a | 30min |

### 🟡 ALTO — Deve passar antes do next release

| Issue | Scope | Devices | Tempo |
|-------|-------|---------|-------|
| **SIG-221** | Permissões (grant/deny/revoke) + rationale | Pixel 7a, Xiaomi Redmi | 40min |
| **SIG-223** | Offline mode + timeout + retry backoff | Pixel 7a, Moto G54 | 35min |
| **Regressão** | Smoke test — Home, Sinal, Histórico, Ajustes | 1 device | 20min |

### 🟢 MÉDIO — Validar antes de beta

| Item | Devices | Tempo |
|------|---------|-------|
| Acessibilidade (TalkBack + WCAG) | 1 device | 15min |
| Documentação (CHANGELOG atualizado) | Manual review | 10min |

**TEMPO TOTAL ESTIMADO:** 3-4 horas (2 devices em paralelo)

---

## POR QUE NÃO RODOU TUDO?

1. **Lock Kotlin Daemon:** IOException ao deletar build intermediates (processo com file open)
2. **Sem Device Real:** Onboarding, permissões sistema, real speedtest, offline mode = requerem interação manual
3. **Sem Firebase Test Lab:** Espresso instrumented tests não foram rodados
4. **Worker IA:** Real IA response (Qwen/fallback) precisa do Worker online ou simulado com proxy

---

## CHECKLIST RÁPIDO (CAPTURA MANUAL)

**Para cada SIG-XXX:**

- [ ] SIG-217: Fresh install → onboarding → update → versionCode exibido
- [ ] SIG-219: Speedtest Rápido (30s), Completo (2min), Cancelar, Compartilhar
- [ ] SIG-222: Diagnóstico → IA Analysis → Chat (3+ mensagens)
- [ ] SIG-221: Grant Localização → negar Notificações → Ajustes mostra status
- [ ] SIG-223: Mode avião → offline banner → reconexão → Diagnóstico timeout
- [ ] Regressão: Home, Sinal, Histórico, Ajustes — sem crash

---

## PRÓXIMOS PASSOS

1. **Testers Alpha:** Começar por SIG-217 em Pixel 7a (fresh install é base para tudo)
2. **Gema:** Consolidar resultados manualmente à medida que vierem
3. **Claudete:** Se tudo passar → go/no-go para Closed Beta (M2)

---

**Build:** v0.22.1 (versionCode 55) — Alpha Track Play Console  
**Data:** 2026-07-03  
**QA Status:** Aguardando Captura Manual (Automação OK)
