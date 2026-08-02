"use client";
import { useEffect } from 'react'
import Link from 'next/link'
import { useDocumentMeta } from '@/hooks/useDocumentMeta'
import { PAGE_META } from '@/lib/pageMetaCatalog'
import { SIGNALLQ_TEST_GROUP_URL } from '@/lib/config'
import { FEATURE_DOWNLOAD_APP_CLICADO, trackFeatureUsed } from '@/lib/telemetry'

const FEATURES = [
  { icon: 'wifi', title: 'Wi-Fi cômodo a cômodo', text: 'Mede o sinal em cada ambiente da casa e mostra onde ele morre.' },
  { icon: 'signal_cellular_alt', title: 'Sinal 4G/5G real', text: 'RSRP, RSRQ e SINR direto do rádio do aparelho, não estimado.' },
  { icon: 'psychology', title: 'Diagnóstico por IA', text: 'Descreve o problema com suas palavras e recebe uma causa provável, não um número solto.' },
  { icon: 'sports_esports', title: 'Modo gamer por jogo', text: 'Escolhe o jogo e o aparelho e recebe um veredito específico para aquela partida.' },
  { icon: 'devices', title: 'Dispositivos na rede', text: 'Vê quem está conectado ao Wi-Fi, sem entrar no roteador.' },
];
const FEATURES_LOOP = [...FEATURES, ...FEATURES];

const STEPS = [
  { n: '1', title: 'Entre no grupo de testadores', text: 'Acesse o grupo oficial usando a mesma conta Google utilizada no seu celular Android.' },
  { n: '2', title: 'Acesse o teste fechado', text: 'Depois de entrar no grupo, abra a página de teste do SignallQ na Play Store.' },
  { n: '3', title: 'Instale e use o aplicativo', text: 'Teste as funções normalmente, principalmente velocidade, diagnóstico, Wi-Fi, fibra e rede móvel.' },
  { n: '4', title: 'Compartilhe sua experiência', text: 'Informe erros, dificuldades ou sugestões diretamente no grupo de testadores.' },
];

const COMPARE_ROWS = [
  { label: 'Sinal Wi-Fi por cômodo' },
  { label: 'Sinal 4G/5G nativo' },
  { label: 'Diagnóstico por IA da causa' },
  { label: 'Modo gamer por jogo' },
  { label: 'Dispositivos conectados à rede' },
];

