import api from './api'

/** 章节生成请求体示例，可根据实际业务扩展字段 */
export interface GenerateChapterPayload {
  storyId: string
  prompt: string
  // 其他业务字段，如 temperature、language 等，后续可按需补充
  [key: string]: unknown
}

/**
 * 自定义错误类型：用于在 UI 层识别“高维数据乱流”并展示专属提示文案。
 */
export class DataTurbulenceError extends Error {
  readonly code = 403

  constructor(message = '检测到高维数据乱流，连接已熔断') {
    super(message)
    this.name = 'DataTurbulenceError'
  }
}

/**
 * 生成章节完整流程（示例）：
 * 1. 先调用 /api/v1/heartbeat/chapter-generate 完成计费与广告触发检查；
 *    - 若后端返回 451，则会被 api.ts 拦截器挂起，等广告播放结束自动恢复；
 * 2. 心跳成功 resolve 后，再调用 /api/v1/chapter/generate 进行正式生成；
 * 3. 若任一步骤返回 403（高维数据乱流），抛出 DataTurbulenceError 供 UI 展示提示并中止生成。
 */
export async function generateChapterProcess(
  payload: GenerateChapterPayload
): Promise<unknown> {
  // 第一步：心跳 & 计费 & 广告触发检查
  try {
    await api.post('/api/v1/heartbeat/chapter-generate', {})
  } catch (error: any) {
    const status = error?.response?.status
    const code = error?.response?.data?.code
    if (status === 403 || code === 403) {
      throw new DataTurbulenceError()
    }
    throw error
  }

  // 第二步：正式章节生成（可根据后端实现改为流式接口）
  try {
    const result = await api.post('/api/v1/chapter/generate', payload)
    return result
  } catch (error: any) {
    const status = error?.response?.status
    const code = error?.response?.data?.code
    if (status === 403 || code === 403) {
      throw new DataTurbulenceError()
    }
    throw error
  }
}

