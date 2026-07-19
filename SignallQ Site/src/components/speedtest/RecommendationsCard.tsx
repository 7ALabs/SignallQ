import type { Recommendation } from '../../lib/recommendations'

interface RecommendationsCardProps {
  recommendations: Recommendation[]
  onRepeatTest: () => void
}

// Card de 1-3 recomendações pós-resultado. Omitido inteiramente quando o
// motor determinístico (lib/recommendations.ts) não tem o que dizer — nunca
// cai para um card genérico ou para IA como fallback.
export function RecommendationsCard({ recommendations, onRepeatTest }: RecommendationsCardProps) {
  if (recommendations.length === 0) return null

  return (
    <div
      className="flex w-full max-w-[560px] flex-col gap-3.5 rounded-2xl border p-4"
      style={{ borderColor: 'color-mix(in srgb, var(--border) 18%, transparent)', background: 'var(--bg-card)' }}
    >
      <div className="overline">Recomendações</div>
      <div className="flex flex-col gap-3">
        {recommendations.map((item) => (
          <div key={item.id} className="flex items-start gap-3">
            <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--accent)' }}>
              {item.icon}
            </span>
            <div className="flex flex-1 flex-col gap-0.5">
              <div className="label-large">{item.title}</div>
              <div className="body-small" style={{ color: 'var(--text-secondary)' }}>
                {item.description}
              </div>
              {item.actionType === 'repeat_test' && (
                <button
                  onClick={onRepeatTest}
                  className="mt-1 flex w-fit items-center gap-1 border-none bg-transparent p-0"
                >
                  <span className="label-medium" style={{ color: 'var(--accent)' }}>
                    Testar novamente
                  </span>
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
