# BJTU Course Review System

北京交通大学课程评价系统，面向学生提供课程搜索、匿名评价、标签筛选、投票和举报能力；面向管理员提供评价审核、举报处理、课程/教师/开课实例维护、数据导入、审计日志和管理员账号管理能力。

## 功能概览

- 学生端：注册登录、课程搜索、课程详情、匿名评价、评价编辑/删除、点赞、点"没用"、举报、个人评价中心。
- 课程检索：支持关键词、学院、教师、评分维度、评价数量、标签和排序筛选。
- 匿名链路：评价、投票、举报通过 `voter_record` 进行匿名身份映射，前台不暴露真实学生身份。
- 后台治理：支持评价审核、举报处理、审计日志、课程/教师/开课实例维护、标签维护、课程数据 CSV 导入。
- 权限控制：学生和管理员分离登录；管理员支持 `SUPER_ADMIN`、`DEPT_OP`、`AUDITOR` 三类角色。
- 部署支持：提供 Windows 本地一键启动脚本，以及 Ubuntu + Nginx + systemd 远端部署脚本。

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | 运行环境 |
| Spring Boot | 3.5.14 | Web 框架 |
| Spring Security | (Spring Boot 管理) | 安全框架 |
| MyBatis Plus | 3.5.9 | ORM 框架 |
| MySQL | 8+ | 数据库 |
| JWT (jjwt) | 0.12.5 | 身份认证 |
| Hutool | 5.8.27 | 工具库 |
| Lombok | 1.18.46 | 代码简化 |
| Maven | 3.6+ | 构建工具 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | ^3.4.27 | 前端框架 |
| Vue Router | ^4.3.2 | 路由管理 |
| Pinia | ^2.1.7 | 状态管理 |
| Axios | ^1.7.2 | HTTP 客户端 |
| Element Plus | ^2.7.5 | UI 组件库 |
| Vite | ^5.2.12 | 构建工具 |

## 目录结构

```text
repo/
  backend/              Spring Boot 后端
  frontend/             Vue 前端
  deploy/               远端部署脚本与 Nginx/systemd 配置
  scripts/              辅助测试脚本
  start.ps1             Windows 本地一键启动脚本
  start.bat             Windows 启动脚本包装器
  smoke-test.bat        本地冒烟测试入口
  使用手册.md            用户与管理员使用说明
```

## 环境配置

### 开发环境要求

| 环境 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 17 | 17 LTS | 后端运行环境 |
| Maven | 3.6 | 3.9+ | 构建工具 |
| Node.js | 18 | 20 LTS | 前端运行环境 |
| MySQL | 8.0 | 8.0+ | 数据库 |
| npm | 9+ | 10+ | 包管理器 |

### 数据库初始化

```powershell
cd D:\homework\software_engineering\BJTU-Course-Selection-System
Get-Content ".\backend\src\main\resources\db\schema.sql" -Raw | mysql -u root -p
Get-Content ".\backend\src\main\resources\db\migration_align_current_schema.sql" -Raw | mysql -u root -p bjtu_review
```

### 配置文件

| 文件 | 用途 | 位置 |
|------|------|------|
| 本地数据库配置 | 开发环境数据库连接 | `backend/src/main/resources/application.yml` |
| 远端环境变量 | 生产环境配置 | `/etc/bjtu-review/bjtu-review.env` |

### 环境变量说明

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `SPRING_DATASOURCE_URL` | 数据库连接 | `jdbc:mysql://127.0.0.1:3306/bjtu_review?...` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `bjtu_review` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | 你的数据库密码 |
| `JWT_SECRET` | JWT 密钥（至少 32 字符） | 随机生成的长字符串 |
| `SPRING_MAIL_HOST` | 邮件服务器 | `smtp.qq.com` |
| `SPRING_MAIL_PORT` | 邮件端口 | `587` |
| `SPRING_MAIL_USERNAME` | 发件邮箱 | `your-email@qq.com` |
| `SPRING_MAIL_PASSWORD` | SMTP 授权码 | QQ 邮箱授权码 |

## 本地启动

初始化数据库：

```powershell
cd D:\homework\software_engineering\BJTU-Course-Selection-System
Get-Content ".\backend\src\main\resources\db\schema.sql" -Raw | mysql -u root -p
Get-Content ".\backend\src\main\resources\db\migration_align_current_schema.sql" -Raw | mysql -u root -p bjtu_review
```

修改本地数据库配置：

```text
backend/src/main/resources/application.yml
```

启动前后端：

```powershell
cd D:\homework\software_engineering\BJTU-Course-Selection-System
.\start.ps1
```

访问地址：

```text
前端：http://localhost:8081
后端：http://localhost:8889
```

只检查数据库结构：

```powershell
.\start.ps1 -CheckOnly
```

## 测试账号

学生账号：

| 学号 | 密码 |
| --- | --- |
| 2022111111 | 123456 |
| 2022111112 | 123456 |

管理员账号：

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| SUPER_ADMIN | admin | 123456 | 超级管理员，拥有全部后台权限 |
| DEPT_OP | dept_op | 123456 | 院系维护员，维护本院系课程、教师、标签和导入数据 |
| AUDITOR | auditor | 123456 | 内容审核员，审核评价、处理举报和查看审核相关日志 |

生产环境请及时修改默认密码。

## 常用命令

后端测试：

```powershell
cd backend
mvn test
```

前端构建：

```powershell
cd frontend
npm run build
```

本地冒烟测试：

```powershell
.\smoke-test.bat
```

## 部署

远端部署说明见 [deploy/DEPLOY.md](deploy/DEPLOY.md)。

当前部署脚本适配 Ubuntu 22.04，会在服务器上安装或复用 MySQL、Nginx、JDK、Maven、Node.js，并通过 systemd 运行后端服务。

## 已知问题

| 问题 | 影响 | 解决方案 |
|------|------|----------|
| Element Plus label 弃用警告 | 控制台显示警告，不影响功能 | 升级 Element Plus 3 时修复 |
| 邮件验证码依赖 SMTP 配置 | 未配置时无法注册 | 配置正确的 SMTP 服务器和授权码 |
| 同一账号仅允许一处登录 | 新登录使旧会话失效 | 预期行为，防止并发登录 |
| 管理员不能在学生端操作 | 管理员账号无法发布评价/举报 | 预期行为，权限隔离 |

## 文档

- [使用手册.md](使用手册.md)：学生、管理员和运维常用操作说明。
- [deploy/DEPLOY.md](deploy/DEPLOY.md)：远端服务器部署、重启、日志查看和故障排查。
- [backend/src/main/resources/db/README.md](backend/src/main/resources/db/README.md)：数据库初始化与迁移说明。

## 注意事项

- `backend/src/main/resources/application.yml` 用于本地开发；远端服务器使用 `/etc/bjtu-review/bjtu-review.env` 注入数据库、JWT 和邮件配置。
- 邮件验证码需要配置 SMTP 账号和授权码。QQ 邮箱应使用"SMTP 授权码"，不是 QQ 登录密码。
- `logs/`、`backend/target/`、`frontend/dist/`、`frontend/node_modules/` 都是运行或构建产物，不应提交到仓库。
