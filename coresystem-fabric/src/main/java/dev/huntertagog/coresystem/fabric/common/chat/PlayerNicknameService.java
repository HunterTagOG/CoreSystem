package dev.huntertagog.coresystem.fabric.common.chat;

import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface PlayerNicknameService extends Service {

    Optional<String> getNickname(UUID playerId);

    /**
     * Mojang-Name + Nickname in einem Call:
     * - Wenn Nick gesetzt: Nick
     * - Sonst: echter Spielername
     */
    String getEffectiveName(ServerPlayerEntity player);

    /**
     * Setzt oder ändert den Nick. value = null oder leer -> Nick entfernen.
     */
    void setNickname(UUID playerId, @Nullable String nickname);

    void clearNickname(UUID playerId);

    /**
     * Optional: für /realname <nick>
     */
    Optional<UUID> findByNickname(String nickname);
}
