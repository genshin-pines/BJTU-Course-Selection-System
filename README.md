# BJTU Course Review System

## Admin test accounts

| Role | Username | Password |
|------|----------|----------|
| SUPER_ADMIN | admin | 123456 |
| DEPT_OP | dept_op | 123456 |
| AUDITOR | auditor | 123456 |
北京交通大学课程评价系统

## 技术栈
- 后端：Spring Boot 3.2.5 + MyBatis Plus 3.5.6 + Spring Security + JWT + MySQL
- 前端：Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios

## 功能
- 课程搜索（关键词 + 学院筛选 + 分页）
- 课程详情（评分展示 + 评价列表）
- 发布匿名评价（评分 + 内容 + 标签 + 敏感词过滤）
- 评价审核（管理员通过/拒绝，自动更新课程/教师评分）
- 举报管理（举报 → 采纳删除评价 / 驳回）
- JWT 登录认证 + RBAC 权限控制（Student / Admin）

## 启动
```bash
# 后端（端口 8889）
cd backend
mvn clean package -DskipTests
java -jar target/review-system-1.0.0.jar 
mvn spring-boot:run

# 前端（端口 8081）
cd frontend
npm install
npm run dev
http://localhost:8081

## 数据库
执行 `backend/src/main/resources/db/schema.sql` 建表并灌入初始数据。

## 测试账号
| 角色 | 账号 | 密码 |
|------|------|------|
| 学生 | 2022111111 | 123456 |
| SUPER_ADMIN | admin | 123456 |
| DEPT_OP | dept_op | 123456 |
| AUDITOR | auditor | 123456 |
