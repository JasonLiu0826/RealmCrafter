import { create } from 'zustand'
import { DEFAULT_THEME_ID, ThemeId, themes } from '../assets/themes'

const THEME_STORAGE_KEY = 'realmcrafter:themeId'

export interface ThemeState {
  currentThemeId: ThemeId
  setTheme: (themeId: ThemeId) => void
  hydrate: () => void
}

function applyThemeToDocument(themeId: ThemeId) {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  root.setAttribute('data-theme', themeId)
}

export const useThemeStore = create<ThemeState>((set) => ({
  currentThemeId: DEFAULT_THEME_ID,

  setTheme: (themeId: ThemeId) => {
    const effectiveThemeId = themes[themeId] ? themeId : DEFAULT_THEME_ID
    set({ currentThemeId: effectiveThemeId })

    if (typeof window !== 'undefined') {
      window.localStorage.setItem(THEME_STORAGE_KEY, effectiveThemeId)
    }
    applyThemeToDocument(effectiveThemeId)
  },

  hydrate: () => {
    if (typeof window === 'undefined') return
    const saved = window.localStorage.getItem(THEME_STORAGE_KEY) as ThemeId | null
    const effectiveThemeId = saved && themes[saved] ? saved : DEFAULT_THEME_ID
    set({ currentThemeId: effectiveThemeId })
    applyThemeToDocument(effectiveThemeId)
  },
}))

