package com.shiksha.erp.controller;

import com.shiksha.erp.dto.ResourceResponseDto;
import com.shiksha.erp.entity.Resource;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.enums.ResourceType;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.service.ParentStudentHelper;
import com.shiksha.erp.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/student/resources")
@RequiredArgsConstructor
@Slf4j
public class StudentResourceController {

    private final ParentStudentHelper parentStudentHelper;
    private final ResourceService resourceService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping
    public String viewResources(
            @RequestParam(required = false) String subject,
            Model model,
            Authentication auth
    ) {
        Student student = parentStudentHelper.getStudentByParentUsername(auth.getName());

        List<ResourceResponseDto> resources = List.of();
        List<String> subjects = List.of();

        if (student.getClassBatch() != null) {
            Long batchId = student.getClassBatch().getId();
            resources = resourceService.getResourcesForStudent(batchId, subject);
            subjects = resourceService.getSubjectsForBatch(batchId);
        }

        model.addAttribute("student", student);
        model.addAttribute("resources", resources);
        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("activePage", "resources");

        return "student/resources";
    }

    @GetMapping("/download/{id}")
    public Object downloadOrOpenResource(
            @PathVariable Long id,
            Authentication auth
    ) {
        Student student = parentStudentHelper.getStudentByParentUsername(auth.getName());
        Resource resource = resourceService.getResourceForDownload(id);

        if (student.getClassBatch() == null || !student.getClassBatch().getId().equals(resource.getClassBatch().getId())) {
            throw new UnauthorizedAccessException("Access Denied: You do not belong to this study material's batch");
        }

        if (resource.getResourceType() == ResourceType.LINK) {
            String url = resource.getFileUrl();
            if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                throw new BusinessValidationException("Invalid resource URL format");
            }
            return "redirect:" + url;
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(resource.getFileUrl()).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new BusinessValidationException("Invalid file path detected");
        }

        File file = filePath.toFile();
        if (!file.exists()) {
            throw new ResourceNotFoundException("Resource file not found on disk: " + resource.getOriginalFileName());
        }

        String mimeType = URLConnection.guessContentTypeFromName(resource.getOriginalFileName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getOriginalFileName() + "\"")
                .body(new FileSystemResource(file));
    }
}
