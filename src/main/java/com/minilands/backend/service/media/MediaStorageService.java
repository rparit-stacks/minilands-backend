package com.minilands.backend.service.media;

import com.minilands.backend.dto.media.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Media upload abstraction (DIP). Cloudinary is the default implementation.
 */
public interface MediaStorageService {

    MediaUploadResponse upload(MultipartFile file, String folder);

    MediaUploadResponse upload(MultipartFile file, String folder, String publicId);

    void delete(String publicId);
}
