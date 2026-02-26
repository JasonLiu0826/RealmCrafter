# RealmCrafter

一个基于Spring Boot的后端服务项目，用于构建沉浸式的叙事体验平台。

## 项目结构

```
src/main/
├── java/com/realmcrafter/
│   ├── api/              # API接口层
│   ├── application/      # 应用服务层
│   ├── config/          # 配置类
│   ├── domain/          # 领域模型
│   ├── infrastructure/  # 基础设施层
│   └── RealmCrafterApplication.java  # 启动类
└── resources/
    └── application.yml  # 配置文件
```

## 技术栈

- **框架**: Spring Boot 2.x
- **语言**: Java 8+
- **构建工具**: Maven
- **数据库**: (待配置)
- **缓存**: (待配置)

## 快速开始

### 环境要求
- JDK 8 或更高版本
- Maven 3.6+

### 运行项目

```bash
# 克隆项目
git clone https://github.com/JasonLiu0826/RealmCrafter.git

# 进入项目目录
cd RealmCrafter_server

# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

## 配置说明

主要配置文件位于 `src/main/resources/application.yml`

## 开发规范

- 遵循领域驱动设计(DDD)架构模式
- 使用分层架构：API → Application → Domain → Infrastructure
- 统一异常处理机制
- RESTful API 设计风格

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进项目。

## 许可证

[MIT License](LICENSE)

## 联系方式

- 作者: JasonLiu
- 邮箱: 1917869590@qq.com
- GitHub: https://github.com/JasonLiu0826