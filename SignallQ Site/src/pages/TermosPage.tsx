import { LegalSectionList, type LegalSection } from '../components/LegalSectionList'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'

// Adaptado de docs_ai/legal/TERMS_OF_USE.md (Termos do app Android) para o
// contexto do site público — gap real fechado nesta entrega: o protótipo
// Claude Design não tinha Termos.dc.html apesar de a issue #1153 exigir /termos.
const SECTIONS: LegalSection[] = [
  {
    title: '1. Aceitação dos termos',
    body: 'Ao usar o site do SignallQ (teste de velocidade e páginas institucionais), você concorda com estes Termos de Uso. Se não concordar, não utilize o site.',
  },
  {
    title: '2. Descrição do serviço',
    body: 'O site do SignallQ oferece um teste de velocidade real (download, upload e latência), histórico local de medições e conteúdo institucional sobre o SignallQ e o SignallQ PRO.',
  },
  {
    title: '3. Uso permitido',
    body: 'Você pode usar o site para medir e entender sua própria conexão de internet, e para compartilhar resultados com terceiros (por exemplo, sua operadora). Você não pode usar o site para atacar, sobrecarregar ou interferir na infraestrutura de medição, nem para fins ilegais.',
  },
  {
    title: '4. Gratuidade',
    body: 'O teste de velocidade e o histórico local são gratuitos e não exigem cadastro. O site pode exibir anúncios (Google AdSense) quando configurado, sempre após o resultado do teste.',
  },
  {
    title: '5. Disponibilidade',
    body: 'O serviço é fornecido "como está" (as is). Não garantimos disponibilidade ininterrupta do site nem precisão absoluta das medições — o teste depende de infraestrutura de terceiros (Cloudflare) que pode sofrer indisponibilidade fora do nosso controle.',
  },
  {
    title: '6. Privacidade',
    body: 'O tratamento dos seus dados é regido pela nossa Política de Privacidade do site, disponível em /privacidade.',
  },
  {
    title: '7. Propriedade intelectual',
    body: 'O SignallQ, incluindo seu código, design, marca e conteúdo, é propriedade da 7Agents Tecnologia. Todos os direitos reservados.',
  },
  {
    title: '8. Limitação de responsabilidade',
    body: 'A 7Agents não se responsabiliza por danos diretos ou indiretos decorrentes do uso do site, decisões tomadas com base nos resultados exibidos, ou indisponibilidade temporária do serviço.',
  },
  {
    title: '9. Alterações nos termos',
    body: 'A 7Agents pode atualizar estes Termos a qualquer momento. O uso continuado do site após alterações implica aceitação dos novos termos.',
  },
  {
    title: '10. Legislação aplicável',
    body: 'Estes Termos são regidos pelas leis da República Federativa do Brasil, em conformidade com a Lei Geral de Proteção de Dados (LGPD — Lei 13.709/2018) e o Marco Civil da Internet (Lei 12.965/2014).',
  },
  {
    title: '11. Contato',
    body: 'Para dúvidas sobre estes Termos: suporte@signallq.com (7Agents Tecnologia).',
  },
]

export default function TermosPage() {
  useDocumentMeta({
    title: 'Termos de Uso — SignallQ',
    description: 'Termos de uso do site público do SignallQ: teste de velocidade, histórico local e conteúdo institucional.',
    path: '/termos',
  })

  return (
    <PageLayout active="termos">
      <div className="mx-auto flex w-full max-w-[680px] flex-col gap-7 px-5 pb-20 pt-12 box-border">
        <div>
          <div className="overline">Termos de Uso</div>
          <div className="headline-large mt-2">Termos de Uso do site SignallQ</div>
          <div className="body-small mt-2.5">Última atualização: 18 de julho de 2026</div>
        </div>

        <LegalSectionList sections={SECTIONS} />
      </div>
    </PageLayout>
  )
}
