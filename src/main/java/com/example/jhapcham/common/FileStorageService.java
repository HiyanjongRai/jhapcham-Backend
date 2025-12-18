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
            Files.createDirectories(this.root);
            System.out.println("[FileStorage] Root dir = " + this.root);
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
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) {
                original = "file.bin";
            }

            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            int dot = original.lastIndexOf('.');
            if (dot >= 0 && !fileName.contains(".")) {
                fileName = fileName + original.substring(dot);
            }

            Path target = dir.resolve(fileName).normalize();
            System.out.println("[FileStorage] Writing -> " + target);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return subdir + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }


}