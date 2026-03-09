package dev.huntertagog.coresystem.platform.audit;

import dev.huntertagog.coresystem.platform.provider.Service;

public interface AuditLogService extends Service {

    void log(AuditContext context, String action, String details);
}
