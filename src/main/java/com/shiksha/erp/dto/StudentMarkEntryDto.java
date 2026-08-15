package com.shiksha.erp.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMarkEntryDto {

    private Long studentId;
    private String studentName;
    private String rollNo;
    private Integer marks;
    private String remarks;
}
