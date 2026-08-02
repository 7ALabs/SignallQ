/* SignallQ UI Kit — Diagnóstico guiado por objetivo + Modo gamer (GH#1474, Feature #550)

   Anexado ao fluxo real "Resultado" (speedtest.jsx). Reaproveita LK/Icon/Badge/hexA/Overline
   de chrome.jsx e screens.jsx — não redefine tokens.

   Conteúdo de "evidências"/"ações" por objetivo e por jogo é REPRESENTATIVO (mock fixo pra
   demonstrar o layout), a lógica real de classificação/branching é do motor Android
   (Camilo, #1475/#1476) — não é o resultado final de produção.
*/

// ── Dados: 7 objetivos fechados ─────────────────────────────────────
const OBJETIVOS = [
  {
    id: 'cai', icon: 'wifi_off',
    titulo: 'Internet cai ou oscila',
    sub: 'A conexão para de funcionar ou fica instável por alguns segundos',
    perguntas: [
      { texto: 'Quando isso costuma acontecer?', opcoes: ['A qualquer momento', 'Em horários de pico', 'Só durante uso pesado (jogos, streaming)', 'Só em alguns cômodos da casa'] },
      { texto: 'O que você percebe quando acontece?', opcoes: ['O Wi-Fi some da lista de redes', 'Continua conectado mas trava', 'Desconecta e reconecta sozinho', 'Não sei dizer'] },
    ],
  },
  {
    id: 'video', icon: 'live_tv',
    titulo: 'Vídeos travam ou dão buffer',
    sub: 'Streaming trava, fica "carregando" ou perde qualidade sozinho',
    perguntas: [
      { texto: 'O que acontece quando o vídeo trava?', opcoes: ['Fica "carregando" (buffer)', 'Cai a qualidade sozinho', 'Trava e o app fecha', 'Só em determinado app'] },
      { texto: 'Isso acontece em qual conexão?', opcoes: ['Wi-Fi', 'Dados móveis', 'Nas duas', 'Só percebi agora'] },
    ],
  },
  {
    id: 'jogos', icon: 'sports_esports',
    titulo: 'Jogos com lag ou ping alto',
    sub: 'Atraso, travadas ou desconexões durante partidas online',
    perguntas: [
      { texto: 'Em qual conexão você joga?', opcoes: ['Wi-Fi', 'Cabo de rede', 'Dados móveis'] },
      { texto: 'Com que frequência isso acontece?', opcoes: ['Direto, quase sempre', 'Só em horário de pico', 'De vez em quando, sem padrão'] },
    ],
  },
  {
    id: 'chamadas', icon: 'videocam',
    titulo: 'Chamadas de vídeo congelam',
    sub: 'Imagem trava, áudio corta ou a chamada cai',
    perguntas: [
      { texto: 'O que costuma acontecer na chamada?', opcoes: ['Imagem congela, áudio segue', 'Áudio corta ou fica robótico', 'A chamada cai e reconecta', 'Os dois travam juntos'] },
      { texto: 'Piora se outra pessoa em casa também estiver usando a internet?', opcoes: ['Sim, bastante', 'Um pouco', 'Não muda', 'Não testei'] },
    ],
  },
  {
    id: 'sites', icon: 'language',
    titulo: 'Sites demoram para carregar',
    sub: 'Páginas demoram, ficam "girando" ou falham ao abrir',
    perguntas: [
      { texto: 'Isso acontece com...', opcoes: ['Qualquer site', 'Só sites específicos', 'Só a primeira página (depois melhora)'] },
      { texto: 'O problema é mais forte em qual conexão?', opcoes: ['Wi-Fi', 'Dados móveis', 'Nas duas igual'] },
    ],
  },
  {
    id: 'velocidade', icon: 'speed',
    titulo: 'Velocidade não chega no contratado',
    sub: 'O teste mostra menos do que você paga pela sua operadora',
    perguntas: [
      { texto: 'Você já comparou com outro teste?', opcoes: ['Só testei pelo SignallQ', 'Comparei com outro app ou site'] },
      { texto: 'Este teste foi feito...', opcoes: ['Perto do roteador, por cabo', 'Perto do roteador, por Wi-Fi', 'Longe do roteador, por Wi-Fi'] },
    ],
  },
  {
    id: 'wifi-operadora', icon: 'compare_arrows',
    titulo: 'Não sei se é o Wi-Fi ou a operadora',
    sub: 'Quer descobrir se o problema é do roteador ou da internet contratada',
    perguntas: [
      { texto: 'O problema muda se você desligar o Wi-Fi e usar dados móveis?', opcoes: ['Sim, melhora muito', 'Sim, um pouco', 'Não muda nada', 'Ainda não testei'] },
      { texto: 'Outros dispositivos em casa têm o mesmo problema?', opcoes: ['Sim, todos', 'Só alguns', 'Não, só este aparelho'] },
    ],
  },
];

