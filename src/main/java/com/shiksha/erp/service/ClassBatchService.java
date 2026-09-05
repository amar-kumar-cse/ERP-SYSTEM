package com.shiksha.erp.service;

import com.shiksha.erp.dto.ClassBatchCreateDto;
import com.shiksha.erp.dto.ClassBatchResponseDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.ResourceRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassBatchService {

    private final ClassBatchRepository classBatchRepository;
    private final StudentRepository studentRepository;
    private final TeacherBatchRepository teacherBatchRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReportRepository reportRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public ClassBatchResponseDto create(ClassBatchCreateDto dto) {
        ClassBatch batch = ClassBatch.builder()
                .batchName(dto.getBatchName().trim())
                .timing(dto.getTiming().trim())
                .days(dto.getDays().trim())
                .build();
        ClassBatch saved = classBatchRepository.save(batch);
        return toResponseDto(saved);
    }

    @Transactional
    public ClassBatchResponseDto update(Long id, ClassBatchCreateDto dto) {
        ClassBatch batch = classBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", id));

        batch.setBatchName(dto.getBatchName().trim());
        batch.setTiming(dto.getTiming().trim());
        batch.setDays(dto.getDays().trim());

        ClassBatch updated = classBatchRepository.save(batch);
        return toResponseDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        ClassBatch batch = classBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", id));

        long studentCount = studentRepository.countByClassBatchId(id);
        long attendanceCount = attendanceRepository.countByClassBatchId(id);
        long reportCount = reportRepository.countByClassBatchId(id);
        long resourceCount = resourceRepository.countByClassBatchId(id);

        if (studentCount > 0 || attendanceCount > 0 || reportCount > 0 || resourceCount > 0) {
            throw new BusinessValidationException(String.format(
                    "Cannot delete batch '%s'. Existing linked records: %d student(s), %d attendance record(s), %d report card(s), %d study resource(s). Reassign or remove these records first.",
                    batch.getBatchName(), studentCount, attendanceCount, reportCount, resourceCount));
        }

        try {
            teacherBatchRepository.deleteByClassBatchId(id);
            classBatchRepository.delete(batch);
            classBatchRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessValidationException("Cannot delete batch because linked historical records exist in the system.");
        }
    }

    @Transactional(readOnly = true)
    public List<ClassBatchResponseDto> findAll() {
        return classBatchRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassBatchResponseDto findById(Long id) {
        ClassBatch batch = classBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", id));
        return toResponseDto(batch);
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAssignableTeachers(Long batchId) {
        Set<Long> assignedTeacherIds = teacherBatchRepository.findByClassBatchId(batchId).stream()
                .map(tb -> tb.getTeacher().getId())
                .collect(Collectors.toSet());

        return teacherRepository.findAll().stream()
                .filter(t -> !assignedTeacherIds.contains(t.getId()))
                .toList();
    }

    public ClassBatchResponseDto toResponseDto(ClassBatch batch) {
        int studentCount = (int) studentRepository.countByClassBatchId(batch.getId());
        List<String> teachers = teacherBatchRepository.findByClassBatchId(batch.getId()).stream()
                .map(tb -> tb.getTeacher().getFullName())
                .toList();

        return ClassBatchResponseDto.builder()
                .id(batch.getId())
                .batchName(batch.getBatchName())
                .timing(batch.getTiming())
                .days(batch.getDays())
                .studentCount(studentCount)
                .assignedTeachers(teachers)
                .createdAt(batch.getCreatedAt())
                .build();
    }
}
