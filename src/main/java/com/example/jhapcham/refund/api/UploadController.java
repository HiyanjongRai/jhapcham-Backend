package com.example.jhapcham.refund.api;

import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String relativePath = fileStorageService.save(file, "refund_evidence", fileName);
            String fileUrl = "/" + relativePath; // converts to "/refund_evidence/filename"
            return ResponseEntity.ok(Map.of("fileUrl", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Upload failed: " + e.getMessage()));
        }
    }
}
