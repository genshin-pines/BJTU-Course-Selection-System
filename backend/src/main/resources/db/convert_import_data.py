#!/usr/bin/env python3
"""Convert legacy import_data.sql to the course_base + course_instance model."""

import hashlib
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "import_data.sql"
DST = ROOT / "import_data.sql"
BACKUP = ROOT / "import_data.legacy.sql"

COURSE_RE = re.compile(
    r"INSERT IGNORE INTO course \(course_code, course_name, credit, department, teacher_id, "
    r"avg_score, grading_score, avg_teaching_score, avg_workload_score, review_count\) "
    r"SELECT '' AS course_code, '((?:[^'\\]|\\.)*)' AS course_name, 0, NULL, t\.id, "
    r"0, 0, 0, 0, 0 FROM teacher t WHERE t\.teacher_name = '((?:[^'\\]|\\.)*)';"
)

REVIEW_RE = re.compile(
    r"INSERT INTO review \(student_id, course_id, teacher_id, overall_score, grading_score, "
    r"teaching_score, workload_score, content, study_tips, exam_type, like_count, status, create_time\) "
    r"SELECT 1, c\.id, t\.id, (\d+), (\d+), (\d+), (\d+), (\d+), "
    r"'((?:[^'\\]|\\.)*)', '((?:[^'\\]|\\.)*)', '((?:[^'\\]|\\.)*)', (\d+), 'APPROVED', NOW\(\) "
    r"FROM course c JOIN teacher t ON c\.teacher_id = t\.id "
    r"WHERE c\.course_name = '((?:[^'\\]|\\.)*)' AND t\.teacher_name = '((?:[^'\\]|\\.)*)';"
)


def course_code(name: str) -> str:
    digest = hashlib.md5(name.encode("utf-8")).hexdigest()[:10].upper()
    return f"IMP-{digest}"


def convert(text: str) -> str:
    lines = text.splitlines()
    out: list[str] = []
    course_names: dict[str, str] = {}
    course_pairs: list[tuple[str, str]] = []

    i = 0
    while i < len(lines):
        line = lines[i]
        m = COURSE_RE.match(line)
        if m:
            course_name, teacher_name = m.group(1), m.group(2)
            if course_name not in course_names:
                course_names[course_name] = course_code(course_name)
            course_pairs.append((course_name, teacher_name))
            i += 1
            continue

        if line.startswith("INSERT INTO review "):
            rm = REVIEW_RE.match(line)
            if rm:
                overall, grading, teaching, workload, _difficulty, content, study_tips, exam_type, like_count, course_name, teacher_name = rm.groups()
                out.append(
                    "INSERT INTO review (student_id, course_instance_id, course_id, teacher_id, overall_score, "
                    "grading_score, teaching_score, workload_score, content, study_tips, exam_type, like_count, status, create_time) "
                    f"SELECT 1, ci.id, cb.id, t.id, {overall}, {grading}, {teaching}, {workload}, "
                    f"'{content}', '{study_tips}', '{exam_type}', {like_count}, 'PUBLISHED', NOW() "
                    "FROM course_base cb "
                    "JOIN course_instance ci ON ci.course_base_id = cb.id "
                    "JOIN teacher t ON ci.teacher_id = t.id "
                    f"WHERE cb.course_name = '{course_name}' AND t.teacher_name = '{teacher_name}';"
                )
                i += 1
                continue

        if line.startswith("-- 插入评价"):
            out.append(line)
            out.append("-- 依赖 course_base / course_instance，status 使用 PUBLISHED")
            i += 1
            continue

        if line.startswith("-- 更新所有课程平均分"):
            out.extend([
                line.replace("课程", "开课实例"),
                "UPDATE course_instance ci SET "
                "ci.avg_score = (SELECT COALESCE(AVG(overall_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), "
                "ci.grading_score = (SELECT COALESCE(AVG(grading_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), "
                "ci.avg_teaching_score = (SELECT COALESCE(AVG(teaching_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), "
                "ci.avg_workload_score = (SELECT COALESCE(AVG(workload_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), "
                "ci.review_count = (SELECT COUNT(*) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED'));",
            ])
            i += 2
            continue

        if line.startswith("UPDATE teacher t SET"):
            out.append(
                "UPDATE teacher t SET t.avg_score = (SELECT COALESCE(AVG(overall_score),0) FROM review r WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED','APPROVED')), "
                "t.avg_teaching_score = (SELECT COALESCE(AVG(teaching_score),0) FROM review r WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED','APPROVED')), "
                "t.avg_workload_score = (SELECT COALESCE(AVG(workload_score),0) FROM review r WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED','APPROVED'));"
            )
            i += 1
            continue

        if "INSERT IGNORE INTO course " in line or line.startswith("UPDATE course c SET"):
            i += 1
            continue

        out.append(line)
        i += 1

    header_end = 0
    for idx, line in enumerate(out):
        if line.startswith("-- 插入教师"):
            header_end = idx
            break

    course_base_block = ["", "-- 插入课程基础信息（按课程名去重）"]
    for name, code in sorted(course_names.items(), key=lambda item: item[0]):
        escaped = name.replace("'", "''")
        course_base_block.append(
            "INSERT IGNORE INTO course_base (course_code, course_name, credit, department) "
            f"VALUES ('{code}', '{escaped}', 0, NULL);"
        )

    course_instance_block = ["", "-- 插入开课实例（一师一课一实例）"]
    for course_name, teacher_name in course_pairs:
        escaped_course = course_name.replace("'", "''")
        escaped_teacher = teacher_name.replace("'", "''")
        course_instance_block.append(
            "INSERT IGNORE INTO course_instance (course_base_id, teacher_id) "
            "SELECT cb.id, t.id FROM course_base cb JOIN teacher t ON t.teacher_name = "
            f"'{escaped_teacher}' WHERE cb.course_name = '{escaped_course}';"
        )

    merged = out[: header_end + 1] + course_base_block + course_instance_block + out[header_end + 1 :]

    if "-- 适配新模型" not in merged[0]:
        merged.insert(
            3,
            "-- 适配新模型：course_base + course_instance + review.course_instance_id",
        )

    return "\n".join(merged) + "\n"


def main() -> None:
    text = SRC.read_text(encoding="utf-8")
    if not BACKUP.exists():
        BACKUP.write_text(text, encoding="utf-8")
    converted = convert(text)
    DST.write_text(converted, encoding="utf-8")
    print(f"Converted {SRC.name}: {len(COURSE_RE.findall(text))} courses, {len(REVIEW_RE.findall(text))} reviews")


if __name__ == "__main__":
    main()
