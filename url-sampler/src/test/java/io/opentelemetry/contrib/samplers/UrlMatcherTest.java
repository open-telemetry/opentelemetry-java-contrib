package io.opentelemetry.contrib.samplers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlMatcherTest {

  @Test public void returnsFalseOnNoPatterns(){
    UrlMatcher matcher = new UrlMatcher(emptyList());
    assertThat(matcher.matches("http://example.com/healthcheck")).isFalse();
  }

  @Test
  public void testExactMatch() {
    UrlMatcher matcher = new UrlMatcher(asList(".*/healthcheck", ".*/actuator"));
    assertThat(matcher.matches("http://example.com/healthcheck")).isTrue();
    assertThat(matcher.matches("http://example.com/actuator")).isTrue();
    assertThat(matcher.matches("http://example.com/customers")).isFalse();
  }

  @Test
  public void testPathStartMatches() {
    UrlMatcher matcher = new UrlMatcher(asList(".*/healthcheck", ".*/actuator"));
    assertThat(matcher.matches("http://example.com/healthcheck?qw=asd")).isTrue();
    assertThat(matcher.matches("http://example.com/actuator/info")).isTrue();
    assertThat(matcher.matches("http://example.com/context/actuator")).isFalse();
  }

  @Test
  public void testHostIsIgnored() {
    UrlMatcher matcher = new UrlMatcher(asList(".*/healthcheck", ".*/actuator"));
    assertThat(matcher.matches("http://healthcheck")).isFalse();
    assertThat(matcher.matches("http://healthcheck/actuator")).isTrue();
  }

  @Test
  public void testPatternCanBeRegexp() {
    UrlMatcher matcher = new UrlMatcher(singletonList(".*/health.*"));
    assertThat(matcher.matches("http://example.com/healthcheck")).isTrue();
    assertThat(matcher.matches("http://example.com/healthinfo")).isTrue();
    assertThat(matcher.matches("http://example.com/health")).isTrue();
    assertThat(matcher.matches("http://example.com/health?as=qw")).isTrue();
    assertThat(matcher.matches("http://example.com/hellOnEarth")).isFalse();
  }

  @Test
  public void testRegexpMatchesEndOfLine() {
    UrlMatcher matcher = new UrlMatcher(singletonList(".*/health$"));
    assertThat(matcher.matches("http://example.com/health")).isTrue();
    assertThat(matcher.matches("http://example.com/healthcheck")).isFalse();
    assertThat(matcher.matches("http://example.com/health/info")).isFalse();
  }

  @Test
  public void testQueryStringIsIgnored() {
    UrlMatcher matcher = new UrlMatcher(singletonList(".*/health$"));
    assertThat(matcher.matches("http://example.com/health")).isTrue();
    assertThat(matcher.matches("http://example.com/health?as=qw")).isTrue();
  }

}