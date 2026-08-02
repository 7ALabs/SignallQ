import '@testing-library/jest-dom/vitest'
import { afterEach, beforeEach, vi } from 'vitest'

// Nenhum teste deve depender de rede real — default seguro para qualquer `fetch`
// não mockado explicitamente (histórico, telemetria etc.). Testes que precisam de
// uma resposta específica continuam livres para sobrescrever com seu próprio `vi.spyOn`.
beforeEach(() => {
  vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: [] }), { status: 200 }))
})

afterEach(() => {
  vi.restoreAllMocks()
})
