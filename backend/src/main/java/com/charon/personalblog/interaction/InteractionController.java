package com.charon.personalblog.interaction;

import com.charon.personalblog.common.ApiException;
import com.charon.personalblog.content.ContentService;
import com.charon.personalblog.security.BlogPrincipal;
import com.charon.personalblog.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class InteractionController {
    private final JdbcClient jdbc;
    private final ContentService contents;

    public InteractionController(JdbcClient jdbc, ContentService contents) {
        this.jdbc = jdbc;
        this.contents = contents;
    }

    @PostMapping("/contents/{contentId}/like")
    @Transactional
    Map<String, Object> toggleLike(@PathVariable UUID contentId) {
        BlogPrincipal actor = CurrentUser.required();
        requireView(contentId, actor.id());
        int deleted = jdbc.sql("DELETE FROM content_like WHERE content_id=:content AND user_id=:user")
                .param("content", contentId).param("user", actor.id()).update();
        boolean liked = deleted == 0;
        if (liked) {
            jdbc.sql("INSERT INTO content_like(content_id,user_id) VALUES (:content,:user)")
                    .param("content", contentId).param("user", actor.id()).update();
            notifyOwner(contentId, actor.id(), "LIKE", null, "赞了你的内容");
        }
        long count = jdbc.sql("SELECT count(*) FROM content_like WHERE content_id=:content")
                .param("content", contentId).query(Long.class).single();
        return Map.of("liked", liked, "likeCount", count);
    }

    @GetMapping("/comments")
    List<CommentView> comments(@RequestParam UUID contentId) {
        requireView(contentId, CurrentUser.idOrNull());
        return jdbc.sql("""
                SELECT c.id,c.content_id,c.author_id,u.username,u.display_name,c.root_id,c.parent_id,
                       c.reply_to_user_id,CASE WHEN c.status='DELETED' THEN NULL ELSE c.body END AS body,
                       c.status,c.created_at,c.updated_at
                FROM comment c JOIN app_user u ON u.id=c.author_id
                WHERE c.content_id=:content AND c.status<>'HIDDEN' ORDER BY c.created_at
                """).param("content", contentId).query(CommentView.class).list();
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    CommentView comment(@Valid @RequestBody CommentRequest request) {
        BlogPrincipal actor = CurrentUser.required();
        requireView(request.contentId(), actor.id());
        boolean enabled = jdbc.sql("SELECT comments_enabled FROM content WHERE id=:id")
                .param("id", request.contentId()).query(Boolean.class).single();
        if (!enabled) throw new ApiException(HttpStatus.CONFLICT, "该内容已关闭评论");
        Parent parent = null;
        if (request.parentId() != null) {
            parent = jdbc.sql("SELECT id,content_id,COALESCE(root_id,id) AS root_id,author_id FROM comment WHERE id=:id AND status='VISIBLE'")
                    .param("id", request.parentId()).query(Parent.class).optional()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "回复目标不存在"));
            if (!parent.contentId().equals(request.contentId()))
                throw new ApiException(HttpStatus.BAD_REQUEST, "回复目标不属于此内容");
        }
        UUID id = jdbc.sql("""
                INSERT INTO comment(content_id,author_id,root_id,parent_id,reply_to_user_id,body)
                VALUES (:content,:author,:root,:parent,:replyTo,:body) RETURNING id
                """).param("content", request.contentId()).param("author", actor.id())
                .param("root", parent == null ? null : parent.rootId()).param("parent", request.parentId())
                .param("replyTo", parent == null ? null : parent.authorId()).param("body", request.body())
                .query(UUID.class).single();
        if (parent == null) notifyOwner(request.contentId(), actor.id(), "COMMENT", id, "评论了你的内容");
        else notifyUser(parent.authorId(), actor.id(), request.contentId(), id, "REPLY", "回复了你的评论");
        return commentById(id);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteComment(@PathVariable UUID id) {
        BlogPrincipal actor = CurrentUser.required();
        String roleClause = "ADMIN".equals(actor.role()) ? "" : " AND author_id=:actor";
        JdbcClient.StatementSpec spec = jdbc.sql("UPDATE comment SET status='DELETED',body=NULL,updated_at=now() WHERE id=:id" + roleClause)
                .param("id", id);
        if (!roleClause.isEmpty()) spec = spec.param("actor", actor.id());
        if (spec.update() == 0) throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
    }

    @GetMapping("/notifications")
    List<NotificationView> notifications() {
        return jdbc.sql("""
                SELECT n.id,n.type,n.message,n.content_id,n.comment_id,n.read_at,n.created_at,
                       u.username AS actor_username,u.display_name AS actor_display_name
                FROM notification n LEFT JOIN app_user u ON u.id=n.actor_id
                WHERE n.recipient_id=:recipient ORDER BY n.created_at DESC LIMIT 100
                """).param("recipient", CurrentUser.required().id()).query(NotificationView.class).list();
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void read(@PathVariable UUID id) {
        jdbc.sql("UPDATE notification SET read_at=COALESCE(read_at,now()) WHERE id=:id AND recipient_id=:recipient")
                .param("id", id).param("recipient", CurrentUser.required().id()).update();
    }

    @PostMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void readAll() {
        jdbc.sql("UPDATE notification SET read_at=now() WHERE recipient_id=:recipient AND read_at IS NULL")
                .param("recipient", CurrentUser.required().id()).update();
    }

    private CommentView commentById(UUID id) {
        return jdbc.sql("""
                SELECT c.id,c.content_id,c.author_id,u.username,u.display_name,c.root_id,c.parent_id,
                       c.reply_to_user_id,c.body,c.status,c.created_at,c.updated_at
                FROM comment c JOIN app_user u ON u.id=c.author_id WHERE c.id=:id
                """).param("id", id).query(CommentView.class).single();
    }

    private void requireView(UUID contentId, UUID viewer) {
        if (!contents.canView(contentId, viewer))
            throw new ApiException(HttpStatus.NOT_FOUND, "内容不存在");
    }

    private void notifyOwner(UUID contentId, UUID actor, String type, UUID comment, String message) {
        UUID owner = jdbc.sql("SELECT author_id FROM content WHERE id=:id").param("id", contentId)
                .query(UUID.class).single();
        notifyUser(owner, actor, contentId, comment, type, message);
    }

    private void notifyUser(UUID recipient, UUID actor, UUID content, UUID comment, String type, String message) {
        if (recipient.equals(actor)) return;
        jdbc.sql("""
                INSERT INTO notification(recipient_id,actor_id,type,content_id,comment_id,message)
                VALUES (:recipient,:actor,:type,:content,:comment,:message)
                """).param("recipient", recipient).param("actor", actor).param("type", type)
                .param("content", content).param("comment", comment).param("message", message).update();
    }

    record CommentRequest(UUID contentId, UUID parentId, @NotBlank @Size(max = 2000) String body) {}
    record Parent(UUID id, UUID contentId, UUID rootId, UUID authorId) {}
    public record CommentView(UUID id, UUID contentId, UUID authorId, String username, String displayName,
                              UUID rootId, UUID parentId, UUID replyToUserId, String body, String status,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record NotificationView(UUID id, String type, String message, UUID contentId, UUID commentId,
                                   OffsetDateTime readAt, OffsetDateTime createdAt, String actorUsername,
                                   String actorDisplayName) {}
}
