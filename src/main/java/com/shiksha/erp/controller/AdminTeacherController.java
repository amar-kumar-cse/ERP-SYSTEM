package com.shiksha.erp.controller;

import com.shiksha.erp.dto.BatchAssignDto;
import com.shiksha.erp.dto.TeacherCreateDto;
import com.shiksha.erp.dto.TeacherResponseDto;
import com.shiksha.erp.dto.TeacherUpdateDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.TeacherBatch;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherService teacherService;
    private final ClassBatchRepository classBatchRepository;
    private final TeacherBatchRepository teacherBatchRepository;

    @GetMapping
    public String listTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<TeacherResponseDto> teacherPage = teacherService.findAll(pageable, search);

        model.addAttribute("teacherPage", teacherPage);
        model.addAttribute("teachers", teacherPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", teacherPage.getTotalPages());
        model.addAttribute("totalTeachers", teacherPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("activePage", "teachers");

        return "admin/teachers/list";
    }

    @GetMapping("/add")
    public String addTeacherForm(Model model) {
        if (!model.containsAttribute("teacher")) {
            model.addAttribute("teacher", new TeacherCreateDto());
        }
        model.addAttribute("activePage", "teachers");
        return "admin/teachers/add";
    }

    @PostMapping("/add")
    public String createTeacher(
            @Valid @ModelAttribute("teacher") TeacherCreateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "teachers");
            return "admin/teachers/add";
        }

        try {
            teacherService.createTeacher(dto);
            redirectAttributes.addFlashAttribute("successMsg", "Teacher registered successfully!");
            return "redirect:/admin/teachers";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("activePage", "teachers");
            return "admin/teachers/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editTeacherForm(@PathVariable Long id, Model model) {
        TeacherResponseDto teacher = teacherService.findById(id);

        TeacherUpdateDto updateDto = TeacherUpdateDto.builder()
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .phone(teacher.getPhone())
                .subject(teacher.getSubject())
                .joiningDate(teacher.getJoiningDate())
                .build();

        model.addAttribute("teacher", updateDto);
        model.addAttribute("teacherId", id);
        model.addAttribute("username", teacher.getUsername());
        model.addAttribute("email", teacher.getEmail());
        model.addAttribute("activePage", "teachers");

        return "admin/teachers/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateTeacher(
            @PathVariable Long id,
            @Valid @ModelAttribute("teacher") TeacherUpdateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            TeacherResponseDto teacher = teacherService.findById(id);
            model.addAttribute("teacherId", id);
            model.addAttribute("username", teacher.getUsername());
            model.addAttribute("email", teacher.getEmail());
            model.addAttribute("activePage", "teachers");
            return "admin/teachers/edit";
        }

        try {
            teacherService.updateTeacher(id, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Teacher profile updated successfully!");
            return "redirect:/admin/teachers";
        } catch (Exception ex) {
            TeacherResponseDto teacher = teacherService.findById(id);
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("teacherId", id);
            model.addAttribute("username", teacher.getUsername());
            model.addAttribute("email", teacher.getEmail());
            model.addAttribute("activePage", "teachers");
            return "admin/teachers/edit";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            teacherService.deleteTeacher(id);
            redirectAttributes.addFlashAttribute("successMsg", "Teacher deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/teachers";
    }

    @GetMapping("/{id}/batches")
    public String teacherBatches(@PathVariable Long id, Model model) {
        TeacherResponseDto teacher = teacherService.findById(id);
        List<TeacherBatch> teacherBatches = teacherBatchRepository.findByTeacherId(id);

        Set<Long> assignedBatchIds = teacherBatches.stream()
                .map(tb -> tb.getClassBatch().getId())
                .collect(Collectors.toSet());

        List<ClassBatch> availableBatches = classBatchRepository.findAll().stream()
                .filter(b -> !assignedBatchIds.contains(b.getId()))
                .toList();

        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherBatches", teacherBatches);
        model.addAttribute("availableBatches", availableBatches);
        model.addAttribute("batchAssignDto", BatchAssignDto.builder().teacherId(id).build());
        model.addAttribute("activePage", "teachers");

        return "admin/teachers/batches";
    }

    @PostMapping("/assign-batch")
    public String assignBatch(
            @Valid @ModelAttribute("batchAssignDto") BatchAssignDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please select a valid batch");
            return "redirect:/admin/teachers/" + dto.getTeacherId() + "/batches";
        }

        try {
            teacherService.assignToBatch(dto);
            redirectAttributes.addFlashAttribute("successMsg", "Class batch assigned to teacher successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        return "redirect:/admin/teachers/" + dto.getTeacherId() + "/batches";
    }

    @PostMapping("/remove-batch")
    public String removeBatch(
            @RequestParam Long teacherId,
            @RequestParam Long batchId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            teacherService.removeFromBatch(teacherId, batchId);
            redirectAttributes.addFlashAttribute("successMsg", "Class batch unassigned successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/teachers/" + teacherId + "/batches";
    }
}
