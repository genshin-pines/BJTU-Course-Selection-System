package com.bjtu.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.bjtu.review.vo.AdminAccountVO;
import com.bjtu.review.vo.AuditLogVO;
import com.bjtu.review.vo.ReportVO;
import com.bjtu.review.vo.ReviewVO;
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

import java.util.List;
import java.util.Set;

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
    public Result<List<AuditLogVO>> getAuditLogs(Authentication auth) {
        Admin admin = currentAdmin(auth);
        return Result.ok(auditLogMapper.selectRecentLogs(
                normalizeRole(admin.getRole()),
                textOrNull(admin.getDepartment()),
                admin.getId()));
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
    public Result<List<CourseBase>> listCourses(Authentication auth) {
        requireDataMaintenance(auth);
        return Result.ok(courseBaseMapper.selectList(null));
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
    public Result<List<Teacher>> listTeachers(Authentication auth) {
        requireDataMaintenance(auth);
        return Result.ok(teacherMapper.selectList(null));
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
    public Result<List<CourseInstance>> listCourseInstances(Authentication auth,
                                                           @RequestParam(required = false) Long courseBaseId) {
        requireDataMaintenance(auth);
        LambdaQueryWrapper<CourseInstance> wrapper = new LambdaQueryWrapper<>();
        if (courseBaseId != null) {
            wrapper.eq(CourseInstance::getCourseBaseId, courseBaseId);
        }
        wrapper.orderByDesc(CourseInstance::getSemester).orderByAsc(CourseInstance::getId);
        return Result.ok(courseInstanceMapper.selectList(wrapper));
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
        instance.setCourseBaseId(courseBaseId);
        instance.setTeacherId(teacherId);
        instance.setSemester(textOrDefault(request.getSemester(), "UNKNOWN"));
        instance.setClassName(textOrNull(request.getClassName()));
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
}
