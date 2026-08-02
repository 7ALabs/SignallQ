# Módulo :corePermissions

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/core/permissions/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:corePermissions` (alias legado; pasta física `android/core/permissions/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Contrato e estado de permissões de rede em runtime (localização para scan Wi-Fi, telefonia para
sinal móvel, etc.). Namespace declarado: `io.signallq.app.core.permissions`.

## Diagrama de componentes

```
:corePermissions (io/veloo/ — caminho legado)
GerenciadorPermissoesRede (interface) → GerenciadorPermissoesRedeAndroid (impl)
    ↳ SnapshotPermissoesRede (estado), EstadoPermissao (enum), LocationPermissionHelper (utilitário)
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `GerenciadorPermissoesRede.kt` | Interface | Contrato de gerenciamento de permissões |
| `GerenciadorPermissoesRedeAndroid.kt` | Implementação | Implementação real via APIs Android |
| `SnapshotPermissoesRede.kt` | Data class | Estado atual das permissões |
| `EstadoPermissao.kt` | Enum | Estados possíveis de uma permissão |
| `LocationPermissionHelper.kt` | Utilitário | Apoio à checagem/solicitação de permissão de localização |

## Fluxo de dados principal

- **Entradas:** resultado de solicitações de permissão do Android (`ActivityResultContracts`,
  chamado a partir de `:app`).
- **Saídas:** `SnapshotPermissoesRede` consumido por `:app` para decidir quais telas/recursos
  habilitar.

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`.
- **Apenas `:app` consome este módulo** (confirmado via grep de `project(":corePermissions")`) —
  nenhuma feature declara dependência direta; a implementação concreta e o wiring de permissões
  concentram-se em `:app`.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Módulo pequeno (5 arquivos) sem nenhum teste (`src/test`: 0) | Regra de permissão sem cobertura automatizada | Adicionar teste ao tocar `GerenciadorPermissoesRedeAndroid` |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |
