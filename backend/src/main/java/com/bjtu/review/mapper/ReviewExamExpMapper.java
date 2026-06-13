package com.bjtu.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjtu.review.entity.ReviewExamExp;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewExamExpMapper extends BaseMapper<ReviewExamExp> {

    @Insert("""
            INSERT INTO review_exam_exp (review_id, exam_type, study_tips, key_chapters, cheat_sheet_allowed)
            VALUES (#{reviewId}, #{examType}, #{studyTips}, #{keyChapters}, #{cheatSheetAllowed})
            ON DUPLICATE KEY UPDATE
                exam_type = VALUES(exam_type),
                study_tips = VALUES(study_tips),
                key_chapters = VALUES(key_chapters),
                cheat_sheet_allowed = VALUES(cheat_sheet_allowed),
                update_time = CURRENT_TIMESTAMP
            """)
    void upsertBasicExperience(@Param("reviewId") Long reviewId,
                               @Param("examType") String examType,
                               @Param("studyTips") String studyTips,
                               @Param("keyChapters") String keyChapters,
                               @Param("cheatSheetAllowed") Boolean cheatSheetAllowed);

    @Delete("DELETE FROM review_exam_exp WHERE review_id = #{reviewId}")
    void deleteByReviewId(Long reviewId);
}
