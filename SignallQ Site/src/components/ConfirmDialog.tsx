interface ConfirmDialogProps {
  title: string
  description: string
  confirmLabel: string
  cancelLabel: string
  icon: string
  danger?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ title, description, confirmLabel, cancelLabel, icon, danger, onConfirm, onCancel }: ConfirmDialogProps) {
  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center bg-black/40 p-5" onClick={onCancel}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
        className="flex w-full max-w-[380px] flex-col items-center gap-3 rounded-[var(--radius-dialog)] p-6 text-center"
        style={{ background: 'var(--bg-card)' }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 32, color: danger ? 'var(--error)' : 'var(--accent)' }}>
          {icon}
        </span>
        <div className="headline-small">{title}</div>
        <div className="body-medium">{description}</div>
        <div className="mt-2 flex w-full gap-2.5">
          <button onClick={onCancel} className="h-11 flex-1 rounded-[var(--radius-button)] border label-large" style={{ borderColor: 'var(--border)' }}>
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            className="h-11 flex-1 rounded-[var(--radius-button)] text-white label-large"
            style={{ background: danger ? 'var(--error)' : 'var(--accent)' }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
