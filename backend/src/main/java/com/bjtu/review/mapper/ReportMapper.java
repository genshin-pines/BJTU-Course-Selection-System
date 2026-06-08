package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.Report;
import com.bjtu.review.vo.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    @Select("SELECT rp.*, rv.content as review_content " +
            "FROM report rp " +
            "LEFT JOIN review rv ON rp.review_id = rv.id " +
            "WHERE rp.status = 'PENDING' " +
            "ORDER BY rp.create_time ASC")
    List<ReportVO> selectPendingReports();

    @Select("SELECT rp.*, rv.content as review_content " +
            "FROM report rp " +
            "LEFT JOIN review rv ON rp.review_id = rv.id " +
            "ORDER BY rp.create_time DESC")
    List<ReportVO> selectAllReports();
}
