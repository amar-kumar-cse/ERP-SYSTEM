package com.shiksha.erp.service;

import com.shiksha.erp.dto.BulkReportDto;
import com.shiksha.erp.dto.ReportRowDto;
import com.shiksha.erp.dto.StudentMarkEntryDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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
            throw new RuntimeException("Unauthorized: You are not assigned to this class batch");
        }

        if (dto.getMaxMarks() == null || dto.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than zero");
        }

        ClassBatch classBatch = classBatchRepository.findById(dto.getClassBatchId())
                .orElseThrow(() -> new RuntimeException("Class batch not found with id: " + dto.getClassBatchId()));

        String subject = dto.getSubject().trim();
        LocalDate examDate = dto.getExamDate();
        Integer maxMarks = dto.getMaxMarks();

        List<Report> reportsToSave = new ArrayList<>();

        for (StudentMarkEntryDto entry : dto.getEntries()) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: " + entry.getStudentId()));

            Integer marks = entry.getMarks() != null ? entry.getMarks() : 0;

            // marks validation (0 <= marks <= maxMarks)
            if (marks < 0 || marks > maxMarks) {
                throw new IllegalArgumentException("Marks for " + student.getName() + " (" + marks + ") must be between 0 and " + maxMarks);
            }

            // same student+subject+examDate already exists → update (not duplicate)
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
            throw new RuntimeException("Unauthorized: You are not assigned to this class batch");
        }

        List<Report> reports = reportRepository.findByClassBatchId(batchId);

        return reports.stream()
                .filter(r -> subject == null || subject.isBlank() || r.getSubject().equalsIgnoreCase(subject.trim()))
                .filter(r -> examDate == null || r.getExamDate().equals(examDate))
                .map(r -> {
                    double percentage = r.getMaxMarks() > 0
                            ? Math.round(((double) r.getMarks() / r.getMaxMarks() * 100.0) * 100.0) / 100.0
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
                            .uploadedByName(r.getUploadedBy().getFullName())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSubjectsForBatch(Long batchId, Teacher teacher) {
        if (!teacherAccessHelper.isBatchOwnedByTeacher(batchId, teacher)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this class batch");
        }
        return reportRepository.findDistinctSubjectsByClassBatch(batchId);
    }

    @Transactional
    public void deleteReport(Long id, Teacher teacher) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));

        // batch ownership check
        if (!teacherAccessHelper.isBatchOwnedByTeacher(report.getClassBatch().getId(), teacher)) {
            throw new RuntimeException("Unauthorized: You can only delete reports for your assigned batches");
        }

        reportRepository.delete(report);
    }
}
