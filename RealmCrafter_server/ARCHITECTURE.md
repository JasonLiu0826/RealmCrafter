# RealmCrafter 后端 Server 架构与接口说明

> 基于对 `RealmCrafter_server` 的遍历整理，包含架构、功能逻辑关系、路径、接口及实现/查询能力说明。

---

## 一、项目概览与技术栈

- **项目名**：realmcrafter-server  
- **描述**：RealmCrafter AIGC 互动剧情与 UGC 社区后端服务  
- **技术栈**：
  - **框架**：Spring Boot 2.7.18，Java 17  
  - **Web**：spring-boot-starter-web + spring-boot-starter-webflux（流式 SSE）  
  - **数据**：Spring Data JPA + MySQL（HikariCP）  
  - **安全**：Spring Security + JWT（jjwt 0.11.5）  
  - **缓存**：Spring Data Redis  
  - **消息**：Spring AMQP（RabbitMQ）  
  - **校验**：spring-boot-starter-validation  
  - **工具**：Lombok  

- **配置入口**：`src/main/resources/application.yml`  
  - 服务端口：8080  
  - 数据库：`jdbc:mysql://localhost:3306/realmcrafter`  
  - Redis：localhost:6379  
  - 计费：`realmcrafter.billing.tokens-per-chapter: 5000`  
  - 分享：`realmcrafter.share.base-url: https://realmcrafter.app`  

- **启动类**：`com.realmcrafter.RealmCrafterApplication`（`@SpringBootApplication` + `@EnableScheduling`）

---

## 二、目录与包结构（src/main/java）

```
com.realmcrafter
├── RealmCrafterApplication.java          # 启动类
├── api/                                   # 对外 HTTP 接口层
│   ├── dto/
│   │   └── Result.java                    # 统一响应 Result<T>
│   ├── asset/                             # 资产：故事、设定集、广场
│   │   ├── StoryController.java
│   │   ├── SettingPackController.java
│   │   ├── SquareController.java
│   │   └── dto/
│   │       ├── CreateStoryRequest.java
│   │       ├── RenameStoryRequest.java
│   │       ├── CreateSettingRequest.java
│   │       └── UpdateSettingRequest.java
│   ├── engine/                            # AI 章节生成
│   │   ├── EngineController.java
│   │   └── dto/
│   │       └── GenerateStreamRequest.java
│   ├── social/                            # 社交：点赞、收藏、评论、分享
│   │   ├── InteractionController.java
│   │   ├── CommentController.java
│   │   ├── ShareController.java
│   │   └── dto/
│   │       ├── AddCommentRequest.java
│   │       └── GenerateShareRequest.java
│   ├── user/                              # 用户偏好与引擎配置
│   │   ├── UserConfigController.java      # 引擎配置（混沌度、模型）
│   │   ├── UserPreferenceController.java # 主题切换
│   │   └── dto/
│   │       ├── EngineConfigDTO.java
│   │       ├── UpdateEngineConfigRequest.java
│   │       └── UpdateThemeRequest.java
│   └── heartbeat/
│       └── HeartbeatController.java      # 生成前心跳（广告触发）
├── application/                           # 应用服务（编排领域 + 基础设施）
│   ├── chapter/
│   │   └── ChapterApplicationService.java # 章节流式生成编排
│   ├── user/
│   │   └── ThemeApplicationService.java    # 主题切换（含 VIP 校验）
│   └── heartbeat/
│       └── HeartbeatService.java         # 心跳逻辑（计费策略 + 广告触发）
├── config/                                # 全局配置与异常
│   ├── GlobalExceptionHandler.java       # 统一异常 -> Result
│   ├── SyncConflictException.java       # 409 同步冲突
│   └── ContentViolationException.java    # 内容违规
├── domain/                                # 领域层
│   ├── asset/                            # 资产领域
│   │   ├── service/
│   │   │   ├── StoryService.java
│   │   │   ├── SettingPackService.java
│   │   │   ├── SquareService.java
│   │   │   └── TrafficWeightComputeService.java
│   │   └── dto/
│   │       └── SettingContentDTO.java
│   ├── billing/                          # 计费与分成
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
│   ├── chapter/                          # 章节生成领域
│   │   ├── PurificationEngine.java       # 内容净化/违禁检测
│   │   └── service/
│   │       └── ChapterGenerationService.java  # L1/L2/L3 提示拼装
│   ├── social/                           # 社交领域
│   │   └── service/
│   │       ├── InteractionService.java   # 点赞/收藏
│   │       ├── CommentService.java       # 评论 + @提及通知
│   │       ├── ShareService.java         # 短链与转发卡片
│   │       └── NotificationService.java
│   └── user/
│       ├── ExpAction.java                # 经验行为枚举
│       └── service/
│           └── UserExpService.java       # 经验与等级
├── infrastructure/                        # 基础设施
│   ├── persistence/
│   │   ├── entity/                       # JPA 实体（见下表）
│   │   └── repository/                   # JPA Repository 接口
│   ├── id/
│   │   └── AssetIdGenerator.java         # 故事/设定集 ID 生成
│   ├── llm/                              # LLM 调用
│   │   ├── LlmClient.java
│   │   ├── DeepSeekClient.java
│   │   └── dto/
│   │       ├── LlmStreamRequest.java
│   │       └── StreamChunk.java
│   ├── redis/
│   │   ├── StoryGenerationLock.java      # 故事级生成锁
│   │   └── AdCounterCache.java           # 广告计数缓存
│   ├── vector/
│   │   ├── VectorMemoryService.java     # RAG/L3 记忆
│   │   └── MockVectorService.java
│   └── mq/                               # 消息队列（包占位）
└── security/
    └── audit/
        ├── SensitiveWordTrie.java        # 敏感词过滤
        ├── CachingRequestBodyWrapper.java
        └── TrieFilterInterceptor.java
```

