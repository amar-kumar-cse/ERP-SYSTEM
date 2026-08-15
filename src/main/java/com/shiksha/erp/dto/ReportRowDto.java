package com.shiksha.erp.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRowDto {

    private Long id;
    private String studentName;
    private String rollNo;
    private String subject;
    private LocalDate examDate;
    private Integer marks;
    private Integer maxMarks;
    private double percentage;
    private String remarks;
    private String uploadedByName;
}
