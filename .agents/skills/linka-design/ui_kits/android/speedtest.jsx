/* Linka UI Kit — Velocidade flow: idle → running gauge → resultado */

function SpeedFlow({ go }) {
  const [phase, setPhase] = React.useState('idle'); // idle | running | result
  const [mode, setMode] = React.useState('Completo');

  if (phase === 'idle') return <SpeedIdle mode={mode} setMode={setMode} onStart={() => setPhase('running')} />;
  if (phase === 'running') return <SpeedRunning onDone={() => setPhase('result')} />;
  return <Resultado onAgain={() => setPhase('idle')} onHome={() => go('home')} onOrbit={() => go('orbit')} />;
}

function SpeedIdle({ mode, setMode, onStart }) {
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
          <Overline>Último resultado</Overline>
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
      {/* phase pills */}
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

function Resultado({ onAgain, onHome, onOrbit }) {
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
  return (
    <div style={{ flex:1, overflowY:'auto', background:LK.bgPrimary, padding:'24px 24px 28px' }}>
      <div style={{ textAlign:'center' }}>
        <div style={{ font:`600 20px/1.3 ${LK.font}`, color:LK.textPrimary }}>Conexão excelente</div>
        <div style={{ font:`400 13px/1.4 ${LK.font}`, color:LK.textSecondary, marginTop:6 }}>
          Sua internet está rápida e estável — pronta para tudo.</div>
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
        <Metric label="Bufferbloat" value="8" unit="ms" color={LK.success} />
      </div>
      <Overline style={{ margin:'26px 0 8px' }}>Experiência de uso</Overline>
      <div style={{ background:LK.bgSecondary, borderRadius:LK.rCard, padding:'4px 16px' }}>
        {verdict('tv','Streaming 4K','Ótimo',LK.success)}
        <div style={{ height:1, background:LK.border }} />
        {verdict('sports_esports','Jogos online','Bom',LK.success)}
        <div style={{ height:1, background:LK.border }} />
        {verdict('videocam','Vídeo chamada','Ótimo',LK.success)}
      </div>
      <button onClick={onOrbit} style={{ width:'100%', marginTop:20, border:0, cursor:'pointer',
        background:LK.accent, color:'#fff', font:`500 15px/1 ${LK.font}`, borderRadius:LK.rBtn, padding:'15px',
        display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
        <Icon name="auto_awesome" size={18} color="#fff" />Conversar com a IA</button>
      <button onClick={onAgain} style={{ width:'100%', marginTop:10, cursor:'pointer',
        background:'transparent', color:LK.textPrimary, font:`500 15px/1 ${LK.font}`,
        border:`1px solid ${LK.border}`, borderRadius:LK.rBtn, padding:'15px' }}>Testar novamente</button>
      <button onClick={onHome} style={{ width:'100%', marginTop:10, cursor:'pointer', background:'transparent',
        color:LK.accent, font:`500 15px/1 ${LK.font}`, border:0, padding:'8px' }}>Ir para o início</button>
    </div>
  );
}

Object.assign(window, { SpeedFlow });
