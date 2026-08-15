package com.shiksha.erp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentUpdateDto {

    @NotBlank(message = "Student name is required")
    private String name;

    private String parentName;

    private String parentPhone;

    // rollNo aur parentUsername change nahi honge
    private Long classBatchId;
}
