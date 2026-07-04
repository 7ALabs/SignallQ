import type { HistoryEntry, HistoryRepository } from '@shared/contracts';
import { createStorageError, openIndexedDb, runIndexCursorRead, runObjectStoreOperation } from './indexedDb';

export const HISTORY_DB_NAME = 'signallq-pwa';
export const HISTORY_DB_VERSION = 1;
const STORE_NAME = 'history_entries';

export function openHistoryDb(): Promise<IDBDatabase> {
  return openIndexedDb({
    databaseName: HISTORY_DB_NAME,
    version: HISTORY_DB_VERSION,
    stores: [
      {
        keyPath: 'id',
        name: STORE_NAME,
        indexes: [
          { keyPath: 'createdAt', name: 'createdAt' },
          { keyPath: 'diagnosis.quality', name: 'quality' },
          { keyPath: 'diagnosis.speed', name: 'speedStatus' },
          { keyPath: 'diagnosis.stability', name: 'stabilityStatus' },
        ],
      },
    ],
  }).catch((error) => {
    throw createStorageError('Falha ao abrir histórico local.', error);
  });
}

export function createHistoryRepository(openDatabase: () => Promise<IDBDatabase> = openHistoryDb): HistoryRepository {
  const runStoreOperation = <T>(mode: IDBTransactionMode, operation: (store: IDBObjectStore) => IDBRequest<T> | void) =>
    runObjectStoreOperation(openDatabase, STORE_NAME, mode, operation).catch((error) => {
      throw createStorageError('Falha no histórico local.', error);
    });

  return {
    async save(entry) {
      await runStoreOperation('readwrite', (store) => store.put(entry));
    },

    async list() {
      return runIndexCursorRead<HistoryEntry>(openDatabase, STORE_NAME, 'createdAt', 'prev').catch((error) => {
        throw createStorageError('Falha no histórico local.', error);
      });
    },

    async getById(id) {
      return (await runStoreOperation<HistoryEntry | undefined>('readonly', (store) => store.get(id))) ?? null;
    },

    async remove(id) {
      await runStoreOperation('readwrite', (store) => store.delete(id));
    },

    async clear() {
      await runStoreOperation('readwrite', (store) => store.clear());
    },
  };
}

export const historyRepository: HistoryRepository = createHistoryRepository();