---

## 三、API 接口清单（路径、方法、功能）

所有接口统一返回体为 `Result<T>`（`code`、`message`、`data`），成功时 `code=0`。  
需登录的接口通过请求头 `X-User-Id` 或 Spring Security 上下文传递用户身份（见各 Controller 注释）。

### 3.1 故事（书架）— `/api/v1/stories`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| GET | `/api/v1/stories` | 分页列表当前用户故事，支持关键词 | Query: `page`, `size`, `keyword`；Header: `X-User-Id`；返回 `Page<StoryDO>` |
| GET | `/api/v1/stories/{id}` | 故事详情（本人或公开） | Path: `id`；可选 Header: `X-User-Id`；返回 `StoryDO` |
| POST | `/api/v1/stories` | 创建故事 | Body: `CreateStoryRequest`（userId, settingPackId, title, cover, description, price）；返回 `StoryDO` |
| PATCH | `/api/v1/stories/{id}/rename` | 重命名 | Body: `RenameStoryRequest`（userId, title） |
| PATCH | `/api/v1/stories/{id}/read` | 更新最后阅读时间 | Header: `X-User-Id` |
| POST | `/api/v1/stories/{id}/fork` | Fork 故事（含设定集策略与付费分润） | Header: `X-User-Id`；返回新 `StoryDO` |
| DELETE | `/api/v1/stories/{id}` | 软删除故事 | Header: `X-User-Id` |

### 3.2 设定集 — `/api/v1/settings`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| GET | `/api/v1/settings` | 分页列表当前用户设定集 | Query: `page`, `size`；Header: `X-User-Id`；返回 `Page<SettingPackDO>` |
| POST | `/api/v1/settings` | 创建设定集 | Header: `X-User-Id`；Body: `CreateSettingRequest`（title, cover, description, content, deviceHash, allowDownload, allowModify, price） |
| GET | `/api/v1/settings/{id}` | 设定集详情（校验归属） | Header: `X-User-Id` |
| PUT | `/api/v1/settings/{id}` | 更新设定集（乐观锁 versionId） | Body: `UpdateSettingRequest`（含 versionId） |
| POST | `/api/v1/settings/{id}/fork` | Fork 设定集（需 allowDownload） | Header: `X-User-Id` |

### 3.3 广场发现 — `/api/v1/square`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| GET | `/api/v1/square/stories` | 公开故事列表（is_public + NORMAL） | Query: `sort`(NEWEST/HOT/TRAFFIC), `keyword`, `page`, `size`；可选 `X-User-Id`；返回 `Page<StoryDO>` |
| GET | `/api/v1/square/settings` | 公开设定集列表 | Query: `sort`(NEWEST/HOT), `keyword`, `page`, `size`；返回 `Page<SettingPackDO>` |

### 3.4 AI 章节生成 — `/api/v1/engine`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| POST | `/api/v1/engine/generate/stream` | 流式生成一章（SSE） | `Content-Type: text/event-stream`；Body: `GenerateStreamRequest`（storyId, userChoice, chaosLevel, useByok）；Header: `X-User-Id` 必填；事件：content / branches / [DONE] 或 error |

