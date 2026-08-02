---
name: checar-release
description: Checklist pré-release por stack (Android e Cloudflare Pages) mais atualização de changelog.
---

## Quando usar
Antes de gerar APK/AAB de release, fazer deploy no Cloudflare Pages ou submeter à loja. Cobre as stacks mais o changelog. Use só as seções relevantes à entrega.

---

## Android

### Versionamento
- [ ] `versionCode` incrementado em `android/gradle/libs.versions.toml`?
- [ ] `versionName` atualizado (SemVer: MAJOR.MINOR.PATCH)?
- [ ] CHANGELOG atualizado com a versão e data?
- [ ] Tag git criada para a versão?

### ProGuard/R8 e build
- [ ] Build de release compilando sem warnings críticos?
- [ ] `minifyEnabled true` e `shrinkResources true` no release build type?
- [ ] Regras de ProGuard para Room, Retrofit, Gson (ou bibliotecas usadas) configuradas?
- [ ] `debuggable false` no release build type?
- [ ] Sem logs de debug excessivos no build release?

### Manifest e permissões
- [ ] Permissões sensíveis justificadas?
- [ ] Target SDK dentro do limite Google?
- [ ] Network security config adequada (não trust all certs)?

### Assinatura
- [ ] APK/AAB assinado com keystore correto (não debug keystore)?
- [ ] `io.veloo.app` preservado (package/applicationId/namespace nunca renomeados)?

### Qualidade
- [ ] Testes unitários passando (`.\android\gradlew.bat test`)?
- [ ] Crash rate do build anterior < 1%?
- [ ] Sem TODOs críticos não resolvidos no diff?

### Segurança pré-release
- [ ] Nenhuma chave API hardcoded no código (grep por `api_key`, `secret`, `password`)?
- [ ] Dados sensíveis não logados em produção (zero `Log.d`/`Log.w`/`Log.e` diretos — apenas Timber)?
- [ ] Credenciais em DataStore criptografadas (não plaintext)?
- [ ] `exported=false` em todos os providers/receivers/services que não precisam ser públicos?
- [ ] `networkSecurityConfig` não permite cleartext exceto para IPs de gateway LAN?
- [ ] ProGuard/R8 mapping.txt gerado e disponível para upload no Crashlytics?
- [ ] Sem dependências com CVE HIGH/CRITICAL conhecidos?

### Observabilidade
- [ ] Firebase Crashlytics ativo e recebendo crashes do release build?
- [ ] Firebase Analytics events chegando (verificar DebugView)?
- [ ] SHA-1/SHA-256 da release keystore registrados no Firebase?
- [ ] ReleaseTree filtrando logs WARN/ERROR para Crashlytics?

### Play Store
- [ ] Screenshots e ícone atualizados se UI mudou?
- [ ] Descrição da release atualizada?
- [ ] Política de privacidade atualizada se novos dados coletados?
- [ ] Data Safety atualizado se tipos de dados coletados mudaram?
- [ ] AAB gerado (não APK) — `gradlew bundleRelease`?
- [ ] Release notes escritas (máx. 500 chars PT-BR)?
- [ ] Consentimento LGPD funcional antes de coletar dados?

---

## Cloudflare Pages

### Deploy
- [ ] Branch correta selecionada para deploy?
- [ ] Preview deploy testado antes do production?

### Variáveis de ambiente
- [ ] Variáveis de produção configuradas no dashboard Cloudflare (não em `.env` commitado)?
- [ ] `VITE_*` → expostas ao bundle do cliente; sem prefixo → apenas em Functions/server?
- [ ] Segredos de servidor em Functions, nunca expostos ao cliente?
- [ ] `.env.production` com segredos nunca commitado?

### Redirects (`_redirects`)
- [ ] SPA redirect configurado: `/* /index.html 200`?
- [ ] Redirects específicos de rotas antes do catch-all?
- [ ] Trailing slash tratado consistentemente?

### Headers (`_headers`)
- [ ] Cache-Control configurado para assets estáticos (`/assets/*`)?
- [ ] Service Worker com `Cache-Control: no-cache`?
- [ ] CSP (Content Security Policy) configurado?
- [ ] `X-Frame-Options: DENY` para prevenir clickjacking?

### Pages Functions (`functions/`)
- Executam no edge (Cloudflare Workers runtime) — sem Node.js APIs, usar Web APIs padrão.
- Sem estado entre requests — KV para persistência.
- Timeout de 50ms CPU em plano gratuito.

