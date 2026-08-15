package com.shiksha.erp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherUpdateDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Subject is required")
    private String subject;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate joiningDate;
}
