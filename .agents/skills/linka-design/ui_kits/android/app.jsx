/* Linka UI Kit — app shell, screen router, mount */

const TABMETA = {
  home:    { title:'Início',        icon:null },
  speed:   { title:'Velocidade',    icon:null },
  sinal:   { title:'Sinal',         icon:'settings_input_antenna' },
  hist:    { title:'Histórico',     icon:null },
  ajustes: { title:'Configurações', icon:null },
};

function App() {
  const [tab, setTab] = React.useState('home');
  const [orbit, setOrbit] = React.useState(false);

  const go = (dest) => {
    if (dest === 'orbit') { setOrbit(true); return; }
    setOrbit(false); setTab(dest);
  };

  const meta = TABMETA[tab];
  const action = tab === 'sinal'
    ? <button style={{ background:'none', border:0, cursor:'pointer', padding:8 }}><Icon name="refresh" size={22} color={LK.textPrimary} /></button>
    : tab === 'speed'
    ? <button style={{ background:'none', border:0, cursor:'pointer', padding:8 }}><Icon name="ios_share" size={20} color={LK.textPrimary} /></button>
    : null;

  return (
    <PhoneFrame>
      <StatusBar />
      {orbit ? (
        <OrbitScreen onClose={() => setOrbit(false)} />
      ) : (
        <React.Fragment>
          <TopBar title={meta.title} icon={meta.icon} action={action} />
          {tab === 'home'    && <HomeScreen go={go} />}
          {tab === 'speed'   && <SpeedFlow go={go} />}
          {tab === 'sinal'   && <SinalScreen />}
          {tab === 'hist'    && <HistoricoScreen />}
          {tab === 'ajustes' && <AjustesScreen />}
          <BottomNav active={tab} onChange={go} />
        </React.Fragment>
      )}
    </PhoneFrame>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
