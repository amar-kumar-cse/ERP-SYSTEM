package com.shiksha.erp.controller;

import com.shiksha.erp.dto.ReportRowDto;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.service.ParentStudentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping({"/student/reports", "/student/report-card"})
@RequiredArgsConstructor
public class ParentReportController {

    private final ParentStudentHelper parentStudentHelper;
    private final ReportRepository reportRepository;

    @GetMapping
    public String viewReports(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate examDate,
            Model model,
            Authentication auth
    ) {
        Student student = parentStudentHelper.getStudentByParentUsername(auth.getName());

        List<Report> allStudentReports = reportRepository.findByStudentIdOrderByExamDateDesc(student.getId());

        List<ReportRowDto> filteredReports = allStudentReports.stream()
                .filter(r -> subject == null || subject.isBlank() || r.getSubject().equalsIgnoreCase(subject.trim()))
                .filter(r -> examDate == null || r.getExamDate().equals(examDate))
                .map(r -> {
                    double pct = r.getMaxMarks() > 0
                            ? Math.round(((double) r.getMarks() / r.getMaxMarks() * 100.0) * 100.0) / 100.0
                            : 0.0;

                    return ReportRowDto.builder()
                            .id(r.getId())
                            .subject(r.getSubject())
                            .examDate(r.getExamDate())
                            .marks(r.getMarks())
                            .maxMarks(r.getMaxMarks())
                            .percentage(pct)
                            .remarks(r.getRemarks())
                            .uploadedByName(r.getUploadedBy() != null ? r.getUploadedBy().getFullName() : "Faculty")
                            .build();
                })
                .toList();

        List<String> subjects = reportRepository.findDistinctSubjectsByStudentId(student.getId());

        double averagePercentage = filteredReports.stream()
                .mapToDouble(ReportRowDto::getPercentage)
                .average()
                .orElse(0.0);
        averagePercentage = Math.round(averagePercentage * 10.0) / 10.0;

        model.addAttribute("student", student);
        model.addAttribute("reports", filteredReports);
        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedExamDate", examDate);
        model.addAttribute("averagePercentage", averagePercentage);
        model.addAttribute("activePage", "reports");

        return "student/reports";
    }
}
