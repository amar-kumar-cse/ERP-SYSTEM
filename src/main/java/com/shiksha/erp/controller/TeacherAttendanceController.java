package com.shiksha.erp.controller;

import com.shiksha.erp.dto.AttendanceSummaryDto;
import com.shiksha.erp.dto.BulkAttendanceDto;
import com.shiksha.erp.dto.StudentAttendanceEntryDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.service.AttendanceService;
import com.shiksha.erp.service.TeacherAccessHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/teacher/attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAccessHelper teacherAccessHelper;
    private final AttendanceService attendanceService;
    private final ClassBatchRepository classBatchRepository;

    @GetMapping
    public String selectBatchForm(Model model, Authentication auth) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> batches = teacherAccessHelper.getTeacherBatches(teacher);

        model.addAttribute("batches", batches);
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("activePage", "attendance");

        return "teacher/attendance/select";
    }

    @GetMapping("/mark")
    public String markAttendanceForm(
            @RequestParam Long batchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unauthorized: You do not have access to this class batch");
            return "redirect:/teacher/attendance";
        }

        LocalDate attendanceDate = (date != null) ? date : LocalDate.now();

        if (attendanceDate.isAfter(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Attendance cannot be recorded for future dates");
            return "redirect:/teacher/attendance";
        }

        ClassBatch batch = classBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Class batch not found: " + batchId));

        List<StudentAttendanceEntryDto> entries = attendanceService.getAttendanceFormData(batchId, attendanceDate, teacher);

        BulkAttendanceDto bulkDto = BulkAttendanceDto.builder()
                .classBatchId(batchId)
                .batchName(batch.getBatchName())
                .date(attendanceDate)
                .entries(entries)
                .build();

        model.addAttribute("bulkAttendanceDto", bulkDto);
        model.addAttribute("batch", batch);
        model.addAttribute("date", attendanceDate);
        model.addAttribute("studentCount", entries.size());
        model.addAttribute("activePage", "attendance");

        return "teacher/attendance/mark";
    }

    @PostMapping("/mark")
    public String saveAttendance(
            @Valid @ModelAttribute("bulkAttendanceDto") BulkAttendanceDto dto,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Invalid attendance form submission");
            return "redirect:/teacher/attendance/mark?batchId=" + dto.getClassBatchId() + "&date=" + dto.getDate();
        }

        try {
            attendanceService.saveBulkAttendance(dto, teacher);
            redirectAttributes.addFlashAttribute("successMsg", "Attendance successfully saved for " + dto.getDate() + "!");
            return "redirect:/teacher/attendance/history?batchId=" + dto.getClassBatchId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
            return "redirect:/teacher/attendance/mark?batchId=" + dto.getClassBatchId() + "&date=" + dto.getDate();
        }
    }

    @GetMapping("/history")
    public String attendanceHistory(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> batches = teacherAccessHelper.getTeacherBatches(teacher);

        Long selectedBatchId = batchId;
        if (selectedBatchId == null && !batches.isEmpty()) {
            selectedBatchId = batches.get(0).getId();
        }

        LocalDate fromDate = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = (to != null) ? to : LocalDate.now();

        List<AttendanceSummaryDto> summaries = List.of();
        ClassBatch selectedBatch = null;

        if (selectedBatchId != null) {
            if (!teacherAccessHelper.isBatchOwnedByTeacher(selectedBatchId, teacher)) {
                redirectAttributes.addFlashAttribute("errorMsg", "Unauthorized: You do not have access to this batch");
                return "redirect:/teacher/attendance";
            }
            summaries = attendanceService.getBatchAttendanceSummary(selectedBatchId, fromDate, toDate, teacher);
            selectedBatch = classBatchRepository.findById(selectedBatchId).orElse(null);
        }

        model.addAttribute("batches", batches);
        model.addAttribute("selectedBatchId", selectedBatchId);
        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("summaries", summaries);
        model.addAttribute("activePage", "attendance");

        return "teacher/attendance/history";
    }
}
