// Lista de espera dos modais "avisar quando lançar" (EmailCaptureDialog) —
// GH#1155. Chama o proxy server-side (functions/api/waitlist.ts), que repassa
// pro signallq-admin-worker (POST /ingest/waitlist) guardando a INGEST_KEY
// como secret — nunca exposta ao navegador.
//
// Fire-and-forget de propósito, mesma regra da telemetria (lib/telemetry.ts):
// falha de rede aqui nunca pode quebrar a experiência de quem preenche o
// formulário. O worker sempre responde sucesso mesmo em duplicata (idempotente
// via UNIQUE(email, product) + INSERT OR IGNORE), então não há retorno de
// estado real pro caller — a UI já assume sucesso otimista.
import { WAITLIST_ENDPOINT } from './config'

export type WaitlistProduct = 'signallq' | 'pro'

export function submitWaitlistSignup(email: string, product: WaitlistProduct, sourcePage: string): void {
  if (typeof fetch === 'undefined') return
  fetch(WAITLIST_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, product, source_page: sourcePage }),
  }).catch(() => {
    // mesma regra da telemetria: falha de rede aqui nunca quebra a experiência do usuário.
  })
}
