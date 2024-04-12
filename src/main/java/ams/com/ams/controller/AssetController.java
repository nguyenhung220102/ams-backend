package ams.com.ams.controller;

import ams.com.ams.model.ActivityLog;
import ams.com.ams.model.Asset;
import ams.com.ams.model.User;
import ams.com.ams.repository.ActivityLogRepository;
import ams.com.ams.repository.AssetRepository;
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
@RequestMapping("/api/assets")
@SecurityRequirement(name = "Authorization")
public class AssetController {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasAuthority('Employee') or hasAuthority('Manager')")
    @GetMapping("")
    public ResponseEntity<List<Asset>> getAllAssets() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        String role = user.get().getRole();
        if (role == "Manager"){
            List<Asset> assets = assetRepository.findAll();
            return new ResponseEntity<>(assets, HttpStatus.OK);
        } else {
            List<Asset> assets = assetRepository.findByStatus("AVAILABLE");
            return new ResponseEntity<>(assets, HttpStatus.OK);
        }

    }

    @PreAuthorize("hasAuthority('Employee') or hasAuthority('Manager')")
    @GetMapping("/{id}")
    public ResponseEntity<Asset> getAssetById(@PathVariable("id") Long id) {
        Optional<Asset> assetData = assetRepository.findById(id);
        if (assetData.isPresent()) {
            return new ResponseEntity<>(assetData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasAuthority('Manager')")
    @PostMapping("")
    public ResponseEntity<Asset> createAsset(@RequestBody Asset asset) {
        logActivity("CREATE", asset.getName());
        Asset createdAsset = assetRepository.save(asset);
        return new ResponseEntity<>(createdAsset, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<Asset> updateAsset(@PathVariable("id") Long id, @RequestBody Asset asset) {
        Optional<Asset> assetData = assetRepository.findById(id);
        if (assetData.isPresent()) {
            Asset updatedAsset = assetData.get();
            logActivity("UPDATE", asset.getName());
            updatedAsset.setName(asset.getName());
            return new ResponseEntity<>(assetRepository.save(updatedAsset), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasAuthority('Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteAsset(@PathVariable("id") Long id) {
        try {
            Optional<Asset> assetData = assetRepository.findById(id);
            if (assetData.isPresent()) {
                logActivity("DELETE", assetData.get().getName());
                assetRepository.deleteById(id);
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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