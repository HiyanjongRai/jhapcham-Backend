package com.example.jhapcham.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Cloudinary service initialized for cloud: {}", cloudName);
    }

    /**
     * Uploads a file to Cloudinary.
     * @param file The file to upload.
     * @param folder The folder in Cloudinary to store the file.
     * @return The secure URL of the uploaded image.
     */
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", folder));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    /**
     * Deletes an image from Cloudinary using its public ID or URL.
     * @param url The URL of the image to delete.
     */
    public void delete(String url) {
        if (url == null || !url.contains("cloudinary.com")) {
            return;
        }
        try {
            // Extract public ID from URL
            // Example: https://res.cloudinary.com/demo/image/upload/v1571218039/folder/sample.jpg
            // Public ID would be "folder/sample"
            String publicId = extractPublicId(url);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", url, e);
        }
    }

    private String extractPublicId(String url) {
        // Simple extraction logic for common Cloudinary URL formats
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash != -1 && lastDot != -1 && lastDot > lastSlash) {
            // Need to handle the folder if present
            String partAfterUpload = url.split("/upload/")[1];
            // Remove the version (e.g., v12345678/)
            String withoutVersion = partAfterUpload.substring(partAfterUpload.indexOf('/') + 1);
            // Remove the extension
            return withoutVersion.substring(0, withoutVersion.lastIndexOf('.'));
        }
        return url;
    }
}
