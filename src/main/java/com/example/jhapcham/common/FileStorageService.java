package com.example.jhapcham.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(
            @Value("${file.upload.dir:C:/jhapcham/uploads}") String uploadDir // fallback default
    ) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root); // ensure base dir exists
            System.out.println("[FileStorage] Root dir = " + this.root); // DEBUG
        } catch (Exception e) {
            throw new RuntimeException("Cannot create upload root: " + this.root, e);
        }
    }

    /** Save under {root}/{subdir}/... and return the absolute path string. */
    public String save(MultipartFile file, String subdir, String prefix) {
        if (file == null || file.isEmpty()) return null;

        try {
            Path dir = this.root.resolve(subdir).normalize();
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) original = "file.bin";
            // Remove Windows-illegal chars
            String safeOriginal = original.replaceAll("[\\\\/:*?\"<>|]", "_");

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String base = (prefix == null || prefix.isBlank() ? "file" : prefix)
                    + "_" + ts + "_" + UUID.randomUUID().toString().substring(0, 8)
                    + "_" + safeOriginal;

            Path target = dir.resolve(base).normalize();
            System.out.println("[FileStorage] Writing -> " + target); // DEBUG

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}