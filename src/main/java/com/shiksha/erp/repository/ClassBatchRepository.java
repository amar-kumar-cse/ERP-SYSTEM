package com.shiksha.erp.repository;

import com.shiksha.erp.entity.ClassBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassBatchRepository extends JpaRepository<ClassBatch, Long> {

    Optional<ClassBatch> findByBatchName(String batchName);

    boolean existsByBatchName(String batchName);
}
