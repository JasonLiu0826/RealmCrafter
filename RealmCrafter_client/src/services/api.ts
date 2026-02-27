import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from 'axios'
import { useAdTriggerStore, AD_TRIGGER_STATUS_CODE } from '../store/useAdTriggerStore'

/** 后端服务 baseURL，开发环境指向本地 8080 端口 */
const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/**
 * 封装 Axios 实例，用于后端 RESTful 请求
 * - 统一 baseURL
 * - 可在此扩展：请求/响应拦截器、Token 无感刷新等
 * - 全局拦截 AD_TRIGGER 状态码（451），调起前端广告组件
 */
const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 挂起中的请求队列：当出现 AD_TRIGGER（451）时，将原请求的重试逻辑放入队列 */
let pendingRequests: Array<() => void> = []
/** 全局标记当前是否正在播放广告，避免重复拉起插屏 */
let isAdPlaying = false

/** 请求拦截器：可在此注入 Token、设备信息等 */
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 简单示例：从 localStorage 读取 JWT 并写入 Authorization 头
    const token = window.localStorage.getItem('token')
    if (token) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器：统一错误处理、AD_TRIGGER 派发、Token 刷新等 */
api.interceptors.response.use(
  (response: AxiosResponse) => {
    // 默认情况下直接返回 data，保持调用端简洁
    return response.data
  },
  (error) => {
    const status = error.response?.status

    if (status === AD_TRIGGER_STATUS_CODE) {
      const originalConfig = error.config
      if (!originalConfig) {
        return Promise.reject(error)
      }

      const { setAdTriggered } = useAdTriggerStore.getState()

      // 首次进入 AD_TRIGGER 时拉起广告
      if (!isAdPlaying) {
        isAdPlaying = true
        setAdTriggered(true, 'ad_break')
      }

      // 将原请求包装成重试函数放入挂起队列，并返回一个挂起的 Promise
      return new Promise((resolve, reject) => {
        pendingRequests.push(() => {
          api(originalConfig)
            .then(resolve)
            .catch(reject)
        })
      })
    }

    return Promise.reject(error)
  }
)

/**
 * 在广告结束或关闭时由上层调用：
 * - 依次执行所有挂起的请求重试函数
 * - 清空队列并重置 isAdPlaying 状态
 */
export function resumePendingRequests() {
  const queue = pendingRequests
  pendingRequests = []
  isAdPlaying = false

  queue.forEach((cb) => {
    try {
      cb()
    } catch {
      // 忽略单个请求重试过程中的异常，由各自的调用方处理
    }
  })
}

export default api
