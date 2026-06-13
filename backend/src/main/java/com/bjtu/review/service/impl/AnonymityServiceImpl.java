package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.entity.Student;
import com.bjtu.review.entity.VoterRecord;
import com.bjtu.review.mapper.StudentMapper;
import com.bjtu.review.mapper.VoterRecordMapper;
import com.bjtu.review.service.AnonymityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AnonymityServiceImpl implements AnonymityService {

    private static final String REVIEW_SCOPE = "COURSE_REVIEW";
    private static final long DEFAULT_INSTANCE_ID = 0L;
    private static final String HASH_SALT = "bjtu-review-anonymous-v1";

    private final VoterRecordMapper voterRecordMapper;
    private final StudentMapper studentMapper;

    public AnonymityServiceImpl(VoterRecordMapper voterRecordMapper, StudentMapper studentMapper) {
        this.voterRecordMapper = voterRecordMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    @Transactional
    public VoterRecord getOrCreateCourseReviewRecord(Long studentId, Long courseId, Long teacherId, Long courseInstanceId) {
        Long normalizedInstanceId = normalizeInstanceId(courseInstanceId);
        LambdaQueryWrapper<VoterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoterRecord::getStudentId, studentId)
                .eq(VoterRecord::getScopeType, REVIEW_SCOPE)
                .eq(VoterRecord::getCourseId, courseId)
                .eq(VoterRecord::getTeacherId, teacherId)
                .eq(VoterRecord::getCourseInstanceId, normalizedInstanceId);
        VoterRecord existing = voterRecordMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        String anonymousKey = buildAnonymousKey(studentId, courseId, teacherId, normalizedInstanceId);
        VoterRecord record = new VoterRecord();
        record.setStudentId(studentId);
        record.setAnonymousKey(anonymousKey);
        record.setDisplayName(buildDisplayName(studentId, anonymousKey));
        record.setScopeType(REVIEW_SCOPE);
        record.setCourseId(courseId);
        record.setTeacherId(teacherId);
        record.setCourseInstanceId(normalizedInstanceId);
        voterRecordMapper.insert(record);
        return record;
    }

    @Override
    public boolean isOwner(Long studentId, Long voterRecordId) {
        if (studentId == null || voterRecordId == null) {
            return false;
        }
        VoterRecord record = voterRecordMapper.selectById(voterRecordId);
        return record != null && studentId.equals(record.getStudentId());
    }

    private Long normalizeInstanceId(Long courseInstanceId) {
        return courseInstanceId == null ? DEFAULT_INSTANCE_ID : courseInstanceId;
    }

    private String buildDisplayName(Long studentId, String anonymousKey) {
        Student student = studentMapper.selectById(studentId);
        if (student != null && student.getAnonymousId() != null && !student.getAnonymousId().isBlank()) {
            return student.getAnonymousId();
        }
        return "匿名用户" + anonymousKey.substring(0, 8).toUpperCase();
    }

    private String buildAnonymousKey(Long studentId, Long courseId, Long teacherId, Long courseInstanceId) {
        String raw = HASH_SALT + ":" + studentId + ":" + courseId + ":" + teacherId + ":" + courseInstanceId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
