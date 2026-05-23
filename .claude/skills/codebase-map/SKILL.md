---
description: Gera um mapa estruturado dos módulos, arquivos e símbolos relevantes para uma feature ou área do código. Executada por Marcelo (Haiku) antes de qualquer agente Sonnet explorar o repositório.
---

## Quando usar
Antes de planejamento técnico, impact map ou implementação. Sempre Marcelo primeiro.

## Passos
1. **Identificar o escopo**: qual feature, módulo ou área está sendo mapeada.
2. **Listar módulos Android afetados** (`linkaAndroidKotlin/`):
   - Glob por módulo relevante (`:featureX`, `:coreY`).
   - Listar arquivos principais: ViewModel, UseCase, Repository, DAO, Screen.
3. **Listar arquivos PWA afetados** (`linkaSpeedtestPwa/src/`):
   - Glob por componente, hook ou serviço relevante.
4. **Identificar contratos públicos**: interfaces, APIs, DAOs, states.
5. **Resumir** em lista compacta com caminhos absolutos reais.

## Output esperado
```
Mapa de código — [área]:
Android:
  - linkaAndroidKotlin/featureX/.../XViewModel.kt
  - linkaAndroidKotlin/featureX/.../XScreen.kt
  - linkaAndroidKotlin/coreY/.../YRepository.kt

PWA:
  - linkaSpeedtestPwa/src/hooks/useX.ts
  - linkaSpeedtestPwa/src/components/XCard.tsx
```

## Limites
- Não interpretar o código — só listar e localizar.
- Não sugerir implementação — isso é do Camilo/Renan.
- Mapa deve ser lido em <30 segundos por um agente Sonnet.
