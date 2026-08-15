package com.shiksha.erp.dto;

import com.shiksha.erp.enums.AttendanceStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttendanceEntryDto {

    private Long studentId;
    private String studentName;
    private String rollNo;

    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;
}
