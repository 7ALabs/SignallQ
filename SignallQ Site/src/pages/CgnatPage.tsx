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
      'Se o teste de velocidade mostra números bons mas o jogo trava, o personagem "teleporta" ou você não consegue hospedar partida, a velocidade não é o único fator a verificar. Um possível motivo é o tipo de conexão que a operadora entrega: muitas compartilham um único endereço IP público entre várias casas, numa técnica chamada CGNAT (Carrier-Grade NAT). Isso pode dificultar conexões diretas usadas por alguns jogos, mesmo quando navegação, streaming e download parecem normais.',
  },
  {
    titulo: 'O que é CGNAT',
    texto:
      'CGNAT é quando a operadora coloca vários clientes atrás do mesmo endereço IP público, em vez de dar um IP exclusivo para cada conexão — prática comum porque endereços IPv4 públicos são escassos e caros. Do ponto de vista da operadora, isso economiza IP; do ponto de vista do jogo, sua conexão passa a ficar atrás de uma camada extra de tradução de endereço que a maioria dos jogos online não foi desenhada para atravessar com facilidade.',
  },
  {
    titulo: 'Por que não consigo hospedar partida ou conectar direto com amigos',
    texto:
      'Isso pode acontecer quando a conexão fica com NAT restrito (também chamado de NAT Strict ou NAT Tipo 3, dependendo da plataforma). Nesse cenário, outro jogador pode não conseguir se conectar diretamente a você; hospedar uma sala e encontrar partidas também pode falhar. CGNAT é uma causa possível, mas as configurações do roteador e as regras do próprio jogo também influenciam o tipo de NAT.',
  },
  {
    titulo: 'Como saber se é isso',
    texto:
      'A maioria dos consoles (Xbox, PlayStation) e alguns jogos mostram o tipo de NAT no menu de configuração de rede — procure a nomenclatura usada pela sua plataforma, como Aberto, Moderado ou Strict. Se aparecer Strict ou Moderado junto com dificuldade recorrente de jogar com amigos específicos ou de hospedar partida, vale verificar com a operadora se a conexão usa CGNAT. O teste de velocidade do SignallQ complementa essa checagem, mas não identifica CGNAT sozinho.',
  },
  {
    titulo: 'O que fazer',
    texto:
      'Antes de alterar o roteador, confirme o tipo de NAT e consulte a documentação do jogo ou do console. Redirecionamento de portas pode ajudar quando o bloqueio está no roteador local, mas geralmente não resolve se a conexão estiver sob CGNAT, porque a tradução também acontece na operadora. Nesse caso, pergunte se há opção de sair do CGNAT ou obter um IPv4 público; IPv6 também pode ajudar quando o jogo, o console e a rede o suportam.',
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
