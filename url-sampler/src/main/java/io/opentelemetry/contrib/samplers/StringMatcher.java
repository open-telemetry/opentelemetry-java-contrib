package io.opentelemetry.contrib.samplers;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pretty trivial regex string matcher using {@link Pattern}.
 *
 * The only caveat is that patterns are automatically expanded to end with ".*" unless they already ends with "$".
 */
class StringMatcher {
  private final Collection<Pattern> patterns;

  public StringMatcher(Collection<String> patterns) {
    this.patterns = patterns.stream()
        .map(p -> p.endsWith("$") ? p : (p + ".*"))
        .map(Pattern::compile)
        .collect(Collectors.toList());
  }

  public boolean matches(String url) {
    for (Pattern pattern : patterns) {
      if (pattern.matcher(url).matches()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "StringMatcher{" +
           "patterns=" + patterns +
           '}';
  }
}
