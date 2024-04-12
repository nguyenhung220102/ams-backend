package ams.com.ams.controller;
import ams.com.ams.model.ActivityLog;
import ams.com.ams.model.Asset;
import ams.com.ams.model.BorrowingRecord;
import ams.com.ams.model.Department;
import ams.com.ams.model.User;
import ams.com.ams.payload.request.BorrowingRecordRequest;
import ams.com.ams.repository.ActivityLogRepository;
import ams.com.ams.repository.AssetRepository;
import ams.com.ams.repository.BorrowingRecordRepository;
import ams.com.ams.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/borrowing-records")
@SecurityRequirement(name = "Authorization")
public class BorrowingRecordController {
    private static final Logger logger = LoggerFactory.getLogger(BorrowingRecordController.class);
    @Autowired
    private BorrowingRecordRepository borrowingRecordRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasAuthority('Employee') or hasAuthority('Manager')")
    @GetMapping("")
    public ResponseEntity<List<BorrowingRecord>> getBorrowingRecords() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            Department department = user.get().getDepartment();
            if (department != null) {
                List<BorrowingRecord> records = borrowingRecordRepository.findByDepartmentId(department.getId());
                return new ResponseEntity<>(records, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("hasAuthority('Manager') or hasAuthority('Employee')")
    @PostMapping("")
    public ResponseEntity<BorrowingRecord> createOrUpdateBorrowingRecord(@RequestBody BorrowingRecordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        if (!user.isPresent()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Department department = user.get().getDepartment();
    
        Optional<Asset> assetOptional = assetRepository.findById(request.getAssetId());
        if (!assetOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Asset asset = assetOptional.get();
        int requestedQuantity = request.getQuantity();
        int availableQuantity = asset.getAvailableQuantity();
        if (availableQuantity < requestedQuantity) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Optional<BorrowingRecord> record = borrowingRecordRepository.findByDepartmentAndAsset(department, asset);
        if (record.isPresent()) {
            BorrowingRecord existingRecord = record.get();
            existingRecord.setQuantity(existingRecord.getQuantity() + requestedQuantity);
            asset.setAvailableQuantity(availableQuantity - requestedQuantity);
            assetRepository.save(asset);
            borrowingRecordRepository.save(existingRecord);
            logger.info(username, "-", asset.getName(), '-', user.get().getUsername());
            return new ResponseEntity<>(existingRecord, HttpStatus.OK);
        } else {
            BorrowingRecord borrowingRecord = new BorrowingRecord();
            borrowingRecord.setQuantity(requestedQuantity);
            borrowingRecord.setAsset(asset);
            asset.setAvailableQuantity(availableQuantity - requestedQuantity);
            assetRepository.save(asset);
            borrowingRecord.setDepartment(department);
            BorrowingRecord createdRecord = borrowingRecordRepository.save(borrowingRecord);
            logActivity("BORROW", asset.getName(), requestedQuantity);
            return new ResponseEntity<>(createdRecord, HttpStatus.CREATED);
        }
    }
    

    @PreAuthorize("hasAuthority('Manager') or hasAuthority('Employee')")
    @PutMapping("/return/{id}")
    public ResponseEntity<BorrowingRecord> returnAsset(@PathVariable("id") Long recordId) {
        Optional<BorrowingRecord> recordOptional = borrowingRecordRepository.findById(recordId);
        if (recordOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        BorrowingRecord borrowingRecord = recordOptional.get();
        Asset asset = borrowingRecord.getAsset();
        int returnedQuantity = borrowingRecord.getQuantity();
            int currentAvailableQuantity = asset.getAvailableQuantity();
        asset.setAvailableQuantity(currentAvailableQuantity + returnedQuantity);
        assetRepository.save(asset);
        logActivity("RETURN", asset.getName(), returnedQuantity);
        borrowingRecordRepository.deleteById(recordId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @PreAuthorize("hasAuthority('Employee') or hasAuthority('Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<BorrowingRecord> updateQuantities(@PathVariable("id") Long id, @RequestParam("missingQuantity") int missingQuantity, @RequestParam("damagedQuantity") int damagedQuantity) {
        Optional<BorrowingRecord> borrowingRecordData = borrowingRecordRepository.findById(id);
        if (borrowingRecordData.isPresent()) {
            BorrowingRecord borrowingRecord = borrowingRecordData.get();     
            borrowingRecord.setQuantity(borrowingRecordData.get().getQuantity() - missingQuantity - damagedQuantity);
            BorrowingRecord updatedBorrowingRecord = borrowingRecordRepository.save(borrowingRecord);
            Asset asset = borrowingRecord.getAsset();
            asset.setMissingQuantity(asset.getMissingQuantity() + missingQuantity);
            asset.setDamagedQuantity(asset.getDamagedQuantity() + damagedQuantity);
            assetRepository.save(asset);
            if (damagedQuantity != 0){
                logActivity("DAMAGED EQUIPMENT", asset.getName(), damagedQuantity);
            } else logActivity("MISSING EQUIPMENT", asset.getName(), missingQuantity);
            return new ResponseEntity<>(updatedBorrowingRecord, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void logActivity(String content, String entity, int quantity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String name = "";
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            String role = user.get().getRole();
                if ("Manager".equals(role)) {
                name = username;
            } else {
                if (user.get().getDepartment() != null) {
                    name = user.get().getDepartment().getName() + ":" + user.get().getUsername();
                }
            }
        }
        ActivityLog activityLog = new ActivityLog();
        activityLog.setContent(content);
        activityLog.setEntity(entity);
        activityLog.setQuantity(quantity);
        activityLog.setTimestamp(new Date());
        activityLog.setUsername(name);
        activityLogRepository.save(activityLog);
    }
}