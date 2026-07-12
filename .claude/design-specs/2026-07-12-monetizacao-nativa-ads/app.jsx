/* SignallQ UI Kit — shell do protótipo de monetização nativa (issue #555)
   Navegação real: Início (com atalho "Dispositivos") → Velocidade (idle → running →
   Resultado) → Histórico, todos dentro do phone frame, mais o painel de simulação ao
   lado (fora do frame, não é parte do app). */

function MiniHome({ go }) {
  return (
    <div style={{ flex:1, overflowY:'auto', background:LK.bgPrimary, padding:'4px 16px 20px',
      display:'flex', flexDirection:'column', gap:14 }}>
      <div style={{ background:LK.bgCard, border:`1px solid ${LK.border}`, borderRadius:LK.rCard, padding:16 }}>
        <span style={{ font:`600 11px/1.3 ${LK.font}`, color:LK.textTertiary, letterSpacing:'.4px',
          textTransform:'uppercase' }}>Última medição</span>
        <div style={{ display:'flex', alignItems:'flex-end', gap:18, margin:'14px 0 4px' }}>
          <div>
            <div style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary, marginBottom:4 }}>Download</div>
            <div style={{ font:`700 26px/1 ${LK.font}`, color:LK.success }}>486<span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textSecondary, marginLeft:3 }}>Mbps</span></div>
          </div>
          <div>
            <div style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary, marginBottom:4 }}>Upload</div>
            <div style={{ font:`700 26px/1 ${LK.font}`, color:LK.accent }}>212<span style={{ font:`400 12px/1 ${LK.font}`, color:LK.textSecondary, marginLeft:3 }}>Mbps</span></div>
          </div>
        </div>
        <button onClick={() => go('speed')} style={{ width:'100%', marginTop:12, border:0, cursor:'pointer',
          background:LK.accent, color:'#fff', font:`500 14px/1 ${LK.font}`,
          borderRadius:LK.rBtn, padding:'14px' }}>Medir velocidade</button>
      </div>

      <div style={{ display:'flex', gap:10 }}>
        <button onClick={() => go('dispositivos')} style={{ flex:1, background:LK.bgCard, border:`1px solid ${LK.border}`,
          borderRadius:LK.rCard, padding:'14px 8px', display:'flex', flexDirection:'column', alignItems:'center',
          gap:8, cursor:'pointer' }}>
          <Icon name="devices" size={22} color={LK.accent} />
          <span style={{ font:`500 12px/1.2 ${LK.font}`, color:LK.textPrimary }}>Dispositivos</span>
        </button>
        <button onClick={() => go('hist')} style={{ flex:1, background:LK.bgCard, border:`1px solid ${LK.border}`,
          borderRadius:LK.rCard, padding:'14px 8px', display:'flex', flexDirection:'column', alignItems:'center',
          gap:8, cursor:'pointer' }}>
          <Icon name="history" size={22} color={LK.accent} />
          <span style={{ font:`500 12px/1.2 ${LK.font}`, color:LK.textPrimary }}>Histórico</span>
        </button>
      </div>

      <div style={{ font:`400 12px/1.5 ${LK.font}`, color:LK.textTertiary, textAlign:'center', marginTop:8 }}>
        Protótipo de escopo — só as 4 telas do card patrocinado estão navegáveis
        (Velocidade, Resultado, Dispositivos, Histórico). Início/Sinal/Ajustes reais
        não fazem parte desta rodada.
      </div>
    </div>
  );
}

const TABMETA = {
  home: 'Início',
  speed: 'Velocidade',
  hist: 'Histórico',
};

function App() {
  const [tab, setTab] = React.useState('home');
  const [adsEnabled, setAdsEnabled] = React.useState(true);
  const [weakSignal, setWeakSignal] = React.useState(true);

  const go = (dest) => setTab(dest);

  return (
    <div style={{ display:'flex', alignItems:'flex-start', gap:48 }}>
      <PhoneFrame>
        <StatusBar />
        {tab === 'dispositivos' ? (
          <DispositivosScreenMock onBack={() => go('home')} adsEnabled={adsEnabled} weakSignal={weakSignal} />
        ) : (
          <React.Fragment>
            <TopBar title={TABMETA[tab]} action={null} />
            {tab === 'home'  && <MiniHome go={go} />}
            {tab === 'speed' && <SpeedFlow go={go} adsEnabled={adsEnabled} weakSignal={weakSignal} />}
            {tab === 'hist'  && <HistoricoScreen adsEnabled={adsEnabled} weakSignal={weakSignal} />}
            <BottomNav active={tab === 'speed' ? 'speed' : tab === 'hist' ? 'hist' : 'home'}
              onChange={(id) => go(id === 'sinal' || id === 'ajustes' ? 'home' : id)} />
          </React.Fragment>
        )}
      </PhoneFrame>
      <DemoPanel adsEnabled={adsEnabled} setAdsEnabled={setAdsEnabled}
        weakSignal={weakSignal} setWeakSignal={setWeakSignal} />
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
