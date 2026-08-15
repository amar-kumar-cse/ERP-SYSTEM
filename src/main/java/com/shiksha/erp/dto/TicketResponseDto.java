package com.shiksha.erp.dto;

import com.shiksha.erp.enums.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDto {

    private Long id;
    private String raisedByUsername;
    private String raisedByRole;
    private String title;
    private String message;
    private TicketStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
