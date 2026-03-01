# RealmCrafter 一键启动依赖 + 后端 + E2E 测试
# 需要：Docker Desktop 已安装并运行（或本机已有 MySQL/Redis/RabbitMQ）

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$ServerDir = Join-Path $ProjectRoot "RealmCrafter_server"

function Log { param($msg, $color = "White") Write-Host $msg -ForegroundColor $color }
function Wait-ForUrl {
    param($url, $maxSeconds = 90)
    $end = [DateTime]::Now.AddSeconds($maxSeconds)
    while ([DateTime]::Now -lt $end) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
            if ($r.StatusCode -eq 200) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}

# 1. 尝试用 Docker 启动 MySQL / Redis / RabbitMQ
$docker = Get-Command docker -ErrorAction SilentlyContinue
if ($docker) {
    Log "[>>] 检测到 Docker，正在启动 MySQL、Redis、RabbitMQ..." Cyan
    Push-Location $ProjectRoot
    try {
        docker compose up -d 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { docker-compose up -d 2>&1 | Out-Null }
        Log "[>>] 等待 MySQL 就绪..." Cyan
        Start-Sleep -Seconds 15
        $mysqlOk = $false
        for ($i = 0; $i -lt 30; $i++) {
            try {
                docker exec realmcrafter-mysql mysqladmin ping -h localhost -uroot 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) { $mysqlOk = $true; break }
            } catch {}
            Start-Sleep -Seconds 2
        }
        if (-not $mysqlOk) { Log "[FAIL] MySQL 未在预期时间内就绪" Red; exit 1 }
        Log "[SUCCESS] 依赖服务已就绪" Green
    } finally { Pop-Location }
} else {
    Log "[INFO] 未检测到 Docker。请确保本机已启动 MySQL (3306 root/空密码)、Redis (6379)、RabbitMQ (5672)。" Yellow
}

# 2. 启动 Spring Boot 后端（后台）
Log "[>>] 启动后端 (Maven Spring Boot)..." Cyan
$job = Start-Job -ScriptBlock {
    Set-Location $using:ServerDir
    mvn -q -DskipTests spring-boot:run 2>&1
}
Start-Sleep -Seconds 3

# 3. 等待 8080 可访问（心跳或任意接口）
$base = "http://localhost:8080"
$heartbeat = "$base/api/v1/heartbeat"
Log "[>>] 等待后端就绪 ($heartbeat)..." Cyan
if (-not (Wait-ForUrl $heartbeat)) {
    try { Stop-Job $job; Remove-Job $job } catch {}
    Log "[FAIL] 后端未在 90 秒内启动。请查看上方/控制台错误（如 MySQL 连接、Redis）。" Red
    exit 1
}
Log "[SUCCESS] 后端已就绪" Green

# 4. 运行 E2E 测试
Log "[>>] 运行 E2E 测试..." Cyan
Push-Location $ProjectRoot
try {
    & python e2e_test.py
    $e2eExit = $LASTEXITCODE
} finally { Pop-Location }

# 5. 可选：停止后端 Job（不强制杀 Maven 子进程）
Stop-Job $job -ErrorAction SilentlyContinue
Remove-Job $job -Force -ErrorAction SilentlyContinue

if ($e2eExit -ne 0) { exit $e2eExit }
Log "`n[SUCCESS] 全链路 E2E 完成。" Green
