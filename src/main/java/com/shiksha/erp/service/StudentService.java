package com.shiksha.erp.service;

import com.shiksha.erp.dto.StudentCreateDto;
import com.shiksha.erp.dto.StudentResponseDto;
import com.shiksha.erp.dto.StudentUpdateDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.Role;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.DuplicateRecordException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final AttendanceRepository attendanceRepository;
    private final FeeRepository feeRepository;
    private final ReportRepository reportRepository;
    private final HelpTicketRepository helpTicketRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponseDto createStudent(StudentCreateDto dto) {
        if (dto.getRollNo() == null || dto.getRollNo().isBlank()) {
            throw new IllegalArgumentException("Student roll number is required");
        }

        if (studentRepository.existsByRollNo(dto.getRollNo().trim())) {
            throw new DuplicateRecordException("Roll number already exists: " + dto.getRollNo().trim());
        }

        if (userRepository.existsByUsername(dto.getParentUsername().trim())) {
            throw new DuplicateRecordException("Parent login username is already taken: " + dto.getParentUsername().trim());
        }

        if (dto.getParentEmail() != null && !dto.getParentEmail().isBlank()) {
            if (userRepository.existsByEmail(dto.getParentEmail().trim())) {
                throw new DuplicateRecordException("Parent email is already registered: " + dto.getParentEmail().trim());
            }
        }

        User parentUser = User.builder()
                .username(dto.getParentUsername().trim())
                .password(passwordEncoder.encode(dto.getParentPassword().trim()))
                .email(dto.getParentEmail() != null && !dto.getParentEmail().isBlank() ? dto.getParentEmail().trim() : null)
                .role(Role.PARENT)
                .enabled(true)
                .build();
        User savedParentUser = userRepository.save(parentUser);

        ClassBatch classBatch = null;
        if (dto.getClassBatchId() != null) {
            classBatch = classBatchRepository.findById(dto.getClassBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", dto.getClassBatchId()));
        }

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
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        student.setName(dto.getName().trim());
        student.setParentName(dto.getParentName() != null ? dto.getParentName().trim() : null);
        student.setParentPhone(dto.getParentPhone() != null ? dto.getParentPhone().trim() : null);

        if (dto.getClassBatchId() != null) {
            ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", dto.getClassBatchId()));
            student.setClassBatch(classBatch);
        } else {
            student.setClassBatch(null);
        }

        if (student.getParentUser() != null && dto.getParentEmail() != null) {
            String newEmail = dto.getParentEmail().trim().isEmpty() ? null : dto.getParentEmail().trim();
            if (newEmail != null && !newEmail.equalsIgnoreCase(student.getParentUser().getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new DuplicateRecordException("Email is already registered to another user: " + newEmail);
                }
            }
            student.getParentUser().setEmail(newEmail);
            userRepository.save(student.getParentUser());
        }

        Student updatedStudent = studentRepository.save(student);
        return toResponseDto(updatedStudent);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        long attendanceCount = attendanceRepository.countByStudentId(id);
        long feeCount = feeRepository.countByStudentId(id);
        long reportCount = reportRepository.countByStudentId(id);

        if (attendanceCount > 0 || feeCount > 0 || reportCount > 0) {
            throw new BusinessValidationException(String.format(
                    "Cannot delete student '%s' (%s): Student has %d attendance record(s), %d fee invoice(s), and %d test report(s). Historical records must be removed or archived first.",
                    student.getName(), student.getRollNo(), attendanceCount, feeCount, reportCount
            ));
        }

        try {
            User parentUser = student.getParentUser();
            studentRepository.delete(student);

            if (parentUser != null && studentRepository.countByParentUserId(parentUser.getId()) == 0) {
                long ticketsCount = helpTicketRepository.countByRaisedById(parentUser.getId());
                if (ticketsCount > 0) {
                    // Parent raised tickets, disable user account instead of hard-deleting to avoid FK violation
                    parentUser.setEnabled(false);
                    userRepository.save(parentUser);
                } else {
                    userRepository.delete(parentUser);
                }
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessValidationException("Cannot delete student: Dependent records exist preventing deletion. Please review related data.");
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
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
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