// Resultado representativo por objetivo — ver nota do topo do arquivo.
const RESULTADOS_GUIADO = {
  'cai': { tone: 'error', titulo: 'Sinal de instabilidade real na sua rede',
    motor: 'Perda estimada 3.8% e oscilação de 61 ms nos últimos testes — acima do esperado para uma conexão estável.',
    evidencias: [
      { icon: 'trending_down', label: 'Perda estimada', valor: '3,8%', tone: 'error' },
      { icon: 'graphic_eq', label: 'Oscilação (jitter)', valor: '61 ms', tone: 'error' },
      { icon: 'wifi_off', label: 'Quedas detectadas', valor: '4 nos últimos testes', tone: 'warning' },
    ],
    acoes: ['Reposicione o roteador longe de paredes e eletrônicos', 'Teste por cabo pra isolar se é Wi-Fi ou operadora'],
    ia: 'O padrão de quedas curtas somado à perda de pacotes normalmente indica saturação do canal Wi-Fi ou instabilidade da própria operadora. Testar por cabo ajuda a isolar qual dos dois é a causa.' },
  'video': { tone: 'warning', titulo: 'Streaming sofre em horário de pico',
    motor: 'O download caiu para 38% do pico registrado durante rajadas simultâneas, com atraso sob carga de 210 ms.',
    evidencias: [
      { icon: 'speed', label: 'Queda em rajada', valor: '−62% do pico', tone: 'warning' },
      { icon: 'hourglass_bottom', label: 'Atraso sob carga', valor: '210 ms', tone: 'warning' },
      { icon: 'monitoring', label: 'Estabilidade', valor: '71%', tone: 'warning' },
    ],
    acoes: ['Pause downloads em segundo plano durante o streaming', 'Teste novamente fora do horário de pico'],
    ia: 'Isso costuma acontecer quando outros dispositivos ou apps disputam banda no mesmo momento — o link não cai, mas fica menos disponível pra manter a qualidade máxima do vídeo.' },
  'jogos': { tone: 'error', titulo: 'Latência sob carga está prejudicando suas partidas',
    motor: 'Latência sob carga de 187 ms e jitter de 44 ms — faixa crítica para jogos competitivos.',
    evidencias: [
      { icon: 'hourglass_bottom', label: 'Latência sob carga', valor: '187 ms', tone: 'error' },
      { icon: 'graphic_eq', label: 'Jitter', valor: '44 ms', tone: 'error' },
      { icon: 'trending_down', label: 'Perda estimada', valor: '1,2%', tone: 'warning' },
    ],
    acoes: ['Jogue com cabo de rede em vez de Wi-Fi', 'Ative priorização de jogos (QoS) no roteador, se disponível'],
    ia: 'Latência que sobe quando a rede está sob carga é sinal clássico de disputa de banda (bufferbloat) — outro dispositivo usando a internet ao mesmo tempo atrasa os pacotes do jogo.' },
  'chamadas': { tone: 'warning', titulo: 'Chamadas sofrem quando a rede está ocupada',
    motor: 'Jitter de 38 ms e queda de 22% no upload durante uso simultâneo de outros dispositivos.',
    evidencias: [
      { icon: 'graphic_eq', label: 'Jitter', valor: '38 ms', tone: 'warning' },
      { icon: 'upload', label: 'Upload sob carga', valor: '−22%', tone: 'warning' },
    ],
    acoes: ['Peça pra outras pessoas em casa pausarem downloads durante a chamada', 'Prefira cabo de rede pra chamadas importantes'],
    ia: 'Vídeo e áudio de chamada são sensíveis a pequenas variações de latência — mesmo sem perda de conexão, uma rede ocupada já é suficiente pra imagem congelar ou o áudio cortar.' },
  'sites': { tone: 'warning', titulo: 'Resolução de nomes (DNS) um pouco lenta',
    motor: 'Latência de DNS de 96 ms — acima do ideal, mesmo com latência geral da rede dentro do esperado.',
    evidencias: [
      { icon: 'dns', label: 'Latência DNS', valor: '96 ms', tone: 'warning' },
      { icon: 'hourglass_bottom', label: 'Latência geral', valor: '54 ms', tone: 'success' },
    ],
    acoes: ['Troque o DNS da rede para um provedor mais rápido nas configurações', 'Teste novamente em outro navegador pra descartar cache'],
    ia: 'Quando a latência geral está boa mas os sites ainda demoram pra abrir, o gargalo costuma estar na resolução do nome do site (DNS), não na velocidade da conexão em si.' },
  'velocidade': { tone: 'error', titulo: 'Resultado abaixo do plano contratado',
    motor: 'Download mediu 38% do valor esperado pelo plano, em teste via Wi-Fi próximo ao roteador.',
    evidencias: [
      { icon: 'speed', label: 'Download vs. contratado', valor: '38%', tone: 'error' },
      { icon: 'wifi', label: 'Conexão do teste', valor: 'Wi-Fi, próximo ao roteador', tone: 'info' },
    ],
    acoes: ['Repita o teste conectado por cabo de rede', 'Se o resultado persistir por cabo, contate a operadora com este resultado'],
    ia: 'Testar por Wi-Fi mede a rede da sua casa inteira, não só o link da operadora. Repetir por cabo é a forma mais confiável de saber se o problema é da internet contratada ou do próprio roteador.' },
  'wifi-operadora': { tone: 'info', titulo: 'Problema parece estar entre roteador e dispositivos',
    motor: 'A conexão via dados móveis apresentou latência e perda melhores que o Wi-Fi no mesmo local.',
    evidencias: [
      { icon: 'wifi', label: 'Wi-Fi — perda estimada', valor: '2,1%', tone: 'warning' },
      { icon: 'signal_cellular_alt', label: 'Dados móveis — perda estimada', valor: '0,1%', tone: 'success' },
    ],
    acoes: ['Reposicione o roteador ou troque o canal Wi-Fi', 'Se o problema for só de um aparelho, teste esquecer e reconectar a rede nele'],
    ia: 'Como a internet contratada (medida via dados móveis, quando disponível, ou pelo histórico da operadora) está com resultado melhor que o Wi-Fi local, o problema tende a estar entre o roteador e os dispositivos — não na internet em si.' },
};

