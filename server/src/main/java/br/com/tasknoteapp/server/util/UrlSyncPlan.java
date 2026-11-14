package br.com.tasknoteapp.server.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes which URLs a patch payload adds and which stored URLs it removes.
 *
 * <p>Both notes and tasks store their URLs the same way, so the normalization and the diff between
 * the payload and the stored rows live here instead of being duplicated in each service.
 */
public record UrlSyncPlan(Set<String> toInsert, Set<String> obsolete) {

  /**
   * Builds a sync plan from a request payload and the URLs currently stored for the owner.
   *
   * @param payload The URLs sent by the client. An empty or blank value means "remove everything".
   * @param stored The URLs already persisted for the owner.
   * @return {@link UrlSyncPlan} with the URLs to insert and the URLs to delete.
   */
  public static UrlSyncPlan of(List<String> payload, List<String> stored) {
    Set<String> wanted = normalize(payload);
    Set<String> existing = normalize(stored);

    Set<String> toInsert = wanted.stream()
        .filter(url -> !existing.contains(url))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Set<String> obsolete = existing.stream()
        .filter(url -> !wanted.contains(url))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return new UrlSyncPlan(toInsert, obsolete);
  }

  /**
   * Normalizes a list of URLs, dropping nulls and blanks and removing duplicates.
   *
   * @param urls The URLs to normalize, may be null.
   * @return A set with trimmed, lower cased URLs, preserving the input order.
   */
  public static Set<String> normalize(List<String> urls) {
    if (Objects.isNull(urls)) {
      return new LinkedHashSet<>();
    }

    return urls.stream()
        .filter(url -> !Objects.isNull(url) && !url.isBlank())
        .map(url -> url.trim().toLowerCase())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
