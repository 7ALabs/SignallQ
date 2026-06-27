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
  primary: '#2754c5',
  onPrimary: '#ffffff',
  primaryContainer: '#dbe6ff',
  background: '#f7f8fc',
  surface: '#ffffff',
  surfaceVariant: '#edf1f7',
  outline: '#d5dbe8',
  error: '#ba1a1a',
  success: '#176b43',
  warning: '#a45b00',
  info: '#006a80',
  download: '#2754c5',
  upload: '#7a4cc2',
  latency: '#006a80',
  stability: '#176b43',
  diagnostic: '#3d4964',
  quality: {
    good: '#176b43',
    fair: '#a45b00',
    poor: '#ba1a1a',
    unknown: '#647084',
  },
  onSurface: '#161b25',
  onSurfaceVariant: '#536070',
};

export const darkColors: SemanticColors = {
  primary: '#a9c7ff',
  onPrimary: '#0d2f73',
  primaryContainer: '#1b438f',
  background: '#10141c',
  surface: '#171c25',
  surfaceVariant: '#242b36',
  outline: '#444d5c',
  error: '#ffb4ab',
  success: '#8fd9ad',
  warning: '#ffbd76',
  info: '#8bd3e5',
  download: '#a9c7ff',
  upload: '#d5bbff',
  latency: '#8bd3e5',
  stability: '#8fd9ad',
  diagnostic: '#c7cede',
  quality: {
    good: '#8fd9ad',
    fair: '#ffbd76',
    poor: '#ffb4ab',
    unknown: '#b8c1d0',
  },
  onSurface: '#eef2fb',
  onSurfaceVariant: '#c3cad8',
};
