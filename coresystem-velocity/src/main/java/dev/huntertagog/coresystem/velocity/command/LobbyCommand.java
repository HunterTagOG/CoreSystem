package dev.huntertagog.coresystem.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

public record LobbyCommand(ProxyServer proxy) implements SimpleCommand {

    private static final String PERMISSION = "coresystem.command.lobby";

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command."));
            return;
        }

        proxy.getServer("lobby01").ifPresentOrElse(
                server -> player.createConnectionRequest(server).fireAndForget(),
                () -> player.sendMessage(Component.text("Unknown server: lobby01"))
        );
    }

    // ---------------------------------------------------------------------
    // Permission Logic
    // ---------------------------------------------------------------------

    private boolean hasAccess(Player player) {
        // OP / Admin wildcard
        if (player.hasPermission("*")) return true;

        // Optional: explizite OP-Permission (falls ihr sie nutzt)
        if (player.hasPermission("velocity.op")) return true;

        // Normale Permission
        return player.hasPermission(PERMISSION);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // Velocity ruft das VOR execute auf
        if (!(invocation.source() instanceof Player player)) return false;
        return hasAccess(player);
    }
}
