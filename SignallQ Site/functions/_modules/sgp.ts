import type { ChamadoCanônico } from '../../src/shared/chamado';

export type SgpAssinanteResult = {
  nome: string;
  endereco: string;
  contrato_id: string;
  contrato_status: string;
  motivo_status?: string;
  pop_nome?: string;
};

type SgpContrato = {
  contratoId?: number;
  contratoStatusDisplay?: string;
  motivo_status?: string;
  razaoSocial?: string;
  endereco_logradouro?: string;
  endereco_numero?: number;
  endereco_bairro?: string;
  endereco_cidade?: string;
  endereco_uf?: string;
  popNome?: string;
};

const MAX_CONTRATOS_ATIVOS = 5;

function formatEnderecoSgp(contrato: SgpContrato): string {
  return [
    [contrato.endereco_logradouro, contrato.endereco_numero].filter(Boolean).join(', '),
    contrato.endereco_bairro,
    [contrato.endereco_cidade, contrato.endereco_uf].filter(Boolean).join('/'),
  ]
    .filter(Boolean)
    .join(' - ');
}

// Endpoint do módulo URA (mesmo módulo de /api/ura/chamado/) — não exige que o
// cliente esteja classificado como "CRM", que é um cadastro à parte no SGP e não
// cobre clientes comuns do Financeiro. É POST, então não esbarra na restrição da
// Fetch API que proíbe body em GET/HEAD.
//
// Um CPF pode ter mais de um contrato ativo simultaneamente (ex.: duas casas). Retorna
// até MAX_CONTRATOS_ATIVOS para o assinante escolher no Estado 0c. Se não houver nenhum
// contrato ativo, cai no primeiro contrato do cadastro (ex.: suspenso) como fallback.
export async function lookupAssinanteSgp(
  baseUrl: string,
  token: string,
  app: string,
  cpf: string,
  fetcher: typeof fetch = fetch,
): Promise<SgpAssinanteResult[]> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetcher(`${baseUrl}/api/ura/consultacliente/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, app, cpfcnpj: cpf }),
      signal: controller.signal,
    });

    if (response.status !== 200) return [];

    const data = (await response.json()) as { contratos?: SgpContrato[] };

    if (!data.contratos || data.contratos.length === 0) return [];

    const ativos = data.contratos.filter((c) => c.contratoStatusDisplay === 'Ativo');
    const candidatos = ativos.length > 0 ? ativos.slice(0, MAX_CONTRATOS_ATIVOS) : data.contratos.slice(0, 1);

    return candidatos.map((contrato) => ({
      nome: contrato.razaoSocial ?? '',
      endereco: formatEnderecoSgp(contrato),
      contrato_id: String(contrato.contratoId ?? ''),
      contrato_status: contrato.contratoStatusDisplay ?? '',
      ...(contrato.motivo_status ? { motivo_status: contrato.motivo_status } : {}),
      ...(contrato.popNome ? { pop_nome: contrato.popNome } : {}),
    }));
  } finally {
    clearTimeout(timeoutId);
  }
}

type SgpChamadoPayload = {
  token: string;
  app: string;
  contrato: number;
  ocorrenciatipo?: number;
  conteudo: string;
  observacao: string;
  setor?: number;
  usuario?: string;
  contato_nome?: string;
};

type SgpChamadoResponse = {
  status: number;
  protocolo?: string;
  contratoId?: number;
  msg?: string;
};

type SgpOcorrenciaOs = {
  id?: number;
  status?: string;
  data_agendamento?: string;
};

export type SgpOcorrencia = {
  numero?: string;
  status?: string;
  data_agendamento?: string;
  ordens_servicos?: SgpOcorrenciaOs[];
};

export type SgpOcorrenciaAbertaResult = {
  temOcorrenciaAberta: boolean;
  protocolo?: string;
  status?: string;
  dataAgendamento?: string;
};

// data_agendamento vem "" (string vazia) quando não agendado, nunca ausente — por isso
// o fallback simples (`||`) já cobre o caso vazio sem checagem extra.
function resolveDataAgendamento(ocorrencia: SgpOcorrencia): string | undefined {
  if (ocorrencia.data_agendamento) return ocorrencia.data_agendamento;
  const osComData = ocorrencia.ordens_servicos?.find((os) => os.data_agendamento);
  return osComData?.data_agendamento || undefined;
}

// Verifica se o contrato selecionado no Estado 0c já tem ocorrência aberta no SGP, para
// bloquear a abertura de um chamado duplicado (issue #83). A checagem é por contrato, não
// por CPF: um CPF pode ter múltiplos contratos, só o selecionado importa.
export async function listarOcorrenciasAbertasSgp(
  baseUrl: string,
  token: string,
  app: string,
  contratoId: string,
  fetcher: typeof fetch = fetch,
): Promise<SgpOcorrenciaAbertaResult> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetcher(`${baseUrl}/api/ura/ocorrencia/list/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, app, contrato: parseInt(contratoId, 10) }),
      signal: controller.signal,
    });

    if (response.status >= 400) {
      throw new Error(`http_error_${response.status}`);
    }

    const data = (await response.json()) as { ocorrencias?: SgpOcorrencia[] };
    const aberta = (data.ocorrencias ?? []).find((o) => o.status === 'Aberta');

    if (!aberta) {
      return { temOcorrenciaAberta: false };
    }

    const dataAgendamento = resolveDataAgendamento(aberta);

    return {
      temOcorrenciaAberta: true,
      ...(aberta.numero !== undefined ? { protocolo: aberta.numero } : {}),
      ...(aberta.status !== undefined ? { status: aberta.status } : {}),
      ...(dataAgendamento !== undefined ? { dataAgendamento } : {}),
    };
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('timeout');
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
}

