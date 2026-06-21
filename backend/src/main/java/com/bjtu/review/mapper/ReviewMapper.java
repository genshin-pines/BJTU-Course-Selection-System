package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Review;
import com.bjtu.review.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    String REVIEW_SELECT_COLUMNS = "SELECT r.*, COALESCE(vr.display_name, '匿名用户') AS anonymous_id, " +
            "vr.anonymous_key AS voter_anonymous_key, " +
            "cb.course_name AS course_name, " +
            "COALESCE(ree.study_tips, r.study_tips) AS review_study_tips, " +
            "COALESCE(ree.exam_type, r.exam_type) AS review_exam_type, " +
            "ree.key_chapters AS review_key_chapters, " +
            "ree.cheat_sheet_allowed AS review_cheat_sheet_allowed, " +
            "t.teacher_name " +
            "FROM review r " +
            "LEFT JOIN review_exam_exp ree ON r.id = ree.review_id " +
            "LEFT JOIN voter_record vr ON r.voter_record_id = vr.id " +
            "LEFT JOIN course_instance ci ON r.course_instance_id = ci.id " +
            "LEFT JOIN course_base cb ON cb.id = COALESCE(ci.course_base_id, r.course_id) " +
            "LEFT JOIN teacher t ON r.teacher_id = t.id ";

    String REVIEW_ORDER_BY = "ORDER BY " +
            "<choose>" +
            "  <when test='sortBy == \"latest\"'>r.create_time DESC</when>" +
            "  <when test='sortBy == \"highScore\"'>r.overall_score DESC, r.create_time DESC</when>" +
            "  <when test='sortBy == \"useful\"'>r.like_count DESC, (r.like_count - r.downvote_count) DESC, r.create_time DESC</when>" +
            "  <when test='sortBy == \"controversial\"'>(r.like_count + r.downvote_count) DESC, r.downvote_count DESC, r.create_time DESC</when>" +
            "  <otherwise>(" +
            "    COALESCE(r.like_count, 0) * 2 " +
            "    - COALESCE(r.downvote_count, 0) * 1.5 " +
            "    + LEAST(COALESCE(CHAR_LENGTH(NULLIF(TRIM(r.content), '')), 0) / 80, 2) " +
            "    + CASE WHEN COALESCE(NULLIF(TRIM(ree.study_tips), ''), NULLIF(TRIM(r.study_tips), '')) IS NULL THEN 0 ELSE 0.6 END " +
            "    + CASE WHEN COALESCE(NULLIF(TRIM(ree.exam_type), ''), NULLIF(TRIM(r.exam_type), '')) IS NULL THEN 0 ELSE 0.4 END " +
            "    + CASE WHEN NULLIF(TRIM(ree.key_chapters), '') IS NULL THEN 0 ELSE 0.5 END " +
            "    + LEAST((SELECT COUNT(1) FROM review_tag rtq WHERE rtq.review_id = r.id) * 0.2, 1) " +
            "    + CASE " +
            "        WHEN r.create_time >= DATE_SUB(NOW(), INTERVAL 180 DAY) THEN 0.8 " +
            "        WHEN r.create_time >= DATE_SUB(NOW(), INTERVAL 365 DAY) THEN 0.4 " +
            "        ELSE 0 " +
            "      END " +
            "  ) DESC, (COALESCE(r.like_count, 0) - COALESCE(r.downvote_count, 0)) DESC, r.like_count DESC, r.create_time DESC</otherwise>" +
            "</choose>";

    @Select("<script>" +
            REVIEW_SELECT_COLUMNS +
            "WHERE r.status IN ('PUBLISHED', 'APPROVED') " +
            "<choose>" +
            "  <when test='courseInstanceId != null'> AND r.course_instance_id = #{courseInstanceId} </when>" +
            "  <otherwise> AND r.course_id = #{courseId} </otherwise>" +
            "</choose>" +
            "<if test='tagIds != null and tagIds.size() > 0'>" +
            "  AND EXISTS (" +
            "    SELECT 1 FROM review_tag rt " +
            "    WHERE rt.review_id = r.id AND rt.tag_id IN " +
            "    <foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>" +
            "  ) " +
            "</if>" +
            REVIEW_ORDER_BY +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "anonymousId", column = "anonymous_id"),
            @Result(property = "voterRecordId", column = "voter_record_id"),
            @Result(property = "anonymousUserKey", column = "anonymous_user_key"),
            @Result(property = "courseInstanceId", column = "course_instance_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseName", column = "course_name"),
            @Result(property = "teacherId", column = "teacher_id"),
            @Result(property = "teacherName", column = "teacher_name"),
            @Result(property = "overallScore", column = "overall_score"),
            @Result(property = "gradingScore", column = "grading_score"),
            @Result(property = "teachingScore", column = "teaching_score"),
            @Result(property = "workloadScore", column = "workload_score"),
            @Result(property = "content", column = "content"),
            @Result(property = "studyTips", column = "review_study_tips"),
            @Result(property = "examType", column = "review_exam_type"),
            @Result(property = "keyChapters", column = "review_key_chapters"),
            @Result(property = "cheatSheetAllowed", column = "review_cheat_sheet_allowed"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "downvoteCount", column = "downvote_count"),
            @Result(property = "status", column = "status"),
            @Result(property = "createTime", column = "create_time"),
    })
    List<ReviewVO> selectByCourseId(@Param("courseId") Long courseId,
                                    @Param("courseInstanceId") Long courseInstanceId,
                                    @Param("sortBy") String sortBy,
                                    @Param("tagIds") List<Long> tagIds);

    @Select("<script>" +
            REVIEW_SELECT_COLUMNS +
            "WHERE r.course_instance_id = #{courseInstanceId} AND r.status IN ('PUBLISHED', 'APPROVED') " +
            "<if test='tagIds != null and tagIds.size() > 0'>" +
            "  AND EXISTS (" +
            "    SELECT 1 FROM review_tag rt " +
            "    WHERE rt.review_id = r.id AND rt.tag_id IN " +
            "    <foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>" +
            "  ) " +
            "</if>" +
            REVIEW_ORDER_BY +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "anonymousId", column = "anonymous_id"),
            @Result(property = "voterRecordId", column = "voter_record_id"),
            @Result(property = "anonymousUserKey", column = "anonymous_user_key"),
            @Result(property = "courseInstanceId", column = "course_instance_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseName", column = "course_name"),
            @Result(property = "teacherId", column = "teacher_id"),
            @Result(property = "teacherName", column = "teacher_name"),
            @Result(property = "overallScore", column = "overall_score"),
            @Result(property = "gradingScore", column = "grading_score"),
            @Result(property = "teachingScore", column = "teaching_score"),
            @Result(property = "workloadScore", column = "workload_score"),
            @Result(property = "content", column = "content"),
            @Result(property = "studyTips", column = "review_study_tips"),
            @Result(property = "examType", column = "review_exam_type"),
            @Result(property = "keyChapters", column = "review_key_chapters"),
            @Result(property = "cheatSheetAllowed", column = "review_cheat_sheet_allowed"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "downvoteCount", column = "downvote_count"),
            @Result(property = "status", column = "status"),
            @Result(property = "createTime", column = "create_time"),
    })
    List<ReviewVO> selectByCourseInstanceId(@Param("courseInstanceId") Long courseInstanceId,
                                            @Param("sortBy") String sortBy,
                                            @Param("tagIds") List<Long> tagIds);

    @Select(REVIEW_SELECT_COLUMNS +
            "WHERE r.status IN ('PENDING_AUDIT', 'PENDING_MANUAL', 'PENDING') " +
            "ORDER BY r.create_time ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "anonymousId", column = "anonymous_id"),
            @Result(property = "voterRecordId", column = "voter_record_id"),
            @Result(property = "anonymousUserKey", column = "anonymous_user_key"),
            @Result(property = "courseInstanceId", column = "course_instance_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseName", column = "course_name"),
            @Result(property = "teacherId", column = "teacher_id"),
            @Result(property = "teacherName", column = "teacher_name"),
            @Result(property = "overallScore", column = "overall_score"),
            @Result(property = "gradingScore", column = "grading_score"),
            @Result(property = "teachingScore", column = "teaching_score"),
            @Result(property = "workloadScore", column = "workload_score"),
            @Result(property = "content", column = "content"),
            @Result(property = "studyTips", column = "review_study_tips"),
            @Result(property = "examType", column = "review_exam_type"),
            @Result(property = "keyChapters", column = "review_key_chapters"),
            @Result(property = "cheatSheetAllowed", column = "review_cheat_sheet_allowed"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "downvoteCount", column = "downvote_count"),
            @Result(property = "status", column = "status"),
            @Result(property = "createTime", column = "create_time"),
    })
    List<ReviewVO> selectPendingReviews();

    @Update("UPDATE review SET like_count = like_count + 1 WHERE id = #{reviewId}")
    void incrementLike(Long reviewId);

    @Update("UPDATE review SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{reviewId}")
    void decrementLike(Long reviewId);

    @Update("UPDATE review SET downvote_count = downvote_count + 1 WHERE id = #{reviewId}")
    void incrementDownvote(Long reviewId);

    @Update("UPDATE review SET downvote_count = GREATEST(downvote_count - 1, 0) WHERE id = #{reviewId}")
    void decrementDownvote(Long reviewId);

}
