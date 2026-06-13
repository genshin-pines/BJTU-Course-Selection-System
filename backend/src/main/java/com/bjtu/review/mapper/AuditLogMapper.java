package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.vo.AuditLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Select("SELECT al.*, rv.content AS review_content, rv.course_instance_id, rv.course_id, rv.teacher_id, " +
            "COALESCE(c.course_name, cb.course_name) AS course_name, " +
            "ci.semester, ci.class_name, t.teacher_name, " +
            "COALESCE(vr.display_name, '匿名用户') AS anonymous_id " +
            "FROM audit_log al " +
            "LEFT JOIN review rv ON al.review_id = rv.id " +
            "LEFT JOIN voter_record vr ON rv.voter_record_id = vr.id " +
            "LEFT JOIN course c ON rv.course_id = c.id " +
            "LEFT JOIN course_instance ci ON rv.course_instance_id = ci.id " +
            "LEFT JOIN course_base cb ON ci.course_base_id = cb.id " +
            "LEFT JOIN teacher t ON rv.teacher_id = t.id " +
            "ORDER BY al.create_time DESC " +
            "LIMIT 100")
    List<AuditLogVO> selectRecentLogs();
}
