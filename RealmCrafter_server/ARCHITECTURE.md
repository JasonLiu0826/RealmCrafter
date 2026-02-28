# RealmCrafter 后端 Server 架构与接口说明

> 基于对 `RealmCrafter_server` 的完整遍历整理，包含架构、功能逻辑关系、详细路径、各层实现及全部接口信息，便于后续开发与扩展。  
> **前端/客户端联调**：参见 [《全栈 API 联调白皮书》](docs/API_INTEGRATION_WHITEPAPER.md)。

---

## 一、项目概览与技术栈

| 项目 | 说明 |
|------|------|
| **项目名** | realmcrafter-server |
| **描述** | RealmCrafter AIGC 互动剧情与 UGC 社区后端服务 |
| **启动类** | `com.realmcrafter.RealmCrafterApplication`（`@SpringBootApplication` + `@EnableScheduling`） |

**技术栈：**

- **框架**：Spring Boot 2.7.18，Java 17  
- **Web**：spring-boot-starter-web + spring-boot-starter-webflux（流式 SSE）  
- **数据**：Spring Data JPA + MySQL（HikariCP）  
- **安全**：Spring Security + JWT（jjwt 0.11.5）  
- **缓存**：Spring Data Redis  
- **消息**：Spring AMQP（RabbitMQ）  
- **校验**：spring-boot-starter-validation  
- **工具**：Lombok  

**配置入口**：`src/main/resources/application.yml`

- 服务端口：**8080**  
- 数据库：`jdbc:mysql://localhost:3306/realmcrafter?...`  
- Redis：localhost:6379  
- 计费：`realmcrafter.billing.tokens-per-chapter: 5000`  
- 分享根域名：`realmcrafter.share.base-url: https://realmcrafter.app`  

---

## 二、目录与包结构（src/main/java）

