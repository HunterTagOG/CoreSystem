package dev.huntertagog.coresystem.common.log;

public enum LogLevel {
    INFO(AnsiColor.GREEN),
    WARN(AnsiColor.YELLOW),
    ERROR(AnsiColor.RED),
    DEBUG(AnsiColor.CYAN);

    public final String color;

    LogLevel(String color) {
        this.color = color;
    }
}
