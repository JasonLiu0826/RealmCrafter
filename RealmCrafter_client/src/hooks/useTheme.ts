import { useMemo } from 'react'
import { ThemeId, themes } from '../assets/themes'
import { useThemeStore } from '../store/useThemeStore'

export function useTheme() {
  const currentThemeId = useThemeStore((state) => state.currentThemeId)
  const setTheme = useThemeStore((state) => state.setTheme)

  const currentTheme = useMemo(() => themes[currentThemeId], [currentThemeId])

  return {
    currentThemeId,
    currentTheme,
    setTheme: (themeId: ThemeId) => setTheme(themeId),
  }
}

