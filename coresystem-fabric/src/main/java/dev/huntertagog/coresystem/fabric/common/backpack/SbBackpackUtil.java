package dev.huntertagog.coresystem.fabric.common.backpack;

import dev.huntertagog.coresystem.common.backpack.SbBackpackUuidUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SbBackpackUtil {

    private SbBackpackUtil() {
    }

    public static List<UUID> getCarriedBackpackUUIDs(ServerPlayerEntity player) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.addAll(player.getInventory().main);
        stacks.addAll(player.getInventory().armor);
        stacks.addAll(player.getInventory().offHand);

        List<UUID> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
            if (wrapper == null) {
                continue;
            }
            SbBackpackUuidUtil.getFromWrapper(wrapper).ifPresent(out::add);
        }
        return out;
    }
}
