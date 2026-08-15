package com.shiksha.erp.service;

import com.shiksha.erp.dto.ClassBatchCreateDto;
import com.shiksha.erp.dto.ClassBatchResponseDto;
import com.shiksha.erp.dto.TeacherResponseDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
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
        // batch nahi mili toh error throw karo
        ClassBatch batch = classBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class batch not found with id: " + id));

        batch.setBatchName(dto.getBatchName().trim());
        batch.setTiming(dto.getTiming().trim());
        batch.setDays(dto.getDays().trim());

        ClassBatch updated = classBatchRepository.save(batch);
        return toResponseDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!classBatchRepository.existsById(id)) {
            throw new RuntimeException("Class batch not found with id: " + id);
        }

        // agar students assigned hain toh delete mat hone do
        long studentCount = studentRepository.countByClassBatchId(id);
        if (studentCount > 0) {
            throw new RuntimeException("Cannot delete batch: " + studentCount + " student(s) are currently enrolled.");
        }

        // teachers ki assignments saaf karo
        teacherBatchRepository.deleteByClassBatchId(id);

        // batch delete karo
        classBatchRepository.deleteById(id);
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
                .orElseThrow(() -> new RuntimeException("Class batch not found with id: " + id));
        return toResponseDto(batch);
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAssignableTeachers(Long batchId) {
        // jo teachers already is batch mein hain unhe exclude karo
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
