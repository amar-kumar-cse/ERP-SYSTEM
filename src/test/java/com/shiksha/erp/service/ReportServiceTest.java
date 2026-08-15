package com.shiksha.erp.service;

import com.shiksha.erp.dto.BulkReportDto;
import com.shiksha.erp.dto.ReportRowDto;
import com.shiksha.erp.dto.StudentMarkEntryDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassBatchRepository classBatchRepository;

    @Mock
    private TeacherAccessHelper teacherAccessHelper;

    @InjectMocks
    private ReportService reportService;

    private Teacher teacher;
    private ClassBatch classBatch;
    private Student student;

    @BeforeEach
    void setUp() {
        teacher = Teacher.builder().id(1L).phone("9876543210").subject("Mathematics").build();
        classBatch = ClassBatch.builder().id(10L).batchName("Class 10").build();
        student = Student.builder().id(100L).name("Aarav").rollNo("SHK-001").classBatch(classBatch).build();
    }

    @Test
    @DisplayName("saveBulkReport: should save marks entry and update without duplicates")
    void testSaveBulkReport_NewEntry() {
        StudentMarkEntryDto entry = StudentMarkEntryDto.builder()
                .studentId(100L)
                .marks(85)
                .remarks("Good work")
                .build();

        BulkReportDto dto = BulkReportDto.builder()
                .classBatchId(10L)
                .subject("Mathematics")
                .examDate(LocalDate.of(2026, 8, 15))
                .maxMarks(100)
                .entries(List.of(entry))
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);
        when(classBatchRepository.findById(10L)).thenReturn(Optional.of(classBatch));
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));
        when(reportRepository.findByStudentIdAndSubjectAndExamDate(100L, "Mathematics", LocalDate.of(2026, 8, 15)))
                .thenReturn(Optional.empty());

        reportService.saveBulkReport(dto, teacher);

        verify(reportRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("saveBulkReport: marks greater than maxMarks should throw BusinessValidationException")
    void testSaveBulkReport_MarksExceedMax_ThrowsException() {
        StudentMarkEntryDto entry = StudentMarkEntryDto.builder()
                .studentId(100L)
                .marks(105)
                .build();

        BulkReportDto dto = BulkReportDto.builder()
                .classBatchId(10L)
                .subject("Mathematics")
                .examDate(LocalDate.of(2026, 8, 15))
                .maxMarks(100)
                .entries(List.of(entry))
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);
        when(classBatchRepository.findById(10L)).thenReturn(Optional.of(classBatch));
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));

        assertThrows(BusinessValidationException.class, () -> reportService.saveBulkReport(dto, teacher));
        verify(reportRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("getReportsByBatchAndFilter: should correctly calculate percentage")
    void testGetReportsByBatchAndFilter_PercentageCalculation() {
        Report report = Report.builder()
                .id(1L)
                .student(student)
                .classBatch(classBatch)
                .subject("Mathematics")
                .examDate(LocalDate.of(2026, 8, 15))
                .marks(85)
                .maxMarks(100)
                .uploadedBy(teacher)
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);
        when(reportRepository.findByClassBatchId(10L)).thenReturn(List.of(report));

        List<ReportRowDto> results = reportService.getReportsByBatchAndFilter(10L, "Mathematics", null, teacher);

        assertEquals(1, results.size());
        assertEquals(85.0, results.get(0).getPercentage());
    }
}
