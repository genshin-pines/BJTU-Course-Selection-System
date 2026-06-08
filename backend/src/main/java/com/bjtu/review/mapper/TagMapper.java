package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    @Select("SELECT t.*, COUNT(rt.id) as count FROM tag t " +
            "LEFT JOIN review_tag rt ON t.id = rt.tag_id " +
            "GROUP BY t.id ORDER BY count DESC")
    List<Tag> selectWithCount();
}
