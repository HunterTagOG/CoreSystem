package dev.huntertagog.coresystem.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public record ServerCommand(ProxyServer proxy) implements SimpleCommand {

    private static final String PERMISSION = "coresystem.command.server";

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /server <name>"));
            return;
        }

        String targetName = args[0];

        proxy.getServer(targetName).ifPresentOrElse(
                server -> player.createConnectionRequest(server).fireAndForget(),
                () -> player.sendMessage(Component.text("Unknown server: " + targetName))
        );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        String prefix = args.length == 0 ? "" : args[args.length - 1];

        String p = prefix.toLowerCase(Locale.ROOT);

        return proxy.getAllServers().stream()
                .map(rs -> rs.getServerInfo().getName())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted()
                .collect(Collectors.toList());
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
