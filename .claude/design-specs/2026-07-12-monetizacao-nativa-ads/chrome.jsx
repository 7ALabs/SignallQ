/* SignallQ UI Kit — phone chrome + shared primitives (copia de ui_kits/android/chrome.jsx
   + DemoToggle/DemoPanel para o protótipo de monetização nativa)
   Exposes: Icon, SignalBars, Badge, Avatar, PhoneFrame, TopBar, BottomNav, LK, DemoToggle, DemoPanel */

const LK = {
  accent:'#6C2BFF', accentBlue:'#2563EB',
  success:'#22C55E', warning:'#F5A623', error:'#FF4D4F',
  bgPrimary:'#FFFFFF', bgSecondary:'#F3F4F6', bgCard:'#FFFFFF',
  textPrimary:'#0D0D1A', textSecondary:'#6B7280', textTertiary:'#9CA3AF',
  border:'#E5E7EB',
  rCard:16, rBtn:12, rPill:999,
  font:"'Roboto', system-ui, sans-serif",
};

function Icon({ name, size = 24, color = 'currentColor', fill = 0, weight = 400, style = {} }) {
  return (
    <span className="material-symbols-outlined" style={{
      fontSize: size, color, lineHeight: 1,
      fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'GRAD' 0, 'opsz' 24`,
      ...style,
    }}>{name}</span>
  );
}

function SignalBars({ level = 4, color = LK.success, big = false }) {
  const hs = big ? [7,11,15,20] : [6,9,12,16];
  const w = big ? 4 : 3;
  return (
    <div style={{ display:'flex', alignItems:'flex-end', gap: w, height: hs[3] }}>
      {hs.map((h,i) => (
        <i key={i} style={{ width:w, height:h, borderRadius:1, display:'block',
          background: i < level ? color : LK.border }} />
      ))}
    </div>
  );
}

function Badge({ children, color = LK.accent, bg, style = {} }) {
  return (
    <span style={{
      display:'inline-flex', alignItems:'center', gap:4,
      font:`600 11px/1 ${LK.font}`, color,
      background: bg || hexA(color, .12), padding:'5px 9px', borderRadius:999,
      whiteSpace:'nowrap', ...style,
    }}>{children}</span>
  );
}

function Avatar({ size = 44, letter = 'L' }) {
  return (
    <div style={{
      width:size, height:size, borderRadius:'50%',
      background:`linear-gradient(135deg, ${LK.accent}, ${LK.accentBlue})`,
      color:'#fff', display:'flex', alignItems:'center', justifyContent:'center',
      font:`700 ${size*0.4}px/1 ${LK.font}`, flex:'none',
    }}>{letter}</div>
  );
}

function hexA(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${(n>>16)&255}, ${(n>>8)&255}, ${n&255}, ${a})`;
}

function StatusBar() {
  return (
    <div style={{ height:34, display:'flex', alignItems:'center', justifyContent:'space-between',
      padding:'0 18px', flex:'none', background:LK.bgPrimary }}>
      <span style={{ font:`600 15px/1 ${LK.font}`, color:LK.textPrimary }}>18:28</span>
      <div style={{ display:'flex', alignItems:'center', gap:6, color:LK.textPrimary }}>
        <Icon name="signal_wifi_4_bar" size={17} />
        <span style={{ font:`700 9px/1 ${LK.font}`, border:`1.4px solid ${LK.textPrimary}`,
          borderRadius:3, padding:'1px 2px' }}>5G</span>
        <Icon name="signal_cellular_alt" size={17} />
        <Icon name="battery_full" size={17} style={{ transform:'rotate(90deg)' }} />
      </div>
    </div>
  );
}

function TopBar({ title, icon, leading, action }) {
  return (
    <div style={{ height:64, display:'flex', alignItems:'center', padding:'0 8px 0 16px',
      flex:'none', background:LK.bgPrimary, borderBottom:`1px solid ${LK.border}` }}>
      <div style={{ width:48, display:'flex', justifyContent:'flex-start' }}>
        {leading || <Avatar />}
      </div>
      <div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
        {icon && <Icon name={icon} size={22} color={LK.textPrimary} />}
        <span style={{ font:`500 18px/1 ${LK.font}`, color:LK.textPrimary }}>{title}</span>
      </div>
      <div style={{ width:48, display:'flex', justifyContent:'flex-end' }}>{action}</div>
    </div>
  );
}

const NAV = [
  { id:'home',  label:'Início',     icon:'home' },
  { id:'speed', label:'Velocidade', icon:'speed' },
  { id:'sinal', label:'Sinal',      icon:'wifi' },
  { id:'hist',  label:'Histórico',  icon:'history' },
  { id:'ajustes', label:'Ajustes',  icon:'settings' },
];
function BottomNav({ active, onChange }) {
  return (
    <div style={{ display:'flex', background:LK.bgPrimary, borderTop:`1px solid ${LK.border}`,
      padding:'8px 4px 6px', flex:'none' }}>
      {NAV.map(t => {
        const on = t.id === active;
        return (
          <button key={t.id} onClick={() => onChange(t.id)} style={{
            flex:1, display:'flex', flexDirection:'column', alignItems:'center', gap:4,
            background:'none', border:0, cursor:'pointer', padding:0,
            color: on ? LK.accent : LK.textTertiary }}>
            <div style={{ padding:'3px 18px', borderRadius:999,
              background: on ? hexA(LK.accent,.12) : 'transparent',
              display:'flex', alignItems:'center', justifyContent:'center' }}>
              <Icon name={t.icon} size={24} fill={on?1:0} />
            </div>
            <span style={{ font:`${on?600:500} 11px/1 ${LK.font}` }}>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

function PhoneFrame({ children }) {
  return (
    <div style={{
      width:390, height:844, background:LK.bgPrimary, borderRadius:36,
      border:'10px solid #111', boxShadow:'0 30px 80px rgba(0,0,0,.28)',
      overflow:'hidden', display:'flex', flexDirection:'column', position:'relative',
      fontFamily:LK.font,
    }}>
      {children}
      <div style={{ position:'absolute', bottom:6, left:'50%', transform:'translateX(-50%)',
        width:120, height:4, borderRadius:2, background:LK.textPrimary, opacity:.25 }} />
    </div>
  );
}

// ── Painel de controle do protótipo (fora do frame do telefone) ──────────
// Simula o Firebase Remote Config (liga/desliga o slot de anúncio globalmente)
// e o sinal do coreRecommendation (Wi-Fi fraco recorrente), que só troca a
// oferta contextual dentro do slot — o slot em si é presença padrão, não
// depende dessa evidência para existir.
function ToggleSwitch({ on, onChange, label, tone = LK.accent }) {
  return (
    <button onClick={() => onChange(!on)} style={{ display:'flex', alignItems:'center', gap:10,
      background:'none', border:0, cursor:'pointer', padding:'6px 0', width:'100%' }}>
      <div style={{ width:40, height:23, borderRadius:12, background: on?tone:LK.border,
        position:'relative', transition:'.15s', flex:'none' }}>
        <div style={{ position:'absolute', top:2.5, left: on?19:2.5, width:18, height:18, borderRadius:'50%',
          background:'#fff', transition:'.15s', boxShadow:'0 1px 3px rgba(0,0,0,.2)' }} />
      </div>
      <span style={{ font:`500 13px/1.3 ${LK.font}`, color:LK.textPrimary, textAlign:'left' }}>{label}</span>
    </button>
  );
}

function DemoPanel({ adsEnabled, setAdsEnabled, weakSignal, setWeakSignal }) {
  return (
    <div style={{ width:280, flex:'none', color:LK.textPrimary }}>
      <div style={{ font:`700 13px/1.3 ${LK.font}`, color:LK.textPrimary, marginBottom:2 }}>
        Painel de simulação (não faz parte do app)
      </div>
      <div style={{ font:`400 12px/1.5 ${LK.font}`, color:LK.textSecondary, marginBottom:16 }}>
        AdMob é presença padrão nas 4 telas — não é fallback raro. O toggle abaixo simula
        só os dois motivos legítimos de ausência: Remote Config geral desligado, ou falha
        de fetch do SDK. coreRecommendation não decide "se" o anúncio aparece, só decide
        "qual oferta contextual" preenche o slot.
      </div>
      <div style={{ background:LK.bgCard, border:`1px solid ${LK.border}`, borderRadius:14, padding:'6px 14px' }}>
        <ToggleSwitch on={adsEnabled} onChange={setAdsEnabled} label="Remote Config + fetch OK: anúncio presente" />
        <div style={{ height:1, background:LK.border, margin:'2px 0' }} />
        <ToggleSwitch on={weakSignal} onChange={setWeakSignal} tone={LK.warning}
          label="coreRecommendation: Wi-Fi fraco recorrente (muda a oferta, não a presença)" />
      </div>
      <div style={{ marginTop:14, font:`400 11.5px/1.5 ${LK.font}`, color:LK.textTertiary }}>
        Com o 1º toggle desligado, o anúncio some sem deixar buraco — o layout se
        recompõe (fallback gracioso). Com o 2º toggle, a copy muda de genérica pra
        contextual — e em Resultado/Histórico o badge muda de "Patrocinado"
        (AdMob puro, sem evidência) pra "Parceiro" (coreRecommendation casou o
        achado real com uma oferta). O anúncio continua lá nos dois estados; só
        a fonte declarada muda.
      </div>
    </div>
  );
}

Object.assign(window, { LK, Icon, SignalBars, Badge, Avatar, hexA, StatusBar, TopBar, BottomNav, NAV, PhoneFrame, ToggleSwitch, DemoPanel });
