package com.shiksha.erp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class BulkReportDto {

    @NotNull(message = "Class batch is required")
    private Long classBatchId;

    private String batchName;

    @NotBlank(message = "Subject name is required")
    private String subject;

    @NotNull(message = "Exam date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate examDate;

    @NotNull(message = "Max marks is required")
    @Min(value = 1, message = "Max marks must be at least 1")
    private Integer maxMarks;

    @Builder.Default
    private List<StudentMarkEntryDto> entries = new ArrayList<>();
}
