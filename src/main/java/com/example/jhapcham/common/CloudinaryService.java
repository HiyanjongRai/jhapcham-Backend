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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.Locale;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf");
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

    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return upload(file, folder, "image", ALLOWED_IMAGE_TYPES, Set.of("jpg", "jpeg", "png", "webp"));
    }

    public String uploadDocument(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return upload(file, folder, "raw", ALLOWED_DOCUMENT_TYPES, Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf"));
    }

    private String upload(MultipartFile file, String folder, String resourceType, Set<String> allowedContentTypes, Set<String> allowedExtensions) {
        validateFile(file, folder, allowedContentTypes, allowedExtensions);
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType,
                            "allowed_formats", List.copyOf(allowedExtensions),
                            "quality", "auto",
                            "fetch_format", "auto"));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new BusinessValidationException("Upload failed. Please try another file.");
        }
    }

    private void validateFile(MultipartFile file, String folder, Set<String> allowedContentTypes, Set<String> allowedExtensions) {
        if (folder == null || !SAFE_FOLDER.matcher(folder).matches() || folder.contains("..")) {
            throw new BusinessValidationException("Invalid upload folder");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessValidationException("Unsupported file type.");
        }
        if (file.getSize() <= 0 || file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessValidationException("File size must be between 1 byte and 10 MB.");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BusinessValidationException("Filename is required.");
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            throw new BusinessValidationException("File extension is required.");
        }

        String extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessValidationException("Unsupported file extension.");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            boolean valid = switch (extension) {
                case "jpg", "jpeg" -> isJpeg(header);
                case "png" -> isPng(header);
                case "webp" -> isWebp(header);
                case "gif" -> isGif(header);
                case "pdf" -> isPdf(header);
                default -> false;
            };
            if (!valid) {
                throw new BusinessValidationException("File content does not match its extension.");
            }
        } catch (BusinessValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessValidationException("Unable to validate file.");
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

    private boolean isGif(byte[] header) {
        return header.length >= 4
                && header[0] == 0x47
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x38;
    }

    private boolean isPdf(byte[] header) {
        return header.length >= 4
                && header[0] == 0x25
                && header[1] == 0x50
                && header[2] == 0x44
                && header[3] == 0x46;
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
            String resourceType = extractResourceType(url);
            String publicId = extractPublicId(url);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", url, e);
        }
    }

    public void deleteByPublicId(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType == null ? "image" : resourceType));
        } catch (IOException e) {
            log.error("Failed to delete Cloudinary asset by publicId: {}", publicId, e);
        }
    }

    private String extractPublicId(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            return url;
        }
        String partAfterUpload = url.substring(uploadIndex + "/upload/".length());
        int firstSlash = partAfterUpload.indexOf('/');
        if (firstSlash >= 0 && partAfterUpload.startsWith("v")) {
            partAfterUpload = partAfterUpload.substring(firstSlash + 1);
        }
        int lastDot = partAfterUpload.lastIndexOf('.');
        if (lastDot > 0) {
            return partAfterUpload.substring(0, lastDot);
        }
        return partAfterUpload;
    }

    private String extractResourceType(String url) {
        if (url.contains("/raw/upload/")) {
            return "raw";
        }
        if (url.contains("/video/upload/")) {
            return "video";
        }
        return "image";
    }
}
