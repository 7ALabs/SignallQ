/* SignallQ UI Kit — Histórico com card de anúncio nativo (issue #555)
   Baseado em ui_kits/android/screens.jsx (HistoricoScreen). Início/Sinal/Ajustes
   fora de escopo desta rodada — não recriados aqui. */

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

// ── Histórico ─────────────────────────────────────────────────────
function HistoricoScreen({ adsEnabled, weakSignal }) {
  // 7 cols x 5 rows uptime grid — com weakSignal, mais células de alerta
  // (recorrência é exatamente o gatilho que a issue #555 exige pra monetizar).
  const cells = Array.from({length:35}, (_,i) => {
    const r = (i*53)%100;
    const bar = weakSignal ? 78 : 92;
    return r>bar ? LK.error : r>(bar-12) ? LK.warning : LK.success;
  });
  const estabilidade = weakSignal ? 61 : 89;

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
          {weakSignal ? (
            <React.Fragment>Sua internet ficou <b style={{ color:LK.warning }}>estável em {estabilidade}%</b> do
            tempo nas últimas 24 h. Houve várias oscilações de sinal Wi-Fi ao longo do dia — um padrão recorrente,
            não pontual.</React.Fragment>
          ) : (
            <React.Fragment>Sua internet ficou <b style={{ color:LK.success }}>estável em {estabilidade}%</b> do
            tempo nas últimas 24 h. Houve uma breve oscilação por volta das 14 h, mas a conexão se recuperou
            sozinha.</React.Fragment>
          )}
        </div>
      </Card>

      {/* ── Slot de anúncio nativo — sempre presente (issue #555, correção 2026-07-12).
          weakSignal só decide a oferta (mesh recorrente vs. genérica) e a fonte —
          recorrência é evidência real (grid de 35 medições), então vira "partner";
          sem recorrência marcada, cai pra fallback "admob" — não a presença. */}
      {adsEnabled && (
        <NativeAdCard
          source={weakSignal ? 'partner' : 'admob'}
          brandLetter={weakSignal ? 'M' : 'R'}
          brandColor={weakSignal ? LK.accentBlue : LK.accent}
          headline={weakSignal
            ? 'Instabilidade recorrente? Um mesh resolve zona morta'
            : 'Roteador com QoS — prioriza o que importa na sua rede'}
          body={weakSignal
            ? 'Seu histórico mostra queda de sinal repetida nos últimos dias.'
            : 'Baseado no seu padrão de uso das últimas medições.'}
          ctaLabel="Ver oferta"
          onCta={() => {}}
          onDismiss={() => {}}
        />
      )}

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

Object.assign(window, { Card, Overline, ScreenScroll, HistoricoScreen });
