package dev.huntertagog.coresystem.common.clans.gui;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClanRolePermissionStore {

    private ClanRolePermissionStore() {
    }

    // TODO: in Redis persistieren: cs:clan:<id>:roleperm:<roleId>
    public static Set<String> getPerms(UUID clanId, String roleId) {
        // minimal default
        return new HashSet<>(Set.of());
    }

    public static void setPerm(UUID clanId, String roleId, String perm, boolean enabled) {
        // TODO persist
    }
}
