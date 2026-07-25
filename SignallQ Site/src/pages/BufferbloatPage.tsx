import { Link } from 'react-router-dom'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { PAGE_META } from '../lib/pageMetaCatalog'

// Conteúdo de SEO long-tail (issue #1399, consultoria de marketing registrada em #1374).
// Ancorado na frase sintomática que o usuário busca de fato ("internet boa mas travando")
// em vez do termo técnico "bufferbloat" -- sem evidência de que o consumidor comum
// pesquise por esse termo. Cada seção segue "resposta primeiro": a(s) primeira(s)
// frase(s) já respondem a pergunta do título, o resto elabora o mecanismo.
const SECOES = [
  {
    titulo: 'Por que a internet trava mesmo com Wi-Fi forte e velocidade boa',
    texto:
      'Sinal forte e velocidade alta no teste medem coisas diferentes de travamento sob uso real. Quando mais de uma coisa usa a internet ao mesmo tempo — um download rodando enquanto você está em chamada de vídeo, por exemplo — o roteador ou o modem podem acumular dados numa fila (buffer) maior do que o necessário antes de enviar. Essa fila cheia atrasa tudo o que vem depois dela, mesmo que a velocidade contratada não tenha caído nem um pouco. O nome técnico para esse atraso é bufferbloat.',
  },
  {
    titulo: 'O que é bufferbloat',
    texto:
      'Bufferbloat é o atraso (latência) que aparece quando os buffers de um roteador, modem ou da rede da operadora enchem além do necessário sob carga. Buffers existem para absorver picos de tráfego sem perder dados — mas quando são grandes demais ou mal configurados, os pacotes ficam esperando na fila em vez de serem organizados por prioridade, e a latência sob carga pode saltar de poucos milissegundos para várias centenas. É por isso que a chamada de vídeo engasga e o jogo trava justo quando outra pessoa em casa começa a assistir stream ou baixar um arquivo grande.',
  },
  {
    titulo: 'Como saber se o problema é esse',
    texto:
      'O sinal mais claro é: a internet funciona bem quando só uma coisa está sendo usada, mas trava, engasga ou dá lag assim que mais de uma coisa disputa a rede ao mesmo tempo. Diferente de sinal Wi-Fi fraco (que causa lentidão constante, mesmo sozinho) ou de queda de conexão (que interrompe totalmente), o bufferbloat aparece como atraso intermitente, ligado ao volume de uso simultâneo — e melhora sozinho quando a outra atividade termina.',
  },
  {
    titulo: 'O que fazer',
    texto:
      'A correção mais eficaz é ativar o gerenciamento de fila (conhecido como SQM, Smart Queue Management, ou "priorização de tráfego") no roteador, quando o modelo suportar — ele organiza a fila para priorizar tráfego sensível a atraso, como chamadas e jogos, sem descartar o resto. Trocar de plano para uma velocidade maior raramente resolve, porque o problema não é quantidade de dado, é como a fila é gerenciada sob carga. Rodar o teste de velocidade do SignallQ ajuda a confirmar o padrão: é a latência e a oscilação subindo quando a rede está sob uso simultâneo — não o número de download isolado — que aponta para bufferbloat.',
  },
]

export default function BufferbloatPage() {
  useDocumentMeta(PAGE_META['/internet-boa-mas-travando'])

  return (
    <PageLayout active="bufferbloat">
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-8 px-5 pb-20 pt-12 box-border">
        <header className="flex flex-col gap-3">
          <div className="overline">Diagnóstico</div>
          <h1 className="headline-large m-0">Internet boa mas travando? Veja por que isso acontece</h1>
          <p className="body-large m-0">
            Se o Wi-Fi está com sinal forte e o teste de velocidade mostra número alto, mas a internet ainda trava ou engasga quando mais de
            uma coisa usa a rede ao mesmo tempo, o motivo normalmente não é velocidade — é latência sob carga, um efeito chamado bufferbloat.
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
          Se o travamento aparece especificamente em jogos online — lag, dificuldade de jogar com amigos ou de hospedar partida — a causa
          pode ser outra: veja{' '}
          <Link to="/lag-em-jogos-online">lag em jogos online e o CGNAT</Link>.
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
