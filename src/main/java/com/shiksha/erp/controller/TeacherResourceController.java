package com.shiksha.erp.controller;

import com.shiksha.erp.dto.ResourceCreateDto;
import com.shiksha.erp.dto.ResourceResponseDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Resource;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.enums.ResourceType;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.service.ResourceService;
import com.shiksha.erp.service.TeacherAccessHelper;
import jakarta.validation.Valid;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/teacher/resources")
@RequiredArgsConstructor
@Slf4j
public class TeacherResourceController {

    private final TeacherAccessHelper teacherAccessHelper;
    private final ResourceService resourceService;
    private final ClassBatchRepository classBatchRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping
    public String listResources(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String subject,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> batches = teacherAccessHelper.getTeacherBatches(teacher);

        Long selectedBatchId = batchId;
        if (selectedBatchId == null && !batches.isEmpty()) {
            selectedBatchId = batches.get(0).getId();
        }

        List<ResourceResponseDto> resources = List.of();
        List<String> subjects = List.of();
        ClassBatch selectedBatch = null;

        if (selectedBatchId != null) {
            if (!teacherAccessHelper.isBatchOwnedByTeacher(selectedBatchId, teacher)) {
                redirectAttributes.addFlashAttribute("errorMsg", "Unauthorized: You do not have access to this class batch");
                return "redirect:/teacher/resources";
            }
            resources = resourceService.getResourcesForBatch(selectedBatchId, subject, teacher);
            subjects = resourceService.getSubjectsForBatch(selectedBatchId);
            selectedBatch = classBatchRepository.findById(selectedBatchId).orElse(null);
        }

        model.addAttribute("batches", batches);
        model.addAttribute("selectedBatchId", selectedBatchId);
        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("resources", resources);
        model.addAttribute("activePage", "resources");

        return "teacher/resources/list";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model, Authentication auth) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> batches = teacherAccessHelper.getTeacherBatches(teacher);

        ResourceCreateDto dto = ResourceCreateDto.builder()
                .resourceType(ResourceType.FILE)
                .subject(teacher.getSubject() != null ? teacher.getSubject() : "")
                .build();

        model.addAttribute("resourceCreateDto", dto);
        model.addAttribute("batches", batches);
        model.addAttribute("activePage", "resources");

        return "teacher/resources/upload";
    }

    @PostMapping("/upload")
    public String handleUpload(
            @Valid @ModelAttribute("resourceCreateDto") ResourceCreateDto dto,
            BindingResult bindingResult,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        if (bindingResult.hasErrors()) {
            model.addAttribute("batches", teacherAccessHelper.getTeacherBatches(teacher));
            model.addAttribute("activePage", "resources");
            return "teacher/resources/upload";
        }

        try {
            resourceService.saveResource(dto, teacher);
            redirectAttributes.addFlashAttribute("successMsg", "Resource uploaded and shared successfully!");
            return "redirect:/teacher/resources?batchId=" + dto.getClassBatchId();
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("batches", teacherAccessHelper.getTeacherBatches(teacher));
            model.addAttribute("activePage", "resources");
            return "teacher/resources/upload";
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @PathVariable Long id,
            Authentication auth
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        com.shiksha.erp.entity.Resource resource = resourceService.getResourceForDownload(id);

        if (!teacherAccessHelper.isBatchOwnedByTeacher(resource.getClassBatch().getId(), teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You do not have access to study material in this batch");
        }

        if (resource.getResourceType() != ResourceType.FILE) {
            throw new BusinessValidationException("Requested resource is a link, not a downloadable file");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(resource.getFileUrl()).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new BusinessValidationException("Path traversal attempt detected");
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

    @PostMapping("/delete/{id}")
    public String deleteResource(
            @PathVariable Long id,
            @RequestParam(required = false) Long batchId,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        try {
            resourceService.deleteResource(id, teacher);
            redirectAttributes.addFlashAttribute("successMsg", "Resource deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        return "redirect:/teacher/resources" + (batchId != null ? "?batchId=" + batchId : "");
    }
}
