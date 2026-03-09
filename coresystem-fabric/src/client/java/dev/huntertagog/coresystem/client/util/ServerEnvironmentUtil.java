package dev.huntertagog.coresystem.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ServerEnvironmentUtil {

    // hier trägst du alle „legalen“ Server ein
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "play.cobblecrew.net",
            "mc.cobblecrew.net"
    );

    private ServerEnvironmentUtil() {
    }

    /**
     * true, wenn der Client gerade mit einem deiner Server verbunden ist.
     */
    public static boolean isCobbleverseServer() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Singleplayer / Lan -> verbieten
        if (client.getServer() != null) {
            return false;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return false;
        }

        ServerInfo info = handler.getServerInfo();
        if (info == null || info.address == null) {
            return false;
        }

        String addr = info.address.toLowerCase(Locale.ROOT).trim();

        // typischerweise "play.cobblecrew.net", "play.cobblecrew.net:25565", "123.123.123.123:25565"
        if (addr.contains(":")) {
            addr = addr.substring(0, addr.indexOf(':'));
        }

        for (String allowed : ALLOWED_HOSTS) {
            if (addr.equalsIgnoreCase(allowed)) {
                return true;
            }
        }

        return false;
    }
}
