package com.shiksha.erp.repository;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.entity.TeacherBatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherBatchRepository extends JpaRepository<TeacherBatch, Long> {

    @EntityGraph(attributePaths = {"classBatch", "teacher"})
    List<TeacherBatch> findByTeacher(Teacher teacher);

    @EntityGraph(attributePaths = {"classBatch", "teacher"})
    List<TeacherBatch> findByTeacherId(Long teacherId);

    @EntityGraph(attributePaths = {"classBatch", "teacher"})
    List<TeacherBatch> findByClassBatch(ClassBatch classBatch);

    @EntityGraph(attributePaths = {"classBatch", "teacher"})
    List<TeacherBatch> findByClassBatchId(Long classBatchId);

    boolean existsByTeacherAndClassBatch(Teacher teacher, ClassBatch classBatch);

    boolean existsByTeacherIdAndClassBatchId(Long teacherId, Long classBatchId);

    void deleteByTeacherAndClassBatch(Teacher teacher, ClassBatch classBatch);

    void deleteByTeacherIdAndClassBatchId(Long teacherId, Long classBatchId);

    void deleteByTeacherId(Long teacherId);

    void deleteByClassBatchId(Long classBatchId);

    long countByClassBatchId(Long classBatchId);
}
