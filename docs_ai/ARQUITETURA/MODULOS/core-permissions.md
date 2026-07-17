# Módulo :corePermissions

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/core/permissions/build.gradle.kts`, código real)
- **Caminho físico:** `android/core/permissions/`
- **Namespace:** `io.signallq.app.core.permissions`

## Responsabilidade

Contrato e estado de permissões de rede em runtime (localização para scan Wi-Fi, telefonia para
sinal móvel, etc.).

## Principais packages/pastas

Base: `corePermissions/src/main/kotlin/io/veloo/app/kotlin/core/permissions/` (caminho físico
legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `GerenciadorPermissoesRede.kt` | Interface | Contrato de gerenciamento de permissões |
| `SnapshotPermissoesRede.kt` | Data class | Estado atual das permissões |

## Entradas/saídas

- **Entradas:** resultado de solicitações de permissão do Android (`ActivityResultContracts`,
  chamado a partir de `:app`).
- **Saídas:** `SnapshotPermissoesRede` consumido por `:app` para decidir quais telas/recursos
  habilitar.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`.

## Consumidores

Via grep de `project(":corePermissions")`: **apenas `:app`**. Nenhuma feature declara dependência
direta — a implementação concreta e o wiring de permissões concentram-se em `:app`.

## Testes existentes

`src/test`: **0 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Módulo minimalista (2 arquivos fonte) sem nenhum teste. Caminho físico `io/veloo` diverge do
package — dívida 4.1.
```

