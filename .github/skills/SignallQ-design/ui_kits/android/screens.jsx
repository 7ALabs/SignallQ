/* SignallQ UI Kit — Início, Sinal, Histórico, Ajustes */

function Card({ children, style = {}, onClick }) {
  return (
    <div onClick={onClick} style={{ background:LK.bgCard, border:`1px solid ${LK.border}`,
      borderRadius:LK.rCard, padding:16, boxSizing:'border-box',
      cursor:onClick?'pointer':'default', ...style }}>{children}</div>
  );
}
function Overline({ children, style }) {
  return <div style={{ font:`600 11px/1.3 ${LK.font}`, color:LK.textTertiary,
    letterSpacing:'.4px', textTransform:'uppercase', ...style }}>{children}</div>;
}
function ScreenScroll({ children }) {
  return <div style={{ flex:1, overflowY:'auto', background:LK.bgPrimary,
    padding:'4px 16px 20px', display:'flex', flexDirection:'column', gap:14 }}>{children}</div>;
}

// ── Início ────────────────────────────────────────────────────────
function HomeScreen({ go }) {
  const node = (icon, label, tone, active) => (
    <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:7, flex:1, zIndex:1 }}>
      <div style={{ width:52, height:52, borderRadius:'50%',
        background: active ? hexA(tone,.12) : LK.bgSecondary,
        border: active ? `1.5px solid ${hexA(tone,.4)}` : `1px solid ${LK.border}`,
        display:'flex', alignItems:'center', justifyContent:'center' }}>
        <Icon name={icon} size={24} color={active ? tone : LK.textTertiary} />
      </div>
      <span style={{ font:`600 11px/1.2 ${LK.font}`, color:LK.textPrimary }}>{label}</span>
    </div>
  );
  return (
    <ScreenScroll>
      {/* NetworkPath */}
      <Card>
        <Overline style={{ marginBottom:14 }}>Caminho da sua internet</Overline>
        <div style={{ position:'relative', display:'flex', justifyContent:'space-between' }}>
          <div style={{ position:'absolute', top:26, left:'18%', right:'18%', height:2,
            background:`linear-gradient(90deg, ${LK.success}, ${LK.accent})` }} />
          {node('smartphone','Seu aparelho', LK.success, true)}
          {node('router','Roteador', LK.accent, true)}
          {node('public','Provedor', LK.accentBlue, true)}
        </div>
        <div style={{ marginTop:14, font:`400 12px/1.45 ${LK.font}`, color:LK.textSecondary, textAlign:'center' }}>
          Tudo conectado. Sua conexão chega até o provedor sem falhas.
        </div>
      </Card>

      {/* MedicoesCard */}
      <Card>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'baseline' }}>
          <Overline>Última medição</Overline>
          <span style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary }}>há 2 h</span>
        </div>
        <div style={{ display:'flex', alignItems:'flex-end', gap:18, margin:'14px 0 4px' }}>
          <div>
            <div style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary, marginBottom:4 }}>Download</div>
            <div style={{ font:`700 26px/1 ${LK.font}`, color:LK.success }}>486<span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textSecondary, marginLeft:3 }}>Mbps</span></div>
          </div>
          <div>
            <div style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary, marginBottom:4 }}>Upload</div>
            <div style={{ font:`700 26px/1 ${LK.font}`, color:LK.accent }}>212<span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textSecondary, marginLeft:3 }}>Mbps</span></div>
          </div>
          <svg width="92" height="40" viewBox="0 0 92 40" style={{ marginLeft:'auto' }}>
            <polyline points="0,30 14,24 28,28 42,14 56,18 70,8 92,12" fill="none"
              stroke={LK.accent} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
        <button onClick={() => go('speed')} style={{ width:'100%', marginTop:12, border:0, cursor:'pointer',
          background:LK.accent, color:'#fff', font:`500 14px/1 ${LK.font}`,
          borderRadius:LK.rBtn, padding:'14px' }}>Medir velocidade</button>
      </Card>

      {/* MiniCardsRow */}
      <div style={{ display:'flex', gap:10 }}>
        {[['dns','Testar DNS','dns'],['network_ping','Ping','ping'],['auto_awesome','Diagnóstico IA','signallq']].map(([ic,lbl,dest]) => (
          <Card key={lbl} onClick={() => dest==='signallq' && go('signallq')} style={{ flex:1, padding:'14px 8px',
            display:'flex', flexDirection:'column', alignItems:'center', gap:8 }}>
            <Icon name={ic} size={22} color={LK.accent} />
            <span style={{ font:`500 12px/1.2 ${LK.font}`, color:LK.textPrimary, textAlign:'center' }}>{lbl}</span>
          </Card>
        ))}
      </div>

      {/* SignalCard */}
      <Card style={{ display:'flex', alignItems:'center', gap:12 }}>
        <div style={{ width:44, height:44, borderRadius:'50%', flex:'none',
          background:hexA(LK.success,.10), display:'flex', alignItems:'center', justifyContent:'center' }}>
          <Icon name="wifi" size={22} color={LK.success} />
        </div>
        <div style={{ flex:1, minWidth:0 }}>
          <Overline>Wi-Fi · 5 GHz</Overline>
          <div style={{ font:`600 15px/1.3 ${LK.font}`, color:LK.textPrimary, margin:'1px 0' }}>Luiz-5G</div>
          <div style={{ font:`400 11px/1.3 ${LK.font}`, color:LK.textSecondary }}>RSSI −27 dBm · Canal 36 · 433 Mbps</div>
        </div>
        <div style={{ textAlign:'right' }}>
          <SignalBars level={4} color={LK.success} />
          <div style={{ font:`600 10px/1 ${LK.font}`, color:LK.success, marginTop:5 }}>Forte</div>
        </div>
      </Card>
    </ScreenScroll>
  );
}

