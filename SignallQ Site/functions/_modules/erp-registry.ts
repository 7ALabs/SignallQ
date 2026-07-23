import type { ErpAdapter, ErpEscopo, ErpTipo } from './erp-adapter';
import { sgpAdapter } from './sgp-adapter';

// Registry de adapters concretos (issue #26/#27). Novo ERP entra aqui como uma linha nova — o
// core nunca referencia sgpAdapter (ou qualquer outro) diretamente, só resolveErpAdapter().
const ADAPTERS: Record<ErpTipo, ErpAdapter> = {
  sgp: sgpAdapter,
};

export type ErpRegistryRow = {
  tenant_id: string;
  erp_tipo: string;
  credencial_ref: string;
  escopos: string;
};

export type ErpRegistroTenant = {
  tenantId: string;
  erpTipo: ErpTipo;
  credencialRef: string;
  escopos: string[];
};

const ERP_TIPOS_VALIDOS = new Set<ErpTipo>(['sgp']);

function isErpTipoValido(valor: string): valor is ErpTipo {
  return ERP_TIPOS_VALIDOS.has(valor as ErpTipo);
}

// Resolve o registro de um tenant no D1 (ispId -> erpTipo, credencial_ref, escopos). Retorna null
// quando o tenant não tem ERP configurado ou quando erp_tipo gravado não é reconhecido (registro
// corrompido/de um ERP ainda não suportado neste deploy) — nunca lança, quem chama decide o 503.
export async function resolveRegistroErp(
  db: D1Database,
  tenantId: string,
): Promise<ErpRegistroTenant | null> {
  const row = await db
    .prepare('SELECT tenant_id, erp_tipo, credencial_ref, escopos FROM erp_registry WHERE tenant_id = ?1 LIMIT 1')
    .bind(tenantId)
    .first<ErpRegistryRow>();

  if (!row || !isErpTipoValido(row.erp_tipo)) return null;

  let escopos: string[];
  try {
    const parsed: unknown = JSON.parse(row.escopos);
    escopos = Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
  } catch {
    escopos = [];
  }

  return {
    tenantId: row.tenant_id,
    erpTipo: row.erp_tipo,
    credencialRef: row.credencial_ref,
    escopos,
  };
}

// Resolve a instância do adapter a partir do erpTipo do registro — nunca instancia adapter fora
// deste mapa, então um erpTipo desconhecido (linha nova ainda sem adapter implementado) retorna
// null em vez de lançar.
export function resolveErpAdapter(erpTipo: ErpTipo): ErpAdapter | null {
  return ADAPTERS[erpTipo] ?? null;
}

export type ErpTipoValido = ErpTipo;

export function isErpTipoSuportado(valor: string): valor is ErpTipo {
  return isErpTipoValido(valor);
}

// Metadados públicos do registro — nunca inclui credencial, só o necessário para a UI de
// onboarding/admin exibir o que está configurado (issue #34: "credencial nunca retornada para a UI
// após salva").
export type ErpRegistroMetadados = {
  tenantId: string;
  erpTipo: ErpTipo;
  escopos: string[];
  status: 'configurado';
};

export function paraMetadados(registro: ErpRegistroTenant): ErpRegistroMetadados {
  return {
    tenantId: registro.tenantId,
    erpTipo: registro.erpTipo,
    escopos: registro.escopos,
    status: 'configurado',
  };
}

// Escopos granulares por ação (issue #28) — o registry carrega a lista de escopos concedidos ao
// tenant (já persistida desde #27, `erp_registry.escopos`); esta função só formaliza a checagem de
// "o tenant tem este escopo?" num único lugar, para não reimplementar `escopos.includes(...)` em
// cada endpoint que decide se pode ou não invocar uma ação do adapter.
export function possuiEscopo(registro: ErpRegistroTenant, escopoRequerido: ErpEscopo): boolean {
  return registro.escopos.includes(escopoRequerido);
}

// Grava ou atualiza o registro do tenant (tenant_id -> erp_tipo, credencial_ref, escopos). Upsert
// por tenant_id (UNIQUE, migration 0004) — um tenant reconfigurando o ERP substitui a linha
// existente, não duplica. A credencial em si NUNCA passa por aqui: quem chama já cifrou e salvou
// no KV antes (erp-credenciais-kv.ts) e só passa a referência.
export async function salvarRegistroErp(
  db: D1Database,
  registro: { tenantId: string; erpTipo: ErpTipo; credencialRef: string; escopos: string[] },
): Promise<void> {
  await db
    .prepare(
      `INSERT INTO erp_registry (tenant_id, erp_tipo, credencial_ref, escopos)
       VALUES (?1, ?2, ?3, ?4)
       ON CONFLICT(tenant_id) DO UPDATE SET
         erp_tipo = excluded.erp_tipo,
         credencial_ref = excluded.credencial_ref,
         escopos = excluded.escopos,
         atualizado_em = datetime('now')`,
    )
    .bind(registro.tenantId, registro.erpTipo, registro.credencialRef, JSON.stringify(registro.escopos))
    .run();
}
