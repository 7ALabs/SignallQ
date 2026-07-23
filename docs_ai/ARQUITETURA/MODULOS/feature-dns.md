# Módulo :featureDns

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/dns/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureDns` (alias legado; pasta física `android/feature/dns/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Benchmark de provedores DNS via DoH e recomendação de configuração. Namespace declarado:
`io.signallq.app.feature.dns`.

## Diagrama de componentes

```
:featureDns (io/veloo/ — caminho legado)
BenchmarkDns (interface) → BenchmarkDnsDoh (impl, 7 provedores)
    ├── AvaliadorCoerenciaDns — verifica coerência das respostas
    ├── AvaliadorRecomendacaoDns / OrientadorConfiguracaoDns — recomenda configuração
    └── DetectorEnderecoIpPrivado — filtra respostas de IP privado/inválidas
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `BenchmarkDns.kt` (interface) / `BenchmarkDnsDoh.kt` (impl) | Serviço | Benchmark de 7 provedores DNS via DoH |
| `SnapshotBenchmarkDns.kt` / `ResultadoBenchmarkDns.kt` | Data class | Estado e resultado do benchmark |
| `EstadoBenchmarkDns.kt` | Enum | Estados do benchmark |
| `AvaliadorCoerenciaDns.kt` | Engine | Verifica coerência das respostas DNS |
| `AvaliadorRecomendacaoDns.kt` / `OrientadorConfiguracaoDns.kt` | Engine | Recomenda configuração de DNS |
| `DetectorEnderecoIpPrivado.kt` | Detector | Identifica resposta DNS apontando para IP privado (sinal de redirecionamento/portal cativo) |
| `FeatureDnsModulo.kt` | Object | Fábrica estática |

## Fluxo de dados principal

- **Entradas:** requisições DoH aos 7 provedores DNS configurados.
- **Saídas:** `SnapshotBenchmarkDns` consumido por `:app`.

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`,
  `kotlinx-coroutines-android`, `okhttp`, `timber`.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Cobertura de teste ainda baixa (`src/test`: **3 arquivos**, cresceu frente a 1) para uma feature com 10 classes de domínio e chamadas HTTP reais a 7 provedores | Regressão silenciosa em benchmark/recomendação | Ampliar teste ao tocar |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |
