import React, { useEffect, useState } from 'react';

export interface TypeOutProps {
  /** Full text to type out */
  text: string;
  /** Milliseconds per character */
  speed?: number;
  /** Called when the full text has been rendered */
  onDone?: () => void;
}

/** Character-by-character typewriter animation used in SignallQ AI responses. */
export function TypeOut({ text, speed = 18, onDone }: TypeOutProps) {
  const [n, setN] = useState(0);

  useEffect(() => {
    if (n >= text.length) {
      onDone?.();
      return;
    }
    const t = setTimeout(() => setN(n + 1), speed);
    return () => clearTimeout(t);
  }, [n, text, speed, onDone]);

  return (
    <span>
      {text.slice(0, n)}
      {n < text.length && <span style={{ opacity: 0.6 }}>▍</span>}
    </span>
  );
}
