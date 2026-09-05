package com.shiksha.erp.repository;

import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.entity.Resource;
import com.shiksha.erp.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByClassBatchAndSubjectOrderByUploadedAtDesc(ClassBatch classBatch, String subject);

    List<Resource> findByClassBatchOrderByUploadedAtDesc(ClassBatch classBatch);

    List<Resource> findByClassBatchIdOrderByUploadedAtDesc(Long classBatchId);

    @Query("SELECT DISTINCT r.subject FROM Resource r WHERE r.classBatch.id = :classBatchId ORDER BY r.subject ASC")
    List<String> findDistinctSubjectsByClassBatch(@Param("classBatchId") Long classBatchId);

    List<Resource> findByUploadedBy(Teacher teacher);

    List<Resource> findByUploadedById(Long teacherId);

    long countByUploadedById(Long teacherId);

    long countByClassBatchId(Long classBatchId);
}
