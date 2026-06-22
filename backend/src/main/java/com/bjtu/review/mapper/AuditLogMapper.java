package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.vo.AuditLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Select("<script>" +
            "SELECT al.*, rv.content AS review_content, rv.course_instance_id, rv.course_id, rv.teacher_id, " +
            "cb.course_name AS course_name, " +
            "t.teacher_name, " +
            "COALESCE(vr.display_name, '匿名用户') AS anonymous_id " +
            "FROM audit_log al " +
            "LEFT JOIN review rv ON al.review_id = rv.id " +
            "LEFT JOIN voter_record vr ON rv.voter_record_id = vr.id " +
            "LEFT JOIN course_instance ci ON rv.course_instance_id = ci.id " +
            "LEFT JOIN course_base cb ON cb.id = COALESCE(ci.course_base_id, rv.course_id) " +
            "LEFT JOIN teacher t ON rv.teacher_id = t.id " +
            "WHERE 1 = 1 " +
            "<if test='role == \"AUDITOR\"'>" +
            "  AND al.operate_type IN ('APPROVE_REVIEW', 'REJECT_REVIEW', 'HIDE_REVIEW', 'DELETE_REVIEW', 'RESOLVE_REPORT', 'DISMISS_REPORT') " +
            "</if>" +
            "<if test='role == \"DEPT_OP\"'>" +
            "  <choose>" +
            "    <when test='department != null and department != \"\"'>" +
            "      AND (COALESCE(cb.department, t.department) = #{department} OR al.admin_id = #{adminId}) " +
            "    </when>" +
            "    <otherwise> AND al.admin_id = #{adminId} </otherwise>" +
            "  </choose>" +
            "</if>" +
            "<if test='operateType != null and operateType != \"\"'>" +
            "  AND al.operate_type = #{operateType} " +
            "</if>" +
            "<if test='operatorId != null'>" +
            "  AND al.admin_id = #{operatorId} " +
            "</if>" +
            "<if test='reviewId != null'>" +
            "  AND al.review_id = #{reviewId} " +
            "</if>" +
            "<if test='courseName != null and courseName != \"\"'>" +
            "  AND cb.course_name LIKE CONCAT('%', #{courseName}, '%') " +
            "</if>" +
            "<if test='startTime != null'>" +
            "  AND al.create_time &gt;= #{startTime} " +
            "</if>" +
            "<if test='endTime != null'>" +
            "  AND al.create_time &lt;= #{endTime} " +
            "</if>" +
            "ORDER BY al.create_time DESC " +
            "LIMIT 100" +
            "</script>")
    List<AuditLogVO> selectRecentLogs(@Param("role") String role,
                                       @Param("department") String department,
                                       @Param("adminId") Long adminId,
                                       @Param("operateType") String operateType,
                                       @Param("operatorId") Long operatorId,
                                       @Param("reviewId") Long reviewId,
                                       @Param("courseName") String courseName,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
