package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.media.MediaUploadResponse;
import com.minilands.backend.service.media.MediaStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaStorageService mediaStorageService;

    public AdminMediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder,
            @RequestParam(value = "publicId", required = false) String publicId) {
        MediaUploadResponse response = StringUtils.hasText(publicId)
                ? mediaStorageService.upload(file, folder, publicId)
                : mediaStorageService.upload(file, folder);
        return ResponseEntity.ok(response);
    }
}
