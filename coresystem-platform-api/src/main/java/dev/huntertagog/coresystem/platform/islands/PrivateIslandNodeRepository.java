package dev.huntertagog.coresystem.platform.islands;

import dev.huntertagog.coresystem.platform.provider.Service;

public interface PrivateIslandNodeRepository extends Service {
    void saveStatus(PrivateIslandWorldNodeStatus status);

    PrivateIslandWorldNodeStatus getStatus(String nodeId);

    java.util.Collection<PrivateIslandWorldNodeStatus> getAllStatuses();
}
