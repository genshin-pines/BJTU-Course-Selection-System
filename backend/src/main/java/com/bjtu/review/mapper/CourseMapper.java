package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Course;
import com.bjtu.review.vo.CourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Select("<script>" +
            "SELECT c.*, t.teacher_name " +
            "FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (c.course_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR c.course_code LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR t.teacher_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='department != null and department != \"\"'>" +
            "  AND c.department = #{department}" +
            "</if>" +
            "<if test='minScore != null'>" +
            "  AND c.avg_score &gt;= #{minScore}" +
            "</if>" +
            "<if test='maxScore != null'>" +
            "  AND c.avg_score &lt;= #{maxScore}" +
            "</if>" +
            "ORDER BY c.review_count DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<CourseVO> searchCourses(@Param("keyword") String keyword,
                                  @Param("department") String department,
                                  @Param("minScore") Double minScore,
                                  @Param("maxScore") Double maxScore,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (c.course_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR c.course_code LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR t.teacher_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='department != null and department != \"\"'>" +
            "  AND c.department = #{department}" +
            "</if>" +
            "<if test='minScore != null'>" +
            "  AND c.avg_score &gt;= #{minScore}" +
            "</if>" +
            "<if test='maxScore != null'>" +
            "  AND c.avg_score &lt;= #{maxScore}" +
            "</if>" +
            "</script>")
    long countSearch(@Param("keyword") String keyword,
                     @Param("department") String department,
                     @Param("minScore") Double minScore,
                     @Param("maxScore") Double maxScore);

    @Update("UPDATE course SET avg_score = (SELECT COALESCE(AVG(overall_score), 0) FROM review WHERE course_id = #{courseId} AND status = 'APPROVED'), " +
            "difficulty_score = (SELECT COALESCE(AVG(difficulty_score), 0) FROM review WHERE course_id = #{courseId} AND status = 'APPROVED'), " +
            "grading_score = (SELECT COALESCE(AVG(grading_score), 0) FROM review WHERE course_id = #{courseId} AND status = 'APPROVED'), " +
            "review_count = (SELECT COUNT(*) FROM review WHERE course_id = #{courseId} AND status = 'APPROVED') " +
            "WHERE id = #{courseId}")
    void updateScores(Long courseId);
}
