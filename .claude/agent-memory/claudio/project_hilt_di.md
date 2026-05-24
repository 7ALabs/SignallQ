---
name: project-hilt-di
description: Estado da migração Hilt na issue #3 — DI manual do MainViewModel para @HiltViewModel
metadata:
  type: project
---

Issue #3: migrar DI manual do MainViewModel para Hilt com @HiltViewModel.

Branch: `refactor/3-hilt-di` (criada 2026-05-24, em andamento com Camilo)

Estado do código no momento do planejamento:
- Hilt: zero no projeto. Nenhuma dependência, nenhum plugin, nenhum @HiltAndroidApp.
- kapt declarado no root build.gradle.kts (2.2.20) mas não aplicado no app/build.gradle.kts.
- MainViewModel estende AndroidViewModel(application) — precisa virar ViewModel + @HiltViewModel.
- Factories ficam nos módulos core/feature como objetos Kotlin (não existem *Modulo.kt em app/src/).
- Nenhum teste instancia MainViewModel diretamente.

Risco principal: OrbitOrchestrator recebe viewModelScope no construtor lazy — com Hilt, avaliar @AssistedInject ou criação no init {}.
Risco secundário: getApplication() em MainViewModel perde acesso após remover AndroidViewModel — injetar Application via @ApplicationContext.

**Why:** refactor de DI para reduzir acoplamento e facilitar testabilidade futura.
**How to apply:** ao retomar a issue, verificar se branch ainda está ativa e se Camilo já implementou os passos 1-2 antes de tocar no MainViewModel.
