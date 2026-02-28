import api from './api'

export type SquareSort = 'NEWEST' | 'HOT'

export interface StorySquareItem {
  id: string
  userId: number
  settingPackId: string
  title: string
  cover?: string
  description?: string
  price: number
  isPublic: boolean
  status: string
  versionId: number
  likesCount: number
  forkCount: number
  favoriteCount?: number
  sourceStoryId?: string
  lastChapterIndex?: number
  createTime: string
  updateTime: string
}

export interface SettingSquareItem {
  id: string
  userId: number
  title: string
  cover?: string
  description?: string
  price: number
  isPublic: boolean
  status: string
  versionId: number
  likesCount: number
  forkCount: number
  favoriteCount?: number
  sourceSettingId?: string
  createTime: string
  updateTime: string
}

export interface SquareStoriesResponse {
  code: number
  message: string
  data: {
    content: StorySquareItem[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }
}

export interface SquareSettingsResponse {
  code: number
  message: string
  data: {
    content: SettingSquareItem[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }
}

/**
 * 故事广场：分页瀑布流，仅公开且 NORMAL 的资产。
 */
export async function fetchSquareStories(params: {
  sort?: SquareSort
  keyword?: string
  page?: number
  size?: number
}): Promise<SquareStoriesResponse['data']> {
  const { sort = 'NEWEST', keyword, page = 0, size = 20 } = params
  const res = await api.get<SquareStoriesResponse>('/api/v1/square/stories', {
    params: { sort, keyword, page, size },
  })
  return (res as unknown as SquareStoriesResponse).data
}

/**
 * 设定广场：分页瀑布流，仅公开且 NORMAL 的资产。
 */
export async function fetchSquareSettings(params: {
  sort?: SquareSort
  keyword?: string
  page?: number
  size?: number
}): Promise<SquareSettingsResponse['data']> {
  const { sort = 'NEWEST', keyword, page = 0, size = 20 } = params
  const res = await api.get<SquareSettingsResponse>('/api/v1/square/settings', {
    params: { sort, keyword, page, size },
  })
  return (res as unknown as SquareSettingsResponse).data
}

/**
 * 获取单个故事详情（公开或本人）。可选传 userId 以识别本人。
 */
export async function getStoryDetail(
  id: string,
  userId?: number | string
): Promise<StorySquareItem> {
  const headers = userId != null ? { 'X-User-Id': String(userId) } : undefined
  const res = await api.get<{ code: number; message: string; data: StorySquareItem }>(
    `/api/v1/stories/${id}`,
    { headers }
  )
  return (res as unknown as { data: StorySquareItem }).data
}

/**
 * Fork 故事：硬核克隆 + 章节深拷贝，付费则扣费并分润。需传 userId（X-User-Id）。
 */
export async function forkStory(
  sourceStoryId: string,
  userId: number | string
): Promise<StorySquareItem> {
  const res = await api.post<{ code: number; message: string; data: StorySquareItem }>(
    `/api/v1/stories/${sourceStoryId}/fork`,
    {},
    { headers: { 'X-User-Id': String(userId) } }
  )
  return (res as unknown as { data: StorySquareItem }).data
}
