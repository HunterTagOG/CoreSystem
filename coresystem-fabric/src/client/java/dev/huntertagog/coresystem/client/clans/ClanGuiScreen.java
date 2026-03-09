package dev.huntertagog.coresystem.client.clans;

import dev.huntertagog.coresystem.fabric.common.clans.gui.ClanGuiPackets;
import dev.huntertagog.coresystem.fabric.common.clans.gui.ClanGuiSnapshot;
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

public final class ClanGuiScreen extends BaseOwoScreen<FlowLayout> {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private ClanGuiSnapshot snapshot;

    private StackLayout contentStack;
    private Component activePanel;

    private FlowLayout membersContent;
    private FlowLayout rolesContent;

    private CheckboxComponent openInvites;
    private CheckboxComponent friendlyFire;
    private CheckboxComponent clanChatDefault;

    public void applySnapshot(ClanGuiSnapshot snapshot) {
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

        String title = (snapshot != null && snapshot.inClan())
                ? ("Clan: [" + snapshot.tag() + "] " + snapshot.name())
                : "Clan";

        root.child(
                Components.label(Text.literal(title))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .sizing(Sizing.fill(100), Sizing.fixed(18))
        );

        FlowLayout body = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        body.gap(10);

        FlowLayout tabs = Containers.verticalFlow(Sizing.fixed(140), Sizing.fill(100));
        tabs.gap(6);

        Component membersPanel = buildMembersPanel().id("panel_members");
        Component rolesPanel = buildRolesPanel().id("panel_roles");
        Component settingsPanel = buildSettingsPanel().id("panel_settings");

        contentStack = Containers.stack(Sizing.fill(100), Sizing.fill(100));
        activePanel = membersPanel;
        contentStack.child(activePanel);

        tabs.child(Components.button(Text.literal("Members"), b -> switchPanel(membersPanel)));
        tabs.child(Components.button(Text.literal("Roles"), b -> switchPanel(rolesPanel)));
        tabs.child(Components.button(Text.literal("Settings"), b -> switchPanel(settingsPanel)));

        body.child(tabs);
        body.child(contentStack);
        root.child(body);

        if (snapshot != null) rebuildAll();
    }

    private void switchPanel(Component next) {
        if (contentStack == null || next == null) return;
        if (activePanel != null) contentStack.removeChild(activePanel);
        activePanel = next;
        contentStack.child(activePanel);
    }

    private Component buildMembersPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.gap(6);

        membersContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        membersContent.gap(4);

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(
                Sizing.fill(100), Sizing.fill(100), membersContent
        );