// ── Sinal (Wi-Fi tab) ─────────────────────────────────────────────
function SinalScreen() {
  const [tab, setTab] = React.useState('wifi');
  const [band, setBand] = React.useState('Todos');
  const tabs = [['wifi','Wi-Fi'],['canal','Canal'],['movel','Móvel']];
  const bands = ['Todos','2.4GHz','5GHz','6GHz'];
  const others = [
    { ssid:'Redes ocultas', sub:'2.4GHz', count:'23', lvl:4, tone:LK.success, lock:false, chevron:true },
    { ssid:'Luiz-2.4G', sub:'2.4GHz', count:'2', lvl:4, tone:LK.success, lock:false, chevron:true },
    { ssid:'Wallace lopes 2G', sub:'Banda: 2.4GHz  RSSI −51 dBm  Canal 11', lvl:3, tone:LK.success, lock:true, router:true },
    { ssid:'Adris 2.4', sub:'Banda: 2.4GHz  RSSI −57 dBm  Canal 1', lvl:3, tone:LK.success, lock:true, router:true },
  ];
  return (
    <div style={{ flex:1, display:'flex', flexDirection:'column', background:LK.bgPrimary, overflow:'hidden' }}>
      {/* TabRow */}
      <div style={{ display:'flex', borderBottom:`1px solid ${LK.border}`, flex:'none' }}>
        {tabs.map(([id,lbl]) => {
          const on = id===tab;
          return (
            <button key={id} onClick={()=>setTab(id)} style={{ flex:1, background:'none', border:0,
              cursor:'pointer', padding:'14px 0', font:`${on?700:500} 15px/1 ${LK.font}`,
              color: on?LK.accent:LK.textSecondary,
              borderBottom: on?`2px solid ${LK.accent}`:'2px solid transparent', marginBottom:-1 }}>{lbl}</button>
          );
        })}
      </div>

      {tab==='wifi' && (
        <div style={{ flex:1, overflowY:'auto', padding:'16px' }}>
          {/* band filters */}
          <div style={{ display:'flex', gap:9, marginBottom:18 }}>
            {bands.map(b => {
              const on = b===band;
              return <button key={b} onClick={()=>setBand(b)} style={{ flex:1, border:0, cursor:'pointer',
                padding:'12px 0', borderRadius:999, font:`${on?600:500} 13px/1 ${LK.font}`,
                background: on?hexA(LK.accent,.12):LK.bgSecondary, color: on?LK.accent:LK.textSecondary }}>{b}</button>;
            })}
          </div>

          <Overline style={{ marginBottom:10 }}>Sua conexão</Overline>
          {/* connected group */}
          <div style={{ display:'flex', alignItems:'center', gap:12, marginBottom:6 }}>
            <div style={{ width:44, height:44, borderRadius:'50%', background:hexA(LK.accent,.12),
              display:'flex', alignItems:'center', justifyContent:'center', flex:'none' }}>
              <Icon name="wifi" size={22} color={LK.accent} />
            </div>
            <div style={{ flex:1 }}>
              <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                <span style={{ font:`600 17px/1.2 ${LK.font}`, color:LK.textPrimary }}>Luiz-5G</span>
                <Badge color={LK.success}>Conectado</Badge>
              </div>
              <div style={{ font:`400 12px/1.3 ${LK.font}`, color:LK.textTertiary }}>2 nós detectados</div>
            </div>
          </div>
          {/* connected node */}
          <div style={{ display:'flex', gap:10, alignItems:'center', background:hexA(LK.success,.12),
            borderRadius:14, padding:'12px 14px', margin:'8px 0' }}>
            <Icon name="router" size={20} color={LK.accent} />
            <div style={{ flex:1 }}>
              <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:2 }}>
                <span style={{ font:`700 14px/1 ${LK.font}`, color:LK.accent }}>Conectado agora</span>
                <Badge color={LK.success} bg={hexA(LK.success,.2)}>✓ Conectado</Badge>
              </div>
              <div style={{ font:`400 11px/1.4 ${LK.font}`, color:LK.textSecondary }}>
                5GHz · <span style={{ color:LK.success, fontWeight:600 }}>Excelente</span><br/>
                Banda: 5GHz  RSSI −27 dBm  Canal 36<br/>Wi-Fi 5 (ac) · 433 Mbps</div>
            </div>
            <SignalBars level={4} color={LK.success} />
          </div>
          {/* mesh node */}
          <div style={{ display:'flex', gap:10, alignItems:'center', padding:'8px 14px' }}>
            <Icon name="wifi" size={20} color={LK.textTertiary} />
            <div style={{ flex:1 }}>
              <div style={{ font:`700 14px/1.2 ${LK.font}`, color:LK.textPrimary }}>Nó #1 <span style={{ color:LK.textTertiary, fontWeight:400 }}>· 39:83:3d</span></div>
              <div style={{ font:`400 11px/1.4 ${LK.font}`, color:LK.textSecondary }}>5GHz · <span style={{ color:LK.warning, fontWeight:600 }}>Regular</span><br/>Banda: 5GHz  RSSI −68 dBm  Canal 36</div>
            </div>
            <SignalBars level={2} color={LK.warning} />
          </div>

          <Overline style={{ margin:'18px 0 4px' }}>Outras redes</Overline>
          {others.map((n,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'13px 0',
              borderBottom:`1px solid ${LK.border}` }}>
              <Icon name="wifi" size={22} color={LK.textTertiary} />
              <div style={{ flex:1 }}>
                <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                  <span style={{ font:`500 15px/1.2 ${LK.font}`, color:LK.textPrimary }}>{n.ssid}</span>
                  {n.count && <span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textTertiary }}>· {n.count}</span>}
                  {n.router && <Icon name="router" size={14} color={LK.textTertiary} />}
                </div>
                <div style={{ font:`400 11px/1.3 ${LK.font}`, color:LK.textTertiary }}>{n.sub}</div>
              </div>
              {n.chevron && <Icon name="expand_more" size={20} color={LK.textTertiary} />}
              {n.lock && <Icon name="lock" size={16} color={LK.textTertiary} />}
              <SignalBars level={n.lvl} color={n.tone} />
            </div>
          ))}
        </div>
      )}

      {tab==='canal' && <ChannelTab />}
      {tab==='movel' && <MovelTab />}
    </div>
  );
}

