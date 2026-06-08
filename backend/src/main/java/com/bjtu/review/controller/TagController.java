package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.service.TagService;
import com.bjtu.review.vo.TagVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<TagVO>> getAllTags() {
        return Result.ok(tagService.getAllTags());
    }
}
