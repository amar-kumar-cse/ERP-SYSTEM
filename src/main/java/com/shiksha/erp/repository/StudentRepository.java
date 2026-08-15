package com.shiksha.erp.repository;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRollNo(String rollNo);

    boolean existsByRollNo(String rollNo);

    // parent dashboard pe unke bachhon ki list dikhane ke liye
    List<Student> findByParentUserId(Long parentUserId);

    Optional<Student> findFirstByParentUserId(Long parentUserId);

    List<Student> findByParentUser(User parentUser);

    Optional<Student> findFirstByParentUser(User parentUser);

    long countByParentUserId(Long parentUserId);

    // batch delete hone se pehle check karne ke liye
    long countByClassBatchId(Long classBatchId);

    // batch ke students list
    List<Student> findByClassBatchId(Long classBatchId);

    List<Student> findByClassBatchIdOrderByNameAsc(Long classBatchId);

    List<Student> findByClassBatchIn(Collection<ClassBatch> batches);

    // search by student name OR roll number
    @Query("SELECT s FROM Student s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.rollNo) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Student> searchStudents(@Param("search") String search, Pageable pageable);
}
