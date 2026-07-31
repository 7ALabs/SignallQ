import { SiteNav } from '../../components/SiteNav'
import { SiteFooter } from '../../components/SiteFooter'
import { EmbeddedSpeedTest } from '../../components/speedtest/EmbeddedSpeedTest'
import { trackScreenView } from '../../lib/telemetry'
import { useEffect } from 'react'

export default function VideochamadaPage() {
  useEffect(() => {
    trackScreenView('diagnostico_videochamada')
  }, [])

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="home" />
      
      <main className="mx-auto w-full max-w-4xl px-4 py-8">
        <header className="mb-8 text-center">
          <h1 className="title-large text-white mb-4">A videochamada trava? Saiba por que a internet engasga</h1>
          <p className="body-medium text-slate-300 max-w-2xl mx-auto">
            Sua reunião do Zoom ou Teams fica caindo, com voz cortada e vídeo travado? Isso costuma ser problema de upload ou oscilação, não de download. Descubra com o teste abaixo.
          </p>
        </header>

        <section className="mb-12">
          <EmbeddedSpeedTest />
        </section>

        <div className="grid gap-6 md:grid-cols-2">
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">O que causa voz robótica e travamento?</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Upload insuficiente:</strong> Você consegue ouvir os outros (download), mas seu vídeo falha para eles (upload).</li>
              <li><strong>Perda de pacotes:</strong> Pedaços de informação se perdem no caminho, gerando cortes e "vozes robóticas".</li>
              <li><strong>Interferência Wi-Fi:</strong> Muito longe do roteador, o sinal enfraquece e os dados não chegam estáveis.</li>
            </ul>
          </article>
          
          <article className="rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
            <h2 className="title-medium text-white mb-3">Como resolver na hora da reunião</h2>
            <ul className="body-medium text-slate-300 list-disc pl-5 space-y-2">
              <li><strong>Desligue a câmera:</strong> Se for crítico continuar a chamada, poupe seu Upload apenas enviando voz.</li>
              <li><strong>Chegue perto do roteador:</strong> Mova seu notebook para o mesmo cômodo do roteador.</li>
              <li><strong>Pausar sincronizações:</strong> Verifique se o Google Drive, OneDrive ou iCloud não estão subindo arquivos gigantes no momento.</li>
            </ul>
          </article>
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
