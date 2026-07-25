import { Link } from 'react-router-dom'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { PAGE_META } from '../lib/pageMetaCatalog'

const SECOES = [
  {
    titulo: 'O que medimos',
    texto: 'O teste envia e recebe dados por HTTPS para medir download e upload. Também faz várias solicitações pequenas para observar a latência e a variação entre as amostras, chamada de jitter.',
  },
  {
    titulo: 'Velocidade, latência e estabilidade',
    texto: 'Velocidade indica quanto dado passou durante o teste. Latência indica o tempo de resposta. Estabilidade resume o quanto as medições de velocidade variaram. Uma conexão pode ter boa velocidade e ainda responder lentamente.',
  },
  {
    titulo: 'Como interpretamos',
    texto: 'O SignallQ compara as métricas observadas com faixas de uso cotidiano e mostra uma leitura em linguagem simples. Essa interpretação descreve aquela execução; não substitui os números nem confirma a velocidade do seu plano.',
  },
  {
    titulo: 'Por que o resultado pode variar',
    texto: 'Wi-Fi, distância do roteador, interferência, aparelhos usando a rede, congestionamento, navegador, capacidade do aparelho e a rota até o servidor podem alterar o resultado. Para comparar, repita o teste nas mesmas condições.',
  },
  {
    titulo: 'Limites do teste no navegador',
    texto: 'O navegador não mede sinal Wi-Fi, não envia ping ICMP e não confirma automaticamente seu provedor ou localização. Por isso não inventamos essas informações. A medição web não é um laudo regulatório nem uma certificação formal.',
  },
]

export default function ComoMedimosPage() {
  useDocumentMeta(PAGE_META['/como-medimos'])

  return (
    <PageLayout active="como-medimos">
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-8 px-5 pb-20 pt-12 box-border">
        <header className="flex flex-col gap-3">
          <div className="overline">Metodologia</div>
          <h1 className="headline-large m-0">Como medimos sua conexão</h1>
          <p className="body-large m-0">Mostramos o que foi medido, como interpretamos e o que um teste no navegador não consegue afirmar.</p>
        </header>

        <div className="flex flex-col gap-4">
          {SECOES.map((secao) => (
            <section key={secao.titulo} className="rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
              <h2 className="title-large m-0">{secao.titulo}</h2>
              <p className="body-medium mb-0 mt-2">{secao.texto}</p>
            </section>
          ))}
        </div>

        <Link to="/" className="flex h-10 w-fit items-center justify-center rounded-[var(--radius-button)] px-5 no-underline" style={{ background: 'var(--accent)', color: '#fff' }}>
          <span className="label-large" style={{ color: '#fff' }}>Iniciar teste</span>
        </Link>
      </main>
    </PageLayout>
  )
}