```
com.realmcrafter
├── RealmCrafterApplication.java              # 启动类
│
├── api/                                       # 对外 HTTP 接口层
│   ├── dto/
│   │   └── Result.java                        # 统一响应 Result<T>（code, message, data）
│   ├── asset/                                 # 资产：故事、设定集、广场
│   │   ├── StoryController.java
│   │   ├── SettingPackController.java
│   │   ├── SquareController.java
│   │   └── dto/
│   │       ├── CreateStoryRequest.java
│   │       ├── RenameStoryRequest.java
│   │       ├── CreateSettingRequest.java
│   │       └── UpdateSettingRequest.java
│   ├── engine/                                # AI 章节生成
│   │   ├── EngineController.java
│   │   └── dto/
│   │       └── GenerateStreamRequest.java
│   ├── social/                                # 社交：点赞、收藏、评论、分享、通知、私信
│   │   ├── InteractionController.java
│   │   ├── CommentController.java
│   │   ├── ShareController.java
│   │   ├── NotificationController.java
│   │   ├── MessageController.java
│   │   └── dto/
│   │       ├── AddCommentRequest.java
│   │       ├── GenerateShareRequest.java
│   │       └── SendMessageRequest.java
│   ├── user/                                  # 用户偏好与引擎配置
│   │   ├── UserConfigController.java          # 引擎配置（混沌度、模型）
│   │   ├── UserPreferenceController.java      # 主题切换
│   │   └── dto/
│   │       ├── EngineConfigDTO.java
│   │       ├── UpdateEngineConfigRequest.java
│   │       └── UpdateThemeRequest.java
│   ├── heartbeat/
│   │   └── HeartbeatController.java           # 生成前心跳（广告触发）
│   └── ad/
│       ├── AdController.java                  # 广告观看完成回调
│       └── dto/
│           └── AdCallbackRequest.java
│
├── application/                               # 应用服务（编排领域 + 基础设施）
│   ├── chapter/
│   │   └── ChapterApplicationService.java     # 章节流式生成编排
│   ├── user/
│   │   └── ThemeApplicationService.java      # 主题切换（含 VIP 校验）
│   └── heartbeat/
│       └── HeartbeatService.java             # 心跳逻辑（计费策略 + 广告触发）
│
├── config/                                    # 全局配置与异常
│   ├── GlobalExceptionHandler.java           # 统一异常 -> Result
│   ├── SyncConflictException.java            # 409 同步冲突
│   └── ContentViolationException.java        # 内容违规（LLM 净化中断）
│
├── domain/                                    # 领域层
│   ├── asset/
│   │   ├── service/
│   │   │   ├── StoryService.java
│   │   │   ├── SettingPackService.java
│   │   │   ├── SquareService.java
│   │   │   └── TrafficWeightComputeService.java
│   │   └── dto/
│   │       └── SettingContentDTO.java
│   ├── billing/
│   │   ├── CreatorPriceValidator.java
│   │   ├── CreatorShareResolver.java
│   │   ├── InsufficientTokenException.java
│   │   ├── AdTriggerRequiredException.java
│   │   ├── VipAccessDeniedException.java
│   │   ├── BillingResult.java
│   │   ├── service/
│   │   │   ├── VipValidator.java
│   │   │   └── VipRenewalService.java
│   │   └── strategy/
│   │       ├── BillingStrategy.java
│   │       ├── FreeUserBillingStrategy.java
│   │       └── ByokUserBillingStrategy.java
│   ├── chapter/
│   │   ├── PurificationEngine.java            # 内容净化/违禁检测
│   │   └── service/
│   │       └── ChapterGenerationService.java   # L1/L2/L3 提示拼装
│   ├── social/
│   │   └── service/
│   │       ├── InteractionService.java       # 点赞/收藏
│   │       ├── CommentService.java           # 评论 + @提及通知
│   │       ├── ShareService.java             # 短链与转发卡片
│   │       ├── NotificationService.java      # 系统通知（提及/奖励/已读）
│   │       └── MessageService.java           # 私信 IM（发送/会话/聊天记录）
│   └── user/
│       ├── ExpAction.java                     # 经验行为枚举
│       └── service/
│           └── UserExpService.java            # 经验与等级
│
├── infrastructure/                            # 基础设施
│   ├── persistence/
│   │   ├── entity/                            # JPA 实体（见下表）
│   │   └── repository/                        # JPA Repository 接口
│   ├── id/
│   │   └── AssetIdGenerator.java             # 故事/设定集 ID 生成
│   ├── llm/
│   │   ├── LlmClient.java
│   │   ├── DeepSeekClient.java
│   │   └── dto/
│   │       ├── LlmStreamRequest.java
│   │       └── StreamChunk.java
│   ├── redis/
│   │   ├── StoryGenerationLock.java          # 故事级生成锁
│   │   └── AdCounterCache.java                # 广告计数缓存
│   ├── vector/
│   │   ├── VectorMemoryService.java          # RAG/L3 记忆
│   │   └── MockVectorService.java
│   └── mq/                                    # 消息队列（包占位）
│
└── security/
    └── audit/
        ├── SensitiveWordTrie.java             # 敏感词过滤
        ├── CachingRequestBodyWrapper.java
        └── TrieFilterInterceptor.java
```

---

## 三、API 接口清单（完整路径、方法、请求与响应）

所有接口统一返回体为 **`Result<T>`**（`code`、`message`、`data`），成功时 `code=0`。  
需登录的接口通过请求头 **`X-User-Id`** 传递当前用户 ID（或由网关从 JWT 解析后注入）。

---

### 3.1 故事（书架）— `/api/v1/stories`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| GET | `/api/v1/stories` | 分页列表当前用户故事，支持关键词 | Header: `X-User-Id`；Query: `page`(0), `size`(20), `keyword`(可选) | `Result<Page<StoryDO>>` |
| GET | `/api/v1/stories/{id}` | 故事详情（本人或公开） | Path: `id`；Header: `X-User-Id`(可选) | `Result<StoryDO>` |
| POST | `/api/v1/stories` | 创建故事 | Body: `CreateStoryRequest`（userId, settingPackId, title, cover?, description?, price?） | `Result<StoryDO>` |
| PATCH | `/api/v1/stories/{id}/rename` | 重命名 | Path: `id`；Body: `RenameStoryRequest`（userId, title） | `Result<StoryDO>` |
| PATCH | `/api/v1/stories/{id}/read` | 更新最后阅读时间 | Path: `id`；Header: `X-User-Id` | `Result<StoryDO>` |
| POST | `/api/v1/stories/{id}/fork` | Fork 故事（付费分润、设定集策略、章节拷贝、REWARD 通知） | Path: `id`；Header: `X-User-Id` | `Result<StoryDO>`（新故事） |
| DELETE | `/api/v1/stories/{id}` | 软删除故事 | Path: `id`；Header: `X-User-Id` | `Result<Void>` |

---

