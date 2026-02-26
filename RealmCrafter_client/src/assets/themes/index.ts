export type ThemeId =
  | 'classic_white'
  | 'dark_night'
  | 'paper'
  | 'sakura_pink'
  | 'cyber_purple'
  | 'hacker_green'
  | 'tech_blue'
  | 'rose_red'
  | 'sunset_orange'
  | 'cyber_gradient'

export interface ThemeDefinition {
  id: ThemeId
  name: string
  isVIP: boolean
}

export const themes: Record<ThemeId, ThemeDefinition> = {
  classic_white: {
    id: 'classic_white',
    name: '经典白',
    isVIP: false,
  },
  dark_night: {
    id: 'dark_night',
    name: '极致暗黑',
    isVIP: false,
  },
  paper: {
    id: 'paper',
    name: '护眼纸张',
    isVIP: false,
  },
  sakura_pink: {
    id: 'sakura_pink',
    name: '樱花粉',
    isVIP: true,
  },
  cyber_purple: {
    id: 'cyber_purple',
    name: '酷炫紫',
    isVIP: true,
  },
  hacker_green: {
    id: 'hacker_green',
    name: '黑客绿',
    isVIP: true,
  },
  tech_blue: {
    id: 'tech_blue',
    name: '科技蓝',
    isVIP: true,
  },
  rose_red: {
    id: 'rose_red',
    name: '玫瑰红',
    isVIP: true,
  },
  sunset_orange: {
    id: 'sunset_orange',
    name: '日落橙',
    isVIP: true,
  },
  cyber_gradient: {
    id: 'cyber_gradient',
    name: '赛博炫彩',
    isVIP: true,
  },
}

export const DEFAULT_THEME_ID: ThemeId = 'classic_white'

export const VIP_THEME_IDS: ThemeId[] = Object.values(themes)
  .filter((t) => t.isVIP)
  .map((t) => t.id)

