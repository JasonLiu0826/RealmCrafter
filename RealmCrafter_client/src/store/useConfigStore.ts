import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/** 混沌阈值（Temperature）范围 */
export const CHAOS_LEVEL_MIN = 0.1
export const CHAOS_LEVEL_MAX = 1.0
export const CHAOS_LEVEL_DEFAULT = 0.7

export interface ConfigState {
  /** 混沌阈值，对应 LLM Temperature */
  chaosLevel: number
  /** 首选模型名称 */
  preferredModel: string
  /** 是否开启极客直连 BYOK */
  isByok: boolean
  setChaosLevel: (value: number) => void
  setPreferredModel: (model: string) => void
  setIsByok: (enabled: boolean) => void
  /** 从服务端/本地恢复后同步到 store（不落盘） */
  setConfig: (config: { chaosLevel?: number; preferredModel?: string; isByok?: boolean }) => void
}

const STORAGE_KEY = 'realmcrafter:config'

export const useConfigStore = create<ConfigState>()(
  persist(
    (set) => ({
      chaosLevel: CHAOS_LEVEL_DEFAULT,
      preferredModel: 'realm_crafter_v1',
      isByok: false,

      setChaosLevel: (value) => {
        const clamped = Math.max(CHAOS_LEVEL_MIN, Math.min(CHAOS_LEVEL_MAX, value))
        set({ chaosLevel: clamped })
      },

      setPreferredModel: (model) => set({ preferredModel: model || 'realm_crafter_v1' }),

      setIsByok: (enabled) => set({ isByok: enabled }),

      setConfig: (config) =>
        set((state) => {
          const next = { ...state }
          if (config.chaosLevel != null) {
            next.chaosLevel = Math.max(
              CHAOS_LEVEL_MIN,
              Math.min(CHAOS_LEVEL_MAX, config.chaosLevel)
            )
          }
          if (config.preferredModel != null) next.preferredModel = config.preferredModel
          if (config.isByok != null) next.isByok = config.isByok
          return next
        }),
    }),
    {
      name: STORAGE_KEY,
      partialize: (s) => ({
        chaosLevel: s.chaosLevel,
        preferredModel: s.preferredModel,
        isByok: s.isByok,
      }),
    }
  )
)