### 3.2 设定集 — `/api/v1/settings`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| GET | `/api/v1/settings` | 分页列表当前用户设定集 | Header: `X-User-Id`；Query: `page`, `size` | `Result<Page<SettingPackDO>>` |
| POST | `/api/v1/settings` | 创建设定集 | Header: `X-User-Id`；Body: `CreateSettingRequest`（title, cover?, description?, content, deviceHash?, allowDownload?, allowModify?, price?） | `Result<SettingPackDO>` |
| GET | `/api/v1/settings/{id}` | 设定集详情（校验归属） | Path: `id`；Header: `X-User-Id` | `Result<SettingPackDO>` |
| PUT | `/api/v1/settings/{id}` | 更新设定集（乐观锁 versionId） | Path: `id`；Header: `X-User-Id`；Body: `UpdateSettingRequest`（versionId, title, cover?, description?, content, deviceHash?, allowDownload?, allowModify?） | `Result<SettingPackDO>` |
| POST | `/api/v1/settings/{id}/fork` | Fork 设定集（需 allowDownload） | Path: `id`；Header: `X-User-Id` | `Result<SettingPackDO>` |

---

### 3.3 广场发现 — `/api/v1/square`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| GET | `/api/v1/square/stories` | 公开故事列表（is_public + NORMAL） | Query: `sort`(NEWEST/HOT/TRAFFIC), `keyword`(可选), `page`, `size`；Header: `X-User-Id`(可选，用于经验 FETCH_FROM_SQUARE) | `Result<Page<StoryDO>>` |
| GET | `/api/v1/square/settings` | 公开设定集列表 | Query: `sort`(NEWEST/HOT), `keyword`(可选), `page`, `size` | `Result<Page<SettingPackDO>>` |

---

### 3.4 AI 章节生成 — `/api/v1/engine`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/engine/generate/stream` | 流式生成一章（SSE） | `Content-Type: application/json`；Header: `X-User-Id`（必填）；Body: `GenerateStreamRequest`（storyId, userChoice?, chaosLevel?, useByok?）；响应 `Content-Type: text/event-stream` | SSE 事件：`content` / `branches` / `[DONE]` 或 `error`（含 message、interrupt?） |

**GenerateStreamRequest**：storyId(必填), userChoice(默认 ""), chaosLevel(0~1，默认 0.7), useByok(默认 false)。

---

### 3.5 社交互动 — `/api/v1/interactions`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/interactions/like` | 点赞/取消赞（幂等） | Header: `X-User-Id`；Body: `{ "type": "STORY|SETTING|COMMENT", "id": "..." }` | `Result<Map>`（liked, type, id） |
| POST | `/api/v1/interactions/favorite` | 收藏/取消收藏（幂等） | Header: `X-User-Id`；Body: `{ "type": "STORY|SETTING", "id": "..." }` | `Result<Map>`（favorited, type, id） |

---

### 3.6 评论 — `/api/v1/comments`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/comments` | 发表评论或回复（含 @提及与通知） | Header: `X-User-Id`；Body: `AddCommentRequest`（storyId, chapterId, content, targetType, targetRef?, parentCommentId?, mentionedUserIds?） | `Result<CommentDO>` |
| GET | `/api/v1/comments/anchor` | 按锚点分页一级评论 | Query: `storyId`, `chapterId`, `targetType`(PARAGRAPH/OPTION), `targetRef`(可选), `page`, `size` | `Result<Page<CommentDO>>` |
| GET | `/api/v1/comments/replies/{rootCommentId}` | 某条顶级评论的楼中楼 | Path: `rootCommentId` | `Result<List<CommentDO>>` |
| GET | `/api/v1/comments/{commentId}` | 单条评论 | Path: `commentId` | `Result<CommentDO>` 或 404 |
| DELETE | `/api/v1/comments/{commentId}` | 软删除（仅作者） | Path: `commentId`；Header: `X-User-Id` | `Result<Void>` |

---

### 3.7 分享 — `/api/v1/share`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/share/generate` | 生成分享短链与站内转发卡片 | Body: `GenerateShareRequest`（type, storyId, chapterId, targetRef?, excerpt?） | `Result<Map>`（shortCode, deepLink, forwardCardPayload） |
| GET | `/api/v1/share/decode/{shortCode}` | 解析短链得到锚点信息 | Path: `shortCode` | `Result<ShareTarget>`（type, storyId, chapterId, targetRef, excerpt）；无效 404 |

---