function ChannelTab() {
  const ch = [
    { c:'Canal 36', use:'Sua rede', pct:30, tone:LK.success },
    { c:'Canal 112', use:'2 redes', pct:55, tone:LK.warning },
    { c:'Canal 161', use:'4 redes', pct:85, tone:LK.error },
    { c:'Canal 44', use:'Livre', pct:12, tone:LK.success },
  ];
  return (
    <div style={{ flex:1, overflowY:'auto', padding:16 }}>
      <Card style={{ background:hexA(LK.warning,.08), border:`1px solid ${hexA(LK.warning,.3)}`, marginBottom:14 }}>
        <div style={{ display:'flex', gap:10, alignItems:'flex-start' }}>
          <Icon name="lightbulb" size={20} color={LK.warning} />
          <div style={{ font:`400 13px/1.5 ${LK.font}`, color:LK.textPrimary }}>
            <b>Canal 161 congestionado.</b> Troque seu Wi-Fi 5GHz para o <b>canal 44</b> para uma conexão mais estável.</div>
        </div>
      </Card>
      <Overline style={{ marginBottom:12 }}>Ocupação dos canais · 5GHz</Overline>
      {ch.map(x => (
        <div key={x.c} style={{ marginBottom:14 }}>
          <div style={{ display:'flex', justifyContent:'space-between', marginBottom:6 }}>
            <span style={{ font:`500 13px/1 ${LK.font}`, color:LK.textPrimary }}>{x.c}</span>
            <span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textSecondary }}>{x.use}</span>
          </div>
          <div style={{ height:8, borderRadius:4, background:LK.bgSecondary, overflow:'hidden' }}>
            <div style={{ width:`${x.pct}%`, height:'100%', background:x.tone, borderRadius:4 }} />
          </div>
        </div>
      ))}
    </div>
  );
}

