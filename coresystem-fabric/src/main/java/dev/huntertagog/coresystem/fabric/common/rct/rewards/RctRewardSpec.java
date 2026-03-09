package dev.huntertagog.coresystem.fabric.common.rct.rewards;

import net.minecraft.item.ItemStack;

import java.util.List;

public record RctRewardSpec(
        int money,                 // optional: CobbleDollars o.ä.
        List<ItemStack> items,     // optional
        List<String> consoleCommands, // optional, {player} placeholder
        List<String> permissionNodes  // optional, wenn du ein Perm-System “grant” hast
) {
    public static RctRewardSpec empty() {
        return new RctRewardSpec(0, List.of(), List.of(), List.of());
    }
}
