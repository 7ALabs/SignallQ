## Contexto
Não há Detekt, Ktlint ou Spotless configurados, nem workflow de CI no repositório. Isso permite que regressões de estilo, `!!`, complexidade alta e outros code smells entrem no main sem barreira. Estabelecer base de qualidade automatizada é pré-requisito para várias outras issues (#4 em particular).

## Evidência
- `build.gradle.kts` raiz — sem plugins de qualidade
- Ausência de `.github/workflows/`

## Critério de aceite
- [ ] Detekt configurado no `build.gradle.kts` raiz com `detekt.yml` versionado em `config/detekt/`
- [ ] Ktlint (ou Spotless com ktlint) configurado e formatando automaticamente
- [ ] Tasks `./gradlew detekt` e `./gradlew ktlintCheck` rodando em todos os módulos
- [ ] Workflow `.github/workflows/ci.yml` rodando em PR: `assembleDebug`, `test`, `detekt`, `ktlintCheck`
- [ ] Wrappers `scripts/lint.ps1` e `scripts/test.ps1` para devs locais
- [ ] Baseline Detekt criada (`detekt-baseline.xml`) para não bloquear primeiro PR; novos achados são erro
- [ ] Documentado em `docs/MAINTENANCE_PLAN.md` ou `docs_ai/technical/`

## Como verificar
```powershell
.\gradlew.bat detekt ktlintCheck test assembleDebug
.\scripts\lint.ps1
```
PR de teste deve ter checks verdes no GitHub.

## Notas para o agente
- Skills: `signallq-arch`, `signallq-docs`
- Manter custo de execução baixo: rodar Detekt incremental por módulo
- Dependências: bloqueia #4 (regras de `!!` são aplicadas via Detekt)
