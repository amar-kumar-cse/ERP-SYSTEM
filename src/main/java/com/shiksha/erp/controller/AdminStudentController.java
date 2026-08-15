package com.shiksha.erp.controller;

import com.shiksha.erp.dto.StudentCreateDto;
import com.shiksha.erp.dto.StudentResponseDto;
import com.shiksha.erp.dto.StudentUpdateDto;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.service.StudentService;
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

@Controller
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;
    private final ClassBatchRepository classBatchRepository;

    @GetMapping
    public String listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<StudentResponseDto> studentPage = studentService.findAll(pageable, search);

        model.addAttribute("studentPage", studentPage);
        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", studentPage.getTotalPages());
        model.addAttribute("totalStudents", studentPage.getTotalElements());
        model.addAttribute("search", search);
        model.addAttribute("activePage", "students");

        return "admin/students/list";
    }

    @GetMapping("/add")
    public String addStudentForm(Model model) {
        if (!model.containsAttribute("student")) {
            model.addAttribute("student", new StudentCreateDto());
        }
        model.addAttribute("batches", classBatchRepository.findAll());
        model.addAttribute("activePage", "students");
        return "admin/students/add";
    }

    @PostMapping("/add")
    public String createStudent(
            @Valid @ModelAttribute("student") StudentCreateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("batches", classBatchRepository.findAll());
            model.addAttribute("activePage", "students");
            return "admin/students/add";
        }

        try {
            studentService.createStudent(dto);
            redirectAttributes.addFlashAttribute("successMsg", "Student enrolled successfully!");
            return "redirect:/admin/students";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("batches", classBatchRepository.findAll());
            model.addAttribute("activePage", "students");
            return "admin/students/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editStudentForm(@PathVariable Long id, Model model) {
        StudentResponseDto studentDto = studentService.findById(id);

        StudentUpdateDto updateDto = StudentUpdateDto.builder()
                .name(studentDto.getName())
                .parentName(studentDto.getParentName())
                .parentPhone(studentDto.getParentPhone())
                .classBatchId(studentDto.getClassBatchId())
                .build();

        model.addAttribute("student", updateDto);
        model.addAttribute("studentId", id);
        model.addAttribute("rollNo", studentDto.getRollNo());
        model.addAttribute("parentUsername", studentDto.getParentUsername());
        model.addAttribute("batches", classBatchRepository.findAll());
        model.addAttribute("activePage", "students");

        return "admin/students/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateStudent(
            @PathVariable Long id,
            @Valid @ModelAttribute("student") StudentUpdateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            StudentResponseDto original = studentService.findById(id);
            model.addAttribute("studentId", id);
            model.addAttribute("rollNo", original.getRollNo());
            model.addAttribute("parentUsername", original.getParentUsername());
            model.addAttribute("batches", classBatchRepository.findAll());
            model.addAttribute("activePage", "students");
            return "admin/students/edit";
        }

        try {
            studentService.updateStudent(id, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Student details updated successfully!");
            return "redirect:/admin/students";
        } catch (Exception ex) {
            StudentResponseDto original = studentService.findById(id);
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("studentId", id);
            model.addAttribute("rollNo", original.getRollNo());
            model.addAttribute("parentUsername", original.getParentUsername());
            model.addAttribute("batches", classBatchRepository.findAll());
            model.addAttribute("activePage", "students");
            return "admin/students/edit";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("successMsg", "Student deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/students";
    }
}
