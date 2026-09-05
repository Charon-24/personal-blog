package com.charon.personalblog.content;

import com.charon.personalblog.common.ApiException;
import com.charon.personalblog.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TaxonomyController {
    private final JdbcClient jdbc;

    public TaxonomyController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/categories")
    List<CategoryView> categories(@RequestParam(required = false) String author) {
        String filter = author == null ? "c.owner_id IS NULL" : "lower(u.username)=lower(:author)";
        JdbcClient.StatementSpec spec = jdbc.sql("""
                SELECT c.id,c.name,c.description,c.sort_order,c.enabled,
                       (SELECT count(*) FROM content x WHERE x.category_id=c.id AND x.status='PUBLISHED'
                         AND x.visibility='PUBLIC' AND x.deleted_at IS NULL) AS content_count
                FROM category c LEFT JOIN app_user u ON u.id=c.owner_id WHERE
                """ + filter + " AND c.enabled=true ORDER BY c.sort_order,c.name");
        if (author != null) spec = spec.param("author", author);
        return spec.query(CategoryView.class).list();
    }

    @GetMapping("/tags")
    List<TagView> tags(@RequestParam(required = false) String author) {
        String filter = author == null ? "t.owner_id IS NULL" : "lower(u.username)=lower(:author)";
        JdbcClient.StatementSpec spec = jdbc.sql("""
                SELECT t.id,t.name,t.description,
                       (SELECT count(*) FROM content_tag ct JOIN content x ON x.id=ct.content_id
                         WHERE ct.tag_id=t.id AND x.status='PUBLISHED' AND x.visibility='PUBLIC'
                         AND x.deleted_at IS NULL) AS content_count
                FROM tag t LEFT JOIN app_user u ON u.id=t.owner_id WHERE
                """ + filter + " ORDER BY t.name");
        if (author != null) spec = spec.param("author", author);
        return spec.query(TagView.class).list();
    }

    @GetMapping("/studio/categories")
    List<CategoryView> ownCategories() {
        return jdbc.sql("""
                SELECT c.id,c.name,c.description,c.sort_order,c.enabled,
                       (SELECT count(*) FROM content x WHERE x.category_id=c.id AND x.deleted_at IS NULL) AS content_count
                FROM category c WHERE c.owner_id=:owner ORDER BY c.sort_order,c.name
                """).param("owner", CurrentUser.required().id()).query(CategoryView.class).list();
    }

    @PostMapping("/studio/categories")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryView createCategory(@Valid @RequestBody CategoryRequest request) {
        UUID owner = CurrentUser.required().id();
        UUID id = jdbc.sql("""
                INSERT INTO category(owner_id,name,description,sort_order,enabled)
                VALUES (:owner,:name,:description,:sortOrder,:enabled) RETURNING id
                """).param("owner", owner).param("name", request.name()).param("description", request.description())
                .param("sortOrder", request.sortOrder()).param("enabled", request.enabled()).query(UUID.class).single();
        return category(id, owner);
    }

    @PutMapping("/studio/categories/{id}")
    CategoryView updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        UUID owner = CurrentUser.required().id();
        int updated = jdbc.sql("""
                UPDATE category SET name=:name,description=:description,sort_order=:sortOrder,
                  enabled=:enabled,updated_at=now() WHERE id=:id AND owner_id=:owner
                """).param("name", request.name()).param("description", request.description())
                .param("sortOrder", request.sortOrder()).param("enabled", request.enabled())
                .param("id", id).param("owner", owner).update();
        if (updated == 0) throw new ApiException(HttpStatus.NOT_FOUND, "分类不存在");
        return category(id, owner);
    }

    @DeleteMapping("/studio/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable UUID id) {
        int deleted = jdbc.sql("DELETE FROM category WHERE id=:id AND owner_id=:owner")
                .param("id", id).param("owner", CurrentUser.required().id()).update();
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "分类不存在");
    }

    @GetMapping("/studio/tags")
    List<TagView> ownTags() {
        return jdbc.sql("""
                SELECT t.id,t.name,t.description,
                       (SELECT count(*) FROM content_tag ct WHERE ct.tag_id=t.id) AS content_count
                FROM tag t WHERE t.owner_id=:owner ORDER BY t.name
                """).param("owner", CurrentUser.required().id()).query(TagView.class).list();
    }

    @DeleteMapping("/studio/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTag(@PathVariable UUID id) {
        int deleted = jdbc.sql("DELETE FROM tag WHERE id=:id AND owner_id=:owner")
                .param("id", id).param("owner", CurrentUser.required().id()).update();
        if (deleted == 0) throw new ApiException(HttpStatus.NOT_FOUND, "标签不存在");
    }

    private CategoryView category(UUID id, UUID owner) {
        return jdbc.sql("""
                SELECT c.id,c.name,c.description,c.sort_order,c.enabled,
                       (SELECT count(*) FROM content x WHERE x.category_id=c.id AND x.deleted_at IS NULL) AS content_count
                FROM category c WHERE c.id=:id AND c.owner_id=:owner
                """).param("id", id).param("owner", owner).query(CategoryView.class).single();
    }

    record CategoryRequest(@NotBlank @Size(max = 80) String name, @Size(max = 300) String description,
                           int sortOrder, boolean enabled) {}
    public record CategoryView(UUID id, String name, String description, int sortOrder,
                               boolean enabled, long contentCount) {}
    public record TagView(UUID id, String name, String description, long contentCount) {}
}