### 3.5 社交互动 — `/api/v1/interactions`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| POST | `/api/v1/interactions/like` | 点赞/取消赞（幂等） | Body: `{ "type": "STORY|SETTING|COMMENT", "id": "..." }`；返回 `{ liked, type, id }` |
| POST | `/api/v1/interactions/favorite` | 收藏/取消收藏（幂等） | Body: `{ "type", "id" }`；返回 `{ favorited, type, id }` |

### 3.6 评论 — `/api/v1/comments`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| POST | `/api/v1/comments` | 发表评论或回复 | Body: `AddCommentRequest`（storyId, chapterId, content, targetType, targetRef, parentCommentId, mentionedUserIds）；支持 @提及 与通知 |
| GET | `/api/v1/comments/anchor` | 按锚点分页一级评论 | Query: `storyId`, `chapterId`, `targetType`(PARAGRAPH/OPTION), `targetRef`, `page`, `size`；返回 `Page<CommentDO>` |
| GET | `/api/v1/comments/replies/{rootCommentId}` | 某条顶级评论的楼中楼 | 返回 `List<CommentDO>` |
| GET | `/api/v1/comments/{commentId}` | 单条评论 | 不存在时 404 |
| DELETE | `/api/v1/comments/{commentId}` | 软删除（仅作者） | Header: `X-User-Id` |

### 3.7 分享 — `/api/v1/share`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| POST | `/api/v1/share/generate` | 生成分享短链与站内转发卡片 | Body: `GenerateShareRequest`（type, storyId, chapterId, targetRef, excerpt）；返回 shortCode、deepLink、forwardCardPayload |
| GET | `/api/v1/share/decode/{shortCode}` | 解析短链得到锚点信息 | 返回 ShareTarget（type, storyId, chapterId, targetRef, excerpt）；无效 404 |

### 3.8 用户 — `/api/v1/users/me` 与引擎配置

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| GET | `/api/v1/users/me/engine-config` | 获取引擎配置 | Header: `X-User-Id`；返回 `EngineConfigDTO`（chaosLevel, preferredModel） |
| PATCH | `/api/v1/users/me/engine-config` | 更新引擎配置 | Body: `UpdateEngineConfigRequest`（chaosLevel 0.1~1.0, preferredModel） |
| PATCH | `/api/v1/users/me/theme` | 切换主题（含 VIP 校验） | Body: `UpdateThemeRequest`（themeId）；Header: `X-User-Id` |

### 3.9 心跳 — `/api/v1/heartbeat`

| 方法 | 路径 | 说明 | 请求/响应要点 |
|------|------|------|----------------|
| POST | `/api/v1/heartbeat/chapter-generate` | 生成章节前心跳 | 用户从 Security 上下文解析；计费策略更新互动计数，满足条件时返回 **451** + `needAd: true`，前端据此拉广告 |

---

## 四、全局异常与业务码

- **GlobalExceptionHandler**（`@RestControllerAdvice`）统一将异常转为 `Result`：
  - **AdTriggerRequiredException** → HTTP 451，`code=451`，`data.needAd=true`
  - **SyncConflictException** → HTTP 409，`code=409`（前端可做合并/冲突页）
  - **InsufficientTokenException** → HTTP 402，`code=402`
  - **IllegalArgumentException** → 400，`code=400`
  - **MethodArgumentNotValidException / BindException** → 400，字段级错误信息
  - **Exception** → 500，`code=500`

---

## 五、领域层与调用关系

### 5.1 资产领域（asset）

- **StoryService**  
  - 依赖：StoryRepository, SettingPackRepository, SettingPackService, ChapterRepository, UserRepository, WalletTransactionRepository, UserExpService, CreatorPriceValidator, CreatorShareResolver, AssetIdGenerator  
  - 能力：故事 CRUD、列表（含关键词）、重命名、更新阅读时间、软删除、**Fork 故事**（付费分润、设定集是否 Fork 的三重校验、章节拷贝）。

- **SettingPackService**  
  - 依赖：SettingPackRepository, UserRepository, UserExpService, CreatorPriceValidator, AssetIdGenerator  
  - 能力：设定集 CRUD、按用户列表、按 ID 校验归属、**Fork 设定集**（allowDownload/allowModify 策略）、**update 乐观锁**（versionId），冲突抛 SyncConflictException。

