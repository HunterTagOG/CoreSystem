package dev.huntertagog.coresystem.fabric.server.world;

import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

public interface RemoveFromRegistry<T> {
    @SuppressWarnings("unchecked")
    static <T> boolean remove(SimpleRegistry<T> registry, Identifier key) {
        return ((RemoveFromRegistry<T>) registry).remove(key);
    }

    boolean remove(Identifier key);
}
