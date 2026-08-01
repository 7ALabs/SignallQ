"use client";
import Link from 'next/link'
import { DocPage, type DocSection } from '../../components/DocPage'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

// Conteúdo de SEO long-tail (issue #1399, consultoria de marketing registrada em #1374).
// Ancorado na frase sintomática que o usuário busca de fato ("internet boa mas travando")
// em vez do termo técnico "bufferbloat" -- sem evidência de que o consumidor comum
// pesquise por esse termo. Cada seção segue "resposta primeiro": a(s) primeira(s)
// frase(s) já respondem a pergunta do título, o resto elabora o mecanismo. Copy
// preservado 1:1 na reconstrução v2 (README, "só a moldura visual muda") — só a
// composição (SiteNav/SiteFooter, via DocPage/PageShell) mudou.
const SECTIONS: DocSection[] = [
  {
    title: 'Por que a internet trava mesmo com Wi-Fi forte e velocidade boa',
    text: 'Sinal forte e velocidade alta no teste medem coisas diferentes de travamento sob uso real. Quando mais de uma coisa usa a internet ao mesmo tempo — um download rodando enquanto você está em chamada de vídeo, por exemplo — o roteador ou o modem podem acumular dados numa fila (buffer) maior do que o necessário antes de enviar. Essa fila cheia atrasa tudo o que vem depois dela, mesmo que a velocidade contratada não tenha caído nem um pouco. O nome técnico para esse atraso é bufferbloat.',
  },
  {
    title: 'O que é bufferbloat',
    text: 'Bufferbloat é o atraso (latência) que aparece quando os buffers de um roteador, modem ou da rede da operadora enchem além do necessário sob carga. Buffers existem para absorver picos de tráfego sem perder dados — mas quando são grandes demais ou mal configurados, os pacotes ficam esperando na fila em vez de serem organizados por prioridade, e a latência sob carga pode saltar de poucos milissegundos para várias centenas. É por isso que a chamada de vídeo engasga e o jogo trava justo quando outra pessoa em casa começa a assistir stream ou baixar um arquivo grande.',
  },
  {
    title: 'Como saber se o problema é esse',
    text: 'Um indício forte é a internet funcionar bem quando só uma coisa está sendo usada, mas travar, engasgar ou dar lag assim que mais de uma atividade disputa a rede ao mesmo tempo. Isso não confirma o diagnóstico por si só: sinal Wi-Fi fraco, interferência e problemas na operadora também podem causar lentidão. Mas, quando o atraso aparece sob uso simultâneo e melhora ao terminar o download, a fila excessiva merece investigação.',
  },
  {
    title: 'O que fazer',
    text: 'Se o seu roteador oferecer gerenciamento de fila (SQM, Smart Queue Management, ou "priorização de tráfego"), essa é uma opção a testar: ela ajuda a controlar a fila para reduzir o atraso em chamadas e jogos. Antes de trocar de plano, compare a latência sob download e upload no teste de velocidade do SignallQ; uma diferença grande em relação à latência sem carga é um sinal útil para investigar. O resultado não substitui uma análise do roteador ou da operadora, mas evita olhar apenas para o número de download isolado.',
  },
]

export default function Page() {
  useDocumentMeta(PAGE_META['/internet-boa-mas-travando'])

  return (
    <PageShell align="center" mobilePadding="pt-7 px-5 pb-10">
      <DocPage
        overline="Diagnóstico"
        title="Internet boa mas travando? Veja por que isso acontece"
        intro="Se o Wi-Fi está com sinal forte e o teste de velocidade mostra número alto, mas a internet ainda trava ou engasga quando mais de uma coisa usa a rede ao mesmo tempo, o motivo normalmente não é velocidade — é latência sob carga, um efeito chamado bufferbloat."
        sections={SECTIONS}
        ctaLabel="Testar minha conexão"
        ctaTo="/"
      >
        <p className="body-medium m-0">
          Se o travamento aparece especificamente em jogos online — lag, dificuldade de jogar com amigos ou de hospedar partida — a causa
          pode ser outra: veja <Link href="/lag-em-jogos-online">lag em jogos online e o CGNAT</Link>.
        </p>
      </DocPage>
    </PageShell>
  )
}