        panel.child(scroll);
        return panel;
    }

    private Component buildRolesPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.gap(6);

        rolesContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        rolesContent.gap(6);

        panel.child(Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), rolesContent));
        return panel;
    }

    private Component buildSettingsPanel() {
        FlowLayout panel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        panel.gap(8);

        openInvites = Components.checkbox(Text.literal("Open invites"));
        friendlyFire = Components.checkbox(Text.literal("Friendly fire"));
        clanChatDefault = Components.checkbox(Text.literal("Clan chat default"));

        ButtonComponent save = Components.button(Text.literal("Save Settings"), b -> pushSettings());

        panel.child(openInvites);
        panel.child(friendlyFire);
        panel.child(clanChatDefault);
        panel.child(save);

        return panel;
    }

    private void rebuildAll() {
        rebuildMembers();
        rebuildRoles();
        applySettings();
    }

    private void rebuildMembers() {
        if (membersContent == null) return;
        membersContent.clearChildren();

        if (snapshot == null || !snapshot.inClan()) {
            membersContent.child(Components.label(Text.literal("You are not in a clan.")).color(Color.ofRgb(0xA0A0A0)));
            return;
        }

        for (var m : snapshot.members()) {
            membersContent.child(memberRow(m));
        }
    }

    private Component memberRow(ClanGuiSnapshot.MemberEntry m) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(32));
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(8);

        row.child(playerHead(m.uuid(), 20));

        Text name = Text.literal(m.name()).formatted(m.online() ? Formatting.GREEN : Formatting.GRAY);

        String lastSeen = "-";
        if (!m.online() && m.lastSeenEpochMillis() > 0) {
            lastSeen = TS.format(Instant.ofEpochMilli(m.lastSeenEpochMillis()));
        }

        FlowLayout textCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(32));
        textCol.child(Components.label(name).sizing(Sizing.fill(100), Sizing.fixed(12)));
        textCol.child(Components.label(Text.literal("Role: " + m.roleId() + " | Last: " + lastSeen))
                .color(Color.ofRgb(0xA0A0A0))
                .sizing(Sizing.fill(100), Sizing.fixed(12)));

        row.child(textCol);

        boolean canManageMembers = snapshot.canManageMembers();
        boolean canManageRoles = snapshot.canManageRoles();

        if (canManageRoles) {
            row.child(Components.button(Text.literal("Promote"), b -> sendSetRole(m.uuid(), "ADMIN"))
                    .sizing(Sizing.fixed(70), Sizing.fixed(20)));
            row.child(Components.button(Text.literal("Member"), b -> sendSetRole(m.uuid(), "MEMBER"))
                    .sizing(Sizing.fixed(70), Sizing.fixed(20)));
        }

        if (canManageMembers) {
            row.child(Components.button(Text.literal("Kick"), b -> sendKick(m.uuid()))
                    .sizing(Sizing.fixed(55), Sizing.fixed(20)));
        }

        return row;
    }

    private void rebuildRoles() {
        if (rolesContent == null) return;
        rolesContent.clearChildren();

        if (snapshot == null || !snapshot.inClan()) {
            rolesContent.child(Components.label(Text.literal("No clan roles available.")).color(Color.ofRgb(0xA0A0A0)));
            return;
        }

        if (!snapshot.canManageRoles()) {
            rolesContent.child(Components.label(Text.literal("You don't have permission to edit roles."))
                    .color(Color.ofRgb(0xA0A0A0)));
            return;
        }

        for (var role : snapshot.roles()) {
            rolesContent.child(roleBox(role));
        }
    }

    private Component roleBox(ClanGuiSnapshot.RoleEntry role) {
        FlowLayout box = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        box.gap(6);
        box.padding(Insets.of(6));

        box.child(Components.label(Text.literal(role.displayName() + " (" + role.roleId() + ")"))
                .color(Color.ofRgb(0xFFFFFF)));

        // Minimal set an permissions – du kannst das beliebig erweitern
        box.child(permissionToggle(role, "CLAN_INVITE"));
        box.child(permissionToggle(role, "CLAN_KICK"));
        box.child(permissionToggle(role, "CLAN_SETTINGS"));
        box.child(permissionToggle(role, "CLAN_ROLES"));

        return box;
    }

    private Component permissionToggle(ClanGuiSnapshot.RoleEntry role, String permKey) {
        boolean enabled = role.permissions().contains(permKey);
        CheckboxComponent cb = Components.checkbox(Text.literal(permKey));
        cb.checked(enabled);

        cb.onChanged(checked -> sendRolePerm(role.roleId(), permKey, checked));

        return cb;
    }

    private void applySettings() {
        if (snapshot == null || openInvites == null) return;
        openInvites.checked(snapshot.settings().openInvites());
        friendlyFire.checked(snapshot.settings().friendlyFire());
        clanChatDefault.checked(snapshot.settings().clanChatDefault());
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
    // Head rendering via textures (no custom component)
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
            if (entry != null) return entry.getSkinTextures().texture();
        }
        return DefaultSkinHelper.getTexture(); // fallback (Steve/Alex)
    }

    // ------------------------------------------------------------
    // C2S actions (payload-based)
    // ------------------------------------------------------------

    private void sendKick(UUID target) {
        ClientPlayNetworking.send(new ClanGuiPackets.C2SKick(target));
    }

    private void sendSetRole(UUID target, String roleId) {
        ClientPlayNetworking.send(new ClanGuiPackets.C2SSetRole(target, roleId));
    }

    private void pushSettings() {
        if (openInvites == null) return;
        boolean openInv = readChecked(openInvites);
        boolean friFire = readChecked(friendlyFire);
        boolean ccDefault = readChecked(clanChatDefault);
        ClientPlayNetworking.send(new ClanGuiPackets.C2SSetSettings(
                openInv,
                friFire,
                ccDefault
        ));
    }

    private void sendRolePerm(String roleId, String permKey, boolean enabled) {
        ClientPlayNetworking.send(new ClanGuiPackets.C2SSetRolePermission(roleId, permKey, enabled));
    }

    @Override
    public void close() {
        super.close();
        ClanGuiClientNet.clearCurrent();
    }
}
