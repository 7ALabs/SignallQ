import { SiteNav } from '../../components/SiteNav'
import { SiteFooter } from '../../components/SiteFooter'
import { EmbeddedSpeedTest } from '../../components/speedtest/EmbeddedSpeedTest'
import { trackScreenView } from '../../lib/telemetry'
import { useEffect } from 'react'

export default function InternetLentaPage() {
  useEffect(() => {
    trackScreenView('diagnostico_internet_lenta')
  }, [])

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="home" />
      
      <main className="mx-auto w-full max-w-4xl px-4 py-8">
        <header className="mb-8 text-center">
          <h1 className="title-large text-white mb-4">Minha internet está lenta: o que fazer?</h1>
          <p className="body-medium text-slate-300 max-w-2xl mx-auto">
            Sua velocidade pode estar boa no papel, mas ruim na prática. Faça o teste abaixo para identificar exatamente onde está o problema (download, upload ou latência) e siga nosso diagnóstico guiado.
          </p>
        </header>

        <section className="mb-12">
          <EmbeddedSpeedTest />
        </section>

        <div className="grid gap-6 md:grid-cols-2">
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">Principais Causas</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Roteador longe:</strong> O sinal Wi-Fi perde força através de paredes.</li>
              <li><strong>Muitos dispositivos:</strong> Celulares, TVs e PCs dividindo a mesma banda.</li>
              <li><strong>Download vs Latência:</strong> A velocidade pode ser alta, mas o "tempo de resposta" (ping) está ruim, causando lentidão ao abrir sites.</li>
              <li><strong>Horário de pico:</strong> Congestionamento na infraestrutura do provedor.</li>
            </ul>
          </article>
          
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">O que você pode fazer</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Aproxime-se do roteador:</strong> Veja se a velocidade melhora. Se sim, o problema é cobertura.</li>
              <li><strong>Use cabo de rede:</strong> Para PCs e consoles, o cabo garante estabilidade que o Wi-Fi não entrega.</li>
              <li><strong>Reinicie os equipamentos:</strong> Tire o roteador da tomada por 30 segundos.</li>
              <li><strong>Diagnóstico avançado:</strong> Use o botão "Refazer diagnóstico" após o teste para um guia passo a passo.</li>
            </ul>
          </article>
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
