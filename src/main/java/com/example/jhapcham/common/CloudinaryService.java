package com.example.jhapcham.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.jhapcham.Error.BusinessValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Pattern SAFE_FOLDER = Pattern.compile("[A-Za-z0-9_/-]{1,80}");

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
        validateImage(file, folder);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "allowed_formats", java.util.List.of("jpg", "jpeg", "png", "webp"),
                            "quality", "auto",
                            "fetch_format", "auto"));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BusinessValidationException("Image upload failed. Please try another image.");
        }
    }

    private void validateImage(MultipartFile file, String folder) {
        if (folder == null || !SAFE_FOLDER.matcher(folder).matches() || folder.contains("..")) {
            throw new BusinessValidationException("Invalid upload folder");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(java.util.Locale.ROOT))) {
            throw new BusinessValidationException("Unsupported image type. Use JPG, PNG, or WebP.");
        }
        if (file.getSize() <= 0 || file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessValidationException("Image size must be between 1 byte and 10 MB.");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            if (!isJpeg(header) && !isPng(header) && !isWebp(header)) {
                throw new BusinessValidationException("Image content does not match JPG, PNG, or WebP.");
            }
        } catch (BusinessValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessValidationException("Unable to validate image.");
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 4
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
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
