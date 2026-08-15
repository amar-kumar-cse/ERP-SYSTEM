package com.shiksha.erp.service;

import com.shiksha.erp.dto.BulkAttendanceDto;
import com.shiksha.erp.dto.StudentAttendanceEntryDto;
import com.shiksha.erp.entity.Attendance;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.enums.AttendanceStatus;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.ClassBatchRepository;
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
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassBatchRepository classBatchRepository;

    @Mock
    private TeacherAccessHelper teacherAccessHelper;

    @InjectMocks
    private AttendanceService attendanceService;

    private Teacher teacher;
    private ClassBatch classBatch;
    private Student student;

    @BeforeEach
    void setUp() {
        teacher = Teacher.builder().id(1L).phone("9876543210").subject("Maths").build();
        classBatch = ClassBatch.builder().id(10L).batchName("Class 10").build();
        student = Student.builder().id(100L).name("Aarav").rollNo("SHK-001").classBatch(classBatch).build();
    }

    @Test
    @DisplayName("saveBulkAttendance: should save new attendance records when none exist for date")
    void testSaveBulkAttendance_NewRecords() {
        LocalDate date = LocalDate.now();

        StudentAttendanceEntryDto entry = StudentAttendanceEntryDto.builder()
                .studentId(100L)
                .status(AttendanceStatus.PRESENT)
                .build();

        BulkAttendanceDto dto = BulkAttendanceDto.builder()
                .classBatchId(10L)
                .date(date)
                .entries(List.of(entry))
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);
        when(classBatchRepository.findById(10L)).thenReturn(Optional.of(classBatch));
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndDate(100L, date)).thenReturn(Optional.empty());

        attendanceService.saveBulkAttendance(dto, teacher);

        verify(attendanceRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("saveBulkAttendance: should update existing attendance for same date without duplicate row")
    void testSaveBulkAttendance_UpdateExisting() {
        LocalDate date = LocalDate.now();

        Attendance existing = Attendance.builder()
                .id(50L)
                .student(student)
                .classBatch(classBatch)
                .date(date)
                .status(AttendanceStatus.ABSENT)
                .build();

        StudentAttendanceEntryDto entry = StudentAttendanceEntryDto.builder()
                .studentId(100L)
                .status(AttendanceStatus.PRESENT)
                .build();

        BulkAttendanceDto dto = BulkAttendanceDto.builder()
                .classBatchId(10L)
                .date(date)
                .entries(List.of(entry))
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);
        when(classBatchRepository.findById(10L)).thenReturn(Optional.of(classBatch));
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndDate(100L, date)).thenReturn(Optional.of(existing));

        attendanceService.saveBulkAttendance(dto, teacher);

        assertEquals(AttendanceStatus.PRESENT, existing.getStatus());
        verify(attendanceRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("saveBulkAttendance: future date should throw BusinessValidationException")
    void testSaveBulkAttendance_FutureDate_ThrowsException() {
        LocalDate futureDate = LocalDate.now().plusDays(2);

        BulkAttendanceDto dto = BulkAttendanceDto.builder()
                .classBatchId(10L)
                .date(futureDate)
                .entries(List.of())
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> attendanceService.saveBulkAttendance(dto, teacher));
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("saveBulkAttendance: unauthorized teacher batch should throw UnauthorizedAccessException")
    void testSaveBulkAttendance_UnauthorizedBatch_ThrowsException() {
        BulkAttendanceDto dto = BulkAttendanceDto.builder()
                .classBatchId(10L)
                .date(LocalDate.now())
                .entries(List.of())
                .build();

        when(teacherAccessHelper.isBatchOwnedByTeacher(10L, teacher)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () -> attendanceService.saveBulkAttendance(dto, teacher));
        verify(attendanceRepository, never()).saveAll(anyList());
    }
}
