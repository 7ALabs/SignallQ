import { afterEach, describe, expect, it, vi } from 'vitest';
import { IDBFactory } from 'fake-indexeddb';
import type { HistoryEntry } from '../shared/contracts';
import { historyRepository } from '../src/shared/storage/historyRepository';

function createEntry(id: string, createdAt: string, quality: HistoryEntry['diagnosis']['quality']): HistoryEntry {
  return {
    createdAt,
    diagnosis: {
      actions: [],
      confidence: 'high',
      generatedAt: createdAt,
      id: `diag_${id}`,
      limitations: [],
      quality,
      source: 'local',
      speed: 'fast',
      stability: 'stable',
      summary: `Diagnóstico ${id}`,
    },
    id,
    speedTest: {
      availability: {
        failedRequests: 0,
        perceivedLossPercent: 0,
        status: 'inferred',
        totalRequests: 17,
      },
      browser: {},
      connection: { source: 'unavailable' },
      download: { bytes: 1_000_000, durationMs: 1000, mbps: 8, samples: 1, status: 'measured' },
      id: `speed_${id}`,
      jitter: { ms: 4, samples: 15, status: 'measured' },
      latency: { method: 'http_timing', ms: 20, samples: 15, status: 'measured' },
      limitations: ['http_latency_not_icmp_ping'],
      measuredAt: createdAt,
      upload: { bytes: 500_000, durationMs: 1000, mbps: 4, samples: 1, status: 'measured' },
    },
  };
}

describe('history repository', () => {
  const originalIndexedDb = globalThis.indexedDB;

  afterEach(() => {
    vi.unstubAllGlobals();
    if (originalIndexedDb !== undefined) {
      vi.stubGlobal('indexedDB', originalIndexedDb);
    }
  });

  it('saves, lists, reads, removes and clears local history entries in IndexedDB', async () => {
    vi.stubGlobal('indexedDB', new IDBFactory());

    const older = createEntry('older', '2026-06-29T10:00:00.000Z', 'attention');
    const newer = createEntry('newer', '2026-06-29T11:00:00.000Z', 'good');

    await historyRepository.save(older);
    await historyRepository.save(newer);

    await expect(historyRepository.list()).resolves.toMatchObject([{ id: 'newer' }, { id: 'older' }]);
    await expect(historyRepository.getById('older')).resolves.toMatchObject({ diagnosis: { quality: 'attention' } });

    await historyRepository.remove('older');
    await expect(historyRepository.getById('older')).resolves.toBeNull();
    await expect(historyRepository.list()).resolves.toMatchObject([{ id: 'newer' }]);

    await historyRepository.clear();
    await expect(historyRepository.list()).resolves.toEqual([]);
  });

  it('reports a clear error when IndexedDB is unavailable', async () => {
    vi.stubGlobal('indexedDB', undefined);

    await expect(historyRepository.list()).rejects.toThrow('IndexedDB indisponível neste navegador.');
  });
});