// ── Dados: Modo gamer ────────────────────────────────────────────────
const GAMES = [
  { id: 'freefire', nome: 'Free Fire', categoria: 'Battle royale mobile', letra: 'FF', cor: '#F97316' },
  { id: 'valorant', nome: 'Valorant', categoria: 'FPS competitivo', letra: 'VL', cor: '#FF4655' },
  { id: 'codm', nome: 'Call of Duty Mobile', categoria: 'FPS competitivo', letra: 'CD', cor: '#111827' },
  { id: 'fifa', nome: 'EA FC', categoria: 'Esporte online', letra: 'FC', cor: '#00A859' },
  { id: 'fortnite', nome: 'Fortnite', categoria: 'Battle royale', letra: 'FN', cor: '#7C3AED' },
  { id: 'lol', nome: 'League of Legends', categoria: 'MOBA', letra: 'LoL', cor: '#0BC6E3' },
  { id: 'minecraft', nome: 'Minecraft', categoria: 'Mundo aberto', letra: 'MC', cor: '#3F8E2F' },
  { id: 'roblox', nome: 'Roblox', categoria: 'Jogo casual/social', letra: 'RB', cor: '#111827' },
  { id: 'genshin', nome: 'Genshin Impact', categoria: 'RPG mundo aberto', letra: 'GI', cor: '#2563EB' },
];
const CATEGORIAS_GENERICAS = [
  { id: 'fps', nome: 'FPS competitivo' },
  { id: 'battle-royale', nome: 'Battle royale' },
  { id: 'moba', nome: 'MOBA' },
  { id: 'casual', nome: 'Jogo casual ou mobile' },
  { id: 'cloud', nome: 'Streaming em nuvem (cloud gaming)' },
  { id: 'outro', nome: 'Outro tipo de jogo' },
];
const DEVICES = [
  { id: 'ps', nome: 'PS5 / PS4', icon: null, letra: 'PS', cor: '#0072CE' },
  { id: 'xbox', nome: 'Xbox', icon: null, letra: 'XB', cor: '#107C10' },
  { id: 'pc', nome: 'PC', icon: 'computer', letra: null, cor: '#5B21D6' },
  { id: 'android', nome: 'Android', icon: 'phone_android', letra: null, cor: '#3DDC84' },
  { id: 'iphone', nome: 'iPhone', icon: 'phone_iphone', letra: null, cor: '#111827' },
  { id: 'switch', nome: 'Switch', icon: null, letra: 'SW', cor: '#E60012' },
  { id: 'tv', nome: 'TV / Cloud gaming', icon: 'cast', letra: null, cor: '#2851B8' },
];

// ── Primitivos compartilhados desta feature ─────────────────────────
function toneColor(tone) {
  return { success: LK.success, warning: LK.warning, error: LK.error, info: LK.accentBlue }[tone] || LK.textSecondary;
}

