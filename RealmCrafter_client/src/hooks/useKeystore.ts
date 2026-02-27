/**
 * BYOK（Bring Your Own Key）密钥存储
 * 将用户自带 API Key 仅存于客户端本地，不提交至服务端。
 * 使用 LocalStorage 命名空间键；生产环境可替换为 Web Crypto 或安全容器。
 */

const BYOK_STORAGE_KEY = 'realmcrafter:byok_api_key'

/** 安全容器接口，便于后续替换为加密存储 */
interface KeystoreAdapter {
  get(): string | null
  set(value: string): void
  clear(): void
}

const localStorageAdapter: KeystoreAdapter = {
  get() {
    if (typeof window === 'undefined') return null
    try {
      return window.localStorage.getItem(BYOK_STORAGE_KEY)
    } catch {
      return null
    }
  },
  set(value: string) {
    if (typeof window === 'undefined') return
    try {
      window.localStorage.setItem(BYOK_STORAGE_KEY, value)
    } catch {
      // quota exceeded or private mode
    }
  },
  clear() {
    if (typeof window === 'undefined') return
    try {
      window.localStorage.removeItem(BYOK_STORAGE_KEY)
    } catch {}
  },
}

/** 当前使用的适配器，可替换为加密实现 */
let adapter: KeystoreAdapter = localStorageAdapter

export function setKeystoreAdapter(next: KeystoreAdapter) {
  adapter = next
}

export function getByokApiKey(): string | null {
  return adapter.get()
}

export function setByokApiKey(key: string): void {
  adapter.set(key)
}

export function clearByokApiKey(): void {
  adapter.clear()
}

/**
 * Hook：读写 BYOK API Key（仅客户端本地）
 */
export function useKeystore() {
  return {
    getApiKey: getByokApiKey,
    setApiKey: setByokApiKey,
    clearApiKey: clearByokApiKey,
    hasApiKey: (): boolean => {
      const k = adapter.get()
      return Boolean(k && k.trim().length > 0)
    },
  }
}
