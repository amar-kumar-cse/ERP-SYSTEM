package com.shiksha.erp.repository;

import com.shiksha.erp.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    // search teachers by first name, last name, subject, or username
    @Query("SELECT t FROM Teacher t WHERE " +
           "LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.user.username) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Teacher> searchTeachers(@Param("search") String search, Pageable pageable);
}
