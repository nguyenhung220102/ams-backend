package ams.com.ams.controller;

import ams.com.ams.model.ActivityLog;
import ams.com.ams.model.Asset;
import ams.com.ams.model.Department;
import ams.com.ams.model.User;
import ams.com.ams.repository.ActivityLogRepository;
import ams.com.ams.repository.AssetRepository;
import ams.com.ams.repository.DepartmentRepository;
import ams.com.ams.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
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
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Get all assets",
           description = "Retrieve a list of all assets.",
           tags = { "Assets" })
    @PreAuthorize("hasAuthority('Employee') or hasAuthority('Manager')")
    @GetMapping("")
    public ResponseEntity<List<Asset>> getAllAssets() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        String role = user.get().getRole();
        
        List<Asset> assets;
        if (role.equals("Manager")) {
            assets = assetRepository.findAll();
        } else {
            assets = assetRepository.findByDepartmentIsNull();
        }
        
        return new ResponseEntity<>(assets, HttpStatus.OK);
    }

    @Operation(summary = "Get asset by id",
           description = "Retrieve an asset by id.",
           tags = { "Assets" })
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

    @Operation(summary = "Create an asset.",
           description = "Create an asset.",
           tags = { "Assets" })
    @PreAuthorize("hasAuthority('Manager')")
    @PostMapping("")
    public ResponseEntity<Asset> createAsset(@RequestBody Asset asset) {
        logActivity("CREATE", asset.getName());
        Asset createdAsset = assetRepository.save(asset);
        return new ResponseEntity<>(createdAsset, HttpStatus.CREATED);
    }

    @Operation(summary = "Create an asset.",
    description = "Edit an asset.",
    tags = { "Assets" })
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

    @Operation(summary = "Delete an asset.",
    description = "Delete an asset.",
    tags = { "Assets" })
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

    @Operation(summary = "Get asset by department id.",
    description = "Get asset by department id.",
    tags = { "Assets" })
    @PreAuthorize("hasAuthority('Manager')")
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<Asset>> getAssetsByDepartment(@PathVariable Long departmentId) {
        Optional<Department> department = departmentRepository.findById(departmentId);
        if (!department.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Asset> assets = assetRepository.findByDepartment(department.get());
        return new ResponseEntity<>(assets, HttpStatus.OK);
    }

    @Operation(summary = "Employee get assets of their own department.",
    description = "Employee get assets of their own department.",
    tags = { "Assets" })
    @PreAuthorize("hasAuthority('Employee')")
    @GetMapping("/department/own")
    public ResponseEntity<List<Asset>> getAssetsOfOwnDepartment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        if (!user.isPresent() || user.get().getDepartment() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Department department = user.get().getDepartment();
        List<Asset> assets = assetRepository.findByDepartment(department);
        return new ResponseEntity<>(assets, HttpStatus.OK);
    }

    @Operation(summary = "Manager assign an asset to a department.",
    description = "Manager assign an asset to a department.",
    tags = { "Assets" })
    @PreAuthorize("hasAuthority('Manager')")
    @PostMapping("/assign")
    public ResponseEntity<Asset> assignAsset(@RequestParam("assetId") Long assetId, @RequestParam("departmentId") Long departmentId) {
        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (!departmentOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Department department = departmentOptional.get();
        Optional<Asset> assetOptional = assetRepository.findById(assetId);
        if (!assetOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Asset asset = assetOptional.get();
        if (!asset.getStatus().equals("AVAILABLE")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        asset.setStatus("IN USE");
        asset.setDepartment(department);
        assetRepository.save(asset);
        logActivity("ASSIGN ASSET TO " + department.getName(), asset.getName());
        return new ResponseEntity<>(asset, HttpStatus.OK);
    }

    @Operation(summary = "Manager or employe update an asset status.",
    description = "Manager or employe update an asset status.",
    tags = { "Assets" })
    @PreAuthorize("hasAuthority('Manager') or hasAuthority('Employee')")
    @PutMapping("/update/{id}")
    public ResponseEntity<Asset> updateAssetStatus(@PathVariable("id") Long assetId, @RequestParam("status") String status) {
        Optional<Asset> assetOptional = assetRepository.findById(assetId);
        if (!assetOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Asset asset = assetOptional.get();
        asset.setStatus(status.toUpperCase());
        assetRepository.save(asset);
        logActivity(status.toUpperCase() + " ASSET", asset.getName());
        return new ResponseEntity<>(asset, HttpStatus.OK);
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