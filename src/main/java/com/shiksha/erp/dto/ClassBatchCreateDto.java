package com.shiksha.erp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassBatchCreateDto {

    @NotBlank(message = "Batch name is required")
    private String batchName;

    @NotBlank(message = "Timing is required (e.g. 4:00 PM - 6:00 PM)")
    private String timing;

    @NotBlank(message = "Days are required (e.g. Mon, Wed, Fri)")
    private String days;
}
