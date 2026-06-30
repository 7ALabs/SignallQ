export type QualityLevel = 'good' | 'fair' | 'poor' | 'unknown';

export interface SemanticColors {
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  background: string;
  surface: string;
  surfaceVariant: string;
  outline: string;
  error: string;
  success: string;
  warning: string;
  info: string;
  download: string;
  upload: string;
  latency: string;
  stability: string;
  diagnostic: string;
  quality: Record<QualityLevel, string>;
  onSurface: string;
  onSurfaceVariant: string;
}

export const lightColors: SemanticColors = {
  primary: '#6C2BFF',
  onPrimary: '#ffffff',
  primaryContainer: '#EFE7FF',
  background: '#F3F4F6',
  surface: '#ffffff',
  surfaceVariant: '#F9FAFB',
  outline: '#E5E7EB',
  error: '#FF4D4F',
  success: '#22C55E',
  warning: '#F5A623',
  info: '#2563EB',
  download: '#22C55E',
  upload: '#F5A623',
  latency: '#2563EB',
  stability: '#22C55E',
  diagnostic: '#6C2BFF',
  quality: {
    good: '#22C55E',
    fair: '#F5A623',
    poor: '#FF4D4F',
    unknown: '#6B7280',
  },
  onSurface: '#0D0D1A',
  onSurfaceVariant: '#6B7280',
};

export const darkColors: SemanticColors = {
  primary: '#6C2BFF',
  onPrimary: '#ffffff',
  primaryContainer: '#1A0B2E',
  background: '#000000',
  surface: '#111111',
  surfaceVariant: '#1A1A1A',
  outline: '#2A2A2A',
  error: '#FF4D4F',
  success: '#22C55E',
  warning: '#F5A623',
  info: '#2563EB',
  download: '#34D399',
  upload: '#FBBF24',
  latency: '#60A5FA',
  stability: '#34D399',
  diagnostic: '#6C2BFF',
  quality: {
    good: '#22C55E',
    fair: '#F5A623',
    poor: '#FF4D4F',
    unknown: '#9CA3AF',
  },
  onSurface: '#F3F4F6',
  onSurfaceVariant: '#9CA3AF',
};
