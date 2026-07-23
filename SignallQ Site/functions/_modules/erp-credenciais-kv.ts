// Persistência da credencial cifrada em Cloudflare Workers KV (issue #34). D1 guarda só
// `credencial_ref` (erp-registry.ts) — a credencial em si nunca passa pelo D1, nunca em texto
// plano, e nunca é logada. Decisão registrada em docs/adr/0003-camada-adapters-erp.md.

import { cifrarCredencial, decifrarCredencial, importarChaveMestra } from './erp-credenciais-cifra';
import type { ErpCredenciais } from './erp-adapter';

export interface ErpCredenciaisEnv {
  ERP_CREDS_KV?: KVNamespace;
  ERP_CREDS_ENCRYPTION_KEY?: string;
}

// credencial_ref é o nome da chave no KV — prefixo fixo evita colisão com outras chaves que
// eventualmente compartilhem o mesmo namespace KV no futuro.
export function credencialRefParaTenant(tenantId: string, erpTipo: string): string {
  return `erp-cred:${tenantId}:${erpTipo}`;
}

function resolveChaveMestra(env: ErpCredenciaisEnv): string | null {
  return env.ERP_CREDS_ENCRYPTION_KEY ?? null;
}

// Cifra e salva a credencial no KV sob credencialRef. Nunca loga o valor da credencial nem da
// chave mestra — só a credencialRef (não sensível, é só um identificador) em caso de erro.
export async function salvarCredencialCifrada(
  env: ErpCredenciaisEnv,
  credencialRef: string,
  credenciais: ErpCredenciais,
): Promise<void> {
  const kv = env.ERP_CREDS_KV;
  const chaveMestraBase64 = resolveChaveMestra(env);
  if (!kv || !chaveMestraBase64) {
    throw new Error('erp_creds_kv_nao_configurado');
  }

  const chave = await importarChaveMestra(chaveMestraBase64);
  const cifrada = await cifrarCredencial(chave, JSON.stringify(credenciais));
  await kv.put(credencialRef, cifrada.valor);
}

// Lê e decifra a credencial do KV. Retorna null quando a referência não existe (registro aponta
// para uma credencial nunca salva, ou já removida) — nunca lança por "não encontrado", só por
// falha real de cifragem (chave errada, valor corrompido) ou infraestrutura ausente.
export async function lerCredencialCifrada(
  env: ErpCredenciaisEnv,
  credencialRef: string,
): Promise<ErpCredenciais | null> {
  const kv = env.ERP_CREDS_KV;
  const chaveMestraBase64 = resolveChaveMestra(env);
  if (!kv || !chaveMestraBase64) {
    throw new Error('erp_creds_kv_nao_configurado');
  }

  const valorCifrado = await kv.get(credencialRef);
  if (!valorCifrado) return null;

  const chave = await importarChaveMestra(chaveMestraBase64);
  const json = await decifrarCredencial(chave, { valor: valorCifrado });
  return JSON.parse(json) as ErpCredenciais;
}
