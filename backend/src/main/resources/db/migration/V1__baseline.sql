CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(40) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    bio VARCHAR(500),
    avatar_media_id UUID,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE INDEX idx_app_user_username_lower ON app_user (lower(username));
CREATE INDEX idx_app_user_email_lower ON app_user (lower(email));

CREATE TABLE account_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_account_token_user_purpose ON account_token(user_id, purpose);

CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(300),
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_category_owner_name ON category(COALESCE(owner_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name));

CREATE TABLE tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_tag_owner_name ON tag(COALESCE(owner_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name));

CREATE TABLE media_resource (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256 CHAR(64) NOT NULL,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('AVATAR', 'COVER', 'CONTENT')),
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_media_owner ON media_resource(owner_id, created_at DESC) WHERE deleted_at IS NULL;

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_avatar FOREIGN KEY (avatar_media_id) REFERENCES media_resource(id) ON DELETE SET NULL;

CREATE TABLE content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES app_user(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('BLOG', 'NOTE')),
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    summary VARCHAR(500),
    body_markdown TEXT NOT NULL DEFAULT '',
    cover_media_id UUID REFERENCES media_resource(id) ON DELETE SET NULL,
    category_id UUID REFERENCES category(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE', 'DELETED')),
    visibility VARCHAR(30) NOT NULL CHECK (visibility IN ('PUBLIC', 'AUTHENTICATED', 'RESTRICTED', 'PRIVATE')),
    comments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    published_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(summary, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(body_markdown, '')), 'C')
    ) STORED,
    CONSTRAINT uk_content_author_slug UNIQUE(author_id, slug)
);
CREATE INDEX idx_content_author_status ON content(author_id, status, updated_at DESC);
CREATE INDEX idx_content_publication ON content(status, visibility, published_at DESC);
CREATE INDEX idx_content_category ON content(category_id, status);
CREATE INDEX idx_content_search_vector ON content USING GIN(search_vector);
CREATE INDEX idx_content_title_trgm ON content USING GIN(title gin_trgm_ops);
CREATE INDEX idx_content_summary_trgm ON content USING GIN(summary gin_trgm_ops);

CREATE TABLE content_grant (
    content_id UUID NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(content_id, user_id)
);
CREATE INDEX idx_content_grant_user ON content_grant(user_id, content_id);

CREATE TABLE content_tag (
    content_id UUID NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY(content_id, tag_id)
);
CREATE INDEX idx_content_tag_tag ON content_tag(tag_id, content_id);

CREATE TABLE content_like (
    content_id UUID NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(content_id, user_id)
);

CREATE TABLE comment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id UUID NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    root_id UUID REFERENCES comment(id),
    parent_id UUID REFERENCES comment(id),
    reply_to_user_id UUID REFERENCES app_user(id),
    body VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'DELETED', 'HIDDEN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comment_content_created ON comment(content_id, created_at);
CREATE INDEX idx_comment_root ON comment(root_id, created_at);

CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('LIKE', 'COMMENT', 'REPLY', 'SYSTEM')),
    content_id UUID REFERENCES content(id) ON DELETE CASCADE,
    comment_id UUID REFERENCES comment(id) ON DELETE CASCADE,
    message VARCHAR(300) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_recipient ON notification(recipient_id, read_at, created_at DESC);

CREATE TABLE friend_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    url VARCHAR(500) NOT NULL,
    icon_url VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE site_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    description VARCHAR(300),
    public_value BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100),
    reason VARCHAR(500),
    result VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_log(created_at DESC);

INSERT INTO site_config(config_key, config_value, description, public_value) VALUES
    ('site.name', 'Personal Blog', '站点名称', TRUE),
    ('site.subtitle', '记录、分享与连接', '站点副标题', TRUE),
    ('site.about', '欢迎来到 Personal Blog。', '关于页面内容', TRUE),
    ('registration.enabled', 'true', '是否开放注册', FALSE)
ON CONFLICT (config_key) DO NOTHING;

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);