function SubTopBar({ title, onBack, step }) {
  return (
    <div style={{ height: 64, display: 'flex', alignItems: 'center', padding: '0 8px', flex: 'none', background: LK.bgPrimary, borderBottom: `1px solid ${hexA(LK.border, .3)}` }}>
      <button onClick={onBack} style={{ width: 40, height: 40, border: 0, background: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Icon name="arrow_back" size={22} color={LK.textPrimary} />
      </button>
      <span style={{ flex: 1, textAlign: 'center', font: `500 17px/1 ${LK.font}`, color: LK.textPrimary, marginRight: 40 }}>{title}</span>
      {step && <span style={{ position: 'absolute', right: 16, font: `600 12px/1 ${LK.font}`, color: LK.textTertiary }}>{step}</span>}
    </div>
  );
}

function ProgressDots({ total, current }) {
  return (
    <div style={{ display: 'flex', gap: 6, padding: '14px 20px 0' }}>
      {Array.from({ length: total }).map((_, i) => (
        <div key={i} style={{ flex: 1, height: 4, borderRadius: 2, background: i <= current ? LK.accent : LK.bgSecondary }} />
      ))}
    </div>
  );
}

function ChipOpcao({ label, selected, onClick }) {
  return (
    <button onClick={onClick} style={{
      width: '100%', textAlign: 'left', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 10,
      padding: '14px 16px', marginBottom: 8, borderRadius: LK.rField,
      border: `1.5px solid ${selected ? LK.accent : LK.border}`,
      background: selected ? hexA(LK.accent, .08) : LK.bgPrimary,
    }}>
      <div style={{
        width: 20, height: 20, borderRadius: '50%', flex: 'none',
        border: `2px solid ${selected ? LK.accent : LK.border}`,
        display: 'flex', alignItems: 'center', justifyContent: 'center', background: selected ? LK.accent : 'transparent',
      }}>
        {selected && <Icon name="check" size={13} color="#fff" />}
      </div>
      <span style={{ font: `${selected ? 600 : 400} 14px/1.3 ${LK.font}`, color: LK.textPrimary }}>{label}</span>
    </button>
  );
}

function StatusBanner({ tone, titulo, mensagem }) {
  const cor = toneColor(tone);
  const icon = { success: 'check_circle', info: 'info', warning: 'warning', error: 'error' }[tone] || 'info';
  return (
    <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start', background: hexA(cor, .12), borderRadius: LK.rCard, padding: 16 }}>
      <Icon name={icon} size={22} color={cor} />
      <div>
        <div style={{ font: `600 15px/1.3 ${LK.font}`, color: cor }}>{titulo}</div>
        <div style={{ font: `400 13px/1.4 ${LK.font}`, color: LK.textSecondary, marginTop: 4 }}>{mensagem}</div>
      </div>
    </div>
  );
}

/* Bloco central da regra de produto (issue #550/#1474): motor local decide o status,
   IA só explica. Visualmente separado em dois containers distintos — não é só microcopy. */
function AiVsMotorExplainer({ resultado }) {
  return (
    <div>
      <Overline style={{ marginBottom: 8 }}>Como chegamos a esse resultado</Overline>
      <div style={{ border: `1px solid ${LK.border}`, borderRadius: LK.rCard, padding: 16, background: LK.bgSecondary }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
          <Icon name="verified" size={16} color={LK.textSecondary} />
          <span style={{ font: `600 11px/1 ${LK.font}`, color: LK.textSecondary, letterSpacing: '.3px', textTransform: 'uppercase' }}>Medido pelo motor SignallQ</span>
        </div>
        {resultado.evidencias.map((e, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '7px 0' }}>
            <Icon name={e.icon} size={17} color={toneColor(e.tone)} />
            <span style={{ flex: 1, font: `400 13px/1 ${LK.font}`, color: LK.textPrimary }}>{e.label}</span>
            <span style={{ font: `700 13px/1 ${LK.font}`, color: toneColor(e.tone) }}>{e.valor}</span>
          </div>
        ))}
      </div>
      <div style={{ border: `1px solid ${hexA(LK.accent, .3)}`, borderRadius: LK.rCard, padding: 16, background: hexA(LK.accent, .06), marginTop: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
          <Icon name="auto_awesome" size={16} color={LK.accent} />
          <span style={{ font: `600 11px/1 ${LK.font}`, color: LK.accent, letterSpacing: '.3px', textTransform: 'uppercase' }}>Explicado por IA</span>
        </div>
        <div style={{ font: `400 13px/1.5 ${LK.font}`, color: LK.textPrimary }}>{resultado.ia}</div>
      </div>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 6, marginTop: 10 }}>
        <Icon name="lock" size={13} color={LK.textTertiary} />
        <span style={{ font: `400 11px/1.4 ${LK.font}`, color: LK.textTertiary }}>
          A IA só explica o resultado — quem decide o status é sempre o motor de diagnóstico local, com base nos números medidos acima.
        </span>
      </div>
    </div>
  );
}

function AcoesCard({ acoes }) {
  return (
    <div>
      <Overline style={{ margin: '18px 0 8px' }}>Ações recomendadas</Overline>
      <div style={{ background: LK.bgSecondary, borderRadius: LK.rCard, padding: '4px 16px' }}>
        {acoes.map((a, i) => (
          <div key={i} style={{ display: 'flex', gap: 10, alignItems: 'flex-start', padding: '12px 0', borderTop: i > 0 ? `1px solid ${LK.border}` : 'none' }}>
            <Icon name="task_alt" size={18} color={LK.success} />
            <span style={{ font: `400 13px/1.4 ${LK.font}`, color: LK.textPrimary }}>{a}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Diagnóstico guiado ───────────────────────────────────────────────
function DiagnosticoGuiadoObjetivos({ onSelect, onBack }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Diagnóstico guiado" onBack={onBack} />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 24px' }}>
        <div style={{ font: `400 13px/1.4 ${LK.font}`, color: LK.textSecondary, marginBottom: 14 }}>
          Qual dessas situações mais parece com o que você está vivendo?
        </div>
        {OBJETIVOS.map(obj => (
          <button key={obj.id} onClick={() => onSelect(obj)} style={{
            width: '100%', textAlign: 'left', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 12,
            padding: 14, marginBottom: 8, borderRadius: LK.rCard, border: `1px solid ${LK.border}`, background: LK.bgPrimary,
          }}>
            <div style={{ width: 40, height: 40, borderRadius: 12, flex: 'none', background: hexA(LK.accent, .1), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Icon name={obj.icon} size={20} color={LK.accent} />
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ font: `600 14px/1.3 ${LK.font}`, color: LK.textPrimary }}>{obj.titulo}</div>
              <div style={{ font: `400 12px/1.3 ${LK.font}`, color: LK.textSecondary, marginTop: 2 }}>{obj.sub}</div>
            </div>
            <Icon name="chevron_right" size={20} color={LK.textTertiary} />
          </button>
        ))}
      </div>
    </div>
  );
}

function DiagnosticoGuiadoPerguntas({ objetivo, onConcluir, onBack }) {
  const [passo, setPasso] = React.useState(0);
  const [respostas, setRespostas] = React.useState([]);
  const pergunta = objetivo.perguntas[passo];
  const total = objetivo.perguntas.length;
  const respondido = respostas[passo] != null;

  const escolher = (opcao) => setRespostas(r => { const n = [...r]; n[passo] = opcao; return n; });
  const avancar = () => {
    if (passo < total - 1) setPasso(passo + 1);
    else onConcluir(respostas);
  };
  const voltar = () => { if (passo === 0) onBack(); else setPasso(passo - 1); };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title={objetivo.titulo} onBack={voltar} step={`${passo + 1}/${total}`} />
      <ProgressDots total={total} current={passo} />
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px 20px 16px' }}>
        <div style={{ font: `600 17px/1.35 ${LK.font}`, color: LK.textPrimary, marginBottom: 16 }}>{pergunta.texto}</div>
        {pergunta.opcoes.map(op => (
          <ChipOpcao key={op} label={op} selected={respostas[passo] === op} onClick={() => escolher(op)} />
        ))}
      </div>
      <div style={{ padding: '8px 20px 22px' }}>
        <button onClick={avancar} disabled={!respondido} style={{
          width: '100%', border: 0, cursor: respondido ? 'pointer' : 'default',
          background: respondido ? LK.accent : LK.bgSecondary, color: respondido ? '#fff' : LK.textTertiary,
          font: `600 15px/1 ${LK.font}`, borderRadius: LK.rBtn, padding: 15,
        }}>{passo < total - 1 ? 'Avançar' : 'Ver resultado'}</button>
      </div>
    </div>
  );
}

function DiagnosticoGuiadoResultado({ objetivo, onOutroObjetivo, onHome, onModoGamer }) {
  const r = RESULTADOS_GUIADO[objetivo.id];
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Resultado do diagnóstico" onBack={onOutroObjetivo} />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px 24px' }}>
        <StatusBanner tone={r.tone} titulo={r.titulo} mensagem={r.motor} />
        <div style={{ marginTop: 18 }}>
          <AiVsMotorExplainer resultado={r} />
        </div>
        <AcoesCard acoes={r.acoes} />
        {onModoGamer && (
          <button onClick={onModoGamer} style={{
            width: '100%', marginTop: 18, border: `1px solid ${LK.accent}`, cursor: 'pointer', background: 'transparent',
            color: LK.accent, font: `600 14px/1 ${LK.font}`, borderRadius: LK.rBtn, padding: 14,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}><Icon name="sports_esports" size={18} color={LK.accent} />Ver diagnóstico por jogo (Modo gamer)</button>
        )}
        <button onClick={onOutroObjetivo} style={{
          width: '100%', marginTop: 10, border: `1px solid ${LK.border}`, cursor: 'pointer', background: 'transparent',
          color: LK.textPrimary, font: `500 14px/1 ${LK.font}`, borderRadius: LK.rBtn, padding: 14,
        }}>Escolher outra situação</button>
        <button onClick={onHome} style={{
          width: '100%', marginTop: 10, border: 0, cursor: 'pointer', background: 'transparent',
          color: LK.accent, font: `500 14px/1 ${LK.font}`, padding: 8,
        }}>Ir para o início</button>
      </div>
    </div>
  );
}

// ── Modo gamer ───────────────────────────────────────────────────────
function LogoTile({ letra, icon, cor, size = 44 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: 12, background: '#FFFFFF', border: `1px solid ${LK.border}`,
      display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 'none',
    }}>
      {icon
        ? <Icon name={icon} size={22} color={cor} />
        : <span style={{ font: `700 ${letra.length > 2 ? 11 : 14}px/1 ${LK.font}`, color: '#fff', background: cor, width: '76%', height: '76%', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{letra}</span>}
    </div>
  );
}

function ModoGamerJogo({ onSelect, onBack }) {
  const [busca, setBusca] = React.useState('');
  const filtrados = GAMES.filter(g => g.nome.toLowerCase().includes(busca.toLowerCase()));
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Modo gamer" onBack={onBack} step="1/3" />
      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: LK.bgSecondary, borderRadius: LK.rField, padding: '10px 14px' }}>
          <Icon name="search" size={18} color={LK.textTertiary} />
          <input value={busca} onChange={e => setBusca(e.target.value)} placeholder="Buscar jogo"
            style={{ border: 0, outline: 0, background: 'transparent', flex: 1, font: `400 14px/1 ${LK.font}`, color: LK.textPrimary }} />
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px' }}>
        <Overline style={{ marginBottom: 10 }}>Escolha o jogo</Overline>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {filtrados.map(g => (
            <button key={g.id} onClick={() => onSelect(g, null)} style={{
              display: 'flex', alignItems: 'center', gap: 10, textAlign: 'left', cursor: 'pointer',
              border: `1px solid ${LK.border}`, borderRadius: LK.rCard, padding: 10, background: LK.bgPrimary,
            }}>
              <LogoTile letra={g.letra} cor={g.cor} />
              <div style={{ minWidth: 0 }}>
                <div style={{ font: `600 13px/1.25 ${LK.font}`, color: LK.textPrimary }}>{g.nome}</div>
                <div style={{ font: `400 11px/1.25 ${LK.font}`, color: LK.textSecondary }}>{g.categoria}</div>
              </div>
            </button>
          ))}
        </div>
        <Overline style={{ margin: '18px 0 10px' }}>Não achou o seu jogo?</Overline>
        <button onClick={() => onSelect(null, CATEGORIAS_GENERICAS[0])} style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: 12, textAlign: 'left', cursor: 'pointer',
          border: `1px dashed ${LK.border}`, borderRadius: LK.rCard, padding: 14, background: 'transparent',
        }}>
          <div style={{ width: 44, height: 44, borderRadius: 12, background: LK.bgSecondary, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Icon name="sports_esports" size={20} color={LK.textSecondary} />
          </div>
          <div>
            <div style={{ font: `600 13px/1.25 ${LK.font}`, color: LK.textPrimary }}>Outro jogo</div>
            <div style={{ font: `400 11px/1.25 ${LK.font}`, color: LK.textSecondary }}>Escolha a categoria mais parecida</div>
          </div>
        </button>
      </div>
    </div>
  );
}

function ModoGamerDevice({ jogo, onSelect, onBack }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Modo gamer" onBack={onBack} step="2/3" />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px' }}>
        <Overline style={{ marginBottom: 10 }}>Em qual aparelho você joga {jogo ? jogo.nome : 'esse jogo'}?</Overline>
        {DEVICES.map(d => (
          <button key={d.id} onClick={() => onSelect(d)} style={{
            width: '100%', display: 'flex', alignItems: 'center', gap: 12, textAlign: 'left', cursor: 'pointer',
            border: `1px solid ${LK.border}`, borderRadius: LK.rCard, padding: 12, marginBottom: 8, background: LK.bgPrimary,
          }}>
            <LogoTile letra={d.letra} icon={d.icon} cor={d.cor} size={40} />
            <span style={{ flex: 1, font: `600 14px/1 ${LK.font}`, color: LK.textPrimary }}>{d.nome}</span>
            <Icon name="chevron_right" size={20} color={LK.textTertiary} />
          </button>
        ))}
      </div>
    </div>
  );
}

