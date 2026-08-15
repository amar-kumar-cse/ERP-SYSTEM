package com.shiksha.erp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchAssignDto {

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotNull(message = "Class Batch ID is required")
    private Long classBatchId;
}
