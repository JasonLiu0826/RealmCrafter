import api from './api'

/** 通知类型：SYSTEM、MENTION、INTERACTION、REWARD */
export type NotificationType = 'SYSTEM' | 'MENTION' | 'INTERACTION' | 'REWARD'

export interface SystemNotification {
  id: number
  userId: number
  type: NotificationType | string
  title?: string
  body?: string
  refType?: string
  refId?: string
  actorUserId?: number
  isRead: boolean
  createTime: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// ---------- 通知 ----------

/** 分页获取通知列表，type 可选：SYSTEM、MENTION、INTERACTION、REWARD；不传为全部 */
export async function listNotifications(params: {
  type?: string
  page?: number
  size?: number
}) {
  const { type, page = 0, size = 20 } = params ?? {}
  const search = new URLSearchParams()
  if (type) search.set('type', type)
  search.set('page', String(page))
  search.set('size', String(size))
  return api.get<{ code: number; message: string; data: PageResponse<SystemNotification> }>(
    `/api/v1/notifications?${search.toString()}`,
  )
}

/** 标记单条通知已读 */
export async function markNotificationRead(notificationId: number) {
  return api.patch<{ code: number; message: string; data: null }>(
    `/api/v1/notifications/${notificationId}/read`,
  )
}

/** 标记全部通知已读 */
export async function markAllNotificationsRead() {
  return api.patch<{ code: number; message: string; data: null }>(
    '/api/v1/notifications/read-all',
  )
}

// ---------- 私信 ----------

export type MsgType = 'TEXT' | 'FORWARD_CARD'

export interface Message {
  id: number
  senderId: number
  receiverId: number
  msgType: MsgType
  content: string
  isRead: boolean
  createTime: string
}

export interface MessageSession {
  id: number
  userId: number
  peerId: number
  lastMessageId?: number
  lastMessagePreview?: string
  lastMessageAt?: string
  unreadCount: number
  createTime: string
  updateTime: string
}

export interface SendMessagePayload {
  receiverId: number
  msgType?: MsgType | string
  content: string
}

/** 发送私信 */
export async function sendMessage(payload: SendMessagePayload) {
  return api.post<{ code: number; message: string; data: Message }>(
    '/api/v1/messages/send',
    payload,
  )
}

/** 获取最近会话列表 */
export async function getRecentSessions(params?: { page?: number; size?: number }) {
  const { page = 0, size = 20 } = params ?? {}
  return api.get<{ code: number; message: string; data: PageResponse<MessageSession> }>(
    `/api/v1/messages/sessions?page=${page}&size=${size}`,
  )
}

/** 获取与某人的聊天记录 */
export async function getChatHistory(peerId: number, params?: { page?: number; size?: number }) {
  const { page = 0, size = 20 } = params ?? {}
  return api.get<{ code: number; message: string; data: PageResponse<Message> }>(
    `/api/v1/messages/chat/${peerId}?page=${page}&size=${size}`,
  )
}
