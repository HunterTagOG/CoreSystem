package dev.huntertagog.coresystem.fabric.common.rct.rewards;

import dev.huntertagog.coresystem.common.rct.rewards.RedisRctRewardStore;
import dev.huntertagog.coresystem.common.rct.rewards.dto.RctRewardItemDto;
import dev.huntertagog.coresystem.common.rct.rewards.dto.RctRewardSpecDto;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RctRewardService {

    private final RedisRctRewardStore store = new RedisRctRewardStore();

    public void grantOnVictory(MinecraftServer server, ServerPlayerEntity player, String trainerId) {
        RegistryWrapper.WrapperLookup registries = server.getRegistryManager(); // passt zu deinem Setup

        RctRewardSpecDto spec = store.find(trainerId).orElse(RctRewardSpecDto.empty());
        if (spec == null) return;

        // Money (placeholder)
        if (spec.money() > 0) {
            // TODO Economy
        }

        // Items
        if (spec.items() != null) {
            for (RctRewardItemDto dto : spec.items()) {
                var stack = RctRewardItemCodec.fromDto(dto, registries);
                if (!stack.isEmpty()) player.getInventory().offerOrDrop(stack);
            }
        }

        // Commands
        if (spec.consoleCommands() != null) {
            var console = server.getCommandSource();
            for (String cmd : spec.consoleCommands()) {
                if (cmd == null || cmd.isBlank()) continue;
                server.getCommandManager().executeWithPrefix(console, cmd.replace("{player}", player.getGameProfile().getName()));
            }
        }

        // Permissions (optional)
        if (spec.permissionNodes() != null) {
            // TODO PermissionService grant
        }
    }

    public void seedDefaultsIfMissing() {
        store.seedDefaultsIfMissing();
    }
}
