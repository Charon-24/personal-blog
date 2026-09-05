package com.charon.personalblog.user;

import com.charon.personalblog.security.BlogPrincipal;
import com.charon.personalblog.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService service;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;

    public AuthController(AuthService service, AuthenticationManager authenticationManager,
                          SecurityContextRepository contexts) {
        this.service = service;
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
    }

    @GetMapping("/auth/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegisterRequest request) {
        service.register(request.username(), request.email(), request.password());
    }

    @PostMapping("/auth/verify-email")
    void verify(@Valid @RequestBody TokenRequest request) {
        service.verifyEmail(request.token());
    }

    @PostMapping("/auth/login")
    AuthService.UserView login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                               HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.identity(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, servletRequest, response);
        return service.me((BlogPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/auth/me")
    AuthService.UserView me() {
        return service.me(CurrentUser.required());
    }

    @PostMapping("/auth/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void forgot(@Valid @RequestBody ForgotRequest request) {
        service.requestPasswordReset(request.email());
    }

    @PostMapping("/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset(@Valid @RequestBody ResetRequest request) {
        service.resetPassword(request.token(), request.password());
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(CurrentUser.required().id(), request.oldPassword(), request.newPassword());
    }

    @PatchMapping("/me/profile")
    AuthService.UserView updateProfile(@Valid @RequestBody ProfileRequest request) {
        return service.updateProfile(CurrentUser.required().id(), request.displayName(), request.bio(), request.avatarMediaId());
    }

    @GetMapping("/users/{username}")
    AuthService.PublicProfile profile(@PathVariable String username) {
        return service.profile(username);
    }

    record RegisterRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 10, max = 128) String password) {}
    record LoginRequest(@NotBlank String identity, @NotBlank String password) {}
    record TokenRequest(@NotBlank String token) {}
    record ForgotRequest(@NotBlank @Email String email) {}
    record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 10, max = 128) String password) {}
    record ChangePasswordRequest(@NotBlank String oldPassword,
                                 @NotBlank @Size(min = 10, max = 128) String newPassword) {}
    record ProfileRequest(@NotBlank @Size(max = 80) String displayName,
                          @Size(max = 500) String bio, UUID avatarMediaId) {}
}
