package com.shiksha.erp.service;

import com.shiksha.erp.dto.AttendanceRowDto;
import com.shiksha.erp.dto.AttendanceSummaryDto;
import com.shiksha.erp.dto.BulkAttendanceDto;
import com.shiksha.erp.dto.StudentAttendanceEntryDto;
import com.shiksha.erp.entity.Attendance;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.enums.AttendanceStatus;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassBatchRepository classBatchRepository;
    private final TeacherAccessHelper teacherAccessHelper;

    @Transactional
    public void saveBulkAttendance(BulkAttendanceDto dto, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(dto.getClassBatchId(), teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + dto.getClassBatchId());
        }

        if (dto.getDate() == null) {
            throw new BusinessValidationException("Attendance date is required");
        }

        if (dto.getDate().isAfter(LocalDate.now())) {
            throw new BusinessValidationException("Attendance cannot be recorded for future dates");
        }

        ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", dto.getClassBatchId()));

        if (dto.getEntries() == null || dto.getEntries().isEmpty()) {
            return;
        }

        List<Attendance> attendancesToSave = new ArrayList<>();

        for (StudentAttendanceEntryDto entry : dto.getEntries()) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", entry.getStudentId()));

            AttendanceStatus status = entry.getStatus() != null ? entry.getStatus() : AttendanceStatus.PRESENT;

            Optional<Attendance> existing = attendanceRepository.findByStudentIdAndDate(student.getId(), dto.getDate());

            if (existing.isPresent()) {
                Attendance att = existing.get();
                att.setStatus(status);
                att.setMarkedBy(teacher);
                att.setClassBatch(classBatch);
                attendancesToSave.add(att);
            } else {
                Attendance newAtt = Attendance.builder()
                        .student(student)
                        .classBatch(classBatch)
                        .date(dto.getDate())
                        .status(status)
                        .markedBy(teacher)
                        .build();
                attendancesToSave.add(newAtt);
            }
        }

        attendanceRepository.saveAll(attendancesToSave);
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceEntryDto> getAttendanceFormData(Long batchId, LocalDate date, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + batchId);
        }

        List<Student> students = studentRepository.findByClassBatchIdOrderByNameAsc(batchId);
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, AttendanceStatus> existingMap = attendanceRepository.findByClassBatchIdAndDate(batchId, date)
                .stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Attendance::getStatus, (a, b) -> b));

        return students.stream().map(s -> {
            AttendanceStatus status = existingMap.getOrDefault(s.getId(), AttendanceStatus.PRESENT);
            return StudentAttendanceEntryDto.builder()
                    .studentId(s.getId())
                    .studentName(s.getName())
                    .rollNo(s.getRollNo())
                    .status(status)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryDto> getBatchAttendanceSummary(Long batchId, LocalDate from, LocalDate to, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + batchId);
        }

        List<Student> students = studentRepository.findByClassBatchIdOrderByNameAsc(batchId);
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        return students.stream().map(student -> {
            long present = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(student.getId(), from, to, AttendanceStatus.PRESENT);
            long absent = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(student.getId(), from, to, AttendanceStatus.ABSENT);
            long total = present + absent;

            double percentage = total > 0
                    ? Math.round(((double) present / total * 100.0) * 100.0) / 100.0
                    : 0.0;

            return AttendanceSummaryDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .rollNo(student.getRollNo())
                    .totalClasses(total)
                    .present(present)
                    .absent(absent)
                    .percentage(percentage)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRowDto> getAttendanceByBatchAndDate(Long batchId, LocalDate date, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + batchId);
        }

        return attendanceRepository.findByClassBatchIdAndDate(batchId, date).stream()
                .map(a -> AttendanceRowDto.builder()
                        .id(a.getId())
                        .studentName(a.getStudent().getName())
                        .rollNo(a.getStudent().getRollNo())
                        .date(a.getDate())
                        .status(a.getStatus())
                        .markedByName(a.getMarkedBy().getFullName())
                        .build())
                .toList();
    }
}
