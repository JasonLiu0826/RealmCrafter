# RealmCrafter 全栈 API 联调白皮书

> 面向前端 / 客户端的接口对接说明，涵盖鉴权、统一响应、全部 REST 与 SSE 接口、广告与生成流程、分享与站内卡片。

---

## 一、基础约定

### 1.1 服务地址与版本

| 项 | 说明 |
|----|------|
| **Base URL** | 开发/生产环境根地址（如 `https://api.realmcrafter.app`），端口默认 8080 |
| **API 前缀** | 所有接口以 `/api/v1` 开头 |

### 1.2 鉴权

- **推荐**：请求头 **`Authorization: Bearer <token>`**，其中 `token` 由登录/注册接口返回。
- **兼容**：请求头 **`X-User-Id`**（当前用户 ID）仍可用，供网关注入或开发联调。
- 未传或无效时，部分接口返回 401/400，SSE 生成接口会直接报错。
- 公开接口（无需登录）：`/api/v1/auth/**`、`/api/v1/payment/**`、`/api/v1/share/decode/**`、`/api/v1/upload/files/**`。

### 1.3 统一响应体 `Result<T>`

除 **SSE 流式接口** 外，所有接口均返回 JSON：

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 0 表示成功；非 0 为业务/错误码 |
| `message` | string | 提示信息 |
| `data` | T \| null | 业务数据，列表/分页时见下节 |

**判断成功**：`response.code === 0`（且 HTTP 状态一般为 200）。

### 1.4 分页响应（Spring Page）

列表类接口的 `data` 为 Spring `Page` 序列化结果，结构如下：

