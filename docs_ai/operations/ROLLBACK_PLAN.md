# Plano de Rollback — SignallQ

> Atualizado em 2026-06-28.

## Quando fazer rollback

| Indicador | Threshold | Ação |
|---|---|---|
| Crash rate | > 2% em 1h | Rollback imediato |
| ANR rate | > 1% em 1h | Rollback imediato |
| Feature core quebrada | Speedtest/diagnóstico inacessível | Rollback imediato |
| Worker down | Error rate > 10% por 15min | Rollback do Worker |
| Regressão severa | Perda de dados, UI inutilizável | Rollback imediato |

## Procedimentos por componente

### Android — Play Store

**Pausar rollout staged:**
1. Play Console > Release > Production
2. Clicar "Halt rollout"
3. O app continua disponível na versão anterior para quem não atualizou

**Rollback para versão anterior:**
1. Play Console > Release > Production > Create new release
2. Selecionar AAB da versão anterior (já uploaded)
3. Staged rollout 100% (rollback é urgente)

**Limitação:** quem já atualizou não recebe downgrade automático. A nova versão (fix ou rollback) precisa ter versionCode maior.

### Android — Firebase App Distribution

```powershell
# Rebuildar versão anterior
git checkout v0.XX.X
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\android\gradlew.bat clean assembleRelease --no-build-cache
.\android\gradlew.bat appDistributionUploadRelease
```

### Workers Cloudflare

**Rollback automático (última versão):**
```bash
npx wrangler rollback <worker-name>
```

**Rollback manual (versão específica):**
```bash
# Checkout do código anterior
git checkout <commit-hash> -- integrations/cloudflare/<worker>/
cd integrations/cloudflare/<worker>
npx wrangler deploy
```

**Workers do SignallQ:**
- `linka-ai-diagnosis-worker` — diagnóstico IA
- `signallq-admin-worker` — admin backend
- Privacy pages (Cloudflare Pages)

### D1 Database

**Não há rollback automático de schema.** Mitigações:
- Migrations são aditivas (ADD COLUMN) — não quebram versões anteriores
- Para rollback de dados: restaurar backup D1
- Backup manual: `npx wrangler d1 export signallq-admin-db --remote --output backup.sql`

## Comunicação durante rollback

1. **Imediato:** comentário na issue do Linear com status
2. **Em 30min:** atualização com causa raiz identificada
3. **Resolução:** post-mortem breve no Linear

## Prevenção

- Staged rollout sempre (10% → 25% → 50% → 100%)
- Monitorar Crashlytics nas primeiras 2h após cada stage
- Nunca fazer rollout 100% no mesmo dia do upload
- Feature flags para funcionalidades novas de risco
