import { lightColors } from '../tokens/colors';
import { elevation } from '../tokens/elevation';
import { motion } from '../tokens/motion';
import { radius } from '../tokens/radius';
import { spacing } from '../tokens/spacing';
import { typography } from '../tokens/typography';

export const lightTheme = {
  name: 'light',
  colors: lightColors,
  elevation,
  motion,
  radius,
  spacing,
  typography,
} as const;
