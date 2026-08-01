"use client";
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

// Copy verbatim de `ScreenDoc.dc.html` (`PAGES['quem-somos']`), reconstrução v4
// — a seção 2 mudou de "O SignallQ PRO" para "Diagnóstico, não só medição" no
// protótipo atual (`/pro` foi removido do site na fase de Fundação).
const SECTIONS: DocSection[] = [
  {
    title: 'O SignallQ gratuito',
    text: 'O aplicativo SignallQ, em fase Beta, é gratuito e feito para qualquer pessoa medir e entender sua própria conexão: Wi-Fi, fibra, DNS ou sinal móvel.',
  },
  {
    title: 'Diagnóstico, não só medição',
    text: 'Além de medir velocidade, o app lê sinal Wi-Fi cômodo a cômodo, rede móvel, modem de fibra e aponta a causa provável do problema, não só um número.',
  },
  {
    title: 'Onde queremos chegar',
    text: 'Tornar diagnósticos de rede mais compreensíveis e acionáveis para qualquer pessoa, sem precisar entender de redes.',
  },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/quem-somos'])

  return (
    <PageShell align="center" mobilePadding="pt-7 px-5 pb-10">
      <DocPage
        overline="Quem somos"
        title="Conectividade explicada, não só medida"
        intro="Mostrar a métrica é só o começo: o valor está em explicar o que ela significa na prática, em português claro."
        sections={SECTIONS}
      />
    </PageShell>
  )
}


