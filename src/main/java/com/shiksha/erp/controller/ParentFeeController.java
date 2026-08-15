package com.shiksha.erp.controller;

import com.shiksha.erp.dto.FeeResponseDto;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.service.FeeService;
import com.shiksha.erp.service.ParentStudentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student/fee")
@RequiredArgsConstructor
public class ParentFeeController {

    private final ParentStudentHelper parentStudentHelper;
    private final FeeService feeService;

    @GetMapping
    public String viewFees(Model model, Authentication auth) {
        Student student = parentStudentHelper.getStudentByParentUsername(auth.getName());

        Optional<FeeResponseDto> currentMonthFee = feeService.getCurrentMonthFee(student.getId());
        List<FeeResponseDto> feeHistory = feeService.getFeesByStudent(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("currentMonthFee", currentMonthFee.orElse(null));
        model.addAttribute("feeHistory", feeHistory);
        model.addAttribute("activePage", "fee");

        return "student/fee";
    }
}
