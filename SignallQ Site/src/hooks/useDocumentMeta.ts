import { useEffect } from 'react'
import { applyPageMeta, type PageMeta } from '../lib/seo'

export function useDocumentMeta(meta: PageMeta) {
  useEffect(() => {
    applyPageMeta(meta)
  }, [meta.title, meta.description, meta.path])
}
