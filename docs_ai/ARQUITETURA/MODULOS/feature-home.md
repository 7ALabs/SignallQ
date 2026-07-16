# Módulo :featureHome

- **Status:** ativo (módulo mínimo)
- **Última validação:** 2026-07-16 (fonte: `android/feature/home/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/home/`
- **Namespace:** `io.signallq.app.feature.home`

## Responsabilidade

Módulo mínimo — só a fábrica do módulo. A tela real (`HomeScreen.kt`, aba 0/Início) reside em
`:app` (`ui/screen/HomeScreen.kt`), não neste módulo.

## Principais packages/pastas

Base: `feature/home/src/main/kotlin/io/veloo/app/kotlin/feature/home/` (caminho físico legado) —
único arquivo: `FeatureHomeModulo.kt`.

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `FeatureHomeModulo.kt` | Object | Fábrica estática — placeholder do padrão de módulo feature |

## Entradas/saídas

Nenhuma lógica própria relevante — módulo esqueleto.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`.

## Consumidores

Via grep de `project(":featureHome")`: apenas `:app`.

## Testes existentes

`src/test`: **0 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Módulo praticamente vazio — a lógica real da Home vive em `:app`, quebrando a convenção "feature
deve possuir estado/ViewModel/casos de uso próprios" (seção 5 da regra de higiene). Não é uma
correção pequena o suficiente para fazer oportunisticamente; registrar como dívida arquitetural se
for revisitado.
```

