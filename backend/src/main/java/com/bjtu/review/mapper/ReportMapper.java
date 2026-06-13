package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Report;
import com.bjtu.review.vo.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    String REPORT_SELECT_COLUMNS = "SELECT rp.*, rv.content AS review_content, " +
            "rv.course_instance_id, rv.course_id, rv.teacher_id, " +
            "COALESCE(c.course_name, cb.course_name) AS course_name, " +
            "ci.semester, ci.class_name, t.teacher_name, " +
            "COALESCE(author_record.display_name, '匿名用户') AS anonymous_id, " +
            "reporter_record.display_name AS reporter_anonymous_id " +
            "FROM report rp " +
            "LEFT JOIN review rv ON rp.review_id = rv.id " +
            "LEFT JOIN voter_record author_record ON rv.voter_record_id = author_record.id " +
            "LEFT JOIN voter_record reporter_record ON rp.reporter_record_id = reporter_record.id " +
            "LEFT JOIN course c ON rv.course_id = c.id " +
            "LEFT JOIN course_instance ci ON rv.course_instance_id = ci.id " +
            "LEFT JOIN course_base cb ON ci.course_base_id = cb.id " +
            "LEFT JOIN teacher t ON rv.teacher_id = t.id ";

    @Select(REPORT_SELECT_COLUMNS +
            "WHERE rp.status = 'PENDING' " +
            "ORDER BY rp.create_time ASC")
    List<ReportVO> selectPendingReports();

    @Select(REPORT_SELECT_COLUMNS +
            "ORDER BY rp.create_time DESC")
    List<ReportVO> selectAllReports();
}
