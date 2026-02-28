import { useCallback, useEffect, useRef, useState } from 'react'
import { streamSSE } from '../services/StreamParser'

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const TYPEWRITER_INTERVAL_MS = 40

export interface GenerateStreamParams {
  storyId: string
  userChoice?: string
  chaosLevel?: number
  useByok?: boolean
  userId?: number | string
}

/**
 * 封装章节流式生成与打字机队列：textContent 为 SSE 物理流，displayedContent 为视觉流。
 * 仅当 !isGenerating && !isTyping && branches.length === 3 时 showBranches 为 true，选项卡才渲染。
 */
export function useStoryEngine() {
  const [textContent, setTextContent] = useState('')
  const [displayedLength, setDisplayedLength] = useState(0)
  const [isGenerating, setIsGenerating] = useState(false)
  const [branches, setBranches] = useState<string[]>([])
  const [error, setError] = useState<Error | null>(null)

  const targetContentRef = useRef(textContent)
  targetContentRef.current = textContent
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const displayedContent = textContent.slice(0, displayedLength)
  const isTyping = displayedLength < textContent.length
  const showBranches = !isGenerating && !isTyping && branches.length === 3

  useEffect(() => {
    if (displayedLength >= textContent.length) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      return
    }
    if (intervalRef.current) return
    intervalRef.current = setInterval(() => {
      setDisplayedLength((prev) => {
        const target = targetContentRef.current.length
        if (prev >= target) {
          if (intervalRef.current) {
            clearInterval(intervalRef.current)
            intervalRef.current = null
          }
          return target
        }
        return prev + 1
      })
    }, TYPEWRITER_INTERVAL_MS)
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
    }
  }, [textContent, displayedLength])

  const generate = useCallback(
    async (params: GenerateStreamParams) => {
      const { storyId, userChoice = '', chaosLevel = 0.7, useByok = false, userId } = params
      setError(null)
      setBranches([])
      setTextContent('')
      setDisplayedLength(0)
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
    setDisplayedLength(0)
    setBranches([])
    setError(null)
    setIsGenerating(false)
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
  }, [])

  return {
    content: displayedContent,
    branches,
    showBranches,
    isGenerating,
    isTyping,
    generate,
    reset,
    error,
  }
}
