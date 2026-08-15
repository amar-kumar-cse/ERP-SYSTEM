package com.shiksha.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResolveDto {

    @NotBlank(message = "Resolution note is required")
    @Size(max = 500, message = "Admin note cannot exceed 500 characters")
    private String adminNote;
}
