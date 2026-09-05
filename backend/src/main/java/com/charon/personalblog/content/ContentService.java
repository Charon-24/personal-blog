package com.charon.personalblog.content;

import com.charon.personalblog.common.ApiException;
import com.charon.personalblog.common.PageResult;
import com.charon.personalblog.security.BlogPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContentService {
    private static final RowMapper<ContentView> CONTENT_MAPPER = (rs, rowNum) -> {
        java.sql.Array tagArray = rs.getArray("tags");
        String[] tags = tagArray == null ? new String[0] : (String[]) tagArray.getArray();
        return new ContentView(
                rs.getObject("id", UUID.class),
                rs.getObject("author_id", UUID.class),
                rs.getString("author_username"),
                rs.getString("author_display_name"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("summary"),
                rs.getString("body_markdown"),
                rs.getObject("cover_media_id", UUID.class),
                rs.getObject("category_id", UUID.class),
                rs.getString("category_name"),
                rs.getString("status"),
                rs.getString("visibility"),
                rs.getBoolean("comments_enabled"),
                rs.getBoolean("pinned"),
                rs.getLong("view_count"),
                rs.getObject("published_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("like_count"),
                rs.getLong("comment_count"),
                tags);
    };
    private static final String VIEWABLE = """
            c.deleted_at IS NULL AND c.status='PUBLISHED' AND (
              c.visibility='PUBLIC'
              OR c.author_id=CAST(:viewer AS uuid)
              OR (CAST(:viewer AS uuid) IS NOT NULL AND c.visibility='AUTHENTICATED')
              OR (CAST(:viewer AS uuid) IS NOT NULL AND c.visibility='RESTRICTED'
                  AND EXISTS (SELECT 1 FROM content_grant g
                              WHERE g.content_id=c.id AND g.user_id=CAST(:viewer AS uuid)))
            )
            """;
    private static final String SELECT = """
            SELECT c.id,c.author_id,u.username AS author_username,u.display_name AS author_display_name,
                   c.type,c.title,c.slug,c.summary,c.body_markdown,c.cover_media_id,c.category_id,
                   cat.name AS category_name,c.status,c.visibility,c.comments_enabled,c.pinned,
                   c.view_count,c.published_at,c.created_at,c.updated_at,
                   (SELECT count(*) FROM content_like l WHERE l.content_id=c.id) AS like_count,
                   (SELECT count(*) FROM comment m WHERE m.content_id=c.id AND m.status='VISIBLE') AS comment_count,
                   COALESCE((SELECT array_agg(t.name ORDER BY t.name) FROM content_tag ct
                             JOIN tag t ON t.id=ct.tag_id WHERE ct.content_id=c.id), ARRAY[]::varchar[]) AS tags
            FROM content c JOIN app_user u ON u.id=c.author_id
            LEFT JOIN category cat ON cat.id=c.category_id
            """;

    private final JdbcClient jdbc;

    public ContentService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<ContentView> publicList(UUID viewer, String type, String author, UUID category,
                                               String tag, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE " + VIEWABLE);
        if (type != null && !type.isBlank()) where.append(" AND c.type=:type");
        if (author != null && !author.isBlank()) where.append(" AND lower(u.username)=lower(:author)");
        if (category != null) where.append(" AND c.category_id=:category");
        if (tag != null && !tag.isBlank()) where.append("""
                 AND EXISTS (SELECT 1 FROM content_tag filter_ct JOIN tag filter_t ON filter_t.id=filter_ct.tag_id
                             WHERE filter_ct.content_id=c.id AND lower(filter_t.name)=lower(:tag))
                """);
        String viewerText = viewer == null ? null : viewer.toString();
        JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) FROM content c JOIN app_user u ON u.id=c.author_id" + where)
                .param("viewer", viewerText);
        JdbcClient.StatementSpec query = jdbc.sql(SELECT + where + " ORDER BY c.pinned DESC,c.published_at DESC NULLS LAST LIMIT :limit OFFSET :offset")
                .param("viewer", viewerText).param("limit", PageResult.safeSize(size)).param("offset", PageResult.offset(page, size));
        count = optionalFilters(count, type, author, category, tag);
        query = optionalFilters(query, type, author, category, tag);
        return new PageResult<>(query.query(CONTENT_MAPPER).list(), count.query(Long.class).single(),
                Math.max(page, 1), PageResult.safeSize(size));
    }

    public ContentView detail(String username, String slug, UUID viewer) {
        ContentView content = jdbc.sql(SELECT + " WHERE lower(u.username)=lower(:username) AND c.slug=:slug AND " + VIEWABLE)
                .param("username", username).param("slug", slug)
                .param("viewer", viewer == null ? null : viewer.toString())
                .query(CONTENT_MAPPER).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "内容不存在"));
        jdbc.sql("UPDATE content SET view_count=view_count+1 WHERE id=:id").param("id", content.id()).update();
        return new ContentView(content.id(), content.authorId(), content.authorUsername(), content.authorDisplayName(),
                content.type(), content.title(), content.slug(), content.summary(), content.bodyMarkdown(),
                content.coverMediaId(), content.categoryId(), content.categoryName(), content.status(),
                content.visibility(), content.commentsEnabled(), content.pinned(), content.viewCount() + 1,
                content.publishedAt(), content.createdAt(), content.updatedAt(), content.likeCount(),
                content.commentCount(), content.tags());
    }

    public PageResult<ContentView> studioList(BlogPrincipal principal, String status, int page, int size) {
        String filter = status == null || status.isBlank() ? "" : " AND c.status=:status";
        JdbcClient.StatementSpec query = jdbc.sql(SELECT + """
                 WHERE c.author_id=:owner AND c.deleted_at IS NULL
                """ + filter + " ORDER BY c.updated_at DESC LIMIT :limit OFFSET :offset")
                .param("owner", principal.id()).param("limit", PageResult.safeSize(size))
                .param("offset", PageResult.offset(page, size));
        JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) FROM content c WHERE c.author_id=:owner AND c.deleted_at IS NULL" + filter)
                .param("owner", principal.id());
        if (!filter.isEmpty()) {
            query = query.param("status", status);
            count = count.param("status", status);
        }
        return new PageResult<>(query.query(CONTENT_MAPPER).list(), count.query(Long.class).single(),
                Math.max(page, 1), PageResult.safeSize(size));
    }

    public ContentView studioDetail(UUID id, BlogPrincipal principal) {
        return jdbc.sql(SELECT + " WHERE c.id=:id AND c.author_id=:owner AND c.deleted_at IS NULL")
                .param("id", id).param("owner", principal.id()).query(CONTENT_MAPPER).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "内容不存在"));
    }

    public List<String> grants(UUID id, BlogPrincipal principal) {
        requireOwner(id, principal.id());
        return jdbc.sql("""
                SELECT u.username FROM content_grant g JOIN app_user u ON u.id=g.user_id
                WHERE g.content_id=:content ORDER BY lower(u.username)
                """).param("content", id).query(String.class).list();
    }

    @Transactional
    public ContentView create(EditRequest request, BlogPrincipal principal) {
        validateEdit(request, principal.id());
        String slug = uniqueSlug(principal.id(), request.slug(), request.title(), null);
        UUID id = jdbc.sql("""
                INSERT INTO content(author_id,type,title,slug,summary,body_markdown,cover_media_id,category_id,
                                    visibility,comments_enabled,pinned)
                VALUES (:author,:type,:title,:slug,:summary,:body,:cover,:category,:visibility,:comments,:pinned)
                RETURNING id
                """).param("author", principal.id()).param("type", request.type()).param("title", request.title())
                .param("slug", slug).param("summary", request.summary()).param("body", request.bodyMarkdown())
                .param("cover", request.coverMediaId()).param("category", request.categoryId())
                .param("visibility", request.visibility()).param("comments", request.commentsEnabled())
                .param("pinned", request.pinned()).query(UUID.class).single();
        replaceTags(id, principal.id(), request.tags());
        replaceGrants(id, principal.id(), request.grantedUsernames(), request.visibility());
        return studioDetail(id, principal);
    }

    @Transactional
    public ContentView update(UUID id, EditRequest request, BlogPrincipal principal) {
        requireOwner(id, principal.id());
        validateEdit(request, principal.id());
        String slug = uniqueSlug(principal.id(), request.slug(), request.title(), id);
        jdbc.sql("""
                UPDATE content SET type=:type,title=:title,slug=:slug,summary=:summary,body_markdown=:body,
                  cover_media_id=:cover,category_id=:category,visibility=:visibility,
                  comments_enabled=:comments,pinned=:pinned,updated_at=now()
                WHERE id=:id AND author_id=:owner AND deleted_at IS NULL
                """).param("type", request.type()).param("title", request.title()).param("slug", slug)
                .param("summary", request.summary()).param("body", request.bodyMarkdown())
                .param("cover", request.coverMediaId()).param("category", request.categoryId())
                .param("visibility", request.visibility()).param("comments", request.commentsEnabled())
                .param("pinned", request.pinned()).param("id", id).param("owner", principal.id()).update();
        replaceTags(id, principal.id(), request.tags());
        replaceGrants(id, principal.id(), request.grantedUsernames(), request.visibility());
        return studioDetail(id, principal);
    }

    @Transactional
    public void transition(UUID id, String action, BlogPrincipal principal) {
        requireOwner(id, principal.id());
        String sql = switch (action) {
            case "publish" -> "UPDATE content SET status='PUBLISHED',published_at=COALESCE(published_at,now()),updated_at=now() WHERE id=:id";
            case "offline" -> "UPDATE content SET status='OFFLINE',updated_at=now() WHERE id=:id";
            case "draft" -> "UPDATE content SET status='DRAFT',updated_at=now() WHERE id=:id";
            case "delete" -> "UPDATE content SET status='DELETED',deleted_at=now(),updated_at=now() WHERE id=:id";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "不支持的状态操作");
        };
        jdbc.sql(sql).param("id", id).update();
    }

    public PageResult<ContentView> search(String q, UUID viewer, int page, int size) {
        if (q == null || q.isBlank()) return new PageResult<>(List.of(), 0, Math.max(page, 1), PageResult.safeSize(size));
        String where = " WHERE " + VIEWABLE + """
                 AND (c.search_vector @@ websearch_to_tsquery('english',:query)
                      OR c.title ILIKE '%' || :query || '%'
                      OR c.summary ILIKE '%' || :query || '%'
                      OR similarity(c.title,:query)>0.15)
                """;
        String viewerText = viewer == null ? null : viewer.toString();
        long total = jdbc.sql("SELECT count(*) FROM content c JOIN app_user u ON u.id=c.author_id" + where)
                .param("viewer", viewerText).param("query", q).query(Long.class).single();
        List<ContentView> items = jdbc.sql(SELECT + where + """
                 ORDER BY GREATEST(ts_rank(c.search_vector,websearch_to_tsquery('english',:query)),
                                   similarity(c.title,:query)) DESC,c.published_at DESC
                 LIMIT :limit OFFSET :offset
                """).param("viewer", viewerText).param("query", q).param("limit", PageResult.safeSize(size))
                .param("offset", PageResult.offset(page, size)).query(CONTENT_MAPPER).list();
        return new PageResult<>(items, total, Math.max(page, 1), PageResult.safeSize(size));
    }

    public List<ArchiveItem> archive(UUID viewer) {
        return jdbc.sql("""
                SELECT EXTRACT(YEAR FROM c.published_at)::int AS year,
                       EXTRACT(MONTH FROM c.published_at)::int AS month,count(*) AS count
                FROM content c WHERE
                """ + VIEWABLE + " GROUP BY year,month ORDER BY year DESC,month DESC")
                .param("viewer", viewer == null ? null : viewer.toString()).query(ArchiveItem.class).list();
    }

    public boolean canView(UUID contentId, UUID viewer) {
        return jdbc.sql("SELECT count(*) FROM content c WHERE c.id=:id AND " + VIEWABLE)
                .param("id", contentId).param("viewer", viewer == null ? null : viewer.toString())
                .query(Long.class).single() > 0;
    }

    private JdbcClient.StatementSpec optionalFilters(JdbcClient.StatementSpec spec, String type, String author,
                                                       UUID category, String tag) {
        if (type != null && !type.isBlank()) spec = spec.param("type", type);
        if (author != null && !author.isBlank()) spec = spec.param("author", author);
        if (category != null) spec = spec.param("category", category);
        if (tag != null && !tag.isBlank()) spec = spec.param("tag", tag);
        return spec;
    }

    private void validateEdit(EditRequest request, UUID owner) {
        if (!List.of("BLOG", "NOTE").contains(request.type()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容类型不合法");
        if (!List.of("PUBLIC", "AUTHENTICATED", "RESTRICTED", "PRIVATE").contains(request.visibility()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "可见性不合法");
        if (request.categoryId() != null) {
            long count = jdbc.sql("SELECT count(*) FROM category WHERE id=:id AND owner_id=:owner")
                    .param("id", request.categoryId()).param("owner", owner).query(Long.class).single();
            if (count == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "分类不存在");
        }
        if (request.coverMediaId() != null) {
            long count = jdbc.sql("SELECT count(*) FROM media_resource WHERE id=:id AND owner_id=:owner AND deleted_at IS NULL")
                    .param("id", request.coverMediaId()).param("owner", owner).query(Long.class).single();
            if (count == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "封面资源不存在");
        }
    }

    private void requireOwner(UUID contentId, UUID owner) {
        long count = jdbc.sql("SELECT count(*) FROM content WHERE id=:id AND author_id=:owner AND deleted_at IS NULL")
                .param("id", contentId).param("owner", owner).query(Long.class).single();
        if (count == 0) throw new ApiException(HttpStatus.NOT_FOUND, "内容不存在");
    }

    private String uniqueSlug(UUID owner, String preferred, String title, UUID current) {
        String source = preferred == null || preferred.isBlank() ? title : preferred;
        String base = Normalizer.normalize(source, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "post-" + UUID.randomUUID().toString().substring(0, 8);
        String candidate = base;
        int suffix = 2;
        while (slugExists(owner, candidate, current)) candidate = base + "-" + suffix++;
        return candidate;
    }

    private boolean slugExists(UUID owner, String slug, UUID current) {
        String extra = current == null ? "" : " AND id<>:current";
        JdbcClient.StatementSpec spec = jdbc.sql("SELECT count(*) FROM content WHERE author_id=:owner AND slug=:slug" + extra)
                .param("owner", owner).param("slug", slug);
        if (current != null) spec = spec.param("current", current);
        return spec.query(Long.class).single() > 0;
    }

    private void replaceTags(UUID contentId, UUID owner, List<String> names) {
        jdbc.sql("DELETE FROM content_tag WHERE content_id=:content").param("content", contentId).update();
        if (names == null) return;
        names.stream().filter(name -> name != null && !name.isBlank()).map(String::trim).distinct().limit(10)
                .forEach(name -> {
                    UUID tagId = jdbc.sql("""
                            INSERT INTO tag(owner_id,name) VALUES (:owner,:name)
                            ON CONFLICT DO NOTHING RETURNING id
                            """).param("owner", owner).param("name", name).query(UUID.class).optional()
                            .orElseGet(() -> jdbc.sql("SELECT id FROM tag WHERE owner_id=:owner AND lower(name)=lower(:name)")
                                    .param("owner", owner).param("name", name).query(UUID.class).single());
                    jdbc.sql("INSERT INTO content_tag(content_id,tag_id) VALUES (:content,:tag) ON CONFLICT DO NOTHING")
                            .param("content", contentId).param("tag", tagId).update();
                });
    }

    private void replaceGrants(UUID contentId, UUID owner, List<String> usernames, String visibility) {
        jdbc.sql("DELETE FROM content_grant WHERE content_id=:content").param("content", contentId).update();
        if (!"RESTRICTED".equals(visibility) || usernames == null) return;
        usernames.stream().filter(name -> name != null && !name.isBlank()).map(String::trim).distinct().limit(100)
                .forEach(username -> {
                    UUID userId = jdbc.sql("SELECT id FROM app_user WHERE lower(username)=lower(:name) AND status='ACTIVE'")
                            .param("name", username).query(UUID.class).optional()
                            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "指定用户不存在: " + username));
                    if (!userId.equals(owner)) {
                        jdbc.sql("INSERT INTO content_grant(content_id,user_id) VALUES (:content,:user) ON CONFLICT DO NOTHING")
                                .param("content", contentId).param("user", userId).update();
                    }
                });
    }

    public record EditRequest(String type, String title, String slug, String summary, String bodyMarkdown,
                              UUID coverMediaId, UUID categoryId, String visibility, boolean commentsEnabled,
                              boolean pinned, List<String> tags, List<String> grantedUsernames) {}
    public record ContentView(UUID id, UUID authorId, String authorUsername, String authorDisplayName,
                              String type, String title, String slug, String summary, String bodyMarkdown,
                              UUID coverMediaId, UUID categoryId, String categoryName, String status,
                              String visibility, boolean commentsEnabled, boolean pinned, long viewCount,
                              OffsetDateTime publishedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                              long likeCount, long commentCount, String[] tags) {}
    public record ArchiveItem(int year, int month, long count) {}
}
