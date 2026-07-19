// Tipos para o módulo virtual `virtual:pwa-register/react` exposto pelo
// `vite-plugin-pwa`. O `tsconfig.json` do Site não referencia
// `vite-plugin-pwa/client` automaticamente, então este arquivo cobre o
// subset usado em `src/components/PwaUpdatePrompt.tsx`.
// Porte 1:1 do repo antecessor `linka-speedtest` (src/types/pwa.d.ts).

declare module 'virtual:pwa-register/react' {
  import type { Dispatch, SetStateAction } from 'react'

  export interface RegisterSWOptions {
    immediate?: boolean
    onNeedRefresh?: () => void
    onOfflineReady?: () => void
    onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void
    onRegisterError?: (error: unknown) => void
  }

  export function useRegisterSW(options?: RegisterSWOptions): {
    needRefresh: [boolean, Dispatch<SetStateAction<boolean>>]
    offlineReady: [boolean, Dispatch<SetStateAction<boolean>>]
    updateServiceWorker: (reloadPage?: boolean) => Promise<void>
  }
}
