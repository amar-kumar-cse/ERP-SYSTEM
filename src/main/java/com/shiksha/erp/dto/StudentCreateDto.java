package com.shiksha.erp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCreateDto {

    @NotBlank(message = "Student name is required")
    private String name;

    @NotBlank(message = "Roll number is required")
    private String rollNo;

    private String parentName;

    private String parentPhone;

    @Email(message = "Please provide a valid parent email")
    private String parentEmail;

    @NotBlank(message = "Parent username is required")
    private String parentUsername;

    @NotBlank(message = "Parent password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String parentPassword;

    // optional class batch selection
    private Long classBatchId;
}
