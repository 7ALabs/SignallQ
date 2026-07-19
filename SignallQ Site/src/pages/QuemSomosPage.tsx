import { Link } from 'react-router-dom'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'

export default function QuemSomosPage() {
  useDocumentMeta({
    title: 'Quem somos — SignallQ',
    description: 'Conheça o SignallQ: diagnóstico de conectividade que explica, não só mede.',
    path: '/quem-somos',
  })

  return (
    <PageLayout active="sobre">
      <div className="mx-auto flex w-full max-w-[680px] flex-col gap-8 px-5 pb-20 pt-12 box-border">
        <div>
          <div className="overline">Quem somos</div>
          <div className="headline-large mt-2">Conectividade explicada, não só medida</div>
        </div>

        <div className="flex flex-col gap-5">
          <div className="body-large">
            O SignallQ é uma ferramenta de diagnóstico de conectividade. Em vez de só mostrar números de velocidade, ele existe para ajudar a
            entender o que está por trás de uma conexão lenta ou instável — e o que fazer a respeito.
          </div>

          <div className="body-large">
            A maior parte dos testes de velocidade entrega um número e para por aí. O SignallQ parte de um princípio diferente: mostrar a
            métrica é só o começo — o valor está em explicar o que ela significa na prática, em português claro e sem jargão técnico.
          </div>

          <div className="flex flex-col gap-2">
            <div className="title-medium">O SignallQ gratuito</div>
            <div className="body-medium">
              O aplicativo SignallQ, atualmente em fase Beta, é gratuito e feito para qualquer pessoa medir e entender sua própria conexão —
              Wi-Fi, fibra, DNS ou sinal móvel.
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <div className="title-medium">O SignallQ PRO</div>
            <div className="body-medium">
              Para quem faz diagnóstico de redes profissionalmente, o SignallQ PRO transforma essa mesma medição em um serviço documentado —
              com cadastro de clientes, registro por ambiente e laudo em PDF. Veja mais na página do{' '}
              <Link to="/pro">SignallQ PRO</Link>.
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <div className="title-medium">Onde queremos chegar</div>
            <div className="body-medium">
              A visão do SignallQ é tornar diagnósticos de rede mais compreensíveis e acionáveis — para quem só quer saber por que a internet
              está lenta, e para quem faz disso um serviço.
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  )
}
