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

### Qualidade e segurança
- [ ] Testes unitários passando (`.\android\gradlew.bat test`)?
- [ ] Crash rate do build anterior < 1%?
- [ ] Sem TODOs críticos não resolvidos no diff?
- [ ] Nenhuma chave API hardcoded no código?
- [ ] Dados sensíveis não logados em produção?

### Play Store
- [ ] Screenshots e ícone atualizados se UI mudou?
- [ ] Descrição da release atualizada?
- [ ] Política de privacidade atualizada se novos dados coletados?

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
- Build/release Android → Camilo. Changelog → Gema.
