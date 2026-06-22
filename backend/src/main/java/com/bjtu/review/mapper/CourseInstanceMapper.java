package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.CourseInstance;
import com.bjtu.review.vo.CourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CourseInstanceMapper extends BaseMapper<CourseInstance> {

    @Select("SELECT ci.id AS id, cb.id AS course_base_id, ci.id AS course_instance_id, " +
            "cb.course_code, cb.course_name, cb.credit, cb.department, " +
            "ci.teacher_id, t.teacher_name, " +
            "ci.avg_score, ci.grading_score, ci.avg_teaching_score, ci.avg_workload_score, ci.review_count " +
            "FROM course_instance ci " +
            "JOIN course_base cb ON cb.id = ci.course_base_id " +
            "LEFT JOIN teacher t ON t.id = ci.teacher_id " +
            "WHERE ci.id = #{instanceId}")
    CourseVO selectDetailByInstanceId(Long instanceId);

    @Select("SELECT * FROM course_instance WHERE course_base_id = #{courseBaseId} AND teacher_id = #{teacherId} LIMIT 1")
    CourseInstance selectByCourseBaseIdAndTeacherId(Long courseBaseId, Long teacherId);

    @Update("UPDATE course_instance SET " +
            "avg_score = (SELECT COALESCE(AVG((grading_score + teaching_score + workload_score) / 3.0), 0) FROM review WHERE course_instance_id = #{courseInstanceId} AND status IN ('PUBLISHED', 'APPROVED')), " +
            "grading_score = (SELECT COALESCE(AVG(grading_score), 0) FROM review WHERE course_instance_id = #{courseInstanceId} AND status IN ('PUBLISHED', 'APPROVED')), " +
            "avg_teaching_score = (SELECT COALESCE(AVG(teaching_score), 0) FROM review WHERE course_instance_id = #{courseInstanceId} AND status IN ('PUBLISHED', 'APPROVED')), " +
            "avg_workload_score = (SELECT COALESCE(AVG(workload_score), 0) FROM review WHERE course_instance_id = #{courseInstanceId} AND status IN ('PUBLISHED', 'APPROVED')), " +
            "review_count = (SELECT COUNT(*) FROM review WHERE course_instance_id = #{courseInstanceId} AND status IN ('PUBLISHED', 'APPROVED')) " +
            "WHERE id = #{courseInstanceId}")
    void updateScores(Long courseInstanceId);
}
