import { useEffect } from 'react'
import { themes } from './assets/themes'
import { useTheme } from './hooks/useTheme'
import { useThemeStore } from './store/useThemeStore'

function App() {
  const hydrate = useThemeStore((state) => state.hydrate)
  const { currentThemeId, currentTheme, setTheme } = useTheme()

  useEffect(() => {
    hydrate()
  }, [hydrate])

  return (
    <div
      className="min-h-screen flex items-center justify-center transition-colors duration-300"
      style={{ background: 'var(--color-bg)' }}
    >
      <div className="flex flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold text-white vip-breathing">
          RealmCrafter
        </h1>
        <p className="text-sm text-white/80">
          当前主题：{currentTheme.name} ({currentThemeId})
        </p>
        <div className="flex flex-wrap justify-center gap-2 max-w-md">
          {Object.values(themes).map((theme) => (
            <button
              key={theme.id}
              type="button"
              onClick={() => setTheme(theme.id)}
              className={`px-3 py-1 rounded-full text-xs border transition-all ${
                currentThemeId === theme.id
                  ? 'bg-[var(--color-primary)] text-white border-transparent'
                  : 'bg-black/40 text-white/80 border-white/20 hover:border-white/60'
              }`}
            >
              {theme.name}
              {theme.isVIP && ' · VIP'}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

export default App
