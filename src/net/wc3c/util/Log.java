package net.wc3c.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Log {
    private static final Logger logger;
    
    static {
        logger = LogManager.getLogger();
    }
    
    public static final void debug(String msg, Object... params) {
        logger.debug(msg, params);
    }
    
    public static final void info(String msg, Object... params) {
        logger.info(msg, params);
    }
    
    public static final void trace(String msg, Object... params) {
        logger.trace(msg, params);
    }
    
    public static final void warn(String msg, Object... params) {
        logger.warn(msg, params);
    }
    
    public static final void error(String msg, Object... params) {
        logger.error(msg, params);
    }
    
    public static final void fatal(String msg, Object... params) {
        logger.fatal(msg, params);
    }
    
    public static final void exception(Throwable t) {
        logger.catching(t);
    }
    
    public static final void entry(Object... params) {
        logger.entry(params);
    }
    
    public static final <R> R exit(R returnValue) {
        return logger.exit(returnValue);
    }
    
    public static final void exit() {
        logger.exit();
    }
}
