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
  '/pro': {
    title: 'SignallQ PRO — venda seu diagnóstico de Wi-Fi como serviço',
    // Achado bloqueante da Lia: description prometia trial ("Experimente grátis por
    // 14 dias") contradizendo "Em breve" da seção Planos/modal. Corrigido para vitrine honesta.
    description:
      'Organize clientes, registre medições por ambiente e entregue um laudo profissional com a sua marca. Em breve — entre na lista de espera.',
    path: '/pro',
  },
  '/historico': {
    title: 'Histórico de medições — SignallQ',
    description: 'Veja o histórico local das suas medições de velocidade. Armazenado somente neste navegador.',
    path: '/historico',
    robots: 'noindex,follow',
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
}

export const NOT_FOUND_META: PageMeta = {
  title: 'Página não encontrada — SignallQ',
  description: 'A página que você acessou não existe ou foi movida.',
  path: '/404',
  robots: 'noindex,follow',
}
