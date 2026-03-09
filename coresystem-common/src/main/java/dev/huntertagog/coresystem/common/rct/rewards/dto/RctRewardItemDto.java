package dev.huntertagog.coresystem.common.rct.rewards.dto;

public record RctRewardItemDto(
        String itemId,     // z.B. "minecraft:diamond_sword"
        int count,         // stack size
        String nbtBase64   // gzip+base64 von NbtCompound oder ""/null
) {
    public static RctRewardItemDto of(String itemId, int count, String nbtBase64) {
        return new RctRewardItemDto(itemId, count, nbtBase64);
    }
}
