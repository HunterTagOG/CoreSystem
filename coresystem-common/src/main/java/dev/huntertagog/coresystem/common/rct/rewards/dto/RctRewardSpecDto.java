package dev.huntertagog.coresystem.common.rct.rewards.dto;

import java.util.List;

public record RctRewardSpecDto(
        int money,
        List<RctRewardItemDto> items,
        List<String> consoleCommands,
        List<String> permissionNodes
) {
    public static RctRewardSpecDto empty() {
        return new RctRewardSpecDto(0, List.of(), List.of(), List.of());
    }

    public static RctRewardSpecDto defaultSpec() {
        return new RctRewardSpecDto(100, List.of(), List.of(), List.of());
    }
}
