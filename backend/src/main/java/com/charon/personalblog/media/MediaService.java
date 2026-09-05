package com.charon.personalblog.media;

import com.charon.personalblog.common.ApiException;
import com.charon.personalblog.content.ContentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp", "image/gif", ".gif");
    private final JdbcClient jdbc;
    private final ContentService contents;
    private final Path root;

    public MediaService(JdbcClient jdbc, ContentService contents,
                        @Value("${app.media.root:./data/media}") String root) {
        this.jdbc = jdbc;
        this.contents = contents;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public MediaView upload(UUID owner, MultipartFile file, String purpose) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new ApiException(HttpStatus.BAD_REQUEST, "文件为空或超过 10MB");
        String type = file.getContentType();
        if (!ALLOWED.contains(type)) throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持 JPEG、PNG、WebP、GIF");
        if (!List.of("AVATAR", "COVER", "CONTENT").contains(purpose))
            throw new ApiException(HttpStatus.BAD_REQUEST, "媒体用途不合法");
        String key = owner + "/" + UUID.randomUUID() + EXTENSIONS.get(type);
        Path destination = root.resolve(key).normalize();
        if (!destination.startsWith(root)) throw new ApiException(HttpStatus.BAD_REQUEST, "存储路径不合法");
        try {
            Files.createDirectories(destination.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream source = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            UUID id = jdbc.sql("""
                    INSERT INTO media_resource(owner_id,original_name,storage_key,media_type,size_bytes,checksum_sha256,purpose)
                    VALUES (:owner,:name,:key,:type,:size,:checksum,:purpose) RETURNING id
                    """).param("owner", owner).param("name", safeName(file.getOriginalFilename()))
                    .param("key", key).param("type", type).param("size", file.getSize())
                    .param("checksum", HexFormat.of().formatHex(digest.digest())).param("purpose", purpose)
                    .query(UUID.class).single();
            return find(id);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "媒体保存失败");
        }
    }

    public List<MediaView> list(UUID owner) {
        return jdbc.sql("""
                SELECT id,owner_id,original_name,media_type,size_bytes,purpose,created_at
                FROM media_resource WHERE owner_id=:owner AND deleted_at IS NULL ORDER BY created_at DESC
                """).param("owner", owner).query(MediaView.class).list();
    }

    public MediaFile read(UUID id, UUID viewer) {
        StoredMedia media = jdbc.sql("""
                SELECT id,owner_id,original_name,storage_key,media_type,size_bytes,purpose,created_at
                FROM media_resource WHERE id=:id AND deleted_at IS NULL
                """).param("id", id).query(StoredMedia.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "媒体不存在"));
        boolean avatar = jdbc.sql("SELECT count(*) FROM app_user WHERE avatar_media_id=:id AND status='ACTIVE'")
                .param("id", id).query(Long.class).single() > 0;
        boolean allowed = media.ownerId().equals(viewer) || avatar ||
                jdbc.sql("SELECT id FROM content WHERE cover_media_id=:id UNION SELECT id FROM content WHERE body_markdown LIKE '%' || :marker || '%'")
                        .param("id", id).param("marker", "/api/v1/media/" + id).query(UUID.class).list().stream()
                        .anyMatch(contentId -> contents.canView(contentId, viewer));
        if (!allowed) throw new ApiException(HttpStatus.NOT_FOUND, "媒体不存在");
        try {
            Resource resource = new UrlResource(root.resolve(media.storageKey()).normalize().toUri());
            if (!resource.exists()) throw new ApiException(HttpStatus.NOT_FOUND, "媒体文件不存在");
            return new MediaFile(media, resource);
        } catch (Exception ex) {
            if (ex instanceof ApiException api) throw api;
            throw new ApiException(HttpStatus.NOT_FOUND, "媒体文件不存在");
        }
    }

    public void delete(UUID id, UUID owner) {
        int updated = jdbc.sql("UPDATE media_resource SET deleted_at=now() WHERE id=:id AND owner_id=:owner AND deleted_at IS NULL")
                .param("id", id).param("owner", owner).update();
        if (updated == 0) throw new ApiException(HttpStatus.NOT_FOUND, "媒体不存在");
    }

    private MediaView find(UUID id) {
        return jdbc.sql("""
                SELECT id,owner_id,original_name,media_type,size_bytes,purpose,created_at
                FROM media_resource WHERE id=:id
                """).param("id", id).query(MediaView.class).single();
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "image";
        return Path.of(name).getFileName().toString().replaceAll("[\\r\\n]", "");
    }

    public record MediaView(UUID id, UUID ownerId, String originalName, String mediaType,
                            long sizeBytes, String purpose, OffsetDateTime createdAt) {}
    public record StoredMedia(UUID id, UUID ownerId, String originalName, String storageKey,
                              String mediaType, long sizeBytes, String purpose, OffsetDateTime createdAt) {}
    public record MediaFile(StoredMedia metadata, Resource resource) {}
}
