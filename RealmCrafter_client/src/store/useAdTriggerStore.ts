import { create } from 'zustand'

/**
 * 广告触发状态：后端返回 AD_TRIGGER（如 HTTP 451）时置为 true，
 * useAdManager 监听此状态并调起插屏/激励视频组件。
 */
export interface AdTriggerState {
  /** 是否因满 N 章触发灵能补给插屏 */
  adTriggered: boolean
  /** 可选：触发原因，用于埋点或 UI 文案 */
  triggerReason?: string
  setAdTriggered: (triggered: boolean, reason?: string) => void
}

export const AD_TRIGGER_STATUS_CODE = 451

type SetState = (
  partial:
    | Partial<AdTriggerState>
    | ((state: AdTriggerState) => Partial<AdTriggerState>)
) => void

export const useAdTriggerStore = create<AdTriggerState>()((set: SetState) => ({
  adTriggered: false,
  triggerReason: undefined,

  setAdTriggered: (triggered: boolean, reason?: string) =>
    set({ adTriggered: triggered, triggerReason: reason }),
}))
