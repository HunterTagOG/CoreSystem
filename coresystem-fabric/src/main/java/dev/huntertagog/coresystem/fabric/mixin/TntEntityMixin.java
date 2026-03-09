package dev.huntertagog.coresystem.fabric.mixin;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import dev.huntertagog.coresystem.fabric.server.protection.WorldProtectionService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntEntity.class)
public class TntEntityMixin {

    /**
     * Intercept TNT explosion BEFORE it happens.
     */
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void onExplode(CallbackInfo ci) {
        TntEntity self = (TntEntity) (Object) this;
        World world = self.getWorld();

        WorldProtectionService protection = ServiceProvider.getService(WorldProtectionService.class);
        if (protection == null || !protection.isTntBlocked(world)) {
            return; // Explosion normal erlauben
        }

        // Wer hat es gezündet? -> Owner vom TNT
        Entity owner = self.getOwner();
        if (owner instanceof PlayerEntity player) {
            // Sound für Spieler abspielen
            player.playSound(
                    SoundEvents.ENTITY_VILLAGER_NO,
                    1.0f,
                    1.0f
            );
            player.sendMessage(Messages.t(CoreMessage.PROTECT_TNT_BLOCKED), false);
        }

        // Explosion canceln – kein Schaden, keine Blöcke ändern
        ci.cancel();
    }
}
