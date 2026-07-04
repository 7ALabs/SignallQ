import type { DiagnosisResult, QualityClassification, RecommendedAction, SpeedClassification, StabilityClassification } from '@shared/contracts';

// =============================================================================
// Contrato REAL da resposta do ai-diagnosis-worker (integrations/cloudflare/ai-diagnosis-worker)
// =============================================================================
// Este e o mesmo Worker consumido pelo Android (AiDiagnosisRepository.parseResult).
// O schema de saida e sempre este, independente do schemaVersion enviado no
// payload de entrada — o Worker sobrescreve schemaVersion/source/generatedAt/
// modeloIa pos-parse (ver ai-diagnosis-worker/src/index.ts). Nao existe um
// contrato "pwa-only" no Worker: PWA e Android compartilham a mesma saida.
//
// Parsing tolerante (mesma postura do AiDiagnosisRepository.kt): todos os
// campos sao opcionais e recebem defaults seguros quando ausentes/invalidos.

interface AiWorkerClassificacaoItemRaw {
  avaliacao?: string;
  justificativa?: string;
}

interface AiWorkerAcaoRaw {
  titulo?: string;
  descricao?: string;
  prioridade?: string;
  tipo?: string;
}

export interface AiWorkerResponseRaw {
  schemaVersion?: string;
  source?: string;
  generatedAt?: number;
  status?: string;
  titulo?: string;
  resumo?: string;
  textoLaudo?: string;
  problemaPrincipal?: {
    tipo?: string;
    descricao?: string;
    confianca?: number;
  };
  classificacaoTecnica?: {
    velocidade?: AiWorkerClassificacaoItemRaw;
    estabilidade?: AiWorkerClassificacaoItemRaw;
  };
  acoesRecomendadas?: AiWorkerAcaoRaw[];
  limitesDaAnalise?: string[];
}

/** Distingue uma resposta 200 válida do Worker de um corpo inesperado
 *  (ex.: proxy quebrado, JSON vazio). Não valida todos os campos — o Worker
 *  é tolerante e a IA pode omitir blocos inteiros; `status` é o único campo
 *  que o Worker sempre tenta preencher (default "inconclusivo" no pior caso). */
export function isAiWorkerResponse(value: unknown): value is AiWorkerResponseRaw {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Record<string, unknown>;
  return typeof candidate.status === 'string' || typeof candidate.titulo === 'string' || typeof candidate.resumo === 'string';
}

function mapStatusToQuality(status: string | undefined): QualityClassification {
  switch ((status ?? '').toLowerCase()) {
    case 'excelente':
    case 'bom':
      return 'good';
    case 'regular':
      return 'attention';
    case 'ruim':
    case 'critico':
      return 'bad';
    default:
      return 'unknown';
  }
}

function mapVelocidadeToSpeed(avaliacao: string | undefined): SpeedClassification {
  switch ((avaliacao ?? '').toLowerCase()) {
    case 'boa':
      return 'fast';
    case 'regular':
      return 'ok';
    case 'ruim':
      return 'slow';
    default:
      return 'unknown';
  }
}

function mapEstabilidadeToStability(avaliacao: string | undefined): StabilityClassification {
  switch ((avaliacao ?? '').toLowerCase()) {
    case 'boa':
      return 'stable';
    case 'regular':
    case 'ruim':
      return 'unstable';
    default:
      return 'unknown';
  }
}

function mapTipoToCategory(tipo: string | undefined): RecommendedAction['category'] {
  switch ((tipo ?? '').toLowerCase()) {
    case 'ajuste_roteador':
      return 'router';
    case 'validacao_local':
      return 'wifi';
    case 'ajuste_dispositivo':
      return 'device';
    case 'contato_isp':
      return 'provider';
    case 'reteste':
      return 'retry';
    default:
      return 'unknown';
  }
}

function mapPrioridadeToPriority(prioridade: string | undefined): RecommendedAction['priority'] {
  switch ((prioridade ?? '').toLowerCase()) {
    case 'alta':
      return 1;
    case 'baixa':
      return 3;
    default:
      return 2;
  }
}

function mapConfiancaToConfidence(confianca: number | undefined): DiagnosisResult['confidence'] {
  if (typeof confianca !== 'number') return 'medium';
  if (confianca >= 0.75) return 'high';
  if (confianca >= 0.5) return 'medium';
  return 'low';
}

/** Traduz a resposta real do Worker (schema compartilhado com o Android) para
 *  o contrato `DiagnosisResult` que o PWA usa na UI. */
export function mapAiWorkerResponseToDiagnosis(raw: AiWorkerResponseRaw): DiagnosisResult {
  const actions: RecommendedAction[] = (raw.acoesRecomendadas ?? [])
    .filter((acao) => (acao.titulo ?? '').trim().length > 0)
    .map((acao) => ({
      category: mapTipoToCategory(acao.tipo),
      description: acao.descricao ?? '',
      priority: mapPrioridadeToPriority(acao.prioridade),
      title: acao.titulo ?? '',
    }));

  const limitations = (raw.limitesDaAnalise ?? []).map((message, index) => ({
    code: `ai_limitation_${index}`,
    message,
  }));

  const generatedAt = typeof raw.generatedAt === 'number' ? new Date(raw.generatedAt).toISOString() : new Date().toISOString();

  return {
    actions,
    confidence: mapConfiancaToConfidence(raw.problemaPrincipal?.confianca),
    generatedAt,
    id: `diag_ai_${Date.now().toString(36)}`,
    limitations,
    quality: mapStatusToQuality(raw.status),
    source: 'ai',
    speed: mapVelocidadeToSpeed(raw.classificacaoTecnica?.velocidade?.avaliacao),
    stability: mapEstabilidadeToStability(raw.classificacaoTecnica?.estabilidade?.avaliacao),
    summary: raw.resumo?.trim() || raw.textoLaudo?.trim() || raw.titulo?.trim() || 'Diagnóstico gerado pela IA.',
  };
}
