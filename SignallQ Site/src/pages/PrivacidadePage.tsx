import { LegalSectionList, type LegalSection } from '../components/LegalSectionList'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'

const SECTIONS: LegalSection[] = [
  {
    title: 'O que é processado durante o teste',
    body: 'Para medir download, upload e latência, o navegador troca dados diretamente com o servidor de medição (rede da Cloudflare). Como em qualquer requisição de internet, esse servidor enxerga o endereço IP público do seu dispositivo durante a medição — isso é necessário tecnicamente para o teste funcionar e não é algo que o site do SignallQ colete ou armazene.',
  },
  {
    title: 'O que fica salvo no seu navegador',
    body: 'O resultado de cada teste (data/horário, download, upload, latência, oscilação, tipo de conexão) é salvo localmente no seu navegador, usando IndexedDB. Esses dados não são enviados a servidores do SignallQ.',
  },
  {
    title: 'Histórico local',
    body: 'O histórico existe só para você reconsultar medições anteriores neste mesmo navegador e aparelho. Ele não sincroniza entre aparelhos e não é acessível por nós.',
  },
  {
    title: 'Cookies',
    body: 'O site em si não define cookies próprios. Se Google AdSense estiver configurado, esse serviço pode definir seus próprios cookies, conforme as políticas da Google.',
  },
  {
    title: 'Telemetria de uso do produto',
    body: 'Registramos apenas eventos agregados de uso do produto (por exemplo: início, conclusão ou compartilhamento de um teste; clique no download do app; visualização da página PRO), sem conteúdo do seu histórico e sem dado que identifique você pessoalmente. Esses eventos são enviados de forma server-side para a mesma infraestrutura de analytics de produto do SignallQ (não usamos Google Analytics/GA4 neste site).',
  },
  {
    title: 'Cloudflare Web Analytics',
    body: 'Usamos o Cloudflare Web Analytics para métricas agregadas de tráfego e desempenho (páginas visitadas, tempo de carregamento). Esse serviço não usa cookies nem identifica visitantes individualmente.',
  },
  {
    title: 'Lista de espera por e-mail',
    body: 'Se você pedir para ser avisado quando o SignallQ ou o SignallQ PRO lançarem, coletamos o e-mail informado só para esse fim (avisar sobre o lançamento). Ele fica armazenado em nossa infraestrutura de dados, não é compartilhado com terceiros e não é usado para qualquer outra finalidade. Para pedir a remoção, escreva para giammattey.luiz@gmail.com.',
  },
  {
    title: 'Google AdSense',
    body: 'Este site pode exibir anúncios via Google AdSense, carregados somente depois que uma medição termina. A Google pode processar dados de acordo com sua própria política de privacidade e uso de cookies.',
  },
  {
    title: 'Fornecedores terceiros',
    body: 'Os únicos terceiros envolvidos no funcionamento do site são: Cloudflare (infraestrutura de medição de velocidade, analytics de tráfego e hospedagem) e, quando configurado, Google AdSense.',
  },
  {
    title: 'Como excluir seu histórico local',
    body: 'Na página Histórico, você pode excluir uma medição específica ou limpar todo o histórico de uma vez. Também é possível apagar esses dados limpando os dados de navegação/armazenamento do site nas configurações do seu navegador.',
  },
  {
    title: 'Como retirar consentimento',
    body: 'Você pode bloquear cookies e limpar dados de site nas configurações do seu navegador a qualquer momento. Isso remove o histórico local e qualquer identificador usado por ferramentas de anúncios neste site.',
  },
]

export default function PrivacidadePage() {
  useDocumentMeta({
    title: 'Política de Privacidade — SignallQ',
    description: 'Como o site do SignallQ processa e armazena dados durante o teste de velocidade e o histórico local.',
    path: '/privacidade',
  })

  return (
    <PageLayout active="privacidade">
      <div className="mx-auto flex w-full max-w-[680px] flex-col gap-7 px-5 pb-20 pt-12 box-border">
        <div>
          <div className="overline">Privacidade</div>
          <div className="headline-large mt-2">Como este site trata seus dados</div>
          <div className="body-small mt-2.5">Última atualização: 18 de julho de 2026</div>
        </div>

        <div className="body-medium">
          Esta política cobre o site público do SignallQ (teste de velocidade e páginas institucionais). Ela é diferente da política do
          aplicativo Android, porque o navegador processa e armazena dados de forma diferente de um app instalado.
        </div>

        <LegalSectionList sections={SECTIONS} />
      </div>
    </PageLayout>
  )
}
