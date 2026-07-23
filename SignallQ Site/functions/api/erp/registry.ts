import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import {
  isErpTipoSuportado,
  paraMetadados,
  resolveRegistroErp,
  salvarRegistroErp,
  type ErpTipoValido,
} from '../../_modules/erp-registry';
import { credencialRefParaTenant, salvarCredencialCifrada, type ErpCredenciaisEnv } from '../../_modules/erp-credenciais-kv';
import type { ErpCredenciais, ErpEscopo } from '../../_modules/erp-adapter';

const ESCOPOS_VALIDOS = new Set<ErpEscopo>(['erp:ticket:write', 'erp:read']);

function isEscopoValido(valor: string): valor is ErpEscopo {
  return ESCOPOS_VALIDOS.has(valor as ErpEscopo);
}

export type ErpRegistryEnv = ErpCredenciaisEnv & { DB?: D1Database };

type SalvarRegistroRequest = {
  tenant_id: string;
  erp_tipo: string;
  credenciais: ErpCredenciais;
  escopos?: string[];
};

function validarSalvarRegistro(body: unknown): body is SalvarRegistroRequest {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  const credenciais = b.credenciais as Record<string, unknown> | undefined;

  return (
    typeof b.tenant_id === 'string' && b.tenant_id.trim() !== '' &&
    typeof b.erp_tipo === 'string' && b.erp_tipo.trim() !== '' &&
    typeof credenciais === 'object' && credenciais !== null &&
    typeof credenciais.baseUrl === 'string' && credenciais.baseUrl.trim() !== '' &&
    typeof credenciais.token === 'string' && credenciais.token.trim() !== '' &&
    typeof credenciais.app === 'string' && credenciais.app.trim() !== '' &&
    (b.escopos === undefined || (Array.isArray(b.escopos) && b.escopos.every((s) => typeof s === 'string')))
  );
}

// POST /api/erp/registry — salva (ou substitui) a credencial de ERP de um tenant. A credencial é
// cifrada e persistida no KV (erp-credenciais-kv.ts) ANTES do registro no D1 apontar pra ela — se a
// cifragem/persistência no KV falhar, o D1 nunca chega a referenciar uma credencial inexistente.
// Resposta nunca inclui a credencial de volta (write-only do ponto de vista da API), só metadados.
export async function handleSalvarRegistroErp(request: Request, env: ErpRegistryEnv): Promise<Response> {
  const body = await readJsonBody<unknown>(request);
  if (!validarSalvarRegistro(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  if (!isErpTipoSuportado(body.erp_tipo)) {
    return errorResponse('erp_tipo não suportado.', 400);
  }
  const erpTipo: ErpTipoValido = body.erp_tipo;

  const escopos = body.escopos ?? [];
  const escopoInvalido = escopos.find((escopo) => !isEscopoValido(escopo));
  if (escopoInvalido !== undefined) {
    return errorResponse(`Escopo não reconhecido: ${escopoInvalido}.`, 400);
  }

  if (!env.DB) {
    return errorResponse('Registry indisponível (binding D1 ausente).', 503);
  }

  const credencialRef = credencialRefParaTenant(body.tenant_id, erpTipo);

  try {
    await salvarCredencialCifrada(env, credencialRef, body.credenciais);
  } catch (err) {
    // Nunca logar body.credenciais nem qualquer campo dela — só a mensagem de erro e a
    // credencialRef (identificador não sensível).
    console.error('[erp/registry] falha ao cifrar/salvar credencial no KV — registro não gravado', {
      tenant_id: body.tenant_id,
      credencial_ref: credencialRef,
      erro: err instanceof Error ? err.message : 'erro_desconhecido',
    });
    return errorResponse('Falha ao salvar credencial cifrada.', 502);
  }

  try {
    await salvarRegistroErp(env.DB, {
      tenantId: body.tenant_id,
      erpTipo,
      credencialRef,
      escopos,
    });
  } catch (err) {
    console.error('[erp/registry] falha ao gravar registro no D1 — credencial já cifrada no KV', {
      tenant_id: body.tenant_id,
      erro: err instanceof Error ? err.message : 'erro_desconhecido',
    });
    return errorResponse('Falha ao gravar registro.', 502);
  }

  return jsonResponse({
    tenantId: body.tenant_id,
    erpTipo,
    escopos,
    status: 'configurado',
  });
}

// GET /api/erp/registry?tenant_id=<id> — devolve só metadados (erpTipo, escopos, status). Nunca
// devolve credencial nem credencialRef (é um identificador interno, não precisa vazar pra UI).
export async function handleLerRegistroErp(request: Request, env: ErpRegistryEnv): Promise<Response> {
  const url = new URL(request.url);
  const tenantId = url.searchParams.get('tenant_id')?.trim();
  if (!tenantId) {
    return errorResponse('tenant_id obrigatório.', 400);
  }

  if (!env.DB) {
    return errorResponse('Registry indisponível (binding D1 ausente).', 503);
  }

  const registro = await resolveRegistroErp(env.DB, tenantId);
  if (!registro) {
    return jsonResponse({ error: 'tenant_sem_registro' }, { status: 404 });
  }

  return jsonResponse(paraMetadados(registro));
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<ErpRegistryEnv> = async ({ request, env }) => {
  return handleSalvarRegistroErp(request, env);
};

export const onRequestGet: PagesFunction<ErpRegistryEnv> = async ({ request, env }) => {
  return handleLerRegistroErp(request, env);
};
