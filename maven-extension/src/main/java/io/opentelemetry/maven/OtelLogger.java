package io.opentelemetry.maven;

import io.opentelemetry.sdk.logs.LogEmitter;
import io.opentelemetry.sdk.logs.data.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

class OtelLogger implements Logger {

  final static Logger logger = LoggerFactory.getLogger(getClass());

  final LogEmitter logEmitter;

  public OtelLogger(LogEmitter logEmitter) {
    this.logEmitter = logEmitter;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void trace(String msg) {
    emitLog(msg, Severity.TRACE);
  }

  @Override
  public void trace(String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void trace(String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void trace(String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String msg) {
    throw unsupportedOperationException();
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void trace(Marker marker, String format, Object... argArray) {

  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {

  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public void debug(String msg) {

  }

  @Override
  public void debug(String format, Object arg) {

  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {

  }

  @Override
  public void debug(String format, Object... arguments) {

  }

  @Override
  public void debug(String msg, Throwable t) {

  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String msg) {

  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public void info(String msg) {
    emitLog(msg, Severity.INFO);
  }

  private void emitLog(String msg, Severity se) {
    logEmitter.logBuilder()
        .setBody(msg)
        .setSeverity(se)
        .emit();
  }

  @Override
  public void info(String format, Object arg) {
    throw unsupportedOperationException();
  }

  private UnsupportedOperationException unsupportedOperationException() {
    throw new UnsupportedOperationException("MEthod not implemented because should not be needed");
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void info(String format, Object... arguments) {

  }

  @Override
  public void info(String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String msg) {
    throw unsupportedOperationException();
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String msg) {
    emitLog(msg, Severity.WARN);
  }

  @Override
  public void warn(String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String msg) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public void error(String msg) {
    emitLog(msg, Severity.ERROR);
  }

  @Override
  public void error(String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(String msg, Throwable t) {
    throw unsupportedOperationException();
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String msg) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    throw unsupportedOperationException();
  }

  @Override
  public void error(Marker marker, String msg, Throwable t) {
    throw unsupportedOperationException();
  }

}
