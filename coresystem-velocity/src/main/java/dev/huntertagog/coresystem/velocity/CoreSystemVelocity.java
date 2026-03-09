package dev.huntertagog.coresystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.huntertagog.coresystem.velocity.command.LobbyCommand;
import dev.huntertagog.coresystem.velocity.command.ServerCommand;
import dev.huntertagog.coresystem.velocity.net.BridgeChannel;
import dev.huntertagog.coresystem.velocity.net.VelocityBridge;
import dev.huntertagog.coresystem.velocity.node.NodeRouter;
import dev.huntertagog.coresystem.velocity.node.NodeRouterConfig;
import org.slf4j.Logger;

@Plugin(
        id = "coresystem-velocity",
        name = "CoreSystem Velocity",
        version = "1.0.0"
)
public final class CoreSystemVelocity {

    private final ProxyServer proxy;
    private final Logger log;

    @Inject
    public CoreSystemVelocity(ProxyServer proxy, Logger log) {
        this.proxy = proxy;
        this.log = log;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        // 1) Channel registrieren
        proxy.getChannelRegistrar().register(BridgeChannel.IDENTIFIER);

        // 2) Commands registrieren
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("server").build(),
                new ServerCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder("lobby")
                        .aliases("hub", "spawn", "l")
                        .build(),
                new LobbyCommand(proxy)
        );

        // 3) Router + Bridge initialisieren
        NodeRouter nodeRouter = new NodeRouter();
        VelocityBridge bridge = new VelocityBridge(proxy, nodeRouter);

        // 4) Inbound PluginMessage Listener
        proxy.getEventManager().register(this, bridge);

        // 5) NodeId -> ServerName Mapping laden (MVP via ENV/Config)
        // Beispiel ENV: CORESYSTEM_NODE_MAP="node-a=lobby-1;node-b=islands-1;node-c=islands-2"
        NodeRouterConfig cfg = NodeRouterConfig.fromEnvOrEmpty("CORESYSTEM_NODE_MAP");

        cfg.mappings().forEach(nodeRouter::register);

        log.info("[CoreSystemVelocity] online. registered node routes: {}", cfg.mappings().size());
    }
}