- **SquareService**  
  - 依赖：StoryRepository, SettingPackRepository, UserRepository, UserExpService  
  - 能力：广场故事/设定列表（仅 is_public + NORMAL），排序 NEWEST/HOT/TRAFFIC（TRAFFIC 依赖定时任务写的 traffic_weight），关键词搜索；访问广场可加经验 FETCH_FROM_SQUARE。

- **TrafficWeightComputeService**  
  - 被定时任务调用，为故事计算流量权重并写库（供广场 TRAFFIC 排序）。

### 5.2 计费领域（billing）

- **BillingStrategy**（接口）  
  - `beforeChapterGeneration(UserDO)`：章节生成前扣费或计数，必要时抛 AdTriggerRequiredException。  
- **FreeUserBillingStrategy**：扣平台 Token。  
- **ByokUserBillingStrategy**：不扣 Token，只做互动计数与广告触发判断。  
- **CreatorPriceValidator**：按用户等级/是否黄金创作者校验定价。  
- **CreatorShareResolver**：作者分润比例。  
- **VipValidator / VipRenewalService**：VIP 有效期校验与续期逻辑。  

### 5.3 章节领域（chapter）

- **ChapterGenerationService**  
  - 拼装 L1（设定集五大维度）、L2（最近 N 章滑动窗口）、L3（RAG 回忆），产出系统提示；  
  - 依赖 VectorMemoryService、配置 l2-window-size、l3-top-k。  

- **PurificationEngine**  
  - 内容净化与违禁检测（含 memetic hazard）；流式文本清洗。  

### 5.4 社交领域（social）

- **InteractionService**  
  - 点赞/收藏 toggle，更新 Story/Setting/Comment 的 likesCount 或 favoriteCount；  
  - 依赖 AssetLikeRepository, AssetFavoriteRepository, StoryRepository, SettingPackRepository, CommentRepository, UserExpService。  

- **CommentService**  
  - 发表评论/回复、按锚点分页一级评论、楼中楼列表、单条查询、软删；  
  - 解析 @提及，写系统通知（NotificationService）；  
  - 依赖 CommentRepository, StoryRepository, UserRepository, NotificationService, UserExpService。  

- **ShareService**  
  - 生成短码、写 ShareRecord、返回 deepLink 与 forwardCardPayload；  
  - decode(shortCode) 返回 ShareTarget；  
  - 依赖 ShareRecordRepository, StoryRepository, UserExpService；  
  - 使用 `realmcrafter.share.base-url`。  

### 5.5 用户领域（user）

- **UserExpService**  
  - 按 ExpAction 增加经验、写 UserExpLogDO、可能升级用户等级；  
  - 被 Story/Setting/Comment/Interaction/Share/Chapter 等多处调用。  

---

## 六、应用层编排

- **ChapterApplicationService**  
  - 流程：敏感词/违禁校验 → 故事归属校验 → **StoryGenerationLock** 加锁 → 解析 BillingStrategy（Free/Byok）→ **beforeChapterGeneration** 预扣/计数 → 拼装 L1+L2+L3（ChapterGenerationService）→ 调用 **LlmClient.stream** → PurificationEngine 清洗流式内容 → 回调 content/branches/done → 落库 Chapter + 更新 Story.lastChapterIndex → 向量索引（VectorMemoryService）→ 经验 READ_CONSUME → 解锁。  
  - 依赖：SensitiveWordTrie, StoryRepository, SettingPackRepository, ChapterRepository, UserRepository, ChapterGenerationService, LlmClient, StoryGenerationLock, List&lt;BillingStrategy&gt;, VectorMemoryService, UserExpService。  

- **HeartbeatService**  
  - 从 Security 上下文取当前用户 → 按用户选 BillingStrategy → beforeChapterGeneration（可抛 AdTriggerRequiredException）→ 持久化用户。  

- **ThemeApplicationService**  
  - 校验 themeId 合法性及 VIP 主题权限（VipValidator）→ 更新 User.currentThemeId。  

---

## 七、基础设施

### 7.1 持久化实体与表

