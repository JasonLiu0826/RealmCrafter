package com.realmcrafter.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * 对象存储抽象：本地目录 / 阿里云 OSS / 腾讯云 COS / AWS S3。
 * 上传后返回可访问的 CDN 或站点 URL，供封面、头像等使用。
 */
public interface ObjectStorageService {

    /**
     * 上传图片，生成唯一 key 并返回公网可访问 URL。
     *
     * @param file 图片文件（限制类型与大小由 Controller 或配置控制）
     * @return 成功时返回 URL，失败返回 empty
     */
    Optional<String> uploadImage(MultipartFile file);
}
