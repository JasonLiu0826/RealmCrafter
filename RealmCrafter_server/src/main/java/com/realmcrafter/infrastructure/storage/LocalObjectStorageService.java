package com.realmcrafter.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * 本地磁盘存储：文件存到 base-dir，返回 base-url + 相对路径。
 * 生产可替换为阿里云 OSS / 腾讯 COS / S3 实现。
 */
@Slf4j
@Service
public class LocalObjectStorageService implements ObjectStorageService {

    @Value("${realmcrafter.upload.base-dir:./uploads}")
    private String baseDir;

    private Path basePath;

    @PostConstruct
    public void init() {
        basePath = Paths.get(baseDir).toAbsolutePath();
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.warn("Create upload dir failed: {}", e.getMessage());
        }
    }

    @Override
    public Optional<String> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return Optional.empty();
        String ext = getImageExtension(file.getOriginalFilename());
        String key = "images/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path target = basePath.resolve(key);
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
            String url = "/api/v1/files/" + key;
            return Optional.of(url);
        } catch (IOException e) {
            log.warn("Upload image failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String getImageExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".png";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return ".png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
        if (lower.endsWith(".gif")) return ".gif";
        if (lower.endsWith(".webp")) return ".webp";
        return ".png";
    }
}
