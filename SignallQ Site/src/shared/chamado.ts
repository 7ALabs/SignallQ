export type ChamadoCanônico = {
  tenant_id: string;
  assinante_ref: string;
  assinante_nome: string;
  problema_declarado: 'lenta' | 'caindo' | 'video' | 'caiu';
  diagnostico_resumo: string;
  metricas: {
    download_mbps: number | null;
    latencia_ms: number | null;
    jitter_ms: number | null;
    confianca: 'high' | 'medium' | 'low';
  };
  timestamp: string;
  diagnostico_id: string;
  // Presente quando o assinante confirmou explicitamente a abertura numa tela intermediária
  // (confiança 'medium'/'low' — issue #22). Permite ao adapter abrir mesmo sem confiança alta,
  // já que a confirmação humana substitui a heurística automática nesse caso.
  confirmado_pelo_assinante?: boolean;
  // Turno preferido para visita técnica (issue #106). Opcional: só é perguntado quando o chamado
  // de fato vai ser aberto (confiança alta, ou confirmação explícita em Estado3AConfirmar). Não há
  // campo dedicado documentado no SGP para isso — vai no texto livre de `conteudo` (ver
  // functions/_modules/sgp.ts).
  turno_preferido?: 'manha' | 'tarde' | 'qualquer';
  // POP do contrato (campo `popNome` do lookup por CPF, issue #97). Opcional: o SGP pode não
  // retornar. Usado só no registro do diagnóstico 3B no D1 (detecção de massiva) — não vai ao SGP.
  pop_nome?: string;
};