export type SgpTituloAberto = {
  id: number;
  valor: number;
  dataVencimento: string;
  linhaDigitavel?: string;
  codigoPix?: string;
  linkBoleto?: string;
};

type SgpTituloRaw = {
  id?: number;
  status?: string;
  valor?: number;
  valorCorrigido?: number;
  dataVencimento?: string;
  linhaDigitavel?: string;
  codigoPix?: string;
  link?: string;
  link_cobranca?: string;
};

// Lista os títulos em aberto do contrato (issue #101) via POST /api/ura/titulos/ — shape validado
// ao vivo no sandbox (api-reference.md §1.7). Campos de pagamento vêm como string vazia quando o
// SGP não fornece (ex.: codigoPix "" no sandbox) — normalizados para ausentes, nunca inventados.
// O valor exposto é o corrigido (com juros/multa) quando o SGP o calcula — é o que o assinante
// paga de fato hoje; senão, o valor nominal.
export async function listarTitulosSgp(
  baseUrl: string,
  token: string,
  app: string,
  contratoId: string,
  fetcher: typeof fetch = fetch,
): Promise<SgpTituloAberto[]> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetcher(`${baseUrl}/api/ura/titulos/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, app, contrato: parseInt(contratoId, 10) }),
      signal: controller.signal,
    });

    if (response.status >= 400) {
      throw new Error(`http_error_${response.status}`);
    }

    const data = (await response.json()) as { titulos?: SgpTituloRaw[] };

    return (data.titulos ?? [])
      .filter((t) => t.status === 'aberto')
      .map((t) => {
        const linkBoleto = t.link || t.link_cobranca || undefined;
        return {
          id: t.id ?? 0,
          valor: t.valorCorrigido && t.valorCorrigido > 0 ? t.valorCorrigido : t.valor ?? 0,
          dataVencimento: t.dataVencimento ?? '',
          ...(t.linhaDigitavel ? { linhaDigitavel: t.linhaDigitavel } : {}),
          ...(t.codigoPix ? { codigoPix: t.codigoPix } : {}),
          ...(linkBoleto ? { linkBoleto } : {}),
        };
      })
      .sort((a, b) => a.dataVencimento.localeCompare(b.dataVencimento));
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('timeout');
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
}

export type SgpLiberacaoResult = {
  liberado: boolean;
  sgpStatus: number;
  dataPromessa?: string;
  protocolo?: string;
};

type SgpLiberacaoResponse = {
  status?: number;
  liberado?: boolean;
  data_promessa?: string;
  protocolo?: string;
  msg?: string;
};

// Religa de confiança via promessa de pagamento (issue #99) — POST /api/ura/liberacaopromessa/,
// validado ao vivo no sandbox (api-reference.md §1.8). Qualquer status !== 1 é "não liberado":
// a distinção entre 0 (contrato inexistente/sem promessa aplicável) e 2 (limite de promessas do
// ciclo atingido) é só observabilidade — sgpStatus vai no retorno para o chamador logar, sem mudar
// o comportamento do produto. Sem data_promessa customizada: a data do compromisso é decidida
// pelas variáveis do próprio SGP do ISP e volta na resposta.
export async function liberarPromessaSgp(
  baseUrl: string,
  token: string,
  app: string,
  contratoId: string,
  fetcher: typeof fetch = fetch,
): Promise<SgpLiberacaoResult> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetcher(`${baseUrl}/api/ura/liberacaopromessa/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, app, contrato: parseInt(contratoId, 10) }),
      signal: controller.signal,
    });

    if (response.status >= 400) {
      throw new Error(`http_error_${response.status}`);
    }

    const data = (await response.json()) as SgpLiberacaoResponse;
    const sgpStatus = data.status ?? 0;

    if (sgpStatus !== 1) {
      return { liberado: false, sgpStatus };
    }

    return {
      liberado: true,
      sgpStatus,
      ...(data.data_promessa ? { dataPromessa: data.data_promessa } : {}),
      ...(data.protocolo ? { protocolo: data.protocolo } : {}),
    };
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('timeout');
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
}

