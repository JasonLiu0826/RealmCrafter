# RealmCrafter V2.0 架构升级 — 任务完成总结

## 一、任务 1：数据库映射与实体更新（已确认）

- **UserDO**：已包含 `is_byok`、`interaction_counter`、`ad_free_expire_time`
- **UserConfigDO**：已创建，对应 `user_config` 表（preferred_model、chaos_level、memory_depth）
- **AdViewLogDO**：已创建，对应 `ad_view_log` 表（ad_platform、ad_type、reward_granted）
- **SettingPackDO** / **StoryDO**：已包含 `simhash` 字段
- **UserConfigRepository**、**AdViewLogRepository**：已存在

---

## 二、任务 2：前端安全与广告状态管理（已完成）

| 项 | 路径 | 说明 |
|----|------|------|
| useKeystore.ts | `RealmCrafter_client/src/hooks/useKeystore.ts` | BYOK API Key 仅存本地，适配器可替换为加密存储 |
| useConfigStore.ts | `RealmCrafter_client/src/store/useConfigStore.ts` | 混沌阈值、首选模型、是否 BYOK，持久化 |
| useAdTriggerStore.ts | `RealmCrafter_client/src/store/useAdTriggerStore.ts` | 广告触发状态，供 api 拦截器与 useAdManager 使用 |
| AdBreakScreen.tsx | `RealmCrafter_client/src/components/ads/AdBreakScreen.tsx` | 灵能补给插屏 UI，占位广告位，支持“我已看完”/“稍后再说” |
| useAdManager.ts | `RealmCrafter_client/src/hooks/useAdManager.ts` | 监听 adTriggered，返回 showAdBreak / onAdComplete / onAdDismiss |
| api.ts | `RealmCrafter_client/src/services/api.ts` | 响应拦截：status 209 或 data.needAd === true 时设置 adTriggered |
| App.tsx | 挂载 AdBreakScreen | 根组件使用 useAdManager，全局展示插屏 |

---

## 三、任务 3：心跳与计费策略（已完成）

### 3.1 基础设施

- **AdCounterCache**  
  `infrastructure/redis/AdCounterCache.java`  
  - Redis key：`ad:counter:{userId}`  
  - `increment(userId)`、`get(userId)`、`reset(userId)`  
  - TTL 30 天  

### 3.2 领域与异常

- **BillingResult**  
  `domain/billing/BillingResult.java`  
  - needAd、success、message  
  - 静态方法：`ok()`、`needAd()`

- **AdTriggerRequiredException**  
  `domain/billing/AdTriggerRequiredException.java`  
  - 由 GlobalExceptionHandler 捕获，返回 HTTP 209 + body `{ needAd: true }`

- **InsufficientTokenException**  
  `domain/billing/InsufficientTokenException.java`  
  - 免费用户 Token 不足时抛出，返回 402

### 3.3 策略模式

- **BillingStrategy**  
  `domain/billing/strategy/BillingStrategy.java`  
  - `isByok()`、`beforeChapterGeneration(UserDO user)` → BillingResult

- **FreeUserBillingStrategy**  
  `domain/billing/strategy/FreeUserBillingStrategy.java`  
  - 扣减 Token（可配置 `realmcrafter.billing.tokens-per-chapter`，默认 5000）  
  - Redis 互动计数 +1  
  - 若 `count % 10 == 0` 且无免广告则返回 needAd

- **ByokUserBillingStrategy**  
  `domain/billing/strategy/ByokUserBillingStrategy.java`  
  - 不扣 Token，仅 Redis 计数 +1  
  - 若 `count % 10 == 0` 且无免广告则返回 needAd

### 3.4 应用层与 API

- **HeartbeatService**  
  `application/heartbeat/HeartbeatService.java`  
  - 按 user.byokEnabled 选择策略  
  - 调用 `strategy.beforeChapterGeneration(user)`  
  - needAd 时抛出 AdTriggerRequiredException  
  - 免费用户扣费后写回 User（userRepository.save）

- **HeartbeatController**  
  `api/heartbeat/HeartbeatController.java`  
  - `POST /api/v1/heartbeat`，Header：`X-User-Id`  
  - 调用 HeartbeatService；若抛 AdTriggerRequiredException 则由全局处理返回 209

### 3.5 配置与全局异常

- **application.yml**  
  - `spring.redis`（host/port/password/database）  
  - `realmcrafter.billing.tokens-per-chapter: 5000`

- **GlobalExceptionHandler**  
  - AdTriggerRequiredException → `ResponseEntity.status(209).body(Result with needAd: true)`  
  - InsufficientTokenException → 402

---

## 四、任务 4：安全审计 — Trie 敏感词过滤（已完成）

- **SensitiveWordTrie**  
  `security/audit/SensitiveWordTrie.java`  
  - 字典树：addWord、containsAny、replaceSensitiveWords  
  - 用于请求体敏感词检测

- **CachingRequestBodyWrapper**  
  `security/audit/CachingRequestBodyWrapper.java`  
  - 包装 HttpServletRequest，首次读取时缓存 body，支持重复读  
  - 供 Filter 前置检测与后续 Controller 共用同一 body

- **TrieFilterInterceptor**  
  `security/audit/TrieFilterInterceptor.java`  
  - 实现 `Filter`，Order(1)  
  - 仅对 POST/PUT/PATCH 且 Content-Type 为 text/* 或 application/json 的请求做 body 检查  
  - 使用 CachingRequestBodyWrapper 读取 body → SensitiveWordTrie.containsAny  
  - 若命中敏感词：400 + “内容包含违规词汇”，不进入 Controller  
  - 默认敏感词列表：DEFAULT_SENSITIVE_WORDS（可改为配置/数据库加载）

---

## 五、配置与运行说明

- **数据库**：执行你提供的 V2 增量 SQL；如需修改密码，在 `application.yml` 中设置 `spring.datasource.password`（例如你的本地密码）。
- **Redis**：需本地或远程 Redis；在 `application.yml` 中配置 `spring.redis`。
- **前端**：后端返回 209 或 `data.needAd === true` 时，前端会弹出灵能补给插屏；心跳接口为 `POST /api/v1/heartbeat`，Header `X-User-Id`。

---

## 六、后续可扩展项（未在本次实现）

- **domain/asset/service/SimHashService**：洗稿检测（发布时比对库内指纹）
- **domain/chapter/service/MemoryRerankService**：L3 向量召回后的重排序
- **api/ad**：广告看完后的 Callback（发放 Token/免广告时长）
- **infrastructure/payment**：Stripe / Ping++ 分账
- **security/audit/SseModerationWorker**：50 字符滑动窗口异步机审（第二道防线）
- **前端**：heartbeat.ts 在章节生成前/后调用心跳；RewardVideoModal；useAIStream 断流重试；Config 页 BYOK 与混沌阈值；Reader 页 SSE 熔断 Glitch 动效

以上为 RealmCrafter V2.0 任务 1～4 的落地总结与后续可扩展点。
