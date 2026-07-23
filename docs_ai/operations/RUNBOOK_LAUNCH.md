# Runbook de Lançamento — SignallQ Android

**Projeto:** SignallQ (diagnóstico de conectividade)
**Package:** `io.signallq.app`
**Criado:** 2026-06-28
**Responsáveis:** Claudete (PM), Gema (QA/Release), Camilo (Dev Android)

---

## Milestones de referência

| Milestone | Data | Descrição |
|-----------|------|-----------|
| M2 — Beta Fechado | 31/07/2026 | Primeiro upload Play Store (internal testing) |
| M3 — Play Store | 07/08/2026 | Publicação na Play Store (closed beta) |
| M4 — Open Beta | 21/08/2026 | Track aberto na Play Store |
| M5 — Produção | 04/09/2026 | Release público com staged rollout |

---

## 1. Pré-lançamento (D-7)

### 1.1 Feature freeze

- [ ] Nenhuma feature nova entra em `main` a partir de D-7
- [ ] Apenas bug fixes críticos e ajustes de copy são aceitos
- [ ] Comunicar freeze no Linear (comentário no cycle ativo)

### 1.2 Checklist técnico

#### Versionamento
- [ ] `versionName` atualizado em `android/gradle/libs.versions.toml` (formato: `1.0.0` para produção, `1.0.0-beta.N` para beta)
- [ ] `versionCode` incrementado (nunca reutilizar — requisito Play Store)
- [ ] Comando: `.\scripts\version.ps1 set <versao>`

#### Código
- [ ] Todos os testes passando: `.\android\gradlew.bat test`
- [ ] ktlint sem violações: `.\android\gradlew.bat ktlintCheck`
- [ ] detekt sem violações: `.\android\gradlew.bat detekt`
- [ ] Feature flags de release conferidas em `app/build.gradle.kts` bloco `release {}`
- [ ] Nenhuma feature flag pós-MVP ativada acidentalmente
- [ ] ProGuard/R8 ativo (`isMinifyEnabled = true`, `isShrinkResources = true`)

#### Signing
- [ ] `key.properties` presente na raiz do projeto Android
- [ ] `segredos/signallq.jks` presente localmente
- [ ] Assinatura validada com build de teste: `.\android\gradlew.bat clean assembleRelease --no-build-cache`

#### Workers Cloudflare
- [ ] `ai-diagnosis-worker` funcionando — testar endpoint: `POST https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev/api/ai/diagnostico-conexao`
- [ ] `signallq-admin-worker` funcionando — testar: `GET https://signallq-admin.giammattey-luiz.workers.dev/health`
- [ ] Privacy pages acessíveis (política de privacidade, termos de uso)
- [ ] Se houver mudanças pendentes em workers, deploy antes do freeze:
  ```powershell
  cd integrations/cloudflare/ai-diagnosis-worker
  npx wrangler deploy
  ```

#### Firebase
- [ ] `google-services.json` atualizado no projeto
- [ ] Crashlytics recebendo eventos (verificar no console Firebase)
- [ ] Analytics recebendo eventos (verificar no console Firebase)
- [ ] App ID Firebase: `1:620840247394:android:0be190e89194bced37713b`

#### Play Console (preparar antes do upload)
- [ ] Conta de desenvolvedor ativa e verificada
- [ ] App criado no Play Console com package `io.signallq.app`
- [ ] Listing completo: título, descrição curta/longa, screenshots, ícone, feature graphic
- [ ] Categorização: Ferramentas > Conectividade
- [ ] Classificação IARC preenchida
- [ ] Data Safety preenchida (declarar: dados de rede/Wi-Fi coletados para diagnóstico, Firebase Analytics, Crashlytics)
- [ ] Política de privacidade URL configurada
- [ ] País de distribuição: Brasil (inicialmente)

### 1.3 Smoke test (D-3)

Executar em dispositivo físico com build release assinado:

- [ ] Onboarding completo (primeira abertura)
- [ ] Permissões solicitadas no contexto correto (localização, Wi-Fi)
- [ ] Speedtest executa e mostra resultado com veredito
- [ ] Diagnóstico local executa e gera relatório
- [ ] Diagnóstico IA executa (chamada ao worker) e retorna laudo
- [ ] Fallback IA funciona quando worker indisponível
- [ ] Análise Wi-Fi mostra dados da rede conectada
- [ ] Análise rede móvel mostra dados do operador
- [ ] Histórico salva e exibe resultados anteriores
- [ ] Laudo PDF gera e abre/compartilha corretamente
- [ ] Tela de ajustes abre e salva preferências
- [ ] Tela de privacidade abre e carrega conteúdo
- [ ] Tela de novidades abre e exibe conteúdo
- [ ] Navegação entre as 5 abas funciona (Início, Velocidade, Sinal, Histórico, Ajustes)
- [ ] App funciona offline (modo degradado, sem IA)
- [ ] Crash-free: nenhum crash durante o smoke test
- [ ] Telemetria: evento `app_open` aparece no Firebase Analytics

### 1.4 Build AAB de homologação

```powershell
cd "C:\Projetos\SignallQ\android"
.\gradlew.bat clean bundleRelease --no-build-cache
```

