# BJTU Course Review System

北京交通大学课程评价系统，面向学生提供课程搜索、匿名评价、标签筛选、投票和举报能力；面向管理员提供评价审核、举报处理、课程/教师/开课实例维护、数据导入、审计日志和管理员账号管理能力。

## 功能概览

- 学生端：注册登录、课程搜索、课程详情、匿名评价、评价编辑/删除、点赞、点“没用”、举报、个人评价中心。
- 课程检索：支持关键词、学院、教师、评分维度、评价数量、标签和排序筛选。
- 匿名链路：评价、投票、举报通过 `voter_record` 进行匿名身份映射，前台不暴露真实学生身份。
- 后台治理：支持评价审核、举报处理、审计日志、课程/教师/开课实例维护、标签维护、课程数据 CSV 导入。
- 权限控制：学生和管理员分离登录；管理员支持 `SUPER_ADMIN`、`DEPT_OP`、`AUDITOR` 三类角色。
- 部署支持：提供 Windows 本地一键启动脚本，以及 Ubuntu + Nginx + systemd 远端部署脚本。

## 技术栈

- 后端：Spring Boot 3、Spring Security、JWT、MyBatis Plus、MySQL、Maven
- 前端：Vue 3、Vite、Pinia、Vue Router、Axios、Element Plus
- 部署：Ubuntu 22.04、Nginx、systemd、MySQL 8

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

## 本地启动

前置要求：

- JDK 17
- Maven 3.6+
- Node.js 18+
- MySQL 8+

初始化数据库：

```powershell
cd D:\BJTU_Course_Selection_Evaluation_System\repo
Get-Content ".\backend\src\main\resources\db\schema.sql" -Raw | mysql -u root -p
Get-Content ".\backend\src\main\resources\db\migration_align_current_schema.sql" -Raw | mysql -u root -p bjtu_review
```

修改本地数据库配置：

```text
backend/src/main/resources/application.yml
```

启动前后端：

```powershell
cd D:\BJTU_Course_Selection_Evaluation_System\repo
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

## 文档

- [使用手册.md](使用手册.md)：学生、管理员和运维常用操作说明。
- [deploy/DEPLOY.md](deploy/DEPLOY.md)：远端服务器部署、重启、日志查看和故障排查。
- [backend/src/main/resources/db/README.md](backend/src/main/resources/db/README.md)：数据库初始化与迁移说明。

## 注意事项

- `backend/src/main/resources/application.yml` 用于本地开发；远端服务器使用 `/etc/bjtu-review/bjtu-review.env` 注入数据库、JWT 和邮件配置。
- 邮件验证码需要配置 SMTP 账号和授权码。QQ 邮箱应使用“SMTP 授权码”，不是 QQ 登录密码。
- `logs/`、`backend/target/`、`frontend/dist/`、`frontend/node_modules/` 都是运行或构建产物，不应提交到仓库。
