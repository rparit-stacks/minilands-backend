package com.minilands.backend.service.media.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minilands.backend.dto.media.MediaUploadResponse;
import com.minilands.backend.service.media.MediaStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CloudinaryMediaStorageService implements MediaStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf",
            "video/mp4",
            "video/webm"
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

        try {
            String resolvedPublicId = resolvePublicId(publicId);
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", normalizeFolder(folder),
                    "public_id", resolvedPublicId,
                    "resource_type", "auto",
                    "overwrite", true);

            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            return mapResponse(result);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read uploaded file", ex);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cloudinary upload failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            throw new IllegalArgumentException("publicId is required");
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cloudinary delete failed: " + ex.getMessage(), ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
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
