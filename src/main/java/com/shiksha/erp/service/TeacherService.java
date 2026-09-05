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
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.DuplicateRecordException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.HelpTicketRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.ResourceRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import com.shiksha.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final AttendanceRepository attendanceRepository;
    private final ReportRepository reportRepository;
    private final ResourceRepository resourceRepository;
    private final HelpTicketRepository helpTicketRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeacherResponseDto createTeacher(TeacherCreateDto dto) {
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new DuplicateRecordException("Teacher username already taken: " + dto.getUsername().trim());
        }

        if (userRepository.existsByEmail(dto.getEmail().trim())) {
            throw new DuplicateRecordException("Teacher email already registered: " + dto.getEmail().trim());
        }

        User user = User.builder()
                .username(dto.getUsername().trim())
                .password(passwordEncoder.encode(dto.getPassword().trim()))
                .email(dto.getEmail().trim())
                .role(Role.TEACHER)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

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
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));

        long attendanceCount = attendanceRepository.countByMarkedById(id);
        long reportCount = reportRepository.countByUploadedById(id);
        long resourceCount = resourceRepository.countByUploadedById(id);

        if (attendanceCount > 0 || reportCount > 0 || resourceCount > 0) {
            throw new BusinessValidationException(String.format(
                    "Cannot delete teacher '%s'. Existing linked records: %d attendance record(s), %d report card(s), %d study resource(s). Reassign or archive records first.",
                    teacher.getFullName(), attendanceCount, reportCount, resourceCount));
        }

        try {
            User user = teacher.getUser();
            teacherBatchRepository.deleteByTeacherId(id);
            teacherRepository.delete(teacher);

            if (user != null) {
                long ticketCount = helpTicketRepository.countByRaisedById(user.getId());
                if (ticketCount > 0) {
                    user.setEnabled(false);
                    userRepository.save(user);
                } else {
                    userRepository.delete(user);
                }
            }
            teacherRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessValidationException("Cannot delete teacher because linked historical records exist in the system.");
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
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
        return toResponseDto(teacher);
    }

    @Transactional
    public void assignToBatch(BatchAssignDto dto) {
        Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", dto.getTeacherId()));
        ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", dto.getClassBatchId()));

        if (teacherBatchRepository.existsByTeacherIdAndClassBatchId(dto.getTeacherId(), dto.getClassBatchId())) {
            throw new DuplicateRecordException("Teacher is already assigned to this batch");
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
