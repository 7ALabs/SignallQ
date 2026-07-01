import { indexedDB as fakeIndexedDb } from 'fake-indexeddb';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { HistoryEntry } from '../shared/contracts';
import {
  HISTORY_DB_NAME,
  createHistoryRepository,
  historyRepository,
  openHistoryDb,
} from '../src/shared/storage/historyRepository';

function deleteDatabase(name: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const request = fakeIndexedDb.deleteDatabase(name);
    request.onsuccess = () => resolve();
    request.onerror = () => reject(request.error);
    request.onblocked = () => reject(new Error('IndexedDB delete blocked.'));
  });
}

function createEntry(id: string, createdAt: string, quality: HistoryEntry['diagnosis']['quality']): HistoryEntry {
  return {
    createdAt,
    diagnosis: {
      actions: [
        {
          category: 'retry',
          description: 'Repita o teste perto do roteador.',
          priority: 1,
          title: 'Testar novamente',
        },
      ],
      confidence: 'medium',
      generatedAt: createdAt,
      id: `diag_${id}`,
      limitations: [{ code: 'http_latency_not_icmp_ping', message: 'Latência HTTP, não ICMP.' }],
      quality,
      source: 'local',
      speed: 'ok',
      stability: 'stable',
      summary: `Resumo ${id}`,
    },
    id,
    speedTest: {
      availability: {
        failedRequests: 0,
        perceivedLossPercent: 0,
        status: 'inferred',
        totalRequests: 10,
      },
      browser: {},
      connection: { source: 'unavailable' },
      download: { bytes: 1_000_000, durationMs: 1000, mbps: 20, samples: 1, status: 'measured' },
      id: `speed_${id}`,
      jitter: { ms: 8, samples: 10, status: 'measured' },
      latency: { method: 'http_timing', ms: 40, samples: 10, status: 'measured' },
      limitations: ['http_latency_not_icmp_ping'],
      measuredAt: createdAt,
      upload: { bytes: 500_000, durationMs: 1000, mbps: 8, samples: 1, status: 'measured' },
    },
  };
}

describe('history repository', () => {
  const originalIndexedDb = globalThis.indexedDB;

  beforeEach(async () => {
    vi.stubGlobal('indexedDB', fakeIndexedDb);
    await deleteDatabase(HISTORY_DB_NAME);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    if (originalIndexedDb !== undefined) {
      vi.stubGlobal('indexedDB', originalIndexedDb);
    }
  });

  it('reports a clear error when IndexedDB is unavailable', async () => {
    vi.stubGlobal('indexedDB', undefined);
    await expect(historyRepository.list()).rejects.toThrow('IndexedDB indisponível neste navegador.');
  });

  it('creates a versioned schema with the expected indexes', async () => {
    const db = await openHistoryDb();
    const transaction = db.transaction('history_entries', 'readonly');
    const store = transaction.objectStore('history_entries');

    expect(db.version).toBe(1);
    expect(Array.from(store.indexNames).sort()).toEqual(['createdAt', 'quality', 'speedStatus', 'stabilityStatus']);

    db.close();
  });

  it('saves, lists, reads, removes and clears local history entries', async () => {
    const repository = createHistoryRepository(openHistoryDb);
    const oldEntry = createEntry('hist_old', '2026-06-27T12:00:00.000Z', 'attention');
    const newEntry = createEntry('hist_new', '2026-06-28T12:00:00.000Z', 'good');

    await repository.save(oldEntry);
    await repository.save(newEntry);

    await expect(repository.list()).resolves.toEqual([newEntry, oldEntry]);
    await expect(repository.getById('hist_old')).resolves.toEqual(oldEntry);

    await repository.remove('hist_old');
    await expect(repository.list()).resolves.toEqual([newEntry]);
    await expect(repository.getById('hist_old')).resolves.toBeNull();

    await repository.clear();
    await expect(repository.list()).resolves.toEqual([]);
  });
});
