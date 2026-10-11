package com.swiftfaze.veil.steps;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;

/** Test probe: reports whether a class's logger emitted a WARN while an action ran. */
final class WarnLogProbe {

    private WarnLogProbe() {
    }

    /** Runs {@code action} and reports whether {@code source}'s logger emitted a WARN meanwhile. */
    static boolean warnsWhile(Class<?> source, Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(source);
        WarnDetector detector = new WarnDetector();
        detector.start();
        logger.addAppender(detector);
        try {
            action.run();
        } finally {
            logger.detachAppender(detector);
        }
        return detector.sawWarning();
    }

    private static final class WarnDetector extends AppenderBase<ILoggingEvent> {
        private boolean sawWarning;

        WarnDetector() {
            LevelFilter warnOnly = new LevelFilter();
            warnOnly.setLevel(Level.WARN);
            warnOnly.setOnMatch(FilterReply.ACCEPT);
            warnOnly.setOnMismatch(FilterReply.DENY);
            warnOnly.start();
            addFilter(warnOnly);
        }

        @Override
        protected void append(ILoggingEvent event) {
            sawWarning = true;
        }

        boolean sawWarning() {
            return sawWarning;
        }
    }
}
