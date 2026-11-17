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

    /** Thumbnail widths we generate (requests are rounded up to the nearest one). */
    public static final int[] THUMB_SIZES = {160, 320, 640};

    /**
     * Small JPEG of a stored photo for list rows and cards (a phone photo is ~1 MB; the thumbnail a few KB).
     * Made on first request and kept under uploads/thumbs, so it also works for photos uploaded earlier.
     * Returns null for videos / unknown names / unreadable images.
     */
    public Path thumbnail(String name, int requested) {
        Path src = resolve(name);
        if (src == null || name.endsWith(".mp4")) return null;
        int size = THUMB_SIZES[THUMB_SIZES.length - 1];
        for (int s : THUMB_SIZES) {
            if (requested <= s) {
                size = s;
                break;
            }
        }
        Path dir = root.resolve("thumbs");
        Path out = dir.resolve(name.substring(0, name.lastIndexOf('.')) + "_" + size + ".jpg");
        if (Files.isRegularFile(out)) return out;
        synchronized (this) {   // one decode at a time keeps memory low on the small server
            if (Files.isRegularFile(out)) return out;
            try {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(src.toFile());
                if (img == null) return null;
                int w = img.getWidth(), h = img.getHeight();
                double scale = Math.min(1.0, (double) size / Math.max(w, h));
                int tw = Math.max(1, (int) Math.round(w * scale)), th = Math.max(1, (int) Math.round(h * scale));
                // halve repeatedly first: plain one-step bilinear shrinking of a 12 MP photo looks grainy
                java.awt.image.BufferedImage cur = img;
                while (cur.getWidth() / 2 >= tw && cur.getHeight() / 2 >= th) cur = scaled(cur, cur.getWidth() / 2, cur.getHeight() / 2);
                cur = scaled(cur, tw, th);
                Files.createDirectories(dir);
                Path tmp = dir.resolve(out.getFileName() + ".tmp");
                javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.82f);
                try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(tmp.toFile())) {
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(cur, null, null), param);
                } finally {
                    writer.dispose();
                }
                Files.move(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return out;
            } catch (Exception | OutOfMemoryError e) {
                return null;
            }
        }
    }

    private static java.awt.image.BufferedImage scaled(java.awt.image.BufferedImage in, int w, int h) {
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE);   // PNG transparency -> white
        g.fillRect(0, 0, w, h);
        g.drawImage(in, 0, 0, w, h, null);
        g.dispose();
        return out;
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
