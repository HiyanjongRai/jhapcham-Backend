package com.example.jhapcham.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class FileStorageService {

    private final Path root;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf");

    public FileStorageService(
            @Value("${file.upload.dir:H:/Project/Ecomm/jhapcham/uploads}") String uploadDir
    ) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
            log.info("File upload root initialized at {}", this.root);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create upload root: " + this.root, e);
        }
    }

    /**
     * Save under {root}/{subdir}/{fileName}
     * Example for seller docs
     * root = H:/Project/Ecomm/jhapcham/uploads
     * subdir = seller_docs
     * final path = H:/Project/Ecomm/jhapcham/uploads/seller_docs/fileName
     *
     * @return relative path like seller_docs/fileName for storing in DB
     */
    public String save(MultipartFile file, String subdir, String fileName) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Path dir = this.root.resolve(subdir).normalize();
            if (!dir.startsWith(this.root)) {
                throw new RuntimeException("Invalid upload directory");
            }
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) {
                original = "file.bin";
            }
            validateFile(file, original);

            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            int dot = original.lastIndexOf('.');
            if (dot >= 0 && !fileName.contains(".")) {
                fileName = fileName + original.substring(dot);
            }

            Path target = dir.resolve(fileName).normalize();
            if (!target.startsWith(dir)) {
                throw new RuntimeException("Invalid upload path");
            }
            log.debug("Writing uploaded file to {}", target);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return subdir + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file, String original) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("Unsupported file type");
        }

        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            throw new RuntimeException("File extension is required");
        }

        String extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Unsupported file extension");
        }
    }


}