---

## Execução automatizada (absorvido de `validar-release`, fundida em 2026-07-23)

As checagens abaixo rodam de forma programável — use depois de percorrer o checklist manual acima, antes de dar "pronto para release".

### 1. Versionamento
```bash
grep "versionCode" android/gradle/libs.versions.toml | grep -v "^#"
grep "versionName" android/gradle/libs.versions.toml | grep -E '"[0-9]+\.[0-9]+\.[0-9]+"'
VERSION=$(grep 'versionName' android/gradle/libs.versions.toml | sed 's/.*"\([^"]*\)".*/\1/')
grep "\[$VERSION\]" CHANGELOG.md
```
Se a versão não aparecer no CHANGELOG: **bloqueador** — adicione antes de seguir.

### 2. Build limpo
```bash
.\android\gradlew.bat clean assembleRelease --no-build-cache
```
Validar `BUILD SUCCESSFUL`, sem warning/error crítico, APK gerado em `app/build/outputs/apk/release/`.

### 3. Testes unitários
```bash
.\android\gradlew.bat test
```
Validar `BUILD SUCCESSFUL` e nenhum `FAILED` em Test Results.

### 4. Lint Kotlin
```bash
.\android\gradlew.bat ktlintCheck
```
Erros → `ktlintFormat`, commitar, rerun (ver skill `/protocolo-ktlint` para supressão/cleanup em escala).

### 5. Higiene de código
```bash
find android -name "*.kt" -type f | xargs grep -l "^[[:space:]]*//.*TODO\|^[[:space:]]*//.*FIXME" | head -5
find android -name "*.old" -o -name "*.bak" -o -name "*.tmp" | wc -l
```
TODO/FIXME crítico deve ter issue aberta no GitHub; contagem de `.old`/`.bak`/`.tmp` deve ser `0`.

### 6. Upload (após todas as checagens acima passarem)
```bash
.\android\gradlew.bat appDistributionUploadRelease
```
Validar `BUILD SUCCESSFUL` e URL de upload retornada no log.

### Validação estrutural do changelog
```bash
head -30 CHANGELOG.md | grep -E "^## \[[0-9]+\.[0-9]+\.[0-9]+\].*—.*[0-9]{4}-[0-9]{2}-[0-9]{2}$"
```
Confirma seção de versão no formato correto, com ao menos um `### Added`/`### Fixed`/`### Changed`, descrições em PT-BR legíveis para usuário final.

### Escalada
- **Build falha:** `.\android\gradlew.bat clean` + limpar `build/`, retry.
- **Testes falham:** investigar stacktrace, corrigir em dev branch, merge, rerun.
- **ktlint falha:** `ktlintFormat`, commit, rerun.
- **Versão não está no CHANGELOG:** adicionar no topo antes do build.
- **Crashlytics com crash rate > 1%:** não lançar — investigar e corrigir primeiro.

O pre-commit hook em `scripts/pre-commit-android.sh` automatiza parte deste checklist no momento do commit.

---

## Changelog

Atualizar após aprovar a entrega, antes do build final.

### Localização
- Android: `android/CHANGELOG.md`

### Formato (Keep a Changelog)

```markdown
## [X.Y.Z] — AAAA-MM-DD

### Added
- Descrição da feature nova em linguagem de usuário.

### Changed
- Descrição de comportamento alterado.

### Fixed
- Descrição do bug corrigido.

### Removed
- O que foi removido.
```

### Versionamento SemVer

| Tipo de mudança | Bump |
|---|---|
| Bug fix sem quebra de contrato | PATCH (X.Y.**Z**) |
| Feature nova retrocompatível | MINOR (X.**Y**.0) |
| Quebra de contrato, remoção | MAJOR (**X**.0.0) |

### Regras de escrita
- Escrever na perspectiva do usuário, não do dev. "Adicionado diagnóstico de fibra óptica" — não "implementado FeatureFibraViewModel".
- Máximo 1 linha por item. Sem abreviações técnicas nas seções Added/Changed/Fixed.
- Seção `[Unreleased]` no topo para mudanças ainda não lançadas.
- No Android, garantir que `versionCode`/`versionName` em `libs.versions.toml` estão consistentes com a versão documentada.

---

## Limites
- Esta skill orienta, não implementa.
- Build/release Android → Camilo. Changelog → Rhodolfo.
