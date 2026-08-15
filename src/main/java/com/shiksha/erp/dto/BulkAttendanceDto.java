package com.shiksha.erp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAttendanceDto {

    @NotNull(message = "Class batch must be selected")
    private Long classBatchId;

    private String batchName;

    @NotNull(message = "Attendance date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Builder.Default
    private List<StudentAttendanceEntryDto> entries = new ArrayList<>();
}
