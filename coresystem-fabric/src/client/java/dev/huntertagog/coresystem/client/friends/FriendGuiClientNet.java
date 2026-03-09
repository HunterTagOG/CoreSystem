package dev.huntertagog.coresystem.client.friends;

import dev.huntertagog.coresystem.fabric.common.friends.gui.FriendGuiPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FriendGuiClientNet {

    private static FriendGuiScreen current;

    private FriendGuiClientNet() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FriendGuiPackets.S2CSnapshot.ID, (payload, context) -> {
            var snap = payload.snapshot();

            context.client().execute(() -> {
                if (current == null) {
                    current = new FriendGuiScreen();
                    current.applySnapshot(snap);
                    context.client().setScreen(current);
                } else {
                    current.applySnapshot(snap);
                }
            });
        });
    }

    public static void requestOpen() {
        ClientPlayNetworking.send(new FriendGuiPackets.C2SOpen());
    }

    public static void clearCurrent() {
        current = null;
    }
}
