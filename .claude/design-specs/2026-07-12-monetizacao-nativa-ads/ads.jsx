/* SignallQ UI Kit — Native Ad components (issue #555)
   Disclosure obrigatório (Google native-ad UX guidelines): nunca imita um componente
   orgânico do app — borda tracejada + badge fixo diferenciam de qualquer Card real.
   Nunca usa violeta sólido no CTA (reservado para ações primárias do app: Iniciar teste,
   Conversar com IA). Sem foto/hero — mesmo motivo de ícone-em-chip do resto do DS.

   Duas origens, duas variantes de badge (2026-07-12, feedback Luiz):
   - "admob"   → "Patrocinado" — native_ad_fallback puro, sem relação comercial com o
     SignallQ além do ad network. Ícone `campaign`, neutro (textTertiary).
   - "partner" → "Parceiro"    — affiliate_product/partner_offer do coreRecommendation,
     escolhido porque casa com evidência real do diagnóstico. Ícone `storefront`, mesmo
     fundo neutro mas texto/ícone em accentBlue — sinaliza "curadoria do app", nunca usa
     o violeta de marca pra não competir com CTA primário. Hoje só a variante "admob" é
     usada de fato (sem parceiro real ainda) — a variante "partner" existe no componente
     pronta pra quando houver. */

function AdBadge({ source = 'admob', style }) {
  const isPartner = source === 'partner';
  const tone = isPartner ? LK.accentBlue : LK.textTertiary;
  const label = isPartner ? 'Parceiro' : 'Patrocinado';
  const icon = isPartner ? 'storefront' : 'campaign';
  return (
    <div style={{ display:'inline-flex', alignItems:'center', gap:4,
      padding:'3px 8px', borderRadius:999, border:`1px solid ${LK.border}`,
      background:LK.bgSecondary, flex:'none', ...style }}>
      <Icon name={icon} size={12} color={tone} />
      <span style={{ font:`700 10px/1 ${LK.font}`, color:tone,
        letterSpacing:'.3px', textTransform:'uppercase' }}>{label}</span>
    </div>
  );
}

// ── Card cheio — usado no Resultado e no Histórico (contextual, com evidência) ──
function NativeAdCard({ headline, body, ctaLabel = 'Ver oferta', brandLetter = 'M',
  brandColor = LK.accentBlue, source = 'admob', onCta, onDismiss }) {
  return (
    <div style={{ background:LK.bgCard, border:`1px dashed ${LK.border}`, borderRadius:LK.rCard,
      padding:16, marginTop:18 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:12 }}>
        <AdBadge source={source} />
        <button onClick={onDismiss} style={{ background:'none', border:0, cursor:'pointer', padding:2,
          display:'flex' }}>
          <Icon name="close" size={16} color={LK.textTertiary} />
        </button>
      </div>
      <div style={{ display:'flex', gap:12, alignItems:'flex-start' }}>
        <div style={{ width:44, height:44, borderRadius:12, flex:'none',
          background:hexA(brandColor,.14), color:brandColor, display:'flex',
          alignItems:'center', justifyContent:'center', font:`700 18px/1 ${LK.font}` }}>{brandLetter}</div>
        <div style={{ flex:1, minWidth:0 }}>
          <div style={{ font:`600 14px/1.35 ${LK.font}`, color:LK.textPrimary }}>{headline}</div>
          <div style={{ font:`400 12px/1.45 ${LK.font}`, color:LK.textSecondary, marginTop:3 }}>{body}</div>
        </div>
      </div>
      <button onClick={onCta} style={{ width:'100%', marginTop:12, cursor:'pointer',
        background:'transparent', color:LK.accent, font:`600 13px/1 ${LK.font}`,
        border:`1px solid ${hexA(LK.accent,.35)}`, borderRadius:LK.rBtn, padding:'11px' }}>{ctaLabel}</button>
    </div>
  );
}

// ── Linha compacta — usada na Velocidade (fallback genérico, espaço vazio) ──
function NativeAdRow({ headline, body, brandLetter = 'X', brandColor = LK.accentBlue, source = 'admob', onCta }) {
  return (
    <div onClick={onCta} style={{ display:'flex', alignItems:'center', gap:12, cursor:'pointer',
      background:LK.bgCard, border:`1px dashed ${LK.border}`, borderRadius:LK.rCard,
      padding:14, marginTop:18 }}>
      <div style={{ width:38, height:38, borderRadius:10, flex:'none', background:hexA(brandColor,.14),
        color:brandColor, display:'flex', alignItems:'center', justifyContent:'center',
        font:`700 16px/1 ${LK.font}` }}>{brandLetter}</div>
      <div style={{ flex:1, minWidth:0 }}>
        <AdBadge source={source} style={{ marginBottom:5 }} />
        <div style={{ font:`600 13px/1.3 ${LK.font}`, color:LK.textPrimary,
          whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{headline}</div>
        <div style={{ font:`400 11px/1.3 ${LK.font}`, color:LK.textSecondary,
          whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{body}</div>
      </div>
      <Icon name="chevron_right" size={18} color={LK.textTertiary} />
    </div>
  );
}

// ── Linha de lista — usada em Dispositivos, dentro da própria lista de devices ──
function NativeAdListRow({ headline, body, brandLetter = 'M', brandColor = LK.accentBlue, source = 'admob', onCta }) {
  return (
    <div onClick={onCta} style={{ display:'flex', alignItems:'center', gap:12, padding:'13px 16px',
      cursor:'pointer', borderTop:`1px dashed ${LK.border}`, borderBottom:`1px dashed ${LK.border}`,
      background:hexA(LK.textTertiary,.04) }}>
      <div style={{ width:40, height:40, borderRadius:10, flex:'none', background:hexA(brandColor,.14),
        color:brandColor, display:'flex', alignItems:'center', justifyContent:'center',
        font:`700 16px/1 ${LK.font}` }}>{brandLetter}</div>
      <div style={{ flex:1, minWidth:0 }}>
        <div style={{ font:`600 14px/1.25 ${LK.font}`, color:LK.textPrimary,
          whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{headline}</div>
        <div style={{ font:`400 11px/1.3 ${LK.font}`, color:LK.textSecondary,
          whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{body}</div>
      </div>
      <AdBadge source={source} />
    </div>
  );
}

Object.assign(window, { AdBadge, NativeAdCard, NativeAdRow, NativeAdListRow });