function ModoGamerConfig({ jogo, categoriaGenerica, device, onConfirm, onBack }) {
  const [salvar, setSalvar] = React.useState(true);
  const nomeJogo = jogo ? jogo.nome : `Outro jogo (${categoriaGenerica.nome})`;
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Modo gamer" onBack={onBack} step="3/3" />
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px 20px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: LK.bgSecondary, borderRadius: LK.rCard, padding: 14, marginBottom: 22 }}>
          <LogoTile letra={jogo ? jogo.letra : 'JG'} icon={jogo ? null : 'sports_esports'} cor={jogo ? jogo.cor : LK.textSecondary} />
          <div>
            <div style={{ font: `600 14px/1.25 ${LK.font}`, color: LK.textPrimary }}>{nomeJogo}</div>
            <div style={{ font: `400 12px/1.25 ${LK.font}`, color: LK.textSecondary }}>{device.nome}</div>
          </div>
        </div>
        <Overline style={{ marginBottom: 10 }}>Como quer usar essa combinação?</Overline>
        <button onClick={() => setSalvar(true)} style={{
          width: '100%', textAlign: 'left', display: 'flex', gap: 10, alignItems: 'flex-start', cursor: 'pointer',
          border: `1.5px solid ${salvar ? LK.accent : LK.border}`, background: salvar ? hexA(LK.accent, .08) : LK.bgPrimary,
          borderRadius: LK.rField, padding: 14, marginBottom: 8,
        }}>
          <Icon name={salvar ? 'radio_button_checked' : 'radio_button_unchecked'} size={20} color={salvar ? LK.accent : LK.border} />
          <div>
            <div style={{ font: `600 14px/1 ${LK.font}`, color: LK.textPrimary }}>Salvar como padrão</div>
            <div style={{ font: `400 12px/1.3 ${LK.font}`, color: LK.textSecondary, marginTop: 3 }}>Próxima vez, o Modo gamer já abre direto com {nomeJogo} + {device.nome}</div>
          </div>
        </button>
        <button onClick={() => setSalvar(false)} style={{
          width: '100%', textAlign: 'left', display: 'flex', gap: 10, alignItems: 'flex-start', cursor: 'pointer',
          border: `1.5px solid ${!salvar ? LK.accent : LK.border}`, background: !salvar ? hexA(LK.accent, .08) : LK.bgPrimary,
          borderRadius: LK.rField, padding: 14,
        }}>
          <Icon name={!salvar ? 'radio_button_checked' : 'radio_button_unchecked'} size={20} color={!salvar ? LK.accent : LK.border} />
          <div>
            <div style={{ font: `600 14px/1 ${LK.font}`, color: LK.textPrimary }}>Usar só desta vez</div>
            <div style={{ font: `400 12px/1.3 ${LK.font}`, color: LK.textSecondary, marginTop: 3 }}>Não muda o padrão do Modo gamer</div>
          </div>
        </button>
      </div>
      <div style={{ padding: '8px 20px 22px' }}>
        <button onClick={() => onConfirm(salvar)} style={{
          width: '100%', border: 0, cursor: 'pointer', background: LK.accent, color: '#fff',
          font: `600 15px/1 ${LK.font}`, borderRadius: LK.rBtn, padding: 15,
        }}>Ver diagnóstico</button>
      </div>
    </div>
  );
}

