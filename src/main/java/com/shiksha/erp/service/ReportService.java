package com.shiksha.erp.service;

import com.shiksha.erp.dto.BulkReportDto;
import com.shiksha.erp.dto.ReportRowDto;
import com.shiksha.erp.dto.StudentMarkEntryDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.exception.BusinessValidationException;
import com.shiksha.erp.exception.ResourceNotFoundException;
import com.shiksha.erp.exception.UnauthorizedAccessException;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final StudentRepository studentRepository;
    private final ClassBatchRepository classBatchRepository;
    private final TeacherAccessHelper teacherAccessHelper;

    @Transactional
    public void saveBulkReport(BulkReportDto dto, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(dto.getClassBatchId(), teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + dto.getClassBatchId());
        }

        if (dto.getMaxMarks() == null || dto.getMaxMarks() <= 0) {
            throw new BusinessValidationException("Maximum marks must be greater than zero");
        }

        if (dto.getSubject() == null || dto.getSubject().isBlank()) {
            throw new BusinessValidationException("Subject name is required");
        }

        if (dto.getExamDate() == null) {
            throw new BusinessValidationException("Exam date is required");
        }

        ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassBatch", "id", dto.getClassBatchId()));

        String subject = dto.getSubject().trim();
        LocalDate examDate = dto.getExamDate();
        Integer maxMarks = dto.getMaxMarks();

        if (dto.getEntries() == null || dto.getEntries().isEmpty()) {
            return;
        }

        List<Report> reportsToSave = new ArrayList<>();

        for (StudentMarkEntryDto entry : dto.getEntries()) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", entry.getStudentId()));

            Integer marks = entry.getMarks() != null ? entry.getMarks() : 0;

            if (marks < 0 || marks > maxMarks) {
                throw new BusinessValidationException("Marks for " + student.getName() + " (" + marks + ") must be between 0 and " + maxMarks);
            }

            Optional<Report> existing = reportRepository.findByStudentIdAndSubjectAndExamDate(student.getId(), subject, examDate);

            if (existing.isPresent()) {
                Report rep = existing.get();
                rep.setMarks(marks);
                rep.setMaxMarks(maxMarks);
                rep.setRemarks(entry.getRemarks() != null ? entry.getRemarks().trim() : null);
                rep.setUploadedBy(teacher);
                rep.setClassBatch(classBatch);
                reportsToSave.add(rep);
            } else {
                Report newRep = Report.builder()
                        .student(student)
                        .classBatch(classBatch)
                        .subject(subject)
                        .examDate(examDate)
                        .marks(marks)
                        .maxMarks(maxMarks)
                        .remarks(entry.getRemarks() != null ? entry.getRemarks().trim() : null)
                        .uploadedBy(teacher)
                        .build();
                reportsToSave.add(newRep);
            }
        }

        reportRepository.saveAll(reportsToSave);
    }

    @Transactional(readOnly = true)
    public List<ReportRowDto> getReportsByBatchAndFilter(Long batchId, String subject, LocalDate examDate, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + batchId);
        }

        List<Report> reports = reportRepository.findByClassBatchId(batchId);
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }

        return reports.stream()
                .filter(r -> subject == null || subject.isBlank() || r.getSubject().equalsIgnoreCase(subject.trim()))
                .filter(r -> examDate == null || r.getExamDate().equals(examDate))
                .map(r -> {
                    double percentage = (r.getMaxMarks() != null && r.getMaxMarks() > 0)
                            ? Math.round(((double) (r.getMarks() != null ? r.getMarks() : 0) / r.getMaxMarks() * 100.0) * 100.0) / 100.0
                            : 0.0;

                    return ReportRowDto.builder()
                            .id(r.getId())
                            .studentName(r.getStudent().getName())
                            .rollNo(r.getStudent().getRollNo())
                            .subject(r.getSubject())
                            .examDate(r.getExamDate())
                            .marks(r.getMarks())
                            .maxMarks(r.getMaxMarks())
                            .percentage(percentage)
                            .remarks(r.getRemarks())
                            .uploadedByName(r.getUploadedBy() != null ? r.getUploadedBy().getFullName() : "Faculty")
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSubjectsForBatch(Long batchId, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You are not assigned to class batch ID: " + batchId);
        }
        return reportRepository.findDistinctSubjectsByClassBatch(batchId);
    }

    @Transactional
    public void deleteReport(Long id, Teacher teacher) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", id));

        if (!teacherAccessHelper.isBatchOwnedByTeacher(report.getClassBatch().getId(), teacher)) {
            throw new UnauthorizedAccessException("Unauthorized: You can only delete reports for your assigned batches");
        }

        reportRepository.delete(report);
    }
}
