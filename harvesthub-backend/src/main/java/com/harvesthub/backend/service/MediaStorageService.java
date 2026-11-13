package com.harvesthub.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores uploaded media under a single directory with server-generated names.
 * Only whitelisted image/video types are accepted, verified by magic bytes, never by client filename.
 */
@Service
public class MediaStorageService {

    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES = 25L * 1024 * 1024;
    private static final Pattern SAFE_NAME = Pattern.compile("^[a-f0-9-]{36}\\.(jpg|png|webp|mp4)$");

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg", "png", "image/png", "webp", "image/webp", "mp4", "video/mp4");

    private final Path root;

    public MediaStorageService(@Value("${harvesthub.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Validates and stores the file; returns the public URL path (e.g. /uploads/uuid.jpg). */
    public String store(MultipartFile file, boolean allowVideo) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String ext = detectExtension(file);
        if (ext == null || ("mp4".equals(ext) && !allowVideo)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    allowVideo ? "Only JPEG, PNG, WEBP images or MP4 video are allowed" : "Only JPEG, PNG or WEBP images are allowed");
        }
        long limit = "mp4".equals(ext) ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
        if (file.getSize() > limit) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large");
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            Files.createDirectories(root);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
        return "/uploads/" + name;
    }

    /** Resolves a stored file by its generated name; rejects anything that is not a generated name. */
    public Path resolve(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return null;
        }
        Path p = root.resolve(name).normalize();
        return p.startsWith(root) && Files.isRegularFile(p) ? p : null;
    }

    public String contentType(String name) {
        String ext = name.substring(name.lastIndexOf('.') + 1);
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private static String detectExtension(MultipartFile file) {
        byte[] h = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int n = in.readNBytes(h, 0, h.length);
            if (n < 12) return null;
        } catch (IOException e) {
            return null;
        }
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        if (h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') return "mp4";
        return null;
    }
}
