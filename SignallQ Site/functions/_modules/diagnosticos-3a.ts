export type RegistroDiagnostico3a = {
  tenantId: string;
  popNome: string | null;
  diagnosticoId: string;
  tipoProblema: string;
};

export async function registrarDiagnostico3a(
  db: D1Database,
  registro: RegistroDiagnostico3a,
): Promise<'registrado' | 'duplicado'> {
  const existente = await db
    .prepare('SELECT id FROM diagnosticos_3a WHERE diagnostico_id = ?1 LIMIT 1')
    .bind(registro.diagnosticoId)
    .first<{ id: number }>();

  if (existente) return 'duplicado';

  await db
    .prepare(
      'INSERT INTO diagnosticos_3a (tenant_id, pop_nome, diagnostico_id, tipo_problema) VALUES (?1, ?2, ?3, ?4)',
    )
    .bind(registro.tenantId, registro.popNome, registro.diagnosticoId, registro.tipoProblema)
    .run();

  return 'registrado';
}
