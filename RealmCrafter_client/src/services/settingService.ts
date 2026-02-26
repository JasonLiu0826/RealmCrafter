import api from './api'

export interface SettingContentDTO {
  characters: string
  worldview: string
  environment: string
  mainline: string
  plotPoints: string
}

export interface SettingPack {
  id: string
  userId: number
  title: string
  cover?: string
  description?: string
  content: SettingContentDTO
  price: number
  versionId: number
  deviceHash?: string
  createTime: string
  updateTime: string
}

export interface CreateSettingPayload {
  title: string
  cover?: string
  description?: string
  content: SettingContentDTO
  deviceHash?: string
}

export interface UpdateSettingPayload {
  versionId: number
  title?: string
  cover?: string
  description?: string
  content: SettingContentDTO
  deviceHash?: string
}

export async function fetchSettings() {
  return api.get<{ code: number; message: string; data: SettingPack[] }>(
    '/api/v1/settings',
  )
}

export async function fetchSettingDetail(id: string) {
  return api.get<{ code: number; message: string; data: SettingPack }>(
    `/api/v1/settings/${id}`,
  )
}

export async function createSetting(payload: CreateSettingPayload) {
  return api.post<{ code: number; message: string; data: SettingPack }>(
    '/api/v1/settings',
    payload,
  )
}

export async function updateSetting(
  id: string,
  payload: UpdateSettingPayload,
) {
  return api.put<{ code: number; message: string; data: SettingPack }>(
    `/api/v1/settings/${id}`,
    payload,
  )
}

