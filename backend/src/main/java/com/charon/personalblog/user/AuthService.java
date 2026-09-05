package com.charon.personalblog.user;

import com.charon.personalblog.common.ApiException;
import com.charon.personalblog.security.BlogPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.charon.personalblog.user.mapper.AppUserEntity;
import com.charon.personalblog.user.mapper.AppUserMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService implements ApplicationRunner {
    private final JdbcClient jdbc;
    private final PasswordEncoder passwords;
    private final JavaMailSender mail;
    private final AppUserMapper users;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.public-base-url:http://localhost:3000}")
    private String publicBaseUrl;
    @Value("${app.bootstrap-admin.username:}")
    private String adminUsername;
    @Value("${app.bootstrap-admin.email:}")
    private String adminEmail;
    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;

    public AuthService(JdbcClient jdbc, PasswordEncoder passwords, JavaMailSender mail, AppUserMapper users) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.mail = mail;
        this.users = users;
    }

    @Transactional
    public void register(String username, String email, String password) {
        long conflicts = jdbc.sql("""
                SELECT count(*) FROM app_user
                WHERE lower(username)=lower(:username) OR lower(email)=lower(:email)
                """).param("username", username).param("email", email).query(Long.class).single();
        if (conflicts > 0) throw new ApiException(HttpStatus.CONFLICT, "用户名或邮箱已被使用");

        UUID id = jdbc.sql("""
                INSERT INTO app_user(username,email,password_hash,display_name)
                VALUES (:username,:email,:password,:username) RETURNING id
                """).param("username", username).param("email", email)
                .param("password", passwords.encode(password)).query(UUID.class).single();
        sendToken(id, email, "EMAIL_VERIFICATION", "验证邮箱", "/verify-email?token=");
    }

    @Transactional
    public void verifyEmail(String token) {
        UUID userId = consumeToken(token, "EMAIL_VERIFICATION");
        jdbc.sql("UPDATE app_user SET email_verified=true,status='ACTIVE',updated_at=now() WHERE id=:id")
                .param("id", userId).update();
    }

    public void requestPasswordReset(String email) {
        jdbc.sql("SELECT id,email FROM app_user WHERE lower(email)=lower(:email) AND status <> 'DISABLED'")
                .param("email", email)
                .query((rs, row) -> new Object[]{rs.getObject("id", UUID.class), rs.getString("email")})
                .optional()
                .ifPresent(values -> sendToken((UUID) values[0], (String) values[1],
                        "PASSWORD_RESET", "重置密码", "/reset-password?token="));
    }

    @Transactional
    public void resetPassword(String token, String password) {
        UUID userId = consumeToken(token, "PASSWORD_RESET");
        jdbc.sql("UPDATE app_user SET password_hash=:hash,updated_at=now() WHERE id=:id")
                .param("hash", passwords.encode(password)).param("id", userId).update();
    }

    @Transactional
    public void changePassword(UUID id, String oldPassword, String newPassword) {
        String hash = jdbc.sql("SELECT password_hash FROM app_user WHERE id=:id")
                .param("id", id).query(String.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (!passwords.matches(oldPassword, hash)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前密码错误");
        }
        jdbc.sql("UPDATE app_user SET password_hash=:hash,updated_at=now() WHERE id=:id")
                .param("hash", passwords.encode(newPassword)).param("id", id).update();
    }

    public UserView me(BlogPrincipal principal) {
        return toView(users.selectById(principal.id()));
    }

    @Transactional
    public UserView updateProfile(UUID id, String displayName, String bio, UUID avatarMediaId) {
        if (avatarMediaId != null) {
            long owned = jdbc.sql("SELECT count(*) FROM media_resource WHERE id=:media AND owner_id=:owner AND deleted_at IS NULL")
                    .param("media", avatarMediaId).param("owner", id).query(Long.class).single();
            if (owned == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "头像资源不存在");
        }
        users.update(null, Wrappers.<AppUserEntity>lambdaUpdate()
                .eq(AppUserEntity::getId, id)
                .set(AppUserEntity::getDisplayName, displayName)
                .set(AppUserEntity::getBio, bio)
                .set(AppUserEntity::getAvatarMediaId, avatarMediaId)
                .set(AppUserEntity::getUpdatedAt, OffsetDateTime.now()));
        return toView(users.selectById(id));
    }

    public PublicProfile profile(String username) {
        return jdbc.sql("""
                SELECT u.id,u.username,u.display_name,u.bio,u.avatar_media_id,u.created_at,
                       (SELECT count(*) FROM content c WHERE c.author_id=u.id AND c.status='PUBLISHED'
                         AND c.visibility='PUBLIC' AND c.deleted_at IS NULL) AS published_count
                FROM app_user u WHERE lower(u.username)=lower(:username) AND u.status='ACTIVE'
                """).param("username", username).query(PublicProfile.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private void sendToken(UUID userId, String email, String purpose, String subject, String path) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        jdbc.sql("""
                INSERT INTO account_token(user_id,token_hash,purpose,expires_at)
                VALUES (:user,:hash,:purpose,:expires)
                """).param("user", userId).param("hash", hash(raw)).param("purpose", purpose)
                .param("expires", OffsetDateTime.now().plusHours(2)).update();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(publicBaseUrl + path + raw);
        mail.send(message);
    }

    private UserView toView(AppUserEntity user) {
        if (user == null) throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        return new UserView(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getBio(), user.getRole(), user.getStatus(), user.isEmailVerified(),
                user.getAvatarMediaId(), user.getCreatedAt());
    }

    private UUID consumeToken(String token, String purpose) {
        UUID id = jdbc.sql("""
                UPDATE account_token SET consumed_at=now()
                WHERE token_hash=:hash AND purpose=:purpose AND consumed_at IS NULL AND expires_at>now()
                RETURNING user_id
                """).param("hash", hash(token)).param("purpose", purpose).query(UUID.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "令牌无效或已过期"));
        return id;
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) return;
        long exists = jdbc.sql("SELECT count(*) FROM app_user WHERE role='ADMIN'").query(Long.class).single();
        if (exists == 0) {
            jdbc.sql("""
                    INSERT INTO app_user(username,email,password_hash,display_name,role,status,email_verified)
                    VALUES (:username,:email,:password,:username,'ADMIN','ACTIVE',true)
                    """).param("username", adminUsername).param("email", adminEmail)
                    .param("password", passwords.encode(adminPassword)).update();
        }
    }

    public record UserView(UUID id, String username, String email, String displayName, String bio,
                           String role, String status, boolean emailVerified, UUID avatarMediaId,
                           OffsetDateTime createdAt) {}
    public record PublicProfile(UUID id, String username, String displayName, String bio,
                                UUID avatarMediaId, OffsetDateTime createdAt, long publishedCount) {}
}
