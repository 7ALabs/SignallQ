import { resolveMassivaTtlHoras, resolveSgpCreds, type SgpEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { detectarPossivelMassiva, registrarDiagnostico3b } from '../../_modules/massiva';
import { possuiEscopo, resolveErpAdapter, resolveRegistroErp } from '../../_modules/erp-registry';
import { sgpAdapter } from '../../_modules/sgp-adapter';
import { confirmarProtocolo, liberarReserva, reservarChamado } from '../../_modules/chamado-idempotencia';
import { lerCredencialCifrada, type ErpCredenciaisEnv } from '../../_modules/erp-credenciais-kv';
import { ESCOPO_CRIAR_CHAMADO, type ErpAdapter, type ErpCredenciais } from '../../_modules/erp-adapter';
import type { ChamadoCanônico } from '../../../src/shared/chamado';

// Interseção (e não interface) porque SgpEnv tem index signature string — o binding D1 não cabe
// nela, mas cabe numa interseção.
export type ChamadoEnv = SgpEnv & ErpCredenciaisEnv & { DB?: D1Database };

const PROBLEMAS_VALIDOS = new Set(['lenta', 'caindo', 'video', 'caiu']);
const NIVEIS_CONFIANCA_VALIDOS = new Set(['high', 'medium', 'low']);
const TURNOS_VALIDOS = new Set(['manha', 'tarde', 'qualquer']);

function validarChamado(body: unknown): body is ChamadoCanônico {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  const metricas = b.metricas as Record<string, unknown> | undefined;
  return (
    typeof b.tenant_id === 'string' && b.tenant_id.trim() !== '' &&
    typeof b.assinante_ref === 'string' && b.assinante_ref.trim() !== '' &&
    typeof b.problema_declarado === 'string' && PROBLEMAS_VALIDOS.has(b.problema_declarado) &&
    typeof metricas === 'object' && metricas !== null &&
    typeof metricas.confianca === 'string' && NIVEIS_CONFIANCA_VALIDOS.has(metricas.confianca) &&
    // turno_preferido é opcional (issue #106) — chamadas antigas sem o campo continuam válidas
    (b.turno_preferido === undefined || (typeof b.turno_preferido === 'string' && TURNOS_VALIDOS.has(b.turno_preferido))) &&
    // pop_nome é opcional (issue #97) — o SGP pode não retornar POP no lookup
    (b.pop_nome === undefined || (typeof b.pop_nome === 'string' && b.pop_nome.trim() !== ''))
  );
}

export type ResolucaoAdapter =
  | { status: 'resolvido'; adapter: ErpAdapter; credenciais: ErpCredenciais }
  | { status: 'nao_configurado' }
  | { status: 'escopo_negado' };

// Resolve o adapter e as credenciais para o tenant. Prioriza o registry no D1 (ispId -> erpTipo,
// issue #27) quando o binding existe e o tenant já foi migrado para lá — a credencial, nesse
// caso, vem cifrada do KV (issue #34, lerCredencialCifrada) via credencial_ref do registro, nunca
// de env var. Sem registro no D1 (caso de todo tenant ainda não migrado) cai para o adapter SGP
// fixo com credenciais por env var (resolveSgpCreds) — mesmo comportamento pré-existente,
// preservado para não quebrar tenant que ainda não passou pelo onboarding via /api/erp/registry.
//
// Escopos granulares (issue #28): só se aplicam a tenant resolvido via registry — é o registry que
// carrega a lista de escopos concedidos (erp_registry.escopos). Tenant em fallback por env var não
// tem conceito de escopo (é o comportamento legado, tudo-ou-nada, preservado como estava). Um
// tenant no registry SEM o escopo erp:ticket:write é negado explicitamente ('escopo_negado') — não
// cai para o fallback por env var, porque isso seria escalar privilégio silenciosamente driblando
// a concessão de escopo que o próprio registry define para aquele tenant.
async function resolveAdapterETenant(env: ChamadoEnv, tenantId: string): Promise<ResolucaoAdapter> {
  if (env.DB) {
    try {
      const registro = await resolveRegistroErp(env.DB, tenantId);
      if (registro) {
        if (!possuiEscopo(registro, ESCOPO_CRIAR_CHAMADO)) {
          console.warn('[erp/chamado] tenant sem escopo erp:ticket:write — chamado recusado', {
            tenant_id: tenantId,
          });
          return { status: 'escopo_negado' };
        }

        const adapter = resolveErpAdapter(registro.erpTipo);
        const creds = await lerCredencialCifrada(env, registro.credencialRef);
        if (adapter && creds) return { status: 'resolvido', adapter, credenciais: creds };

        if (adapter && !creds) {
          // Registro existe mas a credencial referenciada não está no KV (nunca salva, removida,
          // ou credencial_ref órfã) — não cai silenciosamente para env var, porque isso mascararia
          // um registro quebrado como se estivesse usando a credencial certa do tenant errado.
          console.error('[erp/chamado] registro aponta para credencial ausente no KV', {
            tenant_id: tenantId,
          });
          return { status: 'nao_configurado' };
        }
      }
    } catch (err) {
      // Erro real de infraestrutura (D1 ou KV indisponível, chave mestra ausente/inválida) —
      // fail-open para o fallback por env var, nunca derruba a abertura de chamado por causa disso.
      // Nunca logar a credencial nem a chave mestra aqui, só a mensagem de erro.
      console.warn('[erp/chamado] falha ao resolver registry/credencial cifrada — usando fallback por env var', {
        tenant_id: tenantId,
        erro: err instanceof Error ? err.message : 'erro_desconhecido',
      });
    }
  }

  const creds = resolveSgpCreds(env, tenantId);
  if (!creds) return { status: 'nao_configurado' };
  return { status: 'resolvido', adapter: sgpAdapter, credenciais: creds };
}

export async function handleErpChamado(
  request: Request,
  env: ChamadoEnv,
  fetcher: typeof fetch = fetch,
  // ctx.waitUntil do Pages em produção — a detecção de massiva (#98) roda depois da resposta ao
  // assinante e nunca pode atrasá-la. O default descarta o retorno (a promise nunca rejeita).
  waitUntil: (promise: Promise<unknown>) => void = (promise) => {
    void promise;
  },
): Promise<Response> {
  const body = await readJsonBody<unknown>(request);
  if (!validarChamado(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  // Regra de gatilho (issue #22): só abre chamado automaticamente com confiança alta. O front só
  // deveria chamar este endpoint quando confianca === 'high' OU quando o assinante confirmou
  // explicitamente numa tela intermediária (Estado3AConfirmar) — o backend não confia no cliente,
  // reforça a regra aqui e registra o motivo quando bloqueia.
  if (body.metricas.confianca !== 'high' && !body.confirmado_pelo_assinante) {
    console.warn('[erp/chamado] chamado não aberto: confiança da medição não é alta', {
      tenant_id: body.tenant_id,
      diagnostico_id: body.diagnostico_id,
      confianca: body.metricas.confianca,
    });
    return jsonResponse({ error: 'confianca_insuficiente' }, { status: 422 });
  }

  if (body.metricas.confianca !== 'high' && body.confirmado_pelo_assinante) {
    console.log('[erp/chamado] abrindo chamado com confiança baixa/média — confirmado pelo assinante', {
      tenant_id: body.tenant_id,
      diagnostico_id: body.diagnostico_id,
      confianca: body.metricas.confianca,
    });
  }

  const resolvido = await resolveAdapterETenant(env, body.tenant_id);
  if (resolvido.status === 'escopo_negado') {
    // Erro claro, não silencioso (issue #28): a ação canônica nunca é invocada quando o tenant não
    // tem o escopo concedido — 403, não 503, para distinguir "tenant sem permissão" de "tenant sem
    // configuração nenhuma".
    return jsonResponse({ error: 'escopo_negado', escopo_requerido: ESCOPO_CRIAR_CHAMADO }, { status: 403 });
  }
  if (resolvido.status === 'nao_configurado') {
    return errorResponse('Tenant não configurado.', 503);
  }
  const { adapter, credenciais } = resolvido;

  // Guarda de idempotência (não-negociável de integracao-erp/SKILL.md): reserva o diagnostico_id
  // ANTES de chamar o adapter. Sem binding D1 não há como reservar — fail-open sem guarda, mesmo
  // comportamento pré-existente para ambiente sem D1 (nunca visto em produção, só em teste/local).
  // Falha REAL de D1 aqui (conexão, tabela ausente) não é fail-open — sem conseguir reservar, não
  // há garantia contra chamado duplicado, então a requisição falha explicitamente em vez de seguir
  // cega para o adapter.
  if (env.DB) {
    let reserva;
    try {
      reserva = await reservarChamado(env.DB, {
        diagnosticoId: body.diagnostico_id,
        tenantId: body.tenant_id,
        assinanteRef: body.assinante_ref,
      });
    } catch (reservaErr) {
      console.error('[erp/chamado] falha ao reservar guarda de idempotência — chamado não aberto', {
        tenant_id: body.tenant_id,
        diagnostico_id: body.diagnostico_id,
        erro: reservaErr instanceof Error ? reservaErr.message : 'erro_desconhecido',
      });
      return jsonResponse({ error: 'falha_guarda_idempotencia' }, { status: 502 });
    }

    if (reserva.status === 'ja_processado') {
      console.log('[erp/chamado] diagnostico_id já processado — devolvendo protocolo existente sem reabrir', {
        tenant_id: body.tenant_id,
        diagnostico_id: body.diagnostico_id,
      });
      return jsonResponse({ protocolo: reserva.protocolo });
    }

    if (reserva.status === 'em_andamento') {
      console.warn('[erp/chamado] diagnostico_id já em processamento por outra requisição — não reabrindo', {
        tenant_id: body.tenant_id,
        diagnostico_id: body.diagnostico_id,
      });
      return jsonResponse({ error: 'chamado_em_processamento' }, { status: 409 });
    }
  }

  try {
    let protocolo: string;
    try {
      ({ protocolo } = await adapter.criarChamado(credenciais, body, fetcher));
    } catch (adapterErr) {
      // Libera a reserva só quando o próprio adapter falhou (ERP recusou/caiu) — sem isso, um
      // diagnostico_id que falhou ficaria travado em 'em_andamento' para sempre, e um retry
      // legítimo do assinante (após o ERP se recuperar) nunca conseguiria abrir o chamado.
      if (env.DB) {
        await liberarReserva(env.DB, body.diagnostico_id);
      }
      throw adapterErr;
    }

    if (env.DB) {
      // Falha em confirmar o protocolo na guarda não derruba a resposta — o protocolo já foi
      // obtido do ERP e é o que importa para o assinante; só deixaria a reserva presa em
      // 'em_andamento' até uma limpeza futura, o que é aceitável frente a perder/duplicar o laudo.
      try {
        await confirmarProtocolo(env.DB, body.diagnostico_id, protocolo);
      } catch (confirmErr) {
        console.error('[erp/chamado] falha ao confirmar protocolo na guarda de idempotência', {
          tenant_id: body.tenant_id,
          diagnostico_id: body.diagnostico_id,
          erro: confirmErr instanceof Error ? confirmErr.message : 'erro_desconhecido',
        });
      }
    }

    // Registro do diagnóstico 3B por POP (issue #97) — base da detecção de massiva. O protocolo
    // já foi obtido: falha aqui nunca falha a resposta ao assinante, só perde um ponto da série.
    if (env.DB) {
      try {
        await registrarDiagnostico3b(env.DB, {
          tenantId: body.tenant_id,
          popNome: body.pop_nome ?? null,
          diagnosticoId: body.diagnostico_id,
          protocolo,
        });
      } catch (dbErr) {
        console.error('[erp/chamado] falha ao registrar diagnóstico 3B no D1 — resposta ao assinante preservada', {
          tenant_id: body.tenant_id,
          diagnostico_id: body.diagnostico_id,
          erro: dbErr instanceof Error ? dbErr.message : 'erro_desconhecido',
        });
      }

      // Detecção de possível massiva (issue #98) — fire-and-forget: nunca atrasa nem quebra a
      // resposta. Sem POP não há série por região para avaliar.
      if (body.pop_nome) {
        const ttlHoras = resolveMassivaTtlHoras(env, body.tenant_id);
        waitUntil(detectarPossivelMassiva(env.DB, env, body.tenant_id, body.pop_nome, ttlHoras, fetcher));
      }
    } else {
      console.warn('[erp/chamado] binding D1 ausente — diagnóstico 3B não registrado', {
        tenant_id: body.tenant_id,
        diagnostico_id: body.diagnostico_id,
      });
    }

    return jsonResponse({ protocolo });
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'erro_desconhecido';

    if (msg === 'timeout') {
      console.error('[erp/chamado] timeout', { tenant_id: body.tenant_id, diagnostico_id: body.diagnostico_id });
      return jsonResponse({ error: 'timeout' }, { status: 503 });
    }

    // Nunca expor credenciais ou CPF na resposta de erro
    console.error('[erp/chamado] falha ao abrir chamado SGP', {
      tenant_id: body.tenant_id,
      diagnostico_id: body.diagnostico_id,
      erro: msg,
    });

    return jsonResponse({ error: msg }, { status: 502 });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

// waitUntil vai numa arrow presa ao ctx — destruturado solto pode perder o this no runtime
// (Illegal invocation), gotcha conhecido do workerd.
export const onRequestPost: PagesFunction<ChamadoEnv> = async (ctx) => {
  return handleErpChamado(ctx.request, ctx.env, fetch, (promise) => ctx.waitUntil(promise));
};
