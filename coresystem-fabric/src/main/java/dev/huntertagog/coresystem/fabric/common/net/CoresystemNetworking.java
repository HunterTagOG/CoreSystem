package dev.huntertagog.coresystem.fabric.common.net;

import dev.huntertagog.coresystem.fabric.common.clans.gui.ClanGuiPackets;
import dev.huntertagog.coresystem.fabric.common.friends.gui.FriendGuiPackets;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageRemoveS2CPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSetS2CPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.RegionImageSyncRequestC2SPayload;
import dev.huntertagog.coresystem.fabric.common.region.visual.RegionOutlinePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class CoresystemNetworking {

    private CoresystemNetworking() {
    }

    public static void registerCommonPayloads() {
        // --- Friends GUI ---
        PayloadTypeRegistry.playC2S().register(FriendGuiPackets.C2SOpen.ID, FriendGuiPackets.C2SOpen.CODEC);
        PayloadTypeRegistry.playC2S().register(FriendGuiPackets.C2SAccept.ID, FriendGuiPackets.C2SAccept.CODEC);
        PayloadTypeRegistry.playC2S().register(FriendGuiPackets.C2SDeny.ID, FriendGuiPackets.C2SDeny.CODEC);
        PayloadTypeRegistry.playC2S().register(FriendGuiPackets.C2SRemove.ID, FriendGuiPackets.C2SRemove.CODEC);
        PayloadTypeRegistry.playC2S().register(FriendGuiPackets.C2SCancel.ID, FriendGuiPackets.C2SCancel.CODEC);
        PayloadTypeRegistry.playC2S().register(FriendGuiPackets.C2SSetSettings.ID, FriendGuiPackets.C2SSetSettings.CODEC);

        PayloadTypeRegistry.playS2C().register(FriendGuiPackets.S2CSnapshot.ID, FriendGuiPackets.S2CSnapshot.CODEC);

        // --- Clans GUI ---
        PayloadTypeRegistry.playC2S().register(ClanGuiPackets.C2SOpen.ID, ClanGuiPackets.C2SOpen.CODEC);
        PayloadTypeRegistry.playC2S().register(ClanGuiPackets.C2SKick.ID, ClanGuiPackets.C2SKick.CODEC);
        PayloadTypeRegistry.playC2S().register(ClanGuiPackets.C2SSetRole.ID, ClanGuiPackets.C2SSetRole.CODEC);
        PayloadTypeRegistry.playC2S().register(ClanGuiPackets.C2SSetSettings.ID, ClanGuiPackets.C2SSetSettings.CODEC);
        PayloadTypeRegistry.playC2S().register(ClanGuiPackets.C2SSetRolePermission.ID, ClanGuiPackets.C2SSetRolePermission.CODEC);

        PayloadTypeRegistry.playS2C().register(ClanGuiPackets.S2CSnapshot.ID, ClanGuiPackets.S2CSnapshot.CODEC);

        PayloadTypeRegistry.playS2C().register(RegionOutlinePayload.TYPE, RegionOutlinePayload.CODEC);

        PayloadTypeRegistry.playS2C().register(RegionImageSetS2CPayload.ID, RegionImageSetS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RegionImageRemoveS2CPayload.ID, RegionImageRemoveS2CPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(RegionImageSyncRequestC2SPayload.ID, RegionImageSyncRequestC2SPayload.CODEC);
        // später: weitere Payloads hier konsolidieren (RegionOutline, Menus, etc.)
    }
}
