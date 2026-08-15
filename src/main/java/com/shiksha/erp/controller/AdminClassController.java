package com.shiksha.erp.controller;

import com.shiksha.erp.dto.ClassBatchCreateDto;
import com.shiksha.erp.dto.ClassBatchResponseDto;
import com.shiksha.erp.service.ClassBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/classes")
@RequiredArgsConstructor
public class AdminClassController {

    private final ClassBatchService classBatchService;

    @GetMapping
    public String listClasses(Model model) {
        List<ClassBatchResponseDto> classes = classBatchService.findAll();
        model.addAttribute("classes", classes);
        model.addAttribute("totalClasses", classes.size());
        model.addAttribute("activePage", "classes");
        return "admin/classes/list";
    }

    @GetMapping("/add")
    public String addClassForm(Model model) {
        if (!model.containsAttribute("classBatch")) {
            model.addAttribute("classBatch", new ClassBatchCreateDto());
        }
        model.addAttribute("activePage", "classes");
        return "admin/classes/add";
    }

    @PostMapping("/add")
    public String createClass(
            @Valid @ModelAttribute("classBatch") ClassBatchCreateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "classes");
            return "admin/classes/add";
        }

        try {
            classBatchService.create(dto);
            redirectAttributes.addFlashAttribute("successMsg", "Class batch created successfully!");
            return "redirect:/admin/classes";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("activePage", "classes");
            return "admin/classes/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editClassForm(@PathVariable Long id, Model model) {
        ClassBatchResponseDto batch = classBatchService.findById(id);

        ClassBatchCreateDto editDto = ClassBatchCreateDto.builder()
                .batchName(batch.getBatchName())
                .timing(batch.getTiming())
                .days(batch.getDays())
                .build();

        model.addAttribute("classBatch", editDto);
        model.addAttribute("batchId", id);
        model.addAttribute("studentCount", batch.getStudentCount());
        model.addAttribute("activePage", "classes");

        return "admin/classes/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateClass(
            @PathVariable Long id,
            @Valid @ModelAttribute("classBatch") ClassBatchCreateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("batchId", id);
            model.addAttribute("activePage", "classes");
            return "admin/classes/edit";
        }

        try {
            classBatchService.update(id, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Class batch updated successfully!");
            return "redirect:/admin/classes";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            model.addAttribute("batchId", id);
            model.addAttribute("activePage", "classes");
            return "admin/classes/edit";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteClass(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            classBatchService.delete(id);
            redirectAttributes.addFlashAttribute("successMsg", "Class batch deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/classes";
    }
}
