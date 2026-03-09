package dev.huntertagog.coresystem.client.clans;

import dev.huntertagog.coresystem.fabric.common.clans.gui.ClanGuiPackets;
import dev.huntertagog.coresystem.fabric.common.clans.gui.ClanGuiSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClanGuiClientNet {

    private static ClanGuiScreen current;

    private ClanGuiClientNet() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ClanGuiPackets.S2CSnapshot.ID, (payload, ctx) -> {
            ClanGuiSnapshot snap = payload.snapshot();
            ctx.client().execute(() -> {
                if (current == null) {
                    current = new ClanGuiScreen();
                    current.applySnapshot(snap);
                    ctx.client().setScreen(current);
                } else {
                    current.applySnapshot(snap);
                }
            });
        });
    }

    public static void requestOpen() {
        ClientPlayNetworking.send(new ClanGuiPackets.C2SOpen());
    }

    public static void clearCurrent() {
        current = null;
    }
}
