package com.shiksha.erp.repository;

import com.shiksha.erp.entity.Fee;
import com.shiksha.erp.enums.FeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    @EntityGraph(attributePaths = {"student", "student.classBatch"})
    List<Fee> findByStudentIdOrderByYearDescMonthDesc(Long studentId);

    @EntityGraph(attributePaths = {"student", "student.classBatch"})
    Optional<Fee> findByStudentIdAndMonthAndYear(Long studentId, int month, int year);

    @EntityGraph(attributePaths = {"student", "student.classBatch"})
    List<Fee> findByMonthAndYear(int month, int year);

    @EntityGraph(attributePaths = {"student", "student.classBatch"})
    Page<Fee> findByMonthAndYear(int month, int year, Pageable pageable);

    @Query("SELECT f FROM Fee f JOIN FETCH f.student s LEFT JOIN FETCH s.classBatch cb WHERE cb.id = :classBatchId AND f.month = :month AND f.year = :year")
    List<Fee> findByClassBatchAndMonthAndYear(
            @Param("classBatchId") Long classBatchId,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query(value = "SELECT f FROM Fee f JOIN FETCH f.student s LEFT JOIN FETCH s.classBatch cb WHERE cb.id = :classBatchId AND f.month = :month AND f.year = :year",
           countQuery = "SELECT count(f) FROM Fee f JOIN f.student s WHERE s.classBatch.id = :classBatchId AND f.month = :month AND f.year = :year")
    Page<Fee> findByClassBatchAndMonthAndYear(
            @Param("classBatchId") Long classBatchId,
            @Param("month") int month,
            @Param("year") int year,
            Pageable pageable
    );

    long countByStatus(FeeStatus status);

    long countByMonthAndYearAndStatus(int month, int year, FeeStatus status);

    @EntityGraph(attributePaths = {"student", "student.parentUser"})
    List<Fee> findByStatusAndDueDateBefore(FeeStatus status, LocalDate date);

    boolean existsByStudentIdAndMonthAndYear(Long studentId, int month, int year);

    long countByStudentId(Long studentId);
}
