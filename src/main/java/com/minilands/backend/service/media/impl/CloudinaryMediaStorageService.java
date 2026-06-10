package com.minilands.backend.service.media.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minilands.backend.dto.media.MediaUploadResponse;
import com.minilands.backend.service.media.MediaStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CloudinaryMediaStorageService implements MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryMediaStorageService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf",
            "video/mp4",
            "video/webm",
            // Chat attachments: common document types.
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            // Voice notes (future-proofing for the chat feature).
            "audio/mpeg",
            "audio/mp4",
            "audio/aac",
            "audio/ogg",
            "audio/webm",
            "audio/m4a",
            "audio/x-m4a",
            "application/octet-stream"  // Android file picker sends this for images
    );

    private final Cloudinary cloudinary;

    public CloudinaryMediaStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public MediaUploadResponse upload(MultipartFile file, String folder) {
        return upload(file, folder, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaUploadResponse upload(MultipartFile file, String folder, String publicId) {
        validateFile(file);

        String resolvedFolder = normalizeFolder(folder);
        String resolvedPublicId = resolvePublicId(publicId);
        log.info("[Media] uploading file — name={} contentType={} size={} folder={} publicId={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize(), resolvedFolder, resolvedPublicId);

        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", resolvedFolder,
                    "public_id", resolvedPublicId,
                    "resource_type", "auto",
                    "overwrite", true);

            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            MediaUploadResponse response = mapResponse(result);
            log.info("[Media] upload success — secureUrl={} format={} bytes={}",
                    response.secureUrl(), response.format(), response.bytes());
            return response;
        } catch (IOException ex) {
            log.error("[Media] failed to read file bytes: {}", ex.getMessage());
            throw new IllegalArgumentException("Failed to read uploaded file", ex);
        } catch (Exception ex) {
            log.error("[Media] Cloudinary upload failed: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Cloudinary upload failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            throw new IllegalArgumentException("publicId is required");
        }
        log.info("[Media] deleting publicId={}", publicId);
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("[Media] delete success — publicId={}", publicId);
        } catch (Exception ex) {
            log.error("[Media] Cloudinary delete failed: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Cloudinary delete failed: " + ex.getMessage(), ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("[Media] rejected file — contentType={} originalName={}", contentType, file.getOriginalFilename());
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    private String normalizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return "minilands";
        }
        return folder.trim().replaceAll("^/+|/+$", "");
    }

    private String resolvePublicId(String publicId) {
        if (StringUtils.hasText(publicId)) {
            return publicId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private MediaUploadResponse mapResponse(Map<String, Object> result) {
        return new MediaUploadResponse(
                (String) result.get("url"),
                (String) result.get("secure_url"),
                (String) result.get("public_id"),
                (String) result.get("resource_type"),
                (String) result.get("format"),
                toLong(result.get("bytes")),
                toInteger(result.get("width")),
                toInteger(result.get("height")));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
