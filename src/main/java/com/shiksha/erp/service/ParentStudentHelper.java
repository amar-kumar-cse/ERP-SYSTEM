package com.shiksha.erp.service;

import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ParentStudentHelper {

    public static final String ACTIVE_STUDENT_SESSION_KEY = "activeStudentId";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public User getParentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new UnauthorizedAccessException("Unauthenticated parent session");
        }
        return userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Parent user account", "username", username));
    }

    public List<Student> getStudentsForParent(String username) {
        User parent = getParentUser(username);
        List<Student> students = new ArrayList<>(studentRepository.findByParentUserId(parent.getId()));
        if (students.isEmpty()) {
            studentRepository.findFirstByParentUserId(parent.getId()).ifPresent(students::add);
        }
        return students;
    }

    public Student getStudentByParentUsername(String username) {
        List<Student> students = getStudentsForParent(username);
        if (students.isEmpty()) {
            throw new ResourceNotFoundException("No student linked to account: " + username);
        }

        // Check if an active student ID is in session
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                Long activeStudentId = (Long) session.getAttribute(ACTIVE_STUDENT_SESSION_KEY);
                if (activeStudentId != null) {
                    for (Student s : students) {
                        if (s.getId().equals(activeStudentId)) {
                            return s;
                        }
                    }
                }
            }
        }

        Student defaultStudent = students.get(0);
        if (attrs != null) {
            HttpSession session = attrs.getRequest().getSession(true);
            session.setAttribute(ACTIVE_STUDENT_SESSION_KEY, defaultStudent.getId());
        }
        return defaultStudent;
    }

    public Student getStudentForParent(String username) {
        return getStudentByParentUsername(username);
    }

    public void setActiveStudent(String username, Long studentId, HttpSession session) {
        validateParentAccess(username, studentId);
        if (session != null) {
            session.setAttribute(ACTIVE_STUDENT_SESSION_KEY, studentId);
        }
    }

    public void validateParentAccess(String username, Long studentId) {
        List<Student> students = getStudentsForParent(username);
        boolean belongsToParent = students.stream().anyMatch(s -> s.getId().equals(studentId));
        if (!belongsToParent) {
            throw new UnauthorizedAccessException("Access Denied: You cannot view or modify data for another parent's student.");
        }
    }
}
