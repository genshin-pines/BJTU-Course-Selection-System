package com.bjtu.review.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVO {
    private int totalRows;
    private int successCount;
    private int skipCount;
    private int failCount;
    private List<ImportFailure> failures = new ArrayList<>();

    @Data
    public static class ImportFailure {
        private int row;
        private String courseCode;
        private String courseName;
        private String reason;

        public ImportFailure(int row, String courseCode, String courseName, String reason) {
            this.row = row;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.reason = reason;
        }
    }
}
