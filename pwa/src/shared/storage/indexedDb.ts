export interface IndexedDbConfig {
  databaseName: string;
  stores: Array<{
    indexes?: Array<{
      keyPath: string;
      name: string;
      unique?: boolean;
    }>;
    keyPath: string;
    name: string;
  }>;
  version: number;
}

export function createStorageError(message: string, cause?: unknown): Error {
  const detail = cause instanceof Error ? ` ${cause.message}` : '';
  return new Error(`${message}${detail}`);
}

export function openIndexedDb(config: IndexedDbConfig): Promise<IDBDatabase> {
  if (typeof indexedDB === 'undefined') {
    return Promise.reject(createStorageError('IndexedDB indisponível neste navegador.'));
  }

  return new Promise((resolve, reject) => {
    const request = indexedDB.open(config.databaseName, config.version);

    request.onupgradeneeded = () => {
      const db = request.result;
      for (const storeConfig of config.stores) {
        const store = db.objectStoreNames.contains(storeConfig.name)
          ? request.transaction?.objectStore(storeConfig.name)
          : db.createObjectStore(storeConfig.name, { keyPath: storeConfig.keyPath });

        if (!store) continue;

        for (const index of storeConfig.indexes ?? []) {
          if (!store.indexNames.contains(index.name)) {
            store.createIndex(index.name, index.keyPath, { unique: index.unique ?? false });
          }
        }
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(createStorageError('Falha ao abrir banco local.', request.error));
  });
}

export function runIndexCursorRead<T>(
  openDatabase: () => Promise<IDBDatabase>,
  storeName: string,
  indexName: string,
  direction: IDBCursorDirection,
  limit?: number,
): Promise<T[]> {
  return openDatabase().then(
    (db) =>
      new Promise<T[]>((resolve, reject) => {
        const results: T[] = [];
        const transaction = db.transaction(storeName, 'readonly');
        const request = transaction.objectStore(storeName).index(indexName).openCursor(null, direction);

        request.onsuccess = () => {
          const cursor = request.result;
          if (cursor && (limit == null || results.length < limit)) {
            results.push(cursor.value as T);
            cursor.continue();
          }
        };
        request.onerror = () => reject(createStorageError('Falha ao acessar armazenamento local.', request.error));

        transaction.oncomplete = () => {
          db.close();
          resolve(results);
        };
        transaction.onerror = () => {
          db.close();
          reject(createStorageError('Falha na transação do armazenamento local.', transaction.error));
        };
      }),
  );
}

export function runObjectStoreOperation<T>(
  openDatabase: () => Promise<IDBDatabase>,
  storeName: string,
  mode: IDBTransactionMode,
  operation: (store: IDBObjectStore) => IDBRequest<T> | void,
): Promise<T | void> {
  return openDatabase().then(
    (db) =>
      new Promise<T | void>((resolve, reject) => {
        const transaction = db.transaction(storeName, mode);
        const store = transaction.objectStore(storeName);
        const request = operation(store);
        let requestResult: T | void;

        if (request) {
          request.onsuccess = () => {
            requestResult = request.result;
          };
          request.onerror = () => reject(createStorageError('Falha ao acessar armazenamento local.', request.error));
        }

        transaction.oncomplete = () => {
          db.close();
          resolve(requestResult);
        };
        transaction.onerror = () => {
          db.close();
          reject(createStorageError('Falha na transação do armazenamento local.', transaction.error));
        };
      }),
  );
}
