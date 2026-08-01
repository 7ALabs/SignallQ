"use client";
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

// Copy verbatim de `ScreenDoc.dc.html` (`PAGES['como-medimos']`), reconstrução v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/`).
const SECTIONS: DocSection[] = [
  {
    title: 'O que medimos',
    text: 'O teste envia e recebe dados por HTTPS para medir download e upload. Também faz várias solicitações pequenas para observar a latência e a variação entre as amostras, chamada de jitter.',
  },
  {
    title: 'Velocidade, latência e estabilidade',
    text: 'Velocidade indica quanto dado passou durante o teste. Latência indica o tempo de resposta. Estabilidade resume o quanto as medições variaram. Uma conexão pode ter boa velocidade e ainda responder lentamente.',
  },
  {
    title: 'Como interpretamos',
    text: 'Comparamos as métricas observadas com faixas de uso cotidiano e mostramos uma leitura em linguagem simples. Essa interpretação descreve aquela execução; não confirma a velocidade do seu plano.',
  },
  {
    title: 'Por que o resultado pode variar',
    text: 'Wi-Fi, distância do roteador, interferência, aparelhos usando a rede, congestionamento, navegador e a rota até o servidor podem alterar o resultado. Para comparar, repita o teste nas mesmas condições.',
  },
  {
    title: 'Limites do teste no navegador',
    text: 'O navegador não mede sinal Wi-Fi, não envia ping ICMP e não confirma seu provedor ou localização. A medição web não é um laudo regulatório nem uma certificação formal.',
  },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/como-medimos'])

  return (
    <PageShell align="center" mobilePadding="pt-7 px-5 pb-10">
      <DocPage
        overline="Metodologia"
        title="Como medimos sua conexão"
        intro="O que foi medido, como interpretamos e o que um teste no navegador não consegue afirmar."
        sections={SECTIONS}
        ctaLabel="Iniciar teste"
        ctaTo="/"
      />
    </PageShell>
  )
}


