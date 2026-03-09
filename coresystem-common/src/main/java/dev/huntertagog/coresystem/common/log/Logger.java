package dev.huntertagog.coresystem.common.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static boolean isDebug = false;

    static {
        String DEBUG_ENABLED = System.getenv("DEBUG");
        if (DEBUG_ENABLED != null && !DEBUG_ENABLED.isBlank()) {
            isDebug = DEBUG_ENABLED.equalsIgnoreCase("true") || DEBUG_ENABLED.equals("1");
        }
    }

    private final String name;
    private LogLevel level = LogLevel.INFO;
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public Logger(String name) {
        this.name = name;
    }

    private boolean enabled(LogLevel requested) {
        return requested.ordinal() >= level.ordinal();
    }

    // ---------------------------------------------------------
    // Parameter Substitution: "Hello {}, you have {} messages"
    // ---------------------------------------------------------
    private String format(String msg, Object... args) {
        // msg IMMER absichern
        msg = (msg == null) ? "(no message)" : msg;

        // keine args -> direkt zurück
        if (args == null || args.length == 0) return msg;

        StringBuilder sb = new StringBuilder(msg.length() + 32);
        int argIndex = 0;

        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);

            if (c == '{' && i + 1 < msg.length() && msg.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    Object a = args[argIndex++];
                    sb.append(a == null ? "null" : a);
                } else {
                    sb.append("{}");
                }
                i++; // skip '}'
            } else {
                sb.append(c);
            }
        }

        // Falls args übrig sind, optional anhängen (hilft bei "LOGGER.error(msg, e)")
        if (argIndex < args.length) {
            sb.append(" | args=");
            for (int j = argIndex; j < args.length; j++) {
                sb.append(j == argIndex ? "" : ", ");
                sb.append(args[j] == null ? "null" : args[j].toString());
            }
        }

        return sb.toString();
    }

    private void log(LogLevel lvl, String msg, Object... args) {
        if (!enabled(lvl)) return;

        String formatted = format(msg, args);

        System.out.printf(
                "%s[%s] [%s%s%s] [%s] %s%s%n",
                AnsiColor.WHITE,
                FORMAT.format(LocalDateTime.now()),
                lvl.color,
                lvl.name(),
                AnsiColor.WHITE,
                name,
                formatted,
                AnsiColor.RESET
        );
    }

    // ---------------------------------------------------------
    // API-Methoden
    // ---------------------------------------------------------
    public void info(String msg, Object... args) {
        log(LogLevel.INFO, msg, args);
    }

    public void warn(String msg, Object... args) {
        log(LogLevel.WARN, msg, args);
    }

    public void error(String msg, Throwable t) {
        log(LogLevel.ERROR, msg);
        if (t != null) t.printStackTrace(System.out);
    }

    public void error(String msg, Object... args) {
        log(LogLevel.ERROR, msg, args);
    }

    public void debug(String msg, Object... args) {
        if (!isDebug) return;
        log(LogLevel.DEBUG, msg, args);
    }

    public void setLevel(LogLevel lvl) {
        this.level = lvl;
    }
}
