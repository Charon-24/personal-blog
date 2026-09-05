package com.charon.personalblog.media;

import com.charon.personalblog.security.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaService service;

    public MediaController(MediaService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    MediaService.MediaView upload(@RequestPart MultipartFile file,
                                  @RequestParam(defaultValue = "CONTENT") String purpose) {
        return service.upload(CurrentUser.required().id(), file, purpose);
    }

    @GetMapping
    List<MediaService.MediaView> list() {
        return service.list(CurrentUser.required().id());
    }

    @GetMapping("/{id}")
    ResponseEntity<org.springframework.core.io.Resource> read(@PathVariable UUID id) {
        MediaService.MediaFile file = service.read(id, CurrentUser.idOrNull());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.metadata().mediaType()))
                .contentLength(file.metadata().sizeBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(file.resource());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id, CurrentUser.required().id());
    }
}
