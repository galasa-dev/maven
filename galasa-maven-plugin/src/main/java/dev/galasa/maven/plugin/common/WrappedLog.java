package dev.galasa.maven.plugin.log;

public interface WrappedLog {
    void info(String message);
    void error(String message);
}
