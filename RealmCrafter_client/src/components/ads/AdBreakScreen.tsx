import { useEffect } from 'react'

export interface AdBreakScreenProps {
  /** 是否显示插屏 */
  visible: boolean
  /** 用户关闭/跳过（可选，部分策略下不允许跳过） */
  onClose?: () => void
  /** 用户看完广告后回调，用于通知后端发放奖励并继续流程 */
  onWatchComplete: () => void
  /** 是否允许跳过（未看广告时） */
  skippable?: boolean
}

/**
 * 灵能补给插屏：每 10 章被动触发（BYOK/普通用户），
 * 占位实现；生产环境接入 AdMob/Pangle/Unity 等 SDK 或 iframe。
 */
export function AdBreakScreen({
  visible,
  onClose,
  onWatchComplete,
  skippable = false,
}: AdBreakScreenProps) {
  useEffect(() => {
    if (!visible) return
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && skippable) onClose?.()
    }
    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [visible, skippable, onClose])

  if (!visible) return null

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/80 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="ad-break-title"
    >
      <div className="mx-4 flex max-w-md flex-col items-center gap-6 rounded-2xl border border-white/20 bg-white/10 p-8 shadow-2xl backdrop-blur-md">
        <h2
          id="ad-break-title"
          className="text-xl font-semibold text-white"
        >
          灵能补给
        </h2>
        <p className="text-center text-sm text-white/80">
          观看一段激励视频，即可继续生成章节
        </p>
        {/* 占位：实际接入时替换为广告 SDK 容器 */}
        <div className="flex h-48 w-full items-center justify-center rounded-xl bg-black/40 text-white/60">
          [ 广告位：激励视频 ]
        </div>
        <div className="flex w-full gap-3">
          {skippable && onClose && (
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-lg border border-white/30 bg-white/10 py-2 text-sm text-white/80 hover:bg-white/20"
            >
              稍后再说
            </button>
          )}
          <button
            type="button"
            onClick={onWatchComplete}
            className="flex-1 rounded-lg bg-[var(--color-primary,#6366f1)] py-2 text-sm font-medium text-white hover:opacity-90"
          >
            我已看完，继续
          </button>
        </div>
      </div>
    </div>
  )
}
