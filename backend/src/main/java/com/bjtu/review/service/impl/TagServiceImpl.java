package com.bjtu.review.service.impl;

import com.bjtu.review.entity.Tag;
import com.bjtu.review.mapper.TagMapper;
import com.bjtu.review.service.TagService;
import com.bjtu.review.vo.TagVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public List<TagVO> getAllTags() {
        List<Tag> tags = tagMapper.selectWithCount();
        return tags.stream().map(t -> {
            TagVO vo = new TagVO();
            vo.setId(t.getId());
            vo.setTagName(t.getTagName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Tag createTag(String tagName) {
        Tag tag = new Tag();
        tag.setTagName(tagName);
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public void deleteTag(Long tagId) {
        tagMapper.deleteById(tagId);
    }
}