### 3.8 系统通知 — `/api/v1/notifications`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| GET | `/api/v1/notifications` | 分页获取当前用户通知 | Header: `X-User-Id`；Query: `type`(可选 SYSTEM/MENTION/INTERACTION/REWARD)，`page`, `size` | `Result<Page<SystemNotificationDO>>` |
| PATCH | `/api/v1/notifications/{id}/read` | 标记单条已读 | Path: `id`；Header: `X-User-Id` | `Result<Void>` |
| PATCH | `/api/v1/notifications/read-all` | 标记全部已读 | Header: `X-User-Id` | `Result<Void>` |

---

### 3.9 私信 IM — `/api/v1/messages`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/messages/send` | 发送私信 | Header: `X-User-Id`；Body: `SendMessageRequest`（receiverId, msgType?=TEXT, content） | `Result<MessageDO>` |
| GET | `/api/v1/messages/sessions` | 最近会话列表（含最后一条摘要与未读数） | Header: `X-User-Id`；Query: `page`, `size` | `Result<Page<MessageSessionDO>>` |
| GET | `/api/v1/messages/chat/{peerId}` | 与某人的聊天记录（按时间倒序） | Path: `peerId`；Header: `X-User-Id`；Query: `page`, `size` | `Result<Page<MessageDO>>` |

**SendMessageRequest**：receiverId(必填), msgType(TEXT|FORWARD_CARD，默认 TEXT), content(必填)。

---

### 3.10 用户与引擎配置 — `/api/v1/users/me` 与 `/api/v1/users/me/engine-config`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| GET | `/api/v1/users/me/engine-config` | 获取引擎配置 | Header: `X-User-Id` | `Result<EngineConfigDTO>`（chaosLevel, preferredModel） |
| PATCH | `/api/v1/users/me/engine-config` | 更新引擎配置 | Header: `X-User-Id`；Body: `UpdateEngineConfigRequest`（chaosLevel 0.1~1.0, preferredModel?） | `Result<EngineConfigDTO>` |
| PATCH | `/api/v1/users/me/theme` | 切换主题（含 VIP 校验） | Header: `X-User-Id`；Body: `UpdateThemeRequest`（themeId） | `Result<?>` |

---

### 3.11 心跳 — `/api/v1/heartbeat`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/heartbeat/chapter-generate` | 生成章节前心跳 | 用户从 Security 上下文解析；计费策略更新互动计数，满足条件时返回 **HTTP 451** + `code=451`, `data.needAd=true`, `data.adToken`（一次性令牌） | 正常：200；需广告：451，前端展示广告后携带 adToken 调 **广告回调** |

---

### 3.12 广告 — `/api/v1/ad`

| 方法 | 完整路径 | 说明 | 请求 | 响应 |
|------|----------|------|------|------|
| POST | `/api/v1/ad/callback` | 广告观看完成回调 | Body: `AdCallbackRequest`（**adToken**，来自 451 响应） | 核销令牌并设置 ad:watched，下次心跳可继续生成；无效/过期 token：400 |

---

## 四、全局异常与业务码

**GlobalExceptionHandler**（`@RestControllerAdvice`）将异常统一转为 `Result`：

| 异常 | HTTP 状态 | code | 说明 |
|------|------------|------|------|
| AdTriggerRequiredException | 451 | 451 | 需插屏广告，`data.needAd=true`、`data.adToken`（一次性令牌，供 /ad/callback 核销） |
| SyncConflictException | 409 | 409 | 同步冲突（如乐观锁），前端可做合并/冲突页 |
| InsufficientTokenException | 402 | 402 | 灵能水晶/Token 不足 |
| IllegalArgumentException | 400 | 400 | 参数或业务校验失败 |
| MethodArgumentNotValidException / BindException | 400 | 400 | 请求体验证失败，message 为字段级错误 |
| Exception | 500 | 500 | 未分类异常 |

**ContentViolationException**：在 EngineController 内捕获，通过 SSE 发送 `error` 事件后结束流，不经过 GlobalExceptionHandler。

---

## 五、领域层：服务、依赖与能力

### 5.1 资产领域（asset）