function ModoGamerResultado({ jogo, categoriaGenerica, device, salvarPadrao, onTrocar, onHome }) {
  const fallback = !jogo;
  const nomeJogo = jogo ? jogo.nome : 'Jogo fora do catálogo';
  const r = {
    tone: 'warning',
    titulo: fallback ? `Latência dentro do esperado para ${categoriaGenerica.nome.toLowerCase()}` : `Latência um pouco alta para ${nomeJogo}`,
    motor: fallback
      ? `Sem perfil específico deste jogo — usando o perfil de referência de ${categoriaGenerica.nome.toLowerCase()} para avaliar o resultado.`
      : 'Latência sob carga acima do ideal para partidas competitivas neste tipo de jogo.',
    evidencias: [
      { icon: 'hourglass_bottom', label: 'Latência sob carga', valor: '96 ms', tone: 'warning' },
      { icon: 'graphic_eq', label: 'Jitter', valor: '19 ms', tone: 'success' },
      { icon: 'trending_down', label: 'Perda estimada', valor: '0,4%', tone: 'success' },
      { icon: device.icon || 'sports_esports', label: 'Conexão do teste', valor: device.nome, tone: 'info' },
    ],
    ia: fallback
      ? `Como ${nomeJogo} ainda não está no catálogo do SignallQ, comparamos o resultado com o perfil típico de ${categoriaGenerica.nome.toLowerCase()} — os números medidos são reais, só a referência de comparação é genérica.`
      : `Pra ${nomeJogo} em ${device.nome}, uma latência sob carga nessa faixa costuma ser perceptível em momentos de disputa mais acirrada, mesmo sem chegar a travar a partida.`,
  };
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Diagnóstico do jogo" onBack={onTrocar} />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
          <LogoTile letra={jogo ? jogo.letra : 'JG'} icon={jogo ? null : 'sports_esports'} cor={jogo ? jogo.cor : LK.textSecondary} />
          <div>
            <div style={{ font: `600 15px/1.25 ${LK.font}`, color: LK.textPrimary }}>{nomeJogo}</div>
            <div style={{ font: `400 12px/1.25 ${LK.font}`, color: LK.textSecondary }}>{device.nome}</div>
          </div>
          {fallback && <Badge color={LK.textSecondary} bg={LK.bgSecondary} style={{ marginLeft: 'auto' }}>Categoria genérica</Badge>}
        </div>
        {fallback && (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', background: hexA(LK.warning, .12), borderRadius: LK.rField, padding: 12, marginBottom: 14 }}>
            <Icon name="info" size={16} color={LK.warning} />
            <span style={{ font: `400 12px/1.35 ${LK.font}`, color: LK.textPrimary }}>Este jogo não está no catálogo — usando o perfil de referência de {categoriaGenerica.nome.toLowerCase()}.</span>
          </div>
        )}
        <StatusBanner tone={r.tone} titulo={r.titulo} mensagem={r.motor} />
        <div style={{ marginTop: 18 }}>
          <AiVsMotorExplainer resultado={r} />
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 18, padding: '10px 14px', background: LK.bgSecondary, borderRadius: LK.rField }}>
          <Icon name={salvarPadrao ? 'bookmark' : 'bookmark_border'} size={16} color={LK.textSecondary} />
          <span style={{ font: `400 12px/1.3 ${LK.font}`, color: LK.textSecondary }}>
            {salvarPadrao ? `Salvo como padrão do Modo gamer (${nomeJogo} + ${device.nome})` : 'Usado só desta vez — o padrão do Modo gamer não mudou'}
          </span>
        </div>
        <button onClick={onTrocar} style={{
          width: '100%', marginTop: 18, border: `1px solid ${LK.border}`, cursor: 'pointer', background: 'transparent',
          color: LK.textPrimary, font: `500 14px/1 ${LK.font}`, borderRadius: LK.rBtn, padding: 14,
        }}>Trocar jogo ou aparelho</button>
        <button onClick={onHome} style={{
          width: '100%', marginTop: 10, border: 0, cursor: 'pointer', background: 'transparent',
          color: LK.accent, font: `500 14px/1 ${LK.font}`, padding: 8,
        }}>Ir para o início</button>
      </div>
    </div>
  );
}

