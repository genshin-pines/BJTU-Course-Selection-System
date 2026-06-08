package com.bjtu.review.service;

import com.bjtu.review.entity.Tag;
import com.bjtu.review.vo.TagVO;

import java.util.List;

public interface TagService {
    List<TagVO> getAllTags();
    Tag createTag(String tagName);
    void deleteTag(Long tagId);
}
