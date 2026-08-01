"use client";
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

// Copy verbatim de `ScreenDoc.dc.html` (`PAGES['quem-somos']`), reconstrução v2.
const SECTIONS: DocSection[] = [
  {
    title: 'O SignallQ gratuito',
    text: 'O aplicativo SignallQ, em fase Beta, é gratuito e feito para qualquer pessoa medir e entender sua própria conexão — Wi-Fi, fibra, DNS ou sinal móvel.',
  },
  {
    title: 'O SignallQ PRO',
    text: 'Para quem faz diagnóstico de redes profissionalmente, o PRO transforma a medição em serviço documentado: cadastro de clientes, registro por ambiente e laudo em PDF.',
  },
  {
    title: 'Onde queremos chegar',
    text: 'Tornar diagnósticos de rede mais compreensíveis e acionáveis — para quem só quer saber por que a internet está lenta, e para quem faz disso um serviço.',
  },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/quem-somos'])

  return (
    <PageShell >
      <DocPage
        overline="Quem somos"
        title="Conectividade explicada, não só medida"
        intro="Mostrar a métrica é só o começo: o valor está em explicar o que ela significa na prática, em português claro."
        sections={SECTIONS}
      />
    </PageShell>
  )
}


