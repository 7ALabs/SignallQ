import { SiteNav } from '../../components/SiteNav'
import { SiteFooter } from '../../components/SiteFooter'
import { EmbeddedSpeedTest } from '../../components/speedtest/EmbeddedSpeedTest'
import { trackScreenView } from '../../lib/telemetry'
import { useEffect } from 'react'

export default function WifiLentoPage() {
  useEffect(() => {
    trackScreenView('diagnostico_wifi_lento')
  }, [])

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="home" />
      
      <main className="mx-auto w-full max-w-4xl px-4 py-8">
        <header className="mb-8 text-center">
          <h1 className="title-large text-white mb-4">O Wi-Fi não pega? Como resolver sinal fraco e lentidão</h1>
          <p className="body-medium text-slate-300 max-w-2xl mx-auto">
            Ter 1 Giga de fibra não adianta nada se o roteador não alcançar o seu quarto. Faça o teste abaixo para avaliar a capacidade real do seu Wi-Fi.
          </p>
        </header>

        <section className="mb-12">
          <EmbeddedSpeedTest />
        </section>

        <div className="grid gap-6 md:grid-cols-2">
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">Problemas comuns de Wi-Fi</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Paredes e espelhos:</strong> O sinal 5GHz é muito rápido, mas não atravessa obstáculos físicos com facilidade.</li>
              <li><strong>Interferência:</strong> Micro-ondas, telefones sem fio e roteadores de vizinhos atrapalham sua conexão (canal congestionado).</li>
              <li><strong>Equipamento antigo:</strong> Roteadores antigos simplesmente não conseguem distribuir altas velocidades.</li>
            </ul>
          </article>
          
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">Soluções para Wi-Fi</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Faça dois testes:</strong> Teste agora e repita colado no roteador. A diferença te mostra o que você perde pelo Wi-Fi.</li>
              <li><strong>Posicionamento:</strong> Coloque o roteador em um local alto e central da casa, não escondido atrás de móveis.</li>
              <li><strong>Use redes Mesh:</strong> Se a casa for grande, repetidores comuns pioram o ping. Sistemas Mesh são a melhor opção moderna.</li>
            </ul>
          </article>
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
