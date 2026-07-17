# Módulo :featureSettings

- **Status:** ativo (módulo mínimo)
- **Última validação:** 2026-07-16 (fonte: `android/feature/settings/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/settings/`
- **Namespace:** `io.signallq.app.feature.settings`

## Responsabilidade

Módulo mínimo — só a fábrica do módulo. A tela real (`AjustesScreen.kt`, aba Ajustes) reside em
`:app` (`ui/screen/AjustesScreen.kt`, 809 linhas — ver dívida 4.4 da regra de higiene), não neste
módulo.

## Principais packages/pastas

Base: `feature/settings/src/main/kotlin/io/veloo/app/kotlin/feature/settings/` (caminho físico
legado) — único arquivo: `FeatureSettingsModulo.kt`.

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `FeatureSettingsModulo.kt` | Object | Fábrica estática — placeholder do padrão de módulo feature |

## Entradas/saídas

Nenhuma lógica própria relevante — módulo esqueleto.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`.

## Consumidores

Via grep de `project(":featureSettings")`: apenas `:app`.

## Testes existentes

`src/test`: **0 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Mesmo padrão de `:featureHome`: módulo praticamente vazio, lógica real concentrada em `:app`
(`AjustesScreen.kt`, já identificado como dívida crítica de tamanho na regra de higiene, seção
4.4). Não é correção pequena o suficiente para esta tarefa de documentação.
```
=== FIM ARQUIVO ===

Ao final, fora dos blocos de arquivo, conforme pedido:

**Violações reais de dependência encontradas (feature→feature)**
- `:featureDiagnostico` → `implementation(project(":featureSpeedtest"))` em `android/feature/diagnostico/build.gradle.kts`. Única violação real encontrada nos 16 módulos — todas as demais dependências de módulo seguem o padrão feature→core ou app→(core|feature). Não é diagnóstico especulativo: confirmado lendo o `build.gradle.kts` real. Recomendação (não executada, é read-only): extrair um contrato normalizado (ex.: interface `ExecutorSpeedtestProvider` ou `SnapshotExecucaoSpeedtest` já suficiente) para um módulo `core` adequado, ou mover a composição para `:app`, seguindo a regra 4.5 da regra de higiene.

**Contagem real de testes por módulo** (arquivos em `src/test`, mais `src/androidTest` entre parênteses quando > 0):
- `:app` — 42
- `:coreNetwork` — 11
- `:coreDatabase` — 1 (+3 androidTest)
- `:coreDatastore` — 0
- `:corePermissions` — 0
- `:coreTelephony` — 2
- `:coreRecommendation` — 1
- `:featureHome` — 0
- `:featureWifi` — 5
- `:featureDevices` — 10
- `:featureDns` — 1
- `:featureSpeedtest` — 2
- `:featureDiagnostico` — 33
- `:featureFibra` — 3
- `:featureHistory` — 7
- `:featureSettings` — 0
- **Total: 118 arquivos em `src/test` + 3 em `src/androidTest` = 121.** (`.claude/CLAUDE.md` cita "~66 arquivos de teste unitário" — número desatualizado frente à contagem real feita agora; não corrigido no CLAUDE.md porque está fora do escopo desta tarefa de documentação de arquitetura, mas sinalizo aqui.)

**Não confirmável / observações**
- `SignallQDatabase` — `FEATURE_FILE_MAPS.md` e o código-fonte apontam "versão 10", enquanto `.claude/CLAUDE.md` cita "Room v12" em outro contexto. Não abri o arquivo `SignallQDatabase.kt` para arbitrar a versão exata do schema (fora do escopo desta tarefa); sinalizei a divergência no doc de `core-database.md` para reconfirmação futura.
- Não executei build/testes reais (`ktlintCheck`, `detekt`, `test`, `assembleDebug`) — esta foi uma tarefa exclusivamente de leitura e geração de conteúdo markdown, sem gravação em disco nem validação de build, conforme escopo pedido.
- Nenhum arquivo foi criado ou modificado nesta sessão; todo o conteúdo acima é para você (Claudete/squad) revisar e gravar via `Write` quando aprovar o texto.agentId: abe0b402a44edaec3 (use SendMessage with to: 'abe0b402a44edaec3', summary: '<5-10 word recap>' to continue this agent)
<usage>subagent_tokens: 183739
tool_uses: 9
duration_ms: 250020</usage>
