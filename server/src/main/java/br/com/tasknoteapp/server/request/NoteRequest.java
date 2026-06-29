package br.com.tasknoteapp.server.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** This record represents a note request to be created. */
public record NoteRequest(
    @NotNull @Size(max = 100) String title,
    @NotNull @Size(max = 50000) String description,
    @Size(max = 200)
        @Pattern(
            regexp = "^(https?://.*|#.*)?$",
            message = "URL must start with https:// or #")
        String url,
    List<String> tags) {}
