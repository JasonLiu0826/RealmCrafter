import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

/** 后端服务 baseURL，开发环境指向本地 8080 端口 */
const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/**
 * 封装 Axios 实例，用于后端 RESTful 请求
 * - 统一 baseURL
 * - 可在此扩展：请求/响应拦截器、Token 无感刷新等
 */
const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 请求拦截器：可在此注入 Token、设备信息等 */
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 后续可从 store 或 localStorage 读取 token 并写入 Authorization
    // const token = getToken()
    // if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器：统一错误处理、Token 刷新等 */
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 可在此做 401 跳转登录、403 提示、网络错误提示等
    return Promise.reject(error)
  }
)

export default api
