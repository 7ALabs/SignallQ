# Checklist Go/No-Go — SignallQ

> Atualizado em 2026-07-17. Cronograma comprimido pra produção em 07/08/2026 (pedido do
> Luiz) — a trilha `alpha` já está ativa desde 10/07, mais cedo do que o plano original
> (datado 2026-06-28) assumia, o que abre espaço pra antecipar o Open Beta sem cortar o
> mínimo de 14 dias. Riscos reais dessa compressão documentados em `.claude/CLAUDE.md`
> (seção "Milestones") — não repetidos aqui, só as datas revisadas. Usar antes de cada fase
> de lançamento.

## M2 — Beta Fechado (21/07/2026)

### Go Criteria (todos obrigatórios)

**App:**
- [ ] Build release compila e assina sem erros
- [ ] Todas as features MVP ativas (`FEATURE_*` = true no release)
- [ ] Features pós-MVP desativadas no release
- [ ] Zero crashes em smoke test (5 fluxos core)
- [ ] Speedtest funciona end-to-end
- [ ] Diagnóstico IA retorna resultado válido
- [ ] Histórico persiste entre sessões
- [ ] Fresh install funciona sem crash

**Infraestrutura:**
- [ ] Workers Cloudflare respondendo (ai-diagnosis, admin, privacy)
- [ ] D1 database acessível e com schema atualizado
- [ ] Firebase Analytics recebendo eventos
- [ ] Crashlytics ativo e reportando

**Segurança:**
- [ ] Zero logs de debug em release (Timber only)
- [ ] Credenciais do modem criptografadas
- [ ] ProGuard/R8 ativo com mapping.txt gerado
- [ ] networkSecurityConfig correto

**Documentação:**
- [ ] CHANGELOG.md atualizado
- [ ] Release notes para testers
- [ ] Política de privacidade publicada

### No-Go Criteria (qualquer um bloqueia)

- Crash rate > 0.5% em smoke test
- Feature core (speedtest, diagnóstico) quebrada
- Worker Cloudflare com erro 500
- Keystore sem backup confirmado

---

## M3 — Play Store, listing/compliance pronto (concluído — 21/21 issues fechadas em 2026-07-17)

Este milestone cobre a prontidão de listagem/compliance (Data Safety, IARC, política de
privacidade, docs de lançamento) — não é o go-live em produção em si, que é o M5 abaixo.

### Go Criteria (todos obrigatórios — inclui todos do M2 +)

**Play Console:**
- [ ] Conta Google Play Developer ativa ($25 pago)
- [ ] Verificação de identidade aprovada
- [ ] Dados fiscais configurados
- [ ] Play App Signing configurado
- [ ] AAB uploaded e aceito
- [ ] Data Safety preenchido
- [ ] IARC classificação indicativa preenchida
- [ ] Descrição curta e longa aprovadas
- [ ] Screenshots (mín. 8) e Feature Graphic (1024x500)
- [ ] Política de Privacidade URL pública vinculada
- [ ] Termos de Uso publicados

**Qualidade:**
- [ ] ANR rate < 0.47%
- [ ] Crash-free rate >= 99%
- [ ] Cold start < 2s
- [ ] Download size < 15 MB

**Legal:**
- [ ] Consentimento LGPD implementado
- [ ] Política de Privacidade revisada
- [ ] Termos de Uso revisados

### No-Go Criteria

- Qualquer item M2 não atendido
- Play Console com warnings não resolvidos
- Crash-free rate < 99%
- Política de privacidade não publicada

---

## M4 — Open Beta (início ~21/07/2026, mínimo 14 dias → conclui ~04/08/2026)

### Go Criteria (todos do M2 +)

- [ ] Beta fechado (trilha `alpha`) rodou por >= 14 dias (já satisfeito — ativa desde 10/07)
- [ ] Feedback dos beta testers processado
- [ ] Bugs P0/P1 do beta corrigidos
- [ ] Métricas de retenção D1/D7 estáveis
- [ ] Procedimento de hotfix testado
- [ ] `promote-release.yml` ampliado pra aceitar `beta` como destino (guardrail hoje só
      permite `internal`/`alpha`)

---

## M5 — Produção (07/08/2026)

### Go Criteria (todos do M4 +)

- [ ] Open Beta rodou por >= 14 dias
- [ ] Crash-free rate >= 99.5%
- [ ] Plano de hypercare ativo
- [ ] Daily review de crashes configurado
- [ ] Runbook de lançamento revisado
- [ ] Email de suporte configurado
- [ ] Requisito real do Google pra elegibilidade de produção (duração/testadores mínimos em
      teste fechado, contas pessoais pós nov/2023) confirmado direto no Play Console — não
      verificado com precisão na documentação pública consultada em 2026-07-17
