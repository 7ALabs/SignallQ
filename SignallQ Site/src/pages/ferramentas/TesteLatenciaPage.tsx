import { SiteNav } from '../../components/SiteNav'
import { SiteFooter } from '../../components/SiteFooter'
import { EmbeddedSpeedTest } from '../../components/speedtest/EmbeddedSpeedTest'
import { trackScreenView } from '../../lib/telemetry'
import { useEffect } from 'react'

export default function TesteLatenciaPage() {
  useEffect(() => {
    trackScreenView('ferramentas_teste_latencia')
  }, [])

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="home" />
      
      <main className="mx-auto w-full max-w-4xl px-4 py-8">
        <header className="mb-8 text-center">
          <h1 className="title-large text-white mb-4">Teste de Latência: descubra o real tempo de resposta</h1>
          <p className="body-medium text-slate-300 max-w-2xl mx-auto">
            A latência (também conhecida como ping) é o tempo que um pacote de dados leva para ir do seu dispositivo até o servidor e voltar. Faça o teste para ver se a sua rede é responsiva.
          </p>
        </header>

        <section className="mb-12">
          <EmbeddedSpeedTest />
        </section>

        <div className="grid gap-6 md:grid-cols-2">
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">O que significa uma latência alta?</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Acima de 100ms:</strong> Jogos online ficam difíceis de jogar, tiros atrasam e os jogadores teletransportam.</li>
              <li><strong>Oscilações frequentes (Jitter):</strong> O ping fica variando, o que causa cortes no áudio de chamadas e reuniões.</li>
              <li><strong>Bufferbloat:</strong> A latência dispara assim que alguém começa um download na mesma rede.</li>
            </ul>
          </article>
          
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">Por que latência importa mais que velocidade?</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Reatividade:</strong> Clicar em um link e a página abrir imediatamente depende da latência, não de ter 1 Giga de download.</li>
              <li><strong>Para a maioria das pessoas:</strong> 100 Mbps com 10ms de latência entrega uma experiência muito superior a 500 Mbps com 150ms.</li>
              <li><strong>Como melhorar:</strong> Use cabo de rede e verifique se há tráfego de fundo ocupando o roteador.</li>
            </ul>
          </article>
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
