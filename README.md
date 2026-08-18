# 停车场停车管理系统 (ParkCar)

一个面向中小型停车场（小区、写字楼、商场、公共停车场）的停车管理与收费系统。

## 项目简介

本系统提供车位管理、车辆出入场登记、自动计费、缴费结算、会员套餐、数据统计报表等核心能力，帮助停车场运营方高效管理车辆进出与收费，提升车位利用率与运营效率。

## 文档目录

| 文档 | 说明 |
| ---- | ---- |
| [docs/01-项目概述与需求分析.md](docs/01-项目概述与需求分析.md) | 项目背景、目标、用户角色、功能与非功能需求 |
| [docs/02-系统架构与功能设计.md](docs/02-系统架构与功能设计.md) | 技术选型、整体架构、功能模块划分、业务流程 |
| [docs/03-数据库设计.md](docs/03-数据库设计.md) | 数据表结构、字段说明、ER 关系 |
| [docs/04-接口设计.md](docs/04-接口设计.md) | RESTful API 规范与主要接口定义 |
| [docs/05-开发计划与部署方案.md](docs/05-开发计划与部署方案.md) | 里程碑计划、环境要求、部署方案、测试方案 |

## 技术栈

| 层 | 技术 |
| ---- | ---- |
| 前端 | Vue 3 + Element Plus + Vite + Pinia + ECharts |
| 后端 | Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT（jjwt，拦截器鉴权） |

> 说明：为降低部署门槛，一期采用数据库行锁保证并发安全，未引入 Redis；如需扩展可在二期加入。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+（建议 20 LTS）
- MySQL 8.0

### 1. 初始化数据库

```sql
-- 使用 MySQL 客户端执行
source sql/init.sql
source sql/seed.sql
```

初始数据：角色 2 个、收费规则 2 条、区域 3 个（A/B/C 区共 60 个车位）、会员套餐 3 个。

### 2. 启动后端

1. 修改 `backend/src/main/resources/application.yml` 中数据库账号密码（默认 `root/root`）；
2. 启动（任选其一）：

```bash
cd backend
mvn spring-boot:run
# 或打包运行
mvn package -DskipTests
java -jar target/parkcar-backend-1.0.0.jar
```

后端启动时自动创建管理员账号：`admin / 123456`（仅当不存在时创建，请尽快修改）。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`，使用 `admin / 123456` 登录。

> 开发环境已配置 `/api` 代理到 `http://localhost:8080`，无需额外配置。

### 4. 快速体验流程

1. 登录后进入「车辆入场」，输入车牌（如 `京A12345`）→ 系统自动分配车位；
2. 到「车辆出场」输入同一车牌 → 试算费用 → 选择支付方式 → 结算放行；
3. 在「统计报表」查看营收与车流数据；
4. 「会员月卡」可办理月卡，月卡车辆有效期内出场免费。

## 项目结构

```
parkCar/
├── docs/                    # 项目文档（需求/架构/数据库/接口/计划）
├── sql/
│   ├── init.sql             # 建库建表脚本
│   └── seed.sql             # 初始数据（角色/规则/区域/车位/套餐）
├── backend/                 # 后端工程（Spring Boot 3）
│   └── src/main/java/com/parkcar/
│       ├── common/          # 统一响应、异常、实体基类
│       ├── config/          # Web/MyBatis-Plus 配置、管理员初始化
│       ├── security/        # JWT 工具、拦截器、用户上下文
│       └── module/          # auth/user/space/parking/billing/membership/blacklist/report
├── frontend/                # 前端工程（Vue 3 + Element Plus）
│   └── src/
│       ├── api/             # 接口封装
│       ├── router/          # 路由与权限守卫
│       ├── stores/          # Pinia 用户状态
│       ├── layout/          # 主布局
│       └── views/           # 业务页面
└── README.md
```

## 功能清单（一期）

- 系统管理：登录认证、用户/角色管理、操作日志
- 车位管理：区域维护、车位维护（单个/批量）、实时占用总览
- 出入场：车辆入场（自动/指定车位、黑名单预警）、在场查询、出场试算与结算、手工出场
- 收费：按时/按次规则、免费时长、每日封顶、减免、订单与支付流水
- 会员月卡：套餐、办卡、续费、月卡免费出场
- 报表：营收统计（日/月/年、支付方式分布）、车流统计、车位利用率、CSV 导出

## 默认账号

| 账号 | 密码 | 角色 |
| ---- | ---- | ---- |
| admin | 123456 | 管理员（系统启动自动创建） |
