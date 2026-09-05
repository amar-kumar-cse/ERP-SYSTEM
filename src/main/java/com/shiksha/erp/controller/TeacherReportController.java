package com.shiksha.erp.controller;

import com.shiksha.erp.dto.BulkReportDto;
import com.shiksha.erp.dto.ReportRowDto;
import com.shiksha.erp.dto.StudentMarkEntryDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.service.ReportService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/teacher/reports")
@RequiredArgsConstructor
public class TeacherReportController {

    private final TeacherAccessHelper teacherAccessHelper;
    private final ReportService reportService;
    private final ReportRepository reportRepository;
    private final ClassBatchRepository classBatchRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public String selectReportForm(Model model, Authentication auth) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> batches = teacherAccessHelper.getTeacherBatches(teacher);

        model.addAttribute("batches", batches);
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("defaultSubject", teacher.getSubject() != null ? teacher.getSubject() : "");
        model.addAttribute("activePage", "reports");

        return "teacher/reports/select";
    }

    @GetMapping("/mark")
    public String markReportForm(
            @RequestParam Long batchId,
            @RequestParam String subject,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate examDate,
            @RequestParam(defaultValue = "100") Integer maxMarks,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unauthorized: You do not have access to this class batch");
            return "redirect:/teacher/reports";
        }

        if (subject == null || subject.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please specify a subject");
            return "redirect:/teacher/reports";
        }

        if (maxMarks == null || maxMarks <= 0) {
            redirectAttributes.addFlashAttribute("errorMsg", "Maximum marks must be greater than 0");
            return "redirect:/teacher/reports";
        }

        ClassBatch batch = classBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", batchId));

        List<Student> students = studentRepository.findByClassBatchIdOrderByNameAsc(batchId);

        List<StudentMarkEntryDto> entries = students.stream().map(student -> {
            Optional<Report> existing = reportRepository.findByStudentIdAndSubjectAndExamDate(student.getId(), subject.trim(), examDate);
            return StudentMarkEntryDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .rollNo(student.getRollNo())
                    .marks(existing.map(Report::getMarks).orElse(null))
                    .remarks(existing.map(Report::getRemarks).orElse(null))
                    .build();
        }).toList();

        BulkReportDto bulkDto = BulkReportDto.builder()
                .classBatchId(batchId)
                .batchName(batch.getBatchName())
                .subject(subject.trim())
                .examDate(examDate)
                .maxMarks(maxMarks)
                .entries(entries)
                .build();

        model.addAttribute("bulkReportDto", bulkDto);
        model.addAttribute("batch", batch);
        model.addAttribute("studentCount", entries.size());
        model.addAttribute("activePage", "reports");

        return "teacher/reports/mark";
    }

    @PostMapping("/mark")
    public String saveReport(
            @Valid @ModelAttribute("bulkReportDto") BulkReportDto dto,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        String markRedirectUrl = UriComponentsBuilder.fromPath("/teacher/reports/mark")
                .queryParam("batchId", dto.getClassBatchId())
                .queryParam("subject", dto.getSubject())
                .queryParam("examDate", dto.getExamDate())
                .queryParam("maxMarks", dto.getMaxMarks())
                .build().encode().toUriString();

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Invalid report submission data");
            return "redirect:" + markRedirectUrl;
        }

        try {
            reportService.saveBulkReport(dto, teacher);
            redirectAttributes.addFlashAttribute("successMsg", "Test marks successfully uploaded for " + dto.getSubject() + "!");
            String viewRedirectUrl = UriComponentsBuilder.fromPath("/teacher/reports/view")
                    .queryParam("batchId", dto.getClassBatchId())
                    .queryParam("subject", dto.getSubject())
                    .build().encode().toUriString();
            return "redirect:" + viewRedirectUrl;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
            return "redirect:" + markRedirectUrl;
        }
    }

    @GetMapping("/view")
    public String viewReports(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate examDate,
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

        List<ReportRowDto> reports = List.of();
        List<String> subjects = List.of();
        ClassBatch selectedBatch = null;

        if (selectedBatchId != null) {
            if (!teacherAccessHelper.isBatchOwnedByTeacher(selectedBatchId, teacher)) {
                redirectAttributes.addFlashAttribute("errorMsg", "Unauthorized: You do not have access to this batch");
                return "redirect:/teacher/reports";
            }
            subjects = reportService.getSubjectsForBatch(selectedBatchId, teacher);
            reports = reportService.getReportsByBatchAndFilter(selectedBatchId, subject, examDate, teacher);
            selectedBatch = classBatchRepository.findById(selectedBatchId).orElse(null);
        }

        model.addAttribute("batches", batches);
        model.addAttribute("selectedBatchId", selectedBatchId);
        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedExamDate", examDate);
        model.addAttribute("reports", reports);
        model.addAttribute("activePage", "reports");

        return "teacher/reports/view";
    }

    @PostMapping("/delete/{id}")
    public String deleteReport(
            @PathVariable Long id,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String subject,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        try {
            reportService.deleteReport(id, teacher);
            redirectAttributes.addFlashAttribute("successMsg", "Report entry deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/teacher/reports/view");
        if (batchId != null) {
            uriBuilder.queryParam("batchId", batchId);
        }
        if (subject != null && !subject.isBlank()) {
            uriBuilder.queryParam("subject", subject);
        }
        return "redirect:" + uriBuilder.build().encode().toUriString();
    }
}
