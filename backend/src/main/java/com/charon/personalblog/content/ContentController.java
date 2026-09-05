package com.charon.personalblog.content;

import com.charon.personalblog.common.PageResult;
import com.charon.personalblog.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ContentController {
    private final ContentService service;

    public ContentController(ContentService service) {
        this.service = service;
    }

    @GetMapping("/contents")
    PageResult<ContentService.ContentView> list(@RequestParam(required = false) String type,
                                                @RequestParam(required = false) String author,
                                                @RequestParam(required = false) UUID category,
                                                @RequestParam(required = false) String tag,
                                                @RequestParam(defaultValue = "false") boolean publicOnly,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "12") int size) {
        return service.publicList(publicOnly ? null : CurrentUser.idOrNull(), type, author, category, tag, page, size);
    }

    @GetMapping("/contents/{username}/{slug}")
    ContentService.ContentView detail(@PathVariable String username, @PathVariable String slug) {
        return service.detail(username, slug, CurrentUser.idOrNull());
    }

    @GetMapping("/search")
    PageResult<ContentService.ContentView> search(@RequestParam String q,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "12") int size) {
        return service.search(q, CurrentUser.idOrNull(), page, size);
    }

    @GetMapping("/archive")
    List<ContentService.ArchiveItem> archive() {
        return service.archive(CurrentUser.idOrNull());
    }

    @GetMapping("/studio/contents")
    PageResult<ContentService.ContentView> studio(@RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.studioList(CurrentUser.required(), status, page, size);
    }

    @GetMapping("/studio/contents/{id}")
    ContentService.ContentView studioDetail(@PathVariable UUID id) {
        return service.studioDetail(id, CurrentUser.required());
    }

    @GetMapping("/studio/contents/{id}/grants")
    List<String> grants(@PathVariable UUID id) {
        return service.grants(id, CurrentUser.required());
    }

    @PostMapping("/studio/contents")
    @ResponseStatus(HttpStatus.CREATED)
    ContentService.ContentView create(@Valid @RequestBody EditRequest request) {
        return service.create(request.toService(), CurrentUser.required());
    }

    @PutMapping("/studio/contents/{id}")
    ContentService.ContentView update(@PathVariable UUID id, @Valid @RequestBody EditRequest request) {
        return service.update(id, request.toService(), CurrentUser.required());
    }

    @PostMapping("/studio/contents/{id}/{action:publish|offline|draft|delete}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void transition(@PathVariable UUID id, @PathVariable String action) {
        service.transition(id, action, CurrentUser.required());
    }

    record EditRequest(@NotBlank String type, @NotBlank @Size(max = 200) String title,
                       @Size(max = 220) String slug, @Size(max = 500) String summary,
                       @NotBlank String bodyMarkdown, UUID coverMediaId, UUID categoryId,
                       @NotBlank String visibility, boolean commentsEnabled, boolean pinned,
                       List<@Size(max = 50) String> tags,
                       List<@Size(max = 40) String> grantedUsernames) {
        ContentService.EditRequest toService() {
            return new ContentService.EditRequest(type, title, slug, summary, bodyMarkdown, coverMediaId,
                    categoryId, visibility, commentsEnabled, pinned, tags, grantedUsernames);
        }
    }
}
