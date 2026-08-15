package com.shiksha.erp.service;

import com.shiksha.erp.dto.ResourceCreateDto;
import com.shiksha.erp.dto.ResourceResponseDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Resource;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.enums.ResourceType;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ClassBatchRepository classBatchRepository;
    private final TeacherAccessHelper teacherAccessHelper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB limit
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "zip", "jpg", "jpeg", "png", "txt"
    );

    @Transactional
    public void saveResource(ResourceCreateDto dto, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(dto.getClassBatchId(), teacher)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this class batch");
        }

        ClassBatch batch = classBatchRepository.findById(dto.getClassBatchId())
                .orElseThrow(() -> new RuntimeException("Class batch not found: " + dto.getClassBatchId()));

        String fileUrl;
        String originalFileName = null;
        Long fileSize = null;

        if (dto.getResourceType() == ResourceType.FILE) {
            MultipartFile file = dto.getFile();
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Please select a file to upload");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File size cannot exceed 10 MB");
            }

            String origName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = "";
            int dotIdx = origName.lastIndexOf('.');
            if (dotIdx > 0) {
                extension = origName.substring(dotIdx + 1).toLowerCase();
            }

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("Disallowed file type (." + extension + "). Allowed: PDF, DOC, PPT, XLS, ZIP, Images, TXT");
            }

            // Safe filename with UUID prefix
            String sanitizedName = origName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedFileName = UUID.randomUUID().toString() + "_" + sanitizedName;

            try {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);
                Path targetLocation = uploadPath.resolve(storedFileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                fileUrl = storedFileName;
                originalFileName = origName;
                fileSize = file.getSize();
            } catch (IOException ex) {
                log.error("Could not upload file: {}", origName, ex);
                throw new RuntimeException("Could not store file. Please try again!", ex);
            }
        } else {
            // LINK resource
            if (dto.getLinkUrl() == null || dto.getLinkUrl().isBlank()) {
                throw new IllegalArgumentException("External resource URL is required");
            }
            fileUrl = dto.getLinkUrl().trim();
        }

        Resource resource = Resource.builder()
                .title(dto.getTitle().trim())
                .resourceType(dto.getResourceType())
                .fileUrl(fileUrl)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .classBatch(batch)
                .subject(dto.getSubject().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .uploadedBy(teacher)
                .build();

        resourceRepository.save(resource);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDto> getResourcesForBatch(Long batchId, String subject, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this class batch");
        }

        List<Resource> resources = resourceRepository.findByClassBatchIdOrderByUploadedAtDesc(batchId);

        return resources.stream()
                .filter(r -> subject == null || subject.isBlank() || r.getSubject().equalsIgnoreCase(subject.trim()))
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDto> getResourcesForStudent(Long batchId, String subject) {
        if (batchId == null) {
            return Collections.emptyList();
        }

        List<Resource> resources = resourceRepository.findByClassBatchIdOrderByUploadedAtDesc(batchId);

        return resources.stream()
                .filter(r -> subject == null || subject.isBlank() || r.getSubject().equalsIgnoreCase(subject.trim()))
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void deleteResource(Long id, Teacher teacher) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + id));

        if (!teacherAccessHelper.isBatchOwnedByTeacher(resource.getClassBatch().getId(), teacher)) {
            throw new RuntimeException("Unauthorized: You can only delete resources for your assigned batches");
        }

        // file delete from disk if FILE type
        if (resource.getResourceType() == ResourceType.FILE && resource.getFileUrl() != null) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(resource.getFileUrl()).normalize();
                File file = filePath.toFile();
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception ex) {
                log.warn("Failed to delete physical file: {}", resource.getFileUrl(), ex);
            }
        }

        resourceRepository.delete(resource);
    }

    @Transactional(readOnly = true)
    public Resource getResourceForDownload(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<String> getSubjectsForBatch(Long batchId) {
        if (batchId == null) return Collections.emptyList();
        return resourceRepository.findDistinctSubjectsByClassBatch(batchId);
    }

    public ResourceResponseDto toResponseDto(Resource r) {
        return ResourceResponseDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .resourceType(r.getResourceType())
                .fileUrl(r.getFileUrl())
                .originalFileName(r.getOriginalFileName())
                .fileSize(r.getFileSize())
                .formattedFileSize(formatFileSize(r.getFileSize()))
                .classBatchId(r.getClassBatch().getId())
                .batchName(r.getClassBatch().getBatchName())
                .subject(r.getSubject())
                .description(r.getDescription())
                .uploadedByName(r.getUploadedBy() != null ? r.getUploadedBy().getFullName() : "Faculty")
                .uploadedAt(r.getUploadedAt())
                .build();
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "-";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
