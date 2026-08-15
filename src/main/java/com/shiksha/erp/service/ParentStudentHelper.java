package com.shiksha.erp.service;

import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParentStudentHelper {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public Student getStudentByParentUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Unauthenticated parent session");
        }

        User parentUser = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("Parent user account not found: " + username));

        return studentRepository.findFirstByParentUserId(parentUser.getId())
                .orElseThrow(() -> new RuntimeException("No student linked to this account (" + username + ")"));
    }

    public Student getStudentForParent(String username) {
        return getStudentByParentUsername(username);
    }

    public void validateParentAccess(String username, Long studentId) {
        Student student = getStudentByParentUsername(username);
        if (!student.getId().equals(studentId)) {
            throw new RuntimeException("Access Denied: You cannot view or modify data for another parent's student.");
        }
    }
}
