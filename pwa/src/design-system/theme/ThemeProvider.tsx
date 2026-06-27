import { createContext, CSSProperties, ReactNode, useContext, useMemo } from 'react';
import { darkTheme } from './darkTheme';
import { lightTheme } from './lightTheme';

type ThemeMode = 'light' | 'dark';
type Theme = typeof lightTheme | typeof darkTheme;
type CssVars = CSSProperties & Record<`--${string}`, string>;

const themes: Record<ThemeMode, Theme> = {
  light: lightTheme,
  dark: darkTheme,
};

const ThemeContext = createContext<Theme>(lightTheme);

interface ThemeProviderProps {
  children: ReactNode;
  mode?: ThemeMode;
}

function buildCssVariables(theme: Theme): CssVars {
  return {
    '--sq-color-primary': theme.colors.primary,
    '--sq-color-on-primary': theme.colors.onPrimary,
    '--sq-color-primary-container': theme.colors.primaryContainer,
    '--sq-color-background': theme.colors.background,
    '--sq-color-surface': theme.colors.surface,
    '--sq-color-surface-variant': theme.colors.surfaceVariant,
    '--sq-color-outline': theme.colors.outline,
    '--sq-color-error': theme.colors.error,
    '--sq-color-success': theme.colors.success,
    '--sq-color-warning': theme.colors.warning,
    '--sq-color-info': theme.colors.info,
    '--sq-color-download': theme.colors.download,
    '--sq-color-upload': theme.colors.upload,
    '--sq-color-latency': theme.colors.latency,
    '--sq-color-stability': theme.colors.stability,
    '--sq-color-diagnostic': theme.colors.diagnostic,
    '--sq-color-quality-good': theme.colors.quality.good,
    '--sq-color-quality-fair': theme.colors.quality.fair,
    '--sq-color-quality-poor': theme.colors.quality.poor,
    '--sq-color-quality-unknown': theme.colors.quality.unknown,
    '--sq-color-on-surface': theme.colors.onSurface,
    '--sq-color-on-surface-variant': theme.colors.onSurfaceVariant,
    '--sq-font-family': theme.typography.fontFamily,
    '--sq-font-size-label': theme.typography.sizes.label,
    '--sq-font-size-body': theme.typography.sizes.body,
    '--sq-font-size-body-large': theme.typography.sizes.bodyLarge,
    '--sq-font-size-title': theme.typography.sizes.title,
    '--sq-font-size-headline': theme.typography.sizes.headline,
    '--sq-font-size-display': theme.typography.sizes.display,
    '--sq-line-tight': theme.typography.lineHeights.tight,
    '--sq-line-title': theme.typography.lineHeights.title,
    '--sq-line-body': theme.typography.lineHeights.body,
    '--sq-weight-regular': theme.typography.weights.regular,
    '--sq-weight-medium': theme.typography.weights.medium,
    '--sq-weight-semibold': theme.typography.weights.semibold,
    '--sq-weight-bold': theme.typography.weights.bold,
    '--sq-space-xxs': theme.spacing.xxs,
    '--sq-space-xs': theme.spacing.xs,
    '--sq-space-sm': theme.spacing.sm,
    '--sq-space-md': theme.spacing.md,
    '--sq-space-lg': theme.spacing.lg,
    '--sq-space-xl': theme.spacing.xl,
    '--sq-space-xxl': theme.spacing.xxl,
    '--sq-space-section': theme.spacing.section,
    '--sq-radius-xs': theme.radius.xs,
    '--sq-radius-sm': theme.radius.sm,
    '--sq-radius-md': theme.radius.md,
    '--sq-radius-lg': theme.radius.lg,
    '--sq-radius-xl': theme.radius.xl,
    '--sq-radius-pill': theme.radius.pill,
    '--sq-elevation-1': theme.elevation.level1,
    '--sq-elevation-2': theme.elevation.level2,
    '--sq-elevation-3': theme.elevation.level3,
    '--sq-motion-fast': theme.motion.duration.fast,
    '--sq-motion-base': theme.motion.duration.base,
    '--sq-motion-slow': theme.motion.duration.slow,
    '--sq-motion-standard': theme.motion.easing.standard,
  };
}

export function ThemeProvider({ children, mode = 'light' }: ThemeProviderProps) {
  const theme = themes[mode];
  const variables = useMemo(() => buildCssVariables(theme), [theme]);

  return (
    <ThemeContext.Provider value={theme}>
      <div className="sq-theme" data-theme={mode} style={variables}>
        {children}
      </div>
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
