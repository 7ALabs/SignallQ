"use client";
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

// Copy verbatim de `ScreenDoc.dc.html` (`PAGES['privacidade']`), reconstrução v4 —
// 10 seções. A seção "Lista de espera do PRO" existia numa versão anterior desta
// página mas não existe no protótipo atual (`/pro` foi removido do site na fase
// de Fundação) e foi removida daqui para bater 1:1 com a fonte.
const SECTIONS: DocSection[] = [
  {
    title: 'O que é processado durante o teste',
    text: 'Para medir download, upload e latência, o navegador troca dados com o servidor de medição (rede da Cloudflare), que enxerga o IP público do seu dispositivo durante a medição. Isso é necessário tecnicamente e não é coletado pelo site.',
  },
  {
    title: 'O que fica salvo no seu navegador',
    text: 'O resultado de cada teste (data, download, upload, latência, oscilação, tipo de conexão) é salvo localmente via IndexedDB. Esses dados não são enviados a servidores do SignallQ.',
  },
  {
    title: 'Histórico local',
    text: 'O histórico existe para você reconsultar medições neste mesmo navegador e aparelho. Não sincroniza entre aparelhos e não é acessível por nós.',
  },
  {
    title: 'Cookies',
    text: 'O site não define cookies próprios. Se o Google AdSense estiver configurado, esse serviço pode definir os seus, conforme as políticas da Google.',
  },
  {
    title: 'Telemetria de uso',
    text: 'Registramos apenas eventos agregados de uso do produto, sem conteúdo do histórico e sem dado que identifique você. São enviados server-side para a analytics de produto do SignallQ.',
  },
  {
    title: 'Cloudflare Web Analytics',
    text: 'Usamos o Cloudflare Web Analytics para métricas agregadas de tráfego e desempenho. Esse serviço não usa cookies nem identifica visitantes individualmente.',
  },
  {
    title: 'Google AdSense',
    text: 'O site pode exibir anúncios via AdSense, carregados somente depois que uma medição termina. A Google pode processar dados conforme sua própria política.',
  },
  {
    title: 'Fornecedores terceiros',
    text: 'Os únicos terceiros envolvidos são Cloudflare (medição, analytics de tráfego e hospedagem) e, quando configurado, Google AdSense.',
  },
  {
    title: 'Como excluir seu histórico',
    text: 'Na página Histórico você pode excluir uma medição ou limpar tudo. Também dá para apagar limpando os dados do site no navegador.',
  },
  {
    title: 'Como retirar consentimento',
    text: 'Bloqueie cookies e limpe dados do site nas configurações do navegador a qualquer momento. Isso remove o histórico local e identificadores de anúncios.',
  },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/privacidade'])

  return (
    <PageShell align="center" mobilePadding="pt-7 px-5 pb-10">
      <DocPage
        overline="Privacidade"
        title="Como este site trata seus dados"
        updated="Última atualização: 18 de julho de 2026"
        intro="Esta política cobre o site público do SignallQ. Ela é diferente da política do aplicativo Android."
        sections={SECTIONS}
      />
    </PageShell>
  )
}


