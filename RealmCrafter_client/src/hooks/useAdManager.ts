import { useEffect, useState } from 'react'
import {
  useAdTriggerStore,
  type AdTriggerState,
} from '../store/useAdTriggerStore'
import { resumePendingRequests } from '../services/api'

export interface UseAdManagerOptions {
  /** 插屏是否允许跳过（未观看时） */
  adBreakSkippable?: boolean
}

export interface UseAdManagerResult {
  /** 是否应显示灵能补给插屏（由后端 AD_TRIGGER 触发） */
  showAdBreak: boolean
  /** 用户看完广告后调用，用于重置状态并通知后端发放奖励 */
  onAdComplete: () => void
  /** 用户跳过/关闭插屏时调用，仅重置状态不发放奖励 */
  onAdDismiss: () => void
  /** 当前触发原因，便于埋点或 UI 文案 */
  triggerReason: string | undefined
}

/**
 * 拦截后端 AD_TRIGGER 信号并调起广告组件。
 * - 监听 useAdTriggerStore：当 api 拦截器将 adTriggered 置为 true 时展示插屏。
 * - 与 AdBreakScreen 配合：showAdBreak 为 true 时渲染插屏，onAdComplete 由插屏回调。
 */
export function useAdManager(
  _options: UseAdManagerOptions = {}
): UseAdManagerResult {
  const adTriggered = useAdTriggerStore(
    (s: AdTriggerState) => s.adTriggered
  )
  const triggerReason = useAdTriggerStore(
    (s: AdTriggerState) => s.triggerReason
  )
  const setAdTriggered = useAdTriggerStore(
    (s: AdTriggerState) => s.setAdTriggered
  )

  const [showAdBreak, setShowAdBreak] = useState(false)

  useEffect(() => {
    if (adTriggered) {
      setShowAdBreak(true)
    }
  }, [adTriggered])

  const onAdComplete = () => {
    setShowAdBreak(false)
    setAdTriggered(false)
    // 广告结束：恢复挂起的请求队列（重新发起被 451 中断的请求）
    resumePendingRequests()
  }

  const onAdDismiss = () => {
    setShowAdBreak(false)
    setAdTriggered(false)
    // 广告被关闭：同样尝试恢复挂起的请求，避免长期悬挂
    resumePendingRequests()
  }

  return {
    showAdBreak,
    onAdComplete,
    onAdDismiss,
    triggerReason,
  }
}
