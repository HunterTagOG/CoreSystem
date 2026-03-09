package dev.huntertagog.coresystem.fabric.mixin;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.server.islands.PrivateIslandWorldNodeConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(LevelStorage.Session.class)
public abstract class LevelStorageSessionMixin {

    @Unique
    private static final Logger LOG = LoggerFactory.get("WorldStorage");

    @Inject(
            method = "getWorldDirectory(Lnet/minecraft/registry/RegistryKey;)Ljava/nio/file/Path;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void coresystem$redirectWorldPath(RegistryKey<World> key, CallbackInfoReturnable<Path> cir) {
        // Nur unsere eigenen Island-Welten umleiten
        if (!PrivateIslandWorldNodeConfig.isIslandWorldKey(key)) {
            return; // Vanilla-/Fremd-Welten laufen über die normale Mojang-Logik
        }

        Path base = PrivateIslandWorldNodeConfig.getIslandsBasePath();
        Path customPath = PrivateIslandWorldNodeConfig.resolveIslandPath(base, key);

        try {
            Files.createDirectories(customPath);
        } catch (Exception e) {
            LOG.warn("Failed to create world directory '{}' for dimension '{}': {}",
                    customPath, key.getValue(), e.getMessage());
        }

        LOG.debug("Routing ISLAND world directory for dimension '{}' to '{}'.",
                key.getValue(), customPath);

        cir.setReturnValue(customPath);
    }
}
