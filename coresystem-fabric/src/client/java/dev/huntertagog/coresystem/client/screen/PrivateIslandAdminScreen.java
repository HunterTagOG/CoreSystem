package dev.huntertagog.coresystem.client.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PrivateIslandAdminScreen extends BaseOwoScreen<FlowLayout> {

    private static final Identifier PANEL_TEXTURE =
            Identifier.of("coresystem", "textures/gui/server_switch/frame_outer.png");
    private static final Identifier SLOT_BG_TEXTURE =
            Identifier.of("coresystem", "textures/gui/server_switch/admin_slot_bg.png");
    private static final Identifier BUTTON_BACK =
            Identifier.of("coresystem", "textures/gui/server_switch/button_back.png");

    // Frame: echte Größe des Textures
    private static final int WIDTH = 245;
    private static final int HEIGHT = 240;

    private static final int WIDTH_TEX = 245;
    private static final int HEIGHT_TEX = 240;

    // Slots (2 Spalten × 3 sichtbare Zeilen)
    private static final int SLOT_WIDTH = 90;
    private static final int SLOT_HEIGHT = 30;
    private static final int SLOT_START_Y = 85;
    private static final int SLOT_Y_SPACING = 35;

    private static final int SLOT_LEFT_X = 30;
    private static final int SLOT_RIGHT_X = WIDTH - 30 - SLOT_WIDTH;

    private static final int VISIBLE_LINES = 3;

    // Back-Button / Search
    private static final int BACK_BUTTON_WIDTH = 60;
    private static final int BACK_BUTTON_HEIGHT = 20;
    private static final int BACK_BUTTON_Y = HEIGHT - 35 - BACK_BUTTON_HEIGHT;

    private static final int SEARCH_WIDTH = 130;
    private static final int SEARCH_HEIGHT = 10;
    private static final int SEARCH_OFFSET_Y = BACK_BUTTON_Y - 30;

    private final Screen parent;
    private final List<UUID> allPlayers = new ArrayList<>();
    private final List<UUID> filteredPlayers = new ArrayList<>();

    private final List<Component> slotComponents = new ArrayList<>();

    // Back-Button jetzt als owo-TextureComponent
    private TextureComponent backButtonComponent;

    // Scroll-State in Zeilen
    private int scrollLine = 0;

    public PrivateIslandAdminScreen(Screen parent, List<UUID> playersOnPrivateIslands) {
        super(Text.empty());
        this.parent = parent;
        this.allPlayers.addAll(playersOnPrivateIslands);
        this.filteredPlayers.addAll(playersOnPrivateIslands);
    }

    // ----------------------------------------------------------------------
    // oωo-Basis
    // ----------------------------------------------------------------------

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.sizing(Sizing.fill(100), Sizing.fill(100));
        root.surface(Surface.VANILLA_TRANSLUCENT);

        int baseX = (this.width - WIDTH) / 2;
        int baseY = (this.height - HEIGHT) / 2;

        // Panel
        var panel = Components.texture(
                PANEL_TEXTURE,
                0, 0,
                WIDTH_TEX, HEIGHT_TEX
        );
        panel.sizing(Sizing.fixed(WIDTH), Sizing.fixed(HEIGHT))
                .positioning(Positioning.absolute(baseX, baseY))
                .zIndex(0);
        root.child(panel);

        // Back-Button als TextureComponent mit Hover-Effekt
        int backX = baseX + (WIDTH - BACK_BUTTON_WIDTH) / 2;
        int backY = baseY + BACK_BUTTON_Y;

        this.backButtonComponent = Components.texture(
                BUTTON_BACK,
                0, 0,
                BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT,
                BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT
        );

        this.backButtonComponent
                .sizing(Sizing.fixed(BACK_BUTTON_WIDTH), Sizing.fixed(BACK_BUTTON_HEIGHT))
                .positioning(Positioning.absolute(backX, backY))
                .zIndex(10);

        // Hover → leicht vergrößern + zentriert halten
        this.backButtonComponent.mouseEnter().subscribe(() -> {
            backButtonComponent
                    .sizing(Sizing.fixed(BACK_BUTTON_WIDTH + 4), Sizing.fixed(BACK_BUTTON_HEIGHT + 4))
                    .positioning(Positioning.absolute(backX - 2, backY - 2));
        });

        this.backButtonComponent.mouseLeave().subscribe(() -> {
            backButtonComponent
                    .sizing(Sizing.fixed(BACK_BUTTON_WIDTH), Sizing.fixed(BACK_BUTTON_HEIGHT))
                    .positioning(Positioning.absolute(backX, backY));
        });

        // Klick → zurück zum Parent
        this.backButtonComponent.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                MinecraftClient.getInstance().setScreen(parent);
                return true;
            }
            return false;
        });

        root.child(this.backButtonComponent);

        // Slots initial aufbauen
        rebuildSlots(root);
    }

    // ----------------------------------------------------------------------
    // Init: Search-Feld (owo TextBoxComponent)
    // ----------------------------------------------------------------------

    @Override
    protected void init() {
        super.init(); // baut Adapter + build(root)

        int baseX = (this.width - WIDTH) / 2;
        int baseY = (this.height - HEIGHT) / 2;

        int searchX = baseX + (WIDTH - SEARCH_WIDTH) / 2;
        int searchY = baseY + SEARCH_OFFSET_Y;

        TextBoxComponent searchField = Components.textBox(Sizing.fixed(SEARCH_WIDTH));
        searchField.setMaxLength(32);

        searchField
                .positioning(Positioning.absolute(searchX, searchY))
                .sizing(Sizing.fixed(SEARCH_WIDTH), Sizing.fixed(SEARCH_HEIGHT))
                .zIndex(200);

        searchField.onChanged().subscribe(this::onSearchChanged);

        this.uiAdapter.rootComponent.child(searchField);
    }

    // ----------------------------------------------------------------------
    // Filter / Scroll
    // ----------------------------------------------------------------------

    private void onSearchChanged(String newText) {
        String query = newText.trim().toLowerCase(Locale.ROOT);
        filteredPlayers.clear();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        if (query.isEmpty()) {
            filteredPlayers.addAll(allPlayers);
        } else {
            filteredPlayers.addAll(
                    allPlayers.stream()
                            .filter(uuid -> {
                                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
                                if (entry == null) return false;
                                String name = entry.getProfile().getName().toLowerCase(Locale.ROOT);
                                return name.contains(query);
                            })
                            .toList()
            );
        }

        scrollLine = 0;
        clampScroll();

        if (this.uiAdapter != null && this.uiAdapter.rootComponent != null) {
            rebuildSlots(this.uiAdapter.rootComponent);
        }
    }

    private int getTotalLines() {
        return (int) Math.ceil(filteredPlayers.size() / 2.0);
    }

    private void clampScroll() {
        int totalLines = getTotalLines();
        int maxScroll = Math.max(0, totalLines - VISIBLE_LINES);
        if (scrollLine > maxScroll) scrollLine = maxScroll;
        if (scrollLine < 0) scrollLine = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int totalLines = getTotalLines();
        if (totalLines <= VISIBLE_LINES) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }

        if (vertical > 0) scrollLine--;
        else if (vertical < 0) scrollLine++;

        clampScroll();

        if (this.uiAdapter != null && this.uiAdapter.rootComponent != null) {
            rebuildSlots(this.uiAdapter.rootComponent);
        }
        return true;
    }

    // ----------------------------------------------------------------------
    // Slots als Components
    // ----------------------------------------------------------------------

    private void rebuildSlots(FlowLayout root) {
        // alte Components raus
        for (var c : slotComponents) root.removeChild(c);
        slotComponents.clear();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        int baseX = (this.width - WIDTH) / 2;
        int baseY = (this.height - HEIGHT) / 2;

        int totalLines = getTotalLines();
        if (totalLines == 0) {
            LabelComponent noPlayers = Components.label(Text.literal("No players on private islands"));
            int textX = baseX + WIDTH / 2;
            int textY = baseY + SLOT_START_Y + 8;

            noPlayers.positioning(Positioning.absolute(textX - this.textRenderer.getWidth(noPlayers.text()) / 2, textY))
                    .zIndex(5);

            root.child(noPlayers);
            slotComponents.add(noPlayers);
            return;
        }

        for (int lineIdx = 0; lineIdx < VISIBLE_LINES; lineIdx++) {
            int globalLine = scrollLine + lineIdx;
            if (globalLine >= totalLines) break;

            int slotY = baseY + SLOT_START_Y + lineIdx * SLOT_Y_SPACING;

            for (int col = 0; col < 2; col++) {
                int playerIndex = globalLine * 2 + col;
                if (playerIndex >= filteredPlayers.size()) continue;

                UUID uuid = filteredPlayers.get(playerIndex);
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);

                String name = entry != null
                        ? entry.getProfile().getName()
                        : uuid.toString().substring(0, 8);

                int slotX = baseX + (col == 0 ? SLOT_LEFT_X : SLOT_RIGHT_X);

                // Slot-Background
                TextureComponent bg = Components.texture(
                        SLOT_BG_TEXTURE,
                        0, 0,
                        SLOT_WIDTH, SLOT_HEIGHT,
                        SLOT_WIDTH, SLOT_HEIGHT
                );
                bg.sizing(Sizing.fixed(SLOT_WIDTH), Sizing.fixed(SLOT_HEIGHT))
                        .positioning(Positioning.absolute(slotX, slotY))
                        .zIndex(5);

                root.child(bg);
                slotComponents.add(bg);

                // Kopf
                int headSize = 24;
                int headX = slotX + 8;
                int headY = slotY + (SLOT_HEIGHT - headSize) / 2;

                if (entry != null) {
                    SkinTextures skin = entry.getSkinTextures();
                    if (skin != null) {
                        PlayerHeadComponent head = new PlayerHeadComponent(skin, headSize);
                        head.positioning(Positioning.absolute(headX, headY))
                                .zIndex(6);
                        root.child(head);
                        slotComponents.add(head);
                    }
                }

                // Name
                int textX = headX + headSize + 6;
                int textY = slotY + (SLOT_HEIGHT - this.textRenderer.fontHeight) / 2 + 1;

                LabelComponent nameLabel = Components.label(Text.literal(name));
                nameLabel.positioning(Positioning.absolute(textX, textY))
                        .zIndex(7);

                root.child(nameLabel);
                slotComponents.add(nameLabel);
            }
        }
    }

    // eigener Kopf-Renderer als Component
    private static class PlayerHeadComponent extends BoxComponent {

        private final SkinTextures skin;
        private final int size;

        public PlayerHeadComponent(SkinTextures skin, int size) {
            super(Sizing.fixed(size), Sizing.fixed(size));
            this.skin = skin;
            this.size = size;
        }

        @Override
        public void draw(OwoUIDrawContext ctx, int mouseX, int mouseY, float partialTicks, float delta) {
            if (skin != null) {
                PlayerSkinDrawer.draw(ctx, skin, this.x(), this.y(), this.size);
            }
        }
    }

    // ----------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC = Back
        if (keyCode == 256) {
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void updatePlayers(List<UUID> newList) {
        this.allPlayers.clear();
        this.allPlayers.addAll(newList);

        this.filteredPlayers.clear();
        this.filteredPlayers.addAll(newList);

        this.scrollLine = 0;
        this.clampScroll();

        if (this.uiAdapter != null && this.uiAdapter.rootComponent != null) {
            rebuildSlots(this.uiAdapter.rootComponent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
