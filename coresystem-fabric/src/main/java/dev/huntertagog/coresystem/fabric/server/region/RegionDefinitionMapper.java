package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.region.RegionDefinitionDto;
import dev.huntertagog.coresystem.common.region.RegionFlag;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class RegionDefinitionMapper {

    private RegionDefinitionMapper() {
    }

    static RegionDefinitionDto toDto(RegionDefinition region) {
        Set<String> flagNames = region.flags().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new RegionDefinitionDto(
                region.id(),
                region.name(),
                region.ownerId(),
                region.worldId().toString(),
                region.minX(),
                region.minY(),
                region.minZ(),
                region.maxX(),
                region.maxY(),
                region.maxZ(),
                flagNames,
                region.onEnterCommands(),
                region.onLeaveCommands()
        );
    }

    static RegionDefinition fromDto(RegionDefinitionDto dto) {
        Identifier worldId = Identifier.of(dto.world());

        Set<RegionFlag> flags = EnumSet.noneOf(RegionFlag.class);
        if (dto.flags() != null) {
            for (String f : dto.flags()) {
                try {
                    flags.add(RegionFlag.valueOf(f));
                } catch (IllegalArgumentException ignored) {
                    // Unbekannter Flag -> ignorieren
                }
            }
        }

        return new RegionDefinition(
                dto.id(),
                dto.name(),
                dto.ownerId(),
                worldId,
                dto.minX(),
                dto.minY(),
                dto.minZ(),
                dto.maxX(),
                dto.maxY(),
                dto.maxZ(),
                flags,
                dto.onEnterCommands() != null ? dto.onEnterCommands() : List.of(),
                dto.onLeaveCommands() != null ? dto.onLeaveCommands() : List.of()
        );
    }
}