export default function AppLandingPage() {
  useDocumentMeta(PAGE_META['/app'])

  useEffect(() => {
    let n = 0;
    const items: HTMLElement[] = [];

    function scan() {
      document.querySelectorAll('.sq-app-reveal').forEach((el) => {
        const htmlEl = el as HTMLElement;
        if (htmlEl.dataset.sqRev) return;
        htmlEl.dataset.sqRev = '1';
        htmlEl.style.transitionDelay = ((n++ % 4) * 90) + 'ms';
        items.push(htmlEl);
      });
      check();
    }

    function check() {
      const h = window.innerHeight || 800;
      for (let i = items.length - 1; i >= 0; i--) {
        const el = items[i];
        const r = el.getBoundingClientRect();
        if (r.top < h * 0.92 && r.bottom > -h * 0.4) {
          el.classList.add('sq-in');
          items.splice(i, 1);
        }
      }
    }

    scan();
    const observer = new MutationObserver(scan);
    observer.observe(document.documentElement, { childList: true, subtree: true });

    ['scroll', 'wheel', 'resize'].forEach(ev => {
      window.addEventListener(ev, check, { passive: true, capture: true });
    });
    const interval = setInterval(check, 250);

    return () => {
      observer.disconnect();
      ['scroll', 'wheel', 'resize'].forEach(ev => {
        window.removeEventListener(ev, check, { capture: true });
      });
      clearInterval(interval);
    }
  }, [])

  // Teste fechado: entrar no grupo é a etapa correta antes da instalação pela
  // Play Store — mesmo padrão de PlayStoreBadge/SiteFooter, não coleta e-mail.
  const entrarNaListaDeTeste = () => {
    trackFeatureUsed(FEATURE_DOWNLOAD_APP_CLICADO)
    window.open(SIGNALLQ_TEST_GROUP_URL, '_blank', 'noopener,noreferrer')
  }

  return (
    <div
      className="relative flex w-full flex-col"
    >
      <div className="relative w-full box-border flex justify-center overflow-hidden p-[28px_20px_24px] sm:p-[56px_var(--safe-x)_40px]">
        <div className="sq-app-glow-a absolute left-[8%] top-[-80px] w-[420px] h-[420px] rounded-full pointer-events-none" style={{ background: 'radial-gradient(circle, color-mix(in srgb, var(--accent) 30%, transparent), transparent 70%)' }} />
        <div className="sq-app-glow-b absolute right-[6%] top-[120px] w-[380px] h-[380px] rounded-full pointer-events-none" style={{ background: 'radial-gradient(circle, color-mix(in srgb, var(--accent-blue) 26%, transparent), transparent 70%)' }} />

        <div className="relative w-full max-w-[1080px] flex items-center gap-[48px] flex-wrap">
          <div className="flex-[1_1_420px] flex flex-col items-center sm:items-start gap-4 text-center sm:text-left">
            <span className="flex items-center gap-[6px] rounded-full py-[6px] px-[14px] bg-[color-mix(in_srgb,_var(--accent)_16%,_transparent)]">
              <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">science</span>
              <span className="font-semibold text-[12px] leading-[1.3] text-[color:var(--accent)] tracking-[.02em]">Em teste fechado · vagas limitadas</span>
            </span>
            <h1 className="m-0 max-w-[560px] font-bold text-[30px] sm:text-[42px] leading-[1.12] text-[color:var(--text-primary)] text-pretty">
              O app que não para no número: descobre por que sua internet está ruim
            </h1>
            <p className="m-0 max-w-[480px] font-normal text-[16px] leading-[1.5] text-[color:var(--text-secondary)] text-pretty">
              Wi-Fi cômodo a cômodo, sinal 4G/5G real e diagnóstico por IA: o que nenhum teste de velocidade tradicional consegue ver.
            </p>
            <div className="flex flex-wrap justify-center sm:justify-start gap-3 pt-[6px]">
              <button
                type="button"
                onClick={entrarNaListaDeTeste}
                className="h-[48px] flex items-center gap-2 px-6 rounded-full border-none bg-[color:var(--accent)] shadow-[0_14px_30px_color-mix(in_srgb,_var(--accent)_40%,_transparent)] cursor-pointer hover:brightness-110 transition-all"
              >
                <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">how_to_reg</span>
                <span className="font-semibold text-[15px] leading-[1.2] text-[color:var(--on-accent)]">Entrar na lista de teste</span>
              </button>
              <Link href="/" className="h-[48px] flex items-center px-[22px] rounded-full border border-[color:var(--border)] no-underline hover:bg-[color:var(--bg-secondary)] transition-colors cursor-pointer">
                <span className="font-medium text-[15px] leading-[1.2] text-[color:var(--text-primary)]">Testar no navegador agora</span>
              </Link>
            </div>
            <div className="flex items-center gap-2">
              <span className="sq-app-dot w-2 h-2 rounded-full bg-[color:var(--success)]" />
              <span className="font-normal text-[12px] leading-[1.3] text-[color:var(--text-tertiary)]">
                Android · sem custo para entrar na lista · sem cartão de crédito
              </span>
            </div>
          </div>

          <div className="sq-app-float flex-none rounded-[30px] p-[6px] box-border bg-[#16181d] shadow-[0_18px_40px_-14px_rgba(0,0,0,.5)] mx-auto sm:mx-0">
            <img
              src="/assets/teste-01-home-dark.png"
              alt="Tela Início do SignallQ em modo escuro, mostrando o caminho da conexão entre aparelho, roteador e provedor, o resultado da última medição de velocidade e o status do Wi-Fi."
              className="h-[280px] sm:h-[380px] w-auto block rounded-[22px]"
            />
          </div>
        </div>

        <div className="sq-app-bounce absolute left-1/2 bottom-2 -translate-x-1/2 hidden sm:block">
          <span className="material-symbols-outlined text-[24px] text-[color:var(--text-tertiary)]">expand_more</span>
        </div>
      </div>

      <div className="w-full box-border flex justify-center pb-4 px-[var(--safe-x)]">
        <div className="w-full max-w-[1080px] flex flex-col gap-[56px]">

          <div className="sq-app-reveal flex flex-col gap-4">
            <div className="text-center font-medium text-[11px] leading-[1.45] text-[color:var(--accent)] tracking-[.3px] uppercase">
              Diferenciais
            </div>
            <div className="sq-app-scroller w-full overflow-hidden">
              <div className="sq-app-track">
                {FEATURES_LOOP.map((f, i) => (
                  <div key={i} className="flex-none w-[240px] flex flex-col gap-2 rounded-[20px] p-5 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.16)]">
                    <span className="material-symbols-outlined text-[22px] text-[color:var(--accent)]">{f.icon}</span>
                    <div className="font-semibold text-[15px] leading-[1.3] text-[color:var(--text-primary)]">{f.title}</div>
                    <div className="font-normal text-[13px] leading-[1.45] text-[color:var(--text-secondary)]">{f.text}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="sq-app-reveal flex flex-col gap-5">
            <h2 className="m-0 text-center font-bold text-[24px] leading-[1.3] text-[color:var(--text-primary)]">
              Como funciona a inscrição
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {STEPS.map((s, i) => (
                <div key={i} className="flex flex-col gap-2 rounded-[20px] p-[22px] box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.16)]">
                  <div className="w-[32px] h-[32px] rounded-full flex items-center justify-center bg-[color:var(--accent)] font-bold text-[14px] leading-none text-[color:var(--on-accent)]">
                    {s.n}
                  </div>
                  <div className="font-semibold text-[15px] leading-[1.3] text-[color:var(--text-primary)]">{s.title}</div>
                  <div className="font-normal text-[13px] leading-[1.45] text-[color:var(--text-secondary)]">{s.text}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="sq-app-reveal flex flex-col gap-[14px]">
            <h2 className="m-0 text-center font-bold text-[24px] leading-[1.3] text-[color:var(--text-primary)]">
              O que muda em relação a um teste de velocidade comum
            </h2>
            <div className="rounded-[20px] overflow-hidden shadow-[0_12px_30px_rgba(0,0,0,.16)]">
              <div className="grid grid-cols-[1.4fr_1fr_1fr]">
                <div className="p-[14px_18px] bg-[color:var(--bg-secondary)] font-medium text-[13px] leading-[1.3] text-[color:var(--text-tertiary)]" />
                <div className="p-[14px_18px] bg-[color:var(--bg-secondary)] text-center font-medium text-[13px] leading-[1.3] text-[color:var(--text-secondary)]">Testes tradicionais</div>
                <div className="p-[14px_18px] bg-[color:var(--accent)] text-center font-bold text-[13px] leading-[1.3] text-[color:var(--on-accent)]">App SignallQ</div>
              </div>
              {COMPARE_ROWS.map((r, i) => (
                <div key={i} className="grid grid-cols-[1.4fr_1fr_1fr] bg-[color:var(--bg-secondary)]">
                  <div className="p-[12px_18px] border-t border-[color-mix(in_srgb,_var(--border)_18%,_transparent)] font-normal text-[13px] leading-[1.4] text-[color:var(--text-primary)]">
                    {r.label}
                  </div>
                  <div className="p-[12px_18px] border-t border-[color-mix(in_srgb,_var(--border)_18%,_transparent)] flex items-center justify-center">
                    <span className="material-symbols-outlined text-[18px] text-[color:var(--text-tertiary)]">close</span>
                  </div>
                  <div className="p-[12px_18px] border-t border-[color-mix(in_srgb,_var(--border)_18%,_transparent)] flex items-center justify-center">
                    <span className="material-symbols-outlined text-[18px] text-[color:var(--success)]">check_circle</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="sq-app-reveal relative flex flex-wrap items-center justify-between gap-5 rounded-[24px] p-[32px] box-border overflow-hidden bg-[linear-gradient(135deg,_color-mix(in_srgb,_var(--accent)_20%,_var(--bg-secondary)),_var(--bg-secondary))] shadow-[0_16px_40px_rgba(0,0,0,.2)]">
            <div className="sq-app-glow-a absolute right-[-60px] top-[-60px] w-[220px] h-[220px] rounded-full pointer-events-none" style={{ background: 'radial-gradient(circle, color-mix(in srgb, var(--accent) 35%, transparent), transparent 70%)' }} />
            <div className="relative flex-[1_1_320px] flex flex-col gap-1">
              <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
                Vagas do teste fechado são limitadas
              </div>
              <div className="font-bold text-[20px] leading-[1.3] text-[color:var(--text-primary)]">
                Entre agora e ajude a moldar o app antes do lançamento
              </div>
            </div>
            <button
              type="button"
              onClick={entrarNaListaDeTeste}
              className="relative h-[48px] flex items-center gap-2 px-6 rounded-full border-none bg-[color:var(--accent)] shadow-[0_14px_30px_color-mix(in_srgb,_var(--accent)_40%,_transparent)] shrink-0 cursor-pointer hover:brightness-110 transition-all"
            >
              <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">how_to_reg</span>
              <span className="font-semibold text-[15px] leading-[1.2] text-[color:var(--on-accent)] whitespace-nowrap">Entrar na lista de teste</span>
            </button>
          </div>

        </div>
      </div>
    </div>
  )
}
