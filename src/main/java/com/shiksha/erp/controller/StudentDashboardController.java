package com.shiksha.erp.controller;

import com.shiksha.erp.dto.FeeResponseDto;
import com.shiksha.erp.dto.ReportRowDto;
import com.shiksha.erp.entity.Attendance;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.enums.AttendanceStatus;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.service.FeeService;
import com.shiksha.erp.service.ParentStudentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final ParentStudentHelper parentStudentHelper;
    private final AttendanceRepository attendanceRepository;
    private final ReportRepository reportRepository;
    private final FeeService feeService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        Student student = parentStudentHelper.getStudentByParentUsername(auth.getName());

        // 1. Last 30 days attendance % calculation
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        long presentCount = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(student.getId(), thirtyDaysAgo, today, AttendanceStatus.PRESENT);
        long absentCount = attendanceRepository.countByStudentIdAndDateBetweenAndStatus(student.getId(), thirtyDaysAgo, today, AttendanceStatus.ABSENT);
        long totalClasses = presentCount + absentCount;

        double attendancePercentage = totalClasses > 0
                ? Math.round(((double) presentCount / totalClasses * 100.0) * 10.0) / 10.0
                : 100.0;

        // 2. Latest 3 test reports
        List<Report> topReports = reportRepository.findTop3ByStudentIdOrderByExamDateDesc(student.getId());
        List<ReportRowDto> recentReports = topReports.stream().map(r -> {
            double pct = r.getMaxMarks() > 0 ? Math.round(((double) r.getMarks() / r.getMaxMarks() * 100.0) * 10.0) / 10.0 : 0.0;
            return ReportRowDto.builder()
                    .id(r.getId())
                    .subject(r.getSubject())
                    .examDate(r.getExamDate())
                    .marks(r.getMarks())
                    .maxMarks(r.getMaxMarks())
                    .percentage(pct)
                    .remarks(r.getRemarks())
                    .build();
        }).toList();

        // 3. Last 5 attendance records for recent log table
        List<Attendance> recentAttendances = attendanceRepository.findByStudentIdAndDateBetween(student.getId(), thirtyDaysAgo, today)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(5)
                .toList();

        // 4. Current Month Fee Status
        Optional<FeeResponseDto> currentMonthFee = feeService.getCurrentMonthFee(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("studentName", student.getName());
        model.addAttribute("rollNo", student.getRollNo());
        model.addAttribute("batchName", student.getClassBatch() != null ? student.getClassBatch().getBatchName() : "Not Assigned");
        model.addAttribute("attendancePercentage", attendancePercentage);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("totalClassesHeld", totalClasses);
        model.addAttribute("recentReports", recentReports);
        model.addAttribute("recentAttendances", recentAttendances);
        model.addAttribute("currentMonthFee", currentMonthFee.orElse(null));
        model.addAttribute("activePage", "dashboard");

        return "student/dashboard";
    }
}
