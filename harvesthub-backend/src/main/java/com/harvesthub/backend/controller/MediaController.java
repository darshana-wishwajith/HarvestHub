package com.harvesthub.backend.controller;

import com.harvesthub.backend.service.MediaStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@RestController
public class MediaController {

    private final MediaStorageService storage;

    public MediaController(MediaStorageService storage) {
        this.storage = storage;
    }

    /** Authenticated upload of crop photos / short videos. Returns the public URL of the stored file. */
    @PostMapping({"/media/upload", "/api/media/upload"})
    public ResponseEntity<Map<String, String>> uploadMedia(@RequestParam("file") MultipartFile file) {
        String url = storage.store(file, true);
        return ResponseEntity.ok(Map.of("url", url, "filename", url.substring(url.lastIndexOf('/') + 1)));
    }

    /** Serves stored media. Names are random UUIDs, so they are not guessable. */
    @GetMapping("/uploads/{name}")
    public ResponseEntity<Resource> getMedia(@PathVariable String name) {
        Path p = storage.resolve(name);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storage.contentType(name)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(p));
    }
}
