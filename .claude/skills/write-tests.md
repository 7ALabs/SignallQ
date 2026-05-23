---
name: write-tests
description: Escrever testes automatizados para uma feature ou módulo específico do Linka. Use quando testes estiverem faltando após implementação ou quando Gema reprovar por ausência de cobertura. Acione com `/write-tests <escopo>`.
---

## Objetivo

Escrever testes automatizados para o módulo ou feature especificado. Cobre Android (JUnit5, MockK, Compose Testing) e PWA (Vitest, Testing Library).

## Fluxo de execução

1. **Identifique o escopo** — feature, módulo, ViewModel, UseCase, hook ou arquivo específico
2. **Classifique por plataforma e tamanho:**

| Cenário | Agente |
|---|---|
| Android, ≤5 arquivos de teste | Marcelo (sob supervisão de Camilo) |
| Android, feature completa | Camilo |
| PWA, ≤5 arquivos de teste | Marcelo (sob supervisão de Renan) |
| PWA, feature completa | Renan |
| Android + PWA simultâneo | Camilo + Renan em paralelo |

3. **Passe o contexto completo ao agente** — arquivos de implementação relevantes, comportamento esperado, estados críticos

## Tipos de teste por plataforma

### Android (`linkaAndroidKotlin/`)

- **Unit** — ViewModels (com `TestDispatcher`), UseCases, Engines de diagnóstico (JUnit5 + MockK)
- **Integration** — DAOs com Room in-memory, fluxo completo de diagnóstico
- **UI** — Composables críticos com `ComposeTestRule`, estados: loading / erro / sucesso / vazio

### PWA (`linkaSpeedtestPwa/`)

- **Unit** — hooks (`renderHook`), utilitários, lógica de cálculo de velocidade (Vitest)
- **Component** — componentes React com Testing Library (`render`, `screen`, `userEvent`)
- **Integration** — fluxo de speedtest com `msw` para mock de API

## Cobertura mínima obrigatória

- Cada estado de UI mapeado (loading, erro, sucesso, vazio, thinking) → pelo menos 1 teste
- Cada UseCase ou hook com lógica de negócio → testes unitários
- Cada integração com API externa → mock testado de sucesso e falha
- Edge cases de diagnóstico (sem conexão, timeout, sinal fraco) → cobertos

## Formato de entrega do agente

O agente responsável deve reportar:
1. **Arquivos de teste criados** — caminhos completos
2. **Casos cobertos** — lista de cenários testados
3. **Casos não cobertos** — o que ficou de fora e por quê
4. **Comando para rodar** — ex: `./gradlew :featureWifi:test` ou `npm run test`
5. **Resultado da execução** — todos passando antes de fechar
