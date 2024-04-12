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
import ams.com.ams.repository.DepartmentRepository;
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

@RestController
@RequestMapping("/api/borrowing-records")
@SecurityRequirement(name = "Authorization")
public class BorrowingRecordController {
    @Autowired
    private BorrowingRecordRepository borrowingRecordRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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
    public ResponseEntity<BorrowingRecord> assignAsset(@RequestBody BorrowingRecordRequest request) {
        Optional<Department> department = departmentRepository.findById(request.getDepartmentId());
        if (!department.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Optional<Asset> assetOptional = assetRepository.findById(request.getAssetId());
        if (!assetOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Asset asset = assetOptional.get();
        String status = asset.getStatus();

        if (!status.equals("AVAILABLE")){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        BorrowingRecord record = new BorrowingRecord();
        asset.setStatus("IN USE: " + department.get().getName());
        assetRepository.save(asset);
        record.setAsset(asset);
        record.setDepartment(department.get());
        record.setStatus("IN USE");
        borrowingRecordRepository.save(record);
        logActivity("ASSIGN", asset.getName());
        return new ResponseEntity<>(record, HttpStatus.OK);
    }
    
    @PreAuthorize("hasAuthority('Employee') or hasAuthority('Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<BorrowingRecord> updateQuantities(@PathVariable("id") Long id, @RequestParam("status") String status) {
        Optional<BorrowingRecord> borrowingRecordData = borrowingRecordRepository.findById(id);
        if (!(status.equals("missing") || status.equals("damaged"))){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (borrowingRecordData.isPresent()) {
            BorrowingRecord borrowingRecord = borrowingRecordData.get(); 
            borrowingRecord.setStatus(status == "damaged" ? "Damaged" : "Missing");    
            BorrowingRecord updatedBorrowingRecord = borrowingRecordRepository.save(borrowingRecord);
            Asset asset = borrowingRecord.getAsset();
            asset.setStatus(status.toUpperCase());
            assetRepository.save(asset);
            logActivity(status.toUpperCase() + " ASSET", asset.getName());
            return new ResponseEntity<>(updatedBorrowingRecord, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void logActivity(String content, String entity) {
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
        activityLog.setTimestamp(new Date());
        activityLog.setUsername(name);
        activityLogRepository.save(activityLog);
    }
}