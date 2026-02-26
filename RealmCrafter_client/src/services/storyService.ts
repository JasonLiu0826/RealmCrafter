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

