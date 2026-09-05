package com.shiksha.erp.service;

import com.shiksha.erp.dto.BulkReportDto;
import com.shiksha.erp.dto.ResourceCreateDto;
import com.shiksha.erp.dto.StudentMarkEntryDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.ResourceType;
import com.shiksha.erp.enums.Role;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.FeeRepository;
import com.shiksha.erp.repository.HelpTicketRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.ResourceRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import com.shiksha.erp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriticalBugsVerificationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ClassBatchRepository classBatchRepository;
    @Mock
    private TeacherBatchRepository teacherBatchRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private FeeRepository feeRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private HelpTicketRepository helpTicketRepository;
    @Mock
    private TeacherAccessHelper teacherAccessHelper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private ResourceService resourceService;

    @InjectMocks
    private StudentService studentService;

    @InjectMocks
    private TeacherService teacherService;

    @InjectMocks
    private ClassBatchService classBatchService;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Phase 1.1: Account lockout reflects in UserDetails.isAccountNonLocked()")
    void testAccountLockoutEnforcement() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded_pass")
                .role(Role.PARENT)
                .enabled(true)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(loginAttemptService.isBlocked("testuser")).thenReturn(true);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");
        assertFalse(userDetails.isAccountNonLocked(), "Account should be locked when LoginAttemptService.isBlocked() is true");
    }

    @Test
    @DisplayName("Phase 1.3: Study resource LINK protocol validation blocks non-http(s) URLs")
    void testResourceLinkProtocolValidation() {
        ClassBatch batch = ClassBatch.builder().id(1L).batchName("Batch 1").build();
        Teacher teacher = Teacher.builder().id(1L).firstName("Amit").lastName("Kumar").build();

        when(classBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(teacherAccessHelper.isBatchOwnedByTeacher(1L, teacher)).thenReturn(true);

        ResourceCreateDto badDto = ResourceCreateDto.builder()
                .classBatchId(1L)
                .title("Malicious Resource")
                .resourceType(ResourceType.LINK)
                .linkUrl("javascript:alert(1)")
                .build();

        assertThrows(BusinessValidationException.class, () -> resourceService.saveResource(badDto, teacher));

        ResourceCreateDto validDto = ResourceCreateDto.builder()
                .classBatchId(1L)
                .title("Valid Resource")
                .subject("Science")
                .resourceType(ResourceType.LINK)
                .linkUrl("https://example.com/notes.pdf")
                .build();

        when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertDoesNotThrow(() -> resourceService.saveResource(validDto, teacher));
    }

    @Test
    @DisplayName("Phase 2: Student delete prevented when historical attendance records exist")
    void testStudentSafeDelete() {
        Student student = Student.builder().id(1L).name("Aarav").build();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(attendanceRepository.countByStudentId(1L)).thenReturn(5L);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> studentService.deleteStudent(1L));
        assertTrue(ex.getMessage().contains("attendance record(s)"));
        verify(studentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Phase 2: Teacher delete prevented when report card records exist")
    void testTeacherSafeDelete() {
        Teacher teacher = Teacher.builder().id(2L).firstName("Pooja").lastName("Verma").build();
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(attendanceRepository.countByMarkedById(2L)).thenReturn(0L);
        when(reportRepository.countByUploadedById(2L)).thenReturn(12L);
        when(resourceRepository.countByUploadedById(2L)).thenReturn(0L);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> teacherService.deleteTeacher(2L));
        assertTrue(ex.getMessage().contains("report card(s)"));
        verify(teacherRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Phase 2: ClassBatch delete prevented when linked batch records exist")
    void testClassBatchSafeDelete() {
        ClassBatch batch = ClassBatch.builder().id(3L).batchName("Batch C").build();
        when(classBatchRepository.findById(3L)).thenReturn(Optional.of(batch));
        when(studentRepository.countByClassBatchId(3L)).thenReturn(0L);
        when(attendanceRepository.countByClassBatchId(3L)).thenReturn(4L);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> classBatchService.delete(3L));
        assertTrue(ex.getMessage().contains("attendance record(s)"));
        verify(classBatchRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Phase 4.4: Report card marks distinguish absent from blank entries")
    @SuppressWarnings("unchecked")
    void testReportMarksVsAbsentDistinction() {
        ClassBatch batch = ClassBatch.builder().id(1L).batchName("Batch 1").build();
        Teacher teacher = Teacher.builder().id(1L).firstName("Amit").lastName("Kumar").build();
        Student student1 = Student.builder().id(10L).name("Rahul").build();
        Student student2 = Student.builder().id(20L).name("Sneha").build();
        Student student3 = Student.builder().id(30L).name("Priya").build();

        when(classBatchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(teacherAccessHelper.isBatchOwnedByTeacher(1L, teacher)).thenReturn(true);
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(20L)).thenReturn(Optional.of(student2));
        // student3 not called because it has null marks & absent false

        StudentMarkEntryDto entry1 = StudentMarkEntryDto.builder()
                .studentId(10L)
                .marks(85)
                .absent(false)
                .build();

        StudentMarkEntryDto entry2 = StudentMarkEntryDto.builder()
                .studentId(20L)
                .marks(null)
                .absent(true)
                .build();

        StudentMarkEntryDto entry3 = StudentMarkEntryDto.builder()
                .studentId(30L)
                .marks(null)
                .absent(false)
                .build();

        BulkReportDto bulkDto = BulkReportDto.builder()
                .classBatchId(1L)
                .subject("Physics")
                .examDate(LocalDate.now())
                .maxMarks(100)
                .entries(List.of(entry1, entry2, entry3))
                .build();

        reportService.saveBulkReport(bulkDto, teacher);

        ArgumentCaptor<List<Report>> captor = ArgumentCaptor.forClass(List.class);
        verify(reportRepository).saveAll(captor.capture());

        List<Report> savedReports = captor.getValue();
        assertEquals(2, savedReports.size(), "Only entry 1 (marks 85) and entry 2 (absent) should be saved; blank entry 3 should be skipped");

        Report absentReport = savedReports.stream().filter(r -> r.getStudent().getId().equals(20L)).findFirst().orElseThrow();
        assertEquals(0, absentReport.getMarks());
        assertEquals("ABSENT", absentReport.getRemarks());
    }
}
