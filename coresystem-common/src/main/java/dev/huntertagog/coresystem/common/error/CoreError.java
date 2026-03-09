package dev.huntertagog.coresystem.common.error;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public record CoreError(
        CoreErrorCode code,
        CoreErrorSeverity severity,
        String technicalMessage,
        Throwable cause,
        Map<String, Object> context
) {

    private static Logger LOG;

    public CoreError {
        if (context == null || context.isEmpty()) {
            context = Collections.emptyMap();
        } else {
            context = Map.copyOf(context);
        }
    }

    public static CoreError of(CoreErrorCode code,
                               CoreErrorSeverity severity,
                               String technicalMessage) {
        return new CoreError(code, severity, technicalMessage, null, Collections.emptyMap());
    }

    public CoreError withCause(Throwable cause) {
        return new CoreError(code, severity, technicalMessage, cause, context);
    }

    public CoreError withContextEntry(String key, Object value) {
        Map<String, Object> map = new HashMap<>(this.context);
        map.put(key, value);
        return new CoreError(code, severity, technicalMessage, cause, map);
    }

    public String toLogString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CoreError[");
        sb.append("code=").append(code);
        sb.append(", severity=").append(severity);
        sb.append(", message=").append(technicalMessage);
        if (cause != null) {
            sb.append(", cause=").append(cause.toString());
        }
        if (!context.isEmpty()) {
            sb.append(", context={");
            StringJoiner joiner = new StringJoiner(", ");
            for (var entry : context.entrySet()) {
                joiner.add(entry.getKey() + "=" + entry.getValue());
            }
            sb.append(joiner);
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public void log() {
        LOG = LoggerFactory.get(code.toString());

        switch (severity) {
            case INFO -> LOG.info(technicalMessage);
            case WARN -> LOG.warn(technicalMessage);
            case ERROR, CRITICAL, FATAL -> LOG.error(technicalMessage);
            default -> LOG.debug(technicalMessage);
        }
    }
}
