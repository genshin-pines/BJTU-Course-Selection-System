package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.ReviewVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewVoteMapper extends BaseMapper<ReviewVote> {
    @Select("SELECT rv.review_id " +
            "FROM review_vote rv " +
            "JOIN review r ON rv.review_id = r.id " +
            "WHERE rv.student_id = #{studentId} AND r.course_id = #{courseId}")
    List<Long> selectLikedReviewIds(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
