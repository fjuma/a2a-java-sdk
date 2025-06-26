package io.a2a.server.util;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * Temporary wrapper class for Logger to log debug and trace messages at info level since I am not able to figure
 * out how to enable debug logging via configuration.
 */
public class TempLoggerWrapper implements Logger {
    private final Logger delegate;

    public TempLoggerWrapper(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return delegate.makeLoggingEventBuilder(level);
    }

    @CheckReturnValue
    @Override
    public LoggingEventBuilder atLevel(Level level) {
        return delegate.atLevel(level);
    }

    @Override
    public boolean isEnabledForLevel(Level level) {
        return delegate.isEnabledForLevel(level);
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        delegate.info(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.info(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.info(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.info(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @CheckReturnValue
    @Override
    public LoggingEventBuilder atTrace() {
        return delegate.atTrace();
    }

    @Override
    public void trace(Marker marker, String msg) {
        delegate.info(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        delegate.info(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        delegate.info(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        delegate.info(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        delegate.info(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void debug(String msg) {
        delegate.info(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        delegate.info(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        delegate.info(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.info(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        delegate.info(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        delegate.info(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        delegate.info(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        delegate.info(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        delegate.info(marker, msg, t);
    }

    @CheckReturnValue
    @Override
    public LoggingEventBuilder atDebug() {
        return delegate.atDebug();
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        delegate.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        delegate.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        delegate.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        delegate.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        delegate.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        delegate.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        delegate.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        delegate.info(marker, msg, t);
    }

    @CheckReturnValue
    @Override
    public LoggingEventBuilder atInfo() {
        return delegate.atInfo();
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        delegate.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        delegate.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        delegate.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        delegate.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        delegate.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        delegate.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        delegate.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        delegate.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        delegate.warn(marker, msg, t);
    }

    @CheckReturnValue
    @Override
    public LoggingEventBuilder atWarn() {
        return delegate.atWarn();
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        delegate.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        delegate.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        delegate.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        delegate.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        delegate.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        delegate.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        delegate.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        delegate.error(marker, msg, t);
    }

    @CheckReturnValue
    @Override
    public LoggingEventBuilder atError() {
        return delegate.atError();
    }
}
