package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class RegionPlayerTracker {

    private static final Logger LOG = LoggerFactory.get("RegionTracker");

    // Player UUID -> letzte Region-Ids
    private static final Map<UUID, Set<String>> LAST_REGIONS = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_POS = new HashMap<>();

    private RegionPlayerTracker() {
    }

    public static void register(MinecraftServer server) {
        ServerTickEvents.END_SERVER_TICK.register(srv -> {
            RegionService regions = ServiceProvider.getService(RegionService.class);
            if (regions == null) return;

            for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
                trackPlayer(regions, player);
            }
        });

        LOG.info("RegionPlayerTracker wired to END_SERVER_TICK");
    }

    private static void trackPlayer(RegionService regions, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        BlockPos currentPos = player.getBlockPos();
        BlockPos lastPos = LAST_POS.get(id);

        if (lastPos != null && lastPos.equals(currentPos)) {
            return; // nicht bewegt auf Block-Ebene
        }
        LAST_POS.put(id, currentPos);

        // aktuelle Regionen
        List<RegionDefinition> currentRegions = regions.findRegionsAt(player.getServerWorld(), currentPos);
        Set<String> currentIds = new HashSet<>();
        for (RegionDefinition r : currentRegions) {
            currentIds.add(r.id());
        }

        Set<String> lastIds = LAST_REGIONS.getOrDefault(id, Set.of());

        // Enter = in current, aber nicht in last
        Set<String> entered = new HashSet<>(currentIds);
        entered.removeAll(lastIds);

        // Leave = in last, aber nicht in current
        Set<String> left = new HashSet<>(lastIds);
        left.removeAll(currentIds);

        if (!entered.isEmpty() || !left.isEmpty()) {
            handleTransitions(player, regions, entered, left);
        }

        LAST_REGIONS.put(id, currentIds);
    }

    private static void handleTransitions(ServerPlayerEntity player,
                                          RegionService regions,
                                          Set<String> entered,
                                          Set<String> left) {

        MinecraftServer server = player.getServer();
        var cmdManager = server.getCommandManager();

        // Leave-Commands zuerst
        for (String id : left) {
            regions.findById(id).ifPresent(region -> {
                for (String cmd : region.onLeaveCommands()) {
                    executeRegionCommand(player, cmdManager, cmd);
                }
            });
        }

        // Enter-Commands danach
        for (String id : entered) {
            regions.findById(id).ifPresent(region -> {
                for (String cmd : region.onEnterCommands()) {
                    executeRegionCommand(player, cmdManager, cmd);
                }
            });
        }
    }

    private static void executeRegionCommand(ServerPlayerEntity player,
                                             net.minecraft.server.command.CommandManager cmdManager,
                                             String rawCmd) {
        if (rawCmd == null || rawCmd.isBlank()) return;

        String cmd_raw = rawCmd
                .replace("{player}", player.getGameProfile().getName())
                .replace("{uuid}", player.getUuid().toString());

        // Aus Sicht des Servers oder Spielers – hier: Server-Sicht mit hoher Permission
        var source = player.getCommandSource().withSilent();
        String cmd = normalizeCmd(cmd_raw);
        cmdManager.executeWithPrefix(source, cmd);
    }

    private static String normalizeCmd(String cmd) {
        if (cmd == null) return "";
        cmd = cmd.trim();

        // äußere Quotes entfernen
        if (cmd.length() >= 2 && cmd.startsWith("\"") && cmd.endsWith("\"")) {
            cmd = cmd.substring(1, cmd.length() - 1).trim();
        }

        // \" -> "
        cmd = cmd.replace("\\\"", "\"");
        return cmd;
    }
}
