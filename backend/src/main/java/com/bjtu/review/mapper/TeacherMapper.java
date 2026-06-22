package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
    @Update("UPDATE teacher SET avg_score = (SELECT COALESCE(AVG((grading_score + teaching_score + workload_score) / 3.0), 0) FROM review WHERE teacher_id = #{teacherId} AND status IN ('PUBLISHED', 'APPROVED')), " +
            "avg_teaching_score = (SELECT COALESCE(AVG(teaching_score), 0) FROM review WHERE teacher_id = #{teacherId} AND status IN ('PUBLISHED', 'APPROVED')), " +
            "avg_workload_score = (SELECT COALESCE(AVG(workload_score), 0) FROM review WHERE teacher_id = #{teacherId} AND status IN ('PUBLISHED', 'APPROVED')) " +
            "WHERE id = #{teacherId}")
    void updateAvgScore(Long teacherId);
}
