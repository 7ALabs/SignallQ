import { useState } from 'react'
import { FEATURE_DIAGNOSIS_EXPANDED, trackFeatureUsed } from '../../lib/telemetry'

// Bloco "Diagnóstico guiado" — reconstrução v4 (`ScreenHome.dc.html`, variant
// "result-diagnostico"). O protótipo só especifica a entrada estática (2
// opções Wi-Fi/Cabo de rede); a árvore de perguntas seguintes e o diagnóstico
// final são lógica de produto real já existente, mantida aqui em vez de
// reduzida a um placeholder — mesmo entry point visual, funcionalidade real
// por trás. Estilo alinhado ao restante da tela (blocos hairline, sem
// glass-panel/cor literal).
export function GuidedDiagnosis() {
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [diagnosis, setDiagnosis] = useState<{ title: string; action: string } | null>(null)

  const handleAnswer = (question: string, answer: string) => {
    const newAnswers = { ...answers, [question]: answer }
    setAnswers(newAnswers)

    let nextStep = step + 1
    if (step === 0 && answer === 'cabo') {
      nextStep = 2 // pula a pergunta de Wi-Fi
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
      title = 'Pode ser um problema de sinal do Wi-Fi.'
      action = 'Aproxime-se do roteador e repita o teste para comparar.'
    } else if (finalAnswers['outras_pessoas'] === 'sim') {
      title = 'Sua rede parece congestionada.'
      action = 'Muitas pessoas usando a internet ao mesmo tempo causam lentidão e ping alto. Repita o teste quando a rede estiver mais livre.'
    } else if (finalAnswers['outros_aparelhos'] === 'nao') {
      title = 'O problema parece estar restrito a este aparelho.'
      action = 'Como os outros aparelhos funcionam bem, tente reiniciar este dispositivo e desativar a economia de bateria.'
    } else {
      title = 'Há sinais de instabilidade na sua rede.'
      action = 'Como você está no cabo ou perto do roteador e sem sobrecarga na rede, o problema provavelmente vem de fora. Entre em contato com seu provedor.'
    }

    setDiagnosis({ title, action })
    setStep(4)
    trackFeatureUsed(FEATURE_DIAGNOSIS_EXPANDED)
  }

  const reset = () => {
    setStep(0)
    setAnswers({})
    setDiagnosis(null)
  }

  const opcaoButtonClass =
    'flex-1 text-center rounded-xl p-3 border border-[color:var(--border)] font-normal text-[14px] leading-[1.43] text-[color:var(--text-primary)] hover:bg-[color:var(--bg-secondary)] transition-colors'

  if (step === 4 && diagnosis) {
    return (
      <div className="flex flex-col gap-[10px]">
        <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--accent)] tracking-[.3px] uppercase">
          Diagnóstico SignallQ
        </div>
        <h3 className="m-0 font-semibold text-[16px] leading-[1.38] text-[color:var(--text-primary)]">{diagnosis.title}</h3>
        <p className="m-0 font-normal text-[14px] leading-[1.43] text-[color:var(--text-secondary)]">{diagnosis.action}</p>
        <button
          onClick={reset}
          className="mt-2 w-fit rounded-full px-4 py-2 text-[13px] font-medium bg-[color:var(--bg-secondary)] text-[color:var(--text-primary)] border border-[color:var(--border)]"
        >
          Refazer diagnóstico
        </button>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-[10px]">
      <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--accent)] tracking-[.3px] uppercase">
        Diagnóstico guiado
      </div>

      {step === 0 && (
        <div>
          <p className="m-0 mb-3 font-semibold text-[16px] leading-[1.38] text-[color:var(--text-primary)]">Isso acontece em qual conexão?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('cabo_ou_wifi', 'wifi')} className={opcaoButtonClass}>Wi-Fi</button>
            <button onClick={() => handleAnswer('cabo_ou_wifi', 'cabo')} className={opcaoButtonClass}>Cabo de rede</button>
          </div>
        </div>
      )}

      {step === 1 && (
        <div>
          <p className="m-0 mb-3 font-semibold text-[16px] leading-[1.38] text-[color:var(--text-primary)]">Onde você percebe isso?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('perto', 'sim')} className={opcaoButtonClass}>Perto do roteador</button>
            <button onClick={() => handleAnswer('perto', 'nao')} className={opcaoButtonClass}>Longe do roteador</button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div>
          <p className="m-0 mb-3 font-semibold text-[16px] leading-[1.38] text-[color:var(--text-primary)]">Piora se outra pessoa na casa estiver usando a internet?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('outras_pessoas', 'sim')} className={opcaoButtonClass}>Sim, bastante</button>
            <button onClick={() => handleAnswer('outras_pessoas', 'nao')} className={opcaoButtonClass}>Não muda</button>
          </div>
        </div>
      )}

      {step === 3 && (
        <div>
          <p className="m-0 mb-3 font-semibold text-[16px] leading-[1.38] text-[color:var(--text-primary)]">Isso acontece com outros aparelhos?</p>
          <div className="flex gap-2">
            <button onClick={() => handleAnswer('outros_aparelhos', 'sim')} className={opcaoButtonClass}>Com qualquer um</button>
            <button onClick={() => handleAnswer('outros_aparelhos', 'nao')} className={opcaoButtonClass}>Só neste</button>
          </div>
        </div>
      )}
    </div>
  )
}
