package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.ReviewVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewVoteMapper extends BaseMapper<ReviewVote> {
    @Select("<script>" +
            "SELECT rv.review_id " +
            "FROM review_vote rv " +
            "JOIN review r ON rv.review_id = r.id " +
            "LEFT JOIN voter_record vr ON rv.voter_record_id = vr.id " +
            "WHERE 1 = 1 " +
            "<choose>" +
            "  <when test='courseInstanceId != null'> AND r.course_instance_id = #{courseInstanceId} </when>" +
            "  <otherwise> AND r.course_id = #{courseId} </otherwise>" +
            "</choose>" +
            "AND vr.student_id = #{studentId} " +
            "AND rv.vote_type = #{voteType}" +
            "</script>")
    List<Long> selectVotedReviewIds(@Param("studentId") Long studentId,
                                    @Param("courseId") Long courseId,
                                    @Param("courseInstanceId") Long courseInstanceId,
                                    @Param("voteType") String voteType);

    @Select("SELECT rv.review_id " +
            "FROM review_vote rv " +
            "JOIN review r ON rv.review_id = r.id " +
            "LEFT JOIN voter_record vr ON rv.voter_record_id = vr.id " +
            "WHERE r.course_instance_id = #{courseInstanceId} " +
            "AND vr.student_id = #{studentId} " +
            "AND rv.vote_type = #{voteType}")
    List<Long> selectVotedReviewIdsByInstance(@Param("studentId") Long studentId,
                                              @Param("courseInstanceId") Long courseInstanceId,
                                              @Param("voteType") String voteType);
}