| 实体类 | 表名 | 说明 |
|--------|------|------|
| UserDO | user | 用户（等级、水晶、VIP、主题、是否 BYOK 等） |
| UserConfigDO | user_config | 用户引擎配置（混沌度、偏好模型） |
| StoryDO | story | 故事（关联 settingPackId、sourceStoryId、状态、公开、点赞/Fork 数、traffic_weight 等） |
| SettingPackDO | setting_pack | 设定集（content JSON、allowDownload/allowModify、sourceSettingId、versionId） |
| ChapterDO | chapter | 章节（storyId、chapterIndex、content、branchesData） |
| CommentDO | comment | 评论（段落/选项/评论锚点、楼中楼、targetType/targetRef） |
| AssetLikeDO | asset_like | 点赞记录（用户+资产类型+资产ID） |
| AssetFavoriteDO | asset_favorite | 收藏记录 |
| ShareRecordDO | share_record | 分享短链（short_code 为主键，type/storyId/chapterId/targetRef/excerpt） |
| SystemNotificationDO | system_notification | 系统通知（@提及等） |
| WalletTransactionDO | wallet_transaction | 钱包流水（创作者分润等） |
| UserExpLogDO | user_exp_log | 经验日志 |
| AdViewLogDO | ad_view_log | 广告观看日志 |

### 7.2 Repository 与自定义查询（选列）

- **StoryRepository**：按用户+状态分页、按设定集+状态、关键词搜索、公开故事列表（含 TRAFFIC 排序、关键词）。  
- **SettingPackRepository**：按用户分页、公开设定列表、公开+关键词。  
- **ChapterRepository**：按 storyId 正序章节、最近 5 章（L2 窗口）、按 storyId+chapterIndex。  
- **CommentRepository**：按锚点分页一级评论、按 rootCommentId 楼中楼。  
- 其余为 JPA 标准 + 少量自定义方法（如 AssetLikeRepository 按用户+类型+资产ID 查等）。  

### 7.3 LLM 与流式

- **LlmClient** 接口由 **DeepSeekClient** 实现：  
  - 使用 `realmcrafter.llm.deepseek.url/model/api-key`（或 BYOK 的 apiKey）；  
  - 流式请求，解析正文与 branches（约定格式或 JSON）；  
  - 支持 Mock 违禁词触发 ContentViolationException；  
  - 返回消耗的 token 数（当前预扣制下未用于二次结算）。  

### 7.4 Redis

- **StoryGenerationLock**：按 storyId 加锁，防止同一故事并发生成。  
- **AdCounterCache**：广告计数相关缓存。  

### 7.5 其他

- **AssetIdGenerator**：生成故事（gs 前缀）、设定集（sd 前缀）等 ID。  
- **VectorMemoryService**（含 Mock 实现）：章节内容向量索引，供 L3 RAG 回忆。  
- **SensitiveWordTrie**：敏感词 Trie，在章节生成与审计中使用。  

---

## 八、数据库迁移（db/migration）

- V3：故事表增加 tags 等书架字段。  
- V4：点赞数、Fork 数等。  
- V5：设定集 allow_download 等。  
- V6：comment、share_record、system_notification 表及索引。  
- V7：创作者经验与等级。  
- V8：故事 traffic_weight（供广场 TRAFFIC 排序）。  

（具体字段以各 migration 的 SQL 为准。）

---

## 九、实现与查询能力小结

### 已实现能力

- **故事**：创建、列表、详情、重命名、阅读时间、Fork（含付费与设定集策略）、软删。  
- **设定集**：创建、列表、详情、更新（乐观锁）、Fork。  
- **广场**：公开故事/设定列表，NEWEST/HOT/TRAFFIC 排序与关键词搜索。  
- **AI 章节**：流式生成（SSE）、L1/L2/L3 上下文、计费预扣、加锁、净化、向量索引。  
- **社交**：点赞/收藏（故事、设定、评论）、评论与楼中楼、@提及与系统通知、分享短链与解码。  
- **用户**：引擎配置（混沌度、模型）、主题切换（含 VIP）、经验与等级。  
- **心跳**：生成前心跳、计费策略与广告触发（451）。  
- **全局**：统一 Result、异常码（409/402/451/400/500）、敏感词与内容净化。  

### 可查询/可用的数据与接口

- **按用户**：我的故事列表、我的设定集列表、我的引擎配置、我的主题。  
- **按故事**：故事详情、章节列表（通过 ChapterRepository）、评论（按锚点、楼中楼）。  
- **按广场**：公开故事/设定分页（支持排序与关键词）。  
- **按分享**：短链解码得到故事/章节/锚点。  
- **按评论**：单条评论、某根评论下回复。  

以上为 RealmCrafter 后端 Server 的架构、功能逻辑关系、路径与接口及实现/查询能力的详细说明；具体字段与枚举以源码和迁移脚本为准。
