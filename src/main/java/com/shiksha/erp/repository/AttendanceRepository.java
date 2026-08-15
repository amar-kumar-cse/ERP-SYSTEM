package com.shiksha.erp.repository;

import com.shiksha.erp.entity.Attendance;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @EntityGraph(attributePaths = {"student", "markedBy", "classBatch"})
    List<Attendance> findByClassBatchAndDate(ClassBatch classBatch, LocalDate date);

    @EntityGraph(attributePaths = {"student", "markedBy", "classBatch"})
    List<Attendance> findByClassBatchIdAndDate(Long classBatchId, LocalDate date);

    @EntityGraph(attributePaths = {"student", "markedBy", "classBatch"})
    List<Attendance> findByStudentAndDateBetween(Student student, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"student", "markedBy", "classBatch"})
    List<Attendance> findByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);

    boolean existsByClassBatchAndDate(ClassBatch classBatch, LocalDate date);

    boolean existsByClassBatchIdAndDate(Long classBatchId, LocalDate date);

    @EntityGraph(attributePaths = {"student", "markedBy", "classBatch"})
    List<Attendance> findByStudentId(Long studentId);

    long countByStudentAndStatus(Student student, AttendanceStatus status);

    long countByStudentIdAndDateBetweenAndStatus(Long studentId, LocalDate from, LocalDate to, AttendanceStatus status);

    long countByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);

    Optional<Attendance> findByStudentAndDate(Student student, LocalDate date);

    Optional<Attendance> findByStudentIdAndDate(Long studentId, LocalDate date);
}