// Campos de atualização de OS aceitos pelo SGP (coleção Postman, endpoint
// `POST /api/os/update/id/{os_id}/`). Não inclui data/hora de agendamento — validado ao vivo
// (issue #88): o SGP aceita `data_hora_agendamento` sem erro (`msg: "OS alterada com sucesso"`),
// mas o campo não persiste (`os_data_agendamento` continua null após a chamada). Não expor esse
// campo aqui até o SGP suportar de fato — evita a ilusão de que o adapter agenda a OS.
export type SgpOsAtualizarCampos = {
  os_servicoprestado?: string;
  os_observacao?: string;
  os_data_alteracao?: string;
  os_data_finalizacao?: string;
  checkin_data?: string;
  checkin_latitude?: string;
  checkin_longitude?: string;
  assinatura_cliente?: string;
  assinatura_tecnico?: string;
  assinatura_contrato?: string;
  os_status?: 0 | 1 | 2 | 3;
  classificacao_adicionar?: string;
  classificacao_remover?: string;
};

type SgpOsAtualizarResponse = {
  msg?: string;
  os_id?: number;
};

// Atualiza uma OS existente no SGP (issue #20). `{os_id}` vai na URL — o SGP não valida contra
// o token/app se a OS pertence a um contrato do tenant, então o chamador é responsável por só
// atualizar OS que o próprio adapter abriu/consultou.
export async function atualizarOsSgp(
  baseUrl: string,
  token: string,
  app: string,
  osId: string,
  campos: SgpOsAtualizarCampos,
  fetcher: typeof fetch = fetch,
): Promise<{ msg: string; os_id: number }> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetcher(`${baseUrl}/api/os/update/id/${osId}/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, app, ...campos }),
      signal: controller.signal,
    });

    if (response.status >= 400) {
      throw new Error(`http_error_${response.status}`);
    }

    const data = (await response.json()) as SgpOsAtualizarResponse;

    if (data.os_id === undefined) {
      throw new Error('sem_os_id');
    }

    return { msg: data.msg ?? '', os_id: data.os_id };
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('timeout');
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
}

const TURNO_LABELS: Record<NonNullable<ChamadoCanônico['turno_preferido']>, string> = {
  manha: 'Manhã',
  tarde: 'Tarde',
  qualquer: 'Qualquer horário',
};

export async function abrirChamadoSgp(
  baseUrl: string,
  token: string,
  app: string,
  chamado: ChamadoCanônico,
  fetcher: typeof fetch = fetch,
): Promise<string> {
  const dl = chamado.metricas.download_mbps ?? 'N/D';
  const lat = chamado.metricas.latencia_ms ?? 'N/D';

  // Turno preferido de visita (issue #106) — não existe campo dedicado documentado na API do SGP
  // para isso, então vai como texto livre em `conteudo`, junto do resumo do diagnóstico.
  const turnoTexto = chamado.turno_preferido
    ? ` Turno preferido para visita técnica: ${TURNO_LABELS[chamado.turno_preferido]}.`
    : '';

  // POP do contrato (issue #103) — mesmo padrão do turno: sem campo dedicado no SGP para isso,
  // vai como texto livre em `conteudo`. Omitido quando o SGP não retornou popNome no lookup.
  const popTexto = chamado.pop_nome ? ` POP: ${chamado.pop_nome}.` : '';

  const payload: SgpChamadoPayload = {
    token,
    app,
    contrato: parseInt(chamado.assinante_ref, 10),
    conteudo: `Diagnóstico automático: ${chamado.diagnostico_resumo}. Download: ${dl} Mbps. Latência: ${lat} ms.${turnoTexto}${popTexto}`,
    observacao: `Chamado aberto automaticamente pelo Técnico Virtual. ID: ${chamado.diagnostico_id}`,
    contato_nome: chamado.assinante_nome,
  };

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10_000);

  try {
    const response = await fetcher(`${baseUrl}/api/ura/chamado/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });

    if (response.status >= 400) {
      throw new Error(`http_error_${response.status}`);
    }

    const data = (await response.json()) as SgpChamadoResponse;

    if (data.status !== 1) {
      throw new Error(`sgp_status_${data.status}`);
    }

    if (!data.protocolo) {
      throw new Error('sem_protocolo');
    }

    return data.protocolo;
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('timeout');
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }
}
