import { useEffect, useState } from 'react';
import { ConnectionSnapshot, ConnectionStatus } from '@/types/network';

interface NavigatorConnection {
  effectiveType?: string;
  downlink?: number;
  addEventListener?: (type: 'change', listener: () => void) => void;
  removeEventListener?: (type: 'change', listener: () => void) => void;
}

function readConnectionSnapshot(): ConnectionSnapshot {
  const connection = 'connection' in navigator ? (navigator.connection as NavigatorConnection | undefined) : undefined;

  return {
    status: navigator.onLine ? ConnectionStatus.Online : ConnectionStatus.Offline,
    effectiveType: connection?.effectiveType ?? null,
    downlinkMbps: typeof connection?.downlink === 'number' ? connection.downlink : null,
    browserSupportsNetworkInfo: Boolean(connection),
  };
}

export function useConnectionSnapshot(): ConnectionSnapshot {
  const [snapshot, setSnapshot] = useState<ConnectionSnapshot>(() => readConnectionSnapshot());

  useEffect(() => {
    const update = () => setSnapshot(readConnectionSnapshot());
    const connection =
      'connection' in navigator ? (navigator.connection as NavigatorConnection | undefined) : undefined;

    window.addEventListener('online', update);
    window.addEventListener('offline', update);
    connection?.addEventListener?.('change', update);

    return () => {
      window.removeEventListener('online', update);
      window.removeEventListener('offline', update);
      connection?.removeEventListener?.('change', update);
    };
  }, []);

  return snapshot;
}
