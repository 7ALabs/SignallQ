import React, { useEffect, useRef, useState } from 'react';
import { LK, ORB } from '../tokens.js';
import { hexA } from '../utils.js';
import { Icon } from '../primitives/Icon.js';
import { TypeOut } from '../animations/TypeOut.js';
import { Thinking } from '../animations/Thinking.js';

export interface SignallQScreenProps {
  /** Called when the user taps the back button */
  onClose?: () => void;
}

interface Message {
  who: 'ai' | 'user';
  text: string;
  typeIt?: boolean;
}

/** SignallQ AI chat surface — always dark (#0D0D1A). Scripted conversation demo. */
export function SignallQScreen({ onClose }: SignallQScreenProps) {
  const [msgs, setMsgs] = useState<Message[]>([]);
  const [thinking, setThinking] = useState(true);
  const [chips, setChips] = useState<string[] | null>(null);
  const scroller = useRef<HTMLDivElement>(null);

  const push = (m: Message) => setMsgs((x) => [...x, m]);

  useEffect(() => {
    const t1 = setTimeout(() => {
      setThinking(false);
      push({
        who: 'ai',
        typeIt: true,
        text: 'Olá! Analisei sua conexão agora. Sua internet está com ótima velocidade (486 Mbps), mas notei uma pequena oscilação de latência — 24 ms de jitter. Para a maioria das tarefas está excelente.',
      });
    }, 1800);
    return () => clearTimeout(t1);
  }, []);

  useEffect(() => {
    if (scroller.current) scroller.current.scrollTop = scroller.current.scrollHeight;
  });

  const onAiDone = () => setChips(['Por que oscila?', 'Serve para jogos?', 'Como melhorar?']);

  const ask = (q: string) => {
    setChips(null);
    push({ who: 'user', text: q });
    setThinking(true);
    setTimeout(() => {
      setThinking(false);
      const ans =
        q === 'Serve para jogos?'
          ? 'Sim! Com 12 ms de ping e 0% de perda, sua conexão é ótima para jogos online. A oscilação de 24 ms pode causar pequenos picos em jogos muito competitivos, mas nada que atrapalhe a maioria.'
          : q === 'Como melhorar?'
          ? 'Tente trocar o canal do seu Wi-Fi 5GHz para o canal 44, que está livre. Isso reduz a interferência das redes vizinhas e deixa a latência mais estável.'
          : 'A oscilação costuma vir de outros aparelhos usando a rede ao mesmo tempo, ou de interferência no canal Wi-Fi. No seu caso, o canal 161 está congestionado pelas redes vizinhas.';
      push({ who: 'ai', typeIt: true, text: ans });
    }, 1600);
  };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: ORB.bg }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '14px 16px',
          borderBottom: `1px solid ${ORB.surface}`,
          flex: 'none',
        }}
      >
        <button
          onClick={onClose}
          style={{ background: 'none', border: 0, cursor: 'pointer', padding: 4 }}
        >
          <Icon name="arrow_back" size={22} color={ORB.text} />
        </button>
        <div
          style={{
            width: 34,
            height: 34,
            borderRadius: '50%',
            flex: 'none',
            background: `linear-gradient(135deg, ${LK.accent}, ${LK.accentBlue})`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Icon name="auto_awesome" size={18} color="#fff" />
        </div>
        <div>
          <div style={{ font: `600 15px/1.2 ${LK.font}`, color: ORB.text }}>SignallQ</div>
          <div style={{ font: `400 11px/1.2 ${LK.font}`, color: LK.success }}>
            Pronto · analisando sua rede
          </div>
        </div>
      </div>

      {/* Messages */}
      <div
        ref={scroller}
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: 16,
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}
      >
        <div
          style={{
            font: `400 12px/1.5 ${LK.font}`,
            color: ORB.sub,
            textAlign: 'center',
            padding: '4px 20px',
          }}
        >
          O SignallQ analisa seus dados no aparelho. Nada sai do seu celular.
        </div>

        {msgs.map((m, i) =>
          m.who === 'ai' ? (
            <div key={i} style={{ display: 'flex', gap: 10, maxWidth: '90%' }}>
              <div
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: '50%',
                  flex: 'none',
                  marginTop: 2,
                  background: `linear-gradient(135deg, ${LK.accent}, ${LK.accentBlue})`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Icon name="auto_awesome" size={15} color="#fff" />
              </div>
              <div
                style={{
                  background: ORB.card,
                  borderRadius: '4px 16px 16px 16px',
                  padding: '13px 15px',
                  font: `400 14px/1.55 ${LK.font}`,
                  color: ORB.text,
                }}
              >
                {m.typeIt ? (
                  <TypeOut text={m.text} onDone={i === msgs.length - 1 ? onAiDone : undefined} />
                ) : (
                  m.text
                )}
              </div>
            </div>
          ) : (
            <div
              key={i}
              style={{
                alignSelf: 'flex-end',
                maxWidth: '85%',
                background: hexA(LK.accent, 0.18),
                border: `1px solid ${hexA(LK.accent, 0.35)}`,
                borderRadius: '16px 4px 16px 16px',
                padding: '11px 15px',
                font: `400 14px/1.5 ${LK.font}`,
                color: ORB.text,
              }}
            >
              {m.text}
            </div>
          ),
        )}

        {thinking && (
          <div style={{ display: 'flex', gap: 10 }}>
            <div style={{ width: 28 }} />
            <Thinking />
          </div>
        )}

        {chips && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 2, paddingLeft: 38 }}>
            {chips.map((c) => (
              <button
                key={c}
                onClick={() => ask(c)}
                style={{
                  cursor: 'pointer',
                  background: hexA(LK.accent, 0.12),
                  border: `1px solid ${hexA(LK.accent, 0.4)}`,
                  color: LK.accent,
                  borderRadius: 999,
                  padding: '9px 14px',
                  font: `500 13px/1 ${LK.font}`,
                }}
              >
                {c}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Input */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          padding: '12px 16px',
          borderTop: `1px solid ${ORB.surface}`,
          flex: 'none',
        }}
      >
        <div
          style={{
            flex: 1,
            background: ORB.surface,
            borderRadius: 999,
            padding: '12px 16px',
            font: `400 14px/1 ${LK.font}`,
            color: ORB.sub,
          }}
        >
          Pergunte sobre sua conexão…
        </div>
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: '50%',
            background: LK.accent,
            flex: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Icon name="arrow_upward" size={20} color="#fff" />
        </div>
      </div>
      <div
        style={{
          textAlign: 'center',
          font: `400 10px/1 ${LK.font}`,
          color: ORB.sub,
          padding: '0 0 10px',
        }}
      >
        Diagnóstico por IA · on-device
      </div>
    </div>
  );
}
