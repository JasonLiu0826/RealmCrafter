package com.realmcrafter.api.upload;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.infrastructure.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传：返回 CDN/可访问 URL，供故事封面、设定集封面、用户头像使用。
 */
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = { "image/png", "image/jpeg", "image/gif", "image/webp" };

    private final ObjectStorageService objectStorageService;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "请选择图片文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.fail(400, "图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            return Result.fail(400, "仅支持 PNG / JPG / GIF / WebP");
        }
        return objectStorageService.uploadImage(file)
                .map(url -> Result.ok(Map.of("url", url)))
                .orElseGet(() -> Result.fail(500, "上传失败"));
    }

    private static boolean isAllowedImageType(String contentType) {
        for (String t : ALLOWED_TYPES) {
            if (t.equalsIgnoreCase(contentType)) return true;
        }
        return false;
    }
}
