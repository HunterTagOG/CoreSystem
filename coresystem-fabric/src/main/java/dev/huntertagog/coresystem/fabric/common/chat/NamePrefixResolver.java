package dev.huntertagog.coresystem.fabric.common.chat;

import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface NamePrefixResolver extends Service {

    /**
     * Liefert einen Prefix-Text (z. B. "[Admin] ") oder null, wenn kein Prefix angezeigt werden soll.
     */
    @Nullable
    Text resolvePrefix(ServerPlayerEntity player);
}
