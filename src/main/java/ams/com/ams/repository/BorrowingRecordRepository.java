package ams.com.ams.repository;

import ams.com.ams.model.Asset;
import ams.com.ams.model.BorrowingRecord;
import ams.com.ams.model.Department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    List<BorrowingRecord> findByDepartmentId(Long departmentId);
    Optional<BorrowingRecord> findByDepartmentAndAsset(Department department, Asset asset);
}