AAB gerado em: `app/build/outputs/bundle/release/app-release.aab`

Validar com bundletool:
```powershell
java -jar bundletool.jar validate --bundle=app/build/outputs/bundle/release/app-release.aab
```

---

## 2. Dia do lançamento (D-Day)

### 2.1 Preparação final

- [ ] Pull mais recente de `main`: `git pull origin main`
- [ ] Confirmar que não há commits pendentes ou WIP
- [ ] Conferir `versionName` e `versionCode` finais

### 2.2 Build AAB final

```powershell
cd "C:\Projetos\SignallQ\android"

# Limpar tudo
.\gradlew.bat clean

# Build AAB release (Play Store aceita apenas AAB)
.\gradlew.bat bundleRelease --no-build-cache
```

Artefato: `app/build/outputs/bundle/release/app-release.aab`

Verificar assinatura:
```powershell
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

### 2.3 Upload para Play Console

1. Acessar: https://play.google.com/console
2. Selecionar app `io.signallq.app`
3. Navegar para **Release > Production** (ou track adequado ao milestone)

#### Por milestone:

| Milestone | Track Play Console | Ação |
|-----------|-------------------|------|
| M2 (31/07) | Internal testing | Upload AAB, adicionar testadores por email |
| M3 (07/08) | Closed testing (beta) | Upload AAB, grupo de testadores selecionado |
| M4 (21/08) | Open testing | Upload AAB, qualquer usuário pode entrar |
| M5 (04/09) | Production | Upload AAB, staged rollout |

4. Clicar **Create new release**
5. Upload do AAB
6. Preencher release notes em pt-BR:
   ```
   SignallQ v1.0.0

   Diagnóstico completo da sua conexão de internet.

   - Teste de velocidade com veredito inteligente
   - Diagnóstico por IA: laudo detalhado da sua conexão
   - Análise de Wi-Fi e rede móvel
   - Histórico de testes
   - Relatório PDF para compartilhar

   Novidade: assistente IA que analisa sua rede e sugere soluções.
   ```
7. Revisar e clicar **Start rollout**

### 2.4 Staged rollout (apenas M5 — Produção)

| Fase | % usuários | Duração mínima | Critério para avançar |
|------|-----------|----------------|----------------------|
| 1 | 10% | 48h | Crash-free rate > 99%, sem ANR crítico |
| 2 | 25% | 48h | Crash-free rate > 99%, sem regressão em reviews |
| 3 | 50% | 72h | Crash-free rate > 99.5%, métricas estáveis |
| 4 | 100% | — | Tudo estável |

Para avançar o rollout:
1. Play Console > Release > Production
2. Clicar na release ativa
3. **Increase rollout** > selecionar próxima porcentagem
4. Confirmar

### 2.5 Deploy de workers (se houver mudanças)

```powershell
# ai-diagnosis-worker
cd "C:\Projetos\SignallQ\integrations\cloudflare\ai-diagnosis-worker"
npx wrangler deploy

# signallq-admin-worker (se aplicável)
cd "C:\Projetos\SignallQ\integrations\cloudflare\signallq-admin-worker"
npx wrangler deploy
```

### 2.6 Verificação pós-upload

- [ ] Play Console mostra status "In review" ou "Available"
- [ ] Firebase Crashlytics: novo versionCode aparece nos filtros
- [ ] Firebase Analytics: eventos da nova versão chegando
- [ ] Worker AI respondendo normalmente (monitorar logs no Cloudflare dashboard)

---

## 3. Pós-lançamento

### 3.1 D+1 — Verificação imediata

- [ ] Crashlytics: crash-free rate > 99%
- [ ] Crashlytics: nenhum crash novo com > 5 ocorrências
- [ ] Analytics: evento `app_open` registrado por usuários da nova versão
- [ ] Analytics: evento de diagnóstico IA executado com sucesso
- [ ] Play Console: nenhuma review 1-estrela com padrão de bug
- [ ] Worker AI: latência média < 15s, taxa de erro < 5%
- [ ] Worker Admin: ingest funcionando (se telemetria ativa)

### 3.2 D+7 — Estabilidade

- [ ] Crash-free rate > 99.5%
- [ ] ANR rate < 0.5%
- [ ] Nenhum crash com mais de 10 ocorrências sem fix
- [ ] Rating médio na Play Store > 4.0
- [ ] Retenção D1 > 30% (se tiver volume suficiente)
- [ ] Worker AI: custo Cloudflare dentro do esperado (verificar dashboard)
- [ ] Avaliar se rollout pode avançar para próxima fase

### 3.3 D+30 — Consolidação

- [ ] Crash-free rate > 99.7%
- [ ] Rollout em 100% (se staged)
- [ ] Retenção D7 > 15%
- [ ] Feedback de usuários catalogado (reviews + beta testers)
- [ ] Backlog atualizado com bugs e melhorias reportados
- [ ] Documentação de release finalizada em `docs_ai/RELEASES.md`
- [ ] Notion atualizado com status do milestone
- [ ] Linear: milestone marcado como concluído

### Métricas-alvo consolidadas

| Métrica | Alvo | Crítico (rollback) |
|---------|------|-------------------|
| Crash-free rate | > 99.5% | < 98% |
| ANR rate | < 0.5% | > 2% |
| Latência worker IA | < 15s | > 30s |
| Taxa erro worker IA | < 5% | > 20% |
| Rating Play Store | > 4.0 | < 3.0 |

---

## 4. Rollback

### 4.1 Quando reverter

Acionar rollback se qualquer condição:
- Crash-free rate < 98% por mais de 2h
- ANR rate > 2% por mais de 2h
- Bug funcional grave (diagnóstico IA retorna lixo, speedtest trava, dados corrompidos)
- Worker AI fora do ar sem recuperação em 30min
- Vulnerabilidade de segurança identificada

### 4.2 Rollback do app (Play Console)

1. Play Console > Release > Production (ou track ativo)
2. Clicar na release problemática
3. **Halt rollout** (pausa imediata — nenhum novo usuário recebe o update)
4. Se necessário reverter quem já atualizou:
   - Preparar hotfix com versionCode incrementado
   - Ou fazer rollback para versão anterior (Play Console > Manage releases > selecionar versão anterior > **Rollout**)

A Play Store não "desinstala" de quem já atualizou. Rollback só impede novos downloads. Hotfix é a solução efetiva.

### 4.3 Rollback de worker Cloudflare

```powershell
# Reverter para versão anterior via Cloudflare dashboard
# Ou re-deploy de commit anterior:
cd "C:\Projetos\SignallQ\integrations\cloudflare\ai-diagnosis-worker"
git checkout <commit-anterior> -- .
npx wrangler deploy