```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

- `content`：当前页数据数组。
- `number`：当前页码（从 0 开始）。
- `size`：每页条数。

---

## 二、全局错误码与 HTTP 状态

| code | HTTP 状态 | 含义 | 前端建议 |
|------|-----------|------|----------|
| 0 | 200 | 成功 | 正常渲染 `data` |
| 400 | 400 | 参数错误 / 校验失败 | 展示 `message`，检查请求体与 Query |
| 402 | 402 | Token/灵能水晶不足 | 引导观看广告或充值 |
| 409 | 409 | 同步冲突（如乐观锁） | 展示冲突/合并页，让用户选择 |
| 451 | 451 | 需要插屏广告 | 见 [§ 五、广告与心跳](#五广告与心跳) |
| 500 | 500 | 服务端异常 | 展示通用错误提示 |

**注意**：AI 流式生成中的**内容违规**在 SSE 里以 `error` 事件下发，不经过上述 Result（见 [§ 四、SSE 流式生成](#四sse-流式生成)）。

---

## 三、接口清单（按模块）

### 3.1 故事（书架）`/api/v1/stories`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| GET | `/api/v1/stories` | 分页列表当前用户故事 | Header: `X-User-Id`；Query: `page`(0), `size`(20), `keyword`(可选) | `Page<Story>` |
| GET | `/api/v1/stories/{id}` | 故事详情 | Path: `id`；Header: `X-User-Id`(可选) | `Story` |
| POST | `/api/v1/stories` | 创建故事 | Header: `X-User-Id`；Body: 见下 | `Story` |
| PATCH | `/api/v1/stories/{id}/rename` | 重命名 | Header: `X-User-Id`；Body: `{ "userId": Long, "title": string }` | `Story` |
| PATCH | `/api/v1/stories/{id}/read` | 更新最后阅读时间 | Header: `X-User-Id` | `Story` |
| POST | `/api/v1/stories/{id}/fork` | Fork 故事（付费/分润/章节拷贝） | Header: `X-User-Id` | `Story`（新故事） |
| DELETE | `/api/v1/stories/{id}` | 软删除 | Header: `X-User-Id` | null |

**POST 创建故事 Body**（`CreateStoryRequest`）：

```json
{
  "userId": 1,
  "settingPackId": "st_xxx",
  "title": "我的第一个故事",
  "cover": "https://...",
  "description": "简介（可选）",
  "price": 0
}
```

**Story 常用字段**：`id`, `userId`, `settingPackId`, `title`, `cover`, `description`, `price`, `isPublic`, `status`, `lastChapterIndex`, `lastReadTime`, `likesCount`, `forkCount`, `trafficWeight`, `createTime`, `updateTime`。

---

### 3.2 设定集 `/api/v1/settings`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| GET | `/api/v1/settings` | 分页列表当前用户设定集 | Header: `X-User-Id`；Query: `page`, `size` | `Page<SettingPack>` |
| POST | `/api/v1/settings` | 创建设定集 | Header: `X-User-Id`；Body: title, content, allowDownload, allowModify, price 等 | `SettingPack` |
| GET | `/api/v1/settings/{id}` | 设定集详情 | Path: `id`；Header: `X-User-Id` | `SettingPack` |
| PUT | `/api/v1/settings/{id}` | 更新（乐观锁） | Header: `X-User-Id`；Body: `versionId` 必填 + 其他字段 | `SettingPack` |
| POST | `/api/v1/settings/{id}/fork` | Fork 设定集 | Header: `X-User-Id` | `SettingPack` |

**乐观锁**：更新设定集时 Body 必须带当前 `versionId`；若服务端返回 409，表示并发冲突，前端可做合并或冲突页。

---

### 3.3 广场发现 `/api/v1/square`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| GET | `/api/v1/square/stories` | 公开故事列表（带流量权重排序） | Query: `sort`(NEWEST/HOT/**TRAFFIC**), `keyword`(可选), `page`, `size`；Header: `X-User-Id`(可选，用于经验) | `Page<Story>` |
| GET | `/api/v1/square/settings` | 公开设定集列表 | Query: `sort`(NEWEST/HOT), `keyword`, `page`, `size` | `Page<SettingPack>` |

**排序**：`NEWEST` 最新，`HOT` 热度，`TRAFFIC` 按后台计算的 `trafficWeight` 排序（适合「推荐」流）。

---

### 3.4 AI 章节生成（SSE）`/api/v1/engine`

见 [§ 四、SSE 流式生成](#四sse-流式生成)。

---

### 3.5 社交互动 `/api/v1/interactions`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/interactions/like` | 点赞/取消赞（幂等） | Header: `X-User-Id`；Body: `{ "type": "STORY" \| "SETTING" \| "COMMENT", "id": "..." }` | `{ liked, type, id }` |
| POST | `/api/v1/interactions/favorite` | 收藏/取消收藏（幂等） | Header: `X-User-Id`；Body: `{ "type": "STORY" \| "SETTING", "id": "..." }` | `{ favorited, type, id }` |

`id`：故事/设定集为字符串 id，评论为评论 id（数字转字符串即可）。

---

### 3.6 评论 `/api/v1/comments`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/comments` | 发表评论或回复（含 @提及） | Header: `X-User-Id`；Body: 见下 | `Comment` |
| GET | `/api/v1/comments/anchor` | 按锚点分页一级评论 | Query: `storyId`, `chapterId`, `targetType`(PARAGRAPH/OPTION), `targetRef`(可选), `page`, `size` | `Page<Comment>` |
| GET | `/api/v1/comments/replies/{rootCommentId}` | 某条顶级评论的楼中楼 | Path: `rootCommentId` | `Comment[]` |
| GET | `/api/v1/comments/{commentId}` | 单条评论 | Path: `commentId` | `Comment` |
| DELETE | `/api/v1/comments/{commentId}` | 软删除（仅作者） | Path: `commentId`；Header: `X-User-Id` | null |

**POST 评论 Body**（`AddCommentRequest`）：

```json
{
  "storyId": "st_xxx",
  "chapterId": 1,
  "content": "这段写得好 @张三",
  "targetType": "PARAGRAPH",
  "targetRef": "2",
  "parentCommentId": null,
  "mentionedUserIds": [2]
}
```

