package com.shiksha.erp.controller;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.repository.AttendanceRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.service.TeacherAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherAccessHelper teacherAccessHelper;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> myBatches = teacherAccessHelper.getTeacherBatches(teacher);

        // check which batches have NOT had attendance marked for today
        LocalDate today = LocalDate.now();
        List<ClassBatch> pendingBatches = myBatches.stream()
                .filter(b -> !attendanceRepository.existsByClassBatchIdAndDate(b.getId(), today))
                .toList();

        // unique students across all teacher's batches
        Set<Long> uniqueStudentIds = myBatches.stream()
                .flatMap(b -> studentRepository.findByClassBatchId(b.getId()).stream())
                .map(Student::getId)
                .collect(Collectors.toSet());

        model.addAttribute("teacher", teacher);
        model.addAttribute("myClassesCount", myBatches.size());
        model.addAttribute("pendingBatches", pendingBatches);
        model.addAttribute("pendingAttendanceCount", pendingBatches.size());
        model.addAttribute("totalStudentsCount", uniqueStudentIds.size());
        model.addAttribute("myBatches", myBatches);
        model.addAttribute("todayDate", today);
        model.addAttribute("activePage", "dashboard");

        return "teacher/dashboard";
    }
}
