package com.shiksha.erp.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassBatchResponseDto {

    private Long id;
    private String batchName;
    private String timing;
    private String days;
    private int studentCount;

    @Builder.Default
    private List<String> assignedTeachers = new ArrayList<>();

    private LocalDateTime createdAt;
}
