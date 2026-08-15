package com.shiksha.erp.service;

import com.shiksha.erp.dto.BatchAssignDto;
import com.shiksha.erp.dto.TeacherCreateDto;
import com.shiksha.erp.dto.TeacherResponseDto;
import com.shiksha.erp.dto.TeacherUpdateDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.entity.TeacherBatch;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.Role;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import com.shiksha.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final TeacherBatchRepository teacherBatchRepository;
    private final ClassBatchRepository classBatchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeacherResponseDto createTeacher(TeacherCreateDto dto) {
        // username aur email duplicate check
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new RuntimeException("Username already taken");
        }

        if (userRepository.existsByEmail(dto.getEmail().trim())) {
            throw new RuntimeException("Email already registered");
        }

        // 1. User create karo (Role TEACHER)
        User user = User.builder()
                .username(dto.getUsername().trim())
                .password(passwordEncoder.encode(dto.getPassword().trim()))
                .email(dto.getEmail().trim())
                .role(Role.TEACHER)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        // 2. Teacher entity create karo
        Teacher teacher = Teacher.builder()
                .user(savedUser)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .phone(dto.getPhone().trim())
                .subject(dto.getSubject().trim())
                .joiningDate(dto.getJoiningDate())
                .build();
        Teacher savedTeacher = teacherRepository.save(teacher);

        return toResponseDto(savedTeacher);
    }

    @Transactional
    public TeacherResponseDto updateTeacher(Long id, TeacherUpdateDto dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id));

        teacher.setFirstName(dto.getFirstName().trim());
        teacher.setLastName(dto.getLastName().trim());
        teacher.setPhone(dto.getPhone().trim());
        teacher.setSubject(dto.getSubject().trim());
        teacher.setJoiningDate(dto.getJoiningDate());

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return toResponseDto(updatedTeacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id));

        User user = teacher.getUser();

        // pehle teacher ke batch mappings delete karo
        teacherBatchRepository.deleteByTeacherId(id);

        // phir teacher entity delete karo
        teacherRepository.delete(teacher);

        // aakhri mein user account delete karo
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponseDto> findAll(Pageable pageable, String search) {
        Page<Teacher> teacherPage;
        if (search != null && !search.trim().isEmpty()) {
            teacherPage = teacherRepository.searchTeachers(search.trim(), pageable);
        } else {
            teacherPage = teacherRepository.findAll(pageable);
        }
        return teacherPage.map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public TeacherResponseDto findById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id));
        return toResponseDto(teacher);
    }

    @Transactional
    public void assignToBatch(BatchAssignDto dto) {
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                .orElseThrow(() -> new RuntimeException("Class batch not found"));

        // check karo teacher already is batch mein assigned hai ya nahi
        if (teacherBatchRepository.existsByTeacherIdAndClassBatchId(dto.getTeacherId(), dto.getClassBatchId())) {
            throw new RuntimeException("Teacher is already assigned to this batch");
        }

        TeacherBatch teacherBatch = TeacherBatch.builder()
                .teacher(teacher)
                .classBatch(classBatch)
                .build();
        teacherBatchRepository.save(teacherBatch);
    }

    @Transactional
    public void removeFromBatch(Long teacherId, Long batchId) {
        teacherBatchRepository.deleteByTeacherIdAndClassBatchId(teacherId, batchId);
    }

    public TeacherResponseDto toResponseDto(Teacher teacher) {
        List<String> assignedBatches = teacherBatchRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(tb -> tb.getClassBatch().getBatchName())
                .toList();

        return TeacherResponseDto.builder()
                .id(teacher.getId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .fullName(teacher.getFullName())
                .phone(teacher.getPhone())
                .subject(teacher.getSubject())
                .joiningDate(teacher.getJoiningDate())
                .username(teacher.getUser() != null ? teacher.getUser().getUsername() : null)
                .email(teacher.getUser() != null ? teacher.getUser().getEmail() : null)
                .assignedBatches(assignedBatches)
                .createdAt(teacher.getCreatedAt())
                .build();
    }
}