| 服务 | 路径 | 依赖 | 主要能力 |
|------|------|------|----------|
| **StoryService** | domain/asset/service/StoryService.java | StoryRepository, SettingPackRepository, SettingPackService, ChapterRepository, UserRepository, WalletTransactionRepository, UserExpService, NotificationService, CreatorPriceValidator, CreatorShareResolver, AssetIdGenerator | 故事 CRUD、按用户+关键词分页、重命名、更新阅读时间、软删、**Fork 故事**（付费分润、设定集三重校验、章节拷贝、作者经验 BE_BOUGHT/BE_FORKED、**REWARD 通知**） |
| **SettingPackService** | domain/asset/service/SettingPackService.java | SettingPackRepository, UserRepository, UserExpService, CreatorPriceValidator, AssetIdGenerator | 设定集 CRUD、按用户分页、归属校验、**Fork 设定集**（allowDownload/allowModify）、**update 乐观锁**（versionId），冲突抛 SyncConflictException |
| **SquareService** | domain/asset/service/SquareService.java | StoryRepository, SettingPackRepository, UserRepository, UserExpService | 广场故事/设定列表（仅 is_public + NORMAL），排序 NEWEST/HOT/TRAFFIC（TRAFFIC 依赖 traffic_weight）、关键词；访问广场可加经验 FETCH_FROM_SQUARE |
| **TrafficWeightComputeService** | domain/asset/service/TrafficWeightComputeService.java | StoryRepository, UserRepository | 定时任务：为故事计算流量权重并写库（公式：(likesCount + forkCount*2) * 创作者等级系数） |

### 5.2 计费领域（billing）

| 组件 | 路径 | 说明 |
|------|------|------|
| **BillingStrategy** | domain/billing/strategy/BillingStrategy.java | 接口：`beforeChapterGeneration(UserDO)`，预扣或计数，可抛 AdTriggerRequiredException |
| **FreeUserBillingStrategy** | domain/billing/strategy/FreeUserBillingStrategy.java | 原子扣平台 Token（UserRepository.deductTokenBalance），防并发超扣 |
| **ByokUserBillingStrategy** | domain/billing/strategy/ByokUserBillingStrategy.java | 不扣 Token，只做互动计数与广告触发判断；若已通过 /ad/callback 核销则本次不抛 451 |
| **CreatorPriceValidator** | domain/billing/CreatorPriceValidator.java | 按用户等级/是否黄金创作者校验故事定价 |
| **CreatorShareResolver** | domain/billing/CreatorShareResolver.java | 作者分润比例（等级/黄金创作者） |
| **VipValidator / VipRenewalService** | domain/billing/service/ | VIP 有效期校验与续期；主题切换时校验 VIP 主题权限 |

### 5.3 章节领域（chapter）

| 服务 | 路径 | 依赖 | 主要能力 |
|------|------|------|----------|
| **ChapterGenerationService** | domain/chapter/service/ChapterGenerationService.java | VectorMemoryService | 拼装 L1（设定集五大维度）、L2（最近 N 章滑动窗口）、L3（RAG 回忆），产出系统提示 |
| **PurificationEngine** | domain/chapter/PurificationEngine.java | — | 内容净化与违禁检测（含 memetic hazard）；流式文本清洗；`hasMemeticHazard(String)` 供入口校验 |

### 5.4 社交领域（social）

| 服务 | 路径 | 依赖 | 主要能力 |
|------|------|------|----------|
| **InteractionService** | domain/social/service/InteractionService.java | AssetLikeRepository, AssetFavoriteRepository, StoryRepository, SettingPackRepository, CommentRepository, UserExpService | 点赞/收藏 toggle，更新 Story/Setting/Comment 的 likesCount 或 favoriteCount；作者获得 BE_LIKED/BE_FAVORITED 经验 |
| **CommentService** | domain/social/service/CommentService.java | CommentRepository, StoryRepository, UserRepository, NotificationService, UserExpService | 发表评论/回复、按锚点分页一级评论、楼中楼、单条查询、软删；解析 @提及并 **sendMention** 通知；被评作者 BE_COMMENTED 经验 |
| **ShareService** | domain/social/service/ShareService.java | ShareRecordRepository, StoryRepository, UserExpService | 生成短码、写 ShareRecord、返回 deepLink 与 forwardCardPayload；decode(shortCode) 返回 ShareTarget；使用 `realmcrafter.share.base-url` |
| **NotificationService** | domain/social/service/NotificationService.java | SystemNotificationRepository | **listNotifications**(userId, type, pageable)；**markAsRead**(id, userId)、**markAllAsRead**(userId)；**sendMention**、**sendLevelUp**、**sendReward**（作品被叉时给作者发 REWARD 通知） |
| **MessageService** | domain/social/service/MessageService.java | MessageRepository, MessageSessionRepository | **sendMessage**(senderId, receiverId, type, content)：落库 MessageDO，更新/创建双方 MessageSessionDO，接收方 unreadCount+1；**getChatHistory**(userId, peerId, pageable)；**getRecentSessions**(userId, pageable) |

