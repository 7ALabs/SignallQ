/* SignallQ UI Kit — Dispositivos com card de anúncio nativo (issue #555)
   Ancorado em DispositivosScreen.kt real: topbar "Dispositivos na rede" com ícone
   Devices + refresh; seções INFRAESTRUTURA / PONTOS DE ACESSO / DISPOSITIVOS em
   LazyColumn; linhas com ícone em chip quadrado 40dp + badge de tipo. */

function DispositivosTopBar({ onBack }) {
  return (
    <div style={{ height:64, display:'flex', alignItems:'center', padding:'0 8px', flex:'none',
      background:LK.bgPrimary, borderBottom:`1px solid ${LK.border}` }}>
      <button onClick={onBack} style={{ width:48, background:'none', border:0, cursor:'pointer',
        display:'flex', justifyContent:'flex-start' }}>
        <Icon name="arrow_back" size={22} color={LK.textPrimary} />
      </button>
      <div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center', gap:8 }}>
        <Icon name="devices" size={18} color={LK.textPrimary} />
        <span style={{ font:`500 16px/1 ${LK.font}` }}>Dispositivos na rede</span>
      </div>
      <button style={{ width:48, background:'none', border:0, cursor:'pointer', display:'flex', justifyContent:'flex-end' }}>
        <Icon name="refresh" size={22} color={LK.textPrimary} />
      </button>
    </div>
  );
}

function SectionHeaderRow({ title }) {
  return (
    <div style={{ padding:'20px 16px 8px' }}>
      <span style={{ font:`700 11px/1 ${LK.font}`, color:LK.textTertiary, letterSpacing:'.4px' }}>{title}</span>
    </div>
  );
}

function BadgePill({ label, bg, fg }) {
  return (
    <span style={{ display:'inline-block', borderRadius:4, background:bg, color:fg,
      font:`700 10px/1 ${LK.font}`, padding:'4px 8px' }}>{label}</span>
  );
}

function DeviceRow({ icon, iconBg, iconFg, title, subtitle, badge, ip, isGateway, weakSignal }) {
  return (
    <div style={{ display:'flex', alignItems:'center', gap:12, padding:'13px 16px',
      borderTop:`0.5px solid ${hexA(LK.border,.7)}` }}>
      <div style={{ width:40, height:40, borderRadius:12, flex:'none', background:iconBg,
        display:'flex', alignItems:'center', justifyContent:'center' }}>
        <Icon name={icon} size={20} color={iconFg} />
      </div>
      <div style={{ flex:1, minWidth:0 }}>
        <div style={{ display:'flex', alignItems:'center', gap:6 }}>
          <span style={{ font:`500 14px/1.3 ${LK.font}`, color:LK.textPrimary, whiteSpace:'nowrap',
            overflow:'hidden', textOverflow:'ellipsis' }}>{title}</span>
          {isGateway && weakSignal && <Icon name="warning" size={14} color={LK.warning} />}
        </div>
        <div style={{ font:`400 11px/1.3 ${LK.font}`, color:LK.textTertiary }}>{subtitle}</div>
      </div>
      {badge && <BadgePill {...badge} />}
      {ip && <span style={{ font:`400 11px/1 ${LK.font}`, color:LK.textTertiary, marginLeft:4 }}>{ip}</span>}
      <Icon name="chevron_right" size={16} color={LK.textTertiary} />
    </div>
  );
}

function DispositivosScreenMock({ onBack, adsEnabled, weakSignal }) {
  const gatewaySubtitle = weakSignal
    ? '192.168.1.1 · sinal fraco em cômodos afastados'
    : '192.168.1.1 · 2,4G + 5G · 6 clientes';

  return (
    <div style={{ flex:1, display:'flex', flexDirection:'column', background:LK.bgPrimary, overflow:'hidden' }}>
      <DispositivosTopBar onBack={onBack} />
      <div style={{ flex:1, overflowY:'auto' }}>
        <SectionHeaderRow title="INFRAESTRUTURA (1)" />
        <DeviceRow icon="router" iconBg={hexA(LK.accent,.12)} iconFg={LK.accent}
          title="Roteador principal" subtitle={gatewaySubtitle} isGateway weakSignal={weakSignal}
          badge={{ label:'Roteador', bg:hexA(LK.accent,.10), fg:LK.accent }} />

        <SectionHeaderRow title="PONTOS DE ACESSO (1)" />
        <DeviceRow icon="cell_tower" iconBg={hexA(LK.success,.12)} iconFg={LK.success}
          title="Nó #1" subtitle="192.168.1.14"
          badge={{ label:'AP Mesh', bg:hexA(LK.success,.10), fg:LK.success }} />

        <SectionHeaderRow title="DISPOSITIVOS (4)" />
        {[
          { icon:'smartphone', tone:LK.accent, title:'iPhone de Luiz', sub:'Apple', ip:'192.168.1.22', badge:{label:'Este aparelho', bg:hexA(LK.accent,.10), fg:LK.accent} },
          { icon:'laptop', tone:LK.success, title:'MacBook-Pro', sub:'Apple', ip:'192.168.1.31' },
        ].map(d => (
          <DeviceRow key={d.title} icon={d.icon} iconBg={hexA(d.tone,.12)} iconFg={d.tone}
            title={d.title} subtitle={d.sub} ip={d.ip} badge={d.badge} />
        ))}

        {/* ── Slot de anúncio nativo — sempre presente (issue #555, correção 2026-07-12
            + feedback Luiz 2026-07-12: fica DENTRO da lista de dispositivos, não em
            INFRAESTRUTURA — infraestrutura é "seu equipamento real", misturar oferta
            ali pesava a confusão "isso é meu / isso é anúncio". Aqui é só mais uma
            linha entre linhas de device, com o mesmo disclosure que a separa de todas
            elas. weakSignal só muda a oferta, não a presença. Fonte "admob": esta tela
            não roda diagnóstico, então não há evidência pra justificar "Parceiro". */}
        {adsEnabled && (
          <NativeAdListRow
            source="admob"
            brandLetter={weakSignal ? 'M' : 'T'}
            brandColor={weakSignal ? LK.accentBlue : LK.accent}
            headline={weakSignal ? 'Mesh Wi-Fi 6 — cobre os cômodos afastados' : 'Wi-Fi 6E — mais capacidade pra sua rede'}
            body={weakSignal ? 'Seu roteador tem zona de sinal fraco detectada' : 'Compatível com o seu roteador atual'}
            onCta={() => {}}
          />
        )}

        {[
          { icon:'lightbulb', tone:LK.warning, title:'Lâmpada Sala', sub:'TP-Link', ip:'192.168.1.44' },
          { icon:'devices_other', tone:LK.textTertiary, title:'192.168.1.55', sub:'Desconhecido', ip:'192.168.1.55' },
        ].map(d => (
          <DeviceRow key={d.title} icon={d.icon} iconBg={hexA(d.tone,.12)} iconFg={d.tone}
            title={d.title} subtitle={d.sub} ip={d.ip} badge={d.badge} />
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { DispositivosScreenMock });
