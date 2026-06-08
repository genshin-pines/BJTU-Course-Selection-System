package com.bjtu.review.service;

import com.bjtu.review.entity.Teacher;
import com.bjtu.review.vo.TeacherVO;

import java.util.List;

public interface TeacherService {
    List<TeacherVO> getAllTeachers();
    TeacherVO getTeacherDetail(Long id);
}
