"use client";
import Link from 'next/link'
import { PageShell } from '../components/PageShell'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { NOT_FOUND_META } from '../lib/pageMetaCatalog'

export default function NotFound() {
  useDocumentMeta(NOT_FOUND_META)

  return (
    <PageShell mobilePadding="pt-8 px-5 pb-12">
      <div className="w-full min-h-[520px] flex flex-col items-center justify-center gap-[14px] text-center">
        <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-secondary)] tracking-[.3px] uppercase">
          Erro 404
        </div>
        <div className="font-bold text-[26px] leading-[1.23] text-[color:var(--text-primary)]">
          Página não encontrada
        </div>
        <div className="max-w-[380px] font-normal text-[14px] leading-[1.45] text-[color:var(--text-secondary)]">
          O endereço que você acessou não existe ou foi movido.
        </div>
        <Link href="/" className="mt-1 h-[44px] flex items-center gap-2 px-5 rounded-[var(--radius-button)] bg-[color:var(--accent)] no-underline hover:brightness-110 transition-all">
          <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">speed</span>
          <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--on-accent)]">
            Ir para o teste de velocidade
          </span>
        </Link>
      </div>
    </PageShell>
  )
}
