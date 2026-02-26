import api from './api'

export interface EngineConfig {
  modelName: string
  chaosLevel: number
  memoryDepth?: number
  useByok?: boolean
}

export async function fetchEngineConfig() {
  return api.get<{ code: number; message: string; data: EngineConfig }>(
    '/api/v1/users/me/engine-config',
  )
}

export async function updateEngineConfig(payload: Partial<EngineConfig>) {
  return api.patch<{ code: number; message: string; data: EngineConfig }>(
    '/api/v1/users/me/engine-config',
    payload,
  )
}

