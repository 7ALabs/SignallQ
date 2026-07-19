// Histórico local — IndexedDB. Nenhuma sincronização entre aparelhos, nenhum
// envio para o servidor (fica só neste navegador). Porte 1:1 de
// shared/history-store.js do protótipo — implementação real já usava
// IndexedDB, não localStorage (README do protótipo mencionava localStorage,
// era o texto que estava desatualizado, não o código).
import type { SpeedTestResult } from './speedEngine'

const DB_NAME = 'signallq-site-history'
const STORE = 'measurements'
const DB_VERSION = 1

export interface MedicaoRegistro {
  id: string
  timestamp: number
  download: number
  upload: number
  latency: number
  jitter: number | null
  connectionType: string | null
  server: string
}

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('indexeddb-unavailable'))
      return
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: 'id' })
        store.createIndex('timestamp', 'timestamp')
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error || new Error('indexeddb-open-failed'))
  })
}

export async function addRecord(record: MedicaoRegistro): Promise<MedicaoRegistro> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).put(record)
    tx.oncomplete = () => resolve(record)
    tx.onerror = () => reject(tx.error)
  })
}

export async function listRecords(): Promise<MedicaoRegistro[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).getAll()
    req.onsuccess = () => resolve(((req.result as MedicaoRegistro[]) || []).sort((a, b) => b.timestamp - a.timestamp))
    req.onerror = () => reject(req.error)
  })
}

export async function deleteRecord(id: string): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export async function clearAll(): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).clear()
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export function resultToRecord(result: SpeedTestResult): MedicaoRegistro {
  return {
    id: result.id,
    timestamp: result.timestamp,
    download: result.download.mbps,
    upload: result.upload.mbps,
    latency: result.latency.ms,
    jitter: result.jitter ? result.jitter.ms : null,
    connectionType: result.connectionType,
    server: result.server,
  }
}
