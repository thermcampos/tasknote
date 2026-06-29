package br.com.tasknoteapp.server.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** This record represents a note patch payload. */
public record NotePatchRequest(
    @Size(max = 100) String title,
    @Size(max = 50000) String description,
    @Size(max = 200)
        @Pattern(
            regexp = "^(https?://.*|#.*)?$",
            message = "URL must start with https:// or #")
        String url,
    List<String> tags) {}
