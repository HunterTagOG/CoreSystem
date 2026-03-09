package dev.huntertagog.coresystem.fabric.common.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    private final Item baseItem;
    private int count = 1;
    private Text displayName;
    private final List<Text> loreLines = new ArrayList<>();

    private ItemBuilder(Item baseItem) {
        this.baseItem = baseItem;
    }

    public static ItemBuilder of(ItemConvertible convertible) {
        return new ItemBuilder(convertible.asItem());
    }

    public ItemBuilder count(int count) {
        this.count = Math.max(1, count);
        return this;
    }

    public ItemBuilder name(Text displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemBuilder loreLine(Text line) {
        this.loreLines.add(line);
        return this;
    }

    public ItemBuilder lore(List<Text> lines) {
        this.loreLines.clear();
        this.loreLines.addAll(lines);
        return this;
    }

    public ItemStack build() {
        ItemStack stack = new ItemStack(baseItem, count);

        if (displayName != null) {
            // 1.20.5+ Data Components: ITEM_NAME
            stack.set(DataComponentTypes.ITEM_NAME, displayName);
        }

        if (!loreLines.isEmpty()) {
            // LoreComponent arbeitet in Mojang/Yarn mit Text-Liste
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        }

        // Unbreakable / CustomModelData / Flags kannst du später via weiteren
        // Data Components ergänzen (UNBREAKABLE, CUSTOM_MODEL_DATA, etc.)

        return stack;
    }
}
