package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.ReviewTag;
import com.bjtu.review.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewTagMapper extends BaseMapper<ReviewTag> {

    @Select("SELECT t.* FROM tag t " +
            "INNER JOIN review_tag rt ON t.id = rt.tag_id " +
            "WHERE rt.review_id = #{reviewId}")
    List<Tag> selectTagsByReviewId(Long reviewId);

    @Insert("INSERT INTO review_tag (review_id, tag_id) VALUES (#{reviewId}, #{tagId})")
    void insertTag(@Param("reviewId") Long reviewId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM review_tag WHERE review_id = #{reviewId}")
    void deleteByReviewId(Long reviewId);
}