function MovelTab() {
  return (
    <div style={{ flex:1, overflowY:'auto', padding:16 }}>
      <Card style={{ background:hexA(LK.accent,.06), border:`1px solid ${hexA(LK.accent,.25)}`, marginBottom:14 }}>
        <Overline>Rede móvel · 5G</Overline>
        <div style={{ font:`700 22px/1.2 ${LK.font}`, color:LK.textPrimary, margin:'6px 0 2px' }}>Claro</div>
        <div style={{ font:`400 12px/1.3 ${LK.font}`, color:LK.textSecondary }}>RSRP −95 dBm · 5G NR</div>
      </Card>
      {[['signal_cellular_alt','Qualidade do sinal','Bom — chamadas e vídeos sem cortes',LK.success,'Bom'],
        ['cell_tower','Tipo de conexão','5G NR — a tecnologia mais rápida disponível',LK.accent,'5G'],
        ['rocket_launch','Experiência esperada','Ótima para streaming e jogos',LK.success,'Ótima']].map(([ic,t,d,tone,bdg])=>(
        <Card key={t} style={{ display:'flex', alignItems:'center', gap:12, marginBottom:10, padding:12 }}>
          <div style={{ width:36, height:36, borderRadius:'50%', background:hexA(tone,.10), flex:'none',
            display:'flex', alignItems:'center', justifyContent:'center' }}><Icon name={ic} size={20} color={tone} /></div>
          <div style={{ flex:1 }}>
            <div style={{ font:`600 12px/1.2 ${LK.font}`, color:LK.textPrimary }}>{t}</div>
            <div style={{ font:`400 11px/1.35 ${LK.font}`, color:LK.textSecondary }}>{d}</div>
          </div>
          <Badge color={tone} style={{ fontWeight:700 }}>{bdg}</Badge>
        </Card>
      ))}
    </div>
  );
}

