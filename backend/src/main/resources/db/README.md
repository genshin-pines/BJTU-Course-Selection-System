# 数据库初始化与迁移说明

本目录保存 `bjtu_review` 数据库的建表脚本和迁移脚本。

## 全新数据库

全新本地数据库执行：

```powershell
Get-Content ".\backend\src\main\resources\db\schema.sql" -Raw | mysql -u root -p
Get-Content ".\backend\src\main\resources\db\migration_align_current_schema.sql" -Raw | mysql -u root -p bjtu_review
```

远端服务器部署脚本会自动执行：

```bash
mysql < backend/src/main/resources/db/schema.sql
mysql < backend/src/main/resources/db/migration_align_current_schema.sql
```

## 当前核心结构

当前代码依赖的核心表包括：

- `student`
- `admin`
- `teacher`
- `course_base`
- `course_instance`
- `voter_record`
- `review`
- `review_exam_exp`
- `review_vote`
- `report`
- `audit_log`
- `tag`
- `review_tag`
- `course_application`

匿名链路的关键字段：

- `review.voter_record_id`
- `review.anonymous_user_key`
- `review.course_instance_id`
- `review_vote.voter_record_id`
- `review_vote.vote_type`
- `report.reporter_record_id`

## 旧库迁移

如果数据库来自早期版本，建议先备份，再按顺序执行：

1. `migration_session_id.sql`
2. `migration_core_model_anonymity.sql`
3. `migration_backfill_voter_record.sql`
4. `migration_review_exam_exp.sql`
5. `migration_review_downvote.sql`
6. `migration_admin_role.sql`
7. `migration-course-application.sql`
8. `migration_review_hide_reason.sql`
9. `migration_align_current_schema.sql`

如果数据库已经处于半迁移状态，可以直接执行：

```bash
mysql -u bjtu_review -p bjtu_review < /var/www/bjtu-review/backend/src/main/resources/db/migration_align_current_schema.sql
```

`migration_align_current_schema.sql` 是最终对齐脚本，用于补齐当前后端运行所需字段，并回填匿名记录。

## 本地预检

项目根目录提供数据库预检：

```powershell
.\start.ps1 -CheckOnly
```

预检会检查核心表、匿名字段、单点登录字段、审核字段和投票字段。如果提示 `Access denied`，请先修正 `backend/src/main/resources/application.yml` 中的 MySQL 用户名和密码。

## 冒烟测试

本地服务启动后可执行：

```powershell
.\smoke-test.bat
```

默认冒烟测试只验证读取链路和登录链路。如需测试写操作，可执行：

```powershell
.\scripts\smoke-test.ps1 -IncludeWriteChecks
```
