package com.shiksha.erp.controller;

import com.shiksha.erp.dto.ClassBatchResponseDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.service.ClassBatchService;
import com.shiksha.erp.service.TeacherAccessHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping({"/teacher/my-classes", "/teacher/classes"})
@RequiredArgsConstructor
public class TeacherMyClassesController {

    private final TeacherAccessHelper teacherAccessHelper;
    private final ClassBatchService classBatchService;
    private final ClassBatchRepository classBatchRepository;
    private final StudentRepository studentRepository;

    @GetMapping
    public String myClasses(Model model, Authentication auth) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);
        List<ClassBatch> batches = teacherAccessHelper.getTeacherBatches(teacher);

        List<ClassBatchResponseDto> batchDtos = batches.stream()
                .map(classBatchService::toResponseDto)
                .toList();

        model.addAttribute("batches", batchDtos);
        model.addAttribute("teacher", teacher);
        model.addAttribute("activePage", "my-classes");

        return "teacher/my-classes";
    }

    @GetMapping("/{batchId}/students")
    public String batchStudents(
            @PathVariable Long batchId,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        Teacher teacher = teacherAccessHelper.getTeacherFromPrincipal(auth);

        // batch teacher ka hai verify karo
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unauthorized: You do not have access to this class batch");
            return "redirect:/teacher/my-classes";
        }

        ClassBatch batch = classBatchRepository.findById(batchId)
                .orElseThrow(() -> new com.shiksha.erp.exception.ResourceNotFoundException("ClassBatch", "id", batchId));

        List<Student> students = studentRepository.findByClassBatchIdOrderByNameAsc(batchId);

        model.addAttribute("batch", batch);
        model.addAttribute("students", students);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("activePage", "my-classes");

        return "teacher/batch-students";
    }
}
