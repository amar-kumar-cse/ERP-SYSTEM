package com.shiksha.erp.controller;

import com.shiksha.erp.service.ParentStudentHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class ParentSwitchController {

    private final ParentStudentHelper parentStudentHelper;

    @PostMapping("/switch/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public String switchStudent(
            @PathVariable Long studentId,
            Authentication auth,
            HttpSession session,
            HttpServletRequest request
    ) {
        log.info("Parent '{}' requested switch to student ID: {}", auth.getName(), studentId);
        parentStudentHelper.setActiveStudent(auth.getName(), studentId, session);

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/switch/")) {
            return "redirect:" + referer;
        }
        return "redirect:/student/dashboard";
    }

    @GetMapping("/switch/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public String switchStudentGet(
            @PathVariable Long studentId,
            Authentication auth,
            HttpSession session,
            HttpServletRequest request
    ) {
        return switchStudent(studentId, auth, session, request);
    }
}
