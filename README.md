# Agent Store

一个可在本地完整运行的单体电商系统，业务范围参考 `D:\xiaozhe-E-commerce`，保留了商城、支付、物流、售后和管理后台的核心流程。项目面向开发 Agent 的接口调用与业务测试，因此没有引入微服务、Redis、消息队列或第三方支付。

## 技术栈

- 后端：Java 17、Spring Boot 3.3、Spring Security、Spring Data JPA、Bean Validation、Springdoc
- 前端：React 18、TypeScript、Vite、Lucide Icons
- 数据库：MySQL 8.4
- 测试：JUnit 5、MockMvc、H2 MySQL 兼容模式
- 认证：服务端 Session + BCrypt

## 功能范围

买家端：

- 注册、登录、退出和 Session 身份认证
- 个人资料、账户余额和余额流水
- 商品列表、详情、关键词/分类筛选和促销价
- 购物车、库存校验和提交订单
- 余额支付、取消订单、查看物流和确认收货
- 申请仅退款或退货退款、提交退货物流并查询处理进度

管理员端：

- 运营数据概览
- 商品新增、编辑、上下架、库存和促销价管理
- 订单查询和填写物流单号发货
- 售后批准/拒绝；退货退款支持收货确认，确认后退款并恢复库存
- 用户查询和余额调整

## 快速启动

启动 Docker Desktop，在项目根目录执行：

```powershell
docker desktop start
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d --build
```

完整的首次配置、日常启动、日志和故障排查说明见 [STARTUP.md](STARTUP.md)。容器 MySQL 默认映射到宿主机 `3307`，避免与 Windows 本机 MySQL 的 `3306` 冲突。

访问地址：

- 商城及管理台：<http://localhost:5173>
- 后端 API：<http://localhost:8080/api/products>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

演示账号：

| 角色 | 用户名 | 密码 | 初始余额 |
| --- | --- | --- | --- |
| 买家 | `buyer` | `buyer123` | `10000.00` |
| 管理员 | `admin` | `admin123` | `0.00` |

演示账号由后端首次启动时创建，密码以 BCrypt 保存。管理员登录后自动进入管理工作区。

停止服务：

```powershell
docker compose down
```

需要清空本项目的 MySQL 数据卷时执行 `docker compose down -v`。该命令会永久删除 Compose 创建的数据库卷。

## 本地开发

先启动数据库：

```powershell
docker compose up -d mysql
```

启动后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

启动前端：

```powershell
cd frontend
npm install
npm run dev
```

Vite 将 `/api` 代理到 `http://localhost:8080`。后端默认数据库配置如下，可通过环境变量覆盖：

```text
DB_URL=jdbc:mysql://localhost:3307/agent_commerce?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
DB_USERNAME=commerce
DB_PASSWORD=commerce123
```

JPA 会自动创建或补充表结构。需要手动建表时可执行：

```powershell
Get-Content -LiteralPath .\database\init.sql -Raw |
    docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4'
```

## 测试与构建

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
npm run test:e2e
```

后端测试使用 H2 内存数据库，不依赖本机 MySQL。E2E 测试使用本机 Chrome，需要先启动后端；前端开发服务未运行时会由 Playwright 自动启动。

## API 概览

公开接口：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册并建立 Session |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/products` | 查询在售商品 |
| GET | `/api/products/{id}` | 商品详情 |

买家接口：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/PUT | `/api/users/me` | 当前用户资料 |
| GET | `/api/users/me/balance-records` | 余额流水 |
| GET | `/api/cart` | 当前用户购物车 |
| POST | `/api/cart/items` | 添加购物车商品 |
| POST | `/api/orders` | 创建订单 |
| POST | `/api/orders/{orderNo}/pay` | 余额支付 |
| POST | `/api/orders/{orderNo}/cancel` | 取消待支付订单 |
| POST | `/api/orders/{orderNo}/confirm` | 确认收货 |
| GET/POST | `/api/after-sales` | 查询/创建仅退款或退货退款申请 |
| POST | `/api/after-sales/{id}/return-shipment` | 提交退货物流 |

管理员接口统一位于 `/api/admin/**`，包括商品、订单发货、售后审核、退货收货确认和用户余额管理。完整请求模型可在 Swagger UI 查看。

Agent 使用 Session 调用示例：

```bash
curl -c cookie.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"buyer","password":"buyer123"}'

curl -b cookie.txt -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":1}'
```

## 目录

```text
E-commerce-springboot/
├── backend/          Spring Boot API
├── frontend/         React 商城与管理台
├── database/         MySQL 初始化脚本
├── docker-compose.yml
├── STARTUP.md        Docker Desktop 启动与维护说明
└── README.md
```
