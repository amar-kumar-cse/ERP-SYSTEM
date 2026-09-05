package com.shiksha.erp.config;

import com.shiksha.erp.entity.Student;
import com.shiksha.erp.service.ParentStudentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice(basePackages = "com.shiksha.erp.controller")
@RequiredArgsConstructor
public class ParentGlobalModelAdvice {

    private final ParentStudentHelper parentStudentHelper;

    @ModelAttribute("parentStudents")
    public List<Student> populateParentStudents(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"))) {
            try {
                return parentStudentHelper.getStudentsForParent(auth.getName());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @ModelAttribute("activeStudent")
    public Student populateActiveStudent(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"))) {
            try {
                return parentStudentHelper.getStudentByParentUsername(auth.getName());
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
