package com.shiksha.erp.service;

import com.shiksha.erp.dto.StudentCreateDto;
import com.shiksha.erp.dto.StudentResponseDto;
import com.shiksha.erp.dto.StudentUpdateDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.Role;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ClassBatchRepository classBatchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponseDto createStudent(StudentCreateDto dto) {
        // rollNo duplicate check — roll number unique hona zaroori hai
        if (studentRepository.existsByRollNo(dto.getRollNo().trim())) {
            throw new RuntimeException("Roll no. already exists");
        }

        // parent username unique check
        if (userRepository.existsByUsername(dto.getParentUsername().trim())) {
            throw new RuntimeException("Username already taken");
        }

        // parent email unique check agar email provide kiya gaya ho
        if (dto.getParentEmail() != null && !dto.getParentEmail().isBlank()) {
            if (userRepository.existsByEmail(dto.getParentEmail().trim())) {
                throw new RuntimeException("Email already taken");
            }
        }

        // Parent ka User account create karo (PARENT role)
        User parentUser = User.builder()
                .username(dto.getParentUsername().trim())
                .password(passwordEncoder.encode(dto.getParentPassword().trim()))
                .email(dto.getParentEmail() != null && !dto.getParentEmail().isBlank() ? dto.getParentEmail().trim() : null)
                .role(Role.PARENT)
                .enabled(true)
                .build();
        User savedParentUser = userRepository.save(parentUser);

        // agar classBatchId diya hai toh batch fetch karo
        ClassBatch classBatch = null;
        if (dto.getClassBatchId() != null) {
            classBatch = classBatchRepository.findById(dto.getClassBatchId())
                    .orElseThrow(() -> new RuntimeException("Selected class batch not found"));
        }

        // Student entity save karo
        Student student = Student.builder()
                .name(dto.getName().trim())
                .rollNo(dto.getRollNo().trim())
                .parentName(dto.getParentName() != null ? dto.getParentName().trim() : null)
                .parentPhone(dto.getParentPhone() != null ? dto.getParentPhone().trim() : null)
                .parentUser(savedParentUser)
                .classBatch(classBatch)
                .build();

        Student savedStudent = studentRepository.save(student);
        return toResponseDto(savedStudent);
    }

    @Transactional
    public StudentResponseDto updateStudent(Long id, StudentUpdateDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        student.setName(dto.getName().trim());
        student.setParentName(dto.getParentName() != null ? dto.getParentName().trim() : null);
        student.setParentPhone(dto.getParentPhone() != null ? dto.getParentPhone().trim() : null);

        // batch change ya unassign handle karo
        if (dto.getClassBatchId() != null) {
            ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                    .orElseThrow(() -> new RuntimeException("Selected class batch not found"));
            student.setClassBatch(classBatch);
        } else {
            student.setClassBatch(null);
        }

        Student updatedStudent = studentRepository.save(student);
        return toResponseDto(updatedStudent);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        User parentUser = student.getParentUser();

        // student delete karo
        studentRepository.delete(student);

        // agar is parent ka koi aur bacha nahi hai toh parent ka login account bhi delete karo
        if (parentUser != null && studentRepository.countByParentUserId(parentUser.getId()) == 0) {
            userRepository.delete(parentUser);
        }
    }

    @Transactional(readOnly = true)
    public Page<StudentResponseDto> findAll(Pageable pageable, String search) {
        Page<Student> studentPage;
        if (search != null && !search.trim().isEmpty()) {
            studentPage = studentRepository.searchStudents(search.trim(), pageable);
        } else {
            studentPage = studentRepository.findAll(pageable);
        }
        return studentPage.map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public StudentResponseDto findById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        return toResponseDto(student);
    }

    public StudentResponseDto toResponseDto(Student student) {
        return StudentResponseDto.builder()
                .id(student.getId())
                .name(student.getName())
                .rollNo(student.getRollNo())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .parentUsername(student.getParentUser() != null ? student.getParentUser().getUsername() : null)
                .parentEmail(student.getParentUser() != null ? student.getParentUser().getEmail() : null)
                .classBatchId(student.getClassBatch() != null ? student.getClassBatch().getId() : null)
                .batchName(student.getClassBatch() != null ? student.getClassBatch().getBatchName() : null)
                .createdAt(student.getCreatedAt())
                .build();
    }
}
