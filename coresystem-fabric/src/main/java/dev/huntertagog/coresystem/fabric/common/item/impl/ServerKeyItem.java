package dev.huntertagog.coresystem.fabric.common.item.impl;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.model.ServerTarget;
import dev.huntertagog.coresystem.common.model.ServerTargets;
import dev.huntertagog.coresystem.fabric.server.net.ServerSwitcherNetworking;
import dev.huntertagog.coresystem.fabric.server.permission.PermissionCache;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ServerKeyItem extends Item implements GeoItem {

    private static final Logger LOG = LoggerFactory.get("ServerKeyItem");

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.model.crystal");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ServerKeyItem(Settings settings) {
        super(settings);
        // sorgt für serverseitiges Syncing der Animationen
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.coresystem.server_key.lore_1"));
        tooltip.add(Text.translatable("item.coresystem.server_key.lore_2"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "Idle",
                0,
                state -> {
                    state.setAnimation(IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ---- Rechtsklick-Logik (Use) ----
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            try {
                var level = PermissionCache.getLevel(player);
                List<ServerTarget> visibleTargets = ServerTargets.baseTargets(level);
                boolean adminMode = PermissionCache.isAdmin(player);

                // hier wie gehabt das GUI öffnen
                ServerSwitcherNetworking.sendOpenMenu(player, visibleTargets, adminMode);

            } catch (Exception e) {
                CoreError error = CoreError.of(
                                CoreErrorCode.SERVER_SWITCHER_OPEN_FAILED,
                                CoreErrorSeverity.ERROR,
                                "Failed to open server switcher menu from ServerKeyItem."
                        )
                        .withCause(e)
                        .withContextEntry("player", user.getName().getString());

                LOG.error(error.toLogString(), e);
            }
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
