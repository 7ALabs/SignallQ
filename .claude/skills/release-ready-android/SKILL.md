---
description: Checklist pré-release Android — versionamento, ProGuard/R8, permissões no Manifest, assinatura, build release e Play Store compliance.
---

## Quando usar
Antes de gerar APK/AAB de release ou submeter à Play Store.

## Checklist de versionamento
- [ ] `versionCode` incrementado no `build.gradle` do `:app`?
- [ ] `versionName` atualizado (SemVer: MAJOR.MINOR.PATCH)?
- [ ] CHANGELOG atualizado com a versão e data?
- [ ] Tag git criada para a versão?

## Checklist de build
- [ ] Build de release compilando sem warnings críticos?
- [ ] ProGuard/R8 ativo no release build?
- [ ] Regras de ProGuard para Room, Retrofit, Gson (ou bibliotecas usadas) configuradas?
- [ ] APK/AAB assinado com keystore correto (não debug keystore)?
- [ ] `debuggable false` no release build type?
- [ ] `minifyEnabled true` e `shrinkResources true`?

## Checklist de qualidade
- [ ] Testes unitários passando?
- [ ] Crash rate do build anterior < 1%?
- [ ] Sem TODOs críticos não resolvidos no diff?
- [ ] Sem logs de debug excessivos no build release?

## Checklist Play Store
- [ ] Screenshots e ícone atualizados se UI mudou?
- [ ] Descrição da release atualizada?
- [ ] Permissões sensíveis justificadas?
- [ ] Target SDK atualizado (dentro do limite Google)?
- [ ] Política de privacidade atualizada se novos dados coletados?

## Checklist de segurança
- [ ] Nenhuma chave API hardcoded no código?
- [ ] Network security config adequada (não trust all certs)?
- [ ] Dados sensíveis não logados em produção?

## Limites
- Esta skill orienta, não implementa.
- Build e release → Camilo.
- Changelog e documentação → Gema via `/changelog-update`.
