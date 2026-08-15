package com.shiksha.erp.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponseDto {

    private Long id;
    private String name;
    private String rollNo;
    private String parentName;
    private String parentPhone;
    private String parentUsername;
    private String parentEmail;
    private Long classBatchId;
    private String batchName;
    private LocalDateTime createdAt;
}
