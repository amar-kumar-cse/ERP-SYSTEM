package com.shiksha.erp.service;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.entity.TeacherBatch;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import com.shiksha.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeacherAccessHelper {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherBatchRepository teacherBatchRepository;

    public Teacher getTeacherFromPrincipal(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedAccessException("Unauthenticated request. Please log in.");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User account", "username", username));

        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + username));
    }

    public boolean isBatchOwnedByTeacher(Long batchId, Teacher teacher) {
        if (batchId == null || teacher == null) {
            return false;
        }
        return teacherBatchRepository.existsByTeacherIdAndClassBatchId(teacher.getId(), batchId);
    }

    public void validateTeacherBatchAccess(Long batchId, Teacher teacher) {
        if (!isBatchOwnedByTeacher(batchId, teacher)) {
            throw new UnauthorizedAccessException("Access Denied: You are not assigned to class batch ID: " + batchId);
        }
    }

    public List<ClassBatch> getTeacherBatches(Teacher teacher) {
        return teacherBatchRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(TeacherBatch::getClassBatch)
                .toList();
    }
}
