package com.bjtu.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.common.Result;
import com.bjtu.review.dto.AdminCreateRequest;
import com.bjtu.review.dto.AdminCourseInstanceRequest;
import com.bjtu.review.dto.AdminCourseRequest;
import com.bjtu.review.dto.AdminOperationRequest;
import com.bjtu.review.dto.AdminResetPasswordRequest;
import com.bjtu.review.dto.AdminTeacherRequest;
import com.bjtu.review.dto.AdminUpdateRoleRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.entity.CourseBase;
import com.bjtu.review.entity.CourseInstance;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.entity.Teacher;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseBaseMapper;
import com.bjtu.review.mapper.CourseInstanceMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.ReportService;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.service.TagService;
import com.bjtu.review.service.PageResult;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.bjtu.review.vo.AdminAccountVO;
import com.bjtu.review.vo.AuditLogVO;
import com.bjtu.review.vo.CourseInstanceVO;
import com.bjtu.review.vo.ImportResultVO;
import com.bjtu.review.vo.ReportVO;
import com.bjtu.review.vo.ReviewVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String DEPT_OP = "DEPT_OP";
    private static final String AUDITOR = "AUDITOR";
    private static final Set<String> ADMIN_ROLES = Set.of(SUPER_ADMIN, DEPT_OP, AUDITOR);

    private final ReviewService reviewService;
    private final ReportService reportService;
    private final TagService tagService;
    private final AuditLogMapper auditLogMapper;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final CourseBaseMapper courseBaseMapper;
    private final TeacherMapper teacherMapper;
    private final CourseInstanceMapper courseInstanceMapper;
    private final ReviewMapper reviewMapper;

    public AdminController(ReviewService reviewService, ReportService reportService,
                           TagService tagService, AuditLogMapper auditLogMapper,
                           AdminMapper adminMapper, PasswordEncoder passwordEncoder,
                           CourseBaseMapper courseBaseMapper, TeacherMapper teacherMapper,
                           CourseInstanceMapper courseInstanceMapper, ReviewMapper reviewMapper) {
        this.reviewService = reviewService;
        this.reportService = reportService;
        this.tagService = tagService;
        this.auditLogMapper = auditLogMapper;
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.courseBaseMapper = courseBaseMapper;
        this.teacherMapper = teacherMapper;
        this.courseInstanceMapper = courseInstanceMapper;
        this.reviewMapper = reviewMapper;
    }

    @GetMapping("/reviews/pending")
    public Result<List<ReviewVO>> getPendingReviews(Authentication auth) {
        requireContentGovernance(auth);
        return Result.ok(reviewService.getPendingReviews());
    }

    @GetMapping("/reviews")
    public Result<PageResult<ReviewVO>> listReviews(Authentication auth,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String courseName,
                                                    @RequestParam(required = false) String teacherName,
                                                    @RequestParam(required = false) String department,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    LocalDateTime startTime,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    LocalDateTime endTime,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        requireReviewViewAccess(auth);
        Admin admin = currentAdmin(auth);
        return Result.ok(reviewService.getAdminReviews(
                normalizeRole(admin.getRole()),
                textOrNull(admin.getDepartment()),
                textOrNull(status),
                textOrNull(courseName),
                textOrNull(teacherName),
                textOrNull(department),
                startTime,
                endTime,
                normalizePage(page),
                normalizePageSize(pageSize)));
    }

    @PutMapping("/reviews/{id}/approve")
    public Result<?> approveReview(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reviewService.approveReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @PutMapping("/reviews/{id}/reject")
    public Result<?> rejectReview(Authentication auth, @PathVariable Long id,
                                  @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reviewService.rejectReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @GetMapping("/reports/pending")
    public Result<List<ReportVO>> getPendingReports(Authentication auth) {
        requireContentGovernance(auth);
        return Result.ok(reportService.getPendingReports());
    }

    @GetMapping("/reports")
    public Result<List<ReportVO>> getAllReports(Authentication auth) {
        requireContentGovernance(auth);
        return Result.ok(reportService.getAllReports());
    }

    @PutMapping("/reports/{id}/resolve")
    public Result<?> resolveReport(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reportService.resolveReport(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @PutMapping("/reports/{id}/dismiss")
    public Result<?> dismissReport(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reportService.dismissReport(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @DeleteMapping("/reviews/{id}")
    public Result<?> deleteReview(Authentication auth, @PathVariable Long id,
                                  @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reviewService.adminDeleteReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @GetMapping("/audit-logs")
    public Result<List<AuditLogVO>> getAuditLogs(Authentication auth,
                                                 @RequestParam(required = false) String operateType,
                                                 @RequestParam(required = false) Long operatorId,
                                                 @RequestParam(required = false) Long reviewId,
                                                 @RequestParam(required = false) String courseName,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                 LocalDateTime startTime,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                 LocalDateTime endTime) {
        Admin admin = currentAdmin(auth);
        return Result.ok(auditLogMapper.selectRecentLogs(
                normalizeRole(admin.getRole()),
                textOrNull(admin.getDepartment()),
                admin.getId(),
                textOrNull(operateType),
                operatorId,
                reviewId,
                textOrNull(courseName),
                startTime,
                endTime));
    }

    @PostMapping("/tags")
    public Result<Tag> createTag(Authentication auth, @RequestParam String tagName) {
        requireDataMaintenance(auth);
        Tag tag = tagService.createTag(tagName);
        writeAuditLog(adminId(auth), "CREATE_TAG", "新增标签：" + tag.getTagName());
        return Result.ok(tag);
    }

    @DeleteMapping("/tags/{id}")
    public Result<?> deleteTag(Authentication auth, @PathVariable Long id) {
        requireDataMaintenance(auth);
        tagService.deleteTag(id);
        writeAuditLog(adminId(auth), "DELETE_TAG", "删除标签ID：" + id);
        return Result.ok();
    }

    @GetMapping("/courses")
    public Result<PageResult<CourseBase>> listCourses(Authentication auth,
                                                      @RequestParam(required = false) String courseCode,
                                                      @RequestParam(required = false) String courseName,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) String department) {
        requireDataMaintenance(auth);
        LambdaQueryWrapper<CourseBase> wrapper = new LambdaQueryWrapper<>();
        if (courseCode != null && !courseCode.isBlank())
            wrapper.like(CourseBase::getCourseCode, courseCode.trim());
        if (courseName != null && !courseName.isBlank())
            wrapper.like(CourseBase::getCourseName, courseName.trim());
        if (department != null && !department.isBlank())
            wrapper.like(CourseBase::getDepartment, department.trim());
        wrapper.orderByAsc(CourseBase::getCourseCode).orderByAsc(CourseBase::getId);
        return Result.ok(pageQuery(courseBaseMapper, wrapper, page, pageSize));
    }

    @PostMapping("/courses")
    public Result<CourseBase> createCourse(Authentication auth, @RequestBody AdminCourseRequest request) {
        requireDataMaintenance(auth);
        requireBody(request);
        CourseBase course = new CourseBase();
        fillCourse(course, request);
        ensureCourseCodeAvailable(course.getCourseCode(), null);
        courseBaseMapper.insert(course);
        writeAuditLog(adminId(auth), "CREATE_COURSE", "新增课程：" + course.getCourseCode() + " " + course.getCourseName());
        return Result.ok(course);
    }

    @PutMapping("/courses/{id}")
    public Result<?> updateCourse(Authentication auth, @PathVariable Long id, @RequestBody AdminCourseRequest request) {
        requireDataMaintenance(auth);
        requireBody(request);
        CourseBase course = existingCourse(id);
        fillCourse(course, request);
        ensureCourseCodeAvailable(course.getCourseCode(), id);
        courseBaseMapper.updateById(course);
        writeAuditLog(adminId(auth), "UPDATE_COURSE", "更新课程：" + course.getCourseCode() + " " + course.getCourseName());
        return Result.ok();
    }

    @DeleteMapping("/courses/{id}")
    public Result<?> deleteCourse(Authentication auth, @PathVariable Long id) {
        requireDataMaintenance(auth);
        existingCourse(id);
        long instanceCount = courseInstanceMapper.selectCount(new LambdaQueryWrapper<CourseInstance>()
                .eq(CourseInstance::getCourseBaseId, id));
        if (instanceCount > 0) {
            throw new RuntimeException("该课程已有开课实例，不能删除");
        }
        courseBaseMapper.deleteById(id);
        writeAuditLog(adminId(auth), "DELETE_COURSE", "删除课程ID：" + id);
        return Result.ok();
    }

    @GetMapping("/teachers")
    public Result<PageResult<Teacher>> listTeachers(Authentication auth,
                                                    @RequestParam(required = false) String teacherName,
                                                    @RequestParam(required = false) String department,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        requireDataMaintenance(auth);
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (teacherName != null && !teacherName.isBlank())
            wrapper.like(Teacher::getTeacherName, teacherName.trim());
        if (department != null && !department.isBlank())
            wrapper.like(Teacher::getDepartment, department.trim());
        wrapper.orderByAsc(Teacher::getTeacherName).orderByAsc(Teacher::getId);
        return Result.ok(pageQuery(teacherMapper, wrapper, page, pageSize));
    }

    @PostMapping("/teachers")
    public Result<Teacher> createTeacher(Authentication auth, @RequestBody AdminTeacherRequest request) {
        requireDataMaintenance(auth);
        requireBody(request);
        Teacher teacher = new Teacher();
        fillTeacher(teacher, request);
        teacher.setAvgScore(0.0);
        teacher.setAvgTeachingScore(0.0);
        teacher.setAvgWorkloadScore(0.0);
        teacherMapper.insert(teacher);
        writeAuditLog(adminId(auth), "CREATE_TEACHER", "新增教师：" + teacher.getTeacherName());
        return Result.ok(teacher);
    }

    @PutMapping("/teachers/{id}")
    public Result<?> updateTeacher(Authentication auth, @PathVariable Long id, @RequestBody AdminTeacherRequest request) {
        requireDataMaintenance(auth);
        requireBody(request);
        Teacher teacher = existingTeacher(id);
        fillTeacher(teacher, request);
        teacherMapper.updateById(teacher);
        writeAuditLog(adminId(auth), "UPDATE_TEACHER", "更新教师：" + teacher.getTeacherName());
        return Result.ok();
    }

    @DeleteMapping("/teachers/{id}")
    public Result<?> deleteTeacher(Authentication auth, @PathVariable Long id) {
        requireDataMaintenance(auth);
        existingTeacher(id);
        long instanceCount = courseInstanceMapper.selectCount(new LambdaQueryWrapper<CourseInstance>()
                .eq(CourseInstance::getTeacherId, id));
        long reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getTeacherId, id));
        if (instanceCount > 0 || reviewCount > 0) {
            throw new RuntimeException("该教师已有开课实例或评价，不能删除");
        }
        teacherMapper.deleteById(id);
        writeAuditLog(adminId(auth), "DELETE_TEACHER", "删除教师ID：" + id);
        return Result.ok();
    }

    @GetMapping("/course-instances")
    public Result<PageResult<CourseInstanceVO>> listCourseInstances(Authentication auth,
                                                                 @RequestParam(required = false) Long courseBaseId,
                                                                 @RequestParam(required = false) String courseName,
                                                                 @RequestParam(required = false) String teacherName,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        requireDataMaintenance(auth);
        LambdaQueryWrapper<CourseInstance> wrapper = new LambdaQueryWrapper<>();

        if (courseBaseId != null) {
            wrapper.eq(CourseInstance::getCourseBaseId, courseBaseId);
        }

        if (courseName != null && !courseName.isBlank()) {
            List<Long> courseBaseIds = courseBaseMapper.selectList(
                    new LambdaQueryWrapper<CourseBase>()
                            .like(CourseBase::getCourseName, courseName.trim()))
                    .stream().map(CourseBase::getId).toList();
            if (courseBaseIds.isEmpty()) return Result.ok(emptyPage(page, pageSize));
            wrapper.in(CourseInstance::getCourseBaseId, courseBaseIds);
        }

        if (teacherName != null && !teacherName.isBlank()) {
            List<Long> teacherIds = teacherMapper.selectList(
                    new LambdaQueryWrapper<Teacher>()
                            .like(Teacher::getTeacherName, teacherName.trim()))
                    .stream().map(Teacher::getId).toList();
            if (teacherIds.isEmpty()) return Result.ok(emptyPage(page, pageSize));
            wrapper.in(CourseInstance::getTeacherId, teacherIds);
        }

        wrapper.orderByDesc(CourseInstance::getId);
        PageResult<CourseInstance> pageResult = pageQuery(courseInstanceMapper, wrapper, page, pageSize);
        return Result.ok(enrichCourseInstances(pageResult));
    }

    @PostMapping("/course-instances")
    public Result<CourseInstance> createCourseInstance(Authentication auth,
                                                      @RequestBody AdminCourseInstanceRequest request) {
        requireDataMaintenance(auth);
        requireBody(request);
        CourseInstance instance = new CourseInstance();
        fillCourseInstance(instance, request);
        instance.setAvgScore(0.0);
        instance.setGradingScore(0.0);
        instance.setAvgTeachingScore(0.0);
        instance.setAvgWorkloadScore(0.0);
        instance.setReviewCount(0);
        courseInstanceMapper.insert(instance);
        writeAuditLog(adminId(auth), "CREATE_COURSE_INSTANCE", "新增开课实例ID：" + instance.getId());
        return Result.ok(instance);
    }

    @PutMapping("/course-instances/{id}")
    public Result<?> updateCourseInstance(Authentication auth, @PathVariable Long id,
                                          @RequestBody AdminCourseInstanceRequest request) {
        requireDataMaintenance(auth);
        requireBody(request);
        CourseInstance instance = existingCourseInstance(id);
        fillCourseInstance(instance, request);
        courseInstanceMapper.updateById(instance);
        writeAuditLog(adminId(auth), "UPDATE_COURSE_INSTANCE", "更新开课实例ID：" + id);
        return Result.ok();
    }

    @DeleteMapping("/course-instances/{id}")
    public Result<?> deleteCourseInstance(Authentication auth, @PathVariable Long id) {
        requireDataMaintenance(auth);
        existingCourseInstance(id);
        long reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getCourseInstanceId, id));
        if (reviewCount > 0) {
            throw new RuntimeException("该开课实例已有评价，不能删除");
        }
        courseInstanceMapper.deleteById(id);
        writeAuditLog(adminId(auth), "DELETE_COURSE_INSTANCE", "删除开课实例ID：" + id);
        return Result.ok();
    }

    @PostMapping("/courses/import")
    public Result<ImportResultVO> importCourses(Authentication auth, @RequestParam("file") MultipartFile file) {
        requireDataMaintenance(auth);
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要导入的 CSV 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return Result.fail("仅支持 CSV 文件导入");
        }
        ImportResultVO result;
        try {
            result = processImportFile(file);
        } catch (IOException e) {
            return Result.fail("文件读取失败：" + e.getMessage());
        } catch (RuntimeException e) {
            return Result.fail("导入失败：" + e.getMessage());
        }
        writeAuditLog(adminId(auth), "IMPORT_COURSES",
                "导入课程数据：总行数" + result.getTotalRows()
                        + "，成功" + result.getSuccessCount()
                        + "，跳过" + result.getSkipCount()
                        + "，失败" + result.getFailCount());
        return Result.ok(result);
    }

    @GetMapping("/courses/import/template")
    public void downloadImportTemplate(Authentication auth, HttpServletResponse response) throws IOException {
        requireDataMaintenance(auth);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"course_import_template.csv\"");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM for Excel compatibility
        writer.println("courseCode,courseName,credit,department,teacherName,teacherDepartment");
        writer.flush();
    }

    @GetMapping("/accounts")
    public Result<List<AdminAccountVO>> listAdminAccounts(Authentication auth) {
        requireSuperAdmin(auth);
        List<AdminAccountVO> accounts = adminMapper.selectList(null).stream()
                .map(admin -> new AdminAccountVO(
                        admin.getId(),
                        admin.getUsername(),
                        normalizeRole(admin.getRole()),
                        admin.getDepartment()))
                .toList();
        return Result.ok(accounts);
    }

    @PostMapping("/accounts")
    public Result<AdminAccountVO> createAdminAccount(Authentication auth, @RequestBody AdminCreateRequest request) {
        requireSuperAdmin(auth);
        requireBody(request);
        String username = requireText(request.getUsername(), "管理员账号不能为空");
        String password = requireText(request.getPassword(), "管理员密码不能为空");
        String role = requireAdminRole(request.getRole());

        if (adminMapper.selectByUsername(username) != null) {
            throw new RuntimeException("管理员账号已存在");
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(role);
        admin.setDepartment(textOrNull(request.getDepartment()));
        adminMapper.insert(admin);
        writeAuditLog(adminId(auth), "CREATE_ADMIN", "新增管理员：" + admin.getUsername() + "，角色：" + admin.getRole());
        return Result.ok(new AdminAccountVO(admin.getId(), admin.getUsername(), admin.getRole(), admin.getDepartment()));
    }

    @PutMapping("/accounts/{id}/role")
    public Result<?> updateAdminRole(Authentication auth, @PathVariable Long id,
                                     @RequestBody AdminUpdateRoleRequest request) {
        requireSuperAdmin(auth);
        requireBody(request);
        Admin admin = existingAdmin(id);
        admin.setRole(requireAdminRole(request.getRole()));
        admin.setDepartment(textOrNull(request.getDepartment()));
        admin.setCurrentSessionId(null);
        adminMapper.updateById(admin);
        writeAuditLog(adminId(auth), "UPDATE_ADMIN_ROLE", "更新管理员角色/院系：" + admin.getUsername());
        return Result.ok();
    }

    @PutMapping("/accounts/{id}/password")
    public Result<?> resetAdminPassword(Authentication auth, @PathVariable Long id,
                                        @RequestBody AdminResetPasswordRequest request) {
        requireSuperAdmin(auth);
        requireBody(request);
        Admin admin = existingAdmin(id);
        admin.setPassword(passwordEncoder.encode(requireText(request.getPassword(), "新密码不能为空")));
        admin.setCurrentSessionId(null);
        adminMapper.updateById(admin);
        writeAuditLog(adminId(auth), "RESET_ADMIN_PASSWORD", "重置管理员密码：" + admin.getUsername());
        return Result.ok();
    }

    @DeleteMapping("/accounts/{id}")
    public Result<?> deleteAdminAccount(Authentication auth, @PathVariable Long id) {
        requireSuperAdmin(auth);
        if (adminId(auth).equals(id)) {
            throw new RuntimeException("不能删除当前登录的管理员账号");
        }
        Admin admin = existingAdmin(id);
        adminMapper.deleteById(id);
        writeAuditLog(adminId(auth), "DELETE_ADMIN", "删除管理员：" + admin.getUsername());
        return Result.ok();
    }

    private Long adminId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    private String reason(AdminOperationRequest request) {
        return request == null ? null : request.getReason();
    }

    private void writeAuditLog(Long adminId, String operateType, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdminId(adminId);
        auditLog.setOperateType(operateType);
        auditLog.setReason(reason);
        auditLogMapper.insert(auditLog);
    }

    private void requireContentGovernance(Authentication auth) {
        String role = adminRole(auth);
        if (!SUPER_ADMIN.equals(role) && !AUDITOR.equals(role)) {
            throw new AccessDeniedException("当前管理员无内容审核权限");
        }
    }

    private void requireReviewViewAccess(Authentication auth) {
        String role = adminRole(auth);
        if (!SUPER_ADMIN.equals(role) && !DEPT_OP.equals(role)) {
            throw new AccessDeniedException("当前管理员无评价查看权限");
        }
    }

    private void requireDataMaintenance(Authentication auth) {
        String role = adminRole(auth);
        if (!SUPER_ADMIN.equals(role) && !DEPT_OP.equals(role)) {
            throw new AccessDeniedException("当前管理员无数据维护权限");
        }
    }

    private void requireAnyAdmin(Authentication auth) {
        adminRole(auth);
    }

    private void requireSuperAdmin(Authentication auth) {
        if (!SUPER_ADMIN.equals(adminRole(auth))) {
            throw new AccessDeniedException("当前管理员无账号管理权限");
        }
    }

    private String adminRole(Authentication auth) {
        return normalizeRole(currentAdmin(auth).getRole());
    }

    private Admin currentAdmin(Authentication auth) {
        Admin admin = adminMapper.selectById(adminId(auth));
        if (admin == null) {
            throw new AccessDeniedException("管理员账号不存在");
        }
        return admin;
    }

    private Admin existingAdmin(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员账号不存在");
        }
        return admin;
    }

    private CourseBase existingCourse(Long id) {
        CourseBase course = courseBaseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        return course;
    }

    private Teacher existingTeacher(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }
        return teacher;
    }

    private CourseInstance existingCourseInstance(Long id) {
        CourseInstance instance = courseInstanceMapper.selectById(id);
        if (instance == null) {
            throw new RuntimeException("开课实例不存在");
        }
        return instance;
    }

    private void fillCourse(CourseBase course, AdminCourseRequest request) {
        course.setCourseCode(requireText(request.getCourseCode(), "课程代码不能为空"));
        course.setCourseName(requireText(request.getCourseName(), "课程名称不能为空"));
        course.setCredit(request.getCredit() == null ? 0 : request.getCredit());
        course.setDepartment(textOrNull(request.getDepartment()));
    }

    private void fillTeacher(Teacher teacher, AdminTeacherRequest request) {
        teacher.setTeacherName(requireText(request.getTeacherName(), "教师姓名不能为空"));
        teacher.setDepartment(textOrNull(request.getDepartment()));
    }

    private void fillCourseInstance(CourseInstance instance, AdminCourseInstanceRequest request) {
        Long courseBaseId = requireId(request.getCourseBaseId(), "课程不能为空");
        Long teacherId = requireId(request.getTeacherId(), "教师不能为空");
        existingCourse(courseBaseId);
        existingTeacher(teacherId);
        Long currentId = instance.getId();
        LambdaQueryWrapper<CourseInstance> duplicateQuery = new LambdaQueryWrapper<CourseInstance>()
                .eq(CourseInstance::getCourseBaseId, courseBaseId)
                .eq(CourseInstance::getTeacherId, teacherId);
        if (currentId != null) {
            duplicateQuery.ne(CourseInstance::getId, currentId);
        }
        if (courseInstanceMapper.selectCount(duplicateQuery) > 0) {
            throw new RuntimeException("该课程与教师的开课实例已存在");
        }
        instance.setCourseBaseId(courseBaseId);
        instance.setTeacherId(teacherId);
    }

    private void ensureCourseCodeAvailable(String courseCode, Long currentId) {
        CourseBase existing = courseBaseMapper.selectOne(new LambdaQueryWrapper<CourseBase>()
                .eq(CourseBase::getCourseCode, courseCode)
                .last("LIMIT 1"));
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new RuntimeException("课程代码已存在");
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return SUPER_ADMIN;
        }
        return role;
    }

    private String requireAdminRole(String role) {
        String normalized = requireText(role, "管理员角色不能为空");
        if (!ADMIN_ROLES.contains(normalized)) {
            throw new RuntimeException("管理员角色不合法");
        }
        return normalized;
    }

    private void requireBody(Object request) {
        if (request == null) {
            throw new RuntimeException("请求体不能为空");
        }
    }

    private Long requireId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new RuntimeException(message);
        }
        return value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String textOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private PageResult<CourseInstanceVO> enrichCourseInstances(PageResult<CourseInstance> pageResult) {
        List<CourseInstance> instances = pageResult.getRecords();
        if (instances == null || instances.isEmpty()) {
            return new PageResult<>(List.of(), pageResult.getTotal(), pageResult.getPage(), pageResult.getSize());
        }

        Set<Long> courseBaseIds = instances.stream()
                .map(CourseInstance::getCourseBaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> teacherIds = instances.stream()
                .map(CourseInstance::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, CourseBase> courseMap = courseBaseIds.isEmpty()
                ? Map.of()
                : courseBaseMapper.selectBatchIds(courseBaseIds).stream()
                .collect(Collectors.toMap(CourseBase::getId, Function.identity(), (left, right) -> left));
        Map<Long, Teacher> teacherMap = teacherIds.isEmpty()
                ? Map.of()
                : teacherMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(Teacher::getId, Function.identity(), (left, right) -> left));

        List<CourseInstanceVO> records = instances.stream()
                .map(instance -> toCourseInstanceVO(instance, courseMap, teacherMap))
                .toList();
        return new PageResult<>(records, pageResult.getTotal(), pageResult.getPage(), pageResult.getSize());
    }

    private CourseInstanceVO toCourseInstanceVO(CourseInstance instance,
                                                Map<Long, CourseBase> courseMap,
                                                Map<Long, Teacher> teacherMap) {
        CourseInstanceVO vo = new CourseInstanceVO();
        vo.setId(instance.getId());
        vo.setCourseBaseId(instance.getCourseBaseId());
        vo.setTeacherId(instance.getTeacherId());
        vo.setAvgScore(instance.getAvgScore());
        vo.setGradingScore(instance.getGradingScore());
        vo.setAvgTeachingScore(instance.getAvgTeachingScore());
        vo.setAvgWorkloadScore(instance.getAvgWorkloadScore());
        vo.setReviewCount(instance.getReviewCount());

        CourseBase course = courseMap.get(instance.getCourseBaseId());
        if (course != null) {
            vo.setCourseCode(course.getCourseCode());
            vo.setCourseName(course.getCourseName());
        }
        Teacher teacher = teacherMap.get(instance.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getTeacherName());
        }
        return vo;
    }

    private <T> PageResult<T> pageQuery(BaseMapper<T> mapper, LambdaQueryWrapper<T> wrapper,
                                        Integer page, Integer pageSize) {
        int current = normalizePage(page);
        int size = normalizePageSize(pageSize);
        long total = mapper.selectCount(wrapper);
        long offset = (long) (current - 1) * size;
        List<T> records = total == 0
                ? List.of()
                : mapper.selectList(wrapper.last("LIMIT " + offset + ", " + size));
        return new PageResult<>(records, total, current, size);
    }

    private <T> PageResult<T> emptyPage(Integer page, Integer pageSize) {
        return new PageResult<>(List.of(), 0, normalizePage(page), normalizePageSize(pageSize));
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    // ========== CSV 批量导入 ==========

    private ImportResultVO processImportFile(MultipartFile file) throws IOException {
        ImportResultVO result = new ImportResultVO();

        // 读取文件字节并处理编码（UTF-8 BOM 兼容）
        byte[] bytes = file.getBytes();
        boolean hasBom = bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF;
        int offset = hasBom ? 3 : 0;

        CsvReader csvReader = CsvUtil.getReader();
        CsvData csvData = csvReader.read(
                new InputStreamReader(
                        new ByteArrayInputStream(bytes, offset, bytes.length - offset),
                        StandardCharsets.UTF_8));

        List<CsvRow> rows = csvData.getRows();
        if (rows.isEmpty()) {
            throw new RuntimeException("CSV 文件内容为空");
        }

        // 第一行表头，建立列名→索引映射
        CsvRow header = rows.get(0);
        Map<String, Integer> colMap = new java.util.LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            colMap.put(header.get(i).trim(), i);
        }
        validateImportHeaders(colMap);

        // 本批次内去重缓存
        Map<String, Long> batchCourseBaseCache = new java.util.HashMap<>();
        Map<String, Long> batchTeacherCache = new java.util.HashMap<>();
        Set<String> batchInstanceCache = new java.util.HashSet<>();

        int dataStart = 1;
        for (int rowIdx = dataStart; rowIdx < rows.size(); rowIdx++) {
            CsvRow row = rows.get(rowIdx);
            if (isBlankRow(row)) {
                continue;
            }
            result.setTotalRows(result.getTotalRows() + 1);

            try {
                processRow(row, rowIdx + 1, colMap,
                        batchCourseBaseCache,
                        batchTeacherCache, batchInstanceCache,
                        result);
            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                result.getFailures().add(new ImportResultVO.ImportFailure(
                        rowIdx + 1,
                        getField(row, colMap, "courseCode"),
                        getField(row, colMap, "courseName"),
                        e.getMessage()));
            }
        }
        return result;
    }

    private void validateImportHeaders(Map<String, Integer> colMap) {
        List<String> requiredHeaders = List.of(
                "courseCode", "courseName", "credit", "department",
                "teacherName", "teacherDepartment");
        List<String> missingHeaders = requiredHeaders.stream()
                .filter(header -> !colMap.containsKey(header))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new RuntimeException("CSV 表头缺少字段：" + String.join(", ", missingHeaders));
        }
    }

    private boolean isBlankRow(CsvRow row) {
        if (row.size() == 0) return true;
        for (int i = 0; i < row.size(); i++) {
            String val = row.get(i);
            if (val != null && !val.isBlank()) return false;
        }
        return true;
    }

    private void processRow(CsvRow row, int displayRow, Map<String, Integer> colMap,
                            Map<String, Long> batchCourseBaseCache,
                            Map<String, Long> batchTeacherCache,
                            Set<String> batchInstanceCache,
                            ImportResultVO result) {
        String courseCode = getField(row, colMap, "courseCode");
        String courseName = getField(row, colMap, "courseName");
        String creditStr = getField(row, colMap, "credit");
        String department = getField(row, colMap, "department");
        String teacherName = getField(row, colMap, "teacherName");
        String teacherDepartment = getField(row, colMap, "teacherDepartment");

        if (courseCode.isBlank()) throw new RuntimeException("课程代码不能为空");
        if (courseName.isBlank()) throw new RuntimeException("课程名称不能为空");

        courseCode = courseCode.trim().toUpperCase();
        courseName = courseName.trim();
        department = textOrNull(department);
        teacherName = textOrNull(teacherName);
        teacherDepartment = textOrNull(teacherDepartment);
        if (teacherDepartment == null) teacherDepartment = department;
        int credit = 0;
        if (creditStr != null && !creditStr.isBlank()) {
            try {
                credit = Integer.parseInt(creditStr.trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException("学分必须是整数");
            }
        }

        Long courseBaseId = batchCourseBaseCache.get(courseCode);
        boolean changed = false;
        if (courseBaseId == null) {
            CourseBase existing = courseBaseMapper.selectOne(
                    new LambdaQueryWrapper<CourseBase>()
                            .eq(CourseBase::getCourseCode, courseCode)
                            .last("LIMIT 1"));
            if (existing != null) {
                courseBaseId = existing.getId();
            } else {
                CourseBase cb = new CourseBase();
                cb.setCourseCode(courseCode);
                cb.setCourseName(courseName);
                cb.setCredit(credit);
                cb.setDepartment(department);
                courseBaseMapper.insert(cb);
                courseBaseId = cb.getId();
                changed = true;
            }
            batchCourseBaseCache.put(courseCode, courseBaseId);
        }

        Long teacherId = null;
        if (teacherName != null) {
            String teacherKey = teacherName + "||" + (teacherDepartment != null ? teacherDepartment : "");
            teacherId = batchTeacherCache.get(teacherKey);
            if (teacherId == null) {
                LambdaQueryWrapper<Teacher> teacherQuery = new LambdaQueryWrapper<Teacher>()
                        .eq(Teacher::getTeacherName, teacherName);
                if (teacherDepartment != null) {
                    teacherQuery.eq(Teacher::getDepartment, teacherDepartment);
                }
                Teacher existing = teacherMapper.selectOne(teacherQuery.last("LIMIT 1"));
                if (existing != null) {
                    teacherId = existing.getId();
                } else {
                    Teacher t = new Teacher();
                    t.setTeacherName(teacherName);
                    t.setDepartment(teacherDepartment);
                    t.setAvgScore(0.0);
                    t.setAvgTeachingScore(0.0);
                    t.setAvgWorkloadScore(0.0);
                    teacherMapper.insert(t);
                    teacherId = t.getId();
                    changed = true;
                }
                batchTeacherCache.put(teacherKey, teacherId);
            }
        }

        if (teacherId == null) {
            if (changed) {
                result.setSuccessCount(result.getSuccessCount() + 1);
            } else {
                result.setSkipCount(result.getSkipCount() + 1);
            }
            return;
        }

        String instanceKey = courseBaseId + "_" + teacherId;
        if (batchInstanceCache.contains(instanceKey)) {
            result.setSkipCount(result.getSkipCount() + 1);
            return;
        }
        if (courseInstanceMapper.selectCount(new LambdaQueryWrapper<CourseInstance>()
                .eq(CourseInstance::getCourseBaseId, courseBaseId)
                .eq(CourseInstance::getTeacherId, teacherId)) > 0) {
            batchInstanceCache.add(instanceKey);
            result.setSkipCount(result.getSkipCount() + 1);
            return;
        }

        CourseInstance ci = new CourseInstance();
        ci.setCourseBaseId(courseBaseId);
        ci.setTeacherId(teacherId);
        ci.setAvgScore(0.0);
        ci.setGradingScore(0.0);
        ci.setAvgTeachingScore(0.0);
        ci.setAvgWorkloadScore(0.0);
        ci.setReviewCount(0);
        courseInstanceMapper.insert(ci);
        batchInstanceCache.add(instanceKey);
        result.setSuccessCount(result.getSuccessCount() + 1);
    }

    private String getField(CsvRow row, Map<String, Integer> colMap, String colName) {
        Integer idx = colMap.get(colName);
        if (idx == null || idx >= row.size()) return "";
        String val = row.get(idx);
        return val == null ? "" : val.trim();
    }
}
