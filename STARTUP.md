# Agent Store Docker Desktop 启动与维护

本文档说明如何在 Windows Docker Desktop 中启动、验证和维护 Agent Store。项目功能与接口说明见 `README.md`。

## 1. 运行方式

推荐使用 Docker Compose 同时运行三个服务：

| 服务 | 容器端口 | 默认宿主机端口 | 说明 |
| --- | ---: | ---: | --- |
| `mysql` | `3306` | `3307` | MySQL 8.4，数据保存在 Docker 卷中 |
| `backend` | `8080` | `8080` | Spring Boot API |
| `frontend` | `80` | `5173` | Nginx 托管 React 静态资源并代理 `/api` |

MySQL 默认映射到 `3307`，避免与 Windows 本机常用的 `3306` 端口冲突。容器内后端始终通过 `mysql:3306` 访问数据库，不受宿主机映射端口影响。

所有宿主机端口默认只绑定 `127.0.0.1`，仅允许本机访问，不对局域网暴露。

## 2. 启动 Docker Desktop

可以从 Windows 开始菜单启动 Docker Desktop，也可以在 PowerShell 中执行：

```powershell
docker desktop start
```

等待 Docker Desktop 显示 Engine running，然后检查：

```powershell
docker desktop status
docker version
docker compose version
```

`docker version` 应同时显示 Client 和 Server 信息。只有 Client、没有 Server 时，说明 Docker Engine 尚未完成启动。

需要登录 Windows 后自动启动时，在 Docker Desktop 的 `Settings > General` 中启用 `Start Docker Desktop when you sign in to your computer`。

## 3. 首次配置

进入项目目录：

```powershell
Set-Location D:\E-commerce-springboot
```

首次运行且 `.env` 不存在时，从示例生成本机配置：

```powershell
if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
}
```

`.env` 中可配置：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_DATABASE` | `agent_commerce` | 数据库名称 |
| `MYSQL_USER` | `commerce` | 业务数据库用户 |
| `MYSQL_PASSWORD` | `commerce123` | 业务数据库密码 |
| `MYSQL_ROOT_PASSWORD` | `root123` | MySQL root 密码 |
| `MYSQL_HOST_PORT` | `3307` | MySQL 暴露到 Windows 的端口 |
| `BACKEND_HOST_PORT` | `8080` | 后端暴露端口 |
| `FRONTEND_HOST_PORT` | `5173` | 前端暴露端口 |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | 本地开发允许的前端来源 |
| `AGENT_BASE_URL` | `http://host.docker.internal:8000` | 后端访问宿主机客服 Agent 的地址 |
| `AGENT_CONNECT_TIMEOUT_MS` | `2000` | 客服 Agent 连接超时毫秒数 |
| `AGENT_READ_TIMEOUT_MS` | `15000` | 客服 Agent 响应超时毫秒数 |

默认密码只适合本机开发和演示。修改前端端口时，应同步修改 `CORS_ALLOWED_ORIGIN`。修改配置后先校验 Compose：

```powershell
docker compose config --quiet
```

## 4. 构建并启动

后台构建并启动完整环境：

```powershell
Set-Location D:\E-commerce-springboot
docker compose up -d --build
docker compose ps
```

首次启动需要下载 MySQL、Maven、Java、Node.js 和 Nginx 基础镜像，耗时会比后续启动长。MySQL 健康检查通过后，Compose 才会启动后端。

查看启动日志：

```powershell
docker compose logs -f
```

只查看指定服务：

```powershell
docker compose logs -f mysql
docker compose logs -f backend
docker compose logs -f frontend
```

按 `Ctrl+C` 只会退出日志查看，不会停止后台容器。

## 5. 访问和验证

默认访问地址：

| 服务 | 地址 |
| --- | --- |
| 商城及管理台 | <http://localhost:5173> |
| 后端商品接口 | <http://localhost:8080/api/products> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

演示账号：

| 角色 | 用户名 | 密码 | 初始余额 |
| --- | --- | --- | --- |
| 买家 | `buyer` | `buyer123` | `10000.00` |
| 金卡买家 | `zhangsan` | `123456` | `100000.00` |
| 银卡买家 | `lisi` | `123456` | `100000.00` |
| 普通买家 | `wangwu` | `123456` | `100.00` |
| 管理员 | `admin` | `admin123` | `0.00` |

