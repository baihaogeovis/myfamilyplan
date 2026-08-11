# 家庭成长系统

## 项目简介

家庭成长系统是一个帮助家庭成员管理财务的工具,支持收入管理、支出管理、预算规划、资金审批、数据统计等功能。

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.0
- MyBatis Plus 3.5.5
- Spring Security + JWT
- MySQL 8.0

### 前端
- Vue 3.4
- TypeScript
- Vite 5
- Element Plus
- Pinia
- ECharts
- TailwindCSS

## 项目结构

```
myfamilyplan/
├── backend/                 # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/familyplan/
│   │   │   │       ├── common/      # 通用类
│   │   │   │       ├── config/      # 配置类
│   │   │   │       ├── controller/  # 控制器
│   │   │   │       ├── dto/         # 数据传输对象
│   │   │   │       ├── entity/      # 实体类
│   │   │   │       ├── mapper/      # 数据访问层
│   │   │   │       ├── security/    # 安全相关
│   │   │   │       └── service/     # 业务逻辑层
│   │   │   └── resources/
│   │   │       └── application.yml  # 配置文件
│   │   └── test/
│   └── pom.xml                  # Maven配置
├── frontend/                # 前端项目
│   ├── src/
│   │   ├── api/             # API接口
│   │   ├── router/          # 路由配置
│   │   ├── stores/          # Pinia状态管理
│   │   ├── styles/          # 样式文件
│   │   └── views/           # 页面组件
│   ├── package.json         # 依赖配置
│   ├── vite.config.ts       # Vite配置
│   └── tsconfig.json        # TypeScript配置
└── database/                # 数据库脚本
    └── init.sql             # 初始化SQL
```

## 功能模块

### 已实现
- ✅ 用户认证(登录、登出、JWT认证)
- ✅ 家庭成员管理(后端 + 前端页面)
- ✅ 收入管理(后端 CRUD + 前端页面)
- ✅ 支出管理(后端 CRUD + 前端页面)
- ✅ 预算规划(后端 CRUD + 前端页面)
- ✅ 资金审批(申请/通过/驳回 + 前端双 Tab 页面)
- ✅ 通知提醒(后端 + 前端提醒中心)
- ✅ 统计报表(后端汇总接口 + 前端图表页面)
- ✅ 仪表盘(真实数据 + ECharts 趋势图)
- ✅ 数据库表设计
- ✅ 登录页面
- ✅ 主布局组件

### API 接口一览
- `POST /auth/login` 登录
- `GET /member/list` 家庭成员列表
- `GET/POST/PUT/DELETE /income/*` 收入记录
- `GET/POST/PUT/DELETE /expense/*` 支出记录
- `GET/POST/PUT/DELETE /budget/*` 预算计划
- `GET/POST /approval/*` 审批(发起/待审/通过/驳回)
- `GET/PUT /notification/*` 通知(列表/未读计数/标记已读)
- `GET /dashboard/overview|trend|recent` 仪表盘数据
- `GET /statistics/category|monthly` 统计汇总

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+

### 1. 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

### 2. 启动后端

```bash
cd backend
# 修改 application.yml 中的数据库配置
mvn spring-boot:run
```

后端服务将启动在 http://localhost:8080
API文档: http://localhost:8080/doc.html

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务将启动在 http://localhost:5173

### 4. 登录系统

默认账号: `admin`
默认密码: `admin123`

## 开发说明

### 后端开发规范
1. 遵循RESTful API设计规范
2. 所有接口统一返回Result格式
3. 使用MyBatis Plus进行数据库操作
4. 使用Spring Security进行权限控制
5. 业务异常使用BusinessException

### 前端开发规范
1. 使用Vue 3 Composition API
2. 使用TypeScript编写
3. 组件使用<script setup>语法
4. 使用TailwindCSS进行样式编写
5. API调用统一使用@/api/request

## 数据库表说明

- `user_info` - 用户表
- `family_member` - 家庭成员表
- `income_record` - 收入记录表
- `expense_record` - 支出记录表
- `budget_plan` - 预算计划表
- `approval_record` - 资金审批表
- `fund_allocation` - 资金分配表
- `notification` - 通知提醒表

## 许可证

MIT License
