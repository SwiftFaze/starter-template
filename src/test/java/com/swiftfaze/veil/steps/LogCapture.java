package com.swiftfaze.veil.steps;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Records every log event, from any logger, between {@link #attach()} and {@link #detach()}. */
final class LogCapture {

    private final List<ILoggingEvent> events = new ArrayList<>();
    private AppenderBase<ILoggingEvent> appender;

    void attach() {
        appender = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                events.add(event);
            }
        };
        appender.start();
        rootLogger().addAppender(appender);
    }

    void detach() {
        rootLogger().detachAppender(appender);
    }

    List<ILoggingEvent> events() {
        return List.copyOf(events);
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
