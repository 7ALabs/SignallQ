import { useState } from 'react'
import { trackFeatureUsed } from '../../lib/telemetry'

export function GuidedDiagnosis() {
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [diagnosis, setDiagnosis] = useState<{ title: string; action: string } | null>(null)

  const handleAnswer = (question: string, answer: string) => {
    const newAnswers = { ...answers, [question]: answer }
    setAnswers(newAnswers)
    
    // Calcula próximo step
    let nextStep = step + 1
    if (step === 0 && answer === 'cabo') {
      nextStep = 2 // Pula pergunta do wifi
    }

    if (nextStep >= 4) {
      calculateDiagnosis(newAnswers)
    } else {
      setStep(nextStep)
    }
  }

  const calculateDiagnosis = (finalAnswers: Record<string, string>) => {
    let title = ''
    let action = ''
    
    if (finalAnswers['cabo_ou_wifi'] === 'wifi' && finalAnswers['perto'] === 'nao') {
      title = 'O sinal do Wi-Fi pode estar fraco.'
      action = 'Aproxime-se do roteador e repita o teste para comparar.'
    } else if (finalAnswers['outras_pessoas'] === 'sim') {
      title = 'Sua rede pode estar congestionada.'
      action = 'Muitas pessoas usando a internet ao mesmo tempo podem causar lentidão e ping alto. Repita quando a rede estiver livre.'
    } else if (finalAnswers['outros_aparelhos'] === 'nao') {
      title = 'O problema parece estar no seu aparelho.'
      action = 'Como os outros aparelhos funcionam bem, tente reiniciar este dispositivo e desativar a economia de bateria.'
    } else {
      title = 'Pode haver uma instabilidade com sua operadora.'
      action = 'Se você está perto do roteador (ou no cabo) e só você usa a internet, o problema provavelmente vem de fora. Entre em contato com seu provedor e mostre o resultado do teste.'
    }

    setDiagnosis({ title, action })
    setStep(4)
    trackFeatureUsed('diagnosis_expanded')
  }

  const reset = () => {
    setStep(0)
    setAnswers({})
    setDiagnosis(null)
  }

  if (step === 4 && diagnosis) {
    return (
      <div className="sq-fade-up flex flex-col gap-3 rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
        <div className="label-overline" style={{ color: 'var(--accent)' }}>Diagnóstico SignallQ</div>
        <h3 className="title-medium m-0 text-white">{diagnosis.title}</h3>
        <p className="body-medium m-0" style={{ color: 'var(--text-secondary)' }}>{diagnosis.action}</p>
        <button onClick={reset} className="mt-2 w-fit rounded-full px-4 py-2 text-sm font-medium" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>
          Refazer diagnóstico
        </button>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3 rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
      <div className="label-overline" style={{ color: 'var(--accent)' }}>Diagnóstico Guiado</div>
      
      {step === 0 && (
        <div className="sq-fade-up">
          <p className="title-medium m-0 mb-3 text-white">Como você está conectado?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('cabo_ou_wifi', 'wifi')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Wi-Fi</button>
            <button onClick={() => handleAnswer('cabo_ou_wifi', 'cabo')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Cabo de rede</button>
          </div>
        </div>
      )}

      {step === 1 && (
        <div className="sq-fade-up">
          <p className="title-medium m-0 mb-3 text-white">Você está perto do roteador?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('perto', 'sim')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Sim</button>
            <button onClick={() => handleAnswer('perto', 'nao')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Não</button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="sq-fade-up">
          <p className="title-medium m-0 mb-3 text-white">Outras pessoas estão usando a conexão agora?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('outras_pessoas', 'sim')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Sim</button>
            <button onClick={() => handleAnswer('outras_pessoas', 'nao')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Não</button>
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="sq-fade-up">
          <p className="title-medium m-0 mb-3 text-white">A lentidão também acontece em outros aparelhos?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('outros_aparelhos', 'sim')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Sim</button>
            <button onClick={() => handleAnswer('outros_aparelhos', 'nao')} className="flex-1 rounded-xl p-3 hover:bg-white/5" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>Não, só neste</button>
          </div>
        </div>
      )}
    </div>
  )
}
