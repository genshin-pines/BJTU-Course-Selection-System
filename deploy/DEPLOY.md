# 部署文档

本文档说明如何将北京交通大学课程评价系统部署到 Ubuntu 服务器。

## 部署架构

- 操作系统：Ubuntu 22.04 LTS
- Web 服务：Nginx，监听 80 端口
- 后端服务：Spring Boot，通过 systemd 管理，监听本机 8889 端口
- 数据库：MySQL，本机数据库 `bjtu_review`
- 前端资源：构建后由 Nginx 提供静态文件

默认远端地址：

```text
http://101.32.222.213
```

## 前置条件

本地 Windows 机器需要：

- PowerShell
- `ssh` 和 `scp`
- 可访问服务器的网络环境

服务器需要：

- Ubuntu 22.04
- 可使用 `sudo`
- 80 端口已在云服务器安全组中放行

## 一键上传并安装

在本地 PowerShell 执行：

```powershell
cd D:\BJTU_Course_Selection_Evaluation_System\repo
powershell -ExecutionPolicy Bypass -File .\deploy\upload-and-install.ps1 -User ubuntu
```

脚本会完成：

- 打包当前项目。
- 上传到服务器。
- 安装或复用 JDK、Maven、Node.js、MySQL、Nginx。
- 初始化数据库和必要迁移。
- 构建后端 jar。
- 构建前端静态资源。
- 写入 systemd 和 Nginx 配置。
- 重启后端服务并重载 Nginx。

安装过程会提示输入：

- 服务器登录密码。
- MySQL 应用用户 `bjtu_review` 的密码。
- JWT 密钥，至少 32 位。
- SMTP 邮箱账号，可留空。
- SMTP 授权码，可留空。

生产环境不要使用 `123456` 作为数据库密码或管理员密码。

## 手动部署

上传项目：

```powershell
scp -r D:\BJTU_Course_Selection_Evaluation_System\repo ubuntu@101.32.222.213:~/BJTU-Course-Selection-System
```

登录服务器：

```powershell
ssh ubuntu@101.32.222.213
```

执行安装：

```bash
cd ~/BJTU-Course-Selection-System
sudo bash deploy/setup-ubuntu.sh
```

## 环境变量

远端环境变量文件：

```bash
/etc/bjtu-review/bjtu-review.env
```

示例：

```env
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/bjtu_review?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=bjtu_review
SPRING_DATASOURCE_PASSWORD=replace-with-a-strong-password
JWT_SECRET=replace-with-a-random-secret-at-least-32-chars
SPRING_MAIL_HOST=smtp.qq.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=replace-with-your-email@qq.com
SPRING_MAIL_PASSWORD=replace-with-your-smtp-auth-code
```

修改后需要重启后端：

```bash
sudo systemctl restart bjtu-review
```

## 邮件验证码配置

如果使用 QQ 邮箱：

1. 登录 QQ 邮箱网页版。
2. 开启 SMTP 服务。
3. 生成 SMTP 授权码。
4. 将邮箱和授权码写入 `/etc/bjtu-review/bjtu-review.env`。
5. 重启后端服务。

注意：`SPRING_MAIL_PASSWORD` 应填写 SMTP 授权码，不是 QQ 登录密码。

## 数据库

全新服务器会执行：

```bash
mysql < backend/src/main/resources/db/schema.sql
mysql < backend/src/main/resources/db/migration_align_current_schema.sql
```

如果是旧库或半迁移状态，可在服务器上手动执行：

```bash
mysql -u bjtu_review -p bjtu_review < /var/www/bjtu-review/backend/src/main/resources/db/migration_align_current_schema.sql
sudo systemctl restart bjtu-review
```

## 常用运维命令

查看后端状态：

```bash
sudo systemctl status bjtu-review
```

查看后端日志：

```bash
sudo journalctl -u bjtu-review -f
```

查看最近错误：

```bash
sudo journalctl -u bjtu-review --since "10 minutes ago" -p err --no-pager
```

重启后端：

```bash
sudo systemctl restart bjtu-review
```

检查 Nginx 配置：

```bash
sudo nginx -t
```

重载 Nginx：

```bash
sudo systemctl reload nginx
```

重新部署：

```powershell
cd D:\BJTU_Course_Selection_Evaluation_System\repo
powershell -ExecutionPolicy Bypass -File .\deploy\upload-and-install.ps1 -User ubuntu
```

部署脚本会强制重启后端服务，确保新 jar 生效。

## 故障排查

### 访问网站打不开

检查 Nginx：

```bash
sudo systemctl status nginx
sudo nginx -t
```

确认云服务器安全组放行 80 端口。

### 接口 500

查看后端日志：

```bash
sudo journalctl -u bjtu-review --since "10 minutes ago" -p err --no-pager
```

如果日志中出现 `Unknown column`、`doesn't have a default value`、`Duplicate entry` 等数据库错误，先执行数据库对齐脚本。

### 邮件验证码发送失败

检查：

- SMTP 用户名是否为完整邮箱。
- SMTP 密码是否为授权码。
- 邮箱服务商是否开启 SMTP。
- 修改环境变量后是否重启 `bjtu-review`。

### 重新部署后代码未生效

确认服务已重启：

```bash
sudo systemctl restart bjtu-review
```

当前部署脚本已经包含重启逻辑。
