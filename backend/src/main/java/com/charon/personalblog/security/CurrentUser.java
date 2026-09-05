package com.charon.personalblog.security;

import com.charon.personalblog.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<BlogPrincipal> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof BlogPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static BlogPrincipal required() {
        return optional().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    public static UUID idOrNull() {
        return optional().map(BlogPrincipal::id).orElse(null);
    }
}
