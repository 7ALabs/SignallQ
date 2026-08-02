// Catálogo único de metadados por rota — consumido tanto pelo cliente
// (useDocumentMeta, upsert pós-hidratação) quanto por `functions/_middleware.ts`
// (injeção no HTML inicial via HTMLRewriter, issue #1369 Fase 2). Uma só fonte
// de verdade: mudar o title/description de uma rota aqui já reflete nos dois.
import type { PageMeta } from './seo'

export const PAGE_META: Record<string, PageMeta> = {
  '/': {
    title: 'Teste de velocidade real — SignallQ',
    description:
      'Meça agora a velocidade real da sua internet: download, upload e latência, com veredito claro para navegação, streaming, videochamadas e jogos.',
    path: '/',
  },
  '/historico': {
    title: 'Histórico de medições — SignallQ',
    description: 'Veja o histórico local das suas medições de velocidade. Armazenado somente neste navegador.',
    path: '/historico',
    robots: 'noindex,follow',
  },
  '/como-medimos': {
    title: 'Como medimos sua conexão — SignallQ',
    description: 'Entenda como o teste web do SignallQ mede velocidade, latência e estabilidade, além dos limites naturais do navegador.',
    path: '/como-medimos',
  },
  '/quem-somos': {
    title: 'Quem somos — SignallQ',
    description: 'Conheça o SignallQ: diagnóstico de conectividade que explica, não só mede.',
    path: '/quem-somos',
  },
  '/privacidade': {
    title: 'Política de Privacidade — SignallQ',
    description: 'Como o site do SignallQ processa e armazena dados durante o teste de velocidade e o histórico local.',
    path: '/privacidade',
  },
  '/termos': {
    title: 'Termos de Uso — SignallQ',
    description: 'Termos de uso do site público do SignallQ: teste de velocidade, histórico local e conteúdo institucional.',
    path: '/termos',
  },
  '/internet-boa-mas-travando': {
    title: 'Internet boa mas travando? Entenda o bufferbloat — SignallQ',
    description:
      'Sinal Wi-Fi forte e velocidade boa no teste, mas a internet trava quando mais de uma coisa usa a rede? Entenda o bufferbloat, a causa mais comum, e o que fazer.',
    path: '/internet-boa-mas-travando',
  },
  '/lag-em-jogos-online': {
    title: 'Lag em jogos online com boa internet? Pode ser CGNAT — SignallQ',
    description:
      'Internet parece boa mas dá lag em jogos, e você não consegue hospedar partida ou conectar direto com amigos? Entenda o CGNAT e o NAT Strict, e o que fazer.',
    path: '/lag-em-jogos-online',
  },
  '/comparativo': {
    title: 'SignallQ x testes de velocidade tradicionais',
    description: 'Um número de Mbps não diz por que a internet trava. Veja o que muda quando o teste também mede latência sob carga, DNS e o motivo provável do problema.',
    path: '/comparativo',
  },
  '/internet-para-jogos': {
    title: 'Que internet você precisa para jogar online sem travar',
    description: 'Não é só velocidade: latência baixa e estável importa mais que Mbps para a maioria dos jogos competitivos.',
    path: '/internet-para-jogos',
  },
  '/app': {
    title: 'App SignallQ',
    description: 'O app que não para no número: descobre por que sua internet está ruim. Em teste fechado.',
    path: '/app',
  },
  '/brand': {
    title: 'Identidade visual do SignallQ',
    description: 'Logotipo, variações de cor e paleta oficial para uso consistente.',
    path: '/brand',
  },
  // As 4 rotas abaixo são placeholder ("Em breve") pros diagnósticos
  // dedicados linkados pelos chips da Home (`DIAG_ITEMS`, `src/app/page.tsx`)
  // — ainda não implementados, `noindex` até terem conteúdo real.
  '/velocidade-e-latencia': {
    title: 'Velocidade e latência — em breve — SignallQ',
    description: 'Diagnóstico dedicado de velocidade e latência, em construção.',
    path: '/velocidade-e-latencia',
    robots: 'noindex,follow',
  },
  '/latencia-sob-carga': {
    title: 'Latência sob carga — em breve — SignallQ',
    description: 'Diagnóstico dedicado de latência sob carga (bufferbloat), em construção.',
    path: '/latencia-sob-carga',
    robots: 'noindex,follow',
  },
  '/servidores-dns': {
    title: 'Servidores DNS — em breve — SignallQ',
    description: 'Diagnóstico dedicado de servidores DNS, em construção.',
    path: '/servidores-dns',
    robots: 'noindex,follow',
  },
  '/modo-gamer': {
    title: 'Modo gamer — em breve — SignallQ',
    description: 'Diagnóstico dedicado de modo gamer por jogo, em construção.',
    path: '/modo-gamer',
    robots: 'noindex,follow',
  },
}

export const NOT_FOUND_META: PageMeta = {
  title: 'Página não encontrada — SignallQ',
  description: 'A página que você acessou não existe ou foi movida.',
  path: '/404',
  robots: 'noindex,follow',
}
