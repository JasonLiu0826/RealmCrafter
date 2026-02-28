import api from './api'

export interface Story {
  id: string
  userId: number
  settingPackId: string
  title: string
  cover?: string
  description?: string
  price: number
  versionId: number
  deviceHash?: string
  createTime: string
  updateTime: string
}

export interface CreateStoryPayload {
  userId: number
  settingPackId: string
  title: string
  cover?: string
  description?: string
}

export interface RenameStoryPayload {
  userId: number
  title: string
}

export async function fetchStories() {
  return api.get<{ code: number; message: string; data: Story[] }>(
    '/api/v1/stories',
  )
}

export async function createStory(payload: CreateStoryPayload) {
  return api.post<{ code: number; message: string; data: Story }>(
    '/api/v1/stories',
    payload,
  )
}

export async function renameStory(id: string, payload: RenameStoryPayload) {
  return api.patch<{ code: number; message: string; data: Story }>(
    `/api/v1/stories/${id}/rename`,
    payload,
  )
}

/**
 * 获取单条故事（用于冲突时拉取服务端版本）。
 * 若后端暂无 GET /stories/:id，可传 getServerStory 或通过 fetchStories 查找。
 */
export async function getStory(id: string): Promise<Story> {
  const res = await api.get<{ code: number; message: string; data: Story }>(
    `/api/v1/stories/${id}`,
  )
  return (res as unknown as { data: Story }).data
}

/** 409 冲突时 reject 的形态，供 UI 层“二选一”后 resolve。 */
export interface VersionConflictRejection {
  code: 409
  isConflict: true
  serverStory: Story | null
  promise: Promise<Story>
  resolveWithMine: (localStory: Story) => void
  resolveWithTheirs: () => Promise<void>
}

/**
 * 乐观锁版本比对：执行可能触发 409 的请求，若返回 409 则 reject 带 promise 与 resolve 方法，
 * 等待 UI 层“二选一”冲突处理结果后再 resolve。
 * requestFn 可返回 Story 或 { data: Story }（如 axios 响应体）。
 */
export async function checkVersionConflict(
  requestFn: () => Promise<Story | { data: Story }>,
  getServerStory?: () => Promise<Story>
): Promise<Story> {
  try {
    const raw = await requestFn()
    const story = (raw && typeof raw === 'object' && 'data' in raw ? (raw as { data: Story }).data : raw) as Story
    return story
  } catch (e: unknown) {
    const err = e as { response?: { status?: number; data?: { data?: Story } } }
    if (err?.response?.status !== 409) throw e
    let resolvePromise: (s: Story) => void
    const promise = new Promise<Story>((r) => {
      resolvePromise = r
    })
    const serverStory: Story | null = err?.response?.data?.data ?? null
    const rejectObj: VersionConflictRejection = {
      code: 409,
      isConflict: true,
      serverStory,
      promise,
      resolveWithMine: (localStory: Story) => resolvePromise(localStory),
      resolveWithTheirs: () =>
        getServerStory
          ? getServerStory().then((s) => resolvePromise(s))
          : (serverStory != null ? Promise.resolve(resolvePromise(serverStory)) : Promise.reject(new Error('No server story for resolveWithTheirs'))),
    }
    return Promise.reject(rejectObj)
  }
}

