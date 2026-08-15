package com.shiksha.erp.controller;

import com.shiksha.erp.dto.ClassBatchResponseDto;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.TeacherRepository;
import com.shiksha.erp.service.ClassBatchService;
import com.shiksha.erp.service.FeeService;
import com.shiksha.erp.service.HelpTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassBatchRepository classBatchRepository;
    private final ClassBatchService classBatchService;
    private final FeeService feeService;
    private final HelpTicketService helpTicketService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // live counts from database
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalClasses = classBatchRepository.count();
        long feeOverdueCount = feeService.getOverdueCount();
        long openTicketsCount = helpTicketService.getOpenCount();

        List<ClassBatchResponseDto> runningBatches = classBatchService.findAll();

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalTeachers", totalTeachers);
        model.addAttribute("totalClasses", totalClasses);
        model.addAttribute("feeOverdueCount", feeOverdueCount);
        model.addAttribute("openTicketsCount", openTicketsCount);
        model.addAttribute("runningBatches", runningBatches);
        model.addAttribute("activePage", "dashboard");

        return "admin/dashboard";
    }
}
