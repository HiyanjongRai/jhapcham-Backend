package com.example.jhapcham.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class FileStorageService {

    private final Path root;
    private static final long MAX_FILE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf");

    public FileStorageService(
            @Value("${file.upload.dir:uploads}") String uploadDir
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

        if (file.getSize() <= 0 || file.getSize() > MAX_FILE_BYTES) {
            throw new RuntimeException("File size is not allowed");
        }

        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            throw new RuntimeException("File extension is required");
        }

        String extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Unsupported file extension");
        }

        validateMagicBytes(file, extension, contentType.toLowerCase(Locale.ROOT));
    }

    private void validateMagicBytes(MultipartFile file, String extension, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            boolean valid = switch (extension) {
                case "jpg", "jpeg" -> startsWith(header, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                case "png" -> startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
                case "webp" -> header.length >= 12
                        && startsWith(header, new byte[] {0x52, 0x49, 0x46, 0x46})
                        && Arrays.equals(Arrays.copyOfRange(header, 8, 12), new byte[] {0x57, 0x45, 0x42, 0x50});
                case "gif" -> startsWith(header, new byte[] {0x47, 0x49, 0x46, 0x38});
                case "pdf" -> startsWith(header, new byte[] {0x25, 0x50, 0x44, 0x46});
                default -> false;
            };
            if (!valid) {
                throw new RuntimeException("File content does not match its extension");
            }
            if (extension.equals("pdf") && !"application/pdf".equals(contentType)) {
                throw new RuntimeException("PDF content type mismatch");
            }
            if (!extension.equals("pdf") && !contentType.startsWith("image/")) {
                throw new RuntimeException("Image content type mismatch");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to validate uploaded file", e);
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
