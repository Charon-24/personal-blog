package com.charon.personalblog;

import com.charon.personalblog.security.BlogPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApplicationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("personal_blog_test")
            .withUsername("blog_test")
            .withPassword("test-only-password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder passwords;

    @Test
    void flywayAndPublicSiteEndpointAreReady() throws Exception {
        mvc.perform(get("/api/v1/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['site.name']").value("Personal Blog"));
        mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void modifyingRequestRequiresCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username":"writer1","email":"writer1@example.test","password":"strong-password"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void restrictedContentIsHiddenAndVisibleOnlyToGrantee() throws Exception {
        UUID owner = user("owner_" + UUID.randomUUID().toString().substring(0, 6));
        UUID guest = user("guest_" + UUID.randomUUID().toString().substring(0, 6));
        UUID outsider = user("other_" + UUID.randomUUID().toString().substring(0, 6));
        UUID content = jdbc.sql("""
                INSERT INTO content(author_id,type,title,slug,body_markdown,status,visibility,published_at)
                VALUES (:owner,'BLOG','Restricted','restricted','secret','PUBLISHED','RESTRICTED',now())
                RETURNING id
                """).param("owner", owner).query(UUID.class).single();
        jdbc.sql("INSERT INTO content_grant(content_id,user_id) VALUES (:content,:guest)")
                .param("content", content).param("guest", guest).update();

        mvc.perform(get("/api/v1/contents/{username}/restricted", username(owner)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/contents/{username}/restricted", username(owner))
                        .with(authentication(auth(principal(outsider)))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/contents/{username}/restricted", username(owner))
                        .with(authentication(auth(principal(guest)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMarkdown").value("secret"));
    }

    @Test
    void ownerCanCreateAndPublishContent() throws Exception {
        UUID owner = user("author_" + UUID.randomUUID().toString().substring(0, 6));
        BlogPrincipal principal = principal(owner);
        String response = mvc.perform(post("/api/v1/studio/contents")
                        .with(authentication(auth(principal))).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"type":"BLOG","title":"Hello","summary":"intro","bodyMarkdown":"# Hello",
                                 "visibility":"PUBLIC","commentsEnabled":true,"pinned":false,
                                 "tags":["Java"],"grantedUsernames":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String id = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asText();
        mvc.perform(post("/api/v1/studio/contents/{id}/publish", id)
                        .with(authentication(auth(principal))).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private UUID user(String username) {
        return jdbc.sql("""
                INSERT INTO app_user(username,email,password_hash,display_name,status,email_verified)
                VALUES (:username,:email,:password,:username,'ACTIVE',true) RETURNING id
                """).param("username", username).param("email", username + "@example.test")
                .param("password", passwords.encode("strong-password")).query(UUID.class).single();
    }

    private String username(UUID id) {
        return jdbc.sql("SELECT username FROM app_user WHERE id=:id").param("id", id).query(String.class).single();
    }

    private BlogPrincipal principal(UUID id) {
        return new BlogPrincipal(id, username(id), passwords.encode("strong-password"), "USER", true);
    }

    private UsernamePasswordAuthenticationToken auth(BlogPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.password(), principal.getAuthorities());
    }
}
