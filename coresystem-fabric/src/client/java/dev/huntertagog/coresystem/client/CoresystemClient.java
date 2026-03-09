package dev.huntertagog.coresystem.client;

import dev.huntertagog.coresystem.client.clans.ClanGuiClientNet;
import dev.huntertagog.coresystem.client.friends.FriendGuiClientNet;
import dev.huntertagog.coresystem.client.net.RegionImageNetClient;
import dev.huntertagog.coresystem.client.region.ClientRegionOutlineManager;
import dev.huntertagog.coresystem.client.region.RegionImageResourceReloadListener;
import dev.huntertagog.coresystem.client.region.RegionImageWorldRenderer;
import dev.huntertagog.coresystem.client.region.RegionOutlineClientNetworking;
import dev.huntertagog.coresystem.client.render.ServerKeyRenderer;
import dev.huntertagog.coresystem.client.screen.PrivateIslandAdminScreen;
import dev.huntertagog.coresystem.client.screen.ServerSwitcherScreen;
import dev.huntertagog.coresystem.fabric.common.item.ModItems;
import dev.huntertagog.coresystem.fabric.common.net.CoresystemNetworking;
import dev.huntertagog.coresystem.fabric.common.net.payload.OpenMenuPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.PrivateIslandListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;

public final class CoresystemClient implements ClientModInitializer {

    private static final ServerKeyRenderer SERVER_KEY_RENDERER = new ServerKeyRenderer();

    @Override
    public void onInitializeClient() {

        // ------------------------------------------------------------
        // Region / World Visuals
        // ------------------------------------------------------------
        // 1) Payload-Typen registrieren
        CoresystemNetworking.registerCommonPayloads();

        // 2) Danach erst Receiver
        RegionOutlineClientNetworking.register();

        ClientRegionOutlineManager.init();

        RegionImageNetClient.register();
        RegionImageWorldRenderer.init();
        RegionImageResourceReloadListener.register();

        // ------------------------------------------------------------
        // Item Renderer
        // ------------------------------------------------------------
        BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.SERVER_KEY,
                SERVER_KEY_RENDERER::render
        );

        // ------------------------------------------------------------
        // Global GUI / Screen Networking
        // ------------------------------------------------------------

        // Server Switcher
        ClientPlayNetworking.registerGlobalReceiver(OpenMenuPayload.ID, (payload, context) -> {
            var client = context.client();
            client.execute(() -> {
                if (client.player == null) return;
                client.setScreen(new ServerSwitcherScreen(
                        payload.targets(),
                        payload.adminMode()
                ));
            });
        });

        // Private Island Admin
        ClientPlayNetworking.registerGlobalReceiver(
                PrivateIslandListPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    var parent = client.currentScreen;
                    client.setScreen(new PrivateIslandAdminScreen(parent, payload.uuids()));
                })
        );

        // ------------------------------------------------------------
        // Friend GUI (OWO, Client-Side rendered)
        // ------------------------------------------------------------
        FriendGuiClientNet.register();

        // ------------------------------------------------------------
        // Clan GUI (OWO, Client-Side rendered)
        // ------------------------------------------------------------
        ClanGuiClientNet.register();
    }
}