### 5.5 用户领域（user）

| 服务 | 路径 | 依赖 | 主要能力 |
|------|------|------|----------|
| **UserExpService** | domain/user/service/UserExpService.java | UserRepository, UserExpLogRepository, NotificationService | 按 ExpAction 增加经验、写 UserExpLogDO、可能升级用户等级并 **sendLevelUp**；被 Story/Setting/Comment/Interaction/Share/Chapter/Fork 等多处调用 |
| **ExpAction** | domain/user/ExpAction.java | — | 行为与单次经验映射：PUBLISH_*, READ_CONSUME, FETCH_FROM_SQUARE, FORK_ASSET, BE_MENTIONED, BE_LIKED, BE_FAVORITED, BE_SHARED, BE_COMMENTED, BE_FORKED, BE_BOUGHT, VIP_RENEW 等 |

---

## 六、应用层编排

| 服务 | 路径 | 说明 |
|------|------|------|
| **ChapterApplicationService** | application/chapter/ChapterApplicationService.java | 流程：敏感词/违禁校验 → 故事归属校验 → **StoryGenerationLock** 加锁 → 解析 BillingStrategy（Free/Byok）→ **beforeChapterGeneration** 预扣/计数 → 拼装 L1+L2+L3（ChapterGenerationService）→ 调用 **LlmClient.stream** → PurificationEngine 清洗流式内容 → 回调 content/branches/done → 落库 Chapter + 更新 Story.lastChapterIndex → 向量索引（VectorMemoryService）→ 经验 READ_CONSUME → 解锁。依赖：SensitiveWordTrie, StoryRepository, SettingPackRepository, ChapterRepository, UserRepository, ChapterGenerationService, LlmClient, StoryGenerationLock, List&lt;BillingStrategy&gt;, VectorMemoryService, UserExpService。 |
| **HeartbeatService** | application/heartbeat/HeartbeatService.java | 从 Security 上下文取当前用户 → 按用户选 BillingStrategy → beforeChapterGeneration（可抛 AdTriggerRequiredException）→ 持久化用户。 |
| **ThemeApplicationService** | application/user/ThemeApplicationService.java | 校验 themeId 合法性及 VIP 主题权限（VipValidator）→ 更新 User.currentThemeId。 |

---

## 七、基础设施：持久化实体与表

| 实体类 | 表名 | 主要字段与说明 |
|--------|------|----------------|
| UserDO | user | 用户：等级、灵能水晶、VIP、主题、是否 BYOK 等 |
| UserConfigDO | user_config | 用户引擎配置：混沌度、偏好模型 |
| StoryDO | story | 故事：settingPackId、sourceStoryId、状态、公开、价格、点赞/Fork 数、traffic_weight、最后阅读时间等 |
| SettingPackDO | setting_pack | 设定集：content(JSON)、allowDownload/allowModify、sourceSettingId、versionId、点赞/Fork 数 |
| ChapterDO | chapter | 章节：storyId、chapterIndex、content、branchesData |
| CommentDO | comment | 评论：storyId、chapterId、userId、content、targetType/targetRef、rootCommentId、parentCommentId、likesCount、replyCount |
| AssetLikeDO | asset_like | 点赞记录：userId、assetType(STORY/SETTING/COMMENT)、assetId |
| AssetFavoriteDO | asset_favorite | 收藏记录：userId、assetType、assetId |
| ShareRecordDO | share_record | 分享短链：short_code(PK)、type、storyId、chapterId、targetRef、excerpt |
| SystemNotificationDO | system_notification | 系统通知：userId、type(SYSTEM/MENTION/INTERACTION/REWARD/LEVEL_UP)、title、body、refType、refId、actorUserId、isRead、createTime |
| MessageDO | message | 私信：senderId、receiverId、msgType(TEXT/FORWARD_CARD)、content、isRead、createTime |
| MessageSessionDO | message_session | 会话摘要：userId、peerId、lastMessageId、lastMessagePreview、lastMessageAt、unreadCount、createTime、updateTime；唯一 (user_id, peer_id) |
| WalletTransactionDO | wallet_transaction | 钱包流水：userId、amount、type(CREATOR_REVENUE 等)、description |
| UserExpLogDO | user_exp_log | 经验日志：userId、exp、action、createTime |
| AdViewLogDO | ad_view_log | 广告观看日志 |

