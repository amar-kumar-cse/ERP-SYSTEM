package com.shiksha.erp.dto;

import com.shiksha.erp.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceCreateDto {

    @NotBlank(message = "Resource title is required")
    private String title;

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;

    @NotNull(message = "Class batch must be selected")
    private Long classBatchId;

    @NotBlank(message = "Subject name is required")
    private String subject;

    private String description;

    // file upload ke liye
    private MultipartFile file;

    // link type ke liye
    private String linkUrl;
}
