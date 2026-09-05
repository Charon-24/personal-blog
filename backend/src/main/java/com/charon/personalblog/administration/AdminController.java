package com.charon.personalblog.administration;

import com.charon.personalblog.common.ApiException;
import com.charon.personalblog.common.PageResult;
import com.charon.personalblog.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final JdbcClient jdbc;

    public AdminController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/stats")
    Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("users", scalar("SELECT count(*) FROM app_user"));
        result.put("activeUsers", scalar("SELECT count(*) FROM app_user WHERE status='ACTIVE'"));
        result.put("contents", scalar("SELECT count(*) FROM content WHERE deleted_at IS NULL"));
        result.put("publishedContents", scalar("SELECT count(*) FROM content WHERE status='PUBLISHED' AND deleted_at IS NULL"));
        result.put("comments", scalar("SELECT count(*) FROM comment WHERE status='VISIBLE'"));
        result.put("likes", scalar("SELECT count(*) FROM content_like"));
        return result;
    }

    @GetMapping("/users")
    PageResult<UserRow> users(@RequestParam(required = false) String q,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size) {
        String where = q == null || q.isBlank() ? "" :
                " WHERE username ILIKE '%' || :q || '%' OR email ILIKE '%' || :q || '%' OR display_name ILIKE '%' || :q || '%'";
        JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) FROM app_user" + where);
        JdbcClient.StatementSpec query = jdbc.sql("""
                SELECT id,username,email,display_name,role,status,email_verified,created_at,updated_at
                FROM app_user
                """ + where + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
                .param("limit", PageResult.safeSize(size)).param("offset", PageResult.offset(page, size));
        if (!where.isEmpty()) {
            count = count.param("q", q);
            query = query.param("q", q);
        }
        return new PageResult<>(query.query(UserRow.class).list(), count.query(Long.class).single(),
                Math.max(page, 1), PageResult.safeSize(size));
    }

    @PatchMapping("/users/{id}")
    @Transactional
    UserRow updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdate request) {
        if (!List.of("USER", "ADMIN").contains(request.role()) ||
                !List.of("PENDING", "ACTIVE", "DISABLED").contains(request.status()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "角色或状态不合法");
        jdbc.sql("UPDATE app_user SET role=:role,status=:status,updated_at=now() WHERE id=:id")
                .param("role", request.role()).param("status", request.status()).param("id", id).update();
        audit("USER_UPDATE", "USER", id.toString(), request.reason());
        return user(id);
    }

    @GetMapping("/contents")
    PageResult<ContentRow> contents(@RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        String filter = status == null || status.isBlank() ? "" : " WHERE c.status=:status";
        JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) FROM content c" + filter);
        JdbcClient.StatementSpec query = jdbc.sql("""
                SELECT c.id,c.title,c.slug,c.type,c.status,c.visibility,u.username AS author_username,
                       c.published_at,c.updated_at
                FROM content c JOIN app_user u ON u.id=c.author_id
                """ + filter + " ORDER BY c.updated_at DESC LIMIT :limit OFFSET :offset")
                .param("limit", PageResult.safeSize(size)).param("offset", PageResult.offset(page, size));
        if (!filter.isEmpty()) {
            count = count.param("status", status);
            query = query.param("status", status);
        }
        return new PageResult<>(query.query(ContentRow.class).list(), count.query(Long.class).single(),
                Math.max(page, 1), PageResult.safeSize(size));
    }

    @PatchMapping("/contents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void moderateContent(@PathVariable UUID id, @Valid @RequestBody Moderation request) {
        if (!List.of("PUBLISHED", "OFFLINE", "DELETED").contains(request.status()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容状态不合法");
        int changed = jdbc.sql("""
                UPDATE content SET status=:status,deleted_at=CASE WHEN :status='DELETED' THEN now() ELSE NULL END,
                  updated_at=now() WHERE id=:id
                """).param("status", request.status()).param("id", id).update();
        if (changed == 0) throw new ApiException(HttpStatus.NOT_FOUND, "内容不存在");
        audit("CONTENT_MODERATE", "CONTENT", id.toString(), request.reason());
    }

    @PatchMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void moderateComment(@PathVariable UUID id, @Valid @RequestBody CommentModeration request) {
        if (!List.of("VISIBLE", "HIDDEN", "DELETED").contains(request.status()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论状态不合法");
        int changed = jdbc.sql("""
                UPDATE comment SET status=:status,body=CASE WHEN :status='DELETED' THEN NULL ELSE body END,
                  updated_at=now() WHERE id=:id
                """).param("status", request.status()).param("id", id).update();
        if (changed == 0) throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        audit("COMMENT_MODERATE", "COMMENT", id.toString(), request.reason());
    }

    @GetMapping("/comments")
    PageResult<CommentRow> comments(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "50") int size) {
        long total = scalar("SELECT count(*) FROM comment");
        List<CommentRow> items = jdbc.sql("""
                SELECT c.id,c.content_id,c.body,c.status,c.created_at,u.username,
                       x.title AS content_title
                FROM comment c JOIN app_user u ON u.id=c.author_id
                JOIN content x ON x.id=c.content_id
                ORDER BY c.created_at DESC LIMIT :limit OFFSET :offset
                """).param("limit", PageResult.safeSize(size)).param("offset", PageResult.offset(page, size))
                .query(CommentRow.class).list();
        return new PageResult<>(items, total, Math.max(page, 1), PageResult.safeSize(size));
    }

    @GetMapping("/config")
    List<ConfigRow> config() {
        return jdbc.sql("""
                SELECT config_key,config_value,description,public_value,updated_at
                FROM site_config ORDER BY config_key
                """).query(ConfigRow.class).list();
    }

    @PutMapping("/config/{key}")
    ConfigRow updateConfig(@PathVariable String key, @Valid @RequestBody ConfigUpdate request) {
        jdbc.sql("""
                INSERT INTO site_config(config_key,config_value,description,public_value)
                VALUES (:key,:value,:description,:publicValue)
                ON CONFLICT(config_key) DO UPDATE SET config_value=excluded.config_value,
                  description=excluded.description,public_value=excluded.public_value,updated_at=now()
                """).param("key", key).param("value", request.value()).param("description", request.description())
                .param("publicValue", request.publicValue()).update();
        audit("CONFIG_UPDATE", "CONFIG", key, null);
        return jdbc.sql("""
                SELECT config_key,config_value,description,public_value,updated_at
                FROM site_config WHERE config_key=:key
                """).param("key", key).query(ConfigRow.class).single();
    }

    @GetMapping("/links")
    List<LinkRow> links() {
        return jdbc.sql("""
                SELECT id,name,description,url,icon_url,sort_order,enabled,created_at,updated_at
                FROM friend_link ORDER BY sort_order,name
                """).query(LinkRow.class).list();
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    LinkRow createLink(@Valid @RequestBody LinkUpdate request) {
        UUID id = jdbc.sql("""
                INSERT INTO friend_link(name,description,url,icon_url,sort_order,enabled)
                VALUES (:name,:description,:url,:icon,:sortOrder,:enabled) RETURNING id
                """).param("name", request.name()).param("description", request.description())
                .param("url", request.url()).param("icon", request.iconUrl()).param("sortOrder", request.sortOrder())
                .param("enabled", request.enabled()).query(UUID.class).single();
        audit("LINK_CREATE", "FRIEND_LINK", id.toString(), null);
        return link(id);
    }

    @PutMapping("/links/{id}")
    LinkRow updateLink(@PathVariable UUID id, @Valid @RequestBody LinkUpdate request) {
        int changed = jdbc.sql("""
                UPDATE friend_link SET name=:name,description=:description,url=:url,icon_url=:icon,
                  sort_order=:sortOrder,enabled=:enabled,updated_at=now() WHERE id=:id
                """).param("name", request.name()).param("description", request.description())
                .param("url", request.url()).param("icon", request.iconUrl()).param("sortOrder", request.sortOrder())
                .param("enabled", request.enabled()).param("id", id).update();
        if (changed == 0) throw new ApiException(HttpStatus.NOT_FOUND, "友情链接不存在");
        audit("LINK_UPDATE", "FRIEND_LINK", id.toString(), null);
        return link(id);
    }

    @DeleteMapping("/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLink(@PathVariable UUID id) {
        jdbc.sql("DELETE FROM friend_link WHERE id=:id").param("id", id).update();
        audit("LINK_DELETE", "FRIEND_LINK", id.toString(), null);
    }

    @GetMapping("/audit")
    PageResult<AuditRow> audit(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "50") int size) {
        long total = scalar("SELECT count(*) FROM audit_log");
        List<AuditRow> items = jdbc.sql("""
                SELECT a.id,a.action,a.target_type,a.target_id,a.reason,a.result,a.created_at,
                       u.username AS actor_username
                FROM audit_log a LEFT JOIN app_user u ON u.id=a.actor_id
                ORDER BY a.created_at DESC LIMIT :limit OFFSET :offset
                """).param("limit", PageResult.safeSize(size)).param("offset", PageResult.offset(page, size))
                .query(AuditRow.class).list();
        return new PageResult<>(items, total, Math.max(page, 1), PageResult.safeSize(size));
    }

    private void audit(String action, String targetType, String targetId, String reason) {
        jdbc.sql("""
                INSERT INTO audit_log(actor_id,action,target_type,target_id,reason)
                VALUES (:actor,:action,:targetType,:targetId,:reason)
                """).param("actor", CurrentUser.required().id()).param("action", action)
                .param("targetType", targetType).param("targetId", targetId).param("reason", reason).update();
    }

    private long scalar(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    private UserRow user(UUID id) {
        return jdbc.sql("""
                SELECT id,username,email,display_name,role,status,email_verified,created_at,updated_at
                FROM app_user WHERE id=:id
                """).param("id", id).query(UserRow.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private LinkRow link(UUID id) {
        return jdbc.sql("""
                SELECT id,name,description,url,icon_url,sort_order,enabled,created_at,updated_at
                FROM friend_link WHERE id=:id
                """).param("id", id).query(LinkRow.class).single();
    }

    record UserUpdate(@NotBlank String role, @NotBlank String status, @Size(max = 500) String reason) {}
    record Moderation(@NotBlank String status, @Size(max = 500) String reason) {}
    record CommentModeration(@NotBlank String status, @Size(max = 500) String reason) {}
    record ConfigUpdate(String value, @Size(max = 300) String description, boolean publicValue) {}
    record LinkUpdate(@NotBlank @Size(max = 100) String name, @Size(max = 300) String description,
                      @NotBlank @Size(max = 500) String url, @Size(max = 500) String iconUrl,
                      int sortOrder, boolean enabled) {}
    public record UserRow(UUID id, String username, String email, String displayName, String role,
                          String status, boolean emailVerified, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record ContentRow(UUID id, String title, String slug, String type, String status,
                             String visibility, String authorUsername, OffsetDateTime publishedAt,
                             OffsetDateTime updatedAt) {}
    public record ConfigRow(String configKey, String configValue, String description, boolean publicValue,
                            OffsetDateTime updatedAt) {}
    public record LinkRow(UUID id, String name, String description, String url, String iconUrl, int sortOrder,
                          boolean enabled, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record AuditRow(UUID id, String action, String targetType, String targetId, String reason,
                           String result, OffsetDateTime createdAt, String actorUsername) {}
    public record CommentRow(UUID id, UUID contentId, String body, String status, OffsetDateTime createdAt,
                             String username, String contentTitle) {}
}
