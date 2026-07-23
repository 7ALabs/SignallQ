# Módulo :featureHome

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/home/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureHome` (alias legado; pasta física `android/feature/home/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

**Correção factual desde a última auditoria (2026-07-16):** este módulo deixou de ser "só a fábrica
do módulo" — hoje contém `ResolvedorMedicaoHome.kt`, uma peça real de domínio. A tela em si
(`HomeScreen.kt`, aba 0/Início) continua residindo em `:app` (`ui/screen/HomeScreen.kt`), não neste
módulo. Namespace declarado: `io.signallq.app.feature.home`.

## Diagrama de componentes

```
:featureHome (io/veloo/ — caminho legado)
FeatureHomeModulo — fábrica estática
ResolvedorMedicaoHome — resolve/seleciona a medição relevante para a Home
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `FeatureHomeModulo.kt` | Object | Fábrica estática do módulo |
| `ResolvedorMedicaoHome.kt` | Resolver | Resolve qual medição/estado exibir na Home a partir dos dados disponíveis |

## Fluxo de dados principal

- **Entradas:** dados de medição/estado de rede consultados pelo resolvedor.
- **Saídas:** resultado de `ResolvedorMedicaoHome` consumido por `:app` (`HomeScreen.kt`).

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`.
- A tela real permanece em `:app`, não neste módulo — a UI da Home não migrou para cá; só a lógica
  de resolução de medição ganhou um lar próprio.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Módulo ainda concentra pouca responsabilidade própria frente ao volume de lógica de Home que continua em `:app` | Convenção "feature deve possuir estado/ViewModel/casos de uso próprios" (seção 5 da higiene) só parcialmente cumprida | Não é correção pequena o suficiente para fazer oportunisticamente — registrar se revisitado |
| `src/test`: **1 arquivo** (era 0) | Cobertura ainda mínima frente ao papel de resolver estado exibido na Home | Ampliar teste ao tocar |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |
