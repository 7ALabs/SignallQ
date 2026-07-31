import { SiteNav } from '../../components/SiteNav'
import { SiteFooter } from '../../components/SiteFooter'
import { EmbeddedSpeedTest } from '../../components/speedtest/EmbeddedSpeedTest'
import { trackScreenView } from '../../lib/telemetry'
import { useEffect } from 'react'

export default function PingAltoPage() {
  useEffect(() => {
    trackScreenView('diagnostico_ping_alto')
  }, [])

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="home" />
      
      <main className="mx-auto w-full max-w-4xl px-4 py-8">
        <header className="mb-8 text-center">
          <h1 className="title-large text-white mb-4">Ping alto em jogos: como reduzir a latência</h1>
          <p className="body-medium text-slate-300 max-w-2xl mx-auto">
            Não importa se o seu download é de 500 Mega: se o ping for alto, você terá lag nos jogos online. Faça o teste abaixo focando na métrica de Latência.
          </p>
        </header>

        <section className="mb-12">
          <EmbeddedSpeedTest />
        </section>

        <div className="grid gap-6 md:grid-cols-2">
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">O que causa ping alto?</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Jogar no Wi-Fi:</strong> O sinal Wi-Fi sofre interferências o tempo todo. Isso causa "saltos" no tempo de resposta.</li>
              <li><strong>Rota da operadora:</strong> As vezes o sinal dá a volta ao mundo antes de chegar no servidor do jogo.</li>
              <li><strong>Rede ocupada (Bufferbloat):</strong> Alguém assistindo Netflix ou baixando arquivos enquanto você joga faz o ping disparar.</li>
            </ul>
          </article>
          
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">Como resolver</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Cabo de rede é lei:</strong> Sempre jogue usando um cabo ethernet conectado direto ao roteador.</li>
              <li><strong>Atenção a downloads de fundo:</strong> Pause atualizações da Steam/PlayStation/Xbox.</li>
              <li><strong>Fale com a provedora:</strong> Se o problema persistir no cabo, mostre o resultado do teste (que indica latência elevada) ao suporte.</li>
            </ul>
          </article>
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
