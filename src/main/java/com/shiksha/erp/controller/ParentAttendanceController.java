package com.shiksha.erp.controller;

import com.shiksha.erp.dto.AttendanceSummaryDto;
import com.shiksha.erp.entity.Attendance;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.enums.AttendanceStatus;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.service.ParentStudentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/student/attendance")
@RequiredArgsConstructor
public class ParentAttendanceController {

    private final ParentStudentHelper parentStudentHelper;
    private final AttendanceRepository attendanceRepository;

    @GetMapping
    public String viewAttendance(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model,
            Authentication auth
    ) {
        Student student = parentStudentHelper.getStudentByParentUsername(auth.getName());

        LocalDate now = LocalDate.now();
        int selectedMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();
        int selectedYear = (year != null && year >= 2020 && year <= 2030) ? year : now.getYear();

        YearMonth ym = YearMonth.of(selectedYear, selectedMonth);
        LocalDate fromDate = ym.atDay(1);
        LocalDate toDate = ym.atEndOfMonth();

        List<Attendance> monthlyAttendances = attendanceRepository.findByStudentIdAndDateBetween(student.getId(), fromDate, toDate)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();

        long presentCount = monthlyAttendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absentCount = monthlyAttendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long totalClasses = presentCount + absentCount;

        double percentage = totalClasses > 0
                ? Math.round(((double) presentCount / totalClasses * 100.0) * 100.0) / 100.0
                : 0.0;

        AttendanceSummaryDto summary = AttendanceSummaryDto.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .rollNo(student.getRollNo())
                .totalClasses(totalClasses)
                .present(presentCount)
                .absent(absentCount)
                .percentage(percentage)
                .build();

        // Prev & Next Month navigation
        YearMonth prevYm = ym.minusMonths(1);
        YearMonth nextYm = ym.plusMonths(1);

        model.addAttribute("student", student);
        model.addAttribute("summary", summary);
        model.addAttribute("attendances", monthlyAttendances);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("yearMonthStr", ym.getMonth().name() + " " + selectedYear);
        model.addAttribute("prevMonth", prevYm.getMonthValue());
        model.addAttribute("prevYear", prevYm.getYear());
        model.addAttribute("nextMonth", nextYm.getMonthValue());
        model.addAttribute("nextYear", nextYm.getYear());
        model.addAttribute("isFutureMonth", ym.isAfter(YearMonth.now()));
        model.addAttribute("activePage", "attendance");

        return "student/attendance";
    }
}
