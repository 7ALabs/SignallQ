import '@testing-library/jest-dom/vitest'
import { afterEach, beforeEach, vi } from 'vitest'

// Nenhum teste deve depender de rede real. `PageLayout` (reconstrução v2) renderiza
// `AdRail`/`AdBannerWide` em toda página institucional, que buscam o catálogo de
// anúncios locais via `fetch` mesmo sem mock explícito no teste — sem este default,
// esse fetch bateria no endpoint real de produção. Testes que precisam de um
// catálogo específico continuam livres para sobrescrever com seu próprio `vi.spyOn`.
beforeEach(() => {
  vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: [] }), { status: 200 }))
})

afterEach(() => {
  vi.restoreAllMocks()
})
