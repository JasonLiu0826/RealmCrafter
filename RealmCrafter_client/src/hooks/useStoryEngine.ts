import { useCallback, useState } from 'react'
import { streamSSE } from '../services/StreamParser'

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export interface GenerateStreamParams {
  storyId: string
  userChoice?: string
  chaosLevel?: number
  useByok?: boolean
  userId?: number | string
}

/**
 * 封装章节流式生成与 StreamParser 逻辑，对外只暴露 textContent 与 isGenerating。
 * 供阅读器“打字机”等组件消费，不包含 UI 布局。
 */
export function useStoryEngine() {
  const [textContent, setTextContent] = useState('')
  const [isGenerating, setIsGenerating] = useState(false)
  const [branches, setBranches] = useState<string[]>([])
  const [error, setError] = useState<Error | null>(null)

  const generate = useCallback(
    async (params: GenerateStreamParams) => {
      const { storyId, userChoice = '', chaosLevel = 0.7, useByok = false, userId } = params
      setError(null)
      setBranches([])
      setTextContent('')
      setIsGenerating(true)

      const url = `${BASE_URL}/api/v1/engine/generate/stream`
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      }
      if (userId !== undefined) {
        headers['X-User-Id'] = String(userId)
      }
      const token = window.localStorage.getItem('token')
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }

      try {
        await streamSSE(
          url,
          {
            method: 'POST',
            headers,
            body: JSON.stringify({
              storyId,
              userChoice,
              chaosLevel,
              useByok,
            }),
          },
          {
            onContentUpdate: (chunk) => {
              setTextContent((prev) => prev + chunk)
            },
            onBranchesReady: (b) => {
              setBranches(b)
            },
            onDone: () => {
              setIsGenerating(false)
            },
            onError: (err) => {
              setError(err)
              setIsGenerating(false)
            },
          }
        )
      } catch (e) {
        setError(e instanceof Error ? e : new Error(String(e)))
        setIsGenerating(false)
      }
    },
    []
  )

  const reset = useCallback(() => {
    setTextContent('')
    setBranches([])
    setError(null)
    setIsGenerating(false)
  }, [])

  return {
    textContent,
    isGenerating,
    branches,
    error,
    generate,
    reset,
  }
}
