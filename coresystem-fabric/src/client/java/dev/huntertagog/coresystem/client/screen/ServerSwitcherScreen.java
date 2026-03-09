package dev.huntertagog.coresystem.client.screen;

import dev.huntertagog.coresystem.common.model.ServerTarget;
import dev.huntertagog.coresystem.fabric.common.net.payload.RequestPrivateIslandListPayload;
import dev.huntertagog.coresystem.fabric.common.net.payload.SelectServerPayload;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class ServerSwitcherScreen extends BaseOwoScreen<FlowLayout> {

    // Frame-Layout
    private static final int FRAME_WIDTH = 245;
    private static final int FRAME_HEIGHT = 240;

    // Tiles
    private static final int TILE_WIDTH = 50;
    private static final int TILE_HEIGHT = 65;

    // Teleport-Button (Texture)
    private static final int TP_BUTTON_WIDTH = 45;
    private static final int TP_BUTTON_HEIGHT = 20;

    private static final int TILE_SPACING = 5;
    private static final int TILE_ROW_Y = 67;
    private static final int TP_BUTTON_OFFSET_Y = 2;

    // Bottom-Buttons (Admin / VIP)
    private static final int BOTTOM_BUTTON_WIDTH = 60;
    private static final int BOTTOM_BUTTON_HEIGHT = 20;
    private static final int BOTTOM_MARGIN = 58;

    private final List<ServerTarget> targets;
    private final boolean adminMode;

    public ServerSwitcherScreen(List<ServerTarget> targets, boolean adminMode) {
        super(Text.literal("Server Switcher"));
        this.targets = new ArrayList<>(targets);
        this.adminMode = adminMode;
    }

    // ---------- owo-Basis ----------

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.sizing(Sizing.fill(100), Sizing.fill(100));
        root.surface(Surface.VANILLA_TRANSLUCENT);
        rebuildLayout(root);
    }

    private void rebuildLayout(FlowLayout root) {
        // Alle alten Kinder raus
        for (var child : List.copyOf(root.children())) {
            root.removeChild(child);
        }

        int screenW = this.width;
        int screenH = this.height;

        int baseX = (screenW - FRAME_WIDTH) / 2;
        int baseY = (screenH - FRAME_HEIGHT) / 2;

        // ---------- Frame als TextureComponent ----------

        TextureComponent frame = Components.texture(
                ServerSwitcherTextures.FRAME_OUTER,
                0, 0,
                FRAME_WIDTH, FRAME_HEIGHT,
                FRAME_WIDTH, FRAME_HEIGHT
        );
        frame.sizing(Sizing.fixed(FRAME_WIDTH), Sizing.fixed(FRAME_HEIGHT))
                .positioning(Positioning.absolute(baseX, baseY))
                .zIndex(0);

        root.child(frame);

        // ---------- Teleport-Tiles ----------

        var teleports = targets.stream()
                .filter(t -> t.commandTarget() != null && !t.commandTarget().isEmpty())
                .toList();

        int totalTilesWidth = 3 * TILE_WIDTH + 2 * TILE_SPACING;
        int startX = baseX + (FRAME_WIDTH - totalTilesWidth) / 2;
        int tileY = baseY + TILE_ROW_Y;

        int visible = Math.min(3, teleports.size());
        for (int i = 0; i < visible; i++) {
            ServerTarget target = teleports.get(i);

            int tileX = startX + i * (TILE_WIDTH + TILE_SPACING);
            int buttonX = tileX + (TILE_WIDTH - TP_BUTTON_WIDTH) / 2;
            int buttonY = tileY + TILE_HEIGHT + TP_BUTTON_OFFSET_Y;

            // Tile-Texture
            TextureComponent tileTex = Components.texture(
                    ServerSwitcherTextures.tileFor(target),
                    0, 0,
                    TILE_WIDTH, TILE_HEIGHT,
                    TILE_WIDTH, TILE_HEIGHT
            );
            tileTex.sizing(Sizing.fixed(TILE_WIDTH), Sizing.fixed(TILE_HEIGHT))
                    .positioning(Positioning.absolute(tileX, tileY))
                    .zIndex(2)
                    .tooltip(buildTooltip(target));

            root.child(tileTex);

            // Teleport-Button-Texture
            TextureComponent tpTex = Components.texture(
                    ServerSwitcherTextures.BUTTON_TP,
                    0, 0,
                    TP_BUTTON_WIDTH, TP_BUTTON_HEIGHT,
                    TP_BUTTON_WIDTH, TP_BUTTON_HEIGHT
            );
            tpTex.sizing(Sizing.fixed(TP_BUTTON_WIDTH), Sizing.fixed(TP_BUTTON_HEIGHT))
                    .positioning(Positioning.absolute(buttonX, buttonY))
                    .zIndex(3);

            // Hover → leicht vergrößern + zentriert halten
            tpTex.mouseEnter().subscribe(() -> {
                tpTex
                        .sizing(Sizing.fixed(TP_BUTTON_WIDTH + 4), Sizing.fixed(TP_BUTTON_HEIGHT + 4))
                        .positioning(Positioning.absolute(buttonX - 2, buttonY - 2));
            });

            tpTex.mouseLeave().subscribe(() -> {
                tpTex
                        .sizing(Sizing.fixed(TP_BUTTON_WIDTH), Sizing.fixed(TP_BUTTON_HEIGHT))
                        .positioning(Positioning.absolute(buttonX, buttonY));
            });

            // Klick → zurück zum Parent
            tpTex.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    sendSelect(target);
                    return true;
                }
                return false;
            });

            root.child(tpTex);
        }

        // ---------- Bottom-Buttons (Admin / VIP) ----------

        int bottomY = baseY + FRAME_HEIGHT - BOTTOM_MARGIN - BOTTOM_BUTTON_HEIGHT;
        int centerX = baseX + FRAME_WIDTH / 2;
        boolean hasVip = targets.stream().anyMatch(ServerSwitcherTextures::isVip);

        // Admin-Button nur, wenn adminMode
        if (adminMode) {
            int adminX = centerX - BOTTOM_BUTTON_WIDTH - 4;
            addBottomButton(
                    root,
                    adminX,
                    bottomY,
                    ServerSwitcherTextures.BUTTON_ADMIN,
                    Text.literal("Admin-Menü").formatted(Formatting.GOLD),
                    Text.literal("Verwaltung der Private Islands").formatted(Formatting.GRAY),
                    this::openAdminPanel
            );
        }

        // VIP-Button, unabhängig von adminMode – nur wenn VIP-Target existiert
        if (hasVip) {
            int vipX;
            if (adminMode) {
                vipX = centerX + 4;
            } else {
                vipX = centerX - BOTTOM_BUTTON_WIDTH / 2;
            }

            addBottomButton(
                    root,
                    vipX,
                    bottomY,
                    ServerSwitcherTextures.BUTTON_VIP,
                    Text.literal("VIP-Wild").formatted(Formatting.GOLD),
                    Text.literal("Direkter Zugriff auf den VIP-Wild-Server").formatted(Formatting.GRAY),
                    this::selectVipWild
            );
        }
    }

    // ---------- Tooltip-Helper ----------

    private List<TooltipComponent> buildTooltip(ServerTarget target) {
        // 1. Zeile: Servername
        Text line1 = Text.literal(target.displayName()).formatted(Formatting.GOLD);

        // 2. Zeile: kurze Beschreibung je nach Tile
        String desc = describeTarget(target);
        Text line2 = Text.literal(desc).formatted(Formatting.GRAY);

        return List.of(
                TooltipComponent.of(line1.asOrderedText()),
                TooltipComponent.of(line2.asOrderedText())
        );
    }

    private String describeTarget(ServerTarget target) {
        // Wir leiten die "Art" des Tiles aus ID / DisplayName ab,
        // damit du nicht vom Enum abhängig bist.
        String key = (target.id() + " " + target.displayName())
                .toLowerCase(Locale.ROOT);

        if (key.contains("spawn")) {
            return "Haupt-Spawn, Lobby & zentrale Hubs.";
        } else if (key.contains("build") || key.contains("plot")) {
            return "Bauserver für Projekte, Plots und Infrastruktur.";
        } else if (key.contains("farm")) {
            return "Farmwelt für Ressourcen, Mobs & Grinding.";
        } else if (key.contains("event")) {
            return "Event-Instanzen, Minigames und zeitlich begrenzte Inhalte.";
        } else if (key.contains("wild")) {
            return "Freie Wildnis für Exploration & Survival.";
        } else if (key.contains("private") || key.contains("island")) {
            return "Private Inseln & Instanzen einzelner Spieler.";
        } else {
            return "Wechsel auf diesen Server-Endpunkt.";
        }
    }

    private void addBottomButton(
            FlowLayout root,
            int x,
            int y,
            Identifier texture,
            Text tooltipTitle,
            Text tooltipDescription,
            Runnable onClick
    ) {
        // Sichtbare Texture
        TextureComponent tex = Components.texture(
                texture,
                0, 0,
                ServerSwitcherScreen.BOTTOM_BUTTON_WIDTH, ServerSwitcherScreen.BOTTOM_BUTTON_HEIGHT,
                ServerSwitcherScreen.BOTTOM_BUTTON_WIDTH, ServerSwitcherScreen.BOTTOM_BUTTON_HEIGHT
        );
        tex.sizing(Sizing.fixed(ServerSwitcherScreen.BOTTOM_BUTTON_WIDTH), Sizing.fixed(ServerSwitcherScreen.BOTTOM_BUTTON_HEIGHT))
                .positioning(Positioning.absolute(x, y))
                .zIndex(3)
                .tooltip(List.of(
                        TooltipComponent.of(tooltipTitle.asOrderedText()),
                        TooltipComponent.of(tooltipDescription.asOrderedText())
                ));

        final int baseX = x;
        final int baseY = y;
        final int baseW = ServerSwitcherScreen.BOTTOM_BUTTON_WIDTH;
        final int baseH = ServerSwitcherScreen.BOTTOM_BUTTON_HEIGHT;

        tex.mouseEnter().subscribe(() -> {
            int hoverW = baseW + 4;
            int hoverH = baseH + 4;
            tex
                    .sizing(Sizing.fixed(hoverW), Sizing.fixed(hoverH))
                    .positioning(Positioning.absolute(baseX - 2, baseY - 2));
        });

        tex.mouseLeave().subscribe(() -> {
            tex
                    .sizing(Sizing.fixed(baseW), Sizing.fixed(baseH))
                    .positioning(Positioning.absolute(baseX, baseY));
        });

        // Klick → zurück zum Parent
        tex.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                onClick.run();
                return true;
            }
            return false;
        });

        root.child(tex);
    }

    // ---------- Actions ----------

    private void openAdminPanel() {
        ClientPlayNetworking.send(new RequestPrivateIslandListPayload());
    }

    private void selectVipWild() {
        targets.stream()
                .filter(ServerSwitcherTextures::isVip)
                .findFirst()
                .ifPresent(this::sendSelect);
    }

    private void sendSelect(ServerTarget t) {
        if (t.commandTarget() == null || t.commandTarget().isEmpty()) return;
        ClientPlayNetworking.send(new SelectServerPayload(t.commandTarget()));
        MinecraftClient.getInstance().setScreen(null);
    }

    // ---------- Render / Resize ----------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Vanilla-Hintergrund (Welt, kein eigener Blur)
        this.renderInGameBackground(ctx);
        // oωo-UI + Components
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        if (this.uiAdapter != null && this.uiAdapter.rootComponent != null) {
            rebuildLayout(this.uiAdapter.rootComponent);
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
