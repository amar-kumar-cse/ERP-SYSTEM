package com.shiksha.erp.integration;

import com.shiksha.erp.dto.ResourceCreateDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Resource;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.enums.ResourceType;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ResourceRepository;
import com.shiksha.erp.service.ResourceService;
import com.shiksha.erp.service.TeacherAccessHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceUploadTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ClassBatchRepository classBatchRepository;

    @Mock
    private TeacherAccessHelper teacherAccessHelper;

    @InjectMocks
    private ResourceService resourceService;

    private Teacher teacher;
    private ClassBatch classBatch;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceService, "uploadDir", "./target/test-uploads");

        teacher = Teacher.builder().id(1L).phone("9876543210").build();
        classBatch = ClassBatch.builder().id(10L).batchName("Class 10").build();

        lenient().when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);
        lenient().when(classBatchRepository.findById(10L)).thenReturn(Optional.of(classBatch));
    }

    @Test
    @DisplayName("Resource upload: disallowed executable extension (.exe) should be rejected")
    void testDisallowedExtension_ExeRejected() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "dummy binary content".getBytes()
        );

        ResourceCreateDto dto = ResourceCreateDto.builder()
                .classBatchId(10L)
                .resourceType(ResourceType.FILE)
                .title("Software Setup")
                .subject("Computer")
                .file(exeFile)
                .build();

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                resourceService.saveResource(dto, teacher)
        );

        assertTrue(ex.getMessage().contains("Disallowed file type"));
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    @DisplayName("Resource upload: disallowed script extension (.sh) should be rejected")
    void testDisallowedExtension_ShRejected() {
        MockMultipartFile shFile = new MockMultipartFile(
                "file",
                "script.sh",
                "text/x-shellscript",
                "echo 'hello'".getBytes()
        );

        ResourceCreateDto dto = ResourceCreateDto.builder()
                .classBatchId(10L)
                .resourceType(ResourceType.FILE)
                .title("Shell Script")
                .subject("Linux")
                .file(shFile)
                .build();

        assertThrows(BusinessValidationException.class, () ->
                resourceService.saveResource(dto, teacher)
        );
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    @DisplayName("Resource upload: oversized file (>10MB) should be rejected")
    void testOversizedFile_Rejected() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge_book.pdf",
                "application/pdf",
                largeBytes
        );

        ResourceCreateDto dto = ResourceCreateDto.builder()
                .classBatchId(10L)
                .resourceType(ResourceType.FILE)
                .title("Large Book")
                .subject("Physics")
                .file(largeFile)
                .build();

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                resourceService.saveResource(dto, teacher)
        );

        assertTrue(ex.getMessage().contains("File size cannot exceed 10 MB"));
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    @DisplayName("Resource upload: valid PDF file should be accepted and stored")
    void testValidPdf_Accepted() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "chapter1_notes.pdf",
                "application/pdf",
                "Valid PDF sample content".getBytes()
        );

        ResourceCreateDto dto = ResourceCreateDto.builder()
                .classBatchId(10L)
                .resourceType(ResourceType.FILE)
                .title("Chapter 1 Notes")
                .subject("Mathematics")
                .file(pdfFile)
                .build();

        assertDoesNotThrow(() -> resourceService.saveResource(dto, teacher));
        verify(resourceRepository, times(1)).save(any(Resource.class));
    }
}