---

## 八、Repository 与自定义查询（选列）

| Repository | 路径 | 主要方法 |
|------------|------|----------|
| StoryRepository | infrastructure/persistence/repository/StoryRepository.java | 按用户+状态分页、按设定集+状态、关键词、公开故事列表（含 TRAFFIC、关键词） |
| SettingPackRepository | .../SettingPackRepository.java | 按用户分页、公开设定列表、公开+关键词 |
| ChapterRepository | .../ChapterRepository.java | 按 storyId 正序章节、最近 N 章（L2 窗口）、按 storyId+chapterIndex |
| CommentRepository | .../CommentRepository.java | 按锚点分页一级评论、按 rootCommentId 楼中楼 |
| SystemNotificationRepository | .../SystemNotificationRepository.java | findByUserIdOrderByCreateTimeDesc、findByUserIdAndTypeOrderByCreateTimeDesc、markAsReadByIdAndUserId、markAllAsReadByUserId |
| MessageRepository | .../MessageRepository.java | findChatBetween(userId, peerId, pageable) |
| MessageSessionRepository | .../MessageSessionRepository.java | findByUserIdOrderByLastMessageAtDesc、findByUserIdAndPeerId |
| AssetLikeRepository / AssetFavoriteRepository | .../ | 按 userId+assetType+assetId 查询等 |
| ShareRecordRepository | .../ShareRecordRepository.java | 按 shortCode 查询 |
| UserRepository / UserConfigRepository / WalletTransactionRepository / UserExpLogRepository / AdViewLogRepository | .../ | JPA 标准 + 业务所需方法 |

---

## 九、基础设施：LLM、Redis、向量、ID

| 组件 | 路径 | 说明 |
|------|------|------|
| LlmClient | infrastructure/llm/LlmClient.java | 接口：流式请求，返回 StreamChunk（CONTENT/BRANCHES/DONE）及消耗 token |
| DeepSeekClient | infrastructure/llm/DeepSeekClient.java | 实现：DeepSeek API、BYOK 支持；可触发 ContentViolationException |
| StoryGenerationLock | infrastructure/redis/StoryGenerationLock.java | 按 storyId 加锁，防止同一故事并发生成 |
| AdCounterCache | infrastructure/redis/AdCounterCache.java | 广告计数相关缓存 |
| VectorMemoryService | infrastructure/vector/VectorMemoryService.java | 章节内容向量索引，供 L3 RAG 回忆；含 Mock 实现 |
| AssetIdGenerator | infrastructure/id/AssetIdGenerator.java | 生成故事（gs 前缀）、设定集（sd 前缀）等 ID |
| SensitiveWordTrie | security/audit/SensitiveWordTrie.java | 敏感词 Trie，章节生成与审计中使用 |

---

## 十、功能逻辑关系（调用关系简图）

```
[ 前端 ]
    │
    ├─ GET/POST /api/v1/stories ──────────────► StoryController ──► StoryService
    │       (Fork 时) ────────────────────────────────────────────► NotificationService.sendReward
    │
    ├─ GET/POST/PUT /api/v1/settings ─────────► SettingPackController ──► SettingPackService
    │
    ├─ GET /api/v1/square/* ───────────────────► SquareController ──► SquareService ──► UserExpService (FETCH_FROM_SQUARE)
    │
    ├─ POST /api/v1/engine/generate/stream ────► EngineController ──► ChapterApplicationService
    │       ──► StoryGenerationLock / BillingStrategy / ChapterGenerationService / LlmClient
    │       ──► PurificationEngine / VectorMemoryService / UserExpService (READ_CONSUME)
    │
    ├─ POST /api/v1/interactions/* ────────────► InteractionController ──► InteractionService ──► UserExpService (BE_LIKED/BE_FAVORITED)
    │
    ├─ POST/GET/DELETE /api/v1/comments ───────► CommentController ──► CommentService ──► NotificationService.sendMention, UserExpService (BE_COMMENTED)
    │
    ├─ POST/GET /api/v1/share/* ───────────────► ShareController ──► ShareService
    │
    ├─ GET/PATCH /api/v1/notifications ────────► NotificationController ──► NotificationService
    │
    ├─ POST/GET /api/v1/messages/* ────────────► MessageController ──► MessageService (MessageDO + MessageSessionDO)
    │
    ├─ GET/PATCH /api/v1/users/me/* ───────────► UserConfigController / UserPreferenceController ──► ThemeApplicationService (VIP 校验)
    │
    ├─ POST /api/v1/heartbeat/chapter-generate ► HeartbeatController ──► HeartbeatService ──► BillingStrategy (可 451，带 adToken)
    └─ POST /api/v1/ad/callback ───────────────► AdController ──► AdWatchTokenService（核销 adToken，设置 ad:watched）
```