- 顶级评论：`parentCommentId` 不传或 null；`targetType` 为 `PARAGRAPH` / `OPTION`，`targetRef` 为段落索引或选项标识。
- 回复：传 `parentCommentId` 为根评论 id；`targetType` 可为 `COMMENT`，`targetRef` 可为父评论 id。  
- `mentionedUserIds` 为 @ 提及的用户 id 列表，与正文中的 @昵称 互补，用于系统通知。

---

### 3.7 分享 `/api/v1/share`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/share/generate` | 生成短链与站内转发卡片 | Body: 见下 | `{ shortCode, deepLink, forwardCardPayload }` |
| GET | `/api/v1/share/decode/{shortCode}` | 解析短链得到锚点 | Path: `shortCode` | `ShareTarget` |

**POST 生成分享 Body**（`GenerateShareRequest`）：

```json
{
  "type": "PARAGRAPH",
  "storyId": "st_xxx",
  "chapterId": 1,
  "targetRef": "2",
  "excerpt": "摘录文案，用于转发卡片展示"
}
```

`type`：`PARAGRAPH` | `OPTION` | `COMMENT`。

**响应**：

- `shortCode`：短码，用于生成短链。
- `deepLink`：完整深度链接（如 `https://realmcrafter.app/s/abc123`），可用于外部分享。
- `forwardCardPayload`：站内转发卡片 payload，用于 [§ 3.9 私信](#39-私信-im-apiv1messages) 的 `msgType: "FORWARD_CARD"`，直接作为 `content` 或与文案组合。

**GET decode 响应**（`ShareTarget`）：`type`, `storyId`, `chapterId`, `targetRef`, `excerpt`。前端打开深度链接后调此接口，再根据 `storyId`、`chapterId`、`targetRef` 定位到段落/选项/评论。

---

### 3.8 系统通知 `/api/v1/notifications`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| GET | `/api/v1/notifications` | 分页获取当前用户通知 | Header: `X-User-Id`；Query: `type`(可选 SYSTEM/MENTION/INTERACTION/REWARD), `page`, `size` | `Page<SystemNotification>` |
| PATCH | `/api/v1/notifications/{id}/read` | 标记单条已读 | Path: `id`；Header: `X-User-Id` | null |
| PATCH | `/api/v1/notifications/read-all` | 标记全部已读 | Header: `X-User-Id` | null |

---

### 3.9 私信 IM `/api/v1/messages`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/messages/send` | 发送私信 | Header: `X-User-Id`；Body: 见下 | `Message` |
| GET | `/api/v1/messages/sessions` | 最近会话列表 | Header: `X-User-Id`；Query: `page`, `size` | `Page<MessageSession>` |
| GET | `/api/v1/messages/chat/{peerId}` | 与某人的聊天记录 | Path: `peerId`；Header: `X-User-Id`；Query: `page`, `size` | `Page<Message>` |

**POST 发送 Body**（`SendMessageRequest`）：

```json
{
  "receiverId": 2,
  "msgType": "TEXT",
  "content": "你好"
}
```

- `msgType`：`TEXT`（默认）或 `FORWARD_CARD`。
- **站内转发卡片**：`msgType: "FORWARD_CARD"`，`content` 为 [§ 3.7](#37-分享-apiv1share) 中 `forwardCardPayload` 的 JSON 字符串（或服务端约定结构）。前端可先调 `POST /api/v1/share/generate` 拿到 `forwardCardPayload`，再发私信时把该对象序列化进 `content`。

**MessageSession**：`id`, `userId`, `peerId`, `lastMessageId`, `lastMessagePreview`, `lastMessageAt`, `unreadCount`。

---

### 3.10 用户与引擎配置 `/api/v1/users/me`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| GET | `/api/v1/users/me/engine-config` | 获取引擎配置 | Header: `X-User-Id` | `{ chaosLevel, preferredModel }` |
| PATCH | `/api/v1/users/me/engine-config` | 更新引擎配置 | Header: `X-User-Id`；Body: `{ chaosLevel?: 0.1~1, preferredModel?: string }` | 同上 |
| PATCH | `/api/v1/users/me/theme` | 切换主题（含 VIP 校验） | Header: `X-User-Id`；Body: `{ "themeId": "classic_white" }` | 同 Result |

---

### 3.11 心跳与广告

见 [§ 五、广告与心跳](#五广告与心跳)。

---

### 3.12 鉴权与资料 — `/api/v1/auth`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/auth/register` | 注册 | Body: username, password, nickname?, signature? | `userId`, `token`, `profile` |
| POST | `/api/v1/auth/login` | 账号密码登录 | Body: username, password | 同上 |
| POST | `/api/v1/auth/phone-login` | 手机验证码登录 | Body: phone, code（6 位） | 同上 |
| POST | `/api/v1/auth/wechat-login` | 微信登录 | Body: openId | 同上 |
| POST | `/api/v1/auth/apple-login` | 苹果登录 | Body: appleId | 同上 |
| GET | `/api/v1/auth/profile` | 当前用户资料 | 需鉴权 | `UserProfileDTO` |
| PATCH | `/api/v1/auth/profile` | 更新昵称/签名/头像 | Body: nickname?, signature?, avatar? | `UserProfileDTO` |

登录/注册成功时 `data` 含 `token`，后续请求带 `Authorization: Bearer <token>`。`profile` 含 `userId`, `username`, `role`, `nickname`, `signature`, `avatar`, `level`, `exp`, `isGoldenCreator`, `vipExpireTime`, `tokenBalance`, `crystalBalance`。

---

### 3.13 图片上传 — `/api/v1/upload`

| 方法 | 路径 | 说明 | 请求 | 响应 data |
|------|------|------|------|-----------|
| POST | `/api/v1/upload/image` | 上传图片（封面/头像） | Multipart: **file**（≤5MB，PNG/JPG/GIF/WebP）；需鉴权 | `{ "url": "https://..." }` |

上传成功后用返回的 `url` 填到故事/设定集封面或用户头像字段。

---

### 3.14 管理后台 — `/api/v1/admin`（仅 ADMIN/SUPER_ADMIN）

| 方法 | 路径 | 说明 | 请求 | 响应 |
|------|------|------|------|------|
| POST | `/api/v1/admin/seal-user` | 封禁用户 | Query: userId, sealedUntil?（ISO 日期时间） | 200 |
| POST | `/api/v1/admin/take-down-story` | 强制下架故事 | Query: storyId | 200 |
| POST | `/api/v1/admin/grant-golden-creator` | 授予金牌创作者 | Query: userId | 200 |

需管理员登录后带 Bearer token 调用；非管理员返回 403。

---

### 3.15 支付回调 — `/api/v1/payment`

支付渠道回调（微信/支付宝/苹果）由服务端与渠道配置，前端无需直接调用。回调验签后服务端加水晶或续 VIP。

---

## 四、SSE 流式生成

### 4.1 接口与请求

- **URL**：`POST /api/v1/engine/generate/stream`
- **Request Headers**：`Content-Type: application/json`，**`X-User-Id` 必填**。
- **Request Body**（`GenerateStreamRequest`）：

```json
{
  "storyId": "st_xxx",
  "userChoice": "进入深渊之门",
  "chaosLevel": 0.7,
  "useByok": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| storyId | string | 是 | 故事 id |
| userChoice | string | 否 | 用户选择或自定义输入，默认 "" |
| chaosLevel | number | 否 | 0～1，对应 LLM temperature，默认 0.7 |
| useByok | boolean | 否 | 是否使用用户自己的 API Key，默认 false |

- **Response**：`Content-Type: text/event-stream`，流式 SSE。

### 4.2 推荐调用顺序（含广告）

1. 先调 **`POST /api/v1/heartbeat/chapter-generate`**（同鉴权头 `X-User-Id`）。
2. 若 **HTTP 200**：直接调 **`POST /api/v1/engine/generate/stream`** 开始生成。
3. 若 **HTTP 451**：解析响应体 `code=451`、`data.needAd=true`、`data.adToken` → 前端展示广告 → 用户观看完成后调 **`POST /api/v1/ad/callback`**，Body `{ "adToken": data.adToken }` → 再调一次心跳 → 收到 200 后再调流式生成。

这样可保证「先计费/广告，再生成」，避免绕过广告。

### 4.3 SSE 事件格式

服务端发送的是 **标准 SSE**：每条为多行，以 `data:` 行承载内容，末尾双换行结束一条事件。

- **正文片段**：`data` 为单行 JSON：`{"content":"一段文字"}`。可能多次发送，前端逐段拼接并做打字机效果。
- **选项列表**：`data` 为单行 JSON：`{"branches":["选项A","选项B",...]}`。一般出现在流结束前。
- **结束**：`data` 为纯字符串 `[DONE]`（无 JSON 包裹）。
- **错误**：事件名 `error`，`data` 为 JSON：`{"message":"错误说明","interrupt":true}`。`interrupt: true` 表示内容违规被中断；否则为其它异常。收到 `error` 后应结束流并提示用户。

**注意**：同一条 `data` 内是**完整**的一行 JSON，不存在「半段 JSON」；若前端用 `EventSource`，需按行解析 `event.data` 再 `JSON.parse`。若用 `fetch` + 流式读 body，需按行缓冲、拼出完整一行再解析。

### 4.4 前端解析示例（思路）

```javascript
// 使用 fetch 消费 SSE
const res = await fetch('/api/v1/engine/generate/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'X-User-Id': userId },
  body: JSON.stringify({ storyId, userChoice, chaosLevel, useByok })
});
const reader = res.body.getReader();
const decoder = new TextDecoder();
let buffer = '';
while (true) {
  const { value, done } = await reader.read();
  if (done) break;
  buffer += decoder.decode(value, { stream: true });
  const lines = buffer.split('\n');
  buffer = lines.pop() || '';
  for (const line of lines) {
    if (line.startsWith('data:')) {
      const raw = line.slice(5).trim();
      if (raw === '[DONE]') { /* 结束 */ continue; }
      try {
        const obj = JSON.parse(raw);
        if (obj.content) appendContent(obj.content);
        if (obj.branches) setBranches(obj.branches);
      } catch (e) { /* 忽略解析错误或记日志 */ }
    }
  }
}
```

若使用 `EventSource`，需注意它只支持 GET；流式生成为 POST，因此需用 `fetch` + `ReadableStream` 或封装成可订阅的流。

### 4.5 超时与错误

- 服务端 SSE 超时时间较长（如 5 分钟），前端可按需设置 AbortSignal 或展示「生成中」状态。
- 流中收到 `error` 事件：关闭流、展示 `message`；若 `interrupt === true` 可提示「内容不符合规范，已中断」。

---

## 五、广告与心跳

### 5.1 流程概览

1. 用户点击「生成下一章」前，前端先请求 **`POST /api/v1/heartbeat/chapter-generate`**（Header: `X-User-Id`）。
2. **200**：计费/计数已更新，可直接发起 **`POST /api/v1/engine/generate/stream`**。
3. **451**：需要先展示广告。响应体示例：

```json
{
  "code": 451,
  "message": "AD_TRIGGER",
  "data": {
    "needAd": true,
    "adToken": "a1b2c3d4e5f6..."
  }
}
```

4. 前端展示插屏/激励视频；用户**观看完成后**请求 **`POST /api/v1/ad/callback`**：

```json
{
  "adToken": "a1b2c3d4e5f6..."
}
```

5. 若 callback 成功（`code === 0`），再调一次 **`POST /api/v1/heartbeat/chapter-generate`**，此次应返回 200，然后即可调流式生成。

### 5.2 接口小结

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/heartbeat/chapter-generate` | 生成前心跳；200 可生成，451 需广告并带 `adToken` |
| POST | `/api/v1/ad/callback` | 广告观看完成，Body `{ "adToken": "..." }`；成功后下次心跳可 200 |

- `adToken` 一次性有效，过期或重复使用会返回 400。
- 未传 `adToken` 或 token 无效时，callback 返回 `code !== 0`，前端应提示并不可继续生成，直到用户重新完成广告流程。

---

## 六、站内深度链接与转发卡片

### 6.1 外部分享（深度链接）

1. 前端在「分享」时调 **`POST /api/v1/share/generate`**，拿到 `shortCode`、`deepLink`。
2. 将 `deepLink`（如 `https://realmcrafter.app/s/abc123`）复制/分享给站外用户。
3. 用户打开链接进入 App 后，用路径中的 `shortCode`（如 `abc123`）请求 **`GET /api/v1/share/decode/{shortCode}`**。
4. 根据返回的 `storyId`、`chapterId`、`targetRef`、`type` 跳转到对应故事章节并定位到段落/选项/评论。

### 6.2 站内转发（IM 卡片）

1. 同样先调 **`POST /api/v1/share/generate`**，拿到 **`forwardCardPayload`**（对象：`type`, `storyId`, `chapterId`, `targetRef`, `excerpt`）。
2. 发送私信时使用 **`POST /api/v1/messages/send`**，`msgType: "FORWARD_CARD"`，`content` 为 `forwardCardPayload` 的 **JSON 字符串**（或后端约定的序列化格式）。
3. 接收方列表/聊天页解析 `msgType`，对 `FORWARD_CARD` 渲染为可点击的「故事/段落卡片」，点击后同样用 `storyId`、`chapterId`、`targetRef` 跳转定位。

---

## 七、联调检查清单

- [ ] 需登录接口带 `Authorization: Bearer <token>`（或兼容 `X-User-Id`）。
- [ ] 成功判断以 `code === 0` 为准，HTTP 状态仅作参考（451 为业务态）。
- [ ] 分页列表使用 `data.content`、`data.totalElements`、`data.number`、`data.size`。
- [ ] 生成章节前先心跳；遇 451 先展示广告 → callback → 再心跳 → 再 SSE 生成。
- [ ] SSE 按行解析 `data:`，对非 `[DONE]` 做 `JSON.parse`，并处理 `error` 事件。
- [ ] 设定集更新带 `versionId`；收到 409 做冲突/合并处理。
- [ ] 分享：外链用 `deepLink`；站内卡片用 `forwardCardPayload` + 私信 `FORWARD_CARD`。
- [ ] 402 引导广告/充值；400 展示 `message` 做参数校验提示。
- [ ] 登录/注册后保存 `token`，请求头带 `Authorization: Bearer <token>`。
- [ ] 封面/头像先调 `POST /api/v1/upload/image` 拿到 `url` 再提交故事/设定/资料。

---

## 八、附录：常用实体字段速查

- **Story**：id, userId, settingPackId, title, cover, description, price, isPublic, status, lastChapterIndex, lastReadTime, likesCount, forkCount, trafficWeight, createTime, updateTime。
- **Comment**：id, storyId, chapterId, userId, content, targetType, targetRef, rootCommentId, parentCommentId, likesCount, replyCount, createTime。
- **MessageSession**：id, userId, peerId, lastMessageId, lastMessagePreview, lastMessageAt, unreadCount。
- **SystemNotification**：id, userId, type(SYSTEM/MENTION/INTERACTION/REWARD/LEVEL_UP), title, body, refType, refId, actorUserId, isRead, createTime。

以上字段以实际后端实体为准；若有增减，以服务端返回为准。

---

*文档版本与后端 ARCHITECTURE.md 对齐，如有接口变更以后端发布为准。*
