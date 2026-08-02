# Procedimento de Hotfix — SignallQ

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Escopo:** correções críticas fora do ciclo regular de release

## Quando usar

Hotfix é para correções **críticas** que não podem esperar o próximo ciclo regular:
- Crash rate > 1% em release
- Funcionalidade core quebrada (speedtest, diagnóstico, IA)
- Vulnerabilidade de segurança explorada
- Perda de dados do usuário

## Fluxo

### 1. Identificar e classificar

| Severidade | Critério | SLA |
|---|---|---|
| P0 — Crítico | Crash em >5% dos usuários, dados corrompidos | 4h para fix, deploy imediato |
| P1 — Alto | Feature core quebrada, crash em 1-5% | 24h para fix |
| P2 — Médio | Bug visível mas com workaround | Próximo ciclo regular |

### 2. Branch e desenvolvimento

```
git checkout main
git checkout -b hotfix/N-descricao-curta
```

`N` é o número da issue GitHub (não mais `sig-XXX` — Linear deixou de ser fonte da verdade de
execução em 2026-07-09, ver `.claude/CLAUDE.md`, seção "Fontes da Verdade").

- Escopo mínimo: apenas o fix, nada mais
- Sem refactor, sem cleanup, sem features
- Testes do fluxo afetado obrigatórios

### 3. Validação

- [ ] Build release compila sem erros
- [ ] Teste manual do fluxo afetado
- [ ] Crashlytics sem novos crashes no APK de teste
- [ ] Regressão básica: speedtest, diagnóstico, histórico

### 4. Deploy

**Android (Firebase App Distribution):**
```powershell
.\android\gradlew.bat clean assembleRelease --no-build-cache
.\android\gradlew.bat appDistributionUploadRelease
```

**Android (Play Store — após M3):**
- Upload AAB via Play Console
- Staged rollout: 10% → monitorar 2h → 100%
- Rollout imediato apenas para P0

**Workers Cloudflare:**
```bash
cd integrations/cloudflare/<worker-afetado>
npx wrangler deploy
```

### 5. Pós-deploy

- [ ] Monitorar Crashlytics por 2h
- [ ] Verificar Analytics events normais
- [ ] Comunicar na issue GitHub (comentário + fechar via PR "Closes #N")
- [ ] Merge hotfix branch em main
- [ ] Bump versionCode (não versionName para patch)

## Versionamento de Hotfix

- Patch version: `0.21.0` → `0.21.1`
- versionCode: incrementar em 1
- CHANGELOG.md: adicionar entrada na seção da versão atual

## Rollback

Se o hotfix introduzir novo problema:
1. Play Store: pausar rollout imediatamente
2. Firebase: enviar build anterior via App Distribution
3. Workers: `npx wrangler rollback` ou redeploy versão anterior
