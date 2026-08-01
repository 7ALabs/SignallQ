"use client";
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

const SECTIONS: DocSection[] = [
  { title: 'Só Mbps x veredito completo', text: 'Testes tradicionais mostram download e upload soltos. O SignallQ classifica cada métrica (Excelente/Bom/Regular/Fraco) e explica o que ela significa no uso real: streaming, videochamada, jogos.' },
  { title: 'Latência sob carga', text: 'A maioria dos testes só mede latência em repouso. O SignallQ também mede sob download e upload simultâneos, o que revela bufferbloat, a causa mais comum de travamento com sinal forte.' },
  { title: 'DNS entra na conta', text: 'Um DNS lento atrasa toda navegação mesmo com banda de sobra. O SignallQ compara servidores DNS reais e aponta o mais rápido para a sua rede.' },
  { title: 'Diagnóstico, não só número', text: 'Em vez de parar no resultado, o SignallQ aponta a causa mais provável (Wi-Fi, DNS, instabilidade da rede) e o que fazer a seguir.' },
  { title: 'Diagnóstico completo é no app', text: 'O navegador não lê rádio Wi-Fi, sinal 4G/5G nem o modem da operadora. O app Android SignallQ mede tudo isso, cômodo a cômodo.' },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/comparativo'])

  return (
    <PageShell align="center" mobilePadding="pt-7 px-5 pb-10">
      <DocPage
        overline="Comparativo"
        title="SignallQ x testes de velocidade tradicionais"
        intro="Um número de Mbps não diz por que a internet trava. Veja o que muda quando o teste também mede latência sob carga, DNS e o motivo provável do problema."
        sections={SECTIONS}
        ctaLabel="Fazer meu teste"
        ctaTo="/"
      />
    </PageShell>
  )
}
