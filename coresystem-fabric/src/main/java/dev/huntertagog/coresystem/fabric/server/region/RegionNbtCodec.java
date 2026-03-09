package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.region.RegionDefinitionDto;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

final class RegionNbtCodec {

    private static final String K_ID = "id";
    private static final String K_NAME = "name";
    private static final String K_OWNER = "ownerId";
    private static final String K_WORLD = "world";

    private static final String K_MIN_X = "minX";
    private static final String K_MIN_Y = "minY";
    private static final String K_MIN_Z = "minZ";
    private static final String K_MAX_X = "maxX";
    private static final String K_MAX_Y = "maxY";
    private static final String K_MAX_Z = "maxZ";

    private static final String K_FLAGS = "flags";
    private static final String K_ON_ENTER = "onEnter";
    private static final String K_ON_LEAVE = "onLeave";

    private RegionNbtCodec() {
    }

    static NbtCompound writeDto(RegionDefinitionDto dto) {
        if (dto == null) return null;

        NbtCompound n = getNbtCompound(dto);

        // flags (Set<String>)
        NbtList flags = new NbtList();
        if (dto.flags() != null) {
            for (String f : dto.flags()) {
                if (f != null && !f.isBlank()) flags.add(NbtString.of(f));
            }
        }
        n.put(K_FLAGS, flags);

        // onEnterCommands
        NbtList enter = new NbtList();
        if (dto.onEnterCommands() != null) {
            for (String cmd : dto.onEnterCommands()) {
                if (cmd != null && !cmd.isBlank()) enter.add(NbtString.of(cmd));
            }
        }
        n.put(K_ON_ENTER, enter);

        // onLeaveCommands
        NbtList leave = new NbtList();
        if (dto.onLeaveCommands() != null) {
            for (String cmd : dto.onLeaveCommands()) {
                if (cmd != null && !cmd.isBlank()) leave.add(NbtString.of(cmd));
            }
        }
        n.put(K_ON_LEAVE, leave);

        return n;
    }

    private static @NotNull NbtCompound getNbtCompound(RegionDefinitionDto dto) {
        NbtCompound n = new NbtCompound();

        n.putString(K_ID, safe(dto.id()));
        n.putString(K_NAME, safe(dto.name()));

        if (dto.ownerId() != null) {
            n.putString(K_OWNER, dto.ownerId().toString());
        }

        n.putString(K_WORLD, safe(dto.world()));

        n.putInt(K_MIN_X, dto.minX());
        n.putInt(K_MIN_Y, dto.minY());
        n.putInt(K_MIN_Z, dto.minZ());
        n.putInt(K_MAX_X, dto.maxX());
        n.putInt(K_MAX_Y, dto.maxY());
        n.putInt(K_MAX_Z, dto.maxZ());
        return n;
    }

    static RegionDefinitionDto readDto(NbtCompound n) {
        if (n == null) return null;

        String id = n.getString(K_ID);
        if (id == null || id.isBlank()) return null;

        String name = n.getString(K_NAME);
        String world = n.getString(K_WORLD);

        UUID ownerId = null;
        if (n.contains(K_OWNER, NbtElement.STRING_TYPE)) {
            String ownerRaw = n.getString(K_OWNER);
            if (ownerRaw != null && !ownerRaw.isBlank()) {
                try {
                    ownerId = UUID.fromString(ownerRaw);
                } catch (Exception ignored) {
                }
            }
        }

        int minX = n.getInt(K_MIN_X);
        int minY = n.getInt(K_MIN_Y);
        int minZ = n.getInt(K_MIN_Z);
        int maxX = n.getInt(K_MAX_X);
        int maxY = n.getInt(K_MAX_Y);
        int maxZ = n.getInt(K_MAX_Z);

        Set<String> flags = new HashSet<>();
        if (n.contains(K_FLAGS, NbtElement.LIST_TYPE)) {
            // WICHTIG: type ist Element-Type-ID (Strings => NbtElement.STRING_TYPE)
            NbtList list = n.getList(K_FLAGS, NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                String f = list.getString(i);
                if (f != null && !f.isBlank()) flags.add(f);
            }
        }

        List<String> onEnter = new ArrayList<>();
        if (n.contains(K_ON_ENTER, NbtElement.LIST_TYPE)) {
            NbtList list = n.getList(K_ON_ENTER, NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                if (s != null && !s.isBlank()) onEnter.add(s);
            }
        }

        List<String> onLeave = new ArrayList<>();
        if (n.contains(K_ON_LEAVE, NbtElement.LIST_TYPE)) {
            NbtList list = n.getList(K_ON_LEAVE, NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                if (s != null && !s.isBlank()) onLeave.add(s);
            }
        }

        return new RegionDefinitionDto(
                id,
                name,
                ownerId,
                world,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                flags,
                onEnter,
                onLeave
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
