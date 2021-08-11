package io.opentelemetry.contrib.samplers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringMatcherTest {

  @Test public void returnsFalseOnNoPatterns(){
    StringMatcher matcher = new StringMatcher(emptyList());
    assertThat(matcher.matches("http://example.com/healthcheck")).isFalse();
  }

  @Test
  public void testExactMatch() {
    StringMatcher matcher = new StringMatcher(asList(".*/healthcheck", ".*/actuator"));
    assertThat(matcher.matches("http://example.com/healthcheck")).isTrue();
    assertThat(matcher.matches("http://example.com/actuator")).isTrue();
    assertThat(matcher.matches("http://example.com/customers")).isFalse();
  }

  @Test
  public void testFullPathMatches() {
    StringMatcher matcher = new StringMatcher(asList(".*/healthcheck", ".*/actuator"));
    assertThat(matcher.matches("http://example.com/healthcheck?qw=asd")).isTrue();
    assertThat(matcher.matches("http://example.com/actuator/info")).isTrue();
    assertThat(matcher.matches("http://example.com/context/actuator")).isTrue();
  }

  @Test
  public void testHostCanBeMatched() {
    StringMatcher matcher = new StringMatcher(asList(".*/healthcheck", ".*/actuator"));
    assertThat(matcher.matches("http://healthcheck")).isTrue();
    assertThat(matcher.matches("http://healthcheck/actuator")).isTrue();
  }

  @Test
  public void testPatternCanBeRegexp() {
    StringMatcher matcher = new StringMatcher(singletonList(".*/health.*"));
    assertThat(matcher.matches("http://example.com/healthcheck")).isTrue();
    assertThat(matcher.matches("http://example.com/healthinfo")).isTrue();
    assertThat(matcher.matches("http://example.com/health")).isTrue();
    assertThat(matcher.matches("http://example.com/health?as=qw")).isTrue();
    assertThat(matcher.matches("http://example.com/hellOnEarth")).isFalse();
  }

  @Test
  public void testRegexpMatchesEndOfLine() {
    StringMatcher matcher = new StringMatcher(singletonList(".*/health$"));
    assertThat(matcher.matches("http://example.com/health")).isTrue();
    assertThat(matcher.matches("http://example.com/healthcheck")).isFalse();
    assertThat(matcher.matches("http://example.com/health/info")).isFalse();
  }

  @Test
  public void testQueryStringIsNotIgnored() {
    StringMatcher matcher = new StringMatcher(singletonList(".*health$"));
    assertThat(matcher.matches("http://example.com/health")).isTrue();
    assertThat(matcher.matches("http://example.com/health?as=qw")).isFalse();
    assertThat(matcher.matches("http://example.com/actuator?action=health")).isTrue();
  }

}