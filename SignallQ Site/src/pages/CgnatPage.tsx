import { Link } from 'react-router-dom'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { PAGE_META } from '../lib/pageMetaCatalog'

// Conteúdo de SEO long-tail (issue #1399, consultoria de marketing registrada em #1374).
// Tema fora da lista original de 11 termos, sem grande portal dominando ainda --
// encaixa direto no posicionamento do SignallQ de explicar a causa, não só medir.
// Cada seção segue "resposta primeiro": a(s) primeira(s) frase(s) já respondem a
// pergunta do título, o resto elabora o mecanismo.
const SECOES = [
  {
    titulo: 'Por que dá lag em jogos mesmo com a internet parecendo boa',
    texto:
      'Se o teste de velocidade mostra números bons mas o jogo trava, o personagem "teleporta" ou você é expulso da partida com frequência, a causa mais comum não é velocidade — é o tipo de conexão que a operadora entrega. Muitas operadoras compartilham um único endereço IP público entre várias casas ao mesmo tempo, numa técnica chamada CGNAT (Carrier-Grade NAT). Isso praticamente não afeta navegação, streaming ou download, mas prejudica justamente o tipo de conexão direta (peer-to-peer) que jogos online usam entre jogadores.',
  },
  {
    titulo: 'O que é CGNAT',
    texto:
      'CGNAT é quando a operadora coloca vários clientes atrás do mesmo endereço IP público, em vez de dar um IP exclusivo para cada conexão — prática comum porque endereços IPv4 públicos são escassos e caros. Do ponto de vista da operadora, isso economiza IP; do ponto de vista do jogo, sua conexão passa a ficar atrás de uma camada extra de tradução de endereço que a maioria dos jogos online não foi desenhada para atravessar com facilidade.',
  },
  {
    titulo: 'Por que não consigo hospedar partida ou conectar direto com amigos',
    texto:
      'Isso acontece porque o CGNAT normalmente resulta em NAT Strict (também chamado de NAT Tipo 3) no seu console ou PC, o pior tipo de NAT para jogos. Com NAT Strict, seu dispositivo só aceita conexão de quem ele mesmo iniciou contato antes — então outro jogador não consegue se conectar diretamente a você, hospedar uma sala que você criou pode falhar, e o matchmaking do jogo tende a demorar mais ou falhar ao tentar juntar vocês na mesma partida.',
  },
  {
    titulo: 'Como saber se é isso',
    texto:
      'A maioria dos consoles (Xbox, PlayStation) e alguns jogos mostram o tipo de NAT direto no menu de configuração de rede — procure por "NAT Tipo 2 / Aberto" (bom), "NAT Tipo 3 / Strict" (problema) ou nomenclatura equivalente. Se aparecer Strict ou Moderado junto com dificuldade recorrente de jogar com amigos específicos ou de hospedar partida, CGNAT é a explicação mais provável, mesmo que o teste de velocidade do SignallQ mostre resultado excelente.',
  },
  {
    titulo: 'O que fazer',
    texto:
      'Redirecionamento de porta (port forwarding) no roteador, a solução clássica para NAT Strict, não funciona sob CGNAT — porque o roteador de quem joga não é o único responsável pelo endereço público, a operadora é. A saída real é pedir à operadora um IP público dedicado (às vezes chamado de "IP fixo" ou "IP real"), quando o plano oferecer essa opção; em alguns casos, habilitar IPv6 no roteador também contorna o problema, porque conexões IPv6 costumam não passar por CGNAT.',
  },
]

export default function CgnatPage() {
  useDocumentMeta(PAGE_META['/lag-em-jogos-online'])

  return (
    <PageLayout active="cgnat">
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-8 px-5 pb-20 pt-12 box-border">
        <header className="flex flex-col gap-3">
          <div className="overline">Diagnóstico</div>
          <h1 className="headline-large m-0">Lag em jogos online mesmo com boa internet? Pode ser CGNAT</h1>
          <p className="body-large m-0">
            Se a internet parece boa em qualquer outro uso, mas trava, dá lag ou impede de hospedar partida e jogar com amigos, o motivo mais
            comum é CGNAT — uma prática da operadora que resulta em NAT Strict no seu console ou PC.
          </p>
        </header>

        <div className="flex flex-col gap-4">
          {SECOES.map((secao) => (
            <section key={secao.titulo} className="rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
              <h2 className="title-large m-0">{secao.titulo}</h2>
              <p className="body-medium mb-0 mt-2">{secao.texto}</p>
            </section>
          ))}
        </div>

        <p className="body-medium m-0">
          Se a internet trava mesmo fora de jogos — por exemplo, engasga em chamadas de vídeo quando outra pessoa está baixando algo — a
          causa pode ser diferente: veja <Link to="/internet-boa-mas-travando">internet boa mas travando e o bufferbloat</Link>.
        </p>

        <Link
          to="/"
          className="flex h-10 w-fit items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
          style={{ background: 'var(--accent)', color: '#fff' }}
        >
          <span className="label-large" style={{ color: '#fff' }}>
            Testar minha conexão
          </span>
        </Link>
      </main>
    </PageLayout>
  )
}
