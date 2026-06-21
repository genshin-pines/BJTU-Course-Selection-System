package com.bjtu.review.mapper;

import com.bjtu.review.vo.CourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper {

    @Select("<script>" +
            "SELECT ci.id AS id, cb.id AS course_base_id, ci.id AS course_instance_id, " +
            "cb.course_code, cb.course_name, cb.credit, cb.department, " +
            "ci.teacher_id, t.teacher_name, " +
            "ci.avg_score, ci.grading_score, ci.avg_teaching_score, ci.avg_workload_score, ci.review_count " +
            "FROM course_instance ci " +
            "JOIN course_base cb ON cb.id = ci.course_base_id " +
            "LEFT JOIN teacher t ON t.id = ci.teacher_id " +
            "WHERE 1 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (cb.course_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR cb.course_code LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR t.teacher_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='department != null and department != \"\"'>" +
            "  AND cb.department = #{department}" +
            "</if>" +
            "<if test='teacherName != null and teacherName != \"\"'>" +
            "  AND t.teacher_name LIKE CONCAT('%', #{teacherName}, '%')" +
            "</if>" +
            "<if test='minScore != null'>" +
            "  AND ci.avg_score &gt;= #{minScore}" +
            "</if>" +
            "<if test='maxScore != null'>" +
            "  AND ci.avg_score &lt;= #{maxScore}" +
            "</if>" +
            "<if test='minGradingScore != null'>" +
            "  AND ci.grading_score &gt;= #{minGradingScore}" +
            "</if>" +
            "<if test='maxGradingScore != null'>" +
            "  AND ci.grading_score &lt;= #{maxGradingScore}" +
            "</if>" +
            "<if test='minTeachingScore != null'>" +
            "  AND ci.avg_teaching_score &gt;= #{minTeachingScore}" +
            "</if>" +
            "<if test='maxTeachingScore != null'>" +
            "  AND ci.avg_teaching_score &lt;= #{maxTeachingScore}" +
            "</if>" +
            "<if test='minWorkloadScore != null'>" +
            "  AND ci.avg_workload_score &gt;= #{minWorkloadScore}" +
            "</if>" +
            "<if test='maxWorkloadScore != null'>" +
            "  AND ci.avg_workload_score &lt;= #{maxWorkloadScore}" +
            "</if>" +
            "<if test='minReviewCount != null'>" +
            "  AND ci.review_count &gt;= #{minReviewCount}" +
            "</if>" +
            "<if test='tagIds != null and tagIds.size() > 0'>" +
            "  AND EXISTS (" +
            "    SELECT 1 FROM review r_tag " +
            "    JOIN review_tag rt_filter ON rt_filter.review_id = r_tag.id " +
            "    WHERE r_tag.course_instance_id = ci.id " +
            "      AND r_tag.status IN ('PUBLISHED', 'APPROVED') " +
            "      AND rt_filter.tag_id IN " +
            "      <foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>" +
            "<if test='tagMatchMode != null and tagMatchMode == \"AND\"'>" +
            "      GROUP BY r_tag.course_instance_id " +
            "      HAVING COUNT(DISTINCT rt_filter.tag_id) = #{tagIds.size}" +
            "</if>" +
            "  )" +
            "</if>" +
            "ORDER BY " +
            "<choose>" +
            "  <when test='sortBy == \"avgScore\"'>ci.avg_score</when>" +
            "  <when test='sortBy == \"gradingScore\"'>ci.grading_score</when>" +
            "  <when test='sortBy == \"teachingScore\"'>ci.avg_teaching_score</when>" +
            "  <when test='sortBy == \"workloadScore\"'>ci.avg_workload_score</when>" +
            "  <when test='sortBy == \"courseCode\"'>cb.course_code</when>" +
            "  <otherwise>ci.review_count</otherwise>" +
            "</choose> " +
            "<choose>" +
            "  <when test='sortOrder == \"asc\"'>ASC</when>" +
            "  <otherwise>DESC</otherwise>" +
            "</choose>, cb.course_code ASC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<CourseVO> searchCourses(@Param("keyword") String keyword,
                                  @Param("department") String department,
                                  @Param("teacherName") String teacherName,
                                  @Param("minScore") Double minScore,
                                  @Param("maxScore") Double maxScore,
                                  @Param("minGradingScore") Double minGradingScore,
                                  @Param("maxGradingScore") Double maxGradingScore,
                                  @Param("minTeachingScore") Double minTeachingScore,
                                  @Param("maxTeachingScore") Double maxTeachingScore,
                                  @Param("minWorkloadScore") Double minWorkloadScore,
                                  @Param("maxWorkloadScore") Double maxWorkloadScore,
                                  @Param("minReviewCount") Integer minReviewCount,
                                  @Param("tagIds") List<Long> tagIds,
                                  @Param("tagMatchMode") String tagMatchMode,
                                  @Param("sortBy") String sortBy,
                                  @Param("sortOrder") String sortOrder,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM course_instance ci " +
            "JOIN course_base cb ON cb.id = ci.course_base_id " +
            "LEFT JOIN teacher t ON t.id = ci.teacher_id " +
            "WHERE 1 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (cb.course_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR cb.course_code LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR t.teacher_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='department != null and department != \"\"'>" +
            "  AND cb.department = #{department}" +
            "</if>" +
            "<if test='teacherName != null and teacherName != \"\"'>" +
            "  AND t.teacher_name LIKE CONCAT('%', #{teacherName}, '%')" +
            "</if>" +
            "<if test='minScore != null'>" +
            "  AND ci.avg_score &gt;= #{minScore}" +
            "</if>" +
            "<if test='maxScore != null'>" +
            "  AND ci.avg_score &lt;= #{maxScore}" +
            "</if>" +
            "<if test='minGradingScore != null'>" +
            "  AND ci.grading_score &gt;= #{minGradingScore}" +
            "</if>" +
            "<if test='maxGradingScore != null'>" +
            "  AND ci.grading_score &lt;= #{maxGradingScore}" +
            "</if>" +
            "<if test='minTeachingScore != null'>" +
            "  AND ci.avg_teaching_score &gt;= #{minTeachingScore}" +
            "</if>" +
            "<if test='maxTeachingScore != null'>" +
            "  AND ci.avg_teaching_score &lt;= #{maxTeachingScore}" +
            "</if>" +
            "<if test='minWorkloadScore != null'>" +
            "  AND ci.avg_workload_score &gt;= #{minWorkloadScore}" +
            "</if>" +
            "<if test='maxWorkloadScore != null'>" +
            "  AND ci.avg_workload_score &lt;= #{maxWorkloadScore}" +
            "</if>" +
            "<if test='minReviewCount != null'>" +
            "  AND ci.review_count &gt;= #{minReviewCount}" +
            "</if>" +
            "<if test='tagIds != null and tagIds.size() > 0'>" +
            "  AND EXISTS (" +
            "    SELECT 1 FROM review r_tag " +
            "    JOIN review_tag rt_filter ON rt_filter.review_id = r_tag.id " +
            "    WHERE r_tag.course_instance_id = ci.id " +
            "      AND r_tag.status IN ('PUBLISHED', 'APPROVED') " +
            "      AND rt_filter.tag_id IN " +
            "      <foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>" +
            "  )" +
            "</if>" +
            "</script>")
    long countSearch(@Param("keyword") String keyword,
                     @Param("department") String department,
                     @Param("teacherName") String teacherName,
                     @Param("minScore") Double minScore,
                     @Param("maxScore") Double maxScore,
                     @Param("minGradingScore") Double minGradingScore,
                     @Param("maxGradingScore") Double maxGradingScore,
                     @Param("minTeachingScore") Double minTeachingScore,
                     @Param("maxTeachingScore") Double maxTeachingScore,
                     @Param("minWorkloadScore") Double minWorkloadScore,
                     @Param("maxWorkloadScore") Double maxWorkloadScore,
                     @Param("minReviewCount") Integer minReviewCount,
                     @Param("tagIds") List<Long> tagIds);

    @Select("SELECT DISTINCT department FROM course_base WHERE department IS NOT NULL AND department != '' ORDER BY department")
    List<String> selectDistinctDepartments();

    @Select("SELECT DISTINCT t.teacher_name FROM course_instance ci " +
            "JOIN teacher t ON t.id = ci.teacher_id " +
            "WHERE t.teacher_name IS NOT NULL AND t.teacher_name != '' " +
            "ORDER BY t.teacher_name")
    List<String> selectDistinctTeachers();
}
