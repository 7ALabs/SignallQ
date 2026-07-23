// Guarda de idempotência para abertura de chamado no ERP (gate da Gema, PR #118). Não-negociável
// de .claude/skills/integracao-erp/SKILL.md: "não abrir OS duplicada para o mesmo
// diagnóstico/assinante numa janela curta". `diagnosticos_3b` (massiva.ts) não serve pra isso: só
// grava com pop_nome presente, sem UNIQUE, e grava DEPOIS da escrita no ERP — não fecha a corrida
// entre a checagem defensiva em 0c (listarOcorrenciasAbertasSgp, issue #83) e a escrita real em 3B,
// ~20s depois. Esta guarda é escrita ANTES de chamar o adapter e vale pra todo chamado.

export type ReservaIdempotencia =
  | { status: 'reservado' }
  | { status: 'ja_processado'; protocolo: string }
  | { status: 'em_andamento' };

// SQLite (D1) reporta violação de constraint UNIQUE com essa mensagem — é o sinal que diferencia
// "conflito esperado" (outra requisição já reservou este diagnostico_id) de qualquer outro erro
// real de D1 (conexão, tabela ausente), que deve propagar e não ser mascarado como concorrência.
function isViolacaoUnique(err: unknown): boolean {
  return err instanceof Error && /unique/i.test(err.message);
}

// Tenta reservar o diagnostico_id antes de abrir o chamado no ERP. 'reservado': primeira
// requisição, segue para o adapter. 'ja_processado': já existe protocolo gravado — devolve o
// mesmo protocolo, sem chamar o adapter de novo. 'em_andamento': outra requisição está processando
// este diagnostico_id agora (ainda sem protocolo) — não abre um segundo chamado; quem chama trata
// como conflito (retry do cliente deve aguardar, não duplicar).
export async function reservarChamado(
  db: D1Database,
  params: { diagnosticoId: string; tenantId: string; assinanteRef: string },
): Promise<ReservaIdempotencia> {
  try {
    await db
      .prepare('INSERT INTO chamados_idempotencia (diagnostico_id, tenant_id, assinante_ref) VALUES (?1, ?2, ?3)')
      .bind(params.diagnosticoId, params.tenantId, params.assinanteRef)
      .run();
    return { status: 'reservado' };
  } catch (err) {
    if (!isViolacaoUnique(err)) throw err;

    const existente = await db
      .prepare('SELECT protocolo FROM chamados_idempotencia WHERE diagnostico_id = ?1')
      .bind(params.diagnosticoId)
      .first<{ protocolo: string | null }>();

    if (existente?.protocolo) {
      return { status: 'ja_processado', protocolo: existente.protocolo };
    }
    return { status: 'em_andamento' };
  }
}

// Preenche o protocolo após o adapter confirmar a abertura do chamado — sem isso, toda reserva
// ficaria presa em 'em_andamento' pra sempre (nenhuma requisição futura conseguiria ler o
// protocolo já obtido).
export async function confirmarProtocolo(
  db: D1Database,
  diagnosticoId: string,
  protocolo: string,
): Promise<void> {
  await db
    .prepare('UPDATE chamados_idempotencia SET protocolo = ?1 WHERE diagnostico_id = ?2')
    .bind(protocolo, diagnosticoId)
    .run();
}

// Libera a reserva quando o adapter falha (timeout, 4xx/5xx) — sem isso, um diagnostico_id que
// falhou ficaria travado em 'em_andamento' para sempre, e um retry legítimo do assinante (após o
// ERP se recuperar) nunca conseguiria abrir o chamado.
export async function liberarReserva(db: D1Database, diagnosticoId: string): Promise<void> {
  await db.prepare('DELETE FROM chamados_idempotencia WHERE diagnostico_id = ?1').bind(diagnosticoId).run();
}
