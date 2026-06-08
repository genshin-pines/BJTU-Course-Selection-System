package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.entity.Teacher;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.TeacherService;
import com.bjtu.review.vo.TeacherVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;

    public TeacherServiceImpl(TeacherMapper teacherMapper) {
        this.teacherMapper = teacherMapper;
    }

    @Override
    public List<TeacherVO> getAllTeachers() {
        List<Teacher> teachers = teacherMapper.selectList(null);
        return teachers.stream().map(t -> {
            TeacherVO vo = new TeacherVO();
            vo.setId(t.getId());
            vo.setTeacherName(t.getTeacherName());
            vo.setDepartment(t.getDepartment());
            vo.setAvgScore(t.getAvgScore());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public TeacherVO getTeacherDetail(Long id) {
        Teacher t = teacherMapper.selectById(id);
        if (t == null) {
            throw new RuntimeException("教师不存在");
        }
        TeacherVO vo = new TeacherVO();
        vo.setId(t.getId());
        vo.setTeacherName(t.getTeacherName());
        vo.setDepartment(t.getDepartment());
        vo.setAvgScore(t.getAvgScore());
        return vo;
    }
}
