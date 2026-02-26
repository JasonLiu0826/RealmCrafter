# RealmCrafter Client

前端工程：React 18 + TypeScript + Vite，样式使用 TailwindCSS。

## 开发

```bash
npm install
npm run dev
```

开发服务器默认 `http://localhost:5173`，API 代理到后端 `http://localhost:8080`。

## 环境变量

可选：在项目根目录创建 `.env` 或 `.env.local`，覆盖后端地址：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 目录结构（src/）

按《总体架构设计.md》规划：

- **assets/** — 全局静态资源（主题色、占位图、SVG 等）
- **components/** — 全局复用组件
  - glassmorphism/ — 毛玻璃 UI
  - waterfall/ — 广场双列瀑布流
  - chat/ — IM 富文本资产卡片气泡
- **constants/** — 业务枚举（计费、提示词、MAX_TOKEN 等）
- **hooks/** — 自定义钩子（流式打字、震动等）
- **pages/** — 顶层页面（5 大 Tab）
  - home, square, publish, message, profile, reader
- **services/** — 网络与模型请求
  - api.ts — Axios 封装，baseURL 指向后端 8080
  - ai.ts — AI 调度（后续）
- **store/** — 全局状态切片（User, Engine, ActiveStory）
- **utils/** — 工具（流解码、文本压缩等）

## 构建与预览

```bash
npm run build
npm run preview
```
