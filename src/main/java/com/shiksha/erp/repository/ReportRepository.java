package com.shiksha.erp.repository;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByClassBatchAndSubjectAndExamDate(ClassBatch classBatch, String subject, LocalDate examDate);

    List<Report> findByClassBatchIdAndSubjectAndExamDate(Long classBatchId, String subject, LocalDate examDate);

    List<Report> findByStudentId(Long studentId);

    List<Report> findByStudentIdOrderByExamDateDesc(Long studentId);

    List<Report> findTop3ByStudentIdOrderByExamDateDesc(Long studentId);

    List<Report> findByClassBatchId(Long classBatchId);

    // ek batch mein jo subjects upload ho chuke hain
    @Query("SELECT DISTINCT r.subject FROM Report r WHERE r.classBatch.id = :classBatchId ORDER BY r.subject ASC")
    List<String> findDistinctSubjectsByClassBatch(@Param("classBatchId") Long classBatchId);

    @Query("SELECT DISTINCT r.subject FROM Report r WHERE r.student.id = :studentId ORDER BY r.subject ASC")
    List<String> findDistinctSubjectsByStudentId(@Param("studentId") Long studentId);

    boolean existsByStudentAndSubjectAndExamDate(Student student, String subject, LocalDate examDate);

    boolean existsByStudentIdAndSubjectAndExamDate(Long studentId, String subject, LocalDate examDate);

    Optional<Report> findByStudentIdAndSubjectAndExamDate(Long studentId, String subject, LocalDate examDate);
}
