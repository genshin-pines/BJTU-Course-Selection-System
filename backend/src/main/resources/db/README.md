# Database Migration Guide

This directory contains the SQL scripts used to initialize or upgrade the `bjtu_review` database.

## New Database

For a brand-new local database, run:

```sql
source schema.sql;
```

`schema.sql` is expected to contain the current baseline structure, including:

- `course_base`
- `course_instance`
- `course_teacher_relation`
- `voter_record`
- `review_exam_exp`
- anonymous fields on `review`, `review_vote`, and `report`
- session fields used by single-login control

After `schema.sql`, you may optionally import the large seed data:

```sql
source import_data.sql;
```

## Existing Database

For an existing database that was created before the model migration, run the scripts in this order:

1. `migration_session_id.sql`
2. `migration_core_model_anonymity.sql`
3. `migration_backfill_voter_record.sql`
4. `migration_review_exam_exp.sql`
5. `migration_review_downvote.sql`

Do not run `schema.sql` against a populated old database as a replacement for migrations. It is the baseline for new databases.

## Startup Preflight

`start.ps1` checks the most important tables and columns before starting the backend:

- Core tables: `course_base`, `course_instance`, `course_teacher_relation`, `voter_record`
- Review fields: `voter_record_id`, `anonymous_user_key`, `course_instance_id`, `downvote_count`
- Vote fields: `voter_record_id`, `vote_type`
- Report field: `reporter_record_id`
- Exam experience fields: `key_chapters`, `cheat_sheet_allowed`

If the check fails, run the migration scripts above. If your machine does not have the `mysql` command installed, the script will skip automatic checking and print the manual checklist instead.

You can explicitly skip the check:

```powershell
.\start.ps1 -SkipDbCheck
```

To run only the database preflight without starting services:

```powershell
.\start.ps1 -CheckOnly
```

## Runtime Smoke Test

After the backend is running, use the root batch wrapper to verify the core runtime chain:

```bat
smoke-test.bat
```

The smoke test checks course search/detail, review list, student login/session, vote-state endpoints, admin login/session, pending reviews, reports, and audit logs.

By default it avoids write operations. To also test vote toggling:

```powershell
.\scripts\smoke-test.ps1 -IncludeWriteChecks
```
