"use client";
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

const SECTIONS: DocSection[] = [
  { title: 'Velocidade não é o gargalo', text: 'A maioria dos jogos online usa pouca banda: poucos Mbps já bastam. O que derruba a experiência é latência alta ou instável, não a velocidade contratada.' },
  { title: 'O que observar no resultado', text: 'Latência abaixo de 50 ms costuma ser boa para jogos casuais; competitivos pedem menos. Jitter alto (a variação da latência) é o que mais se sente como "engasgo".' },
  { title: 'Bufferbloat arruína partidas', text: 'Se a internet funciona bem sozinha mas trava quando alguém mais usa a rede, o motivo costuma ser latência sob carga; veja o guia de bufferbloat.' },
  { title: 'CGNAT e hospedar partida', text: 'Se você não consegue hospedar partida ou joga com NAT restrito, o motivo pode ser CGNAT da operadora; veja o guia de CGNAT.' },
  { title: 'Diagnóstico por jogo é no app', text: 'O app Android tem um Modo gamer dedicado: escolha o jogo e o aparelho e receba um veredito específico para aquela partida, não uma métrica genérica.' },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/internet-para-jogos'])

  return (
    <PageShell align="center" mobilePadding="pt-7 px-5 pb-10">
      <DocPage
        overline="Guia"
        title="Que internet você precisa para jogar online sem travar"
        intro="Não é só velocidade: latência baixa e estável importa mais que Mbps para a maioria dos jogos competitivos."
        sections={SECTIONS}
        ctaLabel="Testar minha conexão"
        ctaTo="/"
      />
    </PageShell>
  )
}
