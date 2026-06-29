package br.com.tasknoteapp.server.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** This record represents a task request to be created. */
public record TaskRequest(
    @NotNull @NotEmpty @Size(max = 2000) String description,
    List<
            @Size(max = 200)
            @Pattern(
                regexp = "^(https?://.*|#.*)?$",
                message = "URL must start with https:// or #")
            String>
        urls,
    String dueDate,
    Boolean highPriority,
    List<String> tags) {}
