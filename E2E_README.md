# RealmCrafter E2E 点火冒烟测试

## 方式一：一键运行（推荐，需 Docker）

1. **安装并启动 Docker Desktop**（若尚未安装：<https://www.docker.com/products/docker-desktop/>）
2. 在项目根目录执行：

```powershell
.\run-e2e.ps1
```

脚本会依次：启动 MySQL / Redis / RabbitMQ → 等待 MySQL 就绪 → 启动后端 → 等待 8080 → 运行 `python e2e_test.py`。

## 方式二：无 Docker（本机已有 MySQL / Redis / RabbitMQ）

1. **MySQL**：`localhost:3306`，库名 `realmcrafter`，用户 `root`，密码为空（或修改 `RealmCrafter_server/src/main/resources/application.yml` 中的 `spring.datasource.password`）。
2. **首次需执行建表脚本**：按顺序执行 `RealmCrafter_server/src/main/resources/db/migration/` 下 `V1__base_schema.sql` 至 `V12__*.sql`。
3. **Redis**：`localhost:6379`，无密码。
4. **RabbitMQ**：`localhost:5672`（若未装可先注释掉相关依赖再启动）。
5. 启动后端与 E2E：

```powershell
cd RealmCrafter_server
mvn -DskipTests spring-boot:run
```

另开终端：

```powershell
pip install -q requests
python e2e_test.py
```

## 依赖

- Python 3 + `requests`
- 方式一：Docker Desktop
- 方式二：MySQL 8、Redis、RabbitMQ（可选）、Java 17、Maven