执行以下命令验证容器、API 和前端：

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/api/products
(Invoke-WebRequest http://localhost:5173 -UseBasicParsing).StatusCode
```

商品接口返回数据、前端状态码为 `200`，表示完整链路可用。

## 6. 日常维护

启动已经构建的环境：

```powershell
docker compose up -d
```

重启全部服务或单个服务：

```powershell
docker compose restart
docker compose restart backend
```

代码或依赖变化后重新构建：

```powershell
docker compose up -d --build
```

更新基础镜像并重建：

```powershell
docker compose pull mysql
docker compose build --pull
docker compose up -d
```

停止并删除容器和项目网络，但保留数据库卷：

```powershell
docker compose down
```

服务使用 `restart: unless-stopped`。Docker Desktop 关闭时仍在运行的容器，会在 Docker Engine 下次启动后自动恢复；执行过 `docker compose down` 后，需要再次运行 `docker compose up -d`。

## 7. 本地开发模式

需要前后端热更新时，只在 Docker 中运行 MySQL：

```powershell
Set-Location D:\E-commerce-springboot
docker compose up -d mysql
docker compose ps mysql
```

在第一个 PowerShell 终端启动后端。由于容器 MySQL 映射到宿主机 `3307`，必须覆盖数据库地址：

```powershell
Set-Location D:\E-commerce-springboot\backend
$env:DB_URL = "jdbc:mysql://localhost:3307/agent_commerce?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true"
$env:DB_USERNAME = "commerce"
$env:DB_PASSWORD = "commerce123"
$env:AGENT_BASE_URL = "http://127.0.0.1:8000"
.\mvnw.cmd spring-boot:run
```

在第二个 PowerShell 终端启动前端：

```powershell
Set-Location D:\E-commerce-springboot\frontend
npm ci
npm run dev
```

前端监听 `http://localhost:5173`，并将 `/api` 和 `/v3/api-docs` 代理到 `http://localhost:8080`。

客服 Agent 是可选依赖。启动 `D:\E-commerce-agent` 后，商城客服通过其 `/chat` 接口应答；未启动时商城仍可使用，客服窗口会返回固定降级提示。

## 8. 数据库维护

JPA 会自动创建或补充表结构，后端首次启动时会写入演示账号和商品，正常启动无需手工执行 SQL。

进入 MySQL 命令行：

```powershell
docker compose exec mysql mysql -ucommerce -p agent_commerce
```

需要手工执行完整初始化脚本时，在项目根目录运行：

```powershell
Get-Content -LiteralPath .\database\init.sql -Raw |
    docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4'
```

MySQL 数据卷初始化完成后，仅修改 `.env` 中的密码不会修改数据库内已有用户密码。需要改密码时应登录 MySQL 执行 `ALTER USER`，或者在确认数据可以删除后重新初始化数据卷。

## 9. 清空并重新初始化数据库

> **警告：以下命令会永久删除本项目 Docker 数据卷中的全部订单、用户、商品和余额数据，无法恢复。**

确认不再需要现有数据后执行：

```powershell
Set-Location D:\E-commerce-springboot
docker compose down -v
docker compose up -d --build
```

后端重新启动后会自动建表并恢复演示账号和初始商品。

## 10. 常见问题

### Docker Engine 无法连接

```powershell
docker desktop start
docker desktop status
docker version
```

若仍然只有 Client 信息，等待 Docker Desktop 完成启动；必要时从 Docker Desktop 菜单执行 Restart。

### 端口被占用

项目默认使用 `3307`、`8080` 和 `5173`：

```powershell
Get-NetTCPConnection -LocalPort 3307,8080,5173 -State Listen -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

在 `.env` 中修改对应的 `*_HOST_PORT` 后重新运行 `docker compose up -d`。修改前端端口时同步修改 `CORS_ALLOWED_ORIGIN`。

### 后端连接 MySQL 失败

```powershell
docker compose ps mysql
docker compose logs mysql
docker compose logs backend
```

Compose 中的后端使用 `mysql:3306`；只有运行在 Windows 上的后端才使用 `localhost:3307`。

### 页面能打开但接口不可用

```powershell
Invoke-RestMethod http://localhost:8080/api/products
docker compose logs backend
docker compose logs frontend
```

### 修改代码后页面没有变化

Docker 模式不会热更新源码，需要重新构建：

```powershell
docker compose up -d --build
```

需要热更新时使用“本地开发模式”。
