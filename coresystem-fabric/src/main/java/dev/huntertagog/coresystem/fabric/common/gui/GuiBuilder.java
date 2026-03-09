package dev.huntertagog.coresystem.fabric.common.gui;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class GuiBuilder {

    private final int rows; // 1–6
    private Text title;
    private final Map<Integer, GuiItem> items = new HashMap<>();

    private GuiBuilder(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
        this.rows = rows;
        this.title = Text.literal("Menu");
    }

    public static GuiBuilder chestRows(int rows) {
        return new GuiBuilder(rows);
    }

    public GuiBuilder title(Text title) {
        this.title = title;
        return this;
    }

    public GuiBuilder item(int slot, GuiItem guiItem) {
        if (slot < 0 || slot >= rows * 9) {
            throw new IllegalArgumentException("Slot out of bounds for " + rows + " rows: " + slot);
        }
        this.items.put(slot, guiItem);
        return this;
    }

    public GuiBuilder fillRow(int rowIndex, GuiItem guiItem) {
        if (rowIndex < 0 || rowIndex >= rows) {
            throw new IllegalArgumentException("Row index out of bounds: " + rowIndex);
        }
        int start = rowIndex * 9;
        for (int i = 0; i < 9; i++) {
            this.items.put(start + i, guiItem);
        }
        return this;
    }

    public GuiBuilder fillAll(GuiItem guiItem) {
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            this.items.put(i, guiItem);
        }
        return this;
    }

    public void open(ServerPlayerEntity player) {
        int size = rows * 9;
        SimpleInventory inventory = new SimpleInventory(size);

        // Items eintragen
        this.items.forEach((slot, guiItem) -> {
            if (slot >= 0 && slot < size) {
                inventory.setStack(slot, guiItem.itemStack().copy());
            }
        });

        NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {

            @Override
            public Text getDisplayName() {
                return title;
            }

            @Override
            public ScreenHandler createMenu(int syncId,
                                            PlayerInventory playerInventory,
                                            net.minecraft.entity.player.PlayerEntity playerEntity) {

                ScreenHandlerType<GenericContainerScreenHandler> type = switch (rows) {
                    case 1 -> ScreenHandlerType.GENERIC_9X1;
                    case 2 -> ScreenHandlerType.GENERIC_9X2;
                    case 3 -> ScreenHandlerType.GENERIC_9X3;
                    case 4 -> ScreenHandlerType.GENERIC_9X4;
                    case 5 -> ScreenHandlerType.GENERIC_9X5;
                    case 6 -> ScreenHandlerType.GENERIC_9X6;
                    default -> throw new IllegalStateException("Unexpected rows: " + rows);
                };

                return new GenericContainerScreenHandler(type, syncId, playerInventory, inventory, rows);
            }
        };

        player.openHandledScreen(factory);
    }
}
