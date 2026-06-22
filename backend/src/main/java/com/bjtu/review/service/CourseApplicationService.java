package com.bjtu.review.service;

import com.bjtu.review.dto.CourseApplicationRequest;
import com.bjtu.review.vo.CourseApplicationVO;
import java.util.List;

public interface CourseApplicationService {
    /** 学生提交课程申请 */
    Long submitApplication(Long studentId, CourseApplicationRequest request);

    /** 学生查看自己的申请 */
    List<CourseApplicationVO> getMyApplications(Long studentId);

    /** 管理员查看待审核申请 */
    List<CourseApplicationVO> getPendingApplications();

    /** 管理员审核通过：创建课程/教师/实例 + 发布评价 */
    void approve(Long adminId, Long applicationId, String reason);

    /** 管理员审核拒绝 */
    void reject(Long adminId, Long applicationId, String reason);
}
