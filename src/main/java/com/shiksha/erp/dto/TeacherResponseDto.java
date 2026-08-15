package com.shiksha.erp.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String subject;
    private LocalDate joiningDate;
    private String username;
    private String email;

    @Builder.Default
    private List<String> assignedBatches = new ArrayList<>();

    private LocalDateTime createdAt;
}
