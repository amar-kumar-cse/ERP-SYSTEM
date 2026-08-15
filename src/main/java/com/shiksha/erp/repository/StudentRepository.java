package com.shiksha.erp.repository;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    Optional<Student> findByRollNo(String rollNo);

    boolean existsByRollNo(String rollNo);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    List<Student> findByParentUserId(Long parentUserId);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    Optional<Student> findFirstByParentUserId(Long parentUserId);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    List<Student> findByParentUser(User parentUser);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    Optional<Student> findFirstByParentUser(User parentUser);

    long countByParentUserId(Long parentUserId);

    long countByClassBatchId(Long classBatchId);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    List<Student> findByClassBatchId(Long classBatchId);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    List<Student> findByClassBatchIdOrderByNameAsc(Long classBatchId);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    List<Student> findByClassBatchIn(Collection<ClassBatch> batches);

    @Override
    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    Page<Student> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"classBatch", "parentUser"})
    @Query("SELECT s FROM Student s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.rollNo) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Student> searchStudents(@Param("search") String search, Pageable pageable);
}