// ── Ver detalhes técnicos (destino próprio — antes era accordion dentro da sheet) ────
function DetalheRow({ label, valor }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 0' }}>
      <span style={{ font: `400 13px/1 ${LK.font}`, color: LK.textTertiary }}>{label}</span>
      <span style={{ font: `500 13px/1 ${LK.font}`, color: LK.textPrimary }}>{valor}</span>
    </div>
  );
}

function DetalhesTecnicosScreen({ onBack }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: LK.bgPrimary }}>
      <SubTopBar title="Detalhes técnicos" onBack={onBack} />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px 24px' }}>
        <Overline style={{ marginBottom: 8 }}>Orientação por tipo de rede</Overline>
        <div style={{ font: `400 13px/1.5 ${LK.font}`, color: LK.textSecondary, marginBottom: 20 }}>
          Conexão via Wi-Fi. Se o resultado ficou abaixo do esperado, teste perto do roteador ou com um cabo de rede pra isolar se o problema é do Wi-Fi ou da internet contratada.
        </div>
        <Overline style={{ marginBottom: 4 }}>Métricas medidas</Overline>
        <div>
          <DetalheRow label="Bufferbloat" valor="8 ms" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="Pico Download" valor="512.4 Mbps" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="Pico Upload" valor="228.9 Mbps" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="Latência c/ carga ↓" valor="18 ms" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="Latência c/ carga ↑" valor="21 ms" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="Estabilidade" valor="96%" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="DNS (Cloudflare)" valor="9 ms" />
          <div style={{ height: 1, background: LK.border }} />
          <DetalheRow label="Servidor" valor="São Paulo, SP" />
        </div>
        <Overline style={{ margin: '20px 0 4px' }}>Equipamento local</Overline>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: LK.bgSecondary, borderRadius: LK.rCard, padding: 14 }}>
          <Icon name="router" size={20} color={LK.textTertiary} />
          <span style={{ font: `400 13px/1.3 ${LK.font}`, color: LK.textSecondary }}>Nenhum equipamento identificado nesta rede</span>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  OBJETIVOS, DiagnosticoGuiadoObjetivos, DiagnosticoGuiadoPerguntas, DiagnosticoGuiadoResultado,
  ModoGamerJogo, ModoGamerDevice, ModoGamerConfig, ModoGamerResultado,
  DetalhesTecnicosScreen, SubTopBar, AiVsMotorExplainer,
});
