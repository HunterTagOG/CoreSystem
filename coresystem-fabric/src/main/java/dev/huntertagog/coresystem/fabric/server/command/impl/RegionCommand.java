package dev.huntertagog.coresystem.fabric.server.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.permission.PermissionKeys;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.region.RegionFlag;
import dev.huntertagog.coresystem.common.region.RegionImageFacing;
import dev.huntertagog.coresystem.common.region.RegionImageMode;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.error.CoreErrorUtil;
import dev.huntertagog.coresystem.fabric.common.region.RegionImageDef;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import dev.huntertagog.coresystem.fabric.common.region.visual.RegionOutlineService;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.command.CommandUtil;
import dev.huntertagog.coresystem.fabric.server.command.CoreCommand;
import dev.huntertagog.coresystem.fabric.server.region.RegionDefinition;
import dev.huntertagog.coresystem.fabric.server.region.image.RegionImageServerService;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.literal;

@CommandMeta(
        value = "region",
        permission = PermissionKeys.REGION_ADMIN, // definiere dir einen passenden Key
        enabled = true
)
public final class RegionCommand extends BaseCommand implements CoreCommand {

    public RegionCommand() {
    }

    /**
     * In-Memory Selection pro Spieler
     */
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private record Selection(Identifier worldId, BlockPos pos1, BlockPos pos2) {
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher,
                         CommandRegistryAccess registryAccess,
                         CommandManager.RegistrationEnvironment env) {

        dispatcher.register(
                literal("region")
                        // /region pos1
                        .then(
                                CommandUtil.literalWithPermission(this, "pos1", CorePermission.REGION_ADMIN)
                                        .executes(this::executePos1)
                        )
                        // /region pos2
                        .then(
                                CommandUtil.literalWithPermission(this, "pos2", CorePermission.REGION_ADMIN)
                                        .executes(this::executePos2)
                        )
                        // /region create <id> <name...>
                        .then(
                                CommandUtil.literalWithPermission(this, "create", CorePermission.REGION_ADMIN)
                                        .then(CommandManager.argument("id", StringArgumentType.word())
                                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                        .executes(this::executeCreate)
                                                ))
                        )
                        // /region delete <id>
                        .then(
                                CommandUtil.literalWithPermission(this, "delete", CorePermission.REGION_ADMIN)
                                        .then(CommandManager.argument("id", StringArgumentType.word())
                                                .executes(this::executeDelete)
                                        )
                        )
                        // /region list
                        .then(
                                CommandUtil.literalWithPermission(this, "list", CorePermission.REGION_ADMIN)
                                        .executes(this::executeList)
                        )
                        // /region info <id>
                        .then(
                                CommandUtil.literalWithPermission(this, "info", CorePermission.REGION_ADMIN)
                                        .then(CommandManager.argument("id", StringArgumentType.word())
                                                .executes(this::executeInfo)
                                        )
                        )
                        // /region flag add/remove <id> <flag>
                        .then(
                                CommandUtil.literalWithPermission(this, "flag", CorePermission.REGION_ADMIN)
                                        .then(literal("add")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .then(CommandManager.argument("flag", StringArgumentType.word())
                                                                .executes(ctx -> executeFlagModify(ctx, true))
                                                        )
                                                )
                                        )
                                        .then(literal("remove")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .then(CommandManager.argument("flag", StringArgumentType.word())
                                                                .executes(ctx -> executeFlagModify(ctx, false))
                                                        )
                                                )
                                        )
                        )
                        // /region cmd add-enter <id> <command...>
                        // /region cmd add-leave <id> <command...>
                        // /region cmd clear-enter <id>
                        // /region cmd clear-leave <id>
                        .then(
                                CommandUtil.literalWithPermission(this, "cmd", CorePermission.REGION_ADMIN)
                                        .then(literal("add-enter")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                                .executes(ctx -> executeCmdAdd(ctx, true))
                                                        )
                                                )
                                        )
                                        .then(literal("add-leave")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                                .executes(ctx -> executeCmdAdd(ctx, false))
                                                        )
                                                )
                                        )
                                        .then(literal("clear-enter")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> executeCmdClear(ctx, true))
                                                )
                                        )
                                        .then(literal("clear-leave")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> executeCmdClear(ctx, false))
                                                )
                                        )
                        )
                        .then(
                                CommandUtil.literalWithPermission(this, "image", CorePermission.REGION_ADMIN)

                                        // /region image set <id> wall <facing> <imageKey> [baseY]
                                        .then(literal("set")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .then(literal("wall")
                                                                .then(CommandManager.argument("facing", StringArgumentType.word())
                                                                        .suggests((ctx, builder) -> {
                                                                            // no redis here:
                                                                            for (String option : List.of("NORTH", "SOUTH", "EAST", "WEST")) {
                                                                                builder.suggest(option);
                                                                            }
                                                                            return builder.buildFuture();
                                                                        })
                                                                        .then(CommandManager.argument("imageKey", StringArgumentType.word())
                                                                                .executes(this::executeImageSetWall)
                                                                        )
                                                                )
                                                        )

                                                        // /region image set <id> floor <baseY> <imageKey>
                                                        .then(literal("floor")
                                                                .then(CommandManager.argument("baseY", IntegerArgumentType.integer())
                                                                        .then(CommandManager.argument("imageKey", StringArgumentType.word())
                                                                                .executes(this::executeImageSetFloor)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )

                                        // /region image clear <id>
                                        .then(literal("clear")
                                                .then(CommandManager.argument("id", StringArgumentType.word())
                                                        .executes(this::executeImageClear)
                                                )
                                        )
                        )
        );
    }

    // ------------------------------------------------------------------------
    // /region pos1
    // ------------------------------------------------------------------------

    private int executePos1(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        BlockPos pos = player.getBlockPos();
        Identifier worldId = worldId(player.getWorld());

        Selection old = SELECTIONS.get(player.getUuid());
        BlockPos pos2 = old != null ? old.pos2() : pos;

        Selection sel = new Selection(worldId, pos, pos2);
        SELECTIONS.put(player.getUuid(), sel);

        // ✔ Outline anzeigen (kleiner Marker)
        RegionOutlineService outline = ServiceProvider.getService(RegionOutlineService.class);
        if (outline != null) {
            outline.showSelection(
                    player,
                    player.getWorld().getRegistryKey().getValue(),
                    pos,
                    pos,           // nur ein Block
                    20 * 5         // 5 Sekunden
            );
        }

        src.sendMessage(
                Text.literal("§a[Region] Pos1 gesetzt bei §e" + formatPos(pos) + " §7(" + worldId + ")")
        );
        return 1;
    }

    // ------------------------------------------------------------------------
    // /region pos2
    // ------------------------------------------------------------------------

    private int executePos2(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        BlockPos pos = player.getBlockPos();
        Identifier worldId = worldId(player.getWorld());

        Selection old = SELECTIONS.get(player.getUuid());
        BlockPos pos1 = old != null ? old.pos1() : pos;

        Selection sel = new Selection(worldId, pos1, pos);
        SELECTIONS.put(player.getUuid(), sel);

        // ✔ Outline für die komplette Box
        RegionOutlineService outline = ServiceProvider.getService(RegionOutlineService.class);
        if (outline != null) {
            outline.showSelection(
                    player,
                    player.getWorld().getRegistryKey().getValue(),
                    pos1,
                    pos,
                    20 * 5 // 5 Sekunden
            );
        }

        src.sendMessage(
                Text.literal("§a[Region] Pos2 gesetzt bei §e" + formatPos(pos) + " §7(" + worldId + ")")
        );
        return 1;
    }

    // ------------------------------------------------------------------------
    // /region create <id> <name...>
    // ------------------------------------------------------------------------

    private int executeCreate(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            Messages.send(src, CoreMessage.ONLY_PLAYERS);
            return 0;
        }

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            CoreErrorUtil.notify(src,
                    CoreError.of(CoreErrorCode.SERVICE_MISSING, CoreErrorSeverity.CRITICAL,
                                    "RegionService not available for /region create")
                            .withContextEntry("command", "region create")
            );
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        String name = StringArgumentType.getString(ctx, "name");

        Selection sel = SELECTIONS.get(player.getUuid());
        if (sel == null || sel.pos1() == null || sel.pos2() == null) {
            src.sendMessage(
                    Text.literal("§c[Region] Du musst zuerst §e/region pos1 §cund §e/region pos2 §csetzen.")
            );
            return 0;
        }

        // beide Punkte müssen in derselben Welt liegen
        Identifier worldId = sel.worldId();

        BlockPos a = sel.pos1();
        BlockPos b = sel.pos2();

        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());

        RegionDefinition def = new RegionDefinition(
                id,
                name,
                player.getUuid(),
                worldId,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                EnumSet.noneOf(RegionFlag.class),
                List.of(),
                List.of()
        );

        regions.registerRegion(def);

        // ✔ Outline der fertigen Region anzeigen
        RegionOutlineService outline = ServiceProvider.getService(RegionOutlineService.class);
        if (outline != null) {
            outline.showRegion(player, def, 20 * 8); // 8 Sekunden
        }

        src.sendMessage(
                Text.literal("§a[Region] Region §e" + id + " §aerstellt. Bereich: §7" +
                        minX + "," + minY + "," + minZ + " §8-> §7" + maxX + "," + maxY + "," + maxZ)
        );

        return 1;
    }

    // ------------------------------------------------------------------------
    // /region delete <id>
    // ------------------------------------------------------------------------

    private int executeDelete(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            CoreErrorUtil.notify(src,
                    CoreError.of(CoreErrorCode.SERVICE_MISSING, CoreErrorSeverity.CRITICAL,
                                    "RegionService not available for /region delete")
                            .withContextEntry("command", "region delete")
            );
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");

        Optional<RegionDefinition> existing = regions.findById(id);
        if (existing.isEmpty()) {
            src.sendMessage(
                    Text.literal("§c[Region] Unbekannte Region-ID: §e" + id)
            );
            return 0;
        }

        regions.removeRegion(id);

        src.sendMessage(
                Text.literal("§a[Region] Region §e" + id + " §awurde gelöscht.")
        );
        return 1;
    }

    // ------------------------------------------------------------------------
    // /region list
    // ------------------------------------------------------------------------

    private int executeList(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            CoreErrorUtil.notify(src,
                    CoreError.of(CoreErrorCode.SERVICE_MISSING, CoreErrorSeverity.CRITICAL,
                                    "RegionService not available for /region list")
                            .withContextEntry("command", "region list")
            );
            return 0;
        }

        List<RegionDefinition> all;
        try {
            all = regions.getAllRegions();
        } catch (UnsupportedOperationException ex) {
            // falls du getAllRegions noch nicht hast
            src.sendMessage(
                    Text.literal("§c[Region] getAllRegions() ist im aktuellen RegionService nicht implementiert.")
            );
            return 0;
        }

        if (all.isEmpty()) {
            src.sendMessage(
                    Text.literal("§7[Region] Es sind aktuell keine Regionen definiert.")
            );
            return 1;
        }

        src.sendMessage(
                Text.literal("§a[Region] Registrierte Regionen (§e" + all.size() + "§a):")
        );

        for (RegionDefinition r : all) {
            String flags = r.flags().isEmpty()
                    ? "-"
                    : r.flags().stream().map(Enum::name).collect(Collectors.joining(","));

            src.sendMessage(
                    Text.literal(" §7- §e" + r.id() + " §8| §f" + r.name() +
                            " §8| §7" + r.worldId() +
                            " §8| §7Flags: §b" + flags)
            );
        }

        return 1;
    }

    // ------------------------------------------------------------------------
    // /region info <id>
    // ------------------------------------------------------------------------

    private int executeInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            CoreErrorUtil.notify(src,
                    CoreError.of(CoreErrorCode.SERVICE_MISSING, CoreErrorSeverity.CRITICAL,
                                    "RegionService not available for /region info")
                            .withContextEntry("command", "region info")
            );
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        Optional<RegionDefinition> opt = regions.findById(id);

        if (opt.isEmpty()) {
            src.sendMessage(
                    Text.literal("§c[Region] Unbekannte Region-ID: §e" + id)
            );
            return 0;
        }

        RegionDefinition r = opt.get();

        String flags = r.flags().isEmpty()
                ? "-"
                : r.flags().stream().map(Enum::name).collect(Collectors.joining(", "));

        String enterCmds = r.onEnterCommands().isEmpty()
                ? "-"
                : String.join(" §8|| §7", r.onEnterCommands());

        String leaveCmds = r.onLeaveCommands().isEmpty()
                ? "-"
                : String.join(" §8|| §7", r.onLeaveCommands());

        src.sendMessage(
                Text.literal("§a[Region] Info zu §e" + r.id())
        );
        src.sendMessage(
                Text.literal(" §7Name: §f" + r.name())
        );
        src.sendMessage(
                Text.literal(" §7Welt: §f" + r.worldId())
        );
        src.sendMessage(
                Text.literal(" §7Bounds: §f" +
                        r.minX() + "," + r.minY() + "," + r.minZ() +
                        " §8-> §f" +
                        r.maxX() + "," + r.maxY() + "," + r.maxZ())
        );
        src.sendMessage(
                Text.literal(" §7Flags: §b" + flags)
        );
        src.sendMessage(
                Text.literal(" §7OnEnter-Cmds: §f" + enterCmds)
        );
        src.sendMessage(
                Text.literal(" §7OnLeave-Cmds: §f" + leaveCmds)
        );

        return 1;
    }

    // ------------------------------------------------------------------------
    // /region flag add/remove <id> <flag>
    // ------------------------------------------------------------------------

    private int executeFlagModify(CommandContext<ServerCommandSource> ctx, boolean add) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            CoreErrorUtil.notify(src,
                    CoreError.of(CoreErrorCode.SERVICE_MISSING, CoreErrorSeverity.CRITICAL,
                                    "RegionService not available for /region flag")
                            .withContextEntry("command", "region flag")
            );
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        String flagRaw = StringArgumentType.getString(ctx, "flag");

        Optional<RegionDefinition> opt = regions.findById(id);
        if (opt.isEmpty()) {
            src.sendMessage(
                    Text.literal("§c[Region] Unbekannte Region-ID: §e" + id)
            );
            return 0;
        }

        RegionFlag flag;
        try {
            flag = RegionFlag.valueOf(flagRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            src.sendMessage(
                    Text.literal("§c[Region] Unbekanntes Flag: §e" + flagRaw)
            );
            return 0;
        }

        RegionDefinition r = opt.get();
        Set<RegionFlag> newFlags = EnumSet.copyOf(r.flags());

        if (add) {
            newFlags.add(flag);
        } else {
            newFlags.remove(flag);
        }

        RegionDefinition updated = new RegionDefinition(
                r.id(),
                r.name(),
                r.ownerId(),
                r.worldId(),
                r.minX(), r.minY(), r.minZ(),
                r.maxX(), r.maxY(), r.maxZ(),
                newFlags,
                r.onEnterCommands(),
                r.onLeaveCommands()
        );

        regions.registerRegion(updated);

        src.sendMessage(
                Text.literal("§a[Region] Flag §e" + flag.name() + " §awurde " +
                        (add ? "§ahinzugefügt" : "§centfernt") +
                        " §7für Region §e" + id)
        );

        return 1;
    }

    // ------------------------------------------------------------------------
    // /region cmd add-enter|add-leave <id> <command...>
    // ------------------------------------------------------------------------

    private int executeCmdAdd(CommandContext<ServerCommandSource> ctx, boolean enter) {
        ServerCommandSource src = ctx.getSource();

        try {
            RegionService regions = ServiceProvider.getService(RegionService.class);
            if (regions == null) {
                src.sendMessage(Text.literal("§c[Region] RegionService nicht verfügbar."));
                return 0;
            }

            String id = StringArgumentType.getString(ctx, "id");
            String cmdRaw = StringArgumentType.getString(ctx, "command");
            String cmd = cmdRaw == null ? "" : cmdRaw.trim();

            if (cmd.isEmpty()) {
                src.sendMessage(Text.literal("§c[Region] Command darf nicht leer sein."));
                return 0;
            }

            Optional<RegionDefinition> opt = regions.findById(id);
            if (opt.isEmpty()) {
                src.sendMessage(Text.literal("§c[Region] Unbekannte Region-ID: §e" + id));
                return 0;
            }

            RegionDefinition r = opt.get();
            List<String> enterCmds = new ArrayList<>(r.onEnterCommands());
            List<String> leaveCmds = new ArrayList<>(r.onLeaveCommands());

            if (enter) enterCmds.add(cmd);
            else leaveCmds.add(cmd);

            RegionDefinition updated = new RegionDefinition(
                    r.id(), r.name(), r.ownerId(), r.worldId(),
                    r.minX(), r.minY(), r.minZ(),
                    r.maxX(), r.maxY(), r.maxZ(),
                    r.flags(),
                    enterCmds,
                    leaveCmds
            );

            regions.registerRegion(updated);

            src.sendMessage(Text.literal("§a[Region] Command für " +
                    (enter ? "§eOnEnter" : "§eOnLeave") + " §ahinzugefügt: §7" + cmd));

            return 1;

        } catch (Exception e) {
            // Business-taugliches Logging mit Kontext
            LoggerFactory.get("RegionCommand").error(
                    "Region cmd add failed. enter={} input='{}'",
                    enter, ctx.getInput(), e
            );

            src.sendMessage(Text.literal("§c[Region] Interner Fehler. Details im Server-Log."));
            return 0;
        }
    }

    // ------------------------------------------------------------------------
    // /region cmd clear-enter|clear-leave <id>
    // ------------------------------------------------------------------------

    private int executeCmdClear(CommandContext<ServerCommandSource> ctx, boolean enter) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            CoreErrorUtil.notify(src,
                    CoreError.of(CoreErrorCode.SERVICE_MISSING, CoreErrorSeverity.CRITICAL,
                                    "RegionService not available for /region cmd clear-*")
                            .withContextEntry("command", "region cmd clear")
            );
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");

        Optional<RegionDefinition> opt = regions.findById(id);
        if (opt.isEmpty()) {
            src.sendMessage(
                    Text.literal("§c[Region] Unbekannte Region-ID: §e" + id)
            );
            return 0;
        }

        RegionDefinition r = opt.get();

        List<String> enterCmds = enter ? List.of() : r.onEnterCommands();
        List<String> leaveCmds = enter ? r.onLeaveCommands() : List.of();

        RegionDefinition updated = new RegionDefinition(
                r.id(),
                r.name(),
                r.ownerId(),
                r.worldId(),
                r.minX(), r.minY(), r.minZ(),
                r.maxX(), r.maxY(), r.maxZ(),
                r.flags(),
                enterCmds,
                leaveCmds
        );

        regions.registerRegion(updated);

        src.sendMessage(
                Text.literal("§a[Region] " +
                        (enter ? "OnEnter" : "OnLeave") +
                        "-Commands für Region §e" + id + " §awurden geleert.")
        );

        return 1;
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static Identifier worldId(World world) {
        return world.getRegistryKey().getValue();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ";" + pos.getY() + ";" + pos.getZ();
    }

    private static String normalizeTextureId(String raw) {
        if (raw == null) return "";
        raw = raw.trim();

        var id = Identifier.tryParse(raw);
        if (id != null) return id.getPath(); // drop namespace

        return raw.replace(".png", "");
    }

    private static ServerWorld resolveRegionWorld(ServerCommandSource src, RegionDefinition region) {
        var server = src.getServer();
        if (server == null) return null;
        var worldKey = RegistryKey.of(RegistryKeys.WORLD, region.worldId());
        return server.getWorld(worldKey);
    }

    // ------------------------------------------------------------------------
    // /region image set <id> <imageKey> <mode> [yaw]
    // ------------------------------------------------------------------------

    private int executeImageSetWall(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            src.sendMessage(Text.literal("§c[Region] RegionService nicht verfügbar."));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        RegionDefinition region = regions.findById(id).orElse(null);
        if (region == null) {
            src.sendMessage(Text.literal("§c[Region] Unbekannte Region: §e" + id));
            return 0;
        }

        RegionImageFacing facing = RegionImageFacing.valueOf(StringArgumentType.getString(ctx, "facing").toUpperCase(Locale.ROOT));
        if (!facing.name().matches("NORTH|SOUTH|EAST|WEST")) {
            src.sendMessage(Text.literal("§c[Region] Facing muss north|south|east|west sein."));
            return 0;
        }

        String texture = normalizeTextureId(StringArgumentType.getString(ctx, "imageKey"));

        RegionImageDef def = new RegionImageDef(
                region.id(),
                region.worldId(),
                new Box(
                        region.minX(), region.minY(), region.minZ(),
                        region.maxX(), region.maxY(), region.maxZ()
                ),
                RegionImageMode.WALL,
                facing,
                0,              // baseY irrelevant bei WALL
                texture
        );

        ServerWorld world = resolveRegionWorld(src, region);
        RegionImageServerService.upsertAndBroadcast(world, def);

        src.sendMessage(Text.literal(
                "§a[Region] Image gesetzt (WALL) §e" + id +
                        " §7facing=§f" + facing +
                        " §7tex=§f" + texture
        ));
        return 1;
    }

    private int executeImageSetFloor(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            src.sendMessage(Text.literal("§c[Region] RegionService nicht verfügbar."));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        RegionDefinition region = regions.findById(id).orElse(null);
        if (region == null) {
            src.sendMessage(Text.literal("§c[Region] Unbekannte Region: §e" + id));
            return 0;
        }

        int baseY = IntegerArgumentType.getInteger(ctx, "baseY");
        String textureId = normalizeTextureId(StringArgumentType.getString(ctx, "imageKey"));

        RegionImageDef def = new RegionImageDef(
                region.id(),
                region.worldId(),
                new Box(
                        region.minX(), region.minY(), region.minZ(),
                        region.maxX(), region.maxY(), region.maxZ()
                ),
                RegionImageMode.FLOOR,
                RegionImageFacing.NORTH,    // irrelevant bei FLOOR
                baseY,
                textureId
        );

        ServerWorld world = resolveRegionWorld(src, region);
        RegionImageServerService.upsertAndBroadcast(world, def);

        src.sendMessage(Text.literal(
                "§a[Region] Image gesetzt (FLOOR) §e" + id +
                        " §7baseY=§f" + baseY +
                        " §7tex=§f" + textureId
        ));
        return 1;
    }

    private int executeImageClear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            src.sendMessage(Text.literal("§c[Region] RegionService nicht verfügbar."));
            return 0;
        }

        String id = StringArgumentType.getString(ctx, "id");
        RegionDefinition region = regions.findById(id).orElse(null);
        if (region == null) {
            src.sendMessage(Text.literal("§c[Region] Unbekannte Region: §e" + id));
            return 0;
        }

        ServerWorld world = resolveRegionWorld(src, region);
        RegionImageServerService.removeAndBroadcast(world, id);

        src.sendMessage(Text.literal("§a[Region] Image entfernt für §e" + id));
        return 1;
    }
}
