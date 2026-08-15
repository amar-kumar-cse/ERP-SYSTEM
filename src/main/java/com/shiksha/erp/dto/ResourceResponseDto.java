package com.shiksha.erp.dto;

import com.shiksha.erp.enums.ResourceType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponseDto {

    private Long id;
    private String title;
    private ResourceType resourceType;
    private String fileUrl;
    private String originalFileName;
    private String formattedFileSize;
    private Long fileSize;
    private Long classBatchId;
    private String batchName;
    private String subject;
    private String description;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
