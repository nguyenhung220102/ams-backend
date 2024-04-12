package ams.com.ams.controller;

import ams.com.ams.model.ActivityLog;
import ams.com.ams.model.Asset;
import ams.com.ams.model.Category;
import ams.com.ams.model.User;
import ams.com.ams.repository.ActivityLogRepository;
import ams.com.ams.repository.AssetRepository;
import ams.com.ams.repository.CategoryRepository;
import ams.com.ams.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@SecurityRequirement(name = "Authorization")
public class CategoryController {

    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private CategoryRepository categoryRepository;
        @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;
    @PreAuthorize ("hasAuthority('Employee') or hasAuthority('Manager')")
    @GetMapping("/{categoryId}")
    public ResponseEntity<List<Asset>> getAssetsByCategoryId(@PathVariable("categoryId") Long categoryId) {
        List<Asset> assets = assetRepository.findByCategoryId(categoryId);
        return new ResponseEntity<>(assets, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('Manager') or hasAuthority('Employee')")
    @GetMapping("")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }
    
    @PreAuthorize("hasAuthority('Manager')")
    @PostMapping("")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        try {
            logActivity("CREATE", "CATEGORY:" + category.getName(), 1);
            Category createdCategory = categoryRepository.save(category);
            return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('Manager')")
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable("id") Long id, @RequestBody Category category) {
        Optional<Category> categoryData = categoryRepository.findById(id);
        if (categoryData.isPresent()) {
            logActivity("UPDATE", "CATEGORY:" + categoryData.get().getName(), 1);
            Category updatedCategory = categoryData.get();
            updatedCategory.setName(category.getName());
            return new ResponseEntity<>(categoryRepository.save(updatedCategory), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasAuthority('Manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteCategory(@PathVariable("id") Long id) {
        try {
            Optional<Category> categoryData = categoryRepository.findById(id);
            if (categoryData.isPresent()) {
                logActivity("DELETE", "CATEGORY:" + categoryData.get().getName(), 1);
                assetRepository.deleteById(id);
            }
            categoryRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
                    name = user.get().getDepartment().getName();
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