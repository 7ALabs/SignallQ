/* SignallQ UI Kit — Velocidade flow com card de anúncio nativo (issue #555)
   Baseado em ui_kits/android/speedtest.jsx (SpeedIdle → SpeedRunning → Resultado). */

function SpeedFlow({ go, adsEnabled, weakSignal }) {
  const [phase, setPhase] = React.useState('idle'); // idle | running | result
  const [mode, setMode] = React.useState('Completo');

  if (phase === 'idle') return <SpeedIdle mode={mode} setMode={setMode} onStart={() => setPhase('running')}
    adsEnabled={adsEnabled} />;
  if (phase === 'running') return <SpeedRunning onDone={() => setPhase('result')} />;
  return <Resultado onAgain={() => setPhase('idle')} onHome={() => go('home')} onSignallq={() => go('signallq')}
    adsEnabled={adsEnabled} weakSignal={weakSignal} />;
}

function SpeedIdle({ mode, setMode, onStart, adsEnabled }) {
  const modes = ['Rápido','Completo','Triplo'];
  return (
    <div style={{ flex:1, display:'flex', flexDirection:'column', background:LK.bgPrimary,
      padding:'24px 20px', overflowY:'auto' }}>
      <div style={{ display:'flex', alignItems:'center', justifyContent:'center', flex:'none', marginTop:8 }}>
        <button onClick={onStart} style={{ width:230, height:230, borderRadius:'50%', border:0, cursor:'pointer',
          background:LK.accent, color:'#fff', font:`700 30px/1 ${LK.font}`,
          boxShadow:`0 0 0 10px ${hexA(LK.accent,.18)}` }}>Iniciar</button>
      </div>
      {/* mode selector */}
      <div style={{ display:'flex', background:LK.bgSecondary, borderRadius:999, padding:3, marginTop:34 }}>
        {modes.map(m => {
          const on = m===mode;
          return <button key={m} onClick={()=>setMode(m)} style={{ flex:1, border:0, cursor:'pointer',
            padding:'12px 0', borderRadius:999, font:`600 14px/1 ${LK.font}`,
            background: on?LK.bgCard:'transparent', color: on?LK.textPrimary:LK.textSecondary,
            boxShadow: on?'0 1px 3px rgba(0,0,0,.14)':'none' }}>{m}</button>;
        })}
      </div>
      {/* last result */}
      <div style={{ background:LK.bgSecondary, borderRadius:LK.rCard, padding:16, marginTop:18 }}>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'baseline', marginBottom:14 }}>
          <span style={{ font:`600 11px/1.3 ${LK.font}`, color:LK.textTertiary, letterSpacing:'.4px',
            textTransform:'uppercase' }}>Último resultado</span>
          <span style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary }}>há 2 h</span>
        </div>
        <div style={{ display:'flex' }}>
          {[['Download','486',LK.success],['Upload','212',LK.accent],['Latência','12',LK.success]].map(([l,v,c])=>(
            <div key={l} style={{ flex:1 }}>
              <div style={{ font:`400 10px/1 ${LK.font}`, color:LK.textTertiary, marginBottom:4 }}>{l}</div>
              <div style={{ font:`700 20px/1 ${LK.font}`, color:c }}>{v}<span style={{ font:`400 10px/1 ${LK.font}`, color:LK.textSecondary, marginLeft:2 }}>{l==='Latência'?'ms':'Mbps'}</span></div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Slot de anúncio nativo — espaço vazio abaixo do Último resultado ──
          Presença padrão da tela (issue #555): fallback genérico do AdMob, não
          depende de evidência de diagnóstico — só depende do Remote Config. */}
      {adsEnabled && (
        <NativeAdRow
          source="admob"
          brandLetter="T"
          brandColor={LK.accentBlue}
          headline="TP-Link Archer AXE — Wi-Fi 6E"
          body="Para quem já testa a rede com frequência"
          onCta={() => {}}
        />
      )}
    </div>
  );
}

function SpeedRunning({ onDone }) {
  const phases = [
    { key:'LATÊNCIA', tone:LK.phaseLatencia ?? '#60A5FA', unit:'ms', target:12 },
    { key:'DOWN', tone:'#34D399', unit:'Mbps', target:486 },
    { key:'UP', tone:'#FBBF24', unit:'Mbps', target:212 },
  ];
  const [pi, setPi] = React.useState(0);
  const [val, setVal] = React.useState(0);
  const [prog, setProg] = React.useState(0);
  const [done, setDone] = React.useState([]);

  React.useEffect(() => {
    let raf, start = performance.now();
    const dur = 1500;
    const cur = phases[pi];
    const tick = (t) => {
      const e = Math.min(1, (t-start)/dur);
      setVal(Math.round(cur.target * e * (0.85 + Math.random()*0.3)));
      setProg(((pi + e) / 3));
      if (e < 1) { raf = requestAnimationFrame(tick); }
      else {
        setVal(cur.target);
        setDone(d => [...d, cur.key]);
        if (pi < 2) setTimeout(() => setPi(pi+1), 350);
        else setTimeout(onDone, 600);
      }
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [pi]);

  const cur = phases[pi];
  const R = 100, C = 2*Math.PI*R;
  return (
    <div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center',
      background:LK.bgPrimary, padding:'40px 20px' }}>
      <div style={{ position:'relative', width:240, height:240 }}>
        <svg width="240" height="240" style={{ transform:'rotate(-90deg)' }}>
          <circle cx="120" cy="120" r={R} fill="none" stroke={LK.bgSecondary} strokeWidth="14" />
          <circle cx="120" cy="120" r={R} fill="none" stroke={cur.tone} strokeWidth="14" strokeLinecap="round"
            strokeDasharray={C} strokeDashoffset={C*(1-prog)} style={{ transition:'stroke-dashoffset .1s linear' }} />
        </svg>
        <div style={{ position:'absolute', inset:0, display:'flex', flexDirection:'column',
          alignItems:'center', justifyContent:'center' }}>
          <div style={{ font:`700 48px/1 ${LK.font}`, color:LK.textPrimary }}>{val}</div>
          <div style={{ font:`400 14px/1 ${LK.font}`, color:LK.textSecondary, marginTop:4 }}>{cur.unit}</div>
          <div style={{ font:`600 12px/1 ${LK.font}`, color:cur.tone, marginTop:10, letterSpacing:'.5px' }}>{cur.key}</div>
        </div>
      </div>
      <div style={{ display:'flex', gap:8, marginTop:40 }}>
        {[...phases.map(p=>p.key),'CONCLUÍDO'].map(k => {
          const ok = done.includes(k);
          const active = cur.key===k;
          return <div key={k} style={{ display:'flex', alignItems:'center', gap:5,
            padding:'7px 12px', borderRadius:999, font:`600 11px/1 ${LK.font}`,
            background: ok?hexA(LK.success,.12): active?hexA(LK.accent,.12):LK.bgSecondary,
            color: ok?LK.success: active?LK.accent:LK.textTertiary }}>
            {ok && <Icon name="check" size={13} color={LK.success} />}{k}</div>;
        })}
      </div>
      <div style={{ marginTop:'auto', display:'flex', alignItems:'center', gap:8, color:LK.textTertiary }}>
        <Icon name="dns" size={16} color={LK.textTertiary} />
        <span style={{ font:`400 12px/1 ${LK.font}` }}>Servidor: São Paulo · Claro</span>
      </div>
    </div>
  );
}

function Resultado({ onAgain, onHome, onSignallq, adsEnabled, weakSignal }) {
  const Metric = ({ label, value, unit, color }) => (
    <div style={{ background:LK.bgSecondary, borderRadius:LK.rCard, padding:16 }}>
      <div style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary, marginBottom:7 }}>{label}</div>
      <div style={{ font:`700 24px/1 ${LK.font}`, color }}>{value}<span style={{ font:`400 11px/1 ${LK.font}`, color:LK.textSecondary, marginLeft:3 }}>{unit}</span></div>
    </div>
  );
  const verdict = (icon, label, word, tone) => (
    <div style={{ display:'flex', alignItems:'center', gap:12, padding:'12px 0' }}>
      <Icon name={icon} size={22} color={LK.textSecondary} />
      <span style={{ flex:1, font:`400 14px/1 ${LK.font}`, color:LK.textPrimary }}>{label}</span>
      <Badge color={tone} style={{ fontWeight:700 }}>{word}</Badge>
    </div>
  );

  // Cenário controlado pelo painel de simulação — só troca a métrica de Wi-Fi
  // e a oferta do anúncio; nunca inventa achado que a engine não sustentaria.
  const rssiTone = weakSignal ? LK.warning : LK.success;
  const rssiWord = weakSignal ? 'Regular' : 'Excelente';
  const headline = weakSignal ? 'Conexão instável no Wi-Fi' : 'Conexão excelente';
  const sub = weakSignal
    ? 'Sua velocidade está boa, mas o sinal de Wi-Fi está fraco onde você mediu.'
    : 'Sua internet está rápida e estável — pronta para tudo.';

  return (
    <div style={{ flex:1, overflowY:'auto', background:LK.bgPrimary, padding:'24px 24px 28px' }}>
      <div style={{ textAlign:'center' }}>
        <div style={{ font:`600 20px/1.3 ${LK.font}`, color:LK.textPrimary }}>{headline}</div>
        <div style={{ font:`400 13px/1.4 ${LK.font}`, color:LK.textSecondary, marginTop:6 }}>{sub}</div>
        <div style={{ display:'inline-flex', alignItems:'center', gap:6, marginTop:12,
          background:LK.bgSecondary, border:`1px solid ${LK.border}`, borderRadius:999, padding:'6px 12px' }}>
          <Icon name="wifi" size={13} color={LK.textSecondary} />
          <span style={{ font:`500 11px/1 ${LK.font}`, color:LK.textSecondary }}>Via Wi-Fi</span>
        </div>
      </div>
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12, marginTop:26 }}>
        <Metric label="Download" value="486.3" unit="Mbps" color={LK.success} />
        <Metric label="Upload" value="211.7" unit="Mbps" color={LK.accent} />
        <Metric label="Latência" value="12" unit="ms" color={LK.success} />
        <Metric label="Oscilação" value="24" unit="ms" color={LK.warning} />
        <Metric label="Perda" value="0.0" unit="%" color={LK.success} />
        <Metric label="Sinal Wi-Fi (RSSI)" value={weakSignal ? '−71' : '−27'} unit="dBm" color={rssiTone} />
      </div>
      <span style={{ font:`600 11px/1.3 ${LK.font}`, color:LK.textTertiary, letterSpacing:'.4px',
        textTransform:'uppercase', display:'block', margin:'26px 0 8px' }}>Experiência de uso</span>
      <div style={{ background:LK.bgSecondary, borderRadius:LK.rCard, padding:'4px 16px' }}>
        {verdict('tv','Streaming 4K', weakSignal ? 'Regular' : 'Ótimo', weakSignal ? LK.warning : LK.success)}
        <div style={{ height:1, background:LK.border }} />
        {verdict('sports_esports','Jogos online','Bom',LK.success)}
        <div style={{ height:1, background:LK.border }} />
        {verdict('wifi','Sinal Wi-Fi', rssiWord, rssiTone)}
      </div>

      <button onClick={onSignallq} style={{ width:'100%', marginTop:20, border:0, cursor:'pointer',
        background:LK.accent, color:'#fff', font:`500 15px/1 ${LK.font}`, borderRadius:LK.rBtn, padding:'15px',
        display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
        <Icon name="auto_awesome" size={18} color="#fff" />Conversar com a IA</button>
      <button onClick={onAgain} style={{ width:'100%', marginTop:10, cursor:'pointer',
        background:'transparent', color:LK.textPrimary, font:`500 15px/1 ${LK.font}`,
        border:`1px solid ${LK.border}`, borderRadius:LK.rBtn, padding:'15px' }}>Testar novamente</button>
      <button onClick={onHome} style={{ width:'100%', marginTop:10, cursor:'pointer', background:'transparent',
        color:LK.accent, font:`500 15px/1 ${LK.font}`, border:0, padding:'8px' }}>Ir para o início</button>

      {/* ── Slot de anúncio nativo — depois de diagnóstico + CTAs orgânicos ──
          Sempre presente (issue #555 correção 2026-07-12); a oferta muda com
          weakSignal, a presença não. Fonte também muda: com evidência real (RSSI
          fraco medido nesta tela) é "partner" — coreRecommendation casou o achado
          com uma oferta; sem evidência é "admob" — fallback genérico. */}
      {adsEnabled && (
        weakSignal ? (
          <NativeAdCard
            source="partner"
            brandLetter="M"
            brandColor={LK.accentBlue}
            headline="Mesh Wi-Fi 6 — cobertura maior pra sua casa"
            body="Seu sinal ficou fraco nesse teste. Um mesh reduz zona morta sem trocar de operadora."
            ctaLabel="Ver oferta de mesh"
            onCta={() => {}}
            onDismiss={() => {}}
          />
        ) : (
          <NativeAdCard
            source="admob"
            brandLetter="A"
            brandColor={LK.accent}
            headline="Roteador Wi-Fi 6E — pronto para o próximo upgrade"
            body="Sua conexão já está ótima. Se for trocar de roteador, esse aguenta mais dispositivos."
            ctaLabel="Ver oferta"
            onCta={() => {}}
            onDismiss={() => {}}
          />
        )
      )}
    </div>
  );
}

Object.assign(window, { SpeedFlow });
