# Módulo :featureSettings

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/settings/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureSettings` (alias legado; pasta física `android/feature/settings/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

**Correção factual relevante desde a última auditoria (2026-07-16):** este módulo deixou de ser "só
a fábrica do módulo, praticamente vazio" — hoje concentra validadores e resolvedores reais de
domínio (perfil de conexão, tema, cidade/UF, velocidade contratada), com 5 arquivos de teste
próprios. A tela em si (`AjustesScreen.kt`, overlay Perfil/Ajustes, 803 linhas — ver dívida 4.4 da
regra de higiene) continua residindo em `:app`. Namespace declarado: `io.signallq.app.feature.settings`.

## Diagrama de componentes

```
:featureSettings (io/veloo/ — caminho legado)
FeatureSettingsModulo — fábrica estática
├── ConnectionProfile — modelo de perfil de conexão
├── DetectorDivergenciaPerfilConexao — detecta divergência entre perfil informado e real
├── ResolvedorNetworkId — resolve identificador de rede
├── ThemePreference — preferência de tema (claro/escuro)
├── ValidadorCidadeUf — validação de cidade/UF informados pelo usuário
└── ValidadorVelocidadeContratada — validação de velocidade contratada informada pelo usuário
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `FeatureSettingsModulo.kt` | Object | Fábrica estática do módulo |
| `ConnectionProfile.kt` | Data class | Modelo de perfil de conexão do usuário |
| `DetectorDivergenciaPerfilConexao.kt` | Detector | Detecta divergência entre o perfil de conexão informado e o observado na rede |
| `ResolvedorNetworkId.kt` | Resolver | Resolve identificador estável de rede (para persistir preferências por rede) |
| `ThemePreference.kt` | Enum/Data class | Preferência de tema do app |
| `ValidadorCidadeUf.kt` | Validador | Valida cidade/UF informados nos Ajustes |
| `ValidadorVelocidadeContratada.kt` | Validador | Valida a velocidade contratada informada nos Ajustes |

## Fluxo de dados principal

- **Entradas:** dados informados pelo usuário no overlay Ajustes/Perfil (`:app`), estado de rede
  observado.
- **Saídas:** validação/resolução consumida por `:app` (`AjustesScreen.kt`) ao persistir preferência
  via `:coreDatastore`.

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`.
- A UI (`AjustesScreen.kt`) permanece em `:app` — o mesmo padrão de `:featureHome`: lógica de
  domínio ganhou módulo próprio, tela continua centralizada em `:app`.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| `AjustesScreen.kt` em `:app` já é dívida crítica de tamanho (803 linhas, seção 4.4 da higiene) — este módulo tem os validadores mas não a composição da UI | Lógica e UI vivem em módulos diferentes, dificultando visão completa do fluxo de Ajustes | Ao extrair sheets de `AjustesScreen.kt`, considerar se algum validador aqui deveria virar use case explícito consumido pela sheet |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |

`src/test`: **5 arquivos** (era 0 — módulo ganhou cobertura real junto com a lógica de domínio).
`src/androidTest`: 0.
