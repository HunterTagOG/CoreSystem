package dev.huntertagog.coresystem.client.friends;

import dev.huntertagog.coresystem.fabric.common.friends.gui.FriendGuiPackets;
import dev.huntertagog.coresystem.fabric.common.friends.gui.FriendGuiSnapshot;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class FriendGuiScreen extends BaseOwoScreen<FlowLayout> {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private FriendGuiSnapshot snapshot;

    private StackLayout contentStack;

    private FlowLayout friendsContent;
    private FlowLayout incomingContent;
    private FlowLayout outgoingContent;

    private CheckboxComponent allowRequests;
    private CheckboxComponent allowFollow;
    private CheckboxComponent showLastSeen;

    private Component activePanel;

    public void applySnapshot(FriendGuiSnapshot snapshot) {
        this.snapshot = snapshot;
        if (this.uiAdapter != null) rebuildAll();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.sizing(Sizing.fill(100), Sizing.fill(100));
        root.padding(Insets.of(10));
        root.gap(10);

        root.child(
                Components.label(Text.literal("Friends"))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .sizing(Sizing.fill(100), Sizing.fixed(18))
        );

        FlowLayout body = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        body.gap(10);

        // Tabs
        FlowLayout tabs = Containers.verticalFlow(Sizing.fixed(120), Sizing.fill(100));
        tabs.gap(6);

        // Content stack
        contentStack = Containers.stack(Sizing.fill(100), Sizing.fill(100));
        contentStack.id("content");

        Component friendsPanel = buildFriendsPanel().id("panel_friends");
        Component requestsPanel = buildRequestsPanel().id("panel_requests");
        Component settingsPanel = buildSettingsPanel().id("panel_settings");

        // Default: friends anzeigen
        activePanel = friendsPanel;
        contentStack.child(activePanel);

        // Tabs
        tabs.child(Components.button(Text.literal("Friends"), b -> switchPanel(friendsPanel)));
        tabs.child(Components.button(Text.literal("Requests"), b -> switchPanel(requestsPanel)));
        tabs.child(Components.button(Text.literal("Settings"), b -> switchPanel(settingsPanel)));

        body.child(tabs);
        body.child(contentStack);

        root.child(body);

        if (snapshot != null) rebuildAll();
    }

    private void switchPanel(Component next) {
        if (contentStack == null || next == null) return;

        if (activePanel != null) {
            contentStack.removeChild(activePanel);
        }
        activePanel = next;
        contentStack.child(activePanel);
    }

    private Component buildFriendsPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.gap(6);

        friendsContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        friendsContent.gap(4);

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.fill(100),
                friendsContent
        );

        panel.child(scroll);
        return panel;
    }

    private Component buildRequestsPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.gap(10);

        panel.child(Components.label(Text.literal("Incoming")).color(Color.ofRgb(0xA0A0A0)));

        incomingContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        incomingContent.gap(4);

        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(160), incomingContent));

        panel.child(Components.label(Text.literal("Outgoing")).color(Color.ofRgb(0xA0A0A0)));

        outgoingContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        outgoingContent.gap(4);

        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(160), outgoingContent));

        return panel;
    }

    private Component buildSettingsPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.gap(8);

        allowRequests = Components.checkbox(Text.literal("Allow friend requests"));
        allowFollow = Components.checkbox(Text.literal("Allow follow"));
        showLastSeen = Components.checkbox(Text.literal("Show last seen"));

        ButtonComponent save = Components.button(Text.literal("Save"), b -> pushSettings());

        panel.child(allowRequests);
        panel.child(allowFollow);
        panel.child(showLastSeen);
        panel.child(save);

        return panel;
    }

    private void rebuildAll() {
        rebuildFriends();
        rebuildRequests();
        applySettings();
    }

    private void rebuildFriends() {
        if (friendsContent == null) return;
        friendsContent.clearChildren();

        if (snapshot == null || snapshot.friends().isEmpty()) {
            friendsContent.child(Components.label(Text.literal("No friends yet.")).color(Color.ofRgb(0xA0A0A0)));
            return;
        }

        for (var f : snapshot.friends()) {
            friendsContent.child(friendRow(f));
        }
    }

    private Component friendRow(FriendGuiSnapshot.FriendEntry f) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(30));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(8);

        row.child(playerHead(f.uuid(), 20));

        String status = f.online() ? "Online" : "Offline";
        Text name = Text.literal(f.name()).formatted(f.online() ? Formatting.GREEN : Formatting.GRAY);

        String lastSeen = "-";
        if (!f.online() && snapshot.settings().showLastSeen() && f.lastSeenEpochMillis() > 0) {
            lastSeen = TS.format(Instant.ofEpochMilli(f.lastSeenEpochMillis()));
        }

        FlowLayout textCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(30));
        textCol.child(Components.label(name).sizing(Sizing.fill(100), Sizing.fixed(12)));
        textCol.child(Components.label(Text.literal(status + " | Last: " + lastSeen))
                .color(Color.ofRgb(0xA0A0A0))
                .sizing(Sizing.fill(100), Sizing.fixed(12)));

        row.child(textCol);

        ButtonComponent remove = Components.button(Text.literal("Remove"), b -> sendRemove(f.uuid()));
        remove.sizing(Sizing.fixed(70), Sizing.fixed(20));
        row.child(remove);

        return row;
    }

    private void rebuildRequests() {
        if (incomingContent == null || outgoingContent == null) return;

        incomingContent.clearChildren();
        outgoingContent.clearChildren();

        if (snapshot == null) return;

        if (snapshot.incoming().isEmpty()) {
            incomingContent.child(Components.label(Text.literal("No incoming requests.")).color(Color.ofRgb(0xA0A0A0)));
        } else {
            for (var r : snapshot.incoming()) incomingContent.child(incomingRow(r));
        }

        if (snapshot.outgoing().isEmpty()) {
            outgoingContent.child(Components.label(Text.literal("No outgoing requests.")).color(Color.ofRgb(0xA0A0A0)));
        } else {
            for (var r : snapshot.outgoing()) outgoingContent.child(outgoingRow(r));
        }
    }

    private Component incomingRow(FriendGuiSnapshot.RequestEntry r) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(8);

        row.child(playerHead(r.uuid(), 18));
        row.child(Components.label(Text.literal(r.name())).sizing(Sizing.fill(100), Sizing.fixed(18)));

        ButtonComponent accept = Components.button(Text.literal("Accept"), b -> sendAccept(r.uuid()));
        ButtonComponent deny = Components.button(Text.literal("Deny"), b -> sendDeny(r.uuid()));
        accept.sizing(Sizing.fixed(60), Sizing.fixed(18));
        deny.sizing(Sizing.fixed(50), Sizing.fixed(18));

        row.child(accept);
        row.child(deny);

        return row;
    }

    private Component outgoingRow(FriendGuiSnapshot.RequestEntry r) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(8);

        row.child(playerHead(r.uuid(), 18));
        row.child(Components.label(Text.literal(r.name())).sizing(Sizing.fill(100), Sizing.fixed(18)));

        ButtonComponent cancel = Components.button(Text.literal("Cancel"), b -> sendCancel(r.uuid()));
        cancel.sizing(Sizing.fixed(60), Sizing.fixed(18));
        row.child(cancel);

        return row;
    }

    private void applySettings() {
        if (snapshot == null || allowRequests == null) return;
        allowRequests.checked(snapshot.settings().allowRequests());
        allowFollow.checked(snapshot.settings().allowFollow());
        showLastSeen.checked(snapshot.settings().showLastSeen());
    }

    // ----------------------------------------
    // Checkbox → boolean (OWO versionssicher)
    // ----------------------------------------
    private static boolean readChecked(CheckboxComponent box) {
        try {
            // neuere OWO-Versionen
            return (boolean) box.getClass().getMethod("checked").invoke(box);
        } catch (Exception ignored) {
        }

        try {
            // ältere OWO-Versionen
            return (boolean) box.getClass().getMethod("isChecked").invoke(box);
        } catch (Exception ignored) {
        }

        try {
            // Fallback
            return (boolean) box.getClass().getMethod("value").invoke(box);
        } catch (Exception ignored) {
        }

        return false;
    }

    // ------------------------------------------------------------
    // Head as textures (no custom component, no sizing-issues)
    // ------------------------------------------------------------

    private Component playerHead(UUID uuid, int sizePx) {
        Identifier skin = resolveSkin(uuid);

        var head = Components.texture(skin, 8, 8, 8, 8, 64, 64);
        var hat = Components.texture(skin, 40, 8, 8, 8, 64, 64);

        StackLayout box = Containers.stack(Sizing.fixed(sizePx), Sizing.fixed(sizePx));
        box.child(head);
        box.child(hat);
        return box;
    }

    private Identifier resolveSkin(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry != null) {
                return entry.getSkinTextures().texture();
            }
        }
        // deine Mapping-Version hat offenbar nur getTexture() ohne UUID → nimm Default (Steve/Alex)
        return DefaultSkinHelper.getTexture();
    }

    // ------------------------------------------------------------
    // New payload networking (ClientPlayNetworking.send(CustomPayload))
    // ------------------------------------------------------------

    private void sendAccept(UUID from) {
        ClientPlayNetworking.send(new FriendGuiPackets.C2SAccept(from));
    }

    private void sendDeny(UUID from) {
        ClientPlayNetworking.send(new FriendGuiPackets.C2SDeny(from));
    }

    private void sendRemove(UUID other) {
        ClientPlayNetworking.send(new FriendGuiPackets.C2SRemove(other));
    }

    private void sendCancel(UUID target) {
        ClientPlayNetworking.send(new FriendGuiPackets.C2SCancel(target));
    }

    private void pushSettings() {
        boolean allowReq = readChecked(allowRequests);
        boolean allowFol = readChecked(allowFollow);
        boolean showLast = readChecked(showLastSeen);

        ClientPlayNetworking.send(
                new FriendGuiPackets.C2SSetSettings(
                        allowReq,
                        allowFol,
                        showLast
                )
        );
    }

    @Override
    public void close() {
        super.close();
        FriendGuiClientNet.clearCurrent();
    }
}
