package io.opentelemetry.contrib.samplers;

import java.util.Collection;
import java.util.stream.Collectors;

class UrlMatcher {
  private final Collection<String> patterns;

  public UrlMatcher(Collection<String> patterns) {
    this.patterns = patterns.stream()
        .map(p -> p.endsWith("$") ? p : (p + ".*"))
        .collect(Collectors.toList());
  }

  public boolean matches(String url) {
    String path = extractPath(url);
    for (String pattern : patterns) {
      if (path.matches(pattern)) {
        return true;
      }
    }
    return false;
  }

  private String extractPath(String url) {
    int startOfQueryString = url.indexOf('?');
    int i = url.indexOf("//");
    int afterSchema = (i == -1) ? 0 : (i + 2);
    int startOfPath = url.indexOf('/', afterSchema);
    return url.substring(startOfPath == -1 ? 0 : startOfPath, startOfQueryString == -1 ? url.length() : startOfQueryString);
  }
}
