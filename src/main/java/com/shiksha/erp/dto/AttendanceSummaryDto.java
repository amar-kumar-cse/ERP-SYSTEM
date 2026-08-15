package com.shiksha.erp.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryDto {

    private Long studentId;
    private String studentName;
    private String rollNo;
    private long totalClasses;
    private long present;
    private long absent;
    private double percentage;
}
