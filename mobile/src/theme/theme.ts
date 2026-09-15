// ─── Yeniden Design System ──────────────────────────────────────────────────
// Light green nature theme — matches yeniden-ui.html exactly

export const theme = {
  colors: {
    // Backgrounds
    bg:           '#EFF3EA',
    paper:        '#F8FAF6',
    surface:      '#FFFFFF',
    surfaceAlt:   '#E7ECE0',

    // Primary — deep forest green
    primary:      '#1E3A2B',
    primarySoft:  '#35573F',
    primaryTint:  '#DCE7DC',

    // Accent — amber/eco-coin gold
    accent:       '#B98A2E',
    accentTint:   '#F1E3C4',

    // Text hierarchy
    ink:          '#16241B',
    inkSoft:      '#57685B',
    inkFaint:     '#8A9A8C',

    // Borders
    line:         'rgba(22,36,27,0.10)',
    lineStrong:   'rgba(22,36,27,0.16)',

    // Auth screen (inverted — dark green bg)
    authBg:       '#1E3A2B',
    authBgDeep:   '#24422F',
    authPaper:    '#F8FAF6',

    // Status / semantic
    success:      '#2E7D32',
    danger:       '#C62828',
    warning:      '#E65100',

    white: '#FFFFFF',
    black: '#000000',
  },

  font: {
    // Fraunces is used for headings in the HTML — use Georgia as fallback
    serif:  'Georgia',
    sans:   'System',

    size: {
      xs:   11,
      sm:   12.5,
      base: 14,
      md:   16,
      lg:   18,
      xl:   22,
      '2xl': 26,
      '3xl': 32,
    },

    weight: {
      regular:  '400' as const,
      medium:   '500' as const,
      semibold: '600' as const,
      bold:     '700' as const,
      heavy:    '800' as const,
    },
  },

  space: {
    '1': 4,
    '2': 8,
    '3': 12,
    '4': 16,
    '5': 20,
    '6': 24,
    '8': 32,
    '10': 40,
    '12': 48,
    '16': 64,
  },

  radius: {
    sm:   10,
    md:   14,
    lg:   18,
    xl:   22,
    '2xl': 28,
    full: 999,
  },

  shadow: {
    sm: {
      shadowColor: '#16241B',
      shadowOffset: { width: 0, height: 3 },
      shadowOpacity: 0.08,
      shadowRadius: 8,
      elevation: 3,
    },
    md: {
      shadowColor: '#16241B',
      shadowOffset: { width: 0, height: 8 },
      shadowOpacity: 0.12,
      shadowRadius: 20,
      elevation: 6,
    },
    lg: {
      shadowColor: '#16241B',
      shadowOffset: { width: 0, height: 16 },
      shadowOpacity: 0.16,
      shadowRadius: 40,
      elevation: 10,
    },
  },
};

export type Theme = typeof theme;
