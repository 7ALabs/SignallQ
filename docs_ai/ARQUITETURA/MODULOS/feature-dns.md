# Módulo :featureDns

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/dns/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/dns/`
- **Namespace:** `io.signallq.app.feature.dns`

## Responsabilidade

Benchmark de provedores DNS via DoH e recomendação de configuração.

## Principais packages/pastas

Base: `feature/dns/src/main/kotlin/io/veloo/app/kotlin/feature/dns/` (caminho físico legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BenchmarkDns.kt` (interface) / `BenchmarkDnsDoh.kt` (impl) | Serviço | Benchmark de 7 provedores DNS via DoH |
| `SnapshotBenchmarkDns.kt` / `ResultadoBenchmarkDns.kt` | Data class | Estado e resultado do benchmark |
| `EstadoBenchmarkDns.kt` | Enum | Estados do benchmark |
| `AvaliadorCoerenciaDns.kt` | Engine | Verifica coerência das respostas DNS |
| `OrientadorConfiguracaoDns.kt` | Engine | Recomenda configuração de DNS |
| `FeatureDnsModulo.kt` | Object | Fábrica estática |

## Entradas/saídas

- **Entradas:** requisições DoH aos 7 provedores DNS configurados.
- **Saídas:** `SnapshotBenchmarkDns` consumido por `:app`.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`, `okhttp`,
`timber`.

## Consumidores

Via grep de `project(":featureDns")`: apenas `:app`.

## Testes existentes

`src/test`: **1 arquivo**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Cobertura de teste baixa (1 arquivo) para uma feature com 8 classes de domínio e chamadas HTTP reais
a 7 provedores. Caminho físico `io/veloo` diverge do package — dívida 4.1.
```

