package dev.galasa.maven.plugin;

import org.apache.maven.plugin.logging.Log;

import dev.galasa.maven.plugin.log.WrappedLog;

public class WrappedLogMaven implements WrappedLog {

    private Log log ;

    public WrappedLogMaven(Log log) {
        this.log = log;
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }
    
}