- **经验与等级**：UserExpService 被 Story（Fork）、Setting、Comment、Interaction、Share、Chapter（生成）、Square（访问）等调用；升级时 NotificationService.sendLevelUp。
- **通知**：NotificationService 被 CommentService（@提及）、StoryService（Fork 后 REWARD）、UserExpService（等级跃迁）调用。
- **私信**：MessageService 独立使用 MessageDO/MessageSessionDO；FORWARD_CARD 的 content 可与 Share 的 forwardCardPayload 对接，由前端组合后发送。

---

## 十一、数据库迁移（db/migration）

| 脚本 | 说明 |
|------|------|
| V3__add_story_bookshelf_fields.sql | 故事表书架相关字段（如 tags） |
| V4__add_likes_fork_count.sql | 点赞数、Fork 数等 |
| V5__add_setting_allow_download.sql | 设定集 allow_download 等 |
| V6__add_comment_share_notification.sql | comment、share_record、system_notification 表及索引 |
| V7__add_creator_exp_and_level.sql | 创作者经验与等级 |
| V8__add_story_traffic_weight.sql | 故事 traffic_weight 字段 |
| V9__add_message_tables.sql | message、message_session 表及索引 |

---

## 十二、已实现能力小结

- **故事**：创建、列表、详情、重命名、阅读时间、Fork（付费/免费、设定集策略、REWARD 通知）、软删。  
- **设定集**：创建、列表、详情、更新（乐观锁）、Fork。  
- **广场**：公开故事/设定列表，NEWEST/HOT/TRAFFIC 排序与关键词搜索。  
- **AI 章节**：流式生成（SSE）、L1/L2/L3 上下文、计费预扣、加锁、净化、向量索引。  
- **社交**：点赞/收藏（故事、设定、评论）、评论与楼中楼、@提及与系统通知、分享短链与解码。  
- **通知**：分页列表（按类型）、单条/全部已读；Fork 时 REWARD 通知。  
- **私信**：发送（TEXT/FORWARD_CARD）、会话列表、与某人聊天记录。  
- **用户**：引擎配置（混沌度、模型）、主题切换（含 VIP）、经验与等级。  
- **心跳**：生成前心跳、计费策略与广告触发（451，响应含 adToken）。  
- **广告回调**：POST /api/v1/ad/callback 核销 adToken，标记已观看，下次心跳放行。  
- **全局**：统一 Result、异常码（409/402/451/400/500）、敏感词与内容净化。  

---

## 十三、审查结论与已修复风险（2026-02-28）

- **广告闭环**：已补充广告观看完成回调接口 `/api/v1/ad/callback`；451 响应现含一次性 `adToken`，前端展示广告后携带 token 调用回调，后端核销并设置 ad:watched，下次心跳不再抛 451。  
- **并发扣费**：免费用户 Token 扣减改为 `UserRepository.deductTokenBalance`（UPDATE ... WHERE balance >= amount），保证原子性，防止并发超扣。  
- **购买与分润**：当前购买通过「Fork 故事/设定集」完成扣款与分润；独立「购买作品」接口可按需在后续迭代补充。  
- **索引**：V6 已包含 comment 的 `(story_id, chapter_id, target_type, target_ref)` 与 `root_comment_id` 索引；广场 TRAFFIC 排序若需可对 `story.traffic_weight` 增加索引（见迁移脚本）。  
- **后续建议**：RAG 延迟可加本地缓存；流式内容可考虑「先审后发」缓冲；私信可考虑 WebSocket 实时推送；广告计数间隔（如每 10 章）建议可配置化。

以上为 RealmCrafter 后端 Server 的完整架构、功能逻辑关系、详细路径与接口说明；具体字段与枚举以源码和迁移脚本为准。
