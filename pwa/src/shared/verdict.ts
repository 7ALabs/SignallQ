import type { QualityClassification } from '@shared/contracts';
import type { QualityLevel } from '@/design-system/types';
import type { StatusVerdict } from '@/design-system/components/StatusCard';

export function verdictFromQuality(quality: QualityClassification | undefined): StatusVerdict {
  switch (quality) {
    case 'good':
      return 'good';
    case 'attention':
      return 'attention';
    case 'bad':
      return 'bad';
    default:
      return 'unknown';
  }
}

export function qualityLevelFromQuality(quality: QualityClassification | undefined): QualityLevel {
  switch (quality) {
    case 'good':
      return 'good';
    case 'attention':
      return 'fair';
    case 'bad':
      return 'poor';
    default:
      return 'unknown';
  }
}

export function qualityLabel(quality: QualityClassification | undefined): string {
  switch (quality) {
    case 'good':
      return 'Bom';
    case 'attention':
      return 'Atenção';
    case 'bad':
      return 'Ruim';
    default:
      return 'Inconclusivo';
  }
}

export function statusTitle(quality: QualityClassification | undefined): string {
  switch (quality) {
    case 'good':
      return 'Conexão boa';
    case 'attention':
      return 'Conexão com atenção';
    case 'bad':
      return 'Conexão ruim';
    default:
      return 'Diagnóstico inconclusivo';
  }
}

export function stabilityLabel(stability: string): string {
  switch (stability) {
    case 'stable':
      return 'Estável';
    case 'unstable':
      return 'Instável';
    default:
      return 'Não medida';
  }
}

export function metricVerdict(value: number | null, warn: number, critical: number, inverse: boolean): { color: string; label: string } {
  if (value == null) return { color: 'var(--text-secondary)', label: 'não medida' };
  const bad = inverse ? value >= critical : value <= critical;
  const okish = inverse ? value >= warn : value <= warn;
  if (bad) return { color: 'var(--error)', label: 'Fraca' };
  if (!okish) return { color: 'var(--warning)', label: 'Atenção' };
  return { color: 'var(--success)', label: 'Boa' };
}
