# Auditoria de Dependencias — SignallQ Android

Data: 2026-06-28
Issue: SIG-216
Versao auditada: 0.21.0 (versionCode 52)
Catalogo: `android/gradle/libs.versions.toml`

---

## Tabela de Dependencias

| Dependencia | Versao Atual | Versao Recente | Status | Nota |
|---|---|---|---|---|
| **Firebase BOM** | 34.15.0 | 34.15.0 | ok | Atualizado |
| **Hilt / Dagger** | 2.58 | 2.58 | ok | Atualizado |
| **Room** | 2.8.4 | 2.8.4 | ok | Atualizado |
| **OkHttp** | 4.12.0 | 5.x disponivel | atualizar | 4.12.0 sem CVEs conhecidos; 5.x traz HTTP/3, Kotlin-first. Migrar quando conveniente |
| **Compose BOM** | 2025.05.01 | 2026.06.00 | atualizar | 13 meses defasado. Prioridade alta |
| **AndroidX Core KTX** | 1.19.0 | 1.19.0 | ok | Atualizado |
| **Activity KTX** | 1.9.3 | 1.9.3 | ok | Atualizado |
| **Material** | 1.12.0 | 1.12.0 | ok | Atualizado |
| **Lifecycle** | 2.8.7 | 2.11.0 | atualizar | 3 minor versions atras. Prioridade media |
| **Coroutines** | 1.9.0 | 1.11.0 | atualizar | 2 minor versions atras. Prioridade media |
| **Navigation** | 2.8.0 | 2.9.8 | atualizar | Prioridade media |
| **DataStore** | 1.1.1 | 1.2.1 | atualizar | Prioridade baixa |
| **WorkManager** | 2.11.2 | 2.11.2 | ok | Atualizado |
| **Coil 3** | 3.1.0 | 3.5.0 | atualizar | 4 minor versions atras. Prioridade media |
| **Timber** | 5.0.1 | 5.0.1 | ok | Atualizado |
| **ProfileInstaller** | 1.4.1 | 1.4.1 | ok | Atualizado |
| **Desugar JDK Libs** | 2.1.5 | 2.1.5 | ok | Atualizado |
| **Robolectric** | 4.13 | 4.16.1 | atualizar | Prioridade baixa (teste) |
| **Detekt** | 1.23.7 | 1.23.7 (2.0 alpha) | ok | 2.0 ainda em alpha, manter 1.23.x |
| **JUnit** | 4.13.2 | 4.13.2 | ok | Maintenance mode |
| **AndroidX Test JUnit** | 1.2.1 | 1.3.0 | atualizar | Prioridade baixa |
| **Espresso Core** | 3.6.1 | 3.7.0 | atualizar | Prioridade baixa |
| **Google Services Plugin** | 4.4.4 | 4.5.0 | atualizar | Prioridade baixa |
| **Firebase Crashlytics Plugin** | 3.0.2 | 3.0.7 | atualizar | Prioridade baixa |
| **Firebase App Distribution Plugin** | 5.1.1 | 5.3.0 | atualizar | Prioridade baixa |
| **KtLint Gradle Plugin** | 12.1.1 | 14.0.1 | atualizar | Prioridade baixa |

---

## CVEs e Vulnerabilidades

Nenhuma CVE critica afeta as versoes em uso.

- **OkHttp 4.12.0**: CVE-2023-3635 afetou versoes anteriores a 4.9.2 (Okio). A versao 4.12.0 nao e afetada. A linha 5.x e a evolucao natural, mas a migracao nao e urgente do ponto de vista de seguranca.
- **Desugar JDK Libs**: CVE-2026-22008 e do Oracle JDK, nao afeta a lib de desugar do Android.
- **Demais dependencias**: nenhum CVE conhecido nas versoes em uso.

---

## Recomendacoes Priorizadas

### Prioridade Alta

1. **Compose BOM 2025.05.01 -> 2026.06.00**
   - Defasagem de 13 meses. Compose evolui rapido e acumula bug fixes, melhorias de performance e novos componentes Material 3.
   - Risco de manter: incompatibilidades futuras se pular muitas versoes de uma vez.
   - Impacto: todas as dependencias Compose (ui, material3, navigation-compose, activity-compose) sobem juntas via BOM.

### Prioridade Media

2. **Lifecycle 2.8.7 -> 2.11.0** — novas APIs de lifecycle-aware, melhorias de Compose integration.
3. **Coroutines 1.9.0 -> 1.11.0** — bug fixes e melhorias de performance.
4. **Navigation 2.8.0 -> 2.9.8** — bug fixes acumulados, type-safe routes melhorados.
5. **Coil 3.1.0 -> 3.5.0** — melhorias de cache e performance.

### Prioridade Baixa

6. **DataStore 1.1.1 -> 1.2.1** — melhorias incrementais.
7. **Robolectric 4.13 -> 4.16.1** — melhor suporte a SDK 36/37.
8. **AndroidX Test JUnit 1.2.1 -> 1.3.0** e **Espresso 3.6.1 -> 3.7.0** — libs de teste, baixo risco.
9. **Google Services Plugin 4.4.4 -> 4.5.0** e plugins Firebase — melhorias incrementais.
10. **KtLint Gradle Plugin 12.1.1 -> 14.0.1** — atualizacao de tooling.

### Sem Urgencia

- **OkHttp 4.x -> 5.x**: migracao nao urgente. 4.12.0 e estavel e sem CVEs. Migrar quando houver necessidade de HTTP/3 ou quando a 4.x sair de manutencao.
- **Detekt 2.0**: ainda em alpha, manter 1.23.7.
- **JUnit 4 -> JUnit 5**: migracao grande, sem ganho imediato para o projeto.

---

## Plano Sugerido

Agrupar atualizacoes em 2 PRs:

**PR 1 — Compose + Lifecycle + Coroutines + Navigation** (prioridade alta/media)
- Compose BOM, Lifecycle, Coroutines, Navigation
- Rodar suite de testes completa apos atualizar
- Verificar breaking changes no changelog do Compose BOM

**PR 2 — Demais libs** (prioridade baixa)
- Coil, DataStore, Robolectric, AndroidX Test, plugins Gradle
- Menor risco, pode ir em batch

Cada PR deve passar por build completo + testes antes de merge.