// ── Histórico ─────────────────────────────────────────────────────
function HistoricoScreen() {
  // 7 cols x 5 rows uptime grid
  const cells = Array.from({length:35}, (_,i) => {
    const r = (i*53)%100;
    return r>92 ? LK.error : r>80 ? LK.warning : LK.success;
  });
  return (
    <ScreenScroll>
      <Card>
        <Overline style={{ marginBottom:12 }}>Estabilidade · últimas 35 medições</Overline>
        <div style={{ display:'grid', gridTemplateColumns:'repeat(7,1fr)', gap:6 }}>
          {cells.map((c,i) => <div key={i} style={{ aspectRatio:'1', borderRadius:4, background:hexA(c,.85) }} />)}
        </div>
        <div style={{ display:'flex', gap:14, marginTop:14 }}>
          {[['Estável',LK.success],['Instável',LK.warning],['Queda',LK.error]].map(([l,c])=>(
            <div key={l} style={{ display:'flex', alignItems:'center', gap:6 }}>
              <span style={{ width:10, height:10, borderRadius:3, background:c }} />
              <span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textSecondary }}>{l}</span>
            </div>
          ))}
        </div>
      </Card>
      <Card style={{ background:LK.bgSecondary, border:0 }}>
        <div style={{ font:`400 14px/1.6 ${LK.font}`, color:LK.textPrimary }}>
          Sua internet ficou <b style={{ color:LK.success }}>estável em 89%</b> do tempo nas últimas 24 h.
          Houve uma breve oscilação por volta das 14 h, mas a conexão se recuperou sozinha.</div>
      </Card>
      <Overline style={{ marginTop:4 }}>Medições recentes</Overline>
      {[['Hoje, 18:17','486 Mbps',LK.success],['Hoje, 14:02','120 Mbps',LK.warning],['Ontem, 21:40','502 Mbps',LK.success]].map(([t,v,c])=>(
        <Card key={t} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:14 }}>
          <span style={{ font:`400 13px/1 ${LK.font}`, color:LK.textSecondary }}>{t}</span>
          <span style={{ font:`700 15px/1 ${LK.font}`, color:c }}>{v}</span>
        </Card>
      ))}
    </ScreenScroll>
  );
}

// ── Ajustes ───────────────────────────────────────────────────────
function AjustesScreen() {
  const Section = ({ title, children }) => (
    <div style={{ marginBottom:4 }}>
      <Overline style={{ margin:'8px 4px 8px' }}>{title}</Overline>
      <Card style={{ padding:0, overflow:'hidden' }}>{children}</Card>
    </div>
  );
  const Row = ({ icon, label, value, last, toggle, on }) => (
    <div style={{ display:'flex', alignItems:'center', gap:14, padding:'14px 16px',
      borderBottom: last?'0':`1px solid ${LK.border}` }}>
      <div style={{ width:34, height:34, borderRadius:'50%', background:hexA(LK.accent,.12), flex:'none',
        display:'flex', alignItems:'center', justifyContent:'center' }}><Icon name={icon} size={19} color={LK.accent} /></div>
      <span style={{ flex:1, font:`400 15px/1.2 ${LK.font}`, color:LK.textPrimary }}>{label}</span>
      {value && <span style={{ font:`400 13px/1 ${LK.font}`, color:LK.textSecondary }}>{value}</span>}
      {toggle && <div style={{ width:44, height:26, borderRadius:13, background: on?LK.accent:LK.border,
        position:'relative', transition:'.2s' }}>
        <div style={{ position:'absolute', top:3, left: on?21:3, width:20, height:20, borderRadius:'50%',
          background:'#fff', transition:'.2s', boxShadow:'0 1px 3px rgba(0,0,0,.2)' }} /></div>}
    </div>
  );
  return (
    <div style={{ flex:1, overflowY:'auto', background:LK.bgPrimary, padding:'8px 16px 20px' }}>
      <Section title="Minha conexão">
        <Row icon="business" label="Operadora" value="Claro" />
        <Row icon="speed" label="Plano contratado" value="500 Mbps" />
        <Row icon="location_on" label="Cidade" value="São Paulo, SP" last />
      </Section>
      <Section title="Aparência">
        <Row icon="dark_mode" label="Tema" value="Sistema" last />
      </Section>
      <Section title="Histórico e dados">
        <Row icon="monitoring" label="Monitoramento ativo" toggle on={true} />
        <Row icon="notifications" label="Alertas de latência" toggle on={true} />
        <Row icon="wifi" label="Alerta de sinal fraco" toggle on={false} last />
      </Section>
      <Section title="Informações">
        <Row icon="lock" label="Privacidade" />
        <Row icon="campaign" label="Novidades" />
        <Row icon="info" label="Versão do app" value="0.13.0 (35)" last />
      </Section>
    </div>
  );
}

Object.assign(window, { Card, Overline, ScreenScroll, HomeScreen, SinalScreen, HistoricoScreen, AjustesScreen });
