package dev.huntertagog.coresystem.fabric.common.chat;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatDisplayNameFormatter {

    private final PlayerNicknameService nicknameService;
    private final NamePrefixResolver prefixResolver;

    public ChatDisplayNameFormatter() {
        this.nicknameService = ServiceProvider.getService(PlayerNicknameService.class);
        this.prefixResolver = ServiceProvider.getService(NamePrefixResolver.class);
    }

    public Text buildDisplayName(ServerPlayerEntity player) {
        MutableText result = Text.empty();

        if (prefixResolver != null) {
            Text prefix = prefixResolver.resolvePrefix(player);
            if (prefix != null) {
                result.append(prefix);
            }
        }

        String baseName;
        if (nicknameService != null) {
            baseName = nicknameService.getEffectiveName(player);
        } else {
            baseName = player.getGameProfile().getName();
        }

        // Farben-Strategie nach Rolle / Permission anpassbar
        Formatting nameColor = Formatting.WHITE;
        // z.B. Staff farbig:
        // if (perms.has(...)) nameColor = Formatting.AQUA; etc.

        result.append(Text.literal(baseName).formatted(nameColor));
        return result;
    }
}
