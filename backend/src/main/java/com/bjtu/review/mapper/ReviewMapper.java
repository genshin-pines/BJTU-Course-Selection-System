package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Review;
import com.bjtu.review.vo.ReviewVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT r.*, s.anonymous_id, c.course_name, t.teacher_name " +
            "FROM review r " +
            "LEFT JOIN student s ON r.student_id = s.id " +
            "LEFT JOIN course c ON r.course_id = c.id " +
            "LEFT JOIN teacher t ON r.teacher_id = t.id " +
            "WHERE r.course_id = #{courseId} AND r.status IN ('PUBLISHED', 'APPROVED') " +
            "ORDER BY r.like_count DESC, r.create_time DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "anonymousId", column = "anonymous_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseName", column = "course_name"),
            @Result(property = "teacherId", column = "teacher_id"),
            @Result(property = "teacherName", column = "teacher_name"),
            @Result(property = "overallScore", column = "overall_score"),
            @Result(property = "gradingScore", column = "grading_score"),
            @Result(property = "studyTips", column = "study_tips"),
            @Result(property = "examType", column = "exam_type"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "status", column = "status"),
            @Result(property = "createTime", column = "create_time"),
    })
    List<ReviewVO> selectByCourseId(Long courseId);

    @Select("SELECT r.*, s.anonymous_id, c.course_name, t.teacher_name " +
            "FROM review r " +
            "LEFT JOIN student s ON r.student_id = s.id " +
            "LEFT JOIN course c ON r.course_id = c.id " +
            "LEFT JOIN teacher t ON r.teacher_id = t.id " +
            "WHERE r.status IN ('PENDING_AUDIT', 'PENDING_MANUAL', 'PENDING') " +
            "ORDER BY r.create_time ASC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "anonymousId", column = "anonymous_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "courseName", column = "course_name"),
            @Result(property = "teacherId", column = "teacher_id"),
            @Result(property = "teacherName", column = "teacher_name"),
            @Result(property = "overallScore", column = "overall_score"),
            @Result(property = "gradingScore", column = "grading_score"),
            @Result(property = "studyTips", column = "study_tips"),
            @Result(property = "examType", column = "exam_type"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "status", column = "status"),
            @Result(property = "createTime", column = "create_time"),
    })
    List<ReviewVO> selectPendingReviews();

    @Update("UPDATE review SET like_count = like_count + 1 WHERE id = #{reviewId}")
    void incrementLike(Long reviewId);

    @Update("UPDATE review SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{reviewId}")
    void decrementLike(Long reviewId);
}
