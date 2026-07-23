// Cifragem em repouso de credencial de ERP (issue #34, decisão registrada em
// docs/adr/0003-camada-adapters-erp.md § "Onde a cifragem acontece"). AES-GCM via crypto.subtle
// (nativo do runtime workerd, sem dependência externa). A chave mestra nunca é gerada nem lida
// daqui além de vir de fora (Cloudflare Secret, ver resolveChaveMestra em erp-credenciais-kv.ts) —
// este módulo só cifra/decifra dado uma chave já importada.

const ALGORITMO = 'AES-GCM';
const TAMANHO_IV_BYTES = 12; // recomendado para GCM (96 bits) — não usar 16 aqui, é o padrão da spec

export type CredencialCifrada = {
  // IV e ciphertext em base64, concatenados por ':' — formato de armazenamento único no KV, sem
  // precisar de um segundo campo/coluna para o IV.
  valor: string;
};

// Chave mestra vem como string base64 de 32 bytes (256 bits) do Cloudflare Secret
// (ERP_CREDS_ENCRYPTION_KEY) — nunca hardcoded, nunca versionada. Importada uma vez por chamada;
// não há cache de CryptoKey entre requests porque cada invocação do Worker é isolada.
export async function importarChaveMestra(chaveMestraBase64: string): Promise<CryptoKey> {
  const bytes = base64ParaBytes(chaveMestraBase64);
  if (bytes.length !== 32) {
    throw new Error('chave_mestra_tamanho_invalido');
  }
  return crypto.subtle.importKey('raw', bytes, ALGORITMO, false, ['encrypt', 'decrypt']);
}

export async function cifrarCredencial(chave: CryptoKey, credencialJson: string): Promise<CredencialCifrada> {
  const iv = crypto.getRandomValues(new Uint8Array(TAMANHO_IV_BYTES));
  const dados = new TextEncoder().encode(credencialJson);
  const cifrado = await crypto.subtle.encrypt({ name: ALGORITMO, iv }, chave, dados);

  return { valor: `${bytesParaBase64(iv)}:${bytesParaBase64(new Uint8Array(cifrado))}` };
}

// Lança em qualquer falha de decifragem (chave errada, valor corrompido, tag de autenticação
// inválida) — nunca retorna parcial nem mascara erro criptográfico como dado válido.
export async function decifrarCredencial(chave: CryptoKey, credencialCifrada: CredencialCifrada): Promise<string> {
  const [ivBase64, cifradoBase64] = credencialCifrada.valor.split(':');
  if (!ivBase64 || !cifradoBase64) {
    throw new Error('formato_credencial_cifrada_invalido');
  }

  const iv = base64ParaBytes(ivBase64);
  const cifrado = base64ParaBytes(cifradoBase64);
  const decifrado = await crypto.subtle.decrypt({ name: ALGORITMO, iv }, chave, cifrado);

  return new TextDecoder().decode(decifrado);
}

function bytesParaBase64(bytes: Uint8Array): string {
  let binario = '';
  for (const byte of bytes) binario += String.fromCharCode(byte);
  return btoa(binario);
}

function base64ParaBytes(base64: string): Uint8Array {
  const binario = atob(base64);
  const bytes = new Uint8Array(binario.length);
  for (let i = 0; i < binario.length; i++) bytes[i] = binario.charCodeAt(i);
  return bytes;
}
