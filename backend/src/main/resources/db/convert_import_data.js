const fs = require('fs');
const crypto = require('crypto');
const path = require('path');

const dir = __dirname;
const srcArg = process.argv[2];
const src = srcArg ? path.resolve(srcArg) : path.join(dir, 'import_data.legacy.sql');
const backup = path.join(dir, 'import_data.legacy.sql');
const dst = path.join(dir, 'import_data.sql');

if (!fs.existsSync(src)) {
  console.error(`Source file not found: ${src}`);
  process.exit(1);
}

const text = fs.readFileSync(src, 'utf8');
if (srcArg && !fs.existsSync(backup)) {
  fs.writeFileSync(backup, text, 'utf8');
}

const courseRe = /INSERT IGNORE INTO course \(course_code, course_name, credit, department, teacher_id, avg_score, grading_score, avg_teaching_score, avg_workload_score, review_count\) SELECT '' AS course_code, '((?:[^'\\]|\\.)*)' AS course_name, 0, NULL, t\.id, 0, 0, 0, 0, 0 FROM teacher t WHERE t\.teacher_name = '((?:[^'\\]|\\.)*)';/g;

const reviewRe = /INSERT INTO review \(student_id, course_id, teacher_id, overall_score, grading_score, teaching_score, workload_score, content, study_tips, exam_type, like_count, status, create_time\) SELECT 1, c\.id, t\.id, (\d+), (\d+), (\d+), (\d+), (\d+), '((?:[^'\\]|\\.)*)', '((?:[^'\\]|\\.)*)', '((?:[^'\\]|\\.)*)', (\d+), 'APPROVED', NOW\(\) FROM course c JOIN teacher t ON c\.teacher_id = t\.id WHERE c\.course_name = '((?:[^'\\]|\\.)*)' AND t\.teacher_name = '((?:[^'\\]|\\.)*)';/;
const reviewCountRe = /INSERT INTO review \(student_id, course_id, teacher_id,/g;

function courseCode(name) {
  return 'IMP-' + crypto.createHash('md5').update(name, 'utf8').digest('hex').slice(0, 10).toUpperCase();
}

const courseNames = new Map();
const coursePairs = [];
let match;
while ((match = courseRe.exec(text)) !== null) {
  const courseName = match[1];
  const teacherName = match[2];
  if (!courseNames.has(courseName)) {
    courseNames.set(courseName, courseCode(courseName));
  }
  coursePairs.push([courseName, teacherName]);
}

const lines = text.split(/\r?\n/);
const out = [];
let i = 0;
while (i < lines.length) {
  const line = lines[i];
  if (line.includes('INSERT IGNORE INTO course ')) {
    i += 1;
    continue;
  }

  const reviewMatch = line.match(reviewRe);
  if (reviewMatch) {
    const [, overall, grading, teaching, workload, , content, studyTips, examType, likeCount, courseName, teacherName] = reviewMatch;
    out.push(
      "INSERT INTO review (student_id, course_instance_id, course_id, teacher_id, overall_score, grading_score, teaching_score, workload_score, content, study_tips, exam_type, like_count, status, create_time) " +
      `SELECT 1, ci.id, cb.id, t.id, ${overall}, ${grading}, ${teaching}, ${workload}, '${content}', '${studyTips}', '${examType}', ${likeCount}, 'PUBLISHED', NOW() ` +
      "FROM course_base cb JOIN course_instance ci ON ci.course_base_id = cb.id JOIN teacher t ON ci.teacher_id = t.id " +
      `WHERE cb.course_name = '${courseName}' AND t.teacher_name = '${teacherName}';`
    );
    i += 1;
    continue;
  }

  if (line.startsWith('INSERT INTO review ')) {
    throw new Error(`Failed to parse review line: ${line.slice(0, 120)}...`);
  }

  if (line.startsWith('-- 插入评价')) {
    out.push(line);
    out.push('-- 依赖 course_base / course_instance，status 使用 PUBLISHED');
    i += 1;
    continue;
  }

  if (line.startsWith('-- 更新所有课程平均分')) {
    out.push('-- 更新所有开课实例平均分');
    out.push(
      "UPDATE course_instance ci SET " +
      "ci.avg_score = (SELECT COALESCE(AVG(overall_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), " +
      "ci.grading_score = (SELECT COALESCE(AVG(grading_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), " +
      "ci.avg_teaching_score = (SELECT COALESCE(AVG(teaching_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), " +
      "ci.avg_workload_score = (SELECT COALESCE(AVG(workload_score),0) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED')), " +
      "ci.review_count = (SELECT COUNT(*) FROM review r WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED','APPROVED'));"
    );
    i += 2;
    continue;
  }

  if (line.startsWith('UPDATE teacher t SET')) {
    out.push(
      "UPDATE teacher t SET t.avg_score = (SELECT COALESCE(AVG(overall_score),0) FROM review r WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED','APPROVED')), " +
      "t.avg_teaching_score = (SELECT COALESCE(AVG(teaching_score),0) FROM review r WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED','APPROVED')), " +
      "t.avg_workload_score = (SELECT COALESCE(AVG(workload_score),0) FROM review r WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED','APPROVED'));"
    );
    i += 1;
    continue;
  }

  if (line.includes('INSERT IGNORE INTO course ') || line.startsWith('UPDATE course c SET')) {
    i += 1;
    continue;
  }

  out.push(line);
  i += 1;
}

let headerEnd = out.findIndex((line) => line.startsWith('-- 插入课程'));
if (headerEnd < 0) {
  headerEnd = out.findIndex((line) => line.startsWith('-- 插入评价'));
}
if (headerEnd < 0) headerEnd = 0;

const courseBaseBlock = ['', '-- 插入课程基础信息（按课程名去重）'];
[...courseNames.entries()].sort((a, b) => a[0].localeCompare(b[0], 'zh-CN')).forEach(([name, code]) => {
  courseBaseBlock.push(
    `INSERT IGNORE INTO course_base (course_code, course_name, credit, department) VALUES ('${code}', '${name.replace(/'/g, "''")}', 0, NULL);`
  );
});

const instanceBlock = ['', '-- 插入开课实例（一师一课一实例）'];
coursePairs.forEach(([courseName, teacherName]) => {
  instanceBlock.push(
    "INSERT IGNORE INTO course_instance (course_base_id, teacher_id) SELECT cb.id, t.id FROM course_base cb JOIN teacher t ON t.teacher_name = " +
    `'${teacherName.replace(/'/g, "''")}' WHERE cb.course_name = '${courseName.replace(/'/g, "''")}';`
  );
});

const merged = [
  ...out.slice(0, headerEnd + 1),
  ...courseBaseBlock,
  ...instanceBlock,
  ...out.slice(headerEnd + 1),
];

if (!merged[3]?.includes('适配新模型')) {
  merged.splice(3, 0, '-- 适配新模型：course_base + course_instance + review.course_instance_id');
}

fs.writeFileSync(dst, merged.join('\n') + '\n', 'utf8');
console.log(`Source: ${src}`);
console.log(`Output: ${dst}`);
console.log(`Converted ${coursePairs.length} instances, ${(text.match(reviewCountRe) || []).length} reviews`);
