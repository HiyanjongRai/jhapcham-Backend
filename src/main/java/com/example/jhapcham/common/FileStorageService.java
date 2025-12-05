package com.example.jhapcham.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(
            @Value("${file.upload.dir:H:/Project/Ecomm/jhapcham/uploads}") String uploadDir
    ) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root); // ensure base dir exists
            System.out.println("[FileStorage] Root dir = " + this.root);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create upload root: " + this.root, e);
        }
    }

    /**
     * Save under {root}/{subdir}/{fileName} exactly as provided
     * @param file MultipartFile
     * @param subdir folder name, e.g., seller_docs or seller_logos
     * @param fileName desired file name, e.g., id_5.pdf
     * @return relative path for DB: subdir/fileName
     */
    public String save(MultipartFile file, String subdir, String fileName) {
        if (file == null || file.isEmpty()) return null;

        try {
            Path dir = this.root.resolve(subdir).normalize();
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) original = "file.bin";

            // Remove illegal characters from fileName
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            // Ensure the extension is preserved
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && !fileName.contains(".")) {
                fileName = fileName + original.substring(dot);
            }

            Path target = dir.resolve(fileName).normalize();
            System.out.println("[FileStorage] Writing -> " + target);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return relative path for DB
            return subdir + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}
