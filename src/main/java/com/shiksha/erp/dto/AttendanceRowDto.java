package com.shiksha.erp.dto;

import com.shiksha.erp.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRowDto {

    private Long id;
    private String studentName;
    private String rollNo;
    private LocalDate date;
    private AttendanceStatus status;
    private String markedByName;
}