# Voltar para main depois do hotfix
git checkout main -- .
```

Alternativa via dashboard:
1. Cloudflare > Workers & Pages > linka-ai-diagnosis-worker
2. Deployments > selecionar deploy anterior > **Rollback**

### 4.4 Procedimento de hotfix

1. Criar branch: `git checkout -b hotfix/descricao-curta`
2. Corrigir o problema
3. Incrementar `versionCode` (nunca reutilizar)
4. `versionName`: adicionar sufixo se necessário (ex: `1.0.1`)
5. Build: `.\android\gradlew.bat clean bundleRelease --no-build-cache`
6. Upload para Play Console no mesmo track
7. Marcar como "Full rollout" (hotfix vai para 100%)
8. Merge hotfix em `main`

---

## 5. Contatos e escalation

### Responsáveis diretos

| Papel | Agente | Atuação |
|-------|--------|---------|
| Decisão final | **Luiz Giammattey** (CEO) | Aprovar rollback, publicação, custo |
| PM & coordenação | **Claudete** | Coordenar lançamento, comunicar status |
| Dev Android | **Camilo** | Build, hotfix, investigação técnica |
| QA & Release | **Gema** | Smoke test, monitoramento, validação |
| UX | **Lia** | Ajustes visuais emergenciais |
| Admin & Dados | **Felipe** | Worker admin, dados de telemetria |

### Matriz de escalation

| Cenário | Ação imediata | Escalar para |
|---------|--------------|-------------|
| Crash rate < 98% | Halt rollout | Luiz (decisão de rollback) |
| Worker IA fora do ar | Verificar Cloudflare dashboard | Camilo (re-deploy) |
| Worker IA com custo inesperado | Pausar billing alert | Luiz (decisão de custo) |
| Bug funcional grave | Investigar causa, preparar hotfix | Camilo + Gema |
| Review negativa com padrão | Catalogar, priorizar fix | Claudete (triagem) |
| Play Console rejeição | Ler motivo, corrigir listing/AAB | Gema + Claudete |
| Problema de signing/keystore | Verificar `key.properties` e `.jks` | Camilo |

### Recursos externos

| Recurso | URL |
|---------|-----|
| Play Console | https://play.google.com/console |
| Firebase Console | https://console.firebase.google.com |
| Cloudflare Dashboard | https://dash.cloudflare.com |
| Cloudflare Workers Logs | Dashboard > Workers > linka-ai-diagnosis-worker > Logs |
| Linear (backlog) | https://linear.app |
| GitHub (repo) | https://github.com/7ALabs/linka-android |

---

## Apêndice: Comandos rápidos

```powershell
# Versão atual
.\scripts\version.ps1 show

# Incrementar versão
.\scripts\version.ps1 patch          # 1.0.0 → 1.0.1
.\scripts\version.ps1 minor          # 1.0.0 → 1.1.0
.\scripts\version.ps1 set 1.0.0+70   # definir versão específica

# Build AAB (Play Store)
.\android\gradlew.bat clean bundleRelease --no-build-cache

# Build APK (Firebase App Distribution / teste local)
.\android\gradlew.bat clean assembleRelease --no-build-cache

# Upload Firebase App Distribution
.\android\gradlew.bat appDistributionUploadRelease

# Deploy worker Cloudflare
cd integrations/cloudflare/ai-diagnosis-worker && npx wrangler deploy

# Testes
.\android\gradlew.bat test
.\android\gradlew.bat ktlintCheck
.\android\gradlew.bat detekt

# Validar assinatura
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```
