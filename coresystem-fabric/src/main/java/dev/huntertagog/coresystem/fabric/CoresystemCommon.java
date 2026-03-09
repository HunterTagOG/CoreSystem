package dev.huntertagog.coresystem.fabric;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.common.item.ModItemGroups;
import dev.huntertagog.coresystem.fabric.common.item.ModItems;
import dev.huntertagog.coresystem.fabric.common.net.CoresystemNetworking;
import dev.huntertagog.coresystem.fabric.common.rct.rewards.RctRewardHooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;

/**
 * Läuft auf Client + Server.
 * Muss client-safe sein: KEIN Redis, KEIN LuckPerms, KEINE ServerLifecycle/Join-Events,
 * KEINE Server-only Klassen (MinecraftServer, ServerPlayerEntity etc. in Wiring).
 */
public final class CoresystemCommon implements ModInitializer {

    private static final Logger LOG = LoggerFactory.get("CoreSystem");
    public static final String MOD_ID = "coresystem";

    @Override
    public void onInitialize() {
        // ------------------------------------------------------------
        // 1) Statische Registrierungen (client-safe)
        // ------------------------------------------------------------
        ModItems.init();
        ModItemGroups.init();
        RctRewardHooks.register();

        // ------------------------------------------------------------
        // 2) Networking: nur IDs/Codecs/Payload-Typen (client-safe)
        //    -> Receiver/Handlers mit Server-Zugriff gehören in CoresystemServer
        // ------------------------------------------------------------
        CoresystemNetworking.registerCommonPayloads();

        // ------------------------------------------------------------
        // 3) ItemGroup Ergänzung (client-safe)
        // ------------------------------------------------------------
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries ->
                entries.addAfter(Items.COMPASS, ModItems.SERVER_KEY)
        );

        LOG.info("CoreSystem common init completed.");
    }
}
