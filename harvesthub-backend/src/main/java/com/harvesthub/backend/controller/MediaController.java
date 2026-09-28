package com.harvesthub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class MediaController {

    private final String uploadDir = "uploads";

    @PostMapping({"/media/upload", "/api/media/upload"})
    public ResponseEntity<Map<String, String>> uploadMedia(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File is empty"));
        }
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFilename = UUID.randomUUID().toString() + extension;
            Path targetPath = Paths.get(uploadDir).resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + newFilename;
            return ResponseEntity.ok(Map.of("url", fileUrl, "filename", newFilename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "Failed to upload file: " + e.getMessage()));
        }
    }
}
