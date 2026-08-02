/* SignallQ UI Kit — phone chrome + shared primitives
   Exposes: Icon, SignalBars, Badge, Avatar, PhoneFrame, TopBar, BottomNav, LK */

// Tokens atuais (fonte de verdade: .claude/skills/SignallQ-design/colors_and_type.css).
// Corrigido em 2026-07-19 — este objeto ainda usava a paleta antiga (#6C2BFF, Roboto-only,
// raio de botão 12px) mesmo após a migração de 2026-07-13 para #5B21D6 / Google Sans Flex / 20px.
const LK = {
  accent:'#5B21D6', accentBlue:'#2851B8',
  success:'#146C2E', warning:'#8A5000', error:'#BA1A1A',
  bgPrimary:'#FFFFFF', bgSecondary:'#F8F5FB', bgCard:'#FFFFFF',
  textPrimary:'#1C1B1F', textSecondary:'#49454F', textTertiary:'#49454F',
  border:'#79747E',
  rCard:16, rBtn:20, rField:12, rSheet:28, rDialog:24, rPill:999,
  font:"'Google Sans Flex', 'Google Sans', 'Roboto', system-ui, sans-serif",
};

// Material Symbols icon
function Icon({ name, size = 24, color = 'currentColor', fill = 0, weight = 400, style = {} }) {
  return (
    <span className="material-symbols-outlined" style={{
      fontSize: size, color, lineHeight: 1,
      fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'GRAD' 0, 'opsz' 24`,
      ...style,
    }}>{name}</span>
  );
}

// 4-bar signal glyph. level 1..4, tone color, empty bars in border
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

// hex + alpha helper -> rgba
function hexA(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${(n>>16)&255}, ${(n>>8)&255}, ${n&255}, ${a})`;
}

// ── Status bar (Android) ──────────────────────────────────────────
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

// ── Center-aligned top app bar ────────────────────────────────────
function TopBar({ title, icon, leading, action }) {
  return (
    <div style={{ height:64, display:'flex', alignItems:'center', padding:'0 8px 0 16px',
      flex:'none', background:LK.bgPrimary }}>
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

// ── Bottom navigation ─────────────────────────────────────────────
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

// ── Phone frame ───────────────────────────────────────────────────
function PhoneFrame({ children }) {
  return (
    <div style={{
      width:390, height:820, background:LK.bgPrimary, borderRadius:36,
      border:'10px solid #111', boxShadow:'0 30px 80px rgba(0,0,0,.28)',
      overflow:'hidden', display:'flex', flexDirection:'column', position:'relative',
      fontFamily:LK.font,
    }}>
      {children}
      {/* gesture pill */}
      <div style={{ position:'absolute', bottom:6, left:'50%', transform:'translateX(-50%)',
        width:120, height:4, borderRadius:2, background:LK.textPrimary, opacity:.25 }} />
    </div>
  );
}

Object.assign(window, { LK, Icon, SignalBars, Badge, Avatar, hexA, StatusBar, TopBar, BottomNav, NAV, PhoneFrame });
