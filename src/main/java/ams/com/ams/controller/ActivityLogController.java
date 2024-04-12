package ams.com.ams.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ams.com.ams.model.ActivityLog;
import ams.com.ams.repository.ActivityLogRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/activitylogs")
@SecurityRequirement(name = "Authorization")
public class ActivityLogController {
    
    @Autowired
    private ActivityLogRepository activityLogRepository;

    @PreAuthorize("hasAuthority('Manager')")
    @GetMapping("")
    public ResponseEntity<List<ActivityLog>> getAllCategories() {
        List<ActivityLog> activityLogs = activityLogRepository.findAll();
        return new ResponseEntity<>(activityLogs, HttpStatus.OK);
    }
}
