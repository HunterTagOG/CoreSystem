package dev.huntertagog.coresystem.fabric.common.friends.gui;

import dev.huntertagog.coresystem.common.friends.gui.FriendSettingsStore;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.platform.friends.FriendService;
import dev.huntertagog.coresystem.platform.player.PlayerProfile;
import dev.huntertagog.coresystem.platform.player.PlayerProfileService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FriendGuiServerNet {

    private FriendGuiServerNet() {
    }

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(FriendGuiPackets.C2SOpen.ID, (payload, context) -> {
            context.server().execute(() -> sendSnapshot(context.server(), context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(FriendGuiPackets.C2SAccept.ID, (payload, context) -> {
            UUID from = payload.from();
            context.server().execute(() -> {
                FriendService fs = ServiceProvider.getService(FriendService.class);
                if (fs != null) fs.acceptRequest(context.player().getUuid(), from);
                sendSnapshot(context.server(), context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FriendGuiPackets.C2SDeny.ID, (payload, context) -> {
            UUID from = payload.from();
            context.server().execute(() -> {
                FriendService fs = ServiceProvider.getService(FriendService.class);
                if (fs != null) fs.denyRequest(context.player().getUuid(), from);
                sendSnapshot(context.server(), context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FriendGuiPackets.C2SRemove.ID, (payload, context) -> {
            UUID other = payload.other();
            context.server().execute(() -> {
                FriendService fs = ServiceProvider.getService(FriendService.class);
                if (fs != null) fs.removeFriend(context.player().getUuid(), other);
                sendSnapshot(context.server(), context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FriendGuiPackets.C2SCancel.ID, (payload, context) -> {
            UUID target = payload.target();
            context.server().execute(() -> {
                FriendService fs = ServiceProvider.getService(FriendService.class);
                // cancel outgoing -> simplest: denyRequest(target, me)
                if (fs != null) fs.denyRequest(target, context.player().getUuid());
                sendSnapshot(context.server(), context.player());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FriendGuiPackets.C2SSetSettings.ID, (payload, context) -> {
            boolean allowRequests = payload.allowRequests();
            boolean allowFollow = payload.allowFollow();
            boolean showLastSeen = payload.showLastSeen();

            context.server().execute(() -> {
                FriendSettingsStore.set(context.player().getUuid(), allowRequests, allowFollow, showLastSeen);
                sendSnapshot(context.server(), context.player());
            });
        });
    }

    private static void sendSnapshot(MinecraftServer server, ServerPlayerEntity player) {
        FriendService fs = ServiceProvider.getService(FriendService.class);
        if (fs == null) return;

        PlayerProfileService profiles = ServiceProvider.getService(PlayerProfileService.class);

        var settings = FriendSettingsStore.get(player.getUuid());

        // Freunde
        List<UUID> friends = fs.getFriends(player.getUuid());
        List<FriendGuiSnapshot.FriendEntry> friendEntries = new ArrayList<>(friends.size());

        for (UUID id : friends) {
            String name = resolveName(server, id, profiles);
            boolean online = server.getPlayerManager().getPlayer(id) != null;

            long lastSeen = 0L;
            if (profiles != null) {
                PlayerProfile p = profiles.find(id).orElse(null);
                if (p != null) lastSeen = p.getLastSeenAt();
            }

            friendEntries.add(new FriendGuiSnapshot.FriendEntry(id, name, online, lastSeen));
        }

        // incoming/outgoing
        List<FriendGuiSnapshot.RequestEntry> incoming = new ArrayList<>();
        for (UUID id : fs.getIncomingRequests(player.getUuid())) {
            incoming.add(new FriendGuiSnapshot.RequestEntry(id, resolveName(server, id, profiles)));
        }

        List<FriendGuiSnapshot.RequestEntry> outgoing = new ArrayList<>();
        for (UUID id : fs.getOutgoingRequests(player.getUuid())) {
            outgoing.add(new FriendGuiSnapshot.RequestEntry(id, resolveName(server, id, profiles)));
        }

        FriendGuiSnapshot snap = new FriendGuiSnapshot(
                friendEntries,
                incoming,
                outgoing,
                new FriendGuiSnapshot.FriendSettings(settings.allowRequests(), settings.allowFollow(), settings.showLastSeen())
        );

        // S2C als Payload senden
        ServerPlayNetworking.send(player, new FriendGuiPackets.S2CSnapshot(snap));
    }

    private static String resolveName(MinecraftServer server, UUID id, PlayerProfileService profiles) {
        var online = server.getPlayerManager().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();

        if (profiles != null) {
            PlayerProfile p = profiles.find(id).orElse(null);
            if (p != null && p.getName() != null && !p.getName().isBlank()) return p.getName();
        }
        return id.toString().substring(0, 8);
    }

    public static void openFor(MinecraftServer server, ServerPlayerEntity player) {
        server.execute(() -> sendSnapshot(server, player));
    }
}
